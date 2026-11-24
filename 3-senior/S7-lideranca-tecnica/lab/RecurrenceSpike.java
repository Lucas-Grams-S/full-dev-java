// RecurrenceSpike.java — S7.02: a SPIKE (throwaway experiment) that answers the risky questions
// of the "recurring transfers" design doc BEFORE the estimate: dates, time zones, burst and keys.
//   java RecurrenceSpike.java dates      -> "day 31" with three algorithms
//   java RecurrenceSpike.java zones      -> which "today" the scheduler sees (UTC x America/Sao_Paulo)
//   java RecurrenceSpike.java burst      -> how long the 06:00 batch takes (PREMISES, not PayFlow measurements)
//   java RecurrenceSpike.java keys       -> idempotency key per occurrence x per attempt under crash-and-retry
// Deterministic (fixed dates and seeds).

import java.time.*;
import java.util.*;

public class RecurrenceSpike {

    public static void main(String[] args) {
        switch (args.length == 0 ? "dates" : args[0]) {
            case "dates" -> dates();
            case "zones" -> zones();
            case "burst" -> burst();
            case "keys" -> keys();
            default -> System.out.println("usage: dates | zones | burst | keys");
        }
    }

    /** The rule: day D of every month; if the month is shorter, the last day of the month. */
    static LocalDate nextFromRule(int dayOfMonth, YearMonth month) {
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
    }

    static void dates() {
        int day = 31;
        LocalDate first = LocalDate.of(2027, 1, 31);
        LocalDate chained = first, plus30 = first;
        System.out.println("Schedule: day 31, first occurrence 2027-01-31");
        System.out.println("n  | rule (YearMonth)  | chained plusMonths(1) | plusDays(30)");
        for (int n = 0; n < 13; n++) {
            LocalDate rule = nextFromRule(day, YearMonth.from(first).plusMonths(n));
            System.out.printf("%-2d | %s %-6s | %s %-10s | %s%n", n, rule, rule.getDayOfWeek().toString().substring(0, 3),
                    chained, chained.equals(rule) ? "" : "WRONG", plus30.equals(rule) ? plus30 : plus30 + " WRONG");
            chained = chained.plusMonths(1);   // Jan 31 -> Feb 28 -> Mar 28 -> ... never comes back to 31
            plus30 = plus30.plusDays(30);
        }
        System.out.println("Leap year: rule for day 29 in Feb 2028 = " + nextFromRule(29, YearMonth.of(2028, 2))
                + "; in Feb 2027 = " + nextFromRule(29, YearMonth.of(2027, 2)));
    }

    static void zones() {
        ZoneId sp = ZoneId.of("America/Sao_Paulo");
        System.out.println("tzdata version: " + java.time.zone.ZoneRulesProvider.getVersions("America/Sao_Paulo").lastKey());
        for (LocalDate d : List.of(LocalDate.of(2027, 1, 5), LocalDate.of(2027, 7, 5))) {
            ZonedDateTime due = d.atTime(6, 0).atZone(sp);
            System.out.printf("due %s 06:00 %s = %s (offset %s)%n", d, sp, due.toInstant(), due.getOffset());
        }
        System.out.println("DST in Sao Paulo between 2027 and 2030? " + (sp.getRules().nextTransition(Instant.parse("2027-01-01T00:00:00Z")) != null
                && sp.getRules().nextTransition(Instant.parse("2027-01-01T00:00:00Z")).getInstant().isBefore(Instant.parse("2030-01-01T00:00:00Z"))
                ? "yes" : "no transition found"));
        Instant now = Instant.parse("2027-02-05T01:30:00Z");   // job wakes up at 01:30 UTC
        LocalDate todayUtc = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate todaySp = LocalDate.ofInstant(now, sp);
        System.out.printf("job at %s: LocalDate in UTC = %s, in Sao Paulo = %s%n", now, todayUtc, todaySp);
        System.out.println("-> a scheduler that uses the server's UTC date fires the 'day 5' schedules at 22:30 of day 4 (local).");
    }

    static void burst() {
        // PREMISES (invented for the design doc, declared): 50,000 active schedules; 30% choose day 5,
        // 20% day 10, 15% day 1, the rest spread over the other 28 days; 20 ms of service per run
        // (one transfer + one run update, NOT measured on PayFlow); batch released at 06:00.
        int active = 50_000;
        double serviceMs = 20;
        int day5 = (int) Math.round(active * 0.30);
        System.out.printf("Premises: %,d active schedules, %,d due on day 5 at 06:00, %.0f ms per run%n", active, day5, serviceMs);
        System.out.println("workers | drain time | throughput (runs/s) | connections held");
        for (int workers : new int[]{1, 2, 4, 8}) {
            double seconds = day5 * serviceMs / 1000.0 / workers;
            System.out.printf("%7d | %7.1f s  | %19.0f | %d%n", workers, seconds, workers * 1000.0 / serviceMs, workers);
        }
        double sloMinutes = 60;
        System.out.printf("SLO proposal: every run processed within %.0f min of its due time -> even 1 worker fits (%.0f s)%n",
                sloMinutes, day5 * serviceMs / 1000.0);
        System.out.println("Little's law at 2 workers: L = lambda x W = 100 runs/s x 0.020 s = 2 runs in flight = 2 connections");
    }

    static void keys() {
        // A worker executes the occurrence, the transfer commits, and the worker crashes BEFORE marking the
        // run as done (probability 2%). On restart the occurrence is executed again.
        Random rnd = new Random(42);
        int occurrences = 10_000;
        Set<String> derivedKeys = new HashSet<>();   // what the idempotency table stores: one row per key
        Set<String> attemptKeys = new HashSet<>();
        int movedDerived = 0, movedPerAttempt = 0;
        for (int i = 0; i < occurrences; i++) {
            String schedule = "s" + (i % 2_000);
            String due = LocalDate.of(2027, 1, 5).plusMonths(i / 2_000).toString();
            int attempts = 1;
            while (rnd.nextDouble() < 0.02) attempts++;            // crash after commit -> retry
            for (int a = 0; a < attempts; a++) {
                String occKey = "recurring:" + schedule + ":" + due;            // derived: stable across retries
                if (derivedKeys.add(occKey)) movedDerived++;                     // known key -> existing transfer returned
                String attemptKey = occKey + "#attempt-" + a;                    // a fresh key on every attempt
                if (attemptKeys.add(attemptKey)) movedPerAttempt++;
            }
        }
        int duplicatesPerOccurrence = movedDerived - occurrences, duplicatesPerAttempt = movedPerAttempt - occurrences;
        System.out.printf("%,d occurrences, crash-after-commit probability 2%% per attempt (seed 42)%n", occurrences);
        System.out.printf("key = recurring:<schedule>:<due_date> -> duplicated transfers: %d%n", duplicatesPerOccurrence);
        System.out.printf("key = random per attempt               -> duplicated transfers: %d%n", duplicatesPerAttempt);
    }
}
