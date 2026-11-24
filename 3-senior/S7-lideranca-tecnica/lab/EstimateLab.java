// EstimateLab.java — S7.03: estimates as DISTRIBUTIONS, forecasts from throughput, and a risk register.
//   java EstimateLab.java tasks      -> three-point estimates of the recurring-transfers design, Monte Carlo
//   java EstimateLab.java risks      -> risk register: exposure = probability x impact; is the spike worth it?
//   java EstimateLab.java forecast   -> "when will these 30 items be done?" from weekly throughput history
// ALL NUMBERS ARE PREMISES (invented and declared). Fixed seeds: the output is deterministic.

import java.util.*;

public class EstimateLab {

    record Task(String id, String name, double optimistic, double likely, double pessimistic) {
        double pert() { return (optimistic + 4 * likely + pessimistic) / 6; }
        double sample(Random r) {                       // triangular(o, m, p) by inverse transform
            double u = r.nextDouble(), o = optimistic, m = likely, p = pessimistic;
            double c = (m - o) / (p - o);
            return u < c ? o + Math.sqrt(u * (p - o) * (m - o)) : p - Math.sqrt((1 - u) * (p - o) * (p - m));
        }
    }

    record Risk(String id, String name, double probability, double impactDays, String mitigation,
                double mitigationCost, double probabilityAfter, String owner) {
        double exposure() { return probability * impactDays; }
        double exposureAfter() { return probabilityAfter * impactDays; }
    }

    // Work in ideal days (focused work of ONE person), from the design doc. Premises.
    static final List<Task> TASKS = List.of(
            new Task("T1", "V26 + repository + Testcontainers tests", 1, 1.5, 3),
            new Task("T2", "recurrence rule + Clock + tests (spike done)", 0.5, 1, 2),
            new Task("T3", "scheduler: ShedLock, SKIP LOCKED, 2 workers, flag", 1, 2, 4),
            new Task("T4", "execution: transaction, idempotency, business failure", 2, 3, 8),
            new Task("T5", "API: POST/GET/DELETE, policy, MFA, Problem Details", 2, 3, 6),
            new Task("T6", "consent + audit trail (V25)", 1, 1.5, 3),
            new Task("T7", "events + notification consumer", 1, 2, 4),
            new Task("T8", "metrics, alerts, runbook, promtool tests", 1, 2, 4),
            new Task("T9", "load test of the 06:00 peak (k6)", 1, 1.5, 4),
            new Task("T10", "security review + threats.csv", 0.5, 1, 3),
            new Task("T11", "rollout: internal accounts, reconciliation", 1, 2, 3));

    static final List<Risk> RISKS = List.of(
            new Risk("R1", "business exception leaves the tx rollback-only", 0.30, 3, "test first (half a day)", 0.5, 0.05, "tech lead"),
            new Risk("R2", "transfer does not return the existing transfer for a repeated key", 0.40, 4, "read the code + one test", 0.25, 0.10, "tech lead"),
            new Risk("R3", "security review changes the consent flow", 0.25, 2, "review the STRIDE draft early", 0.5, 0.15, "security"),
            new Risk("R4", "waiting for the notifications service team (other deploy)", 0.30, 3, "agree the event contract in week 1", 0.25, 0.15, "tech lead"));

    static final int RUNS = 100_000;
    static final double FOCUS = 0.6;      // premise: 60% of a day is focused work (meetings, support, on-call)

    public static void main(String[] args) {
        switch (args.length == 0 ? "tasks" : args[0]) {
            case "tasks" -> tasks();
            case "risks" -> risks();
            case "forecast" -> forecast();
            default -> System.out.println("usage: tasks | risks | forecast");
        }
    }

    static double percentile(double[] sorted, double p) {
        return sorted[(int) Math.ceil(p * sorted.length) - 1];
    }

    static double[] simulate(boolean withRisks, boolean mitigated, long seed) {
        Random r = new Random(seed);
        double[] totals = new double[RUNS];
        for (int i = 0; i < RUNS; i++) {
            double t = 0;
            for (Task task : TASKS) t += task.sample(r);
            if (withRisks) {
                for (Risk risk : RISKS) {
                    double p = mitigated ? risk.probabilityAfter() : risk.probability();
                    if (r.nextDouble() < p) t += risk.impactDays();
                }
                if (mitigated) for (Risk risk : RISKS) t += risk.mitigationCost();
            }
            totals[i] = t;
        }
        Arrays.sort(totals);
        return totals;
    }

