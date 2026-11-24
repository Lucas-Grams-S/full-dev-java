import java.util.List;

/**
 * S6.02 — VMs × containers × serverless, pelo custo e pela utilização.
 *   curve  : custo mensal da API em Lambda, Fargate e EC2 conforme o tráfego médio (perfil diário da S6.01)
 *   memory : custo e latência do Lambda conforme a memória, para requisição presa à CPU e presa a I/O
 * Preços: AWS Price List (prices.csv). Premissas (INVENTADAS, declaradas): custo de CPU e espera por requisição,
 * alvo de utilização, mínimos para alta disponibilidade. Não inclui ALB (igual nas três), pessoas nem cota gratuita.
 */
public class ComputeLab {
    static final String R = System.getProperty("region", "us-east-1");
    // perfil diário da S6.01 normalizado (média 1,0): o pico é ~1,9× a média
    static final double[] SHAPE;
    static {
        double[] d = WellArchitectedLab.DEMAND;
        double avg = java.util.Arrays.stream(d).average().orElseThrow();
        SHAPE = java.util.Arrays.stream(d).map(x -> x / avg).toArray();
    }
    static final double CPU_MS = 10;   // PREMISSA: CPU por requisição a 1 vCPU
    static final double WAIT_MS = 50;  // PREMISSA: espera de I/O (banco, SQS) por requisição
    static final double TARGET = 0.60; // alvo de CPU do autoscaling (decisão 165)

    public static void main(String[] args) {
        PriceBook pb = PriceBook.load();
        switch (args.length == 0 ? "curve" : args[0]) {
            case "curve" -> curve(pb);
            case "memory" -> memory(pb);
            default -> throw new IllegalArgumentException("modos: curve | memory");
        }
    }

    record Cost(double usd, double paidCpuUsedPct, String sizing) {}

    /** Lambda ARM 1024 MB: paga a duração inteira, inclusive a espera. */
    static Cost lambda(PriceBook pb, double avgRps, int memMb) {
        double durS = lambdaDurationMs(memMb, CPU_MS, WAIT_MS) / 1000;
        double reqMonth = avgRps * 3600 * PriceBook.HOURS_PER_MONTH;
        double usd = reqMonth * (pb.usd("lambda.request", R) + memMb / 1024.0 * durS * pb.usd("lambda.arm.gb_s", R));
        double peakConcurrency = avgRps * max(SHAPE) * durS; // Lei de Little: L = λ × W
        return new Cost(usd, 100.0 * (CPU_MS / 1000) / (durS * memMb / 1769.0), String.format("peak concurrency %.1f", peakConcurrency));
    }

    static double lambdaDurationMs(int memMb, double cpuMs, double waitMs) {
        double vcpu = Math.min(1.0, memMb / 1769.0); // 1.769 MB = 1 vCPU (documentação); um fio não usa mais de 1 vCPU
        return cpuMs / vcpu + waitMs;
    }

    /** Servidores que escalam por hora: n = max(min, ceil(demanda / (vCPU × alvo))). */
    static Cost servers(double avgRps, double vcpuPerUnit, int min, double unitHourUsd) {
        double unitHours = 0, cpuUsed = 0;
        int peak = 0;
        for (double s : SHAPE) {
            double cpuNeeded = avgRps * s * CPU_MS / 1000; // vCPU ocupadas nesta hora
            int n = Math.max(min, (int) Math.ceil(cpuNeeded / (vcpuPerUnit * TARGET)));
            unitHours += n;
            cpuUsed += cpuNeeded;
            peak = Math.max(peak, n);
        }
        double days = PriceBook.HOURS_PER_MONTH / 24;
        return new Cost(unitHours * days * unitHourUsd, 100 * cpuUsed / (unitHours * vcpuPerUnit), "peak " + peak);
    }

    static Cost fargate(PriceBook pb, double rps) {
        return servers(rps, 0.5, 2, 0.5 * pb.usd("fargate.arm.vcpu_h", R) + 1.0 * pb.usd("fargate.arm.gb_h", R));
    }

    static Cost ec2(PriceBook pb, double rps, String priceKey) {
        return servers(rps, 2.0, 2, pb.usd(priceKey, R)); // m7g.large: 2 vCPU, 8 GiB
    }

