import java.util.*;
import java.util.regex.Pattern;

/**
 * S2.07 - o outro lado da moeda: DESEMPENHO DE REGIME. Executa 12 lotes iguais de trabalho de CPU
 * (formatar, regex, mapa, ordenar) e imprime o tempo de cada lote. Na JVM o JIT acelera os lotes
 * seguintes; no native image (compilado antes da execucao, sem JIT) o tempo e ~constante.
 */
public class PeakApp {
    static final Pattern P = Pattern.compile("(\\d+)\\.(\\d{2})");
    static volatile long sink;

    static long batch() {
        long acc = 0;
        Map<Integer, Integer> m = new HashMap<>();
        for (int i = 0; i < 400_000; i++) {
            String s = String.format(Locale.ROOT, "%d.%02d", i, i % 100);
            var mt = P.matcher(s);
            if (mt.matches()) acc += Integer.parseInt(mt.group(1)) + Integer.parseInt(mt.group(2));
            m.merge(i % 5000, 1, Integer::sum);
        }
        int[] a = new int[200_000];
        Random r = new Random(7);
        for (int i = 0; i < a.length; i++) a[i] = r.nextInt();
        Arrays.sort(a);
        return acc + m.size() + a[a.length / 2];
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        long total = 0;
        for (int b = 1; b <= 12; b++) {
            long t0 = System.nanoTime();
            sink = batch();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            total += ms;
            sb.append(ms).append(b < 12 ? " " : "");
        }
        System.out.println("lotes (ms): " + sb + "  | total " + total + " ms");
    }
}
