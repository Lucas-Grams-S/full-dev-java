import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.List;
import java.util.stream.Stream;

/**
 * P9.08: what the JVM does with DNS answers. An RDS Multi-AZ failover CHANGES the DNS record of the endpoint to the
 * standby; a JVM that keeps the old answer keeps talking to a dead address. Must run as root INSIDE a container
 * (it rewrites /etc/hosts to play the role of the DNS server).   java -cp . DnsTtl [max-seconds]
 */
public class DnsTtl {

    static final Path HOSTS = Path.of("/etc/hosts");
    static final String NAME = "payflow-db.lab.internal";

    static void map(String ip) throws Exception {
        List<String> keep;
        try (Stream<String> lines = Files.lines(HOSTS)) {
            keep = lines.filter(l -> !l.contains(NAME)).toList();
        }
        List<String> all = new java.util.ArrayList<>(keep);
        all.add(ip + " " + NAME);
        Files.write(HOSTS, all);
    }

    public static void main(String[] args) throws Exception {
        int max = args.length > 0 ? Integer.parseInt(args[0]) : 60;
        System.out.println("networkaddress.cache.ttl (security property) = " + Security.getProperty("networkaddress.cache.ttl"));
        map("10.0.0.1");
        System.out.println("t= 0s  resolves to " + InetAddress.getByName(NAME).getHostAddress() + "   (the primary)");
        map("10.0.0.2");
        System.out.println("t= 0s  [DNS record now points to the standby: this is the failover]");
        long t0 = System.nanoTime();
        for (int s = 1; s <= max; s++) {
            Thread.sleep(1000);
            String ip = InetAddress.getByName(NAME).getHostAddress();
            if (ip.equals("10.0.0.2")) {
                System.out.printf("t=%2ds  resolves to %s   <- the JVM finally saw the new address (%d s after the change)%n", s, ip, (System.nanoTime() - t0) / 1_000_000_000L);
                return;
            }
            if (s <= 2 || s % 10 == 0) {
                System.out.printf("t=%2ds  still %s   (cached)%n", s, ip);
            }
        }
        System.out.println("never refreshed within " + max + " s");
    }
}
