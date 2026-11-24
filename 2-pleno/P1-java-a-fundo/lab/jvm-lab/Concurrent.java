import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/** P1.07 — java.util.concurrent. Uso: java Concurrent.java <locks|counters|maps|lists|queue|semaphore> */
public class Concurrent {

    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "locks" : args[0]) {
            case "locks" -> locks();
            case "counters" -> counters();
            case "maps" -> maps();
            case "lists" -> lists();
            case "queue" -> queue();
            case "semaphore" -> semaphore();
            default -> System.out.println("?");
        }
    }

    /** Roda `task` em n threads, todas largando juntas, e devolve o tempo em ms. */
    static double inParallel(int n, Runnable task) throws Exception {
        CountDownLatch ready = new CountDownLatch(n), go = new CountDownLatch(1), done = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            Thread.ofPlatform().start(() -> { ready.countDown(); try { go.await(); task.run(); } catch (InterruptedException e) { } finally { done.countDown(); } });
        }
        ready.await();
        long t0 = System.nanoTime(); go.countDown(); done.await();
        return (System.nanoTime() - t0) / 1e6;
    }

    static void locks() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        Thread holder = Thread.ofPlatform().daemon().start(() -> { lock.lock(); try { Thread.sleep(800); } catch (InterruptedException e) { } finally { lock.unlock(); } });
        Thread.sleep(100);
        System.out.println("tryLock() imediato:        " + lock.tryLock());
        System.out.println("tryLock(200 ms):           " + lock.tryLock(200, TimeUnit.MILLISECONDS) + "  (synchronized não permite desistir)");
        System.out.println("isLocked / quem espera:    " + lock.isLocked() + " / " + lock.hasQueuedThreads());
        Thread waiter = Thread.ofPlatform().start(() -> {
            try { lock.lockInterruptibly(); System.out.println("  waiter: entrou"); lock.unlock(); }
            catch (InterruptedException e) { System.out.println("  waiter: interrompido enquanto esperava o lock"); }
        });
        Thread.sleep(100); waiter.interrupt(); waiter.join();
        holder.join();
        // reentrância: a mesma thread pode pegar o lock de novo
        lock.lock(); lock.lock();
        System.out.println("reentrante, holdCount =    " + lock.getHoldCount());
        lock.unlock(); lock.unlock();
        // o padrão obrigatório: unlock no finally
        ReadWriteLock rw = new ReentrantReadWriteLock();
        rw.readLock().lock(); rw.readLock().lock();                       // vários leitores ao mesmo tempo
        System.out.println("2 leitores ao mesmo tempo:  ok; escritor consegue agora? " + rw.writeLock().tryLock());
        rw.readLock().unlock(); rw.readLock().unlock();
        System.out.println("sem leitores, escritor?     " + rw.writeLock().tryLock());
    }

    static void counters() throws Exception {
        int threads = 8; int per = 2_000_000;
        long[] plain = {0};
        double t1 = inParallel(threads, () -> { for (int i = 0; i < per; i++) plain[0]++; });
        System.out.printf("long comum:       %,d (esperado %,d)  %.0f ms  <- perdeu incrementos%n", plain[0], (long) threads * per, t1);
        long[] sync = {0}; Object mon = new Object();
        double t2 = inParallel(threads, () -> { for (int i = 0; i < per; i++) synchronized (mon) { sync[0]++; } });
        System.out.printf("synchronized:     %,d  %.0f ms%n", sync[0], t2);
        AtomicLong atomic = new AtomicLong();
        double t3 = inParallel(threads, () -> { for (int i = 0; i < per; i++) atomic.incrementAndGet(); });
        System.out.printf("AtomicLong:       %,d  %.0f ms%n", atomic.get(), t3);
        LongAdder adder = new LongAdder();
        double t4 = inParallel(threads, () -> { for (int i = 0; i < per; i++) adder.increment(); });
        System.out.printf("LongAdder:        %,d  %.0f ms%n", adder.sum(), t4);
        // CAS à mão: o laço que o AtomicLong faz por baixo
        AtomicLong cas = new AtomicLong();
        inParallel(threads, () -> { for (int i = 0; i < 100_000; i++) { long cur; do { cur = cas.get(); } while (!cas.compareAndSet(cur, cur + 1)); } });
        System.out.printf("CAS manual:       %,d (esperado %,d)%n", cas.get(), threads * 100_000L);
        // ABA em uma linha: o CAS não vê que o valor mudou e voltou
        AtomicInteger v = new AtomicInteger(10);
        int seen = v.get(); v.set(99); v.set(10);
        System.out.println("ABA: valor mudou e voltou; compareAndSet(10, 11) = " + v.compareAndSet(seen, 11));
        AtomicStampedReference<Integer> st = new AtomicStampedReference<>(10, 0);
        int[] stamp = new int[1]; Integer ref = st.get(stamp);
        st.set(99, 1); st.set(10, 2);
        System.out.println("com carimbo:      compareAndSet(10, 11, carimbo 0) = " + st.compareAndSet(ref, 11, stamp[0], stamp[0] + 1));
    }

    static void maps() throws Exception {
        int threads = 8, per = 50_000;
        Map<String, Integer> plain = new HashMap<>();
        AtomicInteger failures = new AtomicInteger();
        inParallel(threads, () -> {
            try { for (int i = 0; i < per; i++) plain.merge("k" + (i % 100), 1, Integer::sum); }
            catch (RuntimeException e) { failures.incrementAndGet(); }          // o HashMap pode detectar a bagunça e lançar
        });
        int total = plain.values().stream().mapToInt(Integer::intValue).sum();
        System.out.printf("HashMap + merge:                 soma=%,d (esperado %,d) chaves=%d (esperado 100) threads que lançaram exceção=%d%n", total, threads * per, plain.size(), failures.get());
        ConcurrentHashMap<String, Integer> chmBad = new ConcurrentHashMap<>();
        inParallel(threads, () -> { for (int i = 0; i < per; i++) { String k = "k" + (i % 100); Integer c = chmBad.get(k); chmBad.put(k, c == null ? 1 : c + 1); } });
        System.out.printf("ConcurrentHashMap get+put:       soma=%,d (esperado %,d)  <- cada operação é segura, a sequência não%n", chmBad.values().stream().mapToInt(Integer::intValue).sum(), threads * per);
        ConcurrentHashMap<String, Integer> chm = new ConcurrentHashMap<>();
        inParallel(threads, () -> { for (int i = 0; i < per; i++) chm.merge("k" + (i % 100), 1, Integer::sum); });
        System.out.printf("ConcurrentHashMap merge:         soma=%,d (esperado %,d)%n", chm.values().stream().mapToInt(Integer::intValue).sum(), threads * per);
        // computeIfAbsent: a função roda uma vez por chave
        ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>(); AtomicInteger loads = new AtomicInteger();
        inParallel(threads, () -> { for (int i = 0; i < 1000; i++) cache.computeIfAbsent("conta-" + (i % 10), k -> { loads.incrementAndGet(); return k.toUpperCase(); }); });
        System.out.println("computeIfAbsent: 10 chaves, 8 threads x 1000 acessos -> carregou " + loads.get() + " vezes");
        // atenção: ConcurrentHashMap não aceita null
        try { chm.put("x", null); } catch (NullPointerException e) { System.out.println("put(x, null) -> NullPointerException"); }
    }

    static void lists() throws Exception {
        List<Integer> arr = new ArrayList<>(List.of(1, 2, 3));
        try { for (Integer i : arr) if (i == 2) arr.add(99); } catch (ConcurrentModificationException e) { System.out.println("ArrayList alterada no for-each: " + e.getClass().getSimpleName()); }
        CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        for (Integer i : cow) if (i == 2) cow.add(99);
        System.out.println("CopyOnWriteArrayList alterada no for-each: sem erro, lista agora = " + cow + " (o laço viu o retrato antigo)");
        // custo: cada escrita copia o array
        CopyOnWriteArrayList<Integer> big = new CopyOnWriteArrayList<>(); List<Integer> sync = Collections.synchronizedList(new ArrayList<>());
        long t0 = System.nanoTime(); for (int i = 0; i < 30_000; i++) big.add(i);
        long t1 = System.nanoTime(); for (int i = 0; i < 30_000; i++) sync.add(i);
        long t2 = System.nanoTime();
        System.out.printf("30 000 adds: CopyOnWrite %.0f ms  x  synchronizedList %.0f ms%n", (t1 - t0) / 1e6, (t2 - t1) / 1e6);
    }

    static void queue() throws Exception {
        BlockingQueue<String> q = new ArrayBlockingQueue<>(3);                  // fila LIMITADA: o produtor sente a pressão
        String POISON = "<fim>";
        Thread consumer = Thread.ofPlatform().start(() -> {
            try { String m; while (!(m = q.take()).equals(POISON)) { Thread.sleep(150); System.out.println("  consumiu " + m); } System.out.println("  consumidor: recebeu o veneno, saindo"); }
            catch (InterruptedException e) { }
        });
        long t0 = System.nanoTime();
        for (int i = 1; i <= 6; i++) { q.put("evento-" + i); System.out.printf("produziu evento-%d (a fila tinha espaço após %.0f ms)%n", i, (System.nanoTime() - t0) / 1e6); }
        q.put(POISON); consumer.join();
        System.out.println("offer() em fila cheia devolve false: " + fillAndOffer());
    }
    static boolean fillAndOffer() { var q = new ArrayBlockingQueue<Integer>(1); q.add(1); return q.offer(2); }

    static void semaphore() throws Exception {
        Semaphore permits = new Semaphore(3);                                      // no máximo 3 chamadas simultâneas ao provedor
        AtomicInteger inside = new AtomicInteger(), peak = new AtomicInteger();
        double ms = inParallel(10, () -> {
            try { permits.acquire(); try { int now = inside.incrementAndGet(); peak.accumulateAndGet(now, Math::max); Thread.sleep(100); inside.decrementAndGet(); } finally { permits.release(); } }
            catch (InterruptedException e) { }
        });
        System.out.printf("10 chamadas, no máximo 3 por vez: pico observado = %d, tempo = %.0f ms (4 \"ondas\" de 100 ms)%n", peak.get(), ms);
    }
}
