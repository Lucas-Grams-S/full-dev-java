import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * S6.04 — FinOps: estimativa, NAT × endpoints, tags, compromissos e Spot.
 * Preços: AWS Price List (prices.csv). Quantidades, tráfego, taxas de interrupção e o inventário são PREMISSAS.
 */
public class FinOpsLab {

    public static void main(String[] args) throws Exception {
        PriceBook pb = PriceBook.load();
        switch (args.length == 0 ? "estimate" : args[0]) {
            case "estimate" -> estimate(pb, Path.of(args.length > 1 ? args[1] : "samples/bom-payflow.csv"));
            case "nat" -> nat(pb);
            case "tags" -> System.exit(tags(Path.of(args[1])) == 0 ? 0 : 1);
            case "commit" -> commit(pb);
            case "spot" -> spot(pb);
            default -> throw new IllegalArgumentException("modos: estimate [BOM] | nat | tags ARQ | commit | spot");
        }
    }

    // ---------------------------------------------------------------- estimate
    record Line(String env, String service, String item, String key, double qty, String kind) {}

    static List<Line> bom(Path file) throws Exception {
        List<Line> out = new ArrayList<>();
        List<String> lines = Files.readAllLines(file);
        for (String l : lines.subList(1, lines.size())) {
            if (l.isBlank()) continue;
            String[] f = l.split(";", -1);
            out.add(new Line(f[0], f[1], f[2], f[3], Double.parseDouble(f[4]), f[5]));
        }
        return out;
    }

    static final double TRANSFERS_PER_MONTH = 10_000_000; // PREMISSA (a mesma do BOM)

