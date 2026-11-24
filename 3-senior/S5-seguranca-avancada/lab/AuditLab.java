import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** S5.06 lab. Modes: chain | lint.  chain: a tamper-evident audit log (HMAC hash chain) and which attacks it detects (with and without an external anchor).
 *  lint: schema checks for an audit event (required fields, no card number or CPF inside). A model of the mechanism, not the PayFlow's table. */
public class AuditLab {
    record Event(long seq, String ts, String actor, String action, String resource, String outcome, String requestId, String prev, String mac) {
        String body() { return seq + "|" + ts + "|" + actor + "|" + action + "|" + resource + "|" + outcome + "|" + requestId + "|" + prev; }
        Event withMac(String m) { return new Event(seq, ts, actor, action, resource, outcome, requestId, prev, m); }
    }
    record Anchor(int count, String headMac) { }

    static String hmac(byte[] key, String data) {
        try { Mac m = Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(key, "HmacSHA256")); return HexFormat.of().formatHex(m.doFinal(data.getBytes(StandardCharsets.UTF_8)), 0, 16); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    static List<Event> build(byte[] key, int n) {
        List<Event> l = new ArrayList<>(); String prev = "GENESIS";
        String[] actors = {"ana", "bia", "sue", "adm", "payflow-api"}, actions = {"READ_ACCOUNT", "TRANSFER", "EXPORT_STATEMENT", "ADMIN_DELETE", "LOGIN"};
        for (int i = 0; i < n; i++) {
            Event e = new Event(i, String.format("2026-11-23T10:%02d:%02dZ", i / 60 % 60, i % 60), actors[i % 5], actions[(i / 5) % 5], "account/" + (1000 + i % 37), i % 11 == 0 ? "DENIED" : "ALLOWED", "req-" + i, prev, "");
            e = e.withMac(hmac(key, e.body())); l.add(e); prev = e.mac();
        }
        return l;
    }
    /** returns -1 if the chain verifies, else the index where it first breaks. With an anchor, also checks count and head. */
    static int verify(byte[] key, List<Event> l, Anchor anchor) {
        String prev = "GENESIS";
        for (int i = 0; i < l.size(); i++) {
            Event e = l.get(i);
            if (e.seq() != i || !e.prev().equals(prev) || !hmac(key, e.body()).equals(e.mac())) return i;
            prev = e.mac();
        }
        if (anchor != null && (l.size() != anchor.count() || !prev.equals(anchor.headMac()))) return Math.min(l.size(), anchor.count());
        return -1;
    }
    /** an attacker who HAS the key rewrites the chain from `from` on, so every link is consistent again. */
    static List<Event> rewriteWithKey(byte[] key, List<Event> l, int from) {
        List<Event> out = new ArrayList<>(l.subList(0, from)); String prev = from == 0 ? "GENESIS" : l.get(from - 1).mac();
        for (int i = from; i < l.size(); i++) {
            Event e = l.get(i);
            if (i == from) e = new Event(e.seq(), e.ts(), e.actor(), e.action(), e.resource(), "ALLOWED", e.requestId(), prev, "");   // the denied becomes allowed
            else e = new Event(e.seq(), e.ts(), e.actor(), e.action(), e.resource(), e.outcome(), e.requestId(), prev, "");
            e = e.withMac(hmac(key, e.body())); out.add(e); prev = e.mac();
        }
        return out;
    }

    static void chain() {
        byte[] key = new byte[32]; new Random(3).nextBytes(key);
        int N = 1000;
        List<Event> log = build(key, N);
        Anchor anchor = new Anchor(N, log.get(N - 1).mac());
        System.out.println("MODEL: " + N + " events, each carries prev (the previous event's MAC) and mac = HMAC(key, event fields + prev). 'anchor' = (count, head MAC) copied to a place the attacker cannot write (e.g. an S3 object with Object Lock).");
        System.out.println("Detected = the verifier reports the first broken index (or the anchor mismatch). The writer has the key; the attacker is someone who can edit the table.\n");
        System.out.println("untouched log: chain only -> " + show(verify(key, log, null)) + " | chain+anchor -> " + show(verify(key, log, anchor)) + "\n");
        System.out.println(String.format("%-58s | %-22s | %s", "attack (on the stored rows)", "chain only", "chain + anchor"));
        row("A1 edit event 495 (DENIED -> ALLOWED), no key", mut(log, l -> l.set(495, new Event(495, l.get(495).ts(), l.get(495).actor(), l.get(495).action(), l.get(495).resource(), "ALLOWED", l.get(495).requestId(), l.get(495).prev(), l.get(495).mac()))), key, anchor);
        row("A2 delete event 500", mut(log, l -> l.remove(500)), key, anchor);
        row("A3 swap events 500 and 501", mut(log, l -> Collections.swap(l, 500, 501)), key, anchor);
        row("A4 delete the LAST 50 events (truncate the tail)", mut(log, l -> { for (int i = 0; i < 50; i++) l.remove(l.size() - 1); }), key, anchor);
        row("A5 append a forged event, without the key", mut(log, l -> l.add(new Event(N, "2026-11-23T11:00:00Z", "adm", "ADMIN_DELETE", "account/1", "ALLOWED", "req-x", l.get(N - 1).mac(), "00"))), key, anchor);
        row("A6 attacker WITH the key rewrites from event 495", rewriteWithKey(key, log, 495), key, anchor);
        System.out.println("\nReading:");
        System.out.println("- Event 495 is one of the DENIED ones (every 11th event is). An earlier version of this lab edited event 500, which was already ALLOWED: the 'attack' changed nothing and the table said 'NOT DETECTED' for the wrong reason.");
        System.out.println("- The chain alone catches edits, deletions in the middle, reordering and forged appends (A1-A3, A5).");
        System.out.println("- A4 (cutting the tail) and A6 (whoever holds the key) pass the chain: only the anchor, kept somewhere the attacker cannot write, exposes them.");
        System.out.println("- So the key must live outside the database (KMS, S5.03), the anchor outside the attacker's reach, and the anchor must be taken often: events after the last anchor are not covered.");
        long t0 = System.nanoTime(); build(key, 20_000); double us = (System.nanoTime() - t0) / 1e3 / 20_000;
        System.out.printf("Cost: building the chain took %.1f us/event here (HMAC-SHA256 + string work; ms vary run to run). Verification is the same work.%n", us);
    }
    static String show(int r) { return r < 0 ? "verifies" : "BREAKS at " + r; }
    static List<Event> mut(List<Event> l, java.util.function.Consumer<List<Event>> f) { List<Event> c = new ArrayList<>(l); f.accept(c); return c; }
    static void row(String name, List<Event> attacked, byte[] key, Anchor anchor) {
        int a = verify(key, attacked, null), b = verify(key, attacked, anchor);
        System.out.println(String.format("%-58s | %-22s | %s", name, a < 0 ? "NOT DETECTED" : "detected at " + a, b < 0 ? "NOT DETECTED" : "detected at " + b));
    }

    // ---------------- lint ----------------
    static final Set<String> OUTCOMES = Set.of("ALLOWED", "DENIED", "ERROR");
    static final Pattern CARD = Pattern.compile("(?<!\\d)\\d(?:[ -]?\\d){12,18}(?!\\d)");
    static final Pattern CPF = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}");

