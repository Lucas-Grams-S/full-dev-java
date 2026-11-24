import java.util.*;

/**
 * S2.01 - Metodologia: medir antes de otimizar. Java puro: java MethodLab.java
 * Quatro secoes: (1) o primeiro numero mente (aquecimento), (2) media x percentis x
 * "pagina com 100 chamadas", (3) onde otimizar (Amdahl na pratica), (4) ruido: repita.
 */
public class MethodLab {
    static volatile long sink; // impede que o JIT descarte o trabalho

    static long work(int n) {
        long h = 7;
        for (int i = 0; i < n; i++) {
            h = h * 31 + Integer.toString(i).hashCode();
        }
        return h;
    }

    public static void main(String[] args) throws Exception {
        secao1();
        secao2();
        secao3();
        secao4();
    }

    // ---- 1. o primeiro numero mente
    static void secao1() {
        System.out.println("== 1. A mesma chamada, 12 vezes seguidas (work(200_000), ms) ==");
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            long t0 = System.nanoTime();
            sink = work(200_000);
            double ms = (System.nanoTime() - t0) / 1e6;
            sb.append(String.format(Locale.ROOT, "#%d=%.2f ", i, ms));
        }
        System.out.println(sb.toString().trim());
        System.out.println();
    }

    // ---- 2. media x percentis
    static double pct(double[] sorted, double p) {
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }

    static void secao2() {
        System.out.println("== 2. Latencia de um servico MODELADO (semente fixa): 98% rapidas, 2% lentas ==");
        Random r = new Random(42);
        int n = 200_000;
        double[] lat = new double[n];
        double soma = 0;
        for (int i = 0; i < n; i++) {
            double v = (r.nextDouble() < 0.98)
                    ? 8 + r.nextGaussian() * 1.5            // ~8 ms
                    : 300 + Math.abs(r.nextGaussian()) * 80; // ~300+ ms (lock, GC, retry)
            lat[i] = Math.max(1, v);
            soma += lat[i];
        }
        double[] s = lat.clone();
        Arrays.sort(s);
        System.out.printf(Locale.ROOT, "media=%.1f ms  p50=%.1f  p95=%.1f  p99=%.1f  p99.9=%.1f  max=%.1f%n",
                soma / n, pct(s, 50), pct(s, 95), pct(s, 99), pct(s, 99.9), s[n - 1]);

        // uma pagina chama o servico 100 vezes: quantas paginas pegam ao menos 1 chamada lenta?
        int paginas = 20_000, chamadas = 100, comLenta = 0;
        double limiar = pct(s, 99);
        for (int p = 0; p < paginas; p++) {
            for (int c = 0; c < chamadas; c++) {
                if (lat[r.nextInt(n)] > limiar) { comLenta++; break; }
            }
        }
        System.out.printf(Locale.ROOT, "paginas com 100 chamadas que sofreram ao menos uma acima do p99: %.1f%% (teoria: 1-0,99^100 = %.1f%%)%n",
                100.0 * comLenta / paginas, 100 * (1 - Math.pow(0.99, 100)));
        System.out.println();
    }

    // ---- 3. onde otimizar
    static String parse(int n) {           // CPU curta
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(i % 10);
        return sb.toString();
    }

    static long serialize(int n) {         // CPU media
        long h = 0;
        for (int i = 0; i < n; i++) h += Long.toString(i * 2654435761L).hashCode();
        return h;
    }

    static void espera(long ms) {          // "banco": espera modelada, nao CPU
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    static void secao3() {
        System.out.println("== 3. Uma requisicao com 3 fases: onde esta o tempo? (media de 200 execucoes, ms) ==");
        int reps = 200;
        for (int i = 0; i < 300; i++) { sink = parse(2000).length() + serialize(20_000); } // aquece
        double tp = 0, td = 0, ts = 0;
        for (int i = 0; i < reps; i++) {
            long a = System.nanoTime(); sink = parse(2000).length();
            long b = System.nanoTime(); espera(5);
            long c = System.nanoTime(); sink = serialize(20_000);
            long d = System.nanoTime();
            tp += (b - a) / 1e6; td += (c - b) / 1e6; ts += (d - c) / 1e6;
        }
        tp /= reps; td /= reps; ts /= reps;
        double tot = tp + td + ts;
        System.out.printf(Locale.ROOT, "parse=%.3f (%.1f%%)  banco(modelado)=%.3f (%.1f%%)  serialize=%.3f (%.1f%%)  total=%.3f%n",
                tp, 100 * tp / tot, td, 100 * td / tot, ts, 100 * ts / tot, tot);
        // Amdahl: acelerar uma fase em 10x
        System.out.printf(Locale.ROOT, "ganho total se parse ficar 10x mais rapido:     %.3fx%n", tot / (tot - tp + tp / 10));
        System.out.printf(Locale.ROOT, "ganho total se o banco ficar 10x mais rapido:    %.3fx%n", tot / (tot - td + td / 10));
        System.out.printf(Locale.ROOT, "ganho total se serialize ficar 10x mais rapido:  %.3fx%n", tot / (tot - ts + ts / 10));
        System.out.println();
    }

    // ---- 4. ruido
    static void secao4() {
        System.out.println("== 4. Ruido: a mesma medicao 15 vezes (cada uma = 30 execucoes de work(100_000), ms) ==");
        for (int i = 0; i < 100; i++) sink = work(100_000);
        double[] v = new double[15];
        for (int k = 0; k < v.length; k++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < 30; i++) sink = work(100_000);
            v[k] = (System.nanoTime() - t0) / 1e6;
        }
        double[] s = v.clone();
        Arrays.sort(s);
        double media = Arrays.stream(v).average().orElse(0);
        double dp = Math.sqrt(Arrays.stream(v).map(x -> (x - media) * (x - media)).sum() / (v.length - 1));
        System.out.printf(Locale.ROOT, "min=%.1f  mediana=%.1f  max=%.1f  media=%.1f  desvio=%.1f (%.1f%% da media)%n",
                s[0], s[s.length / 2], s[s.length - 1], media, dp, 100 * dp / media);
    }
}
