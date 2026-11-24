import java.util.*;

/**
 * S2.03 - vies de amostragem. Um unico thread alterna dois metodos:
 *   heavy(): laco "contado" de int (~90% da CPU, quase sem pontos de seguranca no meio)
 *   light(): muitas chamadas curtas (~10% da CPU; cada retorno e um ponto de seguranca)
 * Um "amostrador de pobre" (Thread.getStackTrace a cada 5 ms) e o JFR olham a mesma execucao.
 *   java -XX:StartFlightRecording:filename=<arq>,settings=profile BiasLab <segundos>
 */
public class BiasLab {
    static volatile long sink;
    static final long[] DATA = new long[1 << 16];

    static long heavy() {
        long s = 0;
        for (int r = 0; r < 40; r++) {
            for (int i = 0; i < DATA.length; i++) {      // laco contado de int
                s += DATA[i] * 31 + i;
                DATA[i] = s;
            }
        }
        return s;
    }

    static long tiny(int x) { return x * 2654435761L; }

    static long light() {
        long s = 0;
        for (int i = 0; i < 400_000; i++) s += tiny(i);
        return s;
    }

    public static void main(String[] a) throws Exception {
        int secs = Integer.parseInt(a[0]);
        long end = System.nanoTime() + secs * 1_000_000_000L;
        Thread worker = Thread.ofPlatform().name("worker").start(() -> {
            while (System.nanoTime() < end) { sink = heavy(); sink = light(); }
        });
        Map<String, Integer> top = new TreeMap<>();
        int n = 0;
        while (worker.isAlive()) {
            StackTraceElement[] st = worker.getStackTrace();
            if (st.length > 0) { top.merge(st[0].getMethodName(), 1, Integer::sum); n++; }
            Thread.sleep(5);
        }
        System.out.println("amostrador de pobre (Thread.getStackTrace, " + n + " amostras): ");
        final int total = n;
        top.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).forEach(e ->
                System.out.printf(Locale.ROOT, "  %-8s %5d  %5.1f%%%n", e.getKey(), e.getValue(), 100.0 * e.getValue() / total));
    }
}