    static List<String> lintEvent(Map<String, String> e) {
        List<String> p = new ArrayList<>();
        for (String f : List.of("ts", "actor", "action", "resource", "outcome", "requestId")) if (e.get(f) == null || e.get(f).isBlank()) p.add("missing field: " + f);
        if (e.get("outcome") != null && !OUTCOMES.contains(e.get("outcome"))) p.add("outcome must be ALLOWED/DENIED/ERROR, got " + e.get("outcome"));
        String d = e.getOrDefault("detail", "");
        if (CARD.matcher(d).find()) p.add("detail contains a card-number-like sequence");
        if (CPF.matcher(d).find()) p.add("detail contains a CPF-like sequence");
        if (e.get("actor") != null && e.get("actor").matches(".+@.+")) p.add("actor is an e-mail: use a stable id (personal data in the audit trail)");
        return p;
    }

    static void lint() {
        System.out.println("MODEL: what a well-formed audit event looks like, and a linter that rejects the bad ones (used in tests of the code that WRITES audit events).\n");
        Map<String, String> good = Map.of("ts", "2026-11-23T10:00:00Z", "actor", "user:42", "action", "EXPORT_STATEMENT", "resource", "account/1007", "outcome", "ALLOWED", "requestId", "req-91", "detail", "range=2026-10;format=csv");
        Map<String, String> denied = Map.of("ts", "2026-11-23T10:00:01Z", "actor", "user:43", "action", "READ_ACCOUNT", "resource", "account/1007", "outcome", "DENIED", "requestId", "req-92", "detail", "reason=not the owner");
        Map<String, String> noActor = Map.of("ts", "2026-11-23T10:00:02Z", "action", "TRANSFER", "resource", "account/1", "outcome", "ALLOWED", "requestId", "req-93");
        Map<String, String> card = Map.of("ts", "2026-11-23T10:00:03Z", "actor", "user:44", "action", "CHARGE", "resource", "account/2", "outcome", "ALLOWED", "requestId", "req-94", "detail", "card=4111 1111 1111 1111");
        Map<String, String> pii = Map.of("ts", "2026-11-23T10:00:04Z", "actor", "ana@example.com", "action", "LOGIN", "resource", "session", "outcome", "success", "requestId", "req-95", "detail", "cpf=123.456.780-62");
        int i = 0;
        for (Map<String, String> e : List.of(good, denied, noActor, card, pii)) {
            List<String> p = lintEvent(e);
            System.out.println("event " + (++i) + " (" + e.getOrDefault("action", "?") + "/" + e.getOrDefault("outcome", "?") + "): " + (p.isEmpty() ? "ok" : p.size() + " problem(s)"));
            p.forEach(x -> System.out.println("   - " + x));
        }
        System.out.println("\nReading: event 2 is the one teams forget: DENIED attempts are the evidence of an attack in progress, so the audit trail records refusals as well as successes.");
    }

    public static void main(String[] a) {
        switch (a.length > 0 ? a[0] : "chain") { case "lint" -> lint(); default -> chain(); }
    }
}
