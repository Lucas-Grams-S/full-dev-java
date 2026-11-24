import java.util.*;

/** S1.09: janela fixa x deslizante x balde de fichas, limite local x compartilhado e falha do armazenamento. Tempo virtual. */
public class RateLimitLab {
    public static void main(String[] args) {
        System.out.println("== 1. Limite 100 por minuto: rajada na virada da janela (tempo virtual) ==");
        // 100 pedidos em 59,9 s e 100 pedidos em 60,1 s
        int[] fixed = new int[3]; int slAccepted = 0; Deque<Double> log = new ArrayDeque<>(); int tbAccepted = 0;
        double tokens = 100; double last = 0; double refillPerSec = 100.0 / 60; double cap = 100;
        for (int burst = 0; burst < 2; burst++) {
            double t0 = burst == 0 ? 59.9 : 60.1;
            for (int i = 0; i < 100; i++) {
                double t = t0 + i * 0.0001;
                int w = (int) (t / 60); if (fixed[w] < 100) fixed[w]++;                       // janela fixa
                while (!log.isEmpty() && log.peekFirst() <= t - 60) log.pollFirst();           // janela deslizante (log)
                if (log.size() < 100) { log.addLast(t); slAccepted++; }
                tokens = Math.min(cap, tokens + (t - last) * refillPerSec); last = t;          // balde de fichas
                if (tokens >= 1) { tokens--; tbAccepted++; }
            }
        }
        System.out.printf("janela fixa      : aceitou %d em ~0,2 s (limite configurado 100/min)%n", fixed[0] + fixed[1]);
        System.out.printf("janela deslizante: aceitou %d%n", slAccepted);
        System.out.printf("balde de fichas  : aceitou %d (capacidade 100 = a rajada maxima permitida)%n", tbAccepted);

        System.out.println("\n== 2. Limite 100 req/s por cliente, 5 instancias, 1.000 req/s do mesmo cliente, balanceador aleatorio (tempo virtual, 10 s) ==");
        Random r = new Random(6); int nodes = 5;
        // (a) cada no aplica 100/s sozinho
        int[][] perNodeSec = new int[nodes][10]; int accA = 0;
        for (int s = 0; s < 10; s++) for (int i = 0; i < 1000; i++) { int n = r.nextInt(nodes); if (perNodeSec[n][s] < 100) { perNodeSec[n][s]++; accA++; } }
        // (b) contador central atomico (um unico dono do contador): 100/s
        int accB = 0; for (int s = 0; s < 10; s++) { int c = 0; for (int i = 0; i < 1000; i++) if (c < 100) { c++; accB++; } }
        // (c) cota local 100/5 = 20 por no
        int accC = 0; int[][] q = new int[nodes][10]; r = new Random(6);
        for (int s = 0; s < 10; s++) for (int i = 0; i < 1000; i++) { int n = r.nextInt(nodes); if (q[n][s] < 20) { q[n][s]++; accC++; } }
        // (c2) cota local com trafego DESEQUILIBRADO: o cliente so cai em 2 dos 5 nos (afinidade)
        int accD = 0; int[][] q2 = new int[nodes][10]; r = new Random(6);
        for (int s = 0; s < 10; s++) for (int i = 0; i < 1000; i++) { int n = r.nextInt(2); if (q2[n][s] < 20) { q2[n][s]++; accD++; } }
        System.out.printf("(a) cada no limita 100/s sozinho           : %d aceitas em 10 s = %.0f/s (o limite real virou ~%d/s)%n", accA, accA / 10.0, accA / 10);
        System.out.printf("(b) contador central atomico                : %d aceitas = %.0f/s%n", accB, accB / 10.0);
        System.out.printf("(c) cota local de 20/s por no, balanceio uniforme   : %.0f/s (correto)%n", accC / 10.0);
        System.out.printf("(d) cota local de 20/s, cliente so em 2 nos         : %.0f/s (cliente legitimo recusado com folga ociosa nos outros 3 nos)%n", accD / 10.0);

        System.out.println("\n== 3. O armazenamento central cai por 10% do tempo: fail-open x fail-closed (1.000.000 pedidos, 1 cliente abusivo = 50%) ==");
        r = new Random(8); int openAbusiveLeak = 0, closedLegitRejected = 0, legit = 0, abusive = 0;
        for (int i = 0; i < 1_000_000; i++) { boolean down = r.nextDouble() < 0.10, abus = r.nextBoolean(); if (abus) abusive++; else legit++;
            if (down) { if (abus) openAbusiveLeak++; if (!abus) closedLegitRejected++; } }
        System.out.printf("fail-open  : %d pedidos do cliente abusivo passaram sem limite (%.1f%% dos abusivos)%n", openAbusiveLeak, 100.0 * openAbusiveLeak / abusive);
        System.out.printf("fail-closed: %d pedidos LEGITIMOS recusados (%.1f%% dos legitimos)%n", closedLegitRejected, 100.0 * closedLegitRejected / legit);
    }
}
