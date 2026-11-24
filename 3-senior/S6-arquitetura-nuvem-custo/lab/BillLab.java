import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * S6.06 — Estudo de caso: a conta de nuvem que triplicou.
 *   generate : monta uma fatura diária SINTÉTICA (ago–out/2026, sa-east-1) com preços REAIS da Price List
 *              e cinco causas PLANTADAS em outubro; grava samples/cur-synthetic.csv
 *   analyze  : mês a mês por serviço, maiores variações por tipo de uso, por centro de custo e custo por unidade
 *   detect   : quando cada causa seria vista: detector diário por serviço × total da conta × orçamento mensal
 * Nada disto é uma fatura real: as quantidades e as causas foram inventadas para o estudo de caso.
 */
public class BillLab {
    static final String R = "sa-east-1";
    static final Path FILE = Path.of("samples/cur-synthetic.csv");
    static final LocalDate START = LocalDate.of(2026, 8, 1), END = LocalDate.of(2026, 10, 31);

    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "analyze" : args[0]) {
            case "generate" -> generate(PriceBook.load());
            case "analyze" -> analyze(read());
            case "detect" -> detect(read());
            default -> throw new IllegalArgumentException("modos: generate | analyze | detect");
        }
    }

    record Row(LocalDate date, String account, String service, String usageType, String costCenter, String env, double usage, String unit, double cost) {}

    // ------------------------------------------------------------ generate
    static void generate(PriceBook pb) throws Exception {
        Random rnd = new Random(7);
        List<String> out = new ArrayList<>();
        out.add("date,account,service,usage_type,cost_center,env,usage,unit,cost_usd,transfers");
        for (LocalDate d = START; !d.isAfter(END); d = d.plusDays(1)) {
            int m = d.getMonthValue();
            double traffic = (m == 8 ? 1.00 : m == 9 ? 1.05 : 1.10) * (d.getDayOfWeek().getValue() >= 6 ? 0.8 : 1.0) * (1 + 0.03 * rnd.nextGaussian());
            long transfers = Math.round(10_000_000 / 30.42 * traffic);
            List<String[]> day = new ArrayList<>();
            // ----- base: o BOM da S6.04 em valores diários
            add(day, pb, "payflow-prod", "ECS", "fargate.arm.vcpu_h", "CC-1001", "prod", 57 * traffic);
            add(day, pb, "payflow-prod", "ECS", "fargate.arm.gb_h", "CC-1001", "prod", 114 * traffic);
            add(day, pb, "payflow-prod", "ELB", "alb.h", "CC-1001", "prod", 24);
            add(day, pb, "payflow-prod", "ELB", "alb.lcu_h", "CC-1001", "prod", 48 * traffic);
            add(day, pb, "payflow-prod", "VPC", "vpc.ipv4_h", "CC-1001", "prod", 48);
            add(day, pb, "payflow-prod", "RDS", "rds.pg.m7g.large.multi_h", "CC-1001", "prod", 24);
            add(day, pb, "payflow-prod", "RDS", "rds.gp3.multi_gb_mo", "CC-1001", "prod", 100 / 30.42);
            add(day, pb, "payflow-prod", "RDS", "rds.pg.t4g.medium.single_h", "CC-1002", "prod", 24);
            add(day, pb, "payflow-prod", "RDS", "rds.gp3.single_gb_mo", "CC-1002", "prod", 100 / 30.42);
            boolean snapshotsKept = !d.isBefore(LocalDate.of(2026, 10, 15)); // causa 5: snapshots manuais "antes da migração" esquecidos
            add(day, pb, "payflow-prod", "RDS", "rds.backup_gb_mo", "CC-1001", "prod", (100 + (snapshotsKept ? 600 : 0)) / 30.42);
            boolean endpointsRemoved = !d.isBefore(LocalDate.of(2026, 10, 3)); // causa 1: PR "limpeza de rede" + NAT para o sandbox do PSP
            add(day, pb, "payflow-prod", "VPC", "vpc.endpoint_h", "CC-1001", "prod", endpointsRemoved ? 2 * 24 : 12 * 24);
            add(day, pb, "payflow-prod", "VPC", "vpc.endpoint_gb", "CC-1001", "prod", (endpointsRemoved ? 1 : 10) * traffic);
            if (endpointsRemoved) {
                add(day, pb, "payflow-prod", "EC2-Other", "nat.h", "CC-1001", "prod", 2 * 24);
                add(day, pb, "payflow-prod", "EC2-Other", "nat.gb", "CC-1001", "prod", 190 * traffic);
            }
            boolean debug = !d.isBefore(LocalDate.of(2026, 10, 10)); // causa 2: DEBUG ligado em produção depois de um incidente
            add(day, pb, "payflow-prod", "CloudWatch", "cw.logs_ingest_gb", "CC-1001", "prod", (debug ? 62 : 3.3) * traffic);
            add(day, pb, "payflow-prod", "CloudWatch", "cw.logs_store_gb_mo", "CC-1001", "prod", (debug ? 100 + 62 * (d.getDayOfMonth() - 9) : 100) / 30.42);
            add(day, pb, "payflow-prod", "CloudWatch", "cw.alarm_mo", "CC-1001", "prod", 12 / 30.42);
            add(day, pb, "payflow-prod", "SQS", "sqs.request", "CC-1001", "prod", 2_000_000 * traffic);
            add(day, pb, "payflow-prod", "SNS", "sns.request", "CC-1001", "prod", 330_000 * traffic);
            add(day, pb, "payflow-prod", "KMS", "kms.key_mo", "CC-1003", "prod", 3 / 30.42);
            add(day, pb, "payflow-prod", "KMS", "kms.request", "CC-1003", "prod", 66_000 * traffic);
            add(day, pb, "payflow-prod", "SecretsManager", "sm.secret_mo", "CC-1001", "prod", 4 / 30.42);
            double archiveGb = 500 + (m == 10 ? 35.0 * d.getDayOfMonth() : 0); // causa 4: versionamento sem ciclo de vida (vazamento lento)
            add(day, pb, "payflow-prod", "S3", "s3.std_gb_mo", "CC-1002", "prod", archiveGb / 30.42);
            add(day, pb, "payflow-prod", "DataTransfer", "dt.out_gb", "", "", 13 * traffic);
            add(day, pb, "payflow-prod", "DataTransfer", "dt.inter_az_gb", "", "", 20 * traffic);
            // staging e contas compartilhadas
            add(day, pb, "payflow-staging", "ECS", "fargate.arm.vcpu_h", "CC-1001", "staging", 24);
            add(day, pb, "payflow-staging", "ECS", "fargate.arm.gb_h", "CC-1001", "staging", 48);
            add(day, pb, "payflow-staging", "ELB", "alb.h", "CC-1001", "staging", 24);
            add(day, pb, "payflow-staging", "VPC", "vpc.ipv4_h", "CC-1001", "staging", 48);
            add(day, pb, "payflow-staging", "RDS", "rds.pg.t4g.medium.single_h", "CC-1001", "staging", 24);
            add(day, pb, "payflow-staging", "VPC", "vpc.endpoint_h", "CC-1001", "staging", 6 * 24);
            add(day, pb, "payflow-staging", "CloudWatch", "cw.logs_ingest_gb", "CC-1001", "staging", 0.33);
            add(day, pb, "log-archive", "S3", "s3.std_gb_mo", "CC-9000", "shared", 100 / 30.42);
            add(day, pb, "audit", "Config", "config.item", "CC-9000", "shared", 165);
            add(day, pb, "payflow-backup", "RDS", "rds.backup_gb_mo", "CC-9000", "shared", 200 / 30.42);
            boolean loadtest = !d.isBefore(LocalDate.of(2026, 10, 1)); // causa 3: ambiente de teste de carga esquecido, sem tags, cópia da produção
            if (loadtest) {
                add(day, pb, "payflow-staging", "ECS", "fargate.x86.vcpu_h", "", "", 12 * 24);
                add(day, pb, "payflow-staging", "ECS", "fargate.x86.gb_h", "", "", 24 * 24);
                add(day, pb, "payflow-staging", "RDS", "rds.pg.m7g.large.multi_h", "", "", 24);
            }
            for (String[] r : day) out.add(d + "," + String.join(",", r) + "," + transfers);
        }
        Files.write(FILE, out);
        System.out.printf("generated %s: %d rows, %s to %s, region %s, real unit prices, SYNTHETIC usage and causes%n", FILE, out.size() - 1, START, END, R);
    }

    static void add(List<String[]> day, PriceBook pb, String account, String service, String key, String cc, String env, double usage) {
        PriceBook.Price p = pb.price(key, R);
        day.add(new String[]{account, service, p.usageType(), cc, env, String.format("%.4f", usage), p.unit().replace(",", ""), String.format("%.4f", usage * p.usd())});
    }

    // ------------------------------------------------------------ leitura
    static long[] transfersByDay;

    static List<Row> read() throws Exception {
        List<Row> rows = new ArrayList<>();
        Map<LocalDate, Long> transfers = new TreeMap<>();
        List<String> lines = Files.readAllLines(FILE);
        for (String l : lines.subList(1, lines.size())) {
            String[] f = l.split(",", -1);
            LocalDate d = LocalDate.parse(f[0]);
            rows.add(new Row(d, f[1], f[2], f[3], f[4], f[5], Double.parseDouble(f[6]), f[7], Double.parseDouble(f[8])));
            transfers.put(d, Long.parseLong(f[9]));
        }
        transfersByDay = transfers.values().stream().mapToLong(Long::longValue).toArray();
        return rows;
    }

    static String month(LocalDate d) { return d.toString().substring(0, 7); }

    // ------------------------------------------------------------ analyze
    static void analyze(List<Row> rows) {
        List<String> months = List.of("2026-08", "2026-09", "2026-10");
        Map<String, double[]> bySvc = new TreeMap<>(), byUsage = new TreeMap<>(), byCc = new TreeMap<>();
        double[] total = new double[3];
        for (Row r : rows) {
            int i = months.indexOf(month(r.date()));
            bySvc.computeIfAbsent(r.service(), k -> new double[3])[i] += r.cost();
            byUsage.computeIfAbsent(r.account() + " " + r.usageType(), k -> new double[3])[i] += r.cost();
            byCc.computeIfAbsent(r.costCenter().isEmpty() ? "(no tag)" : r.costCenter(), k -> new double[3])[i] += r.cost();
            total[i] += r.cost();
        }
        System.out.println("SYNTHETIC bill, " + R + ", real unit prices (see prices.csv). Monthly cost by service:");
        System.out.printf("%-16s %12s %12s %12s %12s%n", "service", months.get(0), months.get(1), months.get(2), "Oct - Aug");
        bySvc.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue()[2] - b.getValue()[0], a.getValue()[2] - a.getValue()[0]))
                .forEach(e -> System.out.printf("%-16s %12s %12s %12s %12s%n", e.getKey(), usd(e.getValue()[0]), usd(e.getValue()[1]), usd(e.getValue()[2]), usd(e.getValue()[2] - e.getValue()[0])));
        System.out.printf("%-16s %12s %12s %12s %12s   (Oct = x%.2f Aug)%n", "TOTAL", usd(total[0]), usd(total[1]), usd(total[2]), usd(total[2] - total[0]), total[2] / total[0]);
        System.out.println();
        System.out.println("Top 8 movers by account + usage type (Oct - Sep):");
        byUsage.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue()[2] - b.getValue()[1], a.getValue()[2] - a.getValue()[1])).limit(8)
                .forEach(e -> System.out.printf("  %-52s %12s -> %12s  (%s)%n", e.getKey(), usd(e.getValue()[1]), usd(e.getValue()[2]), signed(e.getValue()[2] - e.getValue()[1])));
        System.out.println();
        System.out.println("By cost center tag:");
        byCc.forEach((k, v) -> System.out.printf("  %-10s %12s %12s %12s  (%.0f%% of Oct)%n", k, usd(v[0]), usd(v[1]), usd(v[2]), 100 * v[2] / total[2]));
        System.out.println();
        long[] tm = new long[3];
        int day = 0;
        for (LocalDate d = START; !d.isAfter(END); d = d.plusDays(1)) tm[months.indexOf(month(d))] += transfersByDay[day++];
        System.out.println("Unit economics (all accounts):");
        for (int i = 0; i < 3; i++)
            System.out.printf("  %s: %,d transfers, %s per 1,000 transfers%n", months.get(i), tm[i], usd(1000 * total[i] / tm[i]));
    }

    // ------------------------------------------------------------ detect
    static void detect(List<Row> rows) {
        Map<String, double[]> daily = new TreeMap<>();
        int days = (int) (END.toEpochDay() - START.toEpochDay()) + 1;
        double[] totalDaily = new double[days];
        for (Row r : rows) {
            int i = (int) (r.date().toEpochDay() - START.toEpochDay());
            daily.computeIfAbsent(r.service(), k -> new double[days])[i] += r.cost();
            totalDaily[i] += r.cost();
        }
        System.out.println("Daily detector: cost today > mean + 3 sd of the previous 14 days (and +US$ 2), per service and for the account total");
        System.out.println("Planted causes: loadtest env Oct 1 (ECS, RDS); NAT + endpoints removed Oct 3 (EC2-Other, VPC); DEBUG logs Oct 10 (CloudWatch);");
        System.out.println("                manual snapshots kept Oct 15 (RDS); S3 versioning without lifecycle, gradual from Oct 1 (S3)");
        for (var e : daily.entrySet()) {
            List<LocalDate> flagged = flags(e.getValue());
            System.out.printf("  %-16s alert days: %s%n", e.getKey(), dates(flagged));
        }
        System.out.printf("  %-16s alert days: %s%n", "ACCOUNT TOTAL", dates(flags(totalDaily)));
        System.out.println();
        double budget = 1600; // PREMISSA: orçamento mensal (≈ estimativa da S6.04 + 12%)
        double mtd = 0;
        LocalDate actual = null, forecast = null;
        int octStart = (int) (LocalDate.of(2026, 10, 1).toEpochDay() - START.toEpochDay());
        for (int i = octStart; i < days; i++) {
            mtd += totalDaily[i];
            int dom = i - octStart + 1;
            if (forecast == null && dom >= 3 && mtd / dom * 31 > budget) forecast = LocalDate.of(2026, 10, dom);
            if (actual == null && mtd > budget) actual = LocalDate.of(2026, 10, dom);
        }
        System.out.printf("Monthly budget US$ %,.0f (premise): forecast alert (from day 3) on %s; actual > 100%% on %s%n", budget, forecast, actual);
        System.out.println("(AWS Budgets refreshes up to three times a day; billing data itself can lag: count on hours to a day of delay)");
    }

    static List<LocalDate> flags(double[] series) {
        List<LocalDate> out = new ArrayList<>();
        for (int i = 14; i < series.length; i++) {
            double[] w = Arrays.copyOfRange(series, i - 14, i);
            double mean = Arrays.stream(w).average().orElseThrow();
            double sd = Math.sqrt(Arrays.stream(w).map(x -> (x - mean) * (x - mean)).sum() / (w.length - 1));
            if (series[i] > mean + 3 * sd && series[i] - mean > 2) out.add(START.plusDays(i));
        }
        return out;
    }

    static String dates(List<LocalDate> ds) {
        return ds.isEmpty() ? "none" : ds.stream().map(d -> d.toString().substring(5)).collect(Collectors.joining(" "));
    }

    static String usd(double v) { return String.format("US$ %,.2f", v); }

    static String signed(double v) { return (v >= 0 ? "+" : "-") + usd(Math.abs(v)); }
}
