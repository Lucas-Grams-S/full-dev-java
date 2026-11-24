import java.util.*;

/** S1.11: tamanho do codigo base62, colisao de codigos aleatorios (aniversario) e contador ofuscado. */
public class ShortenerLab {
    static final String A = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static String b62(long n) { if (n == 0) return "0"; StringBuilder sb = new StringBuilder(); while (n > 0) { sb.append(A.charAt((int) (n % 62))); n /= 62; } return sb.reverse().toString(); }
    static long mix(long z) { z += 0x9E3779B97F4A7C15L; z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L; z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL; return z ^ (z >>> 31); }

    // permutacao reversivel e sem colisao: Feistel de 4 rodadas sobre o dominio [0, half^2), um pouco menor que 62^7
    static final long M = 62L * 62 * 62 * 62 * 62 * 62 * 62;      // 3.521.614.606.208
    static long feistel(long x) {
        long half = 1_876_000L;                                      // half^2 = 3,519e12 < 62^7; todo codigo cabe em 7 caracteres
        long v = x;
        do { long l = v / half, r = v % half; for (int i = 0; i < 4; i++) { long f = Math.floorMod(mix(r * 31 + i), half); long nl = r, nr = (l + f) % half; l = nl; r = nr; } v = l * half + r; } while (v >= M);
        return v;
    }

    public static void main(String[] args) {
        System.out.println("== 1. Quantos caracteres base62 para N URLs? ==");
        for (int len = 5; len <= 8; len++) System.out.printf("%d caracteres: 62^%d = %,d codigos%n", len, len, (long) Math.pow(62, len));
        System.out.println("exemplos: b62(125) = " + b62(125) + ", b62(1_000_000) = " + b62(1_000_000) + ", b62(3_521_614_606_207) = " + b62(3_521_614_606_207L));

        System.out.println("\n== 2. Codigos ALEATORIOS de 7 caracteres: quantas colisoes em N geracoes? (simulado x teoria n^2/(2M)) ==");
        Random r = new Random(77);
        for (int n : new int[]{100_000, 1_000_000, 10_000_000, 50_000_000}) {
            long[] v = new long[n]; for (int i = 0; i < n; i++) v[i] = Math.floorMod(r.nextLong(), M);
            Arrays.sort(v); int dup = 0; for (int i = 1; i < n; i++) if (v[i] == v[i - 1]) dup++;
            System.out.printf("n=%,11d: colisoes simuladas = %,5d ; teoria ~ %,.1f%n", n, dup, (double) n * n / (2.0 * M));
        }

        System.out.println("\n== 3. Contador sequencial x contador ofuscado (Feistel): unicidade e adivinhacao ==");
        int n = 1_000_000; Set<Long> seen = new HashSet<>(n * 2); boolean unique = true;
        for (long i = 0; i < n; i++) if (!seen.add(feistel(i))) unique = false;
        System.out.println("1.000.000 contadores -> codigos de 7 caracteres todos DISTINTOS: " + unique);
        System.out.println("sequencial 1000000..1000004 : " + b62(1_000_000) + " " + b62(1_000_001) + " " + b62(1_000_002) + " " + b62(1_000_003) + " " + b62(1_000_004) + "  (vizinhos adivinham-se)");
        System.out.println("ofuscado   1000000..1000004 : " + b62(feistel(1_000_000)) + " " + b62(feistel(1_000_001)) + " " + b62(feistel(1_000_002)) + " " + b62(feistel(1_000_003)) + " " + b62(feistel(1_000_004)));
        // varredura: probabilidade de acertar um codigo valido ao acaso se ha 1e9 URLs
        System.out.printf("%nse existem 1.000.000.000 de URLs no espaco de %,d codigos, chutar um codigo valido acerta com probabilidade %.4f%% por tentativa (nao e segredo: nao use para dado privado)%n", M, 100.0 * 1e9 / M);

        System.out.println("\n== 4. Armazenamento e trafego (calculo a partir dos PARAMETROS da aula, nao medicao) ==");
        double writesPerSec = 100_000_000.0 / 30 / 86400, reads = writesPerSec * 100;
        System.out.printf("100 milhoes de novas URLs/mes = %.0f escritas/s em media; razao leitura:escrita 100:1 = %.0f leituras/s; 500 bytes por registro, 5 anos = %.1f TB%n", writesPerSec, reads, 100e6 * 12 * 5 * 500 / 1e12);
    }
}
