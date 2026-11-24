import java.util.*;

/** S1.03: hash mod N x hash consistente (vnodes), equilibrio, chave quente e chave monotonica. */
public class ShardingLab {
    static long mix(long z) { z += 0x9E3779B97F4A7C15L; z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L; z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL; return z ^ (z >>> 31); }
    static long h(String s) { long x = 1125899906842597L; for (int i = 0; i < s.length(); i++) x = 31 * x + s.charAt(i); return mix(x); }

    static final class Ring {
        final TreeMap<Long, String> ring = new TreeMap<>();
        void add(String node, int vnodes) { for (int v = 0; v < vnodes; v++) ring.put(h(node + "#" + v), node); }
        String owner(long keyHash) { var e = ring.ceilingEntry(keyHash); return (e != null ? e : ring.firstEntry()).getValue(); }
    }

    public static void main(String[] args) {
        int keys = 100_000;
        System.out.println("== 1. Mover chaves ao ir de 4 para 5 nos (" + keys + " chaves) ==");
        int moved = 0;
        for (int i = 0; i < keys; i++) { long k = h("acc-" + i); if (Math.floorMod(k, 4) != Math.floorMod(k, 5)) moved++; }
        System.out.printf("hash mod N      : %d chaves mudam de no (%.1f%%)%n", moved, 100.0 * moved / keys);
        for (int vn : new int[]{1, 16, 128, 512}) {
            Ring a = new Ring(); for (int n = 0; n < 4; n++) a.add("n" + n, vn);
            Ring b = new Ring(); for (int n = 0; n < 5; n++) b.add("n" + n, vn);
            int mv = 0; Map<String, Integer> load = new TreeMap<>();
            for (int i = 0; i < keys; i++) { long k = h("acc-" + i); if (!a.owner(k).equals(b.owner(k))) mv++; load.merge(b.owner(k), 1, Integer::sum); }
            int max = Collections.max(load.values()), min = Collections.min(load.values());
            System.out.printf("anel vnodes=%3d : %6d chaves mudam (%.1f%%); com 5 nos: maior no=%d menor no=%d (ideal %d; maior/ideal=%.2f)%n", vn, mv, 100.0 * mv / keys, max, min, keys / 5, max / (keys / 5.0));
        }
        System.out.println("(ideal ao acrescentar o 5o no: 1/5 = 20,0% das chaves)");

        System.out.println("\n== 2. Chave quente: 1.000.000 acessos a 10.000 contas com distribuicao Zipf (s=1,1), 8 shards por hash da conta ==");
        int accounts = 10_000; double[] cdf = new double[accounts]; double sum = 0;
        for (int i = 0; i < accounts; i++) { sum += 1 / Math.pow(i + 1, 1.1); cdf[i] = sum; }
        Random r = new Random(5); int[] shard = new int[8]; int[] rankHits = new int[accounts];
        for (int i = 0; i < 1_000_000; i++) { double x = r.nextDouble() * sum; int lo = 0, hi = accounts - 1; while (lo < hi) { int m = (lo + hi) >>> 1; if (cdf[m] < x) lo = m + 1; else hi = m; } rankHits[lo]++; shard[Math.floorMod(h("acc-" + lo), 8)]++; }
        int mx = Arrays.stream(shard).max().getAsInt(), mn = Arrays.stream(shard).min().getAsInt();
        System.out.printf("acessos por shard = %s; maior/media = %.2f; a conta mais quente sozinha = %d acessos (%.1f%% do total)%n", Arrays.toString(shard), mx / 125000.0, rankHits[0], rankHits[0] / 10000.0);

        System.out.println("\n== 3. Chave monotonica (tempo) x hash: 1.000.000 escritas durante a hora 14, 8 shards ==");
        // faixas: o dia (86.400 s) dividido em 8 faixas de 3 h; chave = segundo do dia. Hash: hash(id).
        int[] byRange = new int[8], byHash = new int[8]; Random rr = new Random(3);
        for (int i = 0; i < 1_000_000; i++) {
            int secondOfDay = 14 * 3600 + rr.nextInt(3600);          // todas as escritas "agora"
            byRange[secondOfDay / (86_400 / 8)]++;
            byHash[Math.floorMod(h("tx-" + i), 8)]++;
        }
        System.out.printf("particao por FAIXA do tempo : %s%n", Arrays.toString(byRange));
        System.out.printf("particao por HASH da chave  : %s%n", Arrays.toString(byHash));
    }
}
