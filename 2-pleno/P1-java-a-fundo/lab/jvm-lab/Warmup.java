import java.math.BigDecimal;
import java.math.RoundingMode;

/** P1.02 — JIT e aquecimento. Uso: java [flags] Warmup.java <batches|poly> */
public class Warmup {

    /** Trabalho "de negócio": tarifa de 1,5% sobre um valor em centavos, arredondada HALF_EVEN (decisão 4). */
    static long fee(long cents) {
        BigDecimal v = BigDecimal.valueOf(cents, 2).multiply(new BigDecimal("0.015")).setScale(2, RoundingMode.HALF_EVEN);
        return v.unscaledValue().longValue();
    }

    static long sink;     // consumir o resultado impede que o JIT elimine o cálculo (dead-code elimination)

    interface Rate { long apply(long x); }
    record A() implements Rate { public long apply(long x) { return x + 1; } }
    record B() implements Rate { public long apply(long x) { return x + 2; } }
    record C() implements Rate { public long apply(long x) { return x + 3; } }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "batches" : args[0];
        if (mode.equals("batches")) batches(); else poly();
    }

    static void batches() {
        int perBatch = 200_000;
        for (int b = 1; b <= 12; b++) {
            long t0 = System.nanoTime();
            long acc = 0;
            for (int i = 0; i < perBatch; i++) acc += fee(10_000 + i);
            long t1 = System.nanoTime();
            sink += acc;
            System.out.printf("lote %2d: %6.1f ms  (%5.0f ns/chamada)%n", b, (t1 - t0) / 1e6, (t1 - t0) / (double) perBatch);
        }
        System.out.println("sink=" + sink);
    }

    static long run(Rate[] rates, int n) {
        long acc = 0;
        for (int i = 0; i < n; i++) acc = rates[i % rates.length].apply(acc);
        return acc;
    }

    static void poly() {
        int n = 50_000_000;
        Rate[] one = { new A() };
        Rate[] two = { new A(), new B() };
        Rate[] three = { new A(), new B(), new C() };
        for (int round = 1; round <= 3; round++) {
            for (Rate[] set : new Rate[][] { one, two, three }) {
                long t0 = System.nanoTime();
                sink += run(set, n);
                System.out.printf("round %d, %d implementação(ões): %6.1f ms%n", round, set.length, (System.nanoTime() - t0) / 1e6);
            }
        }
    }
}
