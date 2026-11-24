import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * P1.13 — "o saldo que não bate": um PayFlow em miniatura, só em memória, que reproduz as regras 1 a 5 do projeto sob carga concorrente.
 * `Store` imita o banco: lê "cópias" das linhas (como o Hibernate) e grava com `UPDATE ... WHERE id=? AND version=?` (como o @Version).
 * Uso: java StudyCase.java <naive|optimistic|retry|locks|idem|all> [hot]
 */
public class StudyCase {

    record Row(UUID id, long cents, long version) {}
    record Entry(UUID account, String type, long cents, UUID transfer) {}
    record Change(Row before, long newCents) {}

    /** "Banco": linhas imutáveis, commit atômico com verificação de versão (transação curta). */
    static class Store {
        final Map<UUID, Row> rows = new ConcurrentHashMap<>();
        final Queue<Entry> ledger = new ConcurrentLinkedQueue<>();
        final Object commitLock = new Object();
        Row read(UUID id) { return rows.get(id); }
        /** commit com versão: tudo ou nada; falha (false) se alguma linha mudou desde a leitura. */
        boolean commitVersioned(List<Change> changes, List<Entry> entries) {
            synchronized (commitLock) {
                for (Change c : changes) if (rows.get(c.before().id()).version() != c.before().version()) return false;
                apply(changes, entries); return true;
            }
        }
        /** commit "às cegas" (sem WHERE version = ?): o último a gravar vence. */
        void commitBlind(List<Change> changes, List<Entry> entries) { synchronized (commitLock) { apply(changes, entries); } }
        private void apply(List<Change> changes, List<Entry> entries) {
            for (Change c : changes) rows.put(c.before().id(), new Row(c.before().id(), c.newCents(), rows.get(c.before().id()).version() + 1));
            ledger.addAll(entries);
        }
    }

    enum Outcome { OK, INSUFFICIENT, CONFLICT }

    interface Strategy { Outcome transfer(Store s, UUID from, UUID to, long cents); }

    // A) ingênua: lê, decide, grava às cegas (regra 3 e 1 quebram)
    static Strategy naive = (s, from, to, cents) -> {
        Row f = s.read(from), t = s.read(to);
        if (f.cents() < cents) return Outcome.INSUFFICIENT;
        UUID tx = UUID.randomUUID();
        s.commitBlind(List.of(new Change(f, f.cents() - cents), new Change(t, t.cents() + cents)),
                List.of(new Entry(from, "DEBIT", cents, tx), new Entry(to, "CREDIT", cents, tx)));
        return Outcome.OK;
    };

    // B) otimista: igual ao @Version do PayFlow (J5): a colisão vira CONFLICT (o 409 da J6)
    static Strategy optimistic = (s, from, to, cents) -> {
        Row f = s.read(from), t = s.read(to);
        if (f.cents() < cents) return Outcome.INSUFFICIENT;
        UUID tx = UUID.randomUUID();
        boolean ok = s.commitVersioned(List.of(new Change(f, f.cents() - cents), new Change(t, t.cents() + cents)),
                List.of(new Entry(from, "DEBIT", cents, tx), new Entry(to, "CREDIT", cents, tx)));
        return ok ? Outcome.OK : Outcome.CONFLICT;
    };

