import java.util.*;

/** S5.01 lab. Modes: layers | policy.
 *  layers: Monte Carlo of an attack crossing N defence layers, independent vs sharing one failure cause (premises, not measurements of the PayFlow).
 *  policy: a zero-trust style decision function + a table of cases + MUTANTS that prove the table can fail. */
public class DefenseLab {

    // ---------- layers ----------
    static void layers() {
        System.out.println("MODEL: an attack succeeds only if it crosses EVERY layer. Each layer misses an attack with probability m (a PREMISE). 2,000,000 simulated attacks per row, seed 42.");
        System.out.println("'shared cause' = with probability c ONE event (the same stolen credential / the same misconfiguration) makes ALL layers miss at once.\n");
        double m = 0.30; int n = 2_000_000;
        System.out.println(String.format("%-46s | %-12s | %-12s | %s", "scenario", "formula", "simulated", "reading"));
        for (int layers : new int[]{1, 2, 3, 5}) {
            double f = Math.pow(m, layers), s = sim(layers, m, 0, n);
            System.out.println(String.format("%-46s | %-12s | %-12s | %s", layers + " independent layer(s), miss 30% each", pct(f), pct(s), layers == 1 ? "one wall" : "each layer multiplies"));
        }
        for (double c : new double[]{0.01, 0.05}) {
            double f = c + (1 - c) * Math.pow(m, 5), s = sim(5, m, c, n);
            System.out.println(String.format("%-46s | %-12s | %-12s | %s", "5 layers + shared cause c=" + (int) (c * 100) + "%", pct(f), pct(s), "the shared cause is the floor"));
        }
        System.out.println("\nReading: five 70% layers give ~0.24% IF they fail independently; one shared cause of 1% leaves the attack succeeding ~" + pct(0.01 + 0.99 * Math.pow(m, 5)) + " of the time.");
        System.out.println("A layer that fails together with another (same credential, same network path, same person) is not another layer: count the shared causes, not the boxes.");
    }
    static double sim(int layers, double miss, double shared, int n) {
        Random r = new Random(42); int ok = 0;
        for (int i = 0; i < n; i++) {
            if (shared > 0 && r.nextDouble() < shared) { ok++; continue; }
            boolean through = true;
            for (int l = 0; l < layers && through; l++) through = r.nextDouble() < miss;
            if (through) ok++;
        }
        return (double) ok / n;
    }
    static String pct(double p) { return String.format("%.4f%%", p * 100); }

    // ---------- policy ----------
    enum Role { CUSTOMER, SUPPORT, ADMIN, SERVICE_API, SERVICE_REPORTING }
    enum Action { READ_ACCOUNT, TRANSFER, EXPORT_STATEMENT, ADMIN_DELETE, INTERNAL_NOTIFY }
    record Caller(String id, Role role, boolean mfa, String ticket) { }
    record Decision(boolean allow, String reason) { }

    static class Policy {
        final boolean checkOwner, denyByDefault, requireTicket;
        Policy(boolean checkOwner, boolean denyByDefault, boolean requireTicket) { this.checkOwner = checkOwner; this.denyByDefault = denyByDefault; this.requireTicket = requireTicket; }
        static Policy real() { return new Policy(true, true, true); }

        Decision decide(Caller c, Action a, String resourceOwner) {
            if (c == null || c.id() == null) return new Decision(false, "unauthenticated");
            switch (c.role()) {
                case CUSTOMER -> {
                    if (a == Action.READ_ACCOUNT || a == Action.TRANSFER || a == Action.EXPORT_STATEMENT) {
                        if (checkOwner && !c.id().equals(resourceOwner)) return new Decision(false, "not the owner");
                        if (a != Action.READ_ACCOUNT && !c.mfa()) return new Decision(false, "step-up (MFA) required");
                        return new Decision(true, "owner");
                    }
                }
                case SUPPORT -> { if (a == Action.READ_ACCOUNT) return new Decision(true, "support read (masked view)"); }
                case ADMIN -> {
                    if (a == Action.READ_ACCOUNT) return new Decision(true, "admin read");
                    if (a == Action.ADMIN_DELETE) {
                        if (requireTicket && (c.ticket() == null || c.ticket().isBlank() || !c.mfa())) return new Decision(false, "needs MFA + approved ticket (break-glass)");
                        return new Decision(true, "approved break-glass");
                    }
                }
                case SERVICE_API -> { if (a == Action.INTERNAL_NOTIFY) return new Decision(true, "service allowed"); }
                case SERVICE_REPORTING -> { if (a == Action.READ_ACCOUNT) return new Decision(true, "service read-only"); }
            }
            return denyByDefault ? new Decision(false, "no rule allows it (deny by default)") : new Decision(true, "BUG: no rule denied it");
        }
    }

