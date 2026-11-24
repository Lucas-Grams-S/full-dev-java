import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * P1.09 — condições de corrida e deadlocks, com classes no formato do PayFlow (Account com BigDecimal, UUID, regra 1: saldo nunca negativo).
 * Uso: java Races.java <lost-update|overdraft|conservation|deadlock|deadlock-fix|deadlock-trylock|throttle|throttle-fix>
 */
public class Races {

    // ---------- contas (versão ingênua: sem proteção nenhuma) ----------
    static class NaiveAccount {
        final UUID id; BigDecimal balance;
        NaiveAccount(UUID id, String balance) { this.id = id; this.balance = new BigDecimal(balance); }
        void deposit(BigDecimal v) { balance = balance.add(v); }                              // ler → somar → escrever
        boolean withdraw(BigDecimal v) {
            if (balance.compareTo(v) >= 0) { balance = balance.subtract(v); return true; }     // verificar → agir
            return false;
        }
    }

    // ---------- três correções para o mesmo contrato ----------
    static class SyncAccount {
        final UUID id; private BigDecimal balance;
        SyncAccount(UUID id, String balance) { this.id = id; this.balance = new BigDecimal(balance); }
        synchronized void deposit(BigDecimal v) { balance = balance.add(v); }
        synchronized boolean withdraw(BigDecimal v) { if (balance.compareTo(v) < 0) return false; balance = balance.subtract(v); return true; }
        synchronized BigDecimal balance() { return balance; }
    }

    static class CasAccount {
        final UUID id; private final AtomicReference<BigDecimal> balance;
        CasAccount(UUID id, String balance) { this.id = id; this.balance = new AtomicReference<>(new BigDecimal(balance)); }
        void deposit(BigDecimal v) { balance.updateAndGet(b -> b.add(v)); }                   // a função pode rodar mais de uma vez: tem de ser pura
        boolean withdraw(BigDecimal v) {
            while (true) {
                BigDecimal cur = balance.get();
                if (cur.compareTo(v) < 0) return false;
                if (balance.compareAndSet(cur, cur.subtract(v))) return true;
            }
        }
        BigDecimal balance() { return balance.get(); }
    }

    // ---------- transferência entre duas contas, com um lock por conta ----------
    static class LockedAccount {
        final UUID id; final ReentrantLock lock = new ReentrantLock(); BigDecimal balance;
        LockedAccount(UUID id, String balance) { this.id = id; this.balance = new BigDecimal(balance); }
    }

    /** Ingênua: trava origem e depois destino, na ordem dos argumentos. */
    static void transferNaive(LockedAccount from, LockedAccount to, BigDecimal v) {
        from.lock.lock();
        try {
            pause();                                           // alarga a janela do deadlock (num sistema real, é o tempo de uma consulta)
            to.lock.lock();
            try { if (from.balance.compareTo(v) >= 0) { from.balance = from.balance.subtract(v); to.balance = to.balance.add(v); } }
            finally { to.lock.unlock(); }
        } finally { from.lock.unlock(); }
    }

    /** Correta: ordem global fixa (pelo id), então ninguém espera em círculo. */
    static void transferOrdered(LockedAccount from, LockedAccount to, BigDecimal v) {
        LockedAccount first = from.id.compareTo(to.id) < 0 ? from : to, second = first == from ? to : from;
        first.lock.lock();
        try {
            pause();                                           // mesma janela (20 ms) da versão ingênua: comparação justa
            second.lock.lock();
            try { if (from.balance.compareTo(v) >= 0) { from.balance = from.balance.subtract(v); to.balance = to.balance.add(v); } }
            finally { second.lock.unlock(); }
        } finally { first.lock.unlock(); }
    }

    static final AtomicInteger retries = new AtomicInteger();

    /** Alternativa sem ordem global: tenta os dois locks; se não conseguir o segundo, SOLTA o primeiro, espera um tempo aleatório e repete. */
    static void transferTry(LockedAccount from, LockedAccount to, BigDecimal v) {
        while (true) {
            if (from.lock.tryLock()) {
                try {
                    pause();
                    if (to.lock.tryLock()) {
                        try { if (from.balance.compareTo(v) >= 0) { from.balance = from.balance.subtract(v); to.balance = to.balance.add(v); } return; }
                        finally { to.lock.unlock(); }
                    }
                } finally { from.lock.unlock(); }
            }
            retries.incrementAndGet();
            try { Thread.sleep(ThreadLocalRandom.current().nextLong(1, 15)); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }   // jitter: evita livelock
        }
    }

