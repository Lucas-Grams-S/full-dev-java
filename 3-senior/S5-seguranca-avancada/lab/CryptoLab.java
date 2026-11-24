import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.HexFormat;

/** S5.03 lab: envelope encryption with AES-256-GCM, a LOCAL stand-in for a KMS (no AWS), nonce reuse, blind index, cost.
 *  Modes: envelope | nonce | blind | cost. A model of the mechanism, not the PayFlow and not AWS KMS. */
public class CryptoLab {
    static final SecureRandom RNG = new SecureRandom();
    static final HexFormat HEX = HexFormat.of();

    /** Stand-in for a KMS: it holds the KEKs (key encryption keys) and NEVER hands them out; callers only wrap/unwrap. */
    static class FakeKms {
        final Map<String, SecretKey> keks = new HashMap<>();
        int calls = 0;
        String current;
        FakeKms(String first) { rotateTo(first); }
        void rotateTo(String id) { keks.put(id, newKey()); current = id; }
        byte[] wrap(byte[] dek, String context) throws Exception { calls++; return seal(keks.get(current), dek, context); }          // output carries the key id
        byte[] unwrap(String kekId, byte[] wrapped, String context) throws Exception { calls++; return open(keks.get(kekId), wrapped, context); }
    }

    static SecretKey newKey() { byte[] k = new byte[32]; RNG.nextBytes(k); return new SecretKeySpec(k, "AES"); }