    static void tasks() {
        double likely = 0, pert = 0, optimistic = 0, pessimistic = 0;
        System.out.println("id  | task                                                    |   o |   m |   p | PERT");
        for (Task t : TASKS) {
            System.out.printf("%-3s | %-55s | %3.1f | %3.1f | %3.1f | %4.2f%n", t.id(), t.name(), t.optimistic(), t.likely(), t.pessimistic(), t.pert());
            likely += t.likely(); pert += t.pert(); optimistic += t.optimistic(); pessimistic += t.pessimistic();
        }
        System.out.printf("Sum: optimistic %.1f | most likely %.1f | PERT means %.1f | pessimistic %.1f (ideal days)%n", optimistic, likely, pert, pessimistic);

        double[] base = simulate(false, false, 7);
        final double sumLikely = likely;
        long hit = Arrays.stream(base).filter(x -> x <= sumLikely).count();
        System.out.printf("%nMonte Carlo, %,d runs, tasks only (triangular, independent, seed 7):%n", RUNS);
        System.out.printf("  p50 %.1f | p85 %.1f | p95 %.1f ideal days; P(total <= sum of most likely %.1f) = %.1f%%%n",
                percentile(base, .50), percentile(base, .85), percentile(base, .95), likely, 100.0 * hit / RUNS);

        double[] withRisks = simulate(true, false, 7);
        double[] mitigated = simulate(true, true, 7);
        System.out.printf("With the 4 risks of the register (not mitigated): p50 %.1f | p85 %.1f | p95 %.1f%n",
                percentile(withRisks, .50), percentile(withRisks, .85), percentile(withRisks, .95));
        System.out.printf("With mitigations paid up front:                 p50 %.1f | p85 %.1f | p95 %.1f%n",
                percentile(mitigated, .50), percentile(mitigated, .85), percentile(mitigated, .95));
        double p85 = percentile(mitigated, .85);
        System.out.printf("%nCalendar (focus factor %.0f%%, 1 person, 5-day weeks): p85 %.1f ideal days -> %.1f working days -> %.1f weeks%n",
                FOCUS * 100, p85, p85 / FOCUS, p85 / FOCUS / 5);
        System.out.printf("Two people do not halve it: T3 before T4 and T5 before T6 are sequential; review and rollout are waits (premise).%n");
    }

    static void risks() {
        System.out.println("id | risk                                                              |  P   | impact | exposure | mitigation                         | cost   | P after | exp. after | owner");
        List<Risk> sorted = new ArrayList<>(RISKS);
        sorted.sort(Comparator.comparingDouble(Risk::exposure).reversed());
        double total = 0, totalAfter = 0, cost = 0;
        for (Risk r : sorted) {
            System.out.printf("%-2s | %-65s | %.2f | %4.1f d | %6.2f d | %-34s | %.2f d |  %.2f   | %6.2f d   | %s%n",
                    r.id(), r.name(), r.probability(), r.impactDays(), r.exposure(), r.mitigation(), r.mitigationCost(),
                    r.probabilityAfter(), r.exposureAfter(), r.owner());
            total += r.exposure(); totalAfter += r.exposureAfter(); cost += r.mitigationCost();
        }
        System.out.printf("Total exposure %.2f d -> %.2f d after mitigations that cost %.2f d (net gain %.2f d)%n",
                total, totalAfter, cost, total - totalAfter - cost);
        for (Risk r : sorted) {
            double gain = r.exposure() - r.exposureAfter() - r.mitigationCost();
            System.out.printf("  %s: mitigation %s (expected gain %+.2f d)%n", r.id(), gain > 0 ? "PAYS" : "does not pay", gain);
        }
        double[] withRisks = simulate(true, false, 7);
        long noneHit = 0;
        Random r = new Random(11);
        for (int i = 0; i < RUNS; i++) {
            boolean any = false;
            for (Risk risk : RISKS) any |= r.nextDouble() < risk.probability();
            if (!any) noneHit++;
        }
        System.out.printf("Chance that NONE of the 4 risks happens: %.1f%% (simulated; formula %.1f%%)%n",
                100.0 * noneHit / RUNS, 100 * RISKS.stream().mapToDouble(k -> 1 - k.probability()).reduce(1, (a, b) -> a * b));
        System.out.printf("p95 - p50 with risks: %.1f d (the tail that a single number hides)%n",
                percentile(withRisks, .95) - percentile(withRisks, .50));
    }

    static void forecast() {
        // Weekly throughput of the team in the last 12 weeks (items finished per week). SYNTHETIC history.
        int[] history = {3, 5, 2, 6, 4, 4, 1, 5, 3, 6, 4, 2};
        int backlog = 30;
        double avg = Arrays.stream(history).average().orElseThrow();
        System.out.printf("History (items/week, synthetic): %s, average %.2f%n", Arrays.toString(history), avg);
        System.out.printf("Average-based answer: %d / %.2f = %.1f weeks%n", backlog, avg, backlog / avg);
        for (double growth : new double[]{0.0, 0.2}) {
            Random r = new Random(3);
            double[] weeks = new double[RUNS];
            int target = (int) Math.round(backlog * (1 + growth));
            for (int i = 0; i < RUNS; i++) {
                int done = 0, w = 0;
                while (done < target) { done += history[r.nextInt(history.length)]; w++; }
                weeks[i] = w;
            }
            Arrays.sort(weeks);
            final double avgWeeks = Math.ceil(backlog / avg);
            long late = Arrays.stream(weeks).filter(x -> x > avgWeeks).count();
            System.out.printf("Monte Carlo (resampling weeks, %,d runs, backlog %d%s): p50 %.0f | p85 %.0f | p95 %.0f weeks; P(> %.0f weeks) = %.1f%%%n",
                    RUNS, target, growth > 0 ? " = 30 + 20% split/discovered" : "", percentile(weeks, .50), percentile(weeks, .85),
                    percentile(weeks, .95), avgWeeks, 100.0 * late / RUNS);
        }
    }
}
