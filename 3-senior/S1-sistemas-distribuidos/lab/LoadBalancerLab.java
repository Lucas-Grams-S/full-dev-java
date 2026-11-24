import java.util.*;

/** S1.01: estrategias de balanceamento sobre 4 servidores, um deles lento; e a lei de Amdahl. */
public class LoadBalancerLab {
    static final int SERVERS = 4;
    static final double MEAN_MS = 10, SLOW_MEAN_MS = 30; // servidor 3 e 3x mais lento
    static final int REQUESTS = 400_000;

    static final class Server {
        double freeAt = 0; final ArrayDeque<Double> inflight = new ArrayDeque<>();
        int outstanding(double now) { while (!inflight.isEmpty() && inflight.peekFirst() <= now) inflight.pollFirst(); return inflight.size(); }
    }

    public static void main(String[] a) {
        // capacidade: 3 servidores a 100 req/s + 1 a 33 req/s = 0,333 req/ms. carga = 75% da capacidade.
        double capacity = 3 * (1 / MEAN_MS) + 1 / SLOW_MEAN_MS;
        double lambda = 0.75 * capacity;
        System.out.printf("capacidade total = %.1f req/s; carga oferecida = %.1f req/s (75%%); servidor 3 e 3x mais lento%n", capacity * 1000, lambda * 1000);
        String[] names = {"aleatorio", "round-robin", "menos conexoes", "2 escolhas (P2C)"};
        for (int n = 0; n < names.length; n++) {
            final int mode = n;
            Random arr = new Random(7), pick = new Random(8), svc = new Random(9);
            Server[] s = new Server[SERVERS]; for (int i = 0; i < SERVERS; i++) s[i] = new Server();
            double t = 0; double[] lat = new double[REQUESTS]; int[] count = new int[SERVERS];
            for (int i = 0; i < REQUESTS; i++) {
                t += -Math.log(1 - arr.nextDouble()) / lambda;
                int k;
                switch (mode) {
                    case 0 -> k = pick.nextInt(SERVERS);
                    case 1 -> k = i % SERVERS;
                    case 2 -> { k = 0; int best = Integer.MAX_VALUE; for (int j = 0; j < SERVERS; j++) { int o = s[j].outstanding(t); if (o < best) { best = o; k = j; } } }
                    default -> { int x = pick.nextInt(SERVERS), y = pick.nextInt(SERVERS); k = s[x].outstanding(t) <= s[y].outstanding(t) ? x : y; }
                }
                double service = -Math.log(1 - svc.nextDouble()) * (k == 3 ? SLOW_MEAN_MS : MEAN_MS);
                double start = Math.max(t, s[k].freeAt);
                s[k].freeAt = start + service; s[k].inflight.addLast(s[k].freeAt);
                lat[i] = s[k].freeAt - t; count[k]++;
            }
            Arrays.sort(lat);
            System.out.printf("%-18s p50=%7.1f ms  p99=%8.1f ms  p99.9=%8.1f ms  pedidos por servidor=%s%n", names[n],
                lat[(int) (REQUESTS * .50)], lat[(int) (REQUESTS * .99)], lat[(int) (REQUESTS * .999)], Arrays.toString(count));
        }
        System.out.println();
        System.out.println("Amdahl: S(n) = 1 / ((1-p) + p/n), p = fracao paralelizavel (formula, nao medicao)");
        for (double p : new double[]{0.90, 0.95, 0.99}) {
            StringBuilder sb = new StringBuilder(String.format("p=%.2f  ", p));
            for (int n : new int[]{2, 4, 8, 16, 64, 1024}) sb.append(String.format("n=%d:%.1fx  ", n, 1 / ((1 - p) + p / n)));
            sb.append(String.format("limite=%.0fx", 1 / (1 - p)));
            System.out.println(sb);
        }
    }
}
