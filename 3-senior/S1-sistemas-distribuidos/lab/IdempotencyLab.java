import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** S1.08: retentativas, chave de idempotencia, concorrencia e queda entre efeito e registro. */
public class IdempotencyLab {
    // "banco" com transacao simulada por um monitor (modela a semantica; o banco real foi a P3.07)
    static final class Bank {
        long balance = 100_000_000L; final Map<String, Object[]> keys = new HashMap<>(); // chave -> {hashDoPedido, status, resposta}
        int debits = 0;
        synchronized String debitNoKey(long cents) { balance -= cents; debits++; return "OK"; }
        synchronized String debit(String key, String reqHash, long cents, boolean crashBetween) {
            Object[] k = keys.get(key);
            if (k != null) {
                if (!k[0].equals(reqHash)) return "422 chave reutilizada com outro corpo";
                return "200 repetido: " + k[2];            // mesma resposta, sem novo efeito
            }
            long b0 = balance; int d0 = debits;              // "inicio da transacao"
            balance -= cents; debits++;                      // efeito
            if (crashBetween) { balance = b0; debits = d0; throw new IllegalStateException("queda"); } // ROLLBACK simulado: efeito e chave na MESMA transacao, tudo some
            keys.put(key, new Object[]{reqHash, "DONE", "OK"});
            return "200 ok";
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== 1. 10.000 debitos de 10,00; a RESPOSTA se perde em 30% das tentativas e o cliente repete ==");
        Random r = new Random(9); Bank noKey = new Bank(), withKey = new Bank(); int attemptsNo = 0, attemptsKey = 0;
        for (int i = 0; i < 10_000; i++) {
            boolean done = false; String key = "pay-" + i;
            while (!done) { attemptsNo++; noKey.debitNoKey(1000); done = r.nextDouble() >= 0.30; }
            done = false;
            while (!done) { attemptsKey++; withKey.debit(key, "h" + i, 1000, false); done = r.nextDouble() >= 0.30; }
        }
        System.out.printf("sem chave: %d tentativas -> %d debitos (esperado 10000); saldo=%d (esperado %d)%n", attemptsNo, noKey.debits, noKey.balance, 100_000_000L - 10_000L * 1000);
        System.out.printf("com chave: %d tentativas -> %d debitos (esperado 10000); saldo=%d (esperado %d)%n", attemptsKey, withKey.debits, withKey.balance, 100_000_000L - 10_000L * 1000);

        System.out.println("\n== 2. A MESMA chave chegando ao mesmo tempo (2 threads, 1.000 chaves) ==");
        Bank b = new Bank(); ExecutorService ex = Executors.newFixedThreadPool(2); AtomicInteger dup = new AtomicInteger();
        for (int i = 0; i < 1000; i++) { final String k = "c-" + i; CountDownLatch go = new CountDownLatch(1);
            Future<String> f1 = ex.submit(() -> { go.await(); return b.debit(k, "h", 100, false); }), f2 = ex.submit(() -> { go.await(); return b.debit(k, "h", 100, false); });
            go.countDown(); f1.get(); f2.get(); }
        ex.shutdown(); System.out.println("debitos efetivos = " + b.debits + " de 1000 chaves (cada chave debitada exatamente uma vez: " + (b.debits == 1000) + ")");

        System.out.println("\n== 3. Mesma chave, corpo diferente; e queda entre o efeito e o registro da chave ==");
        Bank c = new Bank();
        System.out.println("1o pedido (chave k1, R$ 10,00): " + c.debit("k1", "h-1000", 1000, false));
        System.out.println("repeticao igual               : " + c.debit("k1", "h-1000", 1000, false));
        System.out.println("mesma chave, valor diferente  : " + c.debit("k1", "h-9999", 9999, false));
        long before = c.balance;
        try { c.debit("k2", "h-500", 500, true); } catch (IllegalStateException e) { System.out.println("queda no meio de k2: " + e.getMessage()); }
        System.out.println("saldo antes/depois da queda (atomico, efeito e chave na mesma transacao): " + before + " / " + c.balance + " (efeito do k2 NAO ficou; o retry executa por inteiro)");
        // sem atomicidade: efeito persistido, chave nao
        Bank d = new Bank(); d.balance -= 500; d.debits++;   // efeito persistiu e a chave nao foi gravada
        System.out.println("SEM atomicidade (efeito gravado, chave nao): o retry de k2 -> " + d.debit("k2", "h-500", 500, false) + " ; debitos = " + d.debits + " (DUPLICADO)");
    }
}
