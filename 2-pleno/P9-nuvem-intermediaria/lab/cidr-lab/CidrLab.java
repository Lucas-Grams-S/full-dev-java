import java.util.ArrayList;
import java.util.List;

/**
 * P9.01 "logic pause": subnet arithmetic with bits, no library.  Run: java CidrLab.java
 * Every claim printed here is ALSO asserted; a wrong plan fails the program (exit 1).
 */
public class CidrLab {

    record Cidr(long base, int prefix) {

        static Cidr parse(String text) {
            String[] parts = text.split("/");
            String[] octets = parts[0].split("\\.");
            long ip = 0;
            for (String o : octets) {
                ip = (ip << 8) | Integer.parseInt(o);
            }
            int prefix = Integer.parseInt(parts[1]);
            long mask = mask(prefix);
            if ((ip & mask) != ip) {
                throw new IllegalArgumentException(text + " has host bits set; did you mean " + format(ip & mask) + "/" + prefix + "?");
            }
            return new Cidr(ip, prefix);
        }

        static long mask(int prefix) {
            return prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        }

        static String format(long ip) {
            return ((ip >> 24) & 255) + "." + ((ip >> 16) & 255) + "." + ((ip >> 8) & 255) + "." + (ip & 255);
        }

        long size() {
            return 1L << (32 - prefix);
        }

        long last() {
            return base + size() - 1;
        }

        /** AWS reserves 5 addresses per subnet: network, router, DNS, future use, broadcast. */
        long usableOnAws() {
            return size() - 5;
        }

        boolean overlaps(Cidr other) {
            return base <= other.last() && other.base <= last();
        }

        boolean contains(Cidr other) {
            return base <= other.base && other.last() <= last();
        }

        /** Same as Terraform's cidrsubnet(prefix, newbits, netnum). */
        Cidr subnet(int newBits, int netNum) {
            int newPrefix = prefix + newBits;
            if (newPrefix > 32 || netNum >= (1L << newBits)) {
                throw new IllegalArgumentException("does not fit");
            }
            return new Cidr(base + ((long) netNum << (32 - newPrefix)), newPrefix);
        }

        @Override
        public String toString() {
            return format(base) + "/" + prefix;
        }
    }

    static int failures = 0;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }

    public static void main(String[] args) {
        Cidr vpc = Cidr.parse("10.20.0.0/16");
        System.out.println("VPC " + vpc + ": " + vpc.size() + " addresses");

        System.out.println("\n1) the plan of the PayFlow VPC (what cidrsubnet() produced in the network module)");
        List<Cidr> plan = new ArrayList<>();
        String[] names = {"public-a", "public-b", "app-a", "app-b", "data-a", "data-b"};
        int[] nets = {0, 1, 10, 11, 20, 21};
        for (int i = 0; i < nets.length; i++) {
            Cidr c = vpc.subnet(8, nets[i]);
            plan.add(c);
            System.out.printf("  %-9s %-16s size=%4d  usable on AWS=%4d  (%s .. %s)%n", names[i], c, c.size(), c.usableOnAws(),
                    Cidr.format(c.base()), Cidr.format(c.last()));
        }
        check(vpc.subnet(8, 10).toString().equals("10.20.10.0/24"), "cidrsubnet(\"10.20.0.0/16\", 8, 10) == 10.20.10.0/24 (confirmed against terraform console below)");
        boolean noOverlap = true;
        for (int i = 0; i < plan.size(); i++) {
            for (int j = i + 1; j < plan.size(); j++) {
                noOverlap &= !plan.get(i).overlaps(plan.get(j));
            }
            noOverlap &= vpc.contains(plan.get(i));
        }
        check(noOverlap, "6 subnets: no overlap, all inside the VPC");
        check(plan.get(2).usableOnAws() == 251, "a /24 has 256 - 5 = 251 usable addresses");

        System.out.println("\n2) how many IPs does the app subnet need during a deploy?");
        int maxTasks = 6;                 // autoscaling max_count
        int deployFactor = 2;             // deployment_maximum_percent = 200: old + new tasks coexist
        int endpointEnis = 6;             // interface endpoints with a network interface in this subnet (1 AZ in study, 2 in production)
        int perAzTasks = (int) Math.ceil(maxTasks / 2.0) * deployFactor;
        long need = perAzTasks + endpointEnis;
        System.out.printf("  tasks per AZ at max during a rolling deploy: %d, endpoint ENIs: %d -> %d of %d usable%n",
                perAzTasks, endpointEnis, need, plan.get(2).usableOnAws());
        check(need < plan.get(2).usableOnAws(), "the /24 app subnet is comfortable");
        Cidr tiny = vpc.subnet(12, 800); // a /28
        long tinyNeed = perAzTasks + endpointEnis;
        System.out.printf("  a /28 (%s) would have %d usable: %s%n", tiny, tiny.usableOnAws(), tinyNeed <= tiny.usableOnAws() ? "fits" : "does NOT fit -> deploy would stall");
        check(tinyNeed > tiny.usableOnAws(), "a /28 is too small for this service");

        System.out.println("\n3) the load balancer rule: each subnet /27 or larger with >= 8 free IPs (ELB documentation)");
        Cidr slash27 = vpc.subnet(11, 5);
        check(slash27.prefix() == 27 && slash27.usableOnAws() == 27, "a /27 has 32 - 5 = 27 usable, enough for the 8 the ALB needs to scale");
        check(Cidr.parse("10.20.0.0/24").prefix() <= 27, "public /24 satisfies the load balancer minimum");

        System.out.println("\n4) a mistake you will make once: host bits set");
        try {
            Cidr.parse("10.20.5.7/24");
            check(false, "10.20.5.7/24 should be rejected");
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains("10.20.5.0/24"), "10.20.5.7/24 -> " + e.getMessage());
        }

        System.out.println("\n5) peering/VPN later: overlapping VPC ranges cannot be connected");
        Cidr corporate = Cidr.parse("10.20.0.0/14");
        check(vpc.overlaps(corporate), "10.20.0.0/16 overlaps the corporate 10.20.0.0/14: choose the VPC range BEFORE you need to connect it");

        System.out.println(failures == 0 ? "\nALL CHECKS PASSED" : "\n" + failures + " CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
