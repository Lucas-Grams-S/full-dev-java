import java.math.BigDecimal;

/**
 * S2.08 - "mais rapido" so vale se der o MESMO resultado. Soma em centavos com BigDecimal (exato) x double.
 * Tres conjuntos: o do benchmark (MoneySumBench), um com valores "quebrados" e 1.000.000 de somas de R$ 0,10.
 */
public class MoneyCheck {
    static void compara(String nome, long[] cents) {
        BigDecimal exact = BigDecimal.ZERO;
        double dbl = 0;
        for (long c : cents) {
            exact = exact.add(BigDecimal.valueOf(c, 2));
            dbl += c / 100.0;
        }
        BigDecimal diff = new BigDecimal(dbl).subtract(exact);
        System.out.printf("%-34s exato=%s  double=%s  igual? %s  erro=%s%n", nome, exact, dbl,
                new BigDecimal(dbl).compareTo(exact) == 0, diff.toPlainString().length() > 24 ? diff.setScale(15, java.math.RoundingMode.HALF_EVEN).toPlainString() : diff.toPlainString());
    }

    public static void main(String[] a) {
        long[] bench = new long[10_000];
        for (int i = 0; i < bench.length; i++) bench[i] = 100 + (i * 37L) % 99_900;       // o conjunto do MoneySumBench
        compara("benchmark (10.000 valores)", bench);

        long[] quebrados = new long[10_000];
        for (int i = 0; i < quebrados.length; i++) quebrados[i] = 1 + (i * 7919L) % 100_000;   // centavos "quebrados"
        compara("centavos quebrados (10.000)", quebrados);

        long[] dez = new long[1_000_000];
        java.util.Arrays.fill(dez, 10);                                                       // 1.000.000 x R$ 0,10
        compara("1.000.000 x R$ 0,10", dez);

        double s = 0;
        for (int i = 0; i < 10; i++) s += 0.1;
        System.out.println("10 x 0.1 em double = " + s + "  (igual a 1.0? " + (s == 1.0) + ")");
        System.out.println("0.1 + 0.2 == 0.3 em double? " + (0.1 + 0.2 == 0.3) + "   (0.1 + 0.2 = " + (0.1 + 0.2) + ")");
        System.out.println("arredondar 1.005 para 2 casas, meio para cima: double " + Math.round(1.005 * 100) / 100.0
                + "  x  BigDecimal " + new BigDecimal("1.005").setScale(2, java.math.RoundingMode.HALF_UP));
    }
}
