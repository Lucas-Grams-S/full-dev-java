// LiveCodingLab.java — S8.03: one live-coding problem from the PayFlow domain, solved the interview way:
// clarify -> examples -> brute force -> better solution -> test it AGAINST the brute force (differential, fixed seed) -> complexity.
// Problem: flag possibly duplicated transfers = same source, same target, same amount, at most 60 s after an EARLIER one
// (clarified: the 60 s are inclusive; "earlier" = smaller timestamp, or same timestamp and earlier in the input).
//   java LiveCodingLab.java examples -> hand-made edge cases: brute force, fast solution and 3 mutants of the fast one
//   java LiveCodingLab.java scale    -> work COUNTED (pair checks / comparator calls + map operations), not timed
//   java LiveCodingLab.java fuzz     -> 2,000 random cases (seed 8): fast vs brute force, then each mutant vs brute force

import java.math.BigDecimal;
import java.util.*;

public class LiveCodingLab {

    record Transfer(String id, String from, String to, BigDecimal amount, long at) {}

    static final long WINDOW = 60;
    static long work;   // pair checks (brute force) or comparator calls + map operations (fast)

    /** O(n^2): for each transfer, look for an earlier one with the same key inside the window. */
    static Set<String> bruteForce(List<Transfer> ts) {
        Set<String> flagged = new TreeSet<>();
        for (int i = 0; i < ts.size(); i++) {
            Transfer t = ts.get(i);
            for (int j = 0; j < ts.size(); j++) {
                if (j == i) continue;
                work++;
                Transfer e = ts.get(j);
                boolean earlier = e.at() < t.at() || (e.at() == t.at() && j < i);
                if (earlier && t.at() - e.at() <= WINDOW && e.from().equals(t.from()) && e.to().equals(t.to())
                        && e.amount().compareTo(t.amount()) == 0) {
                    flagged.add(t.id());
                    break;
                }
            }
        }
        return flagged;
    }

    record Key(String from, String to, BigDecimal amount) {}

    /** O(n log n): stable sort by time, then remember the LAST time each key was seen (the closest earlier one). */
    static Set<String> fast(List<Transfer> input, String mutant) {
        List<Transfer> ts = new ArrayList<>(input);
        if (!mutant.equals("noSort"))
            ts.sort((a, b) -> { work++; return Long.compare(a.at(), b.at()); });   // List.sort is stable: ties keep input order
        Map<Key, Long> lastSeen = new HashMap<>();
        Set<String> flagged = new TreeSet<>();
        for (Transfer t : ts) {
            // 10.0 and 10.00 are compareTo-equal but NOT equals(): normalize before using BigDecimal in a hash key
            BigDecimal amount = mutant.equals("rawBigDecimal") ? t.amount() : t.amount().stripTrailingZeros();
            Key key = new Key(t.from(), t.to(), amount);
            Long prev = lastSeen.get(key);
            work += 2;
            boolean inside = prev != null && (mutant.equals("strictWindow") ? t.at() - prev < WINDOW : t.at() - prev <= WINDOW);
            if (inside) flagged.add(t.id());
            lastSeen.put(key, t.at());
        }
        return flagged;
    }

    static final List<String> MUTANTS = List.of("strictWindow", "rawBigDecimal", "noSort");

    public static void main(String[] args) {
        switch (args.length > 0 ? args[0] : "examples") {
            case "scale" -> scale();
            case "fuzz" -> fuzz();
            default -> examples();
        }
    }

    static Transfer t(String id, String from, String to, String amount, long at) {
        return new Transfer(id, from, to, new BigDecimal(amount), at);
    }

