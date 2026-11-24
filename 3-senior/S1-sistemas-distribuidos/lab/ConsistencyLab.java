import java.util.*;

/** S1.02: quorum N=3, leituras monotonicas e particao (CP x AP) com LWW. */
public class ConsistencyLab {
    public static void main(String[] args) {
        quorums();
        monotonic();
        partition();
    }

    // ---- 1. quorum: escrita em W replicas, leitura de R replicas (aleatorias), antes da propagacao ----
    static void quorums() {
        System.out.println("== 1. Quorum N=3, 100000 pares escrita-leitura, leitura IMEDIATA (antes da replicacao assincrona) ==");
        int n = 3, trials = 100_000; Random r = new Random(42);
        int[][] wr = {{1, 1}, {1, 2}, {2, 1}, {2, 2}, {3, 1}, {1, 3}};
        for (int[] c : wr) {
            int w = c[0], rd = c[1], stale = 0;
            for (int t = 0; t < trials; t++) {
                long[] ver = new long[n];            // versao em cada replica; todas comecam em 1
                Arrays.fill(ver, 1);
                for (int x : sample(n, w, r)) ver[x] = 2;   // a escrita (versao 2) so chegou a W replicas
                long best = 0; for (int x : sample(n, rd, r)) best = Math.max(best, ver[x]);
                if (best < 2) stale++;
            }
            System.out.printf("W=%d R=%d  W+R=%d %s N  leituras velhas: %6d de %d (%.2f%%)%n", w, rd, w + rd, (w + rd > n ? ">" : "<="), stale, trials, 100.0 * stale / trials);
        }
    }

    static int[] sample(int n, int k, Random r) {
        List<Integer> l = new ArrayList<>(); for (int i = 0; i < n; i++) l.add(i);
        Collections.shuffle(l, r); int[] o = new int[k]; for (int i = 0; i < k; i++) o[i] = l.get(i); return o;
    }

    // ---- 2. leituras monotonicas: ler de replicas aleatorias com replicacao atrasada ----
    static void monotonic() {
        System.out.println("\n== 2. Um cliente escreve v1..v50 (1 por tick) no lider; 3 replicas seguidoras recebem com atraso 0-4 ticks; ele le 1 replica ALEATORIA por tick ==");
        Random r = new Random(1); int ticks = 50, replicas = 3;
        int[][] arrive = new int[replicas][ticks + 1];
        for (int rep = 0; rep < replicas; rep++) for (int v = 1; v <= ticks; v++) arrive[rep][v] = v + r.nextInt(5);
        int backwards = 0, last = 0, stale = 0;
        for (int tick = 1; tick <= ticks; tick++) {
            int rep = r.nextInt(replicas), seen = 0;
            for (int v = 1; v <= ticks; v++) if (arrive[rep][v] <= tick) seen = Math.max(seen, v);
            if (seen < last) backwards++;
            if (seen < tick) stale++;
            last = Math.max(last, seen);
        }
        System.out.printf("leituras que voltaram no tempo (versao menor que a ja vista): %d de %d; leituras defasadas em relacao ao lider: %d%n", backwards, ticks, stale);
        // leitura fixa numa replica (sticky): nunca volta
        int[] fixed = new int[1]; int back2 = 0; last = 0; int rep = 0;
        for (int tick = 1; tick <= ticks; tick++) {
            int seen = 0; for (int v = 1; v <= ticks; v++) if (arrive[rep][v] <= tick) seen = Math.max(seen, v);
            if (seen < last) back2++; last = Math.max(last, seen);
        }
        System.out.printf("lendo SEMPRE da mesma replica (sticky): leituras que voltaram no tempo = %d%n", back2);
    }

    // ---- 3. particao: {A,B} | {C} ----
    static void partition() {
        System.out.println("\n== 3. Particao de rede: {A,B} de um lado e {C} do outro; clientes dos dois lados tentam gravar saldo ==");
        for (String mode : new String[]{"CP (so a maioria grava)", "AP (todos gravam, LWW ao curar)"}) {
            Map<String, long[]> node = new LinkedHashMap<>(); // valor, timestamp
            for (String n : List.of("A", "B", "C")) node.put(n, new long[]{100, 0});
            int accepted = 0, rejected = 0; List<String> log = new ArrayList<>();
            // escritas durante a particao: (no, valor, relogio logico global)
            Object[][] writes = {{"A", 80L, 1L}, {"C", 60L, 2L}, {"B", 70L, 3L}, {"C", 50L, 4L}};
            for (Object[] w : writes) {
                String n = (String) w[0]; long v = (Long) w[1], ts = (Long) w[2];
                boolean inMajority = !n.equals("C");
                if (mode.startsWith("CP") && !inMajority) { rejected++; log.add(n + " grava " + v + ": RECUSADA (sem quorum)"); continue; }
                accepted++; log.add(n + " grava " + v + ": aceita");
                if (inMajority) { node.get("A")[0] = v; node.get("A")[1] = ts; node.get("B")[0] = v; node.get("B")[1] = ts; }
                else { node.get("C")[0] = v; node.get("C")[1] = ts; }
            }
            // cura: LWW por timestamp
            long[] winner = node.values().stream().max(Comparator.comparingLong(x -> x[1])).get();
            System.out.println(mode); log.forEach(l -> System.out.println("   " + l));
            System.out.printf("   gravacoes aceitas=%d recusadas=%d; apos curar (LWW) todos ficam com %d%s%n", accepted, rejected, winner[0],
                mode.startsWith("AP") ? "  <- as gravacoes 80, 60 e 70 foram DESCARTADAS silenciosamente" : "");
        }
    }
}
