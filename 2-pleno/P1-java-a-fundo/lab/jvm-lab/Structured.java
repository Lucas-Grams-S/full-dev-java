import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

/**
 * P1.10 — structured concurrency (PREVIEW no Java 25, JEP 505: a API muda entre versões!).
 * Rode: java --enable-preview --source 25 Structured.java <all|first|fail|timeout>
 */
public class Structured {

    static BigDecimal rate(String provider, long millis, boolean fail) throws InterruptedException {
        Thread.sleep(millis);
        if (fail) throw new IllegalStateException(provider + " fora do ar");
        return new BigDecimal("5.10").add(BigDecimal.valueOf(provider.length(), 2));
    }

    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "all" : args[0]) {
            case "all" -> all();
            case "first" -> first();
            case "fail" -> fail();
            case "timeout" -> timeout();
            default -> System.out.println("?");
        }
    }

    /** Espera TODAS e, se qualquer uma falhar, cancela as outras. */
    static void all() throws Exception {
        long t0 = System.nanoTime();
        try (var scope = StructuredTaskScope.open()) {                       // Joiner padrão: awaitAllSuccessfulOrThrow()
            Subtask<BigDecimal> a = scope.fork(() -> rate("ecb", 300, false));
            Subtask<BigDecimal> b = scope.fork(() -> rate("bcb", 500, false));
            scope.join();                                                    // espera o bloco todo
            System.out.printf("a=%s b=%s em %.0f ms (estado: %s/%s)%n", a.get(), b.get(), (System.nanoTime() - t0) / 1e6, a.state(), b.state());
        }
    }

    /** A primeira que der certo vence; as outras são canceladas (interrompidas). */
    static void first() throws Exception {
        long t0 = System.nanoTime();
        try (var scope = StructuredTaskScope.open(Joiner.<BigDecimal>anySuccessfulResultOrThrow())) {
            scope.fork(() -> rate("lento", 800, false));
            scope.fork(() -> rate("rapido", 200, false));
            scope.fork(() -> rate("quebrado", 50, true));                    // falha de um não derruba o "qualquer sucesso"
            System.out.printf("vencedor: %s em %.0f ms%n", scope.join(), (System.nanoTime() - t0) / 1e6);
        }
    }

    static void fail() throws Exception {
        long t0 = System.nanoTime();
        try (var scope = StructuredTaskScope.open()) {
            Subtask<BigDecimal> slow = scope.fork(() -> rate("lento", 2000, false));
            scope.fork(() -> rate("quebrado", 100, true));
            scope.join();
        } catch (StructuredTaskScope.FailedException e) {
            System.out.printf("FailedException após %.0f ms (a tarefa lenta foi cancelada, não esperamos 2000 ms): causa = %s%n", (System.nanoTime() - t0) / 1e6, e.getCause());
        }
    }

    static void timeout() throws Exception {
        long t0 = System.nanoTime();
        try (var scope = StructuredTaskScope.open(Joiner.<BigDecimal>awaitAllSuccessfulOrThrow(), cf -> cf.withTimeout(Duration.ofMillis(300)))) {
            scope.fork(() -> rate("lento", 2000, false));
            scope.join();
        } catch (StructuredTaskScope.TimeoutException e) {
            System.out.printf("TimeoutException após %.0f ms%n", (System.nanoTime() - t0) / 1e6);
        }
    }
}
