// FlakyLab.java — S7.05: why "just re-run it" is a team-level decision.
// A flaky test fails at random with probability p per run, independently. With N such tests in the suite:
//   P(suite red on a correct change) = 1 - (1 - p)^N        (formula, checked by simulation)
//   with an automatic retry of the failed tests: each flaky test now needs 2 failures in a row -> p^2
// And the price of the retry: a REAL intermittent bug that fails 1 run in 10 also hides behind it.
//   java FlakyLab.java
// Seeded: deterministic.

import java.util.Random;

public class FlakyLab {

    public static void main(String[] args) {
        Random r = new Random(5);
        int runs = 20_000;
        System.out.println("flaky tests | p per run | P(suite red) formula | simulated | with 1 retry | wasted builds/day (40 builds)");
        for (int n : new int[]{5, 20, 50}) {
            for (double p : new double[]{0.001, 0.005, 0.01}) {
                double formula = 1 - Math.pow(1 - p, n);
                int red = 0;
                for (int k = 0; k < runs; k++) {
                    boolean failed = false;
                    for (int t = 0; t < n && !failed; t++) failed = r.nextDouble() < p;
                    if (failed) red++;
                }
                double withRetry = 1 - Math.pow(1 - p * p, n);
                System.out.printf("%11d | %8.1f%% | %19.1f%% | %8.1f%% | %11.3f%% | %.1f%n",
                        n, p * 100, formula * 100, 100.0 * red / runs, withRetry * 100, 40 * formula);
            }
        }
        double bug = 0.10;
        System.out.printf("%nA real intermittent bug failing %.0f%% of runs: seen as red %.0f%% of the time without retry, %.0f%% with 1 retry, %.1f%% with 2 retries.%n",
                bug * 100, bug * 100, bug * bug * 100, Math.pow(bug, 3) * 100);
    }
}
