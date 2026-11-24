import jdk.jfr.*;

/**
 * S2.02 - evento customizado do JFR: o que a JVM nao sabe (uma transferencia, uma fila, um cache miss)
 * vira um evento com campos, duracao e (opcionalmente) pilha, no MESMO arquivo .jfr.
 *   java -XX:StartFlightRecording:filename=x.jfr CustomEventDemo   &&   jfr print --events payflow.Transfer x.jfr
 */
public class CustomEventDemo {
    @Name("payflow.Transfer")
    @Label("Transfer")
    @Category("PayFlow")
    @StackTrace(false)
    static class TransferEvent extends Event {
        @Label("Amount (cents)") long cents;
        @Label("Result") String result;
    }

    static String transfer(long cents) throws InterruptedException {
        TransferEvent e = new TransferEvent();
        e.begin();
        try {
            Thread.sleep(cents > 50_000 ? 30 : 5);                  // "trabalho" modelado
            e.result = cents > 90_000 ? "REJECTED" : "OK";
            return e.result;
        } finally {
            e.cents = cents;
            e.end();
            e.commit();                                              // so grava se o evento esta habilitado
        }
    }

    public static void main(String[] a) throws Exception {
        for (long c : new long[]{1_000, 75_000, 95_000}) System.out.println(c + " -> " + transfer(c));
    }
}
