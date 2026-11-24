import com.sun.net.httpserver.*;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** S5.04 lab. Modes: sast | dast | sca | policy.  A deliberately SMALL stand-in for SAST/DAST/SCA to show what each can and cannot see.
 *  Not a replacement for Semgrep/CodeQL/ZAP/Trivy (Trivy runs for real in run-trivy.sh). Advisories in `sca` are SYNTHETIC. */
public class PipelineLab {

    // ---------------- SAST: line-based rules ----------------
    record Rule(String id, String why, Pattern p) { }
    static final List<Rule> RULES = List.of(
        new Rule("SQL-CONCAT", "SQL built by string concatenation", Pattern.compile("(executeQuery|executeUpdate|prepareStatement|createQuery)\\s*\\(\\s*\"[^\"]*\"\\s*\\+")),
        new Rule("HARDCODED-SECRET", "secret-looking constant with a literal value", Pattern.compile("(?i)(password|secret|apikey|token)\\w*\\s*=\\s*\"[^\"]{6,}\"")),
        new Rule("WEAK-HASH", "MD5/SHA-1 used as a hash", Pattern.compile("MessageDigest\\.getInstance\\(\"(MD5|SHA-?1)\"\\)")),
        new Rule("PREDICTABLE-RANDOM", "java.util.Random is not a CSPRNG", Pattern.compile("new\\s+(java\\.util\\.)?Random\\s*\\(")),
        new Rule("CMD-INJECTION", "shell command built by concatenation", Pattern.compile("exec\\s*\\(\\s*\"[^\"]*\"\\s*\\+")),
        new Rule("TRUST-ALL", "TLS trust manager that accepts everything", Pattern.compile("checkServerTrusted\\(\\)\\s*\\{\\s*\\}")));

    static List<String> scan(Path f) throws IOException {
        List<String> out = new ArrayList<>();
        List<String> lines = Files.readAllLines(f);
        for (int i = 0; i < lines.size(); i++)
            for (Rule r : RULES) if (r.p().matcher(lines.get(i)).find()) out.add(String.format("  %-18s line %-2d %s", r.id(), i + 1, r.why()));
        return out;
    }

    static void sast() throws Exception {
        System.out.println("MODEL: 6 line-based regex rules (what a grep-level SAST does). No data flow, no types.");
        for (String f : List.of("Vulnerable", "Fixed", "Tricky")) {
            List<String> r = scan(Path.of("samples/sast/" + f + ".java.txt"));
            System.out.println("\n" + f + ".java.txt -> " + r.size() + " finding(s)");
            r.forEach(System.out::println);
        }
        System.out.println("\nReading:");
        System.out.println("- Vulnerable: every planted flaw is found (true positives); Fixed: 0 findings (the fixes are recognised).");
        System.out.println("- Tricky: lines 4-5 build the same SQL on TWO lines -> NOT found (false negative: needs taint tracking, e.g. CodeQL or Semgrep in taint mode).");
        System.out.println("- Tricky: line 10 is flagged as a secret but is only the NAME of a form field (false positive: the rule sees text, not meaning).");
        System.out.println("- Tricky: the log line with 'SELECT' is NOT flagged here because this rule looks for executeQuery(...) (a looser rule would flag it).");
    }

