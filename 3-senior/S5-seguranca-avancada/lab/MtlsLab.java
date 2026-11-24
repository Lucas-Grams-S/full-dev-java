import com.sun.net.httpserver.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** S5.02 lab: REAL mutual TLS with the JDK (HttpsServer + HttpClient) and certificates made with keytool.
 *  Modes: cases | cost. Everything is local (127.0.0.1) and the CA is a throwaway. Not the PayFlow. */
public class MtlsLab {
    static Path dir;
    static final String PW = "changeit";
    static String eku = "serverAuth,clientAuth";
    static final String KT = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();

    static void kt(String... args) throws Exception {
        List<String> cmd = new ArrayList<>(List.of(KT));
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).directory(dir.toFile()).redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes());
        if (p.waitFor() != 0) throw new IllegalStateException("keytool failed: " + String.join(" ", args) + "\n" + out);
    }

    static void makeCa(String name) throws Exception {
        kt("-genkeypair", "-alias", name, "-keyalg", "EC", "-groupname", "secp256r1", "-dname", "CN=" + name,
           "-ext", "bc:c", "-ext", "ku:c=keyCertSign,cRLSign", "-validity", "30",
           "-keystore", name + ".p12", "-storetype", "PKCS12", "-storepass", PW);
        kt("-exportcert", "-rfc", "-alias", name, "-keystore", name + ".p12", "-storepass", PW, "-file", name + ".pem");
    }

    /** leaf issued by `ca`: keystore `<cn>-by-<ca>.p12` holds key + chain. extra = keytool args (e.g. -startdate, -validity). */
    static String makeLeaf(String cn, String san, String ca, String... extra) throws Exception {
        String ks = cn + "-by-" + ca + ".p12";
        kt("-genkeypair", "-alias", cn, "-keyalg", "EC", "-groupname", "secp256r1", "-dname", "CN=" + cn,
           "-validity", "30", "-keystore", ks, "-storetype", "PKCS12", "-storepass", PW);
        kt("-certreq", "-alias", cn, "-keystore", ks, "-storepass", PW, "-file", cn + ".csr");
        List<String> g = new ArrayList<>(List.of("-gencert", "-alias", ca, "-keystore", ca + ".p12", "-storepass", PW,
            "-infile", cn + ".csr", "-outfile", cn + "-by-" + ca + ".pem", "-rfc",
            "-ext", "san=" + san, "-ext", "eku=" + eku, "-ext", "ku:c=digitalSignature"));
        if (!Arrays.asList(extra).contains("-validity")) g.addAll(List.of("-validity", "7"));   // short-lived leaf: 7 days
        g.addAll(List.of(extra));
        kt(g.toArray(new String[0]));
        kt("-importcert", "-noprompt", "-alias", ca, "-file", ca + ".pem", "-keystore", ks, "-storepass", PW);
        kt("-importcert", "-noprompt", "-alias", cn, "-file", cn + "-by-" + ca + ".pem", "-keystore", ks, "-storepass", PW);
        return ks;
    }

    static KeyStore load(String file) throws Exception {
        KeyStore k = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(dir.resolve(file))) { k.load(in, PW.toCharArray()); }
        return k;
    }

    static KeyStore trustOnly(String caPem) throws Exception {
        KeyStore k = KeyStore.getInstance("PKCS12");
        k.load(null, null);
        try (InputStream in = Files.newInputStream(dir.resolve(caPem))) {
            k.setCertificateEntry("ca", CertificateFactory.getInstance("X.509").generateCertificate(in));
        }
        return k;
    }

    static SSLContext ctx(String keystore, String trustedCaPem, String protocol) throws Exception {
        KeyManager[] kms = null;
        if (keystore != null) {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(load(keystore), PW.toCharArray());
            kms = kmf.getKeyManagers();
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustOnly(trustedCaPem));
        SSLContext c = SSLContext.getInstance(protocol);
        c.init(kms, tmf.getTrustManagers(), null);
        return c;
    }

    /** server: authn by mTLS (needClientAuth), authz by an allow-list of caller identities on /internal. */
    static HttpsServer server(String keystore, String clientCa, boolean needClientAuth, Set<String> allowed) throws Exception {
        HttpsServer s = HttpsServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        SSLContext c = ctx(keystore, clientCa, "TLS");
        s.setHttpsConfigurator(new HttpsConfigurator(c) {
            @Override public void configure(HttpsParameters p) {
                SSLParameters sp = c.getDefaultSSLParameters();
                if (needClientAuth) sp.setNeedClientAuth(true);
                p.setSSLParameters(sp);
            }
        });
        s.createContext("/internal/ping", ex -> {
            String who = "anonymous";
            try {
                if (ex instanceof HttpsExchange hx && hx.getSSLSession().getPeerCertificates().length > 0) {
                    who = ((X509Certificate) hx.getSSLSession().getPeerCertificates()[0]).getSubjectX500Principal().getName();
                }
            } catch (SSLPeerUnverifiedException ignored) { }
            int code = allowed.contains(who) ? 200 : 403;
            byte[] body = (code == 200 ? "pong to " + who : "forbidden: " + who + " is not allowed on /internal/ping").getBytes();
            ex.sendResponseHeaders(code, body.length);
            try (OutputStream o = ex.getResponseBody()) { o.write(body); }
        });
        s.start();
        return s;
    }

    static String call(SSLContext c, int port, String host, String sslProtocol) {
        try {
            SSLParameters sp = new SSLParameters();
            if (sslProtocol != null) sp.setProtocols(new String[]{sslProtocol});
            HttpClient hc = HttpClient.newBuilder().sslContext(c).sslParameters(sp).connectTimeout(Duration.ofSeconds(3)).build();
            HttpResponse<String> r = hc.send(HttpRequest.newBuilder(URI.create("https://" + host + ":" + port + "/internal/ping"))
                .timeout(Duration.ofSeconds(3)).build(), HttpResponse.BodyHandlers.ofString());
            return r.statusCode() + " " + r.body();
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null && t.getCause() != t) t = t.getCause();
            String m = String.valueOf(t.getMessage()).replace('\n', ' ');
            return "FAILED (" + t.getClass().getSimpleName() + ": " + (m.length() > 110 ? m.substring(0, 110) + "..." : m) + ")";
        }
    }

    static void cases() throws Exception {
        makeCa("payflow-ca"); makeCa("rogue-ca");
        makeLeaf("ledger", "dns:localhost,ip:127.0.0.1", "payflow-ca");
        makeLeaf("ledger-wrong-name", "dns:ledger.internal", "payflow-ca");
        makeLeaf("api", "dns:api.internal", "payflow-ca");
        makeLeaf("reporting", "dns:reporting.internal", "payflow-ca");
        makeLeaf("api", "dns:api.internal", "rogue-ca");
        makeLeaf("api-expired", "dns:api.internal", "payflow-ca", "-startdate", "-3d", "-validity", "1");
        eku = "serverAuth";                                           // a certificate NOT meant for client authentication
        makeLeaf("api-serveronly", "dns:api.internal", "payflow-ca");
        eku = "serverAuth,clientAuth";
        Set<String> allowed = Set.of("CN=api");

        System.out.println("MODEL: throwaway CA made with keytool (EC P-256); server = JDK HttpsServer with needClientAuth; clients = java.net.http.HttpClient.");
        System.out.println("Server trusts ONLY payflow-ca. /internal/ping allows only CN=api (authorization is a separate list).\n");

        HttpsServer s = server("ledger-by-payflow-ca.p12", "payflow-ca.pem", true, allowed);
        int port = s.getAddress().getPort();
        String h = "localhost";
        row("1. api cert issued by payflow-ca", call(ctx("api-by-payflow-ca.p12", "payflow-ca.pem", "TLS"), port, h, null));
        row("2. reporting cert (valid, same CA, NOT on the list)", call(ctx("reporting-by-payflow-ca.p12", "payflow-ca.pem", "TLS"), port, h, null));
        row("3. no client certificate", call(ctx(null, "payflow-ca.pem", "TLS"), port, h, null));
        row("4. api cert issued by rogue-ca (same CN=api!)", call(ctx("api-by-rogue-ca.p12", "payflow-ca.pem", "TLS"), port, h, null));
        row("5. api cert already expired (issued 3 days ago, valid 1 day)", call(ctx("api-expired-by-payflow-ca.p12", "payflow-ca.pem", "TLS"), port, h, null));
        row("6. client forced to TLSv1.2", call(ctx("api-by-payflow-ca.p12", "payflow-ca.pem", "TLS"), port, h, "TLSv1.2"));
        row("7. client forced to TLSv1.1", call(ctx("api-by-payflow-ca.p12", "payflow-ca.pem", "TLS"), port, h, "TLSv1.1"));
        row("8. client does not trust payflow-ca (trusts rogue-ca only)", call(ctx("api-by-payflow-ca.p12", "rogue-ca.pem", "TLS"), port, h, null));
        row("9. valid cert from payflow-ca but extendedKeyUsage = serverAuth only", call(ctx("api-serveronly-by-payflow-ca.p12", "payflow-ca.pem", "TLS"), port, h, null));
        s.stop(0);

        HttpsServer wrong = server("ledger-wrong-name-by-payflow-ca.p12", "payflow-ca.pem", true, allowed);
        row("10. server cert has SAN ledger.internal, client calls localhost", call(ctx("api-by-payflow-ca.p12", "payflow-ca.pem", "TLS"), wrong.getAddress().getPort(), h, null));
        wrong.stop(0);

        HttpsServer tlsOnly = server("ledger-by-payflow-ca.p12", "payflow-ca.pem", false, allowed);
        row("11. server WITHOUT needClientAuth, client sends no cert (plain TLS)", call(ctx(null, "payflow-ca.pem", "TLS"), tlsOnly.getAddress().getPort(), h, null));
        tlsOnly.stop(0);

        System.out.println("\nNegotiated parameters (api -> ledger):");
        HttpsServer s2 = server("ledger-by-payflow-ca.p12", "payflow-ca.pem", true, allowed);
        SSLSocket sock = (SSLSocket) ctx("api-by-payflow-ca.p12", "payflow-ca.pem", "TLS").getSocketFactory().createSocket("localhost", s2.getAddress().getPort());
        sock.startHandshake();
        SSLSession ss = sock.getSession();
        System.out.println("  protocol      = " + ss.getProtocol());
        System.out.println("  cipher suite  = " + ss.getCipherSuite());
        X509Certificate peer = (X509Certificate) ss.getPeerCertificates()[0];
        System.out.println("  server cert   = " + peer.getSubjectX500Principal() + ", key = " + peer.getPublicKey().getAlgorithm() + ", sigalg = " + peer.getSigAlgName());
        Certificate[] local = ss.getLocalCertificates();
        System.out.println("  client cert   = " + ((X509Certificate) local[0]).getSubjectX500Principal() + " (sent in the handshake)");
        sock.close(); s2.stop(0);
        System.out.println("  disabled algorithms (jdk.tls.disabledAlgorithms) start with: "
            + Security.getProperty("jdk.tls.disabledAlgorithms").substring(0, 60) + "...");

        System.out.println("\nExpiry check (what a monitor would compute from the keystores):");
        for (String k : List.of("api-by-payflow-ca.p12", "api-expired-by-payflow-ca.p12")) {
            X509Certificate x = (X509Certificate) load(k).getCertificate(k.substring(0, k.indexOf("-by-")));
            long hours = Duration.between(java.time.Instant.now(), x.getNotAfter().toInstant()).toHours();
            System.out.println("  " + String.format("%-32s", k) + " notAfter in " + hours + " h" + (hours < 0 ? "  <-- EXPIRED" : ""));
        }
    }

    static void row(String label, String result) { System.out.println(String.format("%-68s -> %s", label, result)); }

    static void cost() throws Exception {
        makeCa("payflow-ca");
        makeLeaf("ledger", "dns:localhost,ip:127.0.0.1", "payflow-ca");
        makeLeaf("api", "dns:api.internal", "payflow-ca");
        int n = 200;
        System.out.println("MODEL: " + n + " operations per row over loopback on a shared machine; ms vary run to run.");
        System.out.println("handshake = raw SSLSocket.startHandshake() with a NEW SSLContext each time (so no session resumption); request = HttpClient on a reused connection.");
        for (boolean mtls : new boolean[]{false, true}) {
            HttpsServer s = server("ledger-by-payflow-ca.p12", "payflow-ca.pem", mtls, Set.of("CN=api", "anonymous"));
            int port = s.getAddress().getPort();
            KeyManager[] kms = null;
            if (mtls) { KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); kmf.init(load("api-by-payflow-ca.p12"), PW.toCharArray()); kms = kmf.getKeyManagers(); }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); tmf.init(trustOnly("payflow-ca.pem"));
            for (int i = 0; i < 40; i++) handshake(kms, tmf, port);
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) handshake(kms, tmf, port);
            double hs = (System.nanoTime() - t0) / 1e6 / n;
            SSLContext c = ctx(mtls ? "api-by-payflow-ca.p12" : null, "payflow-ca.pem", "TLS");
            HttpClient hc = HttpClient.newBuilder().sslContext(c).build();
            HttpRequest rq = HttpRequest.newBuilder(URI.create("https://localhost:" + port + "/internal/ping")).build();
            for (int i = 0; i < 40; i++) hc.send(rq, HttpResponse.BodyHandlers.discarding());
            t0 = System.nanoTime();
            for (int i = 0; i < n; i++) hc.send(rq, HttpResponse.BodyHandlers.discarding());
            double reuse = (System.nanoTime() - t0) / 1e6 / n;
            System.out.printf("%-30s full handshake: %6.2f ms | request on a reused connection: %5.2f ms%n",
                mtls ? "mTLS (client cert verified)" : "TLS only (server auth)", hs, reuse);
            s.stop(0);
        }
        System.out.println("Reading: the gap between the two rows is inside the run-to-run noise on this machine; what the numbers DO show is that the handshake costs several ms and a reused connection about 1 ms, so pool connections.");
    }

    static void handshake(KeyManager[] kms, TrustManagerFactory tmf, int port) throws Exception {
        SSLContext c = SSLContext.getInstance("TLS");
        c.init(kms, tmf.getTrustManagers(), null);
        try (SSLSocket sock = (SSLSocket) c.getSocketFactory().createSocket("localhost", port)) { sock.startHandshake(); }
    }

    public static void main(String[] a) throws Exception {
        dir = Files.createTempDirectory("s5-mtls");
        try {
            switch (a.length > 0 ? a[0] : "cases") { case "cost" -> cost(); default -> cases(); }
        } finally {
            try (var w = Files.walk(dir)) { w.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete()); }
        }
        System.exit(0);
    }
}
