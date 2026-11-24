import java.util.*;

/**
 * S4.01 — SLI, SLO, error budget and burn-rate alerts (MODEL: synthetic traffic, not PayFlow).
 * Run: java SloLab.java budget | java SloLab.java alerts
 */
public class SloLab {
    static final int DAY = 24 * 60;

    public static void main(String[] a) {
        String mode = a.length > 0 ? a[0] : "budget";
        if (mode.equals("budget")) budget(); else alerts();
    }

    // ---------- part 1: arithmetic of the budget ----------
    static void budget() {
        System.out.println("== error budget of a 30-day window (time view) ==");
        double[] slos = {99.0, 99.5, 99.9, 99.95, 99.99};
        for (double s : slos) {
            double min = 30 * DAY * (1 - s / 100.0);
            System.out.printf(Locale.ROOT, "SLO %6.2f%% -> budget %8.2f min (%s)%n", s, min, human(min));
        }
        double rps = 5.0; // PREMISE: 5 requests per second on POST /transfers
        long total = (long) (rps * 30 * DAY * 60);
        System.out.printf(Locale.ROOT, "%n== request view (premise: %.0f req/s) ==%n", rps);
        System.out.printf(Locale.ROOT, "requests in 30 days: %,d%n", total);
        for (double s : new double[]{99.9, 99.99}) {
            System.out.printf(Locale.ROOT, "SLO %.2f%% -> %,d failed requests allowed%n", s, Math.round(total * (1 - s / 100)));
        }
        System.out.println("\n== burn rate: how long the budget lasts (SLO 99.9%, window 30 d) ==");
        for (double br : new double[]{1, 2, 6, 14.4, 36, 100, 1000}) {
            double hours = 30 * 24 / br;
            System.out.printf(Locale.ROOT, "burn %6.1fx -> budget gone in %8.2f h (%s) | error ratio %.4f%%%n",
                    br, hours, human(hours * 60), br * 0.1);
        }
        System.out.println("\n== budget consumed by a burn rate over a window (SLO 99.9%, 30 d) ==");
        System.out.println("consumed = burn x window / 30 d");
        double[][] rows = {{14.4, 1}, {6, 6}, {1, 72}};
        for (double[] r : rows) {
            System.out.printf(Locale.ROOT, "burn %5.1fx for %5.1f h -> %5.2f%% of the budget%n", r[0], r[1], 100 * r[0] * r[1] / (30 * 24));
        }
        System.out.println("\n== availability of a chain (serial) vs redundancy (parallel) ==");
        double c = 0.999;
        System.out.printf(Locale.ROOT, "3 components in series, each 99.9%%: %.4f%%%n", 100 * Math.pow(c, 3));
        System.out.printf(Locale.ROOT, "5 components in series, each 99.9%%: %.4f%%%n", 100 * Math.pow(c, 5));
        System.out.printf(Locale.ROOT, "2 independent copies in parallel, each 99.9%%: %.5f%%%n", 100 * (1 - Math.pow(1 - c, 2)));
    }

    static String human(double minutes) {
        if (minutes >= 60 * 24) return String.format(Locale.ROOT, "%.1f days", minutes / DAY);
        if (minutes >= 60) return String.format(Locale.ROOT, "%.1f h", minutes / 60);
        return String.format(Locale.ROOT, "%.1f min", minutes);
    }

    // ---------- part 2: alert strategies on synthetic incidents ----------
    record Scenario(String name, int lengthMin, double baseErr, List<int[]> bursts /* start,len */, double burstErr) {}

    /** per-minute buckets: requests and errors (binomial draw, seeded). */
    static int[][] simulate(Scenario sc, int perMin, long seed) {
        Random r = new Random(seed);
        int[][] b = new int[sc.lengthMin][2];
        for (int m = 0; m < sc.lengthMin; m++) {
            double p = sc.baseErr;
            for (int[] bu : sc.bursts) if (m >= bu[0] && m < bu[0] + bu[1]) p = sc.burstErr;
            int n = perMin, e = 0;
            for (int i = 0; i < n; i++) if (r.nextDouble() < p) e++;
            b[m][0] = n; b[m][1] = e;
        }
        return b;
    }

    static double ratio(int[][] b, int end, int win) {
        long n = 0, e = 0;
        for (int m = Math.max(0, end - win + 1); m <= end; m++) { n += b[m][0]; e += b[m][1]; }
        return n == 0 ? 0 : (double) e / n;
    }

