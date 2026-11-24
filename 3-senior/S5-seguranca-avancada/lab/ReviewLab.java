import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** S5.07 lab. Modes: score | lint | coverage.  Tools for a security review: a risk rubric, a report linter (with a self-test) and a scope-coverage check.
 *  The rubric, the SLAs and the findings are EXAMPLES/PROPOSALS: calibrate them with your own risk owners. */
public class ReviewLab {
    record Finding(String id, String title, String asset, int likelihood, int impact, String severity, String evidence, String recommendation,
                   String owner, String due, String status, String acceptedBy, String acceptedUntil) { }

    static String sev(int l, int i) { int s = l * i; return s >= 20 ? "CRITICAL" : s >= 12 ? "HIGH" : s >= 6 ? "MEDIUM" : "LOW"; }
    static int sla(String sev) { return switch (sev) { case "CRITICAL" -> 7; case "HIGH" -> 30; case "MEDIUM" -> 90; default -> 365; }; }

    static void score() {
        System.out.println("MODEL: severity = likelihood (1-5) x impact (1-5): >=20 CRITICAL, >=12 HIGH, >=6 MEDIUM, else LOW. A PROPOSED rubric; the number is a conversation starter, not a measurement.\n");
        System.out.print(String.format("%-12s", "L \\ I"));
        for (int i = 1; i <= 5; i++) System.out.print(String.format("%-10d", i));
        System.out.println();
        for (int l = 5; l >= 1; l--) {
            System.out.print(String.format("%-12d", l));
            for (int i = 1; i <= 5; i++) System.out.print(String.format("%-10s", sev(l, i).equals("CRITICAL") ? "CRIT" : sev(l, i).equals("MEDIUM") ? "MED" : sev(l, i)));
            System.out.println();
        }
        System.out.println("\nSLA to fix (days, proposed): CRITICAL " + sla("CRITICAL") + " | HIGH " + sla("HIGH") + " | MEDIUM " + sla("MEDIUM") + " | LOW " + sla("LOW"));
        System.out.println("\nWorked examples:");
        record Ex(String what, int l, int i) { }
        for (Ex e : List.of(new Ex("IDOR on GET /accounts/{id} found, trivially exploitable, other customers' balances", 5, 4),
                            new Ex("missing CSP header on a page with no user content", 2, 2),
                            new Ex("DB port open to 0.0.0.0/0 but needs the DB password (strong, rotated)", 2, 5),
                            new Ex("heap dump with personal data left in a shared bucket", 3, 5)))
            System.out.println(String.format("  L=%d I=%d -> %-8s %s", e.l(), e.i(), sev(e.l(), e.i()), e.what()));
        System.out.println("Reading: the same finding moves with its context (the 5432 example is MEDIUM here, because of the password; it would be HIGH or worse if the password were weak). Write the assumption next to the score.");
    }

    static List<String> lintFinding(Finding f, String today) {
        List<String> p = new ArrayList<>();
        if (f.evidence() == null || f.evidence().isBlank() || f.evidence().equalsIgnoreCase("n/a")) p.add("no evidence (a finding nobody can reproduce is an opinion)");
        if (f.recommendation() == null || f.recommendation().isBlank()) p.add("no recommendation");
        if (f.owner() == null || f.owner().isBlank()) p.add("no owner");
        if (f.likelihood() < 1 || f.likelihood() > 5 || f.impact() < 1 || f.impact() > 5) p.add("likelihood/impact outside 1-5");
        else if (!sev(f.likelihood(), f.impact()).equals(f.severity())) p.add("severity " + f.severity() + " does not match L x I = " + sev(f.likelihood(), f.impact()));
        if (f.due() == null) p.add("no due date");
        else if (f.status().equals("OPEN") && f.severity() != null && LocalDate.parse(f.due()).isAfter(LocalDate.parse(today).plusDays(sla(f.severity()))))
            p.add("due date " + f.due() + " is later than the SLA of a " + f.severity() + " (" + sla(f.severity()) + " days)");
        if (f.status().equals("OPEN") && f.due() != null && LocalDate.parse(f.due()).isBefore(LocalDate.parse(today))) p.add("OPEN and overdue since " + f.due());
        if (f.status().equals("ACCEPTED")) {
            if (f.acceptedBy() == null || f.acceptedBy().isBlank()) p.add("accepted risk without an approver");
            if (f.acceptedUntil() == null) p.add("accepted risk without an expiry date");
            else if (LocalDate.parse(f.acceptedUntil()).isBefore(LocalDate.parse(today))) p.add("accepted risk EXPIRED on " + f.acceptedUntil() + ": decide again");
        }
        return p;
    }

