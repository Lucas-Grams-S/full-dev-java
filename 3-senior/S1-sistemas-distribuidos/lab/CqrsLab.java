import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** S1.05: lado de escrita, modelo de leitura assincrono, leitura apos escrita e reconstrucao. */
public class CqrsLab {
    record Entry(long seq, String account, long cents) {}                 // evento/lancamento no lado de escrita

    static final CopyOnWriteArrayList<Entry> log = new CopyOnWriteArrayList<>();   // fonte da verdade (append-only)
    static final ConcurrentHashMap<String, Long> balanceView = new ConcurrentHashMap<>(); // modelo de leitura
    static final AtomicLong projectedUpTo = new AtomicLong(0);

    public static void main(String[] args) throws Exception {
        // projetor assincrono com atraso por evento
        ScheduledExecutorService proj = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger next = new AtomicInteger(0);
        proj.scheduleWithFixedDelay(() -> {
            int i = next.get(); if (i < log.size()) { Entry e = log.get(i); try { Thread.sleep(30); } catch (InterruptedException x) { } balanceView.merge(e.account(), e.cents(), Long::sum); projectedUpTo.set(e.seq()); next.incrementAndGet(); }
        }, 0, 1, TimeUnit.MILLISECONDS);

        System.out.println("== 1. Escrever e ler logo em seguida (projecao com 30 ms por evento) ==");
        long seq = 0; long writtenSeq = 0;
        for (int i = 0; i < 5; i++) { log.add(new Entry(++seq, "acc-1", 1000)); writtenSeq = seq; }
        System.out.println("apos gravar 5 depositos de 10,00: leitura do modelo de leitura = " + fmt(balanceView.getOrDefault("acc-1", 0L)) + " (esperado 50,00)  projetado ate seq=" + projectedUpTo.get());
        long t0 = System.nanoTime(); long want = writtenSeq;
        while (projectedUpTo.get() < want) Thread.sleep(1);
        System.out.printf("esperando o token de versao seq>=%d: leitura = %s apos %d ms%n", want, fmt(balanceView.get("acc-1")), (System.nanoTime() - t0) / 1_000_000);
        proj.shutdownNow();

        System.out.println("\n== 2. Reconstruir o modelo de leitura do zero a partir do log (1.000.000 de lancamentos, 100.000 contas) ==");
        List<Entry> big = new ArrayList<>(1_000_000); Random r = new Random(1);
        for (int i = 1; i <= 1_000_000; i++) big.add(new Entry(i, "acc-" + r.nextInt(100_000), r.nextInt(2000) - 500));
        long a = System.nanoTime(); Map<String, Long> view = new HashMap<>();
        for (Entry e : big) view.merge(e.account(), e.cents(), Long::sum);
        System.out.printf("reconstrucao completa: %d contas em %d ms%n", view.size(), (System.nanoTime() - a) / 1_000_000);
        // invariante: soma da visao == soma do log
        long s1 = big.stream().mapToLong(Entry::cents).sum(), s2 = view.values().stream().mapToLong(Long::longValue).sum();
        System.out.println("invariante soma(log) == soma(visao): " + (s1 == s2) + " (" + fmt(s1) + ")");
        // projecao com BUG (ignora saques) detectada pela comparacao com o log
        Map<String, Long> bad = new HashMap<>(); for (Entry e : big) if (e.cents() > 0) bad.merge(e.account(), e.cents(), Long::sum);
        long s3 = bad.values().stream().mapToLong(Long::longValue).sum();
        System.out.println("projecao com bug (ignora debitos): soma(visao)=" + fmt(s3) + " != soma(log)=" + fmt(s1) + " -> divergencia detectada: " + (s3 != s1));
    }
    static String fmt(Long c) { return c == null ? "(vazio)" : String.format("%d,%02d", c / 100, Math.abs(c % 100)); }
}
