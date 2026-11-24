import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;

/**
 * S2.02/S2.03 - aplicacao com TRES problemas plantados de proposito.
 *   java ProfileApp bad   <segundos>   -> versao com os problemas
 *   java ProfileApp fixed <segundos>   -> versao corrigida
 * W1 validate : regex recompilada a cada chamada (CPU)          x Pattern pre-compilado
 * W2 report   : String += em laco (alocacao)                    x StringBuilder
 * W3 counters : synchronized + trabalho dentro do lock (espera) x LongAdder por chave
 */
public class ProfileApp {
    static final Pattern CODE = Pattern.compile("[A-Z]{3}-\\d{4}");
    static final Object LOCK = new Object();
    static final Map<String, Long> COUNTS = new HashMap<>();
    static final ConcurrentHashMap<String, LongAdder> ADDERS = new ConcurrentHashMap<>();
    static final String[] KEYS = {"a", "b", "c", "d", "e", "f", "g", "h"};
    static volatile long sink;

    // ---------- W1
    static boolean validateBad(String s) { return s.matches("[A-Z]{3}-\\d{4}"); }
    static boolean validateFixed(String s) { return CODE.matcher(s).matches(); }

    // ---------- W2
    static String reportBad(int lines) {
        String out = "";
        for (int i = 0; i < lines; i++) out += "line " + i + ";";
        return out;
    }
    static String reportFixed(int lines) {
        StringBuilder sb = new StringBuilder(lines * 12);
        for (int i = 0; i < lines; i++) sb.append("line ").append(i).append(';');
        return sb.toString();
    }

    // ---------- W3
    static void countBad(String k) {
        synchronized (LOCK) {
            long h = 0;
            for (int i = 0; i < 2000; i++) h += (k.hashCode() + i) * 31L;   // "trabalho" dentro do lock
            COUNTS.merge(k, 1L + (h & 1), Long::sum);
        }
    }
    static void countFixed(String k) {
        long h = 0;
        for (int i = 0; i < 2000; i++) h += (k.hashCode() + i) * 31L;       // trabalho FORA do lock
        sink = h;
        ADDERS.computeIfAbsent(k, x -> new LongAdder()).add(1);
    }

    public static void main(String[] a) throws Exception {
        boolean fixed = a[0].equals("fixed");
        int secs = Integer.parseInt(a[1]);
        long end = System.nanoTime() + secs * 1_000_000_000L;
        AtomicLong w1 = new AtomicLong(), w2 = new AtomicLong(), w3 = new AtomicLong();
        List<Thread> ts = new ArrayList<>();
        for (int t = 0; t < 2; t++) ts.add(Thread.ofPlatform().name("validate-" + t).unstarted(() -> {
            long n = 0; Random r = new Random();
            while (System.nanoTime() < end) {
                String s = "ABC-" + (1000 + r.nextInt(9000));
                sink = (fixed ? validateFixed(s) : validateBad(s)) ? 1 : 0; n++;
            }
            w1.addAndGet(n);
        }));
        for (int t = 0; t < 2; t++) ts.add(Thread.ofPlatform().name("report-" + t).unstarted(() -> {
            long n = 0;
            while (System.nanoTime() < end) { sink = (fixed ? reportFixed(1500) : reportBad(1500)).length(); n++; }
            w2.addAndGet(n);
        }));
        for (int t = 0; t < 4; t++) ts.add(Thread.ofPlatform().name("counter-" + t).unstarted(() -> {
            long n = 0; Random r = new Random();
            while (System.nanoTime() < end) {
                String k = KEYS[r.nextInt(KEYS.length)];
                if (fixed) countFixed(k); else countBad(k);
                n++;
            }
            w3.addAndGet(n);
        }));
        ts.forEach(Thread::start);
        for (Thread t : ts) t.join();
        System.out.printf(Locale.ROOT, "%s: validate=%d ops/s  report=%d ops/s  counters=%d ops/s%n",
                fixed ? "FIXED" : "BAD", w1.get() / secs, w2.get() / secs, w3.get() / secs);
    }
}