    // C) otimista + retentativa com backoff exponencial e jitter (o "cliente educado" da J6)
    static Strategy retry = (s, from, to, cents) -> {
        for (int attempt = 0; attempt < 12; attempt++) {
            Outcome o = optimistic.transfer(s, from, to, cents);
            if (o != Outcome.CONFLICT) return o;
            long cap = Math.min(1L << attempt, 20);
            try { Thread.sleep(ThreadLocalRandom.current().nextLong(0, cap + 1)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return Outcome.CONFLICT; }
        }
        return Outcome.CONFLICT;
    };

    // D) lock por conta dentro da JVM (ordem global por id): funciona com UMA instância; com duas, não protege nada
    static final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();
    static Strategy locked = (s, from, to, cents) -> {
        UUID a = from.compareTo(to) < 0 ? from : to, b = a == from ? to : from;
        ReentrantLock la = locks.computeIfAbsent(a, k -> new ReentrantLock()), lb = locks.computeIfAbsent(b, k -> new ReentrantLock());
        la.lock();
        try {
            lb.lock();
            try { return optimistic.transfer(s, from, to, cents); }     // dentro do lock não há colisão; o commit versionado continua como rede de segurança
            finally { lb.unlock(); }
        } finally { la.unlock(); }
    };

    static List<UUID> ids = new ArrayList<>();

    static Store newStore(int accounts, long initialCents) {
        Store s = new Store(); ids = new ArrayList<>();
        for (int i = 0; i < accounts; i++) { UUID id = new UUID(0, i + 1); ids.add(id); s.rows.put(id, new Row(id, initialCents, 0)); s.ledger.add(new Entry(id, "CREDIT", initialCents, null)); }
        return s;
    }

    static void run(String name, Strategy st, boolean hot) throws Exception {
        int accounts = 10, threads = 16, perThread = 4000; long initial = hot ? 10_000_000 : 100_000;   // conta quente: saldo alto, para a disputa ser pela versão e não pelo saldo
        Store s = newStore(accounts, initial);
        EnumMap<Outcome, AtomicInteger> count = new EnumMap<>(Outcome.class);
        for (Outcome o : Outcome.values()) count.put(o, new AtomicInteger());
        long t0 = System.nanoTime();
        try (ExecutorService ex = Executors.newFixedThreadPool(threads)) {
            CountDownLatch go = new CountDownLatch(1); List<Future<?>> fs = new ArrayList<>();
            for (int t = 0; t < threads; t++) fs.add(ex.submit(() -> {
                go.await(); var r = ThreadLocalRandom.current();
                for (int i = 0; i < perThread; i++) {
                    UUID from = hot ? ids.get(0) : ids.get(r.nextInt(accounts)), to = ids.get(1 + r.nextInt(accounts - 1));
                    if (from.equals(to)) continue;
                    count.get(st.transfer(s, from, to, 1 + r.nextInt(50))).incrementAndGet();
                }
                return null;
            }));
            go.countDown(); for (var f : fs) f.get();
        }
        double ms = (System.nanoTime() - t0) / 1e6;
        long total = s.rows.values().stream().mapToLong(Row::cents).sum();
        long negative = s.rows.values().stream().filter(r -> r.cents() < 0).count();
        Map<UUID, Long> fromLedger = new HashMap<>();
        for (Entry e : s.ledger) fromLedger.merge(e.account(), e.type().equals("CREDIT") ? e.cents() : -e.cents(), Long::sum);
        long mismatched = s.rows.values().stream().filter(r -> !Objects.equals(fromLedger.get(r.id()), r.cents())).count();
        System.out.printf("%-26s OK=%-6d saldo-insuf.=%-5d 409=%-6d %6.0f ms | total=%s (esperado %s) negativas=%d contas_com_saldo≠lançamentos=%d%n",
                name, count.get(Outcome.OK).get(), count.get(Outcome.INSUFFICIENT).get(), count.get(Outcome.CONFLICT).get(), ms,
                total, accounts * initial, negative, mismatched);
    }

    static void idempotency() throws Exception {
        // 8 requisições simultâneas com a MESMA chave de idempotência (cliente impaciente apertou o botão várias vezes)
        Map<String, UUID> naiveKeys = new ConcurrentHashMap<>(); AtomicInteger naiveCreated = new AtomicInteger();
        Map<String, UUID> safeKeys = new ConcurrentHashMap<>(); AtomicInteger safeCreated = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1), done = new CountDownLatch(8);
        for (int i = 0; i < 8; i++) Thread.ofPlatform().start(() -> {
            try {
                go.await();
                if (naiveKeys.get("key-1") == null) {                                   // verifica...
                    try { Thread.sleep(5); } catch (InterruptedException e) { }         // (a transferência leva alguns ms)
                    naiveCreated.incrementAndGet(); naiveKeys.put("key-1", UUID.randomUUID());   // ...e age
                }
                UUID mine = UUID.randomUUID();
                if (safeKeys.putIfAbsent("key-1", mine) == null) safeCreated.incrementAndGet();  // "UNIQUE (idempotency_key)": só um insere
            } catch (InterruptedException e) { } finally { done.countDown(); }
        });
        go.countDown(); done.await();
        System.out.printf("verifica-e-age:      %d transferências criadas para a mesma chave%n", naiveCreated.get());
        System.out.printf("putIfAbsent/UNIQUE:  %d transferência criada para a mesma chave%n", safeCreated.get());
    }

    public static void main(String[] args) throws Exception {
        boolean hot = args.length > 1 && args[1].equals("hot");
        String mode = args.length == 0 ? "all" : args[0];
        if (mode.equals("all") || mode.equals("naive")) run("A ingênua (às cegas)", naive, hot);
        if (mode.equals("all") || mode.equals("optimistic")) run("B otimista (@Version)", optimistic, hot);
        if (mode.equals("all") || mode.equals("retry")) run("C otimista + retry/jitter", retry, hot);
        if (mode.equals("all") || mode.equals("locks")) run("D lock por conta (JVM)", locked, hot);
        if (mode.equals("all") || mode.equals("idem")) idempotency();
    }
}
