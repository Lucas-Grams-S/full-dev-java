import java.util.*;

/**
 * S4.06 — what survives the loss of one availability zone (MODEL: arithmetic + a small simulation, not AWS).
 * PREMISES come from the PayFlow decisions: tasks 2-6 (decision 165), target CPU 60%, pool 10 per task,
 * max_connections is a PREMISE (set below), maximum_percent 200 on deploys (decision 171).
 * Run: java AzLab.java capacity | chain
 */
public class AzLab {
    public static void main(String[] a) {
        if (a.length > 0 && a[0].equals("chain")) chain(); else capacity();
    }

    static void capacity() {
        System.out.println("== 1. capacity left after losing one of 2 AZs (CPU target 60% before the loss) ==");
        System.out.println("tasks | per AZ | after loss: tasks | CPU util after loss | verdict");
        for (int tasks : new int[]{2, 3, 4, 6}) {
            int perAz = tasks / 2;                // tasks spread evenly over 2 AZs (even numbers only; odd -> worst AZ has the larger half)
            int worstAz = (tasks + 1) / 2;
            int left = tasks - worstAz;           // the AZ with MORE tasks is the one that dies (worst case)
            double util = left == 0 ? Double.POSITIVE_INFINITY : 0.60 * tasks / left;
            String v = util <= 0.85 ? "survives" : util <= 1.0 ? "survives but hot (autoscaling needs minutes)" : "OVERLOADED until it scales";
            System.out.printf(Locale.ROOT, "%5d | %6d | %17d | %17.0f%% | %s%n", tasks, perAz, left, util * 100, v);
        }
        System.out.println("\nRule: to survive losing 1 of 2 AZs at <= 85% CPU you need  tasks_before * 0.60 / (tasks_before/2) <= 0.85  ->  60%*2 = 120% > 85%: ALWAYS fails with 2 AZs at 60% target.");
        System.out.println("=> with 2 AZs the steady CPU must be <= 42% (0.85/2) to ride out an AZ loss WITHOUT scaling; or use 3 AZs: 60% * 3/2 = 90%.");
        System.out.println("   with 3 AZs and equal spread: after the loss util = 60% * 3/2 = 90%  (still hot); steady <= 56% keeps it <= 85%.\n");

        System.out.println("== 2. the autoscaler needs time: how long is the service hot after the loss? ==");
        int steadyTasks = 4; double load = 0.60 * steadyTasks; // load measured in "task-equivalents at 100% CPU"
        int tasks = 2; // after losing one of 2 AZs with 4 tasks
        double step = 2; int max = 6;                          // scale-out adds 2 tasks per step, up to 6 (decision 165)
        double t = 0; double detectMin = 3.0;                   // PREMISE: metric period + alarm evaluation + warm-up
        System.out.printf(Locale.ROOT, "t=%.0f min: AZ lost, %d tasks left, CPU %.0f%%%n", t, tasks, 100 * load / tasks);
        t = detectMin; tasks = Math.min(max, tasks + (int) step);
        System.out.printf(Locale.ROOT, "t=%.0f min: first scale-out lands, %d tasks, CPU %.0f%%%n", t, tasks, 100 * load / tasks);
        System.out.println("(the minutes between the loss and the scale-out are spent above 100% -> queueing, timeouts, 5xx: the budget is spent HERE)");

        System.out.println("\n== 3. the connection count after the loss and during a deploy (decisions 171/181) ==");
        int pool = 10, maxConn = 100, reserve = 10;   // PREMISE: max_connections 100, 10 reserved for admin + Flyway
        for (int n : new int[]{4, 6}) {
            for (int pct : new int[]{100, 200}) {
                int conns = (int) Math.ceil(n * (pct / 100.0)) * pool;
                System.out.printf(Locale.ROOT, "%d tasks, maximum_percent=%d -> up to %d connections (limit %d) %s%n", n, pct, conns, maxConn - reserve,
                        conns <= maxConn - reserve ? "ok" : "EXCEEDS");
            }
        }
        System.out.println("A deploy DURING the recovery from an AZ loss (6 tasks x 200%) = 120 connections > 90: the safe rule is 'no deploy while degraded'.");
    }

    /** availability of a request that needs every component (serial) with and without redundancy. PREMISED per-component values. */
    static void chain() {
        System.out.println("PREMISES (not AWS numbers): each component is available a fraction of the time, failures independent.\n");
        double alb = 0.9995, app = 0.999, db = 0.999, queue = 0.999;
        System.out.printf(Locale.ROOT, "single AZ chain  ALB x app x db           = %.4f%%%n", 100 * alb * app * db);
        double app2 = 1 - Math.pow(1 - app, 2), db2 = 1 - Math.pow(1 - db, 2);
        System.out.printf(Locale.ROOT, "2 AZs: app in 2 AZs x db with standby      = %.4f%%   (ALB assumed already multi-AZ)%n", 100 * alb * app2 * db2);
        System.out.printf(Locale.ROOT, "...and the chain still counts a 'shared' dependency (e.g. the same Region/DNS/deploy): x 0.9995 -> %.4f%%%n", 100 * alb * app2 * db2 * 0.9995);
        double hardDep = alb * app2 * db2 * queue;
        System.out.printf(Locale.ROOT, "a HARD dependency on notifications (99.9%%) multiplies the ceiling:      %.4f%%%n", 100 * hardDep);
        System.out.printf(Locale.ROOT, "with notifications as a SOFT dependency (P5: fallback/queue):           %.4f%%%n", 100 * alb * app2 * db2);
        System.out.println("\nCorrelated failures: redundancy only multiplies if failures are INDEPENDENT. Same bad deploy on both copies = one failure.");
        double minutes30 = 30 * 24 * 60;
        System.out.printf(Locale.ROOT, "downtime per 30 days: single AZ %.1f min | multi-AZ %.1f min (model)%n", minutes30 * (1 - alb * app * db), minutes30 * (1 - alb * app2 * db2));
    }
}