    static void lint() {
        String today = "2026-11-23";
        System.out.println("MODEL: a report linter. Today = " + today + ". The findings are EXAMPLES about a fictional PayFlow review, written so that the good ones pass and the bad ones fail for a stated reason.\n");
        List<Finding> good = List.of(
            new Finding("F-01", "IDOR on GET /accounts/{id}", "payflow-api", 5, 4, "CRITICAL", "curl as user A with B's id returns 200 and B's balance (request/response saved in evidence/F-01.txt)", "check ownership in the service layer; add the policy table test (S5.01)", "payments-team", "2026-11-30", "OPEN", null, null),
            new Finding("F-02", "Statements bucket without versioning", "s3://payflow-statements", 2, 3, "MEDIUM", "terraform plan shows no versioning block (main.tf:12)", "enable versioning + Object Lock (S4.05 layer 5)", "platform-team", "2027-02-01", "OPEN", null, null),
            new Finding("F-03", "No CSP on the static pages", "payflow-web", 2, 2, "LOW", "response headers captured in evidence/F-03.txt", "add the header at the gateway", "platform-team", "2027-06-01", "ACCEPTED", "CTO", "2027-01-31"));
        List<Finding> bad = List.of(
            new Finding("B-01", "Weak password policy", "identity", 3, 3, "HIGH", "n/a", "", "", "2026-12-01", "OPEN", null, null),
            new Finding("B-02", "Open DB port", "rds", 4, 5, "MEDIUM", "scan output", "close it", "platform-team", "2027-01-30", "OPEN", null, null),
            new Finding("B-03", "Old TLS config", "gateway", 3, 4, "HIGH", "sslscan output", "disable TLS 1.1", "platform-team", "2026-11-01", "OPEN", null, null),
            new Finding("B-04", "Legacy export job", "reporting", 2, 2, "LOW", "job runs as admin (config line 8)", "split the role", "data-team", "2027-03-01", "ACCEPTED", "", "2026-10-15"));
        int total = 0;
        System.out.println("-- report 'good' (" + good.size() + " findings)");
        for (Finding f : good) { List<String> p = lintFinding(f, today); total += p.size(); System.out.println("  " + f.id() + " " + f.severity() + ": " + (p.isEmpty() ? "ok" : p)); }
        System.out.println("-- report 'bad' (" + bad.size() + " findings, written to FAIL)");
        int badProblems = 0;
        for (Finding f : bad) { List<String> p = lintFinding(f, today); badProblems += p.size(); System.out.println("  " + f.id() + " " + f.severity() + ": " + (p.isEmpty() ? "ok  <-- the linter has a hole" : p)); }
        System.out.println("\nself-test: good report -> " + total + " problems (expected 0); bad report -> " + badProblems + " problems (expected > 0)");
    }

    static void coverage() {
        System.out.println("MODEL: review scope = components x STRIDE (P10.07). Every cell must be REVIEWED, N/A with a reason, or explicitly OUT-OF-SCOPE with who accepted it. Silence is not allowed.\n");
        String[] comps = {"api", "ledger-db", "keycloak", "outbox/broker", "pipeline", "backups"};
        String[] st = {"S", "T", "R", "I", "D", "E"};
        String[][] status = {
            {"ok", "ok", "ok", "ok", "ok", "ok"},
            {"ok", "ok", "ok", "ok", "na", "ok"},
            {"ok", "ok", "ok", "ok", "ok", "ok"},
            {"ok", "ok", "??", "ok", "na", "ok"},
            {"ok", "ok", "ok", "??", "??", "ok"},
            {"na", "ok", "??", "ok", "ok", "ok"}};
        Map<String, String> why = Map.of("ledger-db/D", "N/A: availability of the DB is S4 (SLO), not this review", "outbox/broker/D", "N/A: covered by the S4.04 experiments", "backups/S", "N/A: backups have no interactive identity");
        System.out.print(String.format("%-16s", ""));
        for (String s : st) System.out.print(String.format("%-5s", s));
        System.out.println();
        int unknown = 0, total = 0;
        List<String> holes = new ArrayList<>();
        for (int i = 0; i < comps.length; i++) {
            System.out.print(String.format("%-16s", comps[i]));
            for (int j = 0; j < st.length; j++) {
                total++;
                String v = status[i][j];
                if (v.equals("??")) { unknown++; holes.add(comps[i] + "/" + st[j]); }
                System.out.print(String.format("%-5s", v));
            }
            System.out.println();
        }
        System.out.println("\nlegend: ok = reviewed, na = N/A with a written reason, ?? = silent (not reviewed, no reason). Columns (STRIDE): S = Spoofing, T = Tampering, R = Repudiation, I = Information disclosure, D = Denial of service, E = Elevation of privilege");
        System.out.println("coverage: " + (total - unknown) + "/" + total + " cells decided; silent cells: " + holes);
        System.out.println("verdict: " + (unknown == 0 ? "PASS" : "FAIL: the review cannot say 'no more findings' while these cells are silent"));
        System.out.println("N/A reasons recorded:"); new TreeMap<>(why).forEach((k, v) -> System.out.println("  " + k + " -> " + v));
        System.out.println("\nReading: 'we found nothing' only means something if the report says what was looked at. An empty cell is the most common place for the real finding.");
    }

    public static void main(String[] a) {
        switch (a.length > 0 ? a[0] : "score") { case "lint" -> lint(); case "coverage" -> coverage(); default -> score(); }
    }
}