    // ---------------- DAST: probe a running app ----------------
    static HttpServer app(boolean fixed) throws IOException {
        HttpServer s = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/search", ex -> {
            String q = Optional.ofNullable(ex.getRequestURI().getQuery()).orElse("q=").replaceFirst("^q=", "");
            q = java.net.URLDecoder.decode(q, StandardCharsets.UTF_8);
            String body; int code = 200;
            if (!fixed && q.contains("'")) { code = 500; body = "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"'\"\n\tat com.payflow.db.AccountDao.find(AccountDao.java:42)"; }
            else if (fixed && q.contains("'")) { code = 400; body = "{\"title\":\"Invalid request\"}"; }
            else body = fixed ? "<p>Results for " + q.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;") + "</p>" : "<p>Results for " + q + "</p>";
            ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            if (fixed) {
                ex.getResponseHeaders().add("Content-Security-Policy", "default-src 'self'");
                ex.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
                ex.getResponseHeaders().add("Set-Cookie", "SESSION=abc; HttpOnly; Secure; SameSite=Lax; Path=/");
            } else {
                ex.getResponseHeaders().add("Set-Cookie", "SESSION=abc; Path=/");
                ex.getResponseHeaders().add("Server", "Jetty(9.2.2.v20140723)");
            }
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(code, b.length);
            try (OutputStream o = ex.getResponseBody()) { o.write(b); }
        });
        s.start();
        return s;
    }

    static List<String> probe(int port) throws Exception {
        HttpClient c = HttpClient.newHttpClient();
        List<String> f = new ArrayList<>();
        String xss = "<script>alert(1)</script>";
        HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/search?q=" + URLEncoder.encode(xss, StandardCharsets.UTF_8))).build(), HttpResponse.BodyHandlers.ofString());
        if (r.body().contains(xss)) f.add("REFLECTED-XSS     payload came back unescaped in the HTML");
        if (r.headers().firstValue("Content-Security-Policy").isEmpty()) f.add("MISSING-CSP       no Content-Security-Policy header");
        if (r.headers().firstValue("X-Content-Type-Options").isEmpty()) f.add("MISSING-NOSNIFF   no X-Content-Type-Options header");
        r.headers().allValues("Set-Cookie").forEach(ck -> {
            if (!ck.toLowerCase().contains("httponly")) f.add("COOKIE-HTTPONLY   cookie without HttpOnly: " + ck.split(";")[0].split("=")[0]);
            if (!ck.toLowerCase().contains("samesite")) f.add("COOKIE-SAMESITE   cookie without SameSite");
        });
        r.headers().firstValue("Server").filter(v -> v.matches(".*\\d.*")).ifPresent(v -> f.add("VERSION-BANNER    Server header reveals a version: " + v));
        HttpResponse<String> e = c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/search?q=" + URLEncoder.encode("'", StandardCharsets.UTF_8))).build(), HttpResponse.BodyHandlers.ofString());
        if (e.statusCode() == 500) f.add("ERROR-LEAK        500 with a stack trace / SQL error in the body (" + e.body().substring(0, 40) + "...)");
        return f;
    }

    static void dast() throws Exception {
        System.out.println("MODEL: a 3-request black-box probe against a tiny local app (HttpServer on 127.0.0.1). Secure cookie / HSTS are N/A over plain HTTP and not checked.");
        for (boolean fixed : new boolean[]{false, true}) {
            HttpServer s = app(fixed);
            List<String> f = probe(s.getAddress().getPort());
            System.out.println("\napp (" + (fixed ? "FIXED" : "VULNERABLE") + ") -> " + f.size() + " finding(s)");
            f.forEach(x -> System.out.println("  " + x));
            s.stop(0);
        }
        System.out.println("\nReading: DAST sees only what the running app shows to a request it thought of; it cannot see /admin if nobody sends it there, nor the IDOR that needs two logged-in users (S5.01 policy table).");
    }

    // ---------------- SCA: versions vs advisories (SYNTHETIC) ----------------
    record Adv(String id, String lib, String vulnerableBelow, String fixed, String sev, String note) { }
    record Dep(String lib, String version, boolean direct) { }

    static int cmp(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? Integer.parseInt(x[i]) : 0, q = i < y.length ? Integer.parseInt(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    static void sca() {
        System.out.println("MODEL: dependency list x advisory list. The advisories below are SYNTHETIC (ids SYN-*, invented for the exercise); the real SCA run is run-trivy.sh.\n");
        List<Dep> deps = List.of(new Dep("acme-json", "2.3.1", true), new Dep("acme-http", "1.9.0", true), new Dep("acme-codec", "0.8.4", false), new Dep("acme-log", "3.0.0", true));
        List<Adv> advs = List.of(
            new Adv("SYN-0001", "acme-json", "2.4.0", "2.4.0", "HIGH", "deserialization of untrusted input"),
            new Adv("SYN-0002", "acme-codec", "0.9.0", "0.9.0", "MEDIUM", "decompression bomb"),
            new Adv("SYN-0003", "acme-log", "3.0.0", "3.0.0", "LOW", "log forging (fixed in 3.0.0: this version is NOT vulnerable)"));
        Map<String, String> reachable = Map.of("SYN-0001", "yes: PayFlow parses request bodies with it", "SYN-0002", "no: only used by a build-time tool");
        for (Dep d : deps) for (Adv a : advs)
            if (a.lib().equals(d.lib()) && cmp(d.version(), a.vulnerableBelow()) < 0)
                System.out.println(String.format("%-7s %-8s %-11s %-6s %-26s fix: >= %-6s | used by PayFlow? %s", a.sev(), a.id(), d.lib() + ":" + d.version(), d.direct() ? "direct" : "TRANSIT", a.note(), a.fixed(), reachable.getOrDefault(a.id(), "unknown")));
        System.out.println("\nSYN-0003 is not listed: the version check is what separates 'library present' from 'library vulnerable'.");
        System.out.println("Reading: the transitive one (acme-codec) only shows up because the tool resolves the whole tree; 'reachable?' is a HUMAN column that cuts the noise, and it expires (next section).");
    }

    // ---------------- policy: gate with expiring exceptions ----------------
    record Finding(String id, String sev, boolean fixAvailable) { }
    record Waiver(String id, String until, String reason) { }

    static List<String> gate(List<Finding> fs, List<Waiver> ws, String today) {
        List<String> blocked = new ArrayList<>();
        for (Finding f : fs) {
            boolean severe = f.sev().equals("CRITICAL") || f.sev().equals("HIGH");
            if (!severe || !f.fixAvailable()) continue;
            Optional<Waiver> w = ws.stream().filter(x -> x.id().equals(f.id())).findFirst();
            if (w.isEmpty()) blocked.add(f.id() + " " + f.sev() + ": fix available, no waiver");
            else if (w.get().until().compareTo(today) < 0) blocked.add(f.id() + " " + f.sev() + ": waiver EXPIRED on " + w.get().until() + " (" + w.get().reason() + ")");
        }
        return blocked;
    }

    static void policy() {
        System.out.println("MODEL: the CI gate. Blocks HIGH/CRITICAL findings that HAVE a fix, unless a waiver (with a reason and an expiry date) covers them.\n");
        List<Finding> fs = List.of(new Finding("SYN-0001", "HIGH", true), new Finding("SYN-0002", "MEDIUM", true), new Finding("SYN-0010", "CRITICAL", false), new Finding("SYN-0011", "CRITICAL", true));
        List<Waiver> ws = List.of(new Waiver("SYN-0001", "2026-12-15", "not reachable until the parser feature ships; ticket SEC-88"), new Waiver("SYN-0011", "2026-12-10", "upgrade scheduled"));
        for (String today : new String[]{"2026-11-23", "2026-12-20"}) {
            List<String> b = gate(fs, ws, today);
            System.out.println("today = " + today + " -> " + (b.isEmpty() ? "PASS" : "FAIL (" + b.size() + ")"));
            b.forEach(x -> System.out.println("   " + x));
        }
        System.out.println("\nSYN-0002 (MEDIUM) never blocks; SYN-0010 (CRITICAL, no fix) does not block either (nothing to do but mitigate: a ticket, not a red build).");
        System.out.println("A waiver that never expires is a hidden decision to accept the risk forever.");
    }

    public static void main(String[] a) throws Exception {
        switch (a.length > 0 ? a[0] : "sast") { case "dast" -> dast(); case "sca" -> sca(); case "policy" -> policy(); default -> sast(); }
        System.exit(0);
    }
}
