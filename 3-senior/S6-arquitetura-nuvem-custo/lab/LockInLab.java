import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * S6.03 — Multi-cloud e lock-in.
 *   scan DIR : onde o código importa SDK de provedor; só pacotes ".adapter." podem (o resto é vazamento)
 *   exit     : quanto custa mover dados (preços reais) e o valor esperado de sair × manter portabilidade ativa
 * Premissas de volume, prazo e probabilidade são INVENTADAS e declaradas.
 */
public class LockInLab {
    static final List<String> VENDOR = List.of("software.amazon.awssdk", "com.amazonaws", "io.awspring", "com.azure", "com.google.cloud");

    public static void main(String[] args) throws IOException {
        switch (args.length == 0 ? "exit" : args[0]) {
            case "scan" -> System.exit(scan(Path.of(args[1])) == 0 ? 0 : 1);
            case "exit" -> exit(PriceBook.load());
            default -> throw new IllegalArgumentException("modos: scan DIR | exit");
        }
    }

    record Finding(String pkg, String file, List<String> vendorImports) {}

    static int scan(Path root) throws IOException {
        List<Finding> findings = new ArrayList<>();
        try (Stream<Path> s = Files.walk(root)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).sorted().toList()) {
                String pkg = "";
                List<String> imports = new ArrayList<>();
                for (String line : Files.readAllLines(p)) {
                    String t = line.strip();
                    if (t.startsWith("package ")) pkg = t.substring(8, t.length() - 1);
                    if (t.startsWith("import ")) {
                        String imp = t.substring(7, t.length() - 1).replace("static ", "");
                        if (VENDOR.stream().anyMatch(imp::startsWith)) imports.add(imp);
                    }
                }
                findings.add(new Finding(pkg, p.getFileName().toString(), imports));
            }
        }
        int adapters = 0, leaks = 0;
        Map<String, Integer> services = new TreeMap<>();
        System.out.printf("scan %s: %d classes%n", root, findings.size());
        for (Finding f : findings) {
            if (f.vendorImports().isEmpty()) continue;
            boolean adapter = f.pkg().contains(".adapter.");
            if (adapter) adapters++; else leaks++;
            f.vendorImports().forEach(i -> services.merge(service(i), 1, Integer::sum));
            System.out.printf("  %-5s %-28s %-48s %d vendor import(s): %s%n", adapter ? "ok" : "LEAK", f.file(), f.pkg(),
                    f.vendorImports().size(), f.vendorImports().stream().map(LockInLab::service).distinct().toList());
        }
        System.out.printf("vendor services referenced: %s%n", services);
        System.out.printf("classes to rewrite to change provider: %d adapter(s) + %d leak(s) outside adapters%n", adapters, leaks);
        System.out.println(leaks == 0 ? "RESULT: PASS (provider SDK only in adapters)" : "RESULT: FAIL (" + leaks + " class(es) outside adapters import a provider SDK)");
        return leaks;
    }

    static String service(String imp) {
        String[] p = imp.split("\\.");
        if (imp.startsWith("software.amazon.awssdk.services.")) return "aws:" + p[4];
        if (imp.startsWith("software.amazon.awssdk.enhanced.")) return "aws:" + p[4] + "-enhanced";
        if (imp.startsWith("software.amazon.awssdk.")) return "aws:" + p[3];
        return p[0] + "." + p[1] + "." + p[2];
    }

    // ---------------------------------------------------------------- exit
    static void exit(PriceBook pb) {
        System.out.println("1. Data gravity: moving 1 TB (1,024 GB) out, first pricing tier, prices from the Price List");
        for (String r : List.of("us-east-1", "sa-east-1")) {
            String other = r.equals("us-east-1") ? "sa-east-1" : "us-east-1";
            double internet = Math.max(0, 1024 - 100) * pb.usd("dt.out_gb", r); // 100 GB/mês grátis (agregado global)
            double toOther = 1024 * pb.usd("dt.inter_region_to_" + other + "_gb", r);
            System.out.printf("   from %s: to the internet %s (after the 100 GB free); to %s %s%n", r, usd(internet), other, usd(toOther));
        }
        System.out.println("   (the AWS waiver for data transfer out when leaving AWS exists, through Support, see the 2024 blog post;");
        System.out.println("    it covers the move, not years of running in two clouds)");
        System.out.println();

        // PREMISSAS (inventadas): ambiente de US$ 1.000/mês (a S6.04 calcula o de verdade), 2 TB de dados,
        // migração em 4 meses com os dois ambientes ligados; ativo/ativo dobra a base, replica 200 GB/mês entre nuvens
        // e exige uma camada comum (o mínimo denominador) que não é precificada aqui.
        double monthly = 1000, dataGb = 2048, migrationMonths = 4, replicationGbMonth = 200, years = 3;
        String r = "sa-east-1";
        double egressOnce = Math.max(0, dataGb - 100) * pb.usd("dt.out_gb", r);
        double exitCost = migrationMonths * monthly + egressOnce;
        double activeActive = years * 12 * (monthly + replicationGbMonth * pb.usd("dt.out_gb", r));
        System.out.printf("2. Exit vs. active-active portability over %.0f years (region %s; premises in the source)%n", years, r);
        System.out.printf("   one exit: %.0f months running both (%s) + egress of %.0f GB (%s) = %s, adapters not priced%n",
                migrationMonths, usd(migrationMonths * monthly), dataGb, usd(egressOnce), usd(exitCost));
        System.out.printf("   active-active in a 2nd cloud: %s/month extra x %.0f months = %s, common layer not priced%n",
                usd(monthly + replicationGbMonth * pb.usd("dt.out_gb", r)), years * 12, usd(activeActive));
        System.out.printf("   %-40s %12s%n", "P(must leave within 3 years)", "expected exit");
        for (double p : List.of(0.05, 0.20, 0.50, 1.00))
            System.out.printf("   %-40s %12s%n", String.format("%.0f%%", p * 100), usd(p * exitCost));
        System.out.printf("   even P = 100%% gives %s, %.1fx less than active-active (%s): as lock-in insurance it does not pay;%n",
                usd(exitCost), activeActive / exitCost, usd(activeActive));
        System.out.println("   multi-cloud active-active needs another reason (regulation, contract, availability), priced on its own");
    }

    static String usd(double v) {
        return String.format("US$ %,.2f", v);
    }
}