    /** nonce(12) || ciphertext || tag(16). aad = the "encryption context": bound to the ciphertext, not secret. */
    static byte[] seal(SecretKey key, byte[] plain, String aad) throws Exception {
        byte[] nonce = new byte[12]; RNG.nextBytes(nonce);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
        c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        byte[] ct = c.doFinal(plain);
        byte[] out = new byte[12 + ct.length];
        System.arraycopy(nonce, 0, out, 0, 12); System.arraycopy(ct, 0, out, 12, ct.length);
        return out;
    }
    static byte[] open(SecretKey key, byte[] blob, String aad) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOfRange(blob, 0, 12)));
        c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
        return c.doFinal(blob, 12, blob.length - 12);
    }

    record Stored(String kekId, byte[] wrappedDek, byte[] data) { }

    static Stored encryptField(FakeKms kms, String table, String id, String column, String value) throws Exception {
        String ctx = table + "|" + id + "|" + column;                 // encryption context: binds the value to its place
        SecretKey dek = newKey();                                     // one data key per record
        byte[] wrapped = kms.wrap(dek.getEncoded(), ctx);
        return new Stored(kms.current, wrapped, seal(dek, value.getBytes(StandardCharsets.UTF_8), ctx));
    }
    static String decryptField(FakeKms kms, Stored s, String table, String id, String column) throws Exception {
        String ctx = table + "|" + id + "|" + column;
        SecretKey dek = new SecretKeySpec(kms.unwrap(s.kekId, s.wrappedDek, ctx), "AES");
        return new String(open(dek, s.data, ctx), StandardCharsets.UTF_8);
    }
    static String tryDecrypt(FakeKms kms, Stored s, String t, String id, String col) {
        try { return "OK \"" + decryptField(kms, s, t, id, col) + "\""; }
        catch (Exception e) { return "FAILED (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")"; }
    }
    static String h(byte[] b) { return HEX.formatHex(b, 0, 6) + "..."; }

    static void envelope() throws Exception {
        FakeKms kms = new FakeKms("kek-1");
        Stored s = encryptField(kms, "users", "u-42", "phone_e164", "+5511999990000");
        System.out.println("MODEL: AES-256-GCM per record (DEK), DEK wrapped by a KEK that lives only inside the (fake) KMS; aad = \"table|id|column\".");
        System.out.println("stored: kekId=" + s.kekId + " wrappedDek=" + s.wrappedDek.length + " B data=" + s.data.length + " B  (value was 14 B: 12 nonce + 16 tag = 28 B overhead; wrapped DEK = 12 + 32 + 16 = 60 B)\n");
        System.out.println("1. right place            : " + tryDecrypt(kms, s, "users", "u-42", "phone_e164"));
        System.out.println("2. moved to another row   : " + tryDecrypt(kms, s, "users", "u-43", "phone_e164") + "   <- the context is part of the check");
        System.out.println("3. moved to another column: " + tryDecrypt(kms, s, "users", "u-42", "email") + "");
        Stored t = new Stored(s.kekId, s.wrappedDek, s.data.clone()); t.data[20] ^= 1;
        System.out.println("4. one flipped bit in data : " + tryDecrypt(kms, t, "users", "u-42", "phone_e164") + "   <- GCM authenticates, it does not only hide");
        Stored sw = new Stored(s.kekId, encryptField(kms, "users", "u-42", "other", "x").wrappedDek, s.data);
        System.out.println("5. wrapped DEK from another record: " + tryDecrypt(kms, sw, "users", "u-42", "phone_e164"));

        System.out.println("\nKEY ROTATION (1000 records): re-wrap the DEKs, leave the data alone");
        List<Stored> all = new ArrayList<>();
        for (int i = 0; i < 1000; i++) all.add(encryptField(kms, "users", "u-" + i, "phone_e164", "+55119999" + String.format("%05d", i)));
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        for (Stored x : all) md.update(x.data);
        String dataBefore = HEX.formatHex(md.digest(), 0, 8);
        kms.rotateTo("kek-2");
        long calls0 = kms.calls; long t0 = System.nanoTime();
        List<Stored> re = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            Stored x = all.get(i);
            byte[] dek = kms.unwrap(x.kekId, x.wrappedDek, "users|u-" + i + "|phone_e164");
            re.add(new Stored(kms.current, kms.wrap(dek, "users|u-" + i + "|phone_e164"), x.data));
        }
        double ms = (System.nanoTime() - t0) / 1e6;
        md.reset(); for (Stored x : re) md.update(x.data);
        System.out.println("  data digest before = " + dataBefore + ", after = " + HEX.formatHex(md.digest(), 0, 8) + "  (identical: no data byte was touched)");
        System.out.println("  KMS calls for the re-wrap = " + (kms.calls - calls0) + " (1 unwrap + 1 wrap per record); local time " + String.format("%.1f", ms) + " ms (a real KMS is a network call per operation)");
        System.out.println("  record 7 under kek-2     : " + tryDecrypt(kms, re.get(7), "users", "u-7", "phone_e164"));
        System.out.println("  old record 7 under kek-1 : " + tryDecrypt(kms, all.get(7), "users", "u-7", "phone_e164") + "   <- old KEK must stay until every DEK is re-wrapped");
        FakeKms attacker = new FakeKms("kek-1");
        System.out.println("  copy of the DB, stranger's KMS (its own kek-1): " + tryDecrypt(attacker, all.get(7), "users", "u-7", "phone_e164") + "   <- a database dump alone is useless");
    }

    static void nonce() throws Exception {
        System.out.println("MODEL: what happens when the SAME (key, nonce) encrypts two messages with AES-GCM. The JDK refuses a repeat on one Cipher object; we bypass it with two objects.");
        SecretKey k = newKey(); byte[] nonce = new byte[12]; RNG.nextBytes(nonce);
        byte[] p1 = "transfer 0100.00 to acc-17 ok  ".getBytes(), p2 = "transfer 9999.99 to acc-66 ok  ".getBytes();
        byte[] c1 = raw(k, nonce, p1), c2 = raw(k, nonce, p2);
        byte[] x = new byte[p1.length];
        for (int i = 0; i < x.length; i++) x[i] = (byte) (c1[i] ^ c2[i]);
        byte[] recovered = new byte[x.length];
        for (int i = 0; i < x.length; i++) recovered[i] = (byte) (x[i] ^ p1[i]);
        System.out.println("c1 xor c2           = " + HEX.formatHex(x, 9, 17) + "... (first 9 bytes are 0: both messages start with \"transfer \")");
        System.out.println("p1 (attacker knows) = \"" + new String(p1) + "\"");
        System.out.println("(c1 xor c2) xor p1  = \"" + new String(recovered) + "\"   <- p2 recovered WITHOUT the key");
        System.out.println("p2 (secret)         = \"" + new String(p2) + "\"");
        System.out.println("equal? " + Arrays.equals(recovered, p2));
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, k, new GCMParameterSpec(128, nonce)); c.doFinal(p1);
            c.init(Cipher.ENCRYPT_MODE, k, new GCMParameterSpec(128, nonce));
            System.out.println("JDK: re-init of the same Cipher with the same key+nonce -> allowed?!");
        } catch (Exception e) { System.out.println("JDK guard: re-init of the SAME Cipher with the same key+nonce -> " + e.getClass().getSimpleName() + ": " + e.getMessage()); }
        System.out.println("\nRandom 96-bit nonces under ONE key (birthday bound, math not measurement): P(repeat) ~ n^2 / 2^97");
        for (int e : new int[]{20, 32, 40}) {
            double n = Math.pow(2, e), p = n * n / Math.pow(2, 97);
            System.out.printf("  n = 2^%d messages -> P(repeat) ~ 2^%.0f  (%.2e)%n", e, Math.log(p) / Math.log(2), p);
        }
        System.out.println("NIST SP 800-38D limits random-nonce GCM to 2^32 invocations per key: that is why one DEK per record (or per few thousand) is the habit.");
    }
    static byte[] raw(SecretKey k, byte[] nonce, byte[] p) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, k, new GCMParameterSpec(128, nonce));
        return c.doFinal(p);
    }

    static void blind() throws Exception {
        System.out.println("MODEL: encrypted columns cannot be searched. A BLIND INDEX = HMAC-SHA256(secret, normalized value): equal values -> equal tags (lookup works), and that is also the leak.");
        byte[] key = new byte[32]; RNG.nextBytes(key);
        Mac m = Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(key, "HmacSHA256"));
        Map<String, Integer> freq = new TreeMap<>();
        String[] phones = {"+5511999990000", "+5511999990001", "+5521988887777"};
        int[] counts = {700, 200, 100};
        Map<String, String> tagToPhone = new HashMap<>();
        for (int i = 0; i < phones.length; i++) {
            String tag = HEX.formatHex(m.doFinal(phones[i].getBytes()), 0, 6);
            tagToPhone.put(tag, phones[i]);
            freq.put(tag, counts[i]);
        }
        System.out.println("1000 users, 3 distinct phones (a shared family/office line is common). What a DB dump shows in phone_idx:");
        freq.forEach((t, c) -> System.out.printf("  %s  x %d%n", t, c));
        System.out.println("-> the dump leaks WHO shares a value and HOW MANY (700/200/100) without the key; encrypting the column alone leaks neither.");
        String needle = HEX.formatHex(m.doFinal("+5511999990001".getBytes()), 0, 6);
        System.out.println("lookup of +5511999990001 -> tag " + needle + " -> " + freq.get(needle) + " row(s) found by equality (no decryption).");
        System.out.println("Without normalization the same phone has two tags: " + HEX.formatHex(m.doFinal("+5511999990001".getBytes()), 0, 6) + " vs " + HEX.formatHex(m.doFinal("(11) 99999-0001".getBytes()), 0, 6) + " (phone_e164 from S3.09 is the normalized input).");
        System.out.println("Unlike a plain SHA-256, the HMAC needs the secret: a 10^10-phone dictionary attack needs the key too.");
    }

    static void cost() throws Exception {
        System.out.println("MODEL: local AES-256-GCM on this machine (" + System.getProperty("os.arch") + ", JDK " + Runtime.version().feature() + "); ms vary run to run.");
        SecretKey k = newKey();
        byte[] small = "+5511999990000".getBytes();
        for (int i = 0; i < 50_000; i++) seal(k, small, "users|u-1|phone");
        long t0 = System.nanoTime(); int n = 200_000;
        for (int i = 0; i < n; i++) seal(k, small, "users|u-1|phone");
        System.out.printf("14-byte field (seal only, key reused): %.2f us/op%n", (System.nanoTime() - t0) / 1e3 / n);
        FakeKms kms = new FakeKms("kek-1");
        t0 = System.nanoTime(); n = 50_000;
        for (int i = 0; i < n; i++) encryptField(kms, "users", "u-1", "phone_e164", "+5511999990000");
        System.out.printf("14-byte field, envelope (new DEK + local wrap): %.2f us/op, KMS calls = %d%n", (System.nanoTime() - t0) / 1e3 / n, kms.calls);
        System.out.println("A REAL KMS call is a network round trip (ms, not us) with a quota: caching a data key and reusing it for many records trades fewer calls for a bigger blast radius per key.");
        for (int uses : new int[]{1, 100, 10_000}) System.out.printf("  10,000 records, data key reused for %5d records -> %5d KMS wrap calls (1 per key)%n", uses, (10_000 + uses - 1) / uses);
    }

    public static void main(String[] a) throws Exception {
        switch (a.length > 0 ? a[0] : "envelope") { case "nonce" -> nonce(); case "blind" -> blind(); case "cost" -> cost(); default -> envelope(); }
    }
}
