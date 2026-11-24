import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * S6.01 — Well-Architected na prática.
 *   tradeoff : quanto custa (com preços reais da AWS Price List) cada opção de folga da S4.06 e o Multi-AZ do RDS
 *   review   : linter de uma revisão Well-Architected (cobertura dos pilares, risco com dono/prazo/ação, evidência)
 * Modelo: a demanda por hora e as premissas de negócio são INVENTADAS e declaradas; os preços são os da Price List.
 */
public class WellArchitectedLab {

    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "tradeoff" : args[0]) {
            case "tradeoff" -> tradeoff(PriceBook.load());
            case "review" -> System.exit(review(Path.of(args[1]), LocalDate.parse(args[2])) == 0 ? 0 : 1);
            default -> throw new IllegalArgumentException("modos: tradeoff | review ARQUIVO DATA");
        }
    }

    // ---------------------------------------------------------------- tradeoff
    /** Demanda de CPU por hora do dia, em vCPU efetivamente usadas (PREMISSA: madrugada, comercial, pico às 12h e 19h). */
    static final double[] DEMAND = {
        0.25, 0.2, 0.2, 0.2, 0.2, 0.25, 0.4, 0.6, 0.9, 1.0, 1.0, 1.1,
        1.5, 1.2, 1.0, 1.0, 1.0, 1.1, 1.3, 1.5, 1.1, 0.8, 0.5, 0.35};
    static final double TASK_VCPU = 0.5, TASK_GB = 1.0; // decisão 165 (P9.02)

    record Option(String name, int azs, int min, double target, int extraEndpointAzs, boolean balanced) {}

    static void tradeoff(PriceBook pb) {
        List<Option> options = List.of(
                new Option("A: 2 AZs, min 2, target 60%", 2, 2, 0.60, 0, false),
                new Option("B: 2 AZs, min 4, target 45%", 2, 4, 0.45, 0, false),
                new Option("C: 3 AZs, min 3, target 60%", 3, 3, 0.60, 1, false),
                new Option("B2: B, only multiples of 2", 2, 4, 0.45, 0, true));
        System.out.printf("Prices: AWS Price List, Fargate ARM published %s (us-east-1) / %s (sa-east-1)%n",
                pb.published("fargate.arm.vcpu_h", "us-east-1"), pb.published("fargate.arm.vcpu_h", "sa-east-1"));
        System.out.println("Demand (premise): 24 hourly values, 0.20 to 1.50 vCPU; task = 0.5 vCPU / 1 GB ARM; 30.4 days/month");
        System.out.println();
        System.out.println("1. Headroom options from S4.06, priced (compute + extra AZ endpoints only)");
        System.out.printf("%-28s %10s %10s %22s %22s %12s%n", "option", "task-h/day", "peak tasks", "us-east-1/month", "sa-east-1/month", "worst CPU*");
        Map<String, Double> base = new LinkedHashMap<>();
        for (Option o : options) {
            int taskHours = 0, peakTasks = 0;
            double worstAfterLoss = 0;
            for (double d : DEMAND) {
                int n = Math.max(o.min(), (int) Math.ceil(d / (TASK_VCPU * o.target())));
                if (o.balanced()) n = (n + o.azs() - 1) / o.azs() * o.azs(); // hipótese: mesmo número em cada AZ
                taskHours += n;
                peakTasks = Math.max(peakTasks, n);
                int lost = (int) Math.ceil((double) n / o.azs()); // a AZ que cai é a que tem mais tarefas
                worstAfterLoss = Math.max(worstAfterLoss, d / (TASK_VCPU * (n - lost)));
            }
            StringBuilder line = new StringBuilder(String.format("%-28s %10d %10d", o.name(), taskHours, peakTasks));
            for (String r : List.of("us-east-1", "sa-east-1")) {
                double perTaskHour = TASK_VCPU * pb.usd("fargate.arm.vcpu_h", r) + TASK_GB * pb.usd("fargate.arm.gb_h", r);
                double compute = taskHours * perTaskHour * 730.0 / 24;
                double endpoints = o.extraEndpointAzs() * 6 * pb.usd("vpc.endpoint_h", r) * PriceBook.HOURS_PER_MONTH;
                double total = compute + endpoints;
                base.putIfAbsent(r, total);
                String cell = o.name().startsWith("A") ? usd(total) : String.format("%s (%+.0f%%)", usd(total), 100 * (total / base.get(r) - 1));
                line.append(String.format(" %22s", cell));
            }
            line.append(String.format(" %12s", String.format("%.0f%%", 100 * worstAfterLoss)));
            System.out.println(line);
        }
        System.out.println("   * worst CPU = highest utilization of the surviving tasks, in any hour, right after losing one AZ");
        System.out.println("   (C adds 6 interface endpoints in the 3rd AZ; subnets are free. A and C hit >100% because the AZ");
        System.out.println("    that fails holds ceil(n/AZs) tasks: odd task counts make it worse, as S4.06 showed.");
        System.out.println("    B2 is a hypothesis: target tracking does not keep counts even; it needs step scaling by 2 or similar.)");

        System.out.println();
        System.out.println("2. RDS PostgreSQL db.m7g.large + 100 GB gp3: Single-AZ vs Multi-AZ (on demand, per month)");
        for (String r : List.of("us-east-1", "sa-east-1")) {
            double single = pb.usd("rds.pg.m7g.large.single_h", r) * 730 + 100 * pb.usd("rds.gp3.single_gb_mo", r);
            double multi = pb.usd("rds.pg.m7g.large.multi_h", r) * 730 + 100 * pb.usd("rds.gp3.multi_gb_mo", r);
            double extraYear = (multi - single) * 12;
            System.out.printf("   %s: Single-AZ %s, Multi-AZ %s, extra %s/month = %s/year%n", r, usd(single), usd(multi), usd(multi - single), usd(extraYear));
            // PREMISSAS de negócio (inventadas, para mostrar a conta): 1 evento de AZ que afeta o banco por ano;
            // sem Multi-AZ o RTO é a restauração (S4.05: ~2 h, a medir); com Multi-AZ, ~2 min (P9.08: 60–120 s).
            double hoursAvoided = 1 * (2.0 - 2.0 / 60);
            System.out.printf("      breaks even if 1 hour of payments down costs more than %s (premise: 1 AZ event/year, RTO 2 h -> 2 min)%n",
                    usd(extraYear / hoursAvoided));
        }

        System.out.println();
        System.out.println("3. Same architecture, two regions: sa-east-1 / us-east-1 price ratio");
        for (String k : List.of("fargate.arm.vcpu_h", "fargate.arm.gb_h", "rds.pg.m7g.large.multi_h", "rds.gp3.multi_gb_mo",
                "alb.h", "vpc.endpoint_h", "nat.gb", "cw.logs_ingest_gb", "dt.out_gb", "s3.std_gb_mo", "kms.key_mo")) {
            System.out.printf("   %-26s %10.6f %10.6f  x%.2f%n", k, pb.usd(k, "us-east-1"), pb.usd(k, "sa-east-1"),
                    pb.usd(k, "sa-east-1") / pb.usd(k, "us-east-1"));
        }
    }

    static String usd(double v) {
        return String.format("US$ %,.2f", v);
    }

    // ---------------------------------------------------------------- review
    static final List<String> PILLARS = List.of("FND", "OPS", "SEC", "REL", "PERF", "COST", "SUS");
    static final Pattern OWNER = Pattern.compile("@[a-z0-9-]+");
    static final Pattern EVIDENCE = Pattern.compile("(decisão \\d{1,3}|ADR \\d{4}|docs/[\\w./-]+|out-[\\w-]+\\.txt|S\\d\\.\\d{2})");

    record Item(int line, String id, String pillar, String question, String risk, String evidence, String owner, String due, String action) {}

    static int review(Path file, LocalDate reviewDate) throws Exception {
        List<Item> items = new ArrayList<>();
        List<String> lines = Files.readAllLines(file);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            String[] f = lines.get(i).split(";", -1);
            if (f.length != 8) throw new IllegalArgumentException("linha " + (i + 1) + ": esperava 8 campos, achei " + f.length);
            items.add(new Item(i + 1, f[0], f[1], f[2], f[3], f[4], f[5], f[6], f[7]));
        }
        List<String> problems = new ArrayList<>();
        Map<String, int[]> perPillar = new LinkedHashMap<>(); // HIGH, MEDIUM, NONE, NA
        PILLARS.forEach(p -> perPillar.put(p, new int[4]));
        for (Item it : items) {
            int[] c = perPillar.get(it.pillar());
            if (c == null) { problems.add(it.line() + " " + it.id() + ": pilar desconhecido '" + it.pillar() + "'"); continue; }
            switch (it.risk()) {
                case "HIGH", "MEDIUM" -> {
                    c[it.risk().equals("HIGH") ? 0 : 1]++;
                    if (!OWNER.matcher(it.owner()).matches()) problems.add(it.line() + " " + it.id() + ": risco " + it.risk() + " sem dono (@alguem)");
                    if (it.action().isBlank()) problems.add(it.line() + " " + it.id() + ": risco " + it.risk() + " sem ação");
                    if (it.due().isBlank()) problems.add(it.line() + " " + it.id() + ": risco " + it.risk() + " sem prazo");
                    else {
                        LocalDate due = LocalDate.parse(it.due());
                        if (!due.isAfter(reviewDate)) problems.add(it.line() + " " + it.id() + ": prazo " + due + " não está depois da revisão");
                        if (it.risk().equals("HIGH") && due.isAfter(reviewDate.plusDays(90)))
                            problems.add(it.line() + " " + it.id() + ": risco HIGH com prazo > 90 dias (" + due + ")");
                    }
                }
                case "NONE" -> {
                    c[2]++;
                    if (!EVIDENCE.matcher(it.evidence()).find()) problems.add(it.line() + " " + it.id() + ": 'sem risco' sem evidência (decisão, ADR, docs/, out-*.txt ou aula)");
                }
                case "NA" -> {
                    c[3]++;
                    if (!it.evidence().startsWith("NA:") || it.evidence().length() < 15) problems.add(it.line() + " " + it.id() + ": 'não se aplica' sem justificativa (NA: ...)");
                }
                default -> problems.add(it.line() + " " + it.id() + ": risco inválido '" + it.risk() + "'");
            }
        }
        perPillar.forEach((p, c) -> { if (c[0] + c[1] + c[2] + c[3] == 0) problems.add("pilar " + p + " sem nenhuma pergunta respondida"); });

        System.out.printf("review %s (%d items, date %s)%n", file.getFileName(), items.size(), reviewDate);
        System.out.printf("%-6s %5s %7s %5s %4s%n", "pillar", "HIGH", "MEDIUM", "NONE", "NA");
        perPillar.forEach((p, c) -> System.out.printf("%-6s %5d %7d %5d %4d%n", p, c[0], c[1], c[2], c[3]));
        problems.forEach(p -> System.out.println("  PROBLEM " + p));
        System.out.println(problems.isEmpty() ? "RESULT: PASS" : "RESULT: FAIL (" + problems.size() + " problems)");
        return problems.size();
    }
}
