import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * P8.04 laboratory: a minimal feature-flag evaluator and the experiments the lesson quotes.
 * Run: java FlagLab.java            (JDK 25; no dependencies)
 */
public class FlagLab {

    // ---------------------------------------------------------------- the evaluator
    /** A flag: kill switch, allowlist, attribute rule and a percentage in basis points (0..10000). */
    record Flag(String key, boolean enabled, int basisPoints, Set<String> allowUsers,
                Map<String, String> requiredAttributes, LocalDate expires, String owner) {
        Flag {
            if (basisPoints < 0 || basisPoints > 10_000) throw new IllegalArgumentException("basisPoints 0..10000");
            allowUsers = Set.copyOf(allowUsers);
            requiredAttributes = Map.copyOf(requiredAttributes);
        }
        static Flag on(String key, int percent, LocalDate expires) {
            return new Flag(key, true, percent * 100, Set.of(), Map.of(), expires, "payments-team");
        }
    }

    record Context(String userId, Map<String, String> attributes) {
        static Context of(String userId, String... kv) {
            var m = new HashMap<String, String>();
            for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
            return new Context(userId, m);
        }
    }

    /** Immutable snapshot swapped atomically: readers never see half of an update. */
    static final class FeatureFlags {
        private final AtomicReference<Map<String, Flag>> snapshot = new AtomicReference<>(Map.of());
        private final boolean useSalt;                       // false only to demonstrate the bug

        FeatureFlags(boolean useSalt) { this.useSalt = useSalt; }
        FeatureFlags() { this(true); }

        void replaceAll(Collection<Flag> flags) {
            var m = new HashMap<String, Flag>();
            for (Flag f : flags) m.put(f.key(), f);
            snapshot.set(Map.copyOf(m));
        }

        /** One consistent view: several decisions of the SAME request use the same snapshot. */
        View view() { return new View(snapshot.get()); }

        final class View {
            private final Map<String, Flag> flags;
            private View(Map<String, Flag> flags) { this.flags = flags; }
            boolean isEnabled(String key, Context ctx, boolean defaultValue) { return decide(flags, key, ctx, defaultValue); }
        }

        /** The caller states the default: what to do when the flag does not exist (fail closed = false). */
        boolean isEnabled(String key, Context ctx, boolean defaultValue) {
            return decide(snapshot.get(), key, ctx, defaultValue);
        }

        private boolean decide(Map<String, Flag> flags, String key, Context ctx, boolean defaultValue) {
            Flag f = flags.get(key);
            if (f == null) return defaultValue;
            if (!f.enabled()) return false;                              // 1. kill switch beats everything
            if (f.allowUsers().contains(ctx.userId())) return true;      // 2. internal testers
            for (var e : f.requiredAttributes().entrySet())              // 3. targeting rule
                if (!e.getValue().equals(ctx.attributes().get(e.getKey()))) return false;
            return bucket(f.key(), ctx.userId()) < f.basisPoints();      // 4. percentage rollout
        }

        /** Stable bucket 0..9999 from SHA-256 of "flag:user" (the flag key is the salt). */
        int bucket(String flagKey, String userId) {
            try {
                var md = MessageDigest.getInstance("SHA-256");
                String input = useSalt ? flagKey + ":" + userId : userId;
                byte[] h = md.digest(input.getBytes(StandardCharsets.UTF_8));
                long v = ((h[0] & 0xFFL) << 24) | ((h[1] & 0xFFL) << 16) | ((h[2] & 0xFFL) << 8) | (h[3] & 0xFFL);
                return (int) (v % 10_000);
            } catch (Exception e) { throw new IllegalStateException(e); }
        }

        /** Flag debt: the flags past their expiry date (a CI test fails if this list is not empty). */
        List<String> expired(LocalDate today) {
            return snapshot.get().values().stream().filter(f -> f.expires().isBefore(today)).map(Flag::key).sorted().toList();
        }
    }

    // ---------------------------------------------------------------- experiments
    static void check(boolean ok, String what) {
        System.out.println((ok ? "  PASS " : "  FAIL ") + what);
        if (!ok) { System.out.println("EXPERIMENT FAILED"); System.exit(1); }
    }

