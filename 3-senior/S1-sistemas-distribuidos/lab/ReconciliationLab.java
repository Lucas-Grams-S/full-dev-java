import java.util.*;

/** S1.12: partidas dobradas e conciliacao de 100.000 pagamentos contra o extrato do PSP, com anomalias plantadas. */
public class ReconciliationLab {
    record Payment(String id, long cents, String status) {}
    public static void main(String[] args) {
        Random r = new Random(33); int n = 100_000;
        List<Payment> ours = new ArrayList<>(), psp = new ArrayList<>(); Map<String, String> planted = new HashMap<>();
        for (int i = 0; i < n; i++) {
            String id = "pay-" + i; long amt = 100 + r.nextInt(100_000);
            ours.add(new Payment(id, amt, "CAPTURED")); psp.add(new Payment(id, amt, "SETTLED"));
        }
        // anomalias plantadas (posicoes fixas, conhecidas)
        for (int i = 0; i < 30; i++) { psp.set(i * 1000, null); planted.put("pay-" + i * 1000, "FALTA_NO_PSP"); }
        for (int i = 0; i < 20; i++) { int k = i * 1000 + 7; psp.set(k, new Payment("pay-" + k, psp.get(k).cents() + 1, "SETTLED")); planted.put("pay-" + k, "VALOR_DIFERENTE"); }
        for (int i = 0; i < 10; i++) { int k = i * 1000 + 13; psp.add(new Payment("pay-" + k, psp.get(k).cents(), "SETTLED")); planted.put("pay-" + k, "DUPLICADO_NO_PSP"); }
        for (int i = 0; i < 5; i++) { psp.add(new Payment("ext-" + i, 5000, "SETTLED")); planted.put("ext-" + i, "SO_NO_PSP"); }
        long t0 = System.nanoTime();
        Map<String, List<Payment>> byId = new HashMap<>(); for (Payment p : psp) if (p != null) byId.computeIfAbsent(p.id(), k -> new ArrayList<>()).add(p);
        Map<String, String> found = new TreeMap<>();
        for (Payment p : ours) { var l = byId.remove(p.id()); if (l == null) found.put(p.id(), "FALTA_NO_PSP"); else if (l.size() > 1) found.put(p.id(), "DUPLICADO_NO_PSP"); else if (l.get(0).cents() != p.cents()) found.put(p.id(), "VALOR_DIFERENTE"); }
        for (String k : byId.keySet()) found.put(k, "SO_NO_PSP");
        long ms = (System.nanoTime() - t0) / 1_000_000;
        Map<String, Integer> count = new TreeMap<>(); found.values().forEach(v -> count.merge(v, 1, Integer::sum));
        System.out.println("== Conciliacao de " + n + " pagamentos (extrato do PSP com anomalias plantadas) ==");
        System.out.println("encontradas por categoria: " + count + " em " + ms + " ms (inclui geracao dos mapas)");
        System.out.println("plantadas = 30 FALTA_NO_PSP + 20 VALOR_DIFERENTE + 10 DUPLICADO_NO_PSP + 5 SO_NO_PSP; encontradas exatamente as plantadas: " + found.equals(planted));
        // partidas dobradas: soma(debitos) == soma(creditos) por transacao
        System.out.println("\n== Partidas dobradas: cada transferencia gera um debito e um credito de mesmo valor ==");
        long debits = 0, credits = 0; for (int i = 0; i < n; i++) { long a = 100 + r.nextInt(100_000); debits += a; credits += a; }
        System.out.println("soma dos debitos = " + debits + "; soma dos creditos = " + credits + "; invariante global (soma = 0): " + (debits - credits == 0));
        credits -= 1;  // um lancamento perdido
        System.out.println("com UM credito perdido de 1 centavo: soma dos debitos - creditos = " + (debits - credits) + " -> o invariante detecta, mesmo sem saber qual lancamento");
    }
}
