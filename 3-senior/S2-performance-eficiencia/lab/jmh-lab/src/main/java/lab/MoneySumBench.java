package lab;

import org.openjdk.jmh.annotations.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * Somar N valores monetarios: a decisao 4 (BigDecimal) contra centavos em long e contra double.
 * O double esta aqui SO para mostrar o preco do "rapido e errado" (veja o teste de correcao no main do lab).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class MoneySumBench {
    @Param({"100", "10000"})
    int n;

    BigDecimal[] bd;
    long[] cents;
    double[] dbl;

    @Setup
    public void setup() {
        bd = new BigDecimal[n];
        cents = new long[n];
        dbl = new double[n];
        for (int i = 0; i < n; i++) {
            long c = 100 + (i * 37L) % 99_900;                 // 1,00 a 999,99
            cents[i] = c;
            bd[i] = BigDecimal.valueOf(c, 2);
            dbl[i] = c / 100.0;
        }
    }

    @Benchmark
    public BigDecimal bigDecimal() {
        BigDecimal s = BigDecimal.ZERO;
        for (BigDecimal v : bd) s = s.add(v);
        return s.setScale(2, RoundingMode.HALF_EVEN);
    }

    @Benchmark
    public long longCents() {
        long s = 0;
        for (long v : cents) s += v;
        return s;
    }

    @Benchmark
    public double doubleSum() {
        double s = 0;
        for (double v : dbl) s += v;
        return s;
    }
}