    static void curve(PriceBook pb) {
        System.out.printf("Region %s, prices published %s (Fargate) / %s (Lambda) / %s (EC2)%n", R,
                pb.published("fargate.arm.vcpu_h", R), pb.published("lambda.arm.gb_s", R), pb.published("ec2.m7g.large.od_h", R));
        System.out.printf("Premises: %.0f ms CPU + %.0f ms wait per request; daily shape from S6.01 (peak = %.2fx average); target %.0f%%;%n",
                CPU_MS, WAIT_MS, max(SHAPE), TARGET * 100);
        System.out.println("  Lambda ARM 1024 MB behind the same ALB; Fargate ARM 0.5 vCPU/1 GB, min 2; EC2 m7g.large, min 2 (one per AZ).");
        System.out.println("  Excluded (equal or not priced here): ALB, data transfer, free tier, people, patching, AMIs.");
        System.out.println();
        System.out.printf("%8s | %13s %8s | %13s %8s | %13s %8s | %13s%n", "avg req/s", "Lambda", "CPU used", "Fargate", "CPU used", "EC2 on-demand", "CPU used", "EC2 RI 1y");
        for (double rps : List.of(0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0)) {
            Cost l = lambda(pb, rps, 1024), f = fargate(pb, rps), e = ec2(pb, rps, "ec2.m7g.large.od_h"), ri = ec2(pb, rps, "ec2.m7g.large.ri1y_nu_h");
            System.out.printf("%8.1f | %13s %7.0f%% | %13s %7.0f%% | %13s %7.0f%% | %13s%n", rps,
                    usd(l.usd()), l.paidCpuUsedPct(), usd(f.usd()), f.paidCpuUsedPct(), usd(e.usd()), e.paidCpuUsedPct(), usd(ri.usd()));
        }
        System.out.println();
        System.out.printf("Crossover Lambda = Fargate at %.1f req/s on average%n", crossover(pb, "fargate"));
        System.out.printf("Crossover Lambda = EC2 on-demand at %.1f req/s on average%n", crossover(pb, "ec2"));
        Cost f20 = fargate(pb, 20), l20 = lambda(pb, 20, 1024);
        System.out.printf("At 20 req/s: Fargate %s (%s), Lambda %s (%s)%n", f20.sizing(), usd(f20.usd()), l20.sizing(), usd(l20.usd()));
        System.out.printf("Fargate x86 / ARM price ratio: vCPU x%.2f, memory x%.2f%n",
                pb.usd("fargate.x86.vcpu_h", R) / pb.usd("fargate.arm.vcpu_h", R), pb.usd("fargate.x86.gb_h", R) / pb.usd("fargate.arm.gb_h", R));
    }

    static double crossover(PriceBook pb, String other) {
        double lo = 0.01, hi = 1000;
        for (int i = 0; i < 60; i++) {
            double mid = Math.sqrt(lo * hi);
            double o = other.equals("fargate") ? fargate(pb, mid).usd() : ec2(pb, mid, "ec2.m7g.large.od_h").usd();
            if (lambda(pb, mid, 1024).usd() < o) lo = mid; else hi = mid;
        }
        return lo;
    }

    static void memory(PriceBook pb) {
        System.out.printf("Lambda ARM, region %s: cost per 1 million requests and duration by memory (1,769 MB = 1 vCPU)%n", R);
        System.out.printf("%8s | %-30s | %-30s%n", "memory", "CPU-bound (200 ms CPU, 0 wait)", "I/O-bound (10 ms CPU, 50 ms wait)");
        for (int mem : List.of(128, 256, 512, 1024, 1769, 3008, 10240)) {
            String a = cell(pb, mem, 200, 0), b = cell(pb, mem, 10, 50);
            System.out.printf("%6d MB | %-30s | %-30s%n", mem, a, b);
        }
        System.out.println("(single-threaded handler assumed: above 1,769 MB extra vCPUs do not speed it up)");
    }

    static String cell(PriceBook pb, int mem, double cpuMs, double waitMs) {
        double ms = lambdaDurationMs(mem, cpuMs, waitMs);
        double usd = 1e6 * (pb.usd("lambda.request", R) + mem / 1024.0 * ms / 1000 * pb.usd("lambda.arm.gb_s", R));
        return String.format("%8.1f ms  %s", ms, usd(usd));
    }

    static double max(double[] a) {
        return java.util.Arrays.stream(a).max().orElseThrow();
    }

    static String usd(double v) {
        return String.format("US$ %,.2f", v);
    }
}
