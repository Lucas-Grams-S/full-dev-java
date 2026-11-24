import java.util.*;

/**
 * S4.07 — capacity planning in three small experiments (MODEL; no PayFlow, no AWS).
 *  queue   : utilization x latency in a c-server queue (discrete-event simulation, exponential service)
 *  little  : Little's law L = lambda * W applied to threads and connection pools
 *  forecast: least-squares projection of a growing metric (disk) and how a short window misleads
 * Run: java CapacityLab.java queue | little | forecast
 */
public class CapacityLab {
    public static void main(String[] a) {
        switch (a.length > 0 ? a[0] : "queue") {
            case "queue" -> queue();
            case "little" -> little();
            default -> forecast();
        }
    }

    /** M/M/c FIFO simulation: c workers, mean service 20 ms; sweep the utilization; report wait percentiles. */
    static void queue() {
        int c = 4; double meanService = 0.020; int n = 400_000;
        System.out.printf(Locale.ROOT, "M/M/%d queue, mean service %.0f ms, %,d requests per row, seeded. MODEL.%n", c, meanService * 1000, n);
        System.out.println("utilization | p50 latency | p95 latency | p99 latency | max latency | mean wait in queue");
        for (double rho : new double[]{0.30, 0.50, 0.60, 0.70, 0.80, 0.90, 0.95, 0.98}) {
            Random r = new Random(11);
            double lambda = rho * c / meanService;
            double[] free = new double[c]; // time each worker becomes free
            double t = 0; double[] lat = new double[n]; double waitSum = 0;
            for (int i = 0; i < n; i++) {
                t += -Math.log(1 - r.nextDouble()) / lambda;
                int k = 0; for (int j = 1; j < c; j++) if (free[j] < free[k]) k = j;
                double start = Math.max(t, free[k]);
                double service = -Math.log(1 - r.nextDouble()) * meanService;
                free[k] = start + service;
                lat[i] = free[k] - t; waitSum += start - t;
            }
            Arrays.sort(lat);
            System.out.printf(Locale.ROOT, "%10.0f%% | %8.0f ms | %8.0f ms | %8.0f ms | %8.0f ms | %8.1f ms%n", rho * 100,
                    lat[n / 2] * 1000, lat[(int) (n * 0.95)] * 1000, lat[(int) (n * 0.99)] * 1000, lat[n - 1] * 1000, waitSum / n * 1000);
        }
        System.out.println("\nThe curve is not a line: from 80% to 95% the p99 grows ~3x (140 -> 400 ms) and the maximum latency doubles. 'We have 20% spare' is not 'we have headroom'.");
    }

    static void little() {
        System.out.println("Little's law: L = lambda x W  (items in the system = arrival rate x time each spends inside)\n");
        double[][] cases = {{50, 0.040}, {50, 0.200}, {50, 3.000}, {200, 0.040}};
        System.out.println("rate (req/s) | time inside W | concurrent L (threads/connections busy)");
        for (double[] cs : cases) System.out.printf(Locale.ROOT, "%12.0f | %10.0f ms | %.1f%n", cs[0], cs[1] * 1000, cs[0] * cs[1]);
        System.out.println("\nThe ChaosLab case: 40 req/s with notifications at 3 s -> L = 120 threads wanted; the pool had 16.");
        System.out.println("With a 200 ms timeout: L = 40 x 0.213 = 8.5 threads: fits; at 100 req/s: 21 > 16: it does not (measured in S4.04).\n");
        System.out.println("Connections: per-task pool P, tasks N, deploy factor f -> N x f x P must stay under max_connections (decisions 171/181).");
        int p = 10, maxC = 100, reserve = 10;
        System.out.printf(Locale.ROOT, "premise max_connections=%d, reserve=%d: largest N with f=2: %d tasks%n", maxC, reserve, (maxC - reserve) / (2 * p));
        System.out.println("=> the database, not the CPU, sets the real ceiling of the autoscaler (decision 152: max 6 tasks); at 6 tasks and f=2 it is already over.");
    }

    static void forecast() {
        // synthetic disk usage (GB): 400 GB base, +0.9 GB/day trend, weekly wiggle, a one-off +25 GB bulk load on day 40, noise
        Random r = new Random(5);
        int days = 90; double[] gb = new double[days];
        for (int d = 0; d < days; d++) gb[d] = 400 + 0.9 * d + 1.5 * Math.sin(2 * Math.PI * d / 7) + (d >= 40 ? 25 : 0) + r.nextGaussian() * 0.5;
        double limit = 600;
        System.out.printf(Locale.ROOT, "synthetic disk usage: day 0 = %.0f GB, day %d = %.0f GB; limit = %.0f GB (PREMISE: volume size)%n", gb[0], days - 1, gb[days - 1], limit);
        System.out.println("true trend: +0.9 GB/day (+25 GB one-off bulk load on day 40, the kind of step a short window misreads)\n");
        System.out.println("window used for the fit        | slope GB/day | days until the limit (from day 89)");
        int[][] w = {{60, 89}, {75, 89}, {82, 89}, {86, 89}, {0, 89}, {41, 89}};
        String[] names = {"days 60-89 (30 d, after the step)", "days 75-89 (15 d)", "days 82-89 (7 d)", "days 86-89 (4 d, half a week)", "days 0-89 (whole, includes the step)", "days 41-89 (49 d, after the step)"};
        for (int i = 0; i < w.length; i++) {
            double[] f = fit(gb, w[i][0], w[i][1]);
            double dl = f[0] > 0 ? (limit - gb[days - 1]) / f[0] : Double.POSITIVE_INFINITY;
            System.out.printf(Locale.ROOT, "%-36s | %12.2f | %s%n", names[i], f[0], Double.isInfinite(dl) ? "never (negative slope: WRONG)" : String.format(Locale.ROOT, "%.0f", dl));
        }
        System.out.printf(Locale.ROOT, "true answer (known by construction): %.0f days%n", (limit - gb[days - 1]) / 0.9);
        System.out.println("\nPrometheus predict_linear(v[range], secs) does exactly this least-squares line over `range`: the RANGE is the whole decision.");
        System.out.println("A window that contains the step sees a slope that was never there; a window shorter than the weekly cycle sees the wiggle as trend.");
        System.out.println("\nWAL retained by a stuck replication slot (S3.06 measured ~375 MB per 1M updated rows):");
        double freeGb = 120, writeMbPerH = 3_000;  // PREMISES: free disk and WAL generated per hour at peak
        System.out.printf(Locale.ROOT, "free %.0f GB, WAL %.1f GB/h -> disk full in %.1f h if a slot stops consuming; max_slot_wal_keep_size must stay well below %.0f GB%n",
                freeGb, writeMbPerH / 1000, freeGb * 1000 / writeMbPerH, freeGb);
    }

    /** least squares slope/intercept for y[from..to] against day index. */
    static double[] fit(double[] y, int from, int to) {
        int n = to - from + 1; double sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (int i = from; i <= to; i++) { sx += i; sy += y[i]; sxx += (double) i * i; sxy += i * y[i]; }
        double slope = (n * sxy - sx * sy) / (n * sxx - sx * sx);
        return new double[]{slope, (sy - slope * sx) / n};
    }
}