    static void alerts() {
        final double slo = 0.999, budget = 1 - slo;
        final int perMin = 300; // PREMISE: 5 req/s
        List<Scenario> list = List.of(
            new Scenario("A  total outage 10 min (100% errors)", 3 * DAY, 0.0002, List.of(new int[]{1000, 10}), 1.0),
            new Scenario("B  slow burn: 0.8% errors for 12 h", 5 * DAY, 0.0002, List.of(new int[]{2000, 12 * 60}), 0.008),
            new Scenario("C  blips: 1 min of 100% errors every 30 min for 6 h", 2 * DAY, 0.0002, blips(1000, 6 * 60, 30, 1), 1.0),
            new Scenario("D  mild: 0.15% errors for 4 days (burn 1.5x)", 6 * DAY, 0.0002, List.of(new int[]{500, 4 * DAY}), 0.0015),
            new Scenario("E  healthy: base 0.02% only (no incident)", 3 * DAY, 0.0002, List.of(), 0.0002));
        System.out.println("PREMISES: SLO 99.9%, 5 req/s, base error 0.02%, seeded random (reproducible). MODEL, not PayFlow.");
        System.out.println("strategy N = naive: page when 5m error ratio > 2% for 5 min (the P6.04 rule, no SLO)");
        System.out.println("strategy M = multi-window burn rate (SRE Workbook, table 5-8): page 1h&5m>14.4x or 6h&30m>6x; ticket 3d&6h>1x\n");
        System.out.printf("%-58s | %-34s | %-34s%n", "scenario", "N: first page (min after start)", "M: first page / ticket");
        for (Scenario sc : list) {
            int[][] b = simulate(sc, perMin, 42);
            int start = sc.bursts.isEmpty() ? -1 : sc.bursts.get(0)[0];
            int nPage = -1, nCount = 0, mPage = -1, mCount = 0, mTicket = -1, mTCount = 0;
            boolean nOn = false, mOn = false, tOn = false;
            for (int m = 0; m < sc.lengthMin; m++) {
                boolean n = m >= 4 && allAbove(b, m, 5, 0.02);
                boolean pg = (ratio(b, m, 60) > 14.4 * budget && ratio(b, m, 5) > 14.4 * budget)
                          || (ratio(b, m, 360) > 6 * budget && ratio(b, m, 30) > 6 * budget);
                boolean tk = m >= 3 * DAY && ratio(b, m, 3 * DAY) > 1 * budget && ratio(b, m, 360) > 1 * budget;
                if (n && !nOn) { nCount++; if (nPage < 0) nPage = m; } nOn = n;
                if (pg && !mOn) { mCount++; if (mPage < 0) mPage = m; } mOn = pg;
                if (tk && !tOn) { mTCount++; if (mTicket < 0) mTicket = m; } tOn = tk;
            }
            long totErr = 0, tot = 0; for (int[] x : b) { tot += x[0]; totErr += x[1]; }
            double consumed = 100.0 * totErr / (budget * 30.0 * DAY * perMin);
            System.out.printf(Locale.ROOT, "%-58s | %-34s | %-34s%n", sc.name,
                    fmt(nPage, start, nCount), fmt2(mPage, mTicket, start, mCount, mTCount));
            System.out.printf(Locale.ROOT, "%-58s |   budget (30 d) consumed in this run: %.1f%%%n", "", consumed);
        }
    }

    static boolean allAbove(int[][] b, int end, int win, double thr) {
        for (int m = end - win + 1; m <= end; m++) if ((double) b[m][1] / b[m][0] <= thr) return false;
        return true;
    }

    static List<int[]> blips(int start, int total, int every, int len) {
        List<int[]> l = new ArrayList<>();
        for (int t = 0; t < total; t += every) l.add(new int[]{start + t, len});
        return l;
    }

    static String fmt(int page, int start, int count) {
        if (page < 0) return "never (0 pages)";
        return String.format(Locale.ROOT, "+%d min, %d page(s)", page - start, count);
    }

    static String fmt2(int page, int ticket, int start, int pages, int tickets) {
        String p = page < 0 ? "no page" : String.format(Locale.ROOT, "page +%d min (%dx)", page - start, pages);
        String t = ticket < 0 ? "no ticket" : String.format(Locale.ROOT, "ticket +%.1f h (%dx)", (ticket - start) / 60.0, tickets);
        return p + " / " + t;
    }
}