    static void examples() {
        // the examples you write on the board BEFORE coding (each one answers a clarifying question)
        Map<String, List<Transfer>> cases = new LinkedHashMap<>();
        cases.put("empty", List.of());
        cases.put("exactly 60 s apart (inclusive)", List.of(t("a1", "A", "B", "10.00", 0), t("a2", "A", "B", "10.00", 60)));
        cases.put("61 s apart", List.of(t("b1", "A", "B", "10.00", 0), t("b2", "A", "B", "10.00", 61)));
        cases.put("10.0 vs 10.00 (same money)", List.of(t("c1", "A", "B", "10.0", 0), t("c2", "A", "B", "10.00", 30)));
        cases.put("other target / other amount", List.of(t("d1", "A", "B", "10.00", 0), t("d2", "A", "C", "10.00", 5), t("d3", "A", "B", "10.01", 9)));
        cases.put("chain 0, 50, 100", List.of(t("e1", "A", "B", "5.00", 0), t("e2", "A", "B", "5.00", 50), t("e3", "A", "B", "5.00", 100)));
        cases.put("same second twice", List.of(t("f1", "A", "B", "7.00", 10), t("f2", "A", "B", "7.00", 10)));
        System.out.printf("%-32s | %-12s | %-12s | %-12s | %-13s | %-12s%n", "case", "brute force", "fast", "strictWindow", "rawBigDecimal", "noSort");
        Map<String, Integer> killed = new LinkedHashMap<>();
        for (var c : cases.entrySet()) {
            Set<String> expected = bruteForce(c.getValue());
            StringBuilder row = new StringBuilder(String.format("%-32s | %-12s | %-12s", c.getKey(), expected, fast(c.getValue(), "none")));
            for (String m : MUTANTS) {
                Set<String> got = fast(c.getValue(), m);
                row.append(String.format(" | %-" + (m.equals("rawBigDecimal") ? 13 : 12) + "s", got.equals(expected) ? "same" : got + "!"));
                if (!got.equals(expected)) killed.merge(m, 1, Integer::sum);
            }
            System.out.println(row);
        }
        for (String m : MUTANTS)
            System.out.println("mutant " + m + ": " + (killed.containsKey(m) ? "KILLED by " + killed.get(m) + " hand-made case(s)" : "SURVIVED all hand-made cases"));
    }

    static List<Transfer> random(Random rnd, int n, long timeRange, int accounts) {
        String[] amounts = {"10.00", "10.0", "10", "25.50", "99.99"};
        List<Transfer> ts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int f = rnd.nextInt(accounts), g = (f + 1 + rnd.nextInt(accounts - 1)) % accounts;   // never to itself (business rule)
            String from = "A" + f, to = "A" + g;
            ts.add(t("t" + i, from, to, amounts[rnd.nextInt(amounts.length)], rnd.nextLong(timeRange)));
        }
        return ts;   // NOT sorted by time, like a merge of two batches
    }

    static void fuzz() {
        Random rnd = new Random(8);
        List<List<Transfer>> cases = new ArrayList<>();
        for (int i = 0; i < 2000; i++) cases.add(random(rnd, 1 + rnd.nextInt(12), 200, 3));
        for (String variant : List.of("none", "strictWindow", "rawBigDecimal", "noSort")) {
            int diff = 0;
            List<Transfer> smallest = null;
            for (List<Transfer> c : cases) {
                if (!fast(c, variant).equals(bruteForce(c))) {
                    diff++;
                    if (smallest == null || c.size() < smallest.size()) smallest = c;
                }
            }
            System.out.printf("%-14s differs from brute force in %4d of 2000 random cases%n", variant.equals("none") ? "fast" : variant, diff);
            if (smallest != null) {
                System.out.println("   smallest counterexample (" + smallest.size() + " transfers): brute=" + bruteForce(smallest) + " got=" + fast(smallest, variant));
                for (Transfer x : smallest) System.out.println("     " + x.id() + " " + x.from() + "->" + x.to() + " " + x.amount() + " at " + x.at());
            }
        }
    }

    static void scale() {
        System.out.println("n        | brute force: pair checks | fast: comparator calls + map ops | ratio");
        for (int n : new int[]{1_000, 5_000, 20_000}) {
            List<Transfer> ts = random(new Random(n), n, 86_400, 500);   // one day, 500 accounts: few duplicates
            work = 0; bruteForce(ts); long b = work;
            work = 0; fast(ts, "none"); long f = work;
            System.out.printf("%-8d | %24d | %32d | %,d x%n", n, b, f, b / f);
        }
        for (int n : new int[]{100_000, 1_000_000}) {
            List<Transfer> ts = random(new Random(n), n, 86_400, 500);
            work = 0; fast(ts, "none"); long f = work;
            System.out.printf("%-8d | %24s | %32d | brute force would need ~%.1e pair checks%n", n, "(not run)", f, (double) n * (n - 1));
        }
    }
}