    static void estimate(PriceBook pb, Path file) throws Exception {
        List<Line> lines = bom(file);
        List<String> regions = List.of("us-east-1", "sa-east-1");
        System.out.printf("Bill of materials %s: %d lines; prices from the AWS Price List (published %s)%n",
                file.getFileName(), lines.size(), pb.publishedRange());
        System.out.println("On demand, no free tier, no taxes, no support plan, no Savings Plans. Quantities are premises.");
        System.out.println();
        System.out.printf("%-10s %14s %14s%n", "env", regions.get(0), regions.get(1));
        Map<String, double[]> perEnv = new LinkedHashMap<>(), perService = new TreeMap<>();
        double[] total = new double[2], fixed = new double[2];
        for (Line l : lines) {
            for (int i = 0; i < 2; i++) {
                double c = l.qty() * pb.usd(l.key(), regions.get(i));
                perEnv.computeIfAbsent(l.env(), k -> new double[2])[i] += c;
                perService.computeIfAbsent(l.service(), k -> new double[2])[i] += c;
                total[i] += c;
                if (l.kind().equals("fixed")) fixed[i] += c;
            }
        }
        perEnv.forEach((e, c) -> System.out.printf("%-10s %14s %14s%n", e, usd(c[0]), usd(c[1])));
        System.out.printf("%-10s %14s %14s   (sa-east-1 = x%.2f)%n", "TOTAL", usd(total[0]), usd(total[1]), total[1] / total[0]);
        System.out.println();
        System.out.println("By service (sorted by sa-east-1):");
        perService.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue()[1], a.getValue()[1]))
                .forEach(e -> System.out.printf("  %-16s %12s %12s  %5.1f%%%n", e.getKey(), usd(e.getValue()[0]), usd(e.getValue()[1]), 100 * e.getValue()[1] / total[1]));
        System.out.println();
        System.out.println("Top 5 lines (sa-east-1):");
        lines.stream().sorted((a, b) -> Double.compare(b.qty() * pb.usd(b.key(), "sa-east-1"), a.qty() * pb.usd(a.key(), "sa-east-1")))
                .limit(5).forEach(l -> System.out.printf("  %-8s %-55s %12s%n", l.env(), l.item(), usd(l.qty() * pb.usd(l.key(), "sa-east-1"))));
        System.out.println();
        for (int i = 0; i < 2; i++) {
            double variable = total[i] - fixed[i];
            System.out.printf("%s: fixed %s (%.0f%%), variable %s; traffic x2 -> %s (x%.2f, not x2)%n", regions.get(i), usd(fixed[i]),
                    100 * fixed[i] / total[i], usd(variable), usd(fixed[i] + 2 * variable), (fixed[i] + 2 * variable) / total[i]);
        }
        double prod = perEnv.get("prod")[1];
        System.out.printf("Unit cost (sa-east-1, prod only): %s per 1,000 transfers (premise: %,.0f transfers/month)%n",
                usd(prod / (TRANSFERS_PER_MONTH / 1000)), TRANSFERS_PER_MONTH);
        System.out.printf("Unit cost (sa-east-1, all envs):  %s per 1,000 transfers%n", usd(total[1] / (TRANSFERS_PER_MONTH / 1000)));
    }

    // ---------------------------------------------------------------- nat
    static void nat(PriceBook pb) {
        System.out.println("Reaching AWS APIs (ECR, logs, Secrets Manager, SQS, SNS) from private subnets in 2 AZs.");
        System.out.println("S3 always through the gateway endpoint (no additional charge, VPC docs). Per month, 730 h.");
        for (String r : List.of("us-east-1", "sa-east-1")) {
            double epFixed = 6 * 2 * PriceBook.HOURS_PER_MONTH * pb.usd("vpc.endpoint_h", r);
            double nat2Fixed = 2 * PriceBook.HOURS_PER_MONTH * pb.usd("nat.h", r);
            double nat1Fixed = PriceBook.HOURS_PER_MONTH * pb.usd("nat.h", r);
            System.out.printf("%n%s: endpoint %s/h + %s/GB; NAT %s/h + %s/GB; inter-AZ %s/GB each way%n", r,
                    pb.usd("vpc.endpoint_h", r), pb.usd("vpc.endpoint_gb", r), pb.usd("nat.h", r), pb.usd("nat.gb", r), pb.usd("dt.inter_az_gb", r));
            System.out.printf("%10s %22s %22s %26s%n", "GB/month", "6 endpoints x 2 AZs", "2 NAT (one per AZ)", "1 NAT (no AZ redundancy)");
            for (double gb : List.of(0.0, 100.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0)) {
                double ep = epFixed + gb * pb.usd("vpc.endpoint_gb", r);
                double n2 = nat2Fixed + gb * pb.usd("nat.gb", r);
                double n1 = nat1Fixed + gb * pb.usd("nat.gb", r) + gb / 2 * 2 * pb.usd("dt.inter_az_gb", r); // metade cruza AZ, 2 sentidos
                System.out.printf("%10.0f %22s %22s %26s%n", gb, usd(ep), usd(n2), usd(n1));
            }
            double be2 = (epFixed - nat2Fixed) / (pb.usd("nat.gb", r) - pb.usd("vpc.endpoint_gb", r));
            double be1 = (epFixed - nat1Fixed) / (pb.usd("nat.gb", r) + pb.usd("dt.inter_az_gb", r) - pb.usd("vpc.endpoint_gb", r));
            System.out.printf("break-even: endpoints cheaper than 2 NAT above %.0f GB/month; than 1 NAT above %.0f GB/month%n", be2, be1);
        }
        System.out.println("(NAT also reaches the internet; endpoints do not. Each new AWS service in the code = one more endpoint pair.)");
    }

    // ---------------------------------------------------------------- tags
    static final List<String> REQUIRED = List.of("app", "env", "owner", "cost-center");
    static final Set<String> ENVS = Set.of("prod", "staging", "study", "shared");
    static final Set<String> DATA_CLASSES = Set.of("public", "internal", "personal", "financial");
    static final Pattern COST_CENTER = Pattern.compile("CC-\\d{4}");

    static int tags(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file);
        double total = 0, compliant = 0, untaggable = 0;
        int resources = 0, ok = 0;
        List<String> problems = new ArrayList<>();
        Map<String, Double> byCostCenter = new TreeMap<>();
        for (String l : lines.subList(1, lines.size())) {
            if (l.isBlank()) continue;
            String[] f = l.split(";", -1);
            String id = f[0], type = f[1];
            double usd = Double.parseDouble(f[2]);
            total += usd;
            if (id.startsWith("UNTAGGABLE/")) { untaggable += usd; continue; }
            resources++;
            Map<String, String> tags = new LinkedHashMap<>();
            if (!f[3].isBlank()) for (String kv : f[3].split("\\|")) { String[] p = kv.split("=", 2); tags.put(p[0], p[1]); }
            List<String> mine = new ArrayList<>();
            for (String k : REQUIRED) if (!tags.containsKey(k)) {
                String near = tags.keySet().stream().filter(t -> t.equalsIgnoreCase(k)).findFirst().map(t -> " (found '" + t + "': keys are case sensitive)").orElse("");
                mine.add("missing " + k + near);
            }
            if (tags.containsKey("env") && !ENVS.contains(tags.get("env"))) mine.add("env='" + tags.get("env") + "' not in " + new java.util.TreeSet<>(ENVS));
            if (tags.containsKey("cost-center") && !COST_CENTER.matcher(tags.get("cost-center")).matches()) mine.add("cost-center='" + tags.get("cost-center") + "' does not match CC-NNNN");
            if (Set.of("rds", "s3").contains(type) && !DATA_CLASSES.contains(tags.getOrDefault("data-class", ""))) mine.add("data store without a valid data-class");
            if (mine.isEmpty()) { ok++; compliant += usd; byCostCenter.merge(tags.get("cost-center"), usd, Double::sum); }
            else problems.add(String.format("%-26s %9s  %s", id, usd(usd), String.join("; ", mine)));
        }
        System.out.printf("tag policy: required %s; env in %s; cost-center CC-NNNN; data-class on rds/s3%n", REQUIRED, new java.util.TreeSet<>(ENVS));
        problems.forEach(p -> System.out.println("  VIOLATION " + p));
        System.out.printf("resources compliant: %d of %d (%.0f%%)%n", ok, resources, 100.0 * ok / resources);
        System.out.printf("cost attributable to a cost center: %s of %s (%.0f%%)%n", usd(compliant), usd(total), 100 * compliant / total);
        System.out.printf("cost that no tag can carry (data transfer, support, taxes): %s (%.0f%%): allocate by a written rule%n", usd(untaggable), 100 * untaggable / total);
        System.out.println("showback (compliant only): " + byCostCenter.entrySet().stream().map(e -> e.getKey() + " " + usd(e.getValue())).toList());
        System.out.println(problems.isEmpty() ? "RESULT: PASS" : "RESULT: FAIL (" + problems.size() + " resources)");
        return problems.size();
    }

    // ---------------------------------------------------------------- commit
    static void commit(PriceBook pb) {
        String r = "sa-east-1";
        double od = 0.5 * pb.usd("fargate.arm.vcpu_h", r) + pb.usd("fargate.arm.gb_h", r);
        double sp = 0.5 * pb.usd("sp.fargate.arm.vcpu_h.1y_nu", r) + pb.usd("sp.fargate.arm.gb_h.1y_nu", r);
        int[] tasks = new int[24];
        for (int h = 0; h < 24; h++) // produção na opção B da S6.01 (min 4, alvo 45%) + staging com 2 tarefas fixas
            tasks[h] = Math.max(4, (int) Math.ceil(WellArchitectedLab.DEMAND[h] / (0.5 * 0.45))) + 2;
        double days = PriceBook.HOURS_PER_MONTH / 24;
        System.out.printf("1. Compute Savings Plan, 1 year, no upfront, Fargate ARM task (0.5 vCPU + 1 GB), %s%n", r);
        System.out.printf("   task-hour: on demand US$ %.5f, Savings Plan US$ %.5f (-%.0f%%); hourly tasks: min %d, max %d, avg %.1f%n",
                od, sp, 100 * (1 - sp / od), Arrays.stream(tasks).min().orElseThrow(), Arrays.stream(tasks).max().orElseThrow(), Arrays.stream(tasks).average().orElseThrow());
        double base = 0;
        System.out.printf("   %-16s %14s %14s %10s %16s%n", "commit (tasks)", "commit $/h", "month total", "saving", "unused commit");
        for (int covered : List.of(0, 4, 6, 7, 8, 9, 10, 13)) {
            double c = covered * sp, month = 0, wasted = 0;
            for (int n : tasks) {
                month += c + Math.max(0, n - covered) * od;
                wasted += Math.max(0, covered - n) * sp;
            }
            month *= days; wasted *= days;
            if (covered == 0) base = month;
            System.out.printf("   %-16d %14.5f %14s %9.1f%% %16s%n", covered, c, usd(month), 100 * (1 - month / base), usd(wasted));
        }
        System.out.println("   (a commitment is paid every hour, used or not; above the floor part of it is wasted at night)");
        System.out.println();
        System.out.println("2. Reservations: minimum months the resource must keep running for the reservation to pay off");
        for (String reg : List.of("us-east-1", "sa-east-1")) {
            breakEven(pb, reg, "RDS db.m7g.large Multi-AZ, 1y no upfront", "rds.pg.m7g.large.multi_h", "rds.pg.m7g.large.multi.ri1y_nu_h", 12);
            breakEven(pb, reg, "EC2 m7g.large, 1y no upfront", "ec2.m7g.large.od_h", "ec2.m7g.large.ri1y_nu_h", 12);
            breakEven(pb, reg, "EC2 m7g.large, 3y no upfront", "ec2.m7g.large.od_h", "ec2.m7g.large.ri3y_nu_h", 36);
        }
        System.out.println("   (an RDS reservation cannot be cancelled; changing the instance class ends the discount: RDS docs)");
    }

    static void breakEven(PriceBook pb, String r, String label, String odKey, String riKey, int months) {
        double o = pb.usd(odKey, r), ri = pb.usd(riKey, r);
        System.out.printf("   %-10s %-42s on demand %.4f/h, reserved %.4f/h (-%.0f%%): pays off after %.1f of %d months%n",
                r, label, o, ri, 100 * (1 - ri / o), months * ri / o, months);
    }

    // ---------------------------------------------------------------- spot
    static void spot(PriceBook pb) {
        double work = 4.0, deadline = 8.0, checkpointEvery = 0.25, restart = 5.0 / 60; // PREMISSAS: horas
        int runs = 20_000;
        for (String r : List.of("us-east-1", "sa-east-1")) {
            double od = pb.usd("ec2.m7g.large.od_h", r), saving = pb.usd("spot.m7g.large.savings_pct", r) / 100;
            double spot = od * (1 - saving);
            System.out.printf("%s: m7g.large on demand US$ %.4f/h; Spot Advisor saving %.0f%% -> US$ %.4f/h (%s)%n", r, od, saving * 100, spot,
                    pb.price("spot.m7g.large.savings_pct", r).description().replaceAll(".*interrupção ", "interruption frequency "));
            System.out.printf("  job: %.0f h of work, deadline %.0f h; on demand = %s, always on time%n", work, deadline, usd(work * od));
            System.out.printf("  %-14s %-15s %10s %10s %12s%n", "hazard/hour", "checkpoint", "mean cost", "p95 time", "late runs");
            for (double hazard : List.of(0.01, 0.05, 0.20, 0.50)) {
                for (boolean cp : List.of(true, false)) {
                    Random rnd = new Random(42);
                    double[] times = new double[runs];
                    double costSum = 0;
                    int late = 0;
                    for (int i = 0; i < runs; i++) {
                        double done = 0, elapsed = 0;
                        while (done < work - 1e-9) {
                            double ttl = -Math.log(1 - rnd.nextDouble()) / hazard; // tempo até a interrupção (exponencial)
                            double need = work - done;
                            if (ttl >= need) { elapsed += need; done = work; break; }
                            elapsed += ttl + restart;
                            done = cp ? done + Math.floor(ttl / checkpointEvery) * checkpointEvery : 0;
                            if (elapsed > 200) break; // nunca termina: corta a simulação
                        }
                        times[i] = elapsed;
                        costSum += elapsed * spot;
                        if (elapsed > deadline) late++;
                    }
                    Arrays.sort(times);
                    System.out.printf("  %-14s %-15s %10s %9.1fh %11.1f%%%n", String.format("%.0f%%", hazard * 100), cp ? "every 15 min" : "none",
                            usd(costSum / runs), times[(int) (runs * 0.95)], 100.0 * late / runs);
                }
            }
            System.out.printf("  x 500 runs/month (e.g. a nightly fleet): on demand %s vs Spot with checkpoints at 5%%/h ~%s%n",
                    usd(500 * work * od), usd(500 * spotMean(work, 0.05, checkpointEvery, restart, spot, runs)));
            System.out.println();
        }
        System.out.println("(hazard per hour is a PREMISE; the Advisor gives a monthly frequency bucket, not a per-hour rate)");
    }

    static double spotMean(double work, double hazard, double every, double restart, double price, int runs) {
        Random rnd = new Random(42);
        double sum = 0;
        for (int i = 0; i < runs; i++) {
            double done = 0, elapsed = 0;
            while (done < work - 1e-9) {
                double ttl = -Math.log(1 - rnd.nextDouble()) / hazard;
                if (ttl >= work - done) { elapsed += work - done; break; }
                elapsed += ttl + restart;
                done += Math.floor(ttl / every) * every;
            }
            sum += elapsed * price;
        }
        return sum / runs;
    }

    static String usd(double v) {
        return String.format("US$ %,.2f", v);
    }
}
