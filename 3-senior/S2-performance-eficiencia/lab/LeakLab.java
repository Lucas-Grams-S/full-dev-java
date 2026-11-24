import java.util.*;

/**
 * S2.04 - vazamento de memoria de proposito.
 *   java LeakLab leak  <req/s ou 0> <segundos>   -> cache por requestId, sem limite (VAZA)
 *   java LeakLab fixed <req/s ou 0> <segundos>   -> mesmo trafego, cache LRU limitado (NAO vaza)
 * Bug: a chave do cache inclui o requestId (unico a cada chamada): nunca acerta e nunca sai.
 */
public class LeakLab {
    record Session(String id, byte[] payload, long createdAt) {}

    static final Map<String, Session> CACHE = new HashMap<>();
    static final int MAX = 5_000;
    static final Map<String, Session> LRU = new LinkedHashMap<>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Session> e) { return size() > MAX; }
    };
    static long hits, misses;

    static Session handle(boolean fixed, long n) {
        String userId = "user-" + (n % 500);
        String requestId = "req-" + n;                       // unico por chamada
        String key = fixed ? userId : userId + ":" + requestId;   // <-- o bug esta na chave
        Map<String, Session> cache = fixed ? LRU : CACHE;
        Session s = cache.get(key);
        if (s == null) {
            misses++;
            s = new Session(key, new byte[2048], System.nanoTime());
            cache.put(key, s);
        } else hits++;
        return s;
    }

    public static void main(String[] a) throws Exception {
        boolean fixed = a[0].equals("fixed");
        int rate = Integer.parseInt(a[1]);
        long end = System.nanoTime() + Long.parseLong(a[2]) * 1_000_000_000L;
        long n = 0, t0 = System.nanoTime();
        while (System.nanoTime() < end) {
            handle(fixed, n++);
            if (rate > 0 && n % 100 == 0) {                      // regula a vazao (100 em 100)
                long due = t0 + n * 1_000_000_000L / rate;
                long wait = due - System.nanoTime();
                if (wait > 0) Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
            }
            if (n % 200_000 == 0) report(n);
        }
        report(n);
        System.out.println("fim sem OutOfMemoryError");
    }

    static void report(long n) {
        Runtime r = Runtime.getRuntime();
        long usedMb = (r.totalMemory() - r.freeMemory()) >> 20;
        System.out.printf(Locale.ROOT, "req=%d cache=%d acertos=%d erros=%d heapUsado~%d MB%n",
                n, CACHE.size() + LRU.size(), hits, misses, usedMb);
    }
}