    static void pause() { try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }

    // ---------- utilitário ----------
    static void inParallel(int n, Runnable task) throws Exception {
        CountDownLatch go = new CountDownLatch(1), done = new CountDownLatch(n);
        for (int i = 0; i < n; i++) Thread.ofPlatform().start(() -> { try { go.await(); task.run(); } catch (InterruptedException e) { } finally { done.countDown(); } });
        go.countDown(); done.await();
    }

    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "lost-update" : args[0]) {
            case "lost-update" -> lostUpdate();
            case "overdraft" -> overdraft();
            case "conservation" -> conservation();
            case "deadlock" -> deadlock(false);
            case "deadlock-fix" -> deadlock(true);
            case "deadlock-trylock" -> deadlockTry();
            case "throttle" -> throttle(false);
            case "throttle-fix" -> throttle(true);
            default -> System.out.println("?");
        }
    }

    static void lostUpdate() throws Exception {
        int threads = 8, per = 20_000; BigDecimal one = new BigDecimal("1.00");
        NaiveAccount naive = new NaiveAccount(UUID.randomUUID(), "0.00");
        inParallel(threads, () -> { for (int i = 0; i < per; i++) naive.deposit(one); });
        System.out.printf("ingênua:      %s  (esperado %d.00)%n", naive.balance, threads * per);
        SyncAccount sync = new SyncAccount(UUID.randomUUID(), "0.00");
        inParallel(threads, () -> { for (int i = 0; i < per; i++) sync.deposit(one); });
        System.out.printf("synchronized: %s%n", sync.balance());
        CasAccount cas = new CasAccount(UUID.randomUUID(), "0.00");
        inParallel(threads, () -> { for (int i = 0; i < per; i++) cas.deposit(one); });
        System.out.printf("CAS (AtomicReference): %s%n", cas.balance());
    }

    static void overdraft() throws Exception {
        BigDecimal v = new BigDecimal("100.00"); int threads = 16;
        int worst = 0; BigDecimal lowest = BigDecimal.ZERO; int runs = 2000, bad = 0;
        for (int r = 0; r < runs; r++) {
            NaiveAccount a = new NaiveAccount(UUID.randomUUID(), "100.00");           // só dá para UM saque de 100,00
            AtomicInteger ok = new AtomicInteger();
            inParallel(threads, () -> { if (a.withdraw(v)) ok.incrementAndGet(); });
            if (ok.get() > 1) { bad++; worst = Math.max(worst, ok.get()); if (a.balance.compareTo(lowest) < 0) lowest = a.balance; }
        }
        System.out.printf("ingênua:      em %d de %d rodadas mais de um saque passou (até %d saques; saldo final até %s)%n", bad, runs, worst, lowest);
        int badSync = 0;
        for (int r = 0; r < runs; r++) {
            SyncAccount a = new SyncAccount(UUID.randomUUID(), "100.00"); AtomicInteger ok = new AtomicInteger();
            inParallel(threads, () -> { if (a.withdraw(v)) ok.incrementAndGet(); });
            if (ok.get() != 1) badSync++;
        }
        System.out.printf("synchronized: em %d de %d rodadas passou mais (ou menos) de um saque%n", badSync, runs);
    }

    static void conservation() throws Exception {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        NaiveAccount x = new NaiveAccount(a, "1000.00"), y = new NaiveAccount(b, "1000.00");
        BigDecimal ten = new BigDecimal("10.00");
        inParallel(8, () -> { for (int i = 0; i < 5000; i++) { NaiveAccount from = (i & 1) == 0 ? x : y, to = from == x ? y : x; if (from.withdraw(ten)) to.deposit(ten); } });
        System.out.printf("ingênua:      total = %s (começou com 2000.00)  x=%s y=%s%n", x.balance.add(y.balance), x.balance, y.balance);
        SyncAccount sx = new SyncAccount(a, "1000.00"), sy = new SyncAccount(b, "1000.00");
        inParallel(8, () -> { for (int i = 0; i < 5000; i++) { SyncAccount from = (i & 1) == 0 ? sx : sy, to = from == sx ? sy : sx; if (from.withdraw(ten)) to.deposit(ten); } });
        System.out.printf("cada método synchronized: total = %s  <- ainda pode ter dinheiro \"no ar\" (sacou de um, ainda não depositou no outro), mas ao final fecha%n", sx.balance().add(sy.balance()));
    }

    static void deadlock(boolean fix) throws Exception {
        LockedAccount a = new LockedAccount(new UUID(0, 1), "500.00"), b = new LockedAccount(new UUID(0, 2), "500.00");
        BigDecimal v = new BigDecimal("1.00"); long t0 = System.nanoTime();
        Thread t1 = Thread.ofPlatform().name("ana-para-bia").daemon().start(() -> { for (int i = 0; i < 100; i++) { if (fix) transferOrdered(a, b, v); else transferNaive(a, b, v); } });
        Thread t2 = Thread.ofPlatform().name("bia-para-ana").daemon().start(() -> { for (int i = 0; i < 100; i++) { if (fix) transferOrdered(b, a, v); else transferNaive(b, a, v); } });
        t1.join(fix ? 15_000 : 2500); t2.join(fix ? 15_000 : 500);
        System.out.printf("t1 viva? %s  t2 viva? %s  (tempo %.0f ms)%n", t1.isAlive(), t2.isAlive(), (System.nanoTime() - t0) / 1e6);
        var mx = ManagementFactory.getThreadMXBean();
        long[] ids = mx.findDeadlockedThreads();
        if (ids == null) { System.out.println("findDeadlockedThreads() = null (sem deadlock); saldos a=" + a.balance + " b=" + b.balance); return; }
        System.out.println("DEADLOCK detectado em " + ids.length + " threads:");
        for (ThreadInfo ti : mx.getThreadInfo(ids, true, true))
            System.out.println("  " + ti.getThreadName() + " espera o lock de " + ti.getLockOwnerName() + " (" + ti.getLockName().replaceAll("@.*", "") + ")");
    }

    static void deadlockTry() throws Exception {
        LockedAccount a = new LockedAccount(new UUID(0, 1), "500.00"), b = new LockedAccount(new UUID(0, 2), "500.00");
        BigDecimal v = new BigDecimal("1.00"); long t0 = System.nanoTime();
        Thread t1 = Thread.ofPlatform().daemon().start(() -> { for (int i = 0; i < 100; i++) transferTry(a, b, v); });
        Thread t2 = Thread.ofPlatform().daemon().start(() -> { for (int i = 0; i < 100; i++) transferTry(b, a, v); });
        t1.join(10_000); t2.join(10_000);
        System.out.printf("tryLock + jitter: t1 viva? %s  t2 viva? %s  saldos a=%s b=%s  retentativas=%d  tempo=%.0f ms  deadlock? %s%n", t1.isAlive(), t2.isAlive(), a.balance, b.balance, retries.get(),
                (System.nanoTime() - t0) / 1e6, ManagementFactory.getThreadMXBean().findDeadlockedThreads() != null);
    }

    // ---------- LoginThrottle do PayFlow (J8.06), simplificado ----------
    static class Throttle {
        private final int max; private int failures; private int inFlight;
        Throttle(int max) { this.max = max; }
        synchronized boolean isLocked() { return failures >= max; }
        synchronized void failure() { failures++; }
        /** versão corrigida: reserva a tentativa ANTES do trabalho caro (verifica e age numa só região crítica). */
        synchronized boolean tryBegin() { if (failures + inFlight >= max) return false; inFlight++; return true; }
        synchronized void endFailure() { inFlight--; failures++; }
    }

    static void throttle(boolean fix) throws Exception {
        int max = 5, attackers = 60;
        Throttle t = new Throttle(max); AtomicInteger bcryptCalls = new AtomicInteger();
        inParallel(attackers, () -> {
            if (!fix) {
                if (t.isLocked()) return;                          // 1) verifica
                bcryptCalls.incrementAndGet(); pause();            // 2) trabalho caro (BCrypt ~ dezenas de ms)
                t.failure();                                       // 3) registra o erro
            } else {
                if (!t.tryBegin()) return;
                bcryptCalls.incrementAndGet(); pause();
                t.endFailure();
            }
        });
        System.out.printf("%s: limite = %d erros; %d tentativas simultâneas; senhas realmente testadas = %d%n", fix ? "corrigido " : "ingênuo   ", max, attackers, bcryptCalls.get());
    }
}
