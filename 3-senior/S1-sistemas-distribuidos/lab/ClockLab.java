import java.util.*;

/** S1.10: relogios fisicos com desvio, Lamport, vetorial e HLC. */
public class ClockLab {
    public static void main(String[] args) {
        lww(); lamport(); vector(); hlc();
    }

    static void lww() {
        System.out.println("== 1. Ultima-escrita-vence (LWW) com relogio de parede: o no B esta 5 s ATRASADO ==");
        // tempo real (s):   t=100 A grava "email=velho@x" (relogio A certo: 100);  t=102 B grava "email=novo@x" (relogio B mostra 97)
        long[] a = {100, 0}, b = {97, 0};
        String valA = "velho@x", valB = "novo@x";
        System.out.println("A grava '" + valA + "' com timestamp " + 100 + " ; 2 s DEPOIS, B grava '" + valB + "' com timestamp " + 97 + " (relogio atrasado)");
        String winner = 100 >= 97 ? valA : valB;
        System.out.println("LWW escolhe: " + winner + "  <- a escrita MAIS RECENTE (de B) foi descartada");
    }

    static void lamport() {
        System.out.println("\n== 2. Relogio de Lamport: 3 processos trocando mensagens ==");
        int[] c = new int[3]; String[] n = {"P1", "P2", "P3"};
        // eventos: P1 evento local; P1 -> P2; P2 -> P3; P3 evento local; P3 -> P1
        c[0]++; System.out.println("P1 evento local          L=" + c[0]);
        c[0]++; int m1 = c[0]; System.out.println("P1 envia a P2            L=" + m1);
        c[1] = Math.max(c[1], m1) + 1; System.out.println("P2 recebe de P1          L=" + c[1]);
        c[1]++; int m2 = c[1]; System.out.println("P2 envia a P3            L=" + m2);
        c[2]++; System.out.println("P3 evento local (concorrente com os de P1/P2)  L=" + c[2]);
        c[2] = Math.max(c[2], m2) + 1; System.out.println("P3 recebe de P2          L=" + c[2]);
        System.out.println("Lamport garante: a -> b  implica  L(a) < L(b). O contrario NAO vale: o evento local de P3 (L=1) e o primeiro de P1 (L=1) sao concorrentes e nao ha como saber so pelos numeros.");
    }

    static int[] max(int[] a, int[] b) { int[] r = new int[a.length]; for (int i = 0; i < a.length; i++) r[i] = Math.max(a[i], b[i]); return r; }
    static String cmp(int[] a, int[] b) {
        boolean le = true, ge = true; for (int i = 0; i < a.length; i++) { if (a[i] > b[i]) le = false; if (a[i] < b[i]) ge = false; }
        return le && ge ? "iguais" : le ? "a ANTES de b" : ge ? "b ANTES de a" : "CONCORRENTES (conflito)";
    }
    static void vector() {
        System.out.println("\n== 3. Relogio vetorial: detectar escritas concorrentes (3 replicas) ==");
        int[] A = {0, 0, 0}, B = {0, 0, 0};
        A[0]++; int[] w1 = A.clone();                                      // replica 0 grava v1
        B = max(B, w1); B[1]++; int[] w2 = B.clone();                      // replica 1 viu v1 e grava v2 (sucede w1)
        int[] w3 = A.clone(); w3[0]++;                                      // replica 0, sem ver v2, grava v3 (concorrente com v2)
        System.out.println("w1=" + Arrays.toString(w1) + " w2=" + Arrays.toString(w2) + " w3=" + Arrays.toString(w3));
        System.out.println("w1 x w2: " + cmp(w1, w2));
        System.out.println("w2 x w3: " + cmp(w2, w3) + "  <- o sistema sabe que precisa de MERGE (ou de pedir ao usuario), sem adivinhar por horario");
    }

    // HLC: (l, c) = maior relogio fisico visto + contador logico
    static final class Hlc {
        long l, c; long phys;
        long[] send() { long pt = phys; if (pt > l) { l = pt; c = 0; } else c++; return new long[]{l, c}; }
        long[] recv(long[] m) { long pt = phys; long nl = Math.max(Math.max(l, m[0]), pt);
            if (nl == l && nl == m[0]) c = Math.max(c, m[1]) + 1; else if (nl == l) c++; else if (nl == m[0]) c = m[1] + 1; else c = 0; l = nl; return new long[]{l, c}; }
    }
    static void hlc() {
        System.out.println("\n== 4. Relogio hibrido (HLC): relogio fisico de B atrasado 5 s, mas a ordem causal e respeitada ==");
        Hlc a = new Hlc(), b = new Hlc(); a.phys = 100_000; b.phys = 95_000; // ms: B esta 5 s atras
        long[] m = a.send(); System.out.println("A envia (relogio 100000):       HLC=(" + m[0] + "," + m[1] + ")");
        long[] r = b.recv(m);  System.out.println("B recebe (relogio fisico 95000): HLC=(" + r[0] + "," + r[1] + ")  -> o evento de B ja fica DEPOIS do de A");
        b.phys = 95_001; long[] w = b.send(); System.out.println("B grava logo apos (relogio 95001): HLC=(" + w[0] + "," + w[1] + ")  -> continua maior que o de A, mesmo com o relogio fisico de B atrasado");
        System.out.println("comparacao (l, c): " + (w[0] > m[0] || (w[0] == m[0] && w[1] > m[1]) ? "a escrita de B vence, como deveria (aconteceu depois)" : "ERRO"));
    }
}
