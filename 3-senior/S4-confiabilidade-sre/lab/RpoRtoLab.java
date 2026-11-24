import java.util.*;

/**
 * S4.05 — RPO and RTO as numbers (MODEL: simulated commit stream and a ship-the-log mechanism, not PostgreSQL).
 * Run: java RpoRtoLab.java rpo | rto
 */
public class RpoRtoLab {
    public static void main(String[] a) {
        if (a.length > 0 && a[0].equals("rto")) rto(); else rpo();
    }

    /** commits arrive as a Poisson process; the log is shipped every `shipEverySec`; the disaster hits at a random instant. */
    static void rpo() {
        double commitsPerSec = 5;           // PREMISE: transfers (each one commits)
        int disasters = 20000;
        Random r = new Random(7);
        System.out.println("MODEL: " + commitsPerSec + " commits/s (premise), " + disasters + " random disasters per row, seeded.");
        System.out.println("RPO = how much committed data is LOST. A row says how often the log is shipped to a place that survives the disaster.\n");
        System.out.printf("%-44s | %12s | %12s | %12s%n", "mechanism (ship interval)", "mean lost tx", "p99 lost tx", "max lost tx");
        Object[][] mech = {
            {"synchronous (every commit waits for the copy)", 0.0},
            {"async stream, lag ~ 0.003 s (the P9.08 measure)", 0.003},
            {"async stream, lag ~ 1 s", 1.0},
            {"log shipped every 60 s (archive_timeout = 60)", 60.0},
            {"log shipped every 300 s (RDS: ~5 min, P9.08)", 300.0},
            {"nightly dump (24 h)", 86400.0}};
        for (Object[] m : mech) {
            double interval = (Double) m[1];
            int[] lost = new int[disasters];
            for (int i = 0; i < disasters; i++) {
                double exposed = interval == 0 ? 0 : (interval < 1 ? interval : r.nextDouble() * interval);
                // for tiny lags the exposure is the lag itself; for periodic shipping it is the time since the last shipment
                lost[i] = poisson(r, commitsPerSec * exposed);
            }
            Arrays.sort(lost);
            double mean = Arrays.stream(lost).average().orElse(0);
            System.out.printf(Locale.ROOT, "%-44s | %12.1f | %12d | %12d%n", m[0], mean, lost[(int) (disasters * 0.99) - 1], lost[disasters - 1]);
        }
        System.out.println("\nThe worst case of periodic shipping is the interval itself (RPO = 300 s -> up to 1500 transfers at 5/s).");
        System.out.println("Synchronous copy gives RPO 0 but every commit now pays the round trip to the other site (latency x availability trade).");
    }

    static int poisson(Random r, double lambda) {
        if (lambda <= 0) return 0;
        if (lambda > 60) return (int) Math.max(0, Math.round(lambda + Math.sqrt(lambda) * r.nextGaussian()));
        double L = Math.exp(-lambda), p = 1; int k = 0;
        do { k++; p *= r.nextDouble(); } while (p > L);
        return k - 1;
    }

    /** RTO is a SUM of steps. Every number below is a PREMISE to be replaced by the result of a restore drill. */
    static void rto() {
        System.out.println("RTO as a sum of steps (all numbers are PREMISES: replace them with a drill's measurements)\n");
        double sizeGb = 200, copyMBs = 150, replayMBs = 40, walGbSince = 6;
        String[][] steps = {
            {"detect the failure (alert + human on duty)", "5"},
            {"decide to restore and get approval", "10"},
            {"provision the new database instance", "15"},
            {String.format(Locale.ROOT, "restore base backup (%.0f GB at %.0f MB/s)", sizeGb, copyMBs), String.format(Locale.ROOT, "%.1f", sizeGb * 1024 / copyMBs / 60)},
            {String.format(Locale.ROOT, "replay the WAL since the backup (%.0f GB at %.0f MB/s)", walGbSince, replayMBs), String.format(Locale.ROOT, "%.1f", walGbSince * 1024 / replayMBs / 60)},
            {"validate (invariants, row counts, a test transfer)", "10"},
            {"point the application at it (secret, DNS, deploy)", "10"}};
        double total = 0;
        for (String[] s : steps) { double v = Double.parseDouble(s[1]); total += v; System.out.printf(Locale.ROOT, "%-62s %7.1f min%n", s[0], v); }
        System.out.printf(Locale.ROOT, "%-62s %7.1f min (%.1f h)%n", "TOTAL (PITR path)", total, total / 60);
        System.out.println();
        System.out.printf(Locale.ROOT, "Same database, 10x bigger (%.0f GB): restore step becomes %.1f min -> total %.1f min (%.1f h)%n",
                sizeGb * 10, sizeGb * 10 * 1024 / copyMBs / 60, total - sizeGb * 1024 / copyMBs / 60 + sizeGb * 10 * 1024 / copyMBs / 60,
                (total - sizeGb * 1024 / copyMBs / 60 + sizeGb * 10 * 1024 / copyMBs / 60) / 60);
        System.out.println("Multi-AZ failover path instead: detect 1 + failover 1-2 (P9.08: ~60-120 s) + JVM DNS cache 0.5 (30 s measured, P9.08) = ~3-4 min.");
        System.out.println("The difference is why the two exist: failover handles 'a machine/AZ died'; restore handles 'the DATA is wrong' (a bad DELETE replicates instantly).");
    }
}