    record Case(String name, Caller caller, Action action, String owner, boolean expectAllow) { }

    static List<Case> cases() {
        Caller ana = new Caller("ana", Role.CUSTOMER, true, null), anaNoMfa = new Caller("ana", Role.CUSTOMER, false, null);
        List<Case> l = new ArrayList<>();
        l.add(new Case("customer reads own account", ana, Action.READ_ACCOUNT, "ana", true));
        l.add(new Case("customer reads SOMEONE ELSE's account (IDOR)", ana, Action.READ_ACCOUNT, "bia", false));
        l.add(new Case("customer transfers from own account with MFA", ana, Action.TRANSFER, "ana", true));
        l.add(new Case("customer transfers from own account WITHOUT MFA", anaNoMfa, Action.TRANSFER, "ana", false));
        l.add(new Case("customer transfers from someone else's account", ana, Action.TRANSFER, "bia", false));
        l.add(new Case("customer exports someone else's statement", ana, Action.EXPORT_STATEMENT, "bia", false));
        l.add(new Case("customer calls admin delete", ana, Action.ADMIN_DELETE, "ana", false));
        l.add(new Case("support reads an account", new Caller("sue", Role.SUPPORT, true, null), Action.READ_ACCOUNT, "ana", true));
        l.add(new Case("support transfers", new Caller("sue", Role.SUPPORT, true, null), Action.TRANSFER, "ana", false));
        l.add(new Case("admin deletes with MFA and ticket", new Caller("adm", Role.ADMIN, true, "CHG-1042"), Action.ADMIN_DELETE, "ana", true));
        l.add(new Case("admin deletes WITHOUT ticket", new Caller("adm", Role.ADMIN, true, null), Action.ADMIN_DELETE, "ana", false));
        l.add(new Case("admin deletes with ticket but WITHOUT MFA", new Caller("adm", Role.ADMIN, false, "CHG-1042"), Action.ADMIN_DELETE, "ana", false));
        l.add(new Case("api service calls /internal/notify", new Caller("payflow-api", Role.SERVICE_API, true, null), Action.INTERNAL_NOTIFY, null, true));
        l.add(new Case("reporting service calls /internal/notify", new Caller("payflow-reporting", Role.SERVICE_REPORTING, true, null), Action.INTERNAL_NOTIFY, null, false));
        l.add(new Case("reporting service transfers", new Caller("payflow-reporting", Role.SERVICE_REPORTING, true, null), Action.TRANSFER, "ana", false));
        l.add(new Case("anonymous reads an account", null, Action.READ_ACCOUNT, "ana", false));
        return l;
    }

    static int run(Policy p, boolean print) {
        int bad = 0;
        for (Case c : cases()) {
            Decision d = p.decide(c.caller(), c.action(), c.owner());
            boolean ok = d.allow() == c.expectAllow();
            if (!ok) bad++;
            if (print) System.out.println(String.format("%-4s %-52s expect %-5s got %-5s (%s)", ok ? "ok" : "FAIL", c.name(), c.expectAllow() ? "allow" : "deny", d.allow() ? "allow" : "deny", d.reason()));
        }
        return bad;
    }

    static void policy() {
        System.out.println("MODEL: Policy.decide(caller, action, resourceOwner) — every call decides again (no trust from 'being inside'); deny by default; owner check; step-up for money; break-glass for admin delete.\n");
        int bad = run(Policy.real(), true);
        System.out.println("\nreal policy: " + cases().size() + " cases, " + bad + " failing\n");
        System.out.println("MUTANTS (a test table that never fails proves nothing): each removes ONE defence; the table must notice.");
        record Mut(String name, Policy p) { }
        for (Mut m : List.of(
                new Mut("M1 no owner check (IDOR)", new Policy(false, true, true)),
                new Mut("M2 allow by default (no rule -> allow)", new Policy(true, false, true)),
                new Mut("M3 admin delete without ticket/MFA", new Policy(true, true, false)))) {
            int f = run(m.p(), false);
            System.out.println(String.format("  %-42s -> %d case(s) fail %s", m.name(), f, f > 0 ? "(killed)" : "(SURVIVED: the table has a hole)"));
        }
    }

    public static void main(String[] a) {
        switch (a.length > 0 ? a[0] : "layers") { case "policy" -> policy(); default -> layers(); }
    }
}