    public static void main(String[] args) throws Exception {
        final int USERS = 100_000;
        var today = LocalDate.of(2026, 11, 14);
        var future = today.plusDays(30);

        System.out.println("== 1. precedence of the rules");
        var ff = new FeatureFlags();
        ff.replaceAll(List.of(
            new Flag("pix-v2", true, 0, Set.of("alice"), Map.of(), future, "payments-team"),
            new Flag("fx-live", true, 10_000, Set.of(), Map.of("country", "BR"), future, "payments-team"),
            new Flag("killed", false, 10_000, Set.of("alice"), Map.of(), future, "payments-team")));
        check(ff.isEnabled("pix-v2", Context.of("alice"), false), "allowlisted user sees pix-v2 even at 0%");
        check(!ff.isEnabled("pix-v2", Context.of("bob"), false), "everybody else does not (0%)");
        check(ff.isEnabled("fx-live", Context.of("bob", "country", "BR"), false), "rule country=BR matches");
        check(!ff.isEnabled("fx-live", Context.of("bob", "country", "AR"), false), "rule country=BR rejects AR");
        check(!ff.isEnabled("killed", Context.of("alice"), true), "kill switch beats the allowlist");
        check(ff.isEnabled("does-not-exist", Context.of("bob"), true) && !ff.isEnabled("does-not-exist", Context.of("bob"), false),
              "unknown flag returns the CALLER's default (a typo is not silent: see the log in the lesson)");

        System.out.println("== 2. a 10% rollout over " + USERS + " users");
        ff.replaceAll(List.of(Flag.on("new-limits", 10, future)));
        int on10 = 0; var set10 = new HashSet<String>();
        for (int i = 0; i < USERS; i++) { String u = "user-" + i; if (ff.isEnabled("new-limits", Context.of(u), false)) { on10++; set10.add(u); } }
        System.out.printf("  enabled for %d of %d users = %.2f%% (target 10%%)%n", on10, USERS, 100.0 * on10 / USERS);
        check(Math.abs(on10 - 10_000) < 400, "within 4% of the target (binomial stdev ~ 95 users)");

        System.out.println("== 3. stickiness and ramp-up");
        int flips = 0;
        for (int i = 0; i < 20_000; i++) { String u = "user-" + i; boolean a = ff.isEnabled("new-limits", Context.of(u), false), b = ff.isEnabled("new-limits", Context.of(u), false); if (a != b) flips++; }
        check(flips == 0, "same user, same answer, every time (no state stored)");
        ff.replaceAll(List.of(Flag.on("new-limits", 25, future)));
        int kept = 0, on25 = 0;
        for (int i = 0; i < USERS; i++) { String u = "user-" + i; if (ff.isEnabled("new-limits", Context.of(u), false)) { on25++; if (set10.contains(u)) kept++; } }
        System.out.printf("  at 25%%: %d users enabled; %d of the %d users from the 10%% step are still in%n", on25, kept, set10.size());
        check(kept == set10.size(), "ramp 10% -> 25% only ADDS users (nobody loses the feature)");

        System.out.println("== 4. independence between flags (the salt)");
        for (boolean salt : new boolean[] {true, false}) {
            var f2 = new FeatureFlags(salt);
            f2.replaceAll(List.of(Flag.on("flag-a", 50, future), Flag.on("flag-b", 50, future)));
            int both = 0;
            for (int i = 0; i < USERS; i++) { var c = Context.of("user-" + i); if (f2.isEnabled("flag-a", c, false) && f2.isEnabled("flag-b", c, false)) both++; }
            System.out.printf("  salt=%-5s two 50%% flags: both on for %.1f%% of users (independent = 25%%, bug = 50%%)%n", salt, 100.0 * both / USERS);
            if (salt) check(Math.abs(both - 25_000) < 800, "with the flag key as salt the flags are independent");
            else check(both > 49_000, "without salt the SAME 50% get every flag (a 'canary group' that suffers everything)");
        }

        System.out.println("== 5. atomic updates under concurrent reads");
        // two flags that must always be changed TOGETHER (new endpoint + its limit): readers must never see (on, off) or (off, on)
        var f3 = new FeatureFlags();
        f3.replaceAll(List.of(Flag.on("pair-a", 100, future), Flag.on("pair-b", 100, future)));
        for (boolean useView : new boolean[] {false, true}) {
            var stop = new AtomicBoolean(); var torn = new AtomicLong(); var reads = new AtomicLong();
            var pool = Executors.newFixedThreadPool(5);
            for (int t = 0; t < 4; t++) pool.submit(() -> {
                var c = Context.of("u");
                while (!stop.get()) {
                    boolean a, b;
                    if (useView) { var v = f3.view(); a = v.isEnabled("pair-a", c, false); b = v.isEnabled("pair-b", c, false); }
                    else { a = f3.isEnabled("pair-a", c, false); b = f3.isEnabled("pair-b", c, false); }
                    reads.incrementAndGet(); if (a != b) torn.incrementAndGet();
                }
            });
            pool.submit(() -> {
                boolean on = true;
                while (!stop.get()) { on = !on; int p = on ? 100 : 0; f3.replaceAll(List.of(Flag.on("pair-a", p, future), Flag.on("pair-b", p, future))); }
            });
            Thread.sleep(1500); stop.set(true); pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) throw new IllegalStateException("workers did not stop");
            System.out.printf("  %-34s %,d paired reads, %,d inconsistent (%.1f%%)%n",
                useView ? "ONE snapshot per decision (view):" : "two separate isEnabled() calls:", reads.get(), torn.get(), 100.0 * torn.get() / reads.get());
            if (useView) check(torn.get() == 0, "with a snapshot the pair is never torn");
            else check(torn.get() > 0, "without it a reader CAN see (on, off): the update is atomic, the PAIR of reads is not");
        }

        System.out.println("== 6. flag debt");
        ff.replaceAll(List.of(Flag.on("old-experiment", 100, today.minusDays(1)), Flag.on("fresh", 10, future)));
        System.out.println("  expired flags: " + ff.expired(today));
        check(ff.expired(today).equals(List.of("old-experiment")), "the expiry check finds exactly the forgotten flag");
        System.out.println("ALL EXPERIMENTS PASSED");
    }
}
