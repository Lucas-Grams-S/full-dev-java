// StranglerLab.java — S7.04: modernizing the statement read path (legacy /entries over the ledger ->
// /statement over the CQRS read model of S1.05) with BRANCH BY ABSTRACTION and a PARALLEL RUN.
//   java StranglerLab.java [correct|m1|m2]   (default: all three)
// MODEL, not PayFlow: in-memory ledger, projector with random lag, 10 simulated minutes. Seeded: deterministic.
//   correct : projector applies entries in account_seq order and refuses gaps (decision 188)
//   m1      : projector deduplicates by transfer_id (deposits have transfer_id = null, ADR 0001)
//   m2      : projector applies entries on ARRIVAL (accepts gaps): balance_after computed out of order

import java.math.BigDecimal;
import java.util.*;

public class StranglerLab {

    record Entry(long seq, String transferId, BigDecimal amount, BigDecimal balanceAfter, long writtenAt) {}

    /** The abstraction both implementations live behind (branch by abstraction). */
    interface StatementReader { List<Entry> lastEntries(int account, int limit, long now); }

    static final int ACCOUNTS = 50, WINDOW = 20;
    static final long DURATION_MS = 600_000;
    static final double WRITES_PER_S = 40, READS_PER_S = 20, MEAN_LAG_MS = 150, DEPOSIT_SHARE = 0.2;

    public static void main(String[] args) {
        List<String> variants = args.length == 0 ? List.of("correct", "m1", "m2") : List.of(args);
        for (String v : variants) run(v);
    }

    static void run(String variant) {
        Random r = new Random(2026);
        // ---- ledger (source of truth): per account, entries in seq order, written at writtenAt
        List<List<Entry>> ledger = new ArrayList<>();
        List<List<Long>> visibleAt = new ArrayList<>();        // when the projector RECEIVES each entry
        for (int a = 0; a < ACCOUNTS; a++) { ledger.add(new ArrayList<>()); visibleAt.add(new ArrayList<>()); }
        long t = 0; int transfers = 0;
        BigDecimal[] balance = new BigDecimal[ACCOUNTS];
        Arrays.fill(balance, BigDecimal.ZERO);
        while ((t += (long) (-Math.log(1 - r.nextDouble()) * 1000 / WRITES_PER_S)) < DURATION_MS) {
            int a = r.nextInt(ACCOUNTS);
            boolean deposit = r.nextDouble() < DEPOSIT_SHARE || balance[a].signum() == 0;
            BigDecimal amount = BigDecimal.valueOf(100 + r.nextInt(9_900), 2);
            if (!deposit) amount = amount.min(balance[a]).negate();
            balance[a] = balance[a].add(amount);
            String transferId = deposit ? null : "t" + (++transfers);
            long seq = ledger.get(a).size() + 1;
            ledger.get(a).add(new Entry(seq, transferId, amount, balance[a], t));
            visibleAt.get(a).add(t + (long) (-Math.log(1 - r.nextDouble()) * MEAN_LAG_MS));   // exponential lag
        }

        StatementReader legacy = (account, limit, now) -> {             // old path: query the ledger directly
            List<Entry> all = ledger.get(account).stream().filter(e -> e.writtenAt() <= now).toList();
            return all.subList(Math.max(0, all.size() - limit), all.size());
        };
        StatementReader projected = (account, limit, now) -> project(variant, ledger.get(account), visibleAt.get(account), now, limit);

        // ---- parallel run: every read goes to BOTH; the legacy answer is returned; differences are recorded
        int reads = 0, naiveDiff = 0, lag = 0, mismatch = 0, firstMismatchAt = -1;
        long rt = 0;
        while ((rt += (long) (-Math.log(1 - r.nextDouble()) * 1000 / READS_PER_S)) < DURATION_MS) {
            int a = r.nextInt(ACCOUNTS);
            List<Entry> old = legacy.lastEntries(a, WINDOW, rt), neu = projected.lastEntries(a, WINDOW, rt);
            reads++;
            if (!old.equals(neu)) naiveDiff++;
            long newMax = neu.isEmpty() ? 0 : neu.getLast().seq();
            long oldMax = old.isEmpty() ? 0 : old.getLast().seq();
            // version-aware: compare only what both have seen (seq <= newMax) inside the legacy window
            List<Entry> oldUpToNew = old.stream().filter(e -> e.seq() <= newMax).toList();
            List<Entry> neuInOldWindow = neu.stream().filter(e -> old.isEmpty() || e.seq() >= old.getFirst().seq()).toList();
            if (!oldUpToNew.equals(neuInOldWindow)) {
                mismatch++;
                if (firstMismatchAt < 0) firstMismatchAt = reads;
            } else if (newMax < oldMax) lag++;
        }
        int entries = ledger.stream().mapToInt(List::size).sum();
        System.out.printf("%n[%s] %,d ledger entries, %,d reads in parallel (10 simulated minutes, mean projector lag %.0f ms)%n",
                variant, entries, reads, MEAN_LAG_MS);
        System.out.printf("  naive equals()      : %5d different (%.1f%%)%n", naiveDiff, 100.0 * naiveDiff / reads);
        System.out.printf("  version-aware LAG   : %5d (%.1f%%) -> expected, transient (would be a fallback with X-Min-Seq)%n", lag, 100.0 * lag / reads);
        System.out.printf("  version-aware BUG   : %5d%s%n", mismatch, mismatch == 0 ? "" : "  (first at read #" + firstMismatchAt + ")");
        if (mismatch == 0) {
            System.out.printf("  0 bugs in %,d reads -> 95%% upper bound on the bug rate = 3/n = %.3f%% (rule of three)%n", reads, 300.0 / reads);
            for (double rate : new double[]{1e-3, 1e-4, 1e-5})
                System.out.printf("  a bug that hits 1 read in %,.0f would show up at least once with probability %.1f%%%n",
                        1 / rate, 100 * (1 - Math.pow(1 - rate, reads)));
        }
    }

    static List<Entry> project(String variant, List<Entry> ledger, List<Long> arrivals, long now, int limit) {
        List<Entry> applied = new ArrayList<>();
        switch (variant) {
            case "correct" -> {                // in order; stop at the first entry not yet received (gap)
                for (int i = 0; i < ledger.size() && arrivals.get(i) <= now; i++) applied.add(ledger.get(i));
            }
            case "m1" -> {                     // dedupe by transfer_id: every deposit after the first is "a duplicate"
                Set<String> seen = new HashSet<>();
                for (int i = 0; i < ledger.size() && arrivals.get(i) <= now; i++) {
                    String key = String.valueOf(ledger.get(i).transferId());          // "null" for every deposit
                    if (seen.add(key)) applied.add(ledger.get(i));
                }
            }
            case "m2" -> {                     // apply on arrival, compute balance_after from what arrived so far
                List<Integer> idx = new ArrayList<>();
                for (int i = 0; i < ledger.size(); i++) if (arrivals.get(i) <= now) idx.add(i);
                idx.sort(Comparator.comparingLong(arrivals::get));
                BigDecimal running = BigDecimal.ZERO;
                List<Entry> tmp = new ArrayList<>();
                for (int i : idx) {
                    Entry e = ledger.get(i);
                    running = running.add(e.amount());
                    tmp.add(new Entry(e.seq(), e.transferId(), e.amount(), running, e.writtenAt()));
                }
                tmp.sort(Comparator.comparingLong(Entry::seq));
                applied.addAll(tmp);
            }
            default -> throw new IllegalArgumentException(variant);
        }
        return applied.subList(Math.max(0, applied.size() - limit), applied.size());
    }
}
