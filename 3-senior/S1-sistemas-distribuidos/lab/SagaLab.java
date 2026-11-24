import java.util.*;

/** S1.07: saga orquestrada com compensacao e recuperacao pelo log; 2PC bloqueando apos queda do coordenador. */
public class SagaLab {
    // "bancos" locais de cada servico
    static long payerBalance, merchantBalance, reserved; static boolean fraudOk; static List<String> log;
    static int failAtStep; static boolean crashAfterStep; static int compensationFailuresLeft;

    interface Step { String name(); void run(); void undo(); }

    static List<Step> steps() {
        return List.of(
            new Step() { public String name() { return "reservar"; } public void run() { if (payerBalance < 100) throw new IllegalStateException("saldo insuficiente"); payerBalance -= 100; reserved += 100; } public void undo() { if (reserved >= 100) { reserved -= 100; payerBalance += 100; } } },
            new Step() { public String name() { return "antifraude"; } public void run() { if (!fraudOk) throw new IllegalStateException("antifraude recusou"); } public void undo() { } },
            new Step() { public String name() { return "creditar"; } public void run() { merchantBalance += 100; reserved -= 100; } public void undo() { if (compensationFailuresLeft-- > 0) throw new IllegalStateException("estorno indisponivel (tentativa falhou)"); merchantBalance -= 100; reserved += 100; } },
            new Step() { public String name() { return "notificar"; } public void run() { } public void undo() { } });
    }

    static String execute(String title, int failStep, boolean fraud, int compFails, long initial, boolean retryAfterPivot) {
        payerBalance = initial; merchantBalance = 0; reserved = 0; fraudOk = fraud; failAtStep = failStep; compensationFailuresLeft = compFails; log = new ArrayList<>();
        long total0 = payerBalance + merchantBalance + reserved;
        var st = steps(); int done = 0; String outcome = "COMPLETA";
        int retries = 0;
        try { for (int i = 0; i < st.size(); i++) {
                if (i == failStep && retryAfterPivot && i >= 3) { retries = 2; log.add("FALHOU " + st.get(i).name() + " 2x e foi REPETIDO (passo apos o pivo nao se compensa)"); }
                else if (i == failStep) throw new IllegalStateException("falha injetada no passo " + st.get(i).name());
                st.get(i).run(); log.add("OK " + st.get(i).name()); done++; } }
        catch (IllegalStateException e) {
            outcome = "COMPENSADA (" + e.getMessage() + ")"; log.add("FALHA " + e.getMessage());
            for (int i = done - 1; i >= 0; i--) { int tries = 0; while (true) { try { st.get(i).undo(); log.add("DESFEZ " + st.get(i).name() + (tries > 0 ? " (apos " + tries + " tentativa(s) falha(s))" : "")); break; } catch (IllegalStateException x) { tries++; } } }
        }
        long total1 = payerBalance + merchantBalance + reserved;
        System.out.printf("%-44s -> %-48s pagador=%d lojista=%d reservado=%d dinheiro conservado=%b%n", title, outcome, payerBalance, merchantBalance, reserved, total0 == total1);
        return String.join(" | ", log);
    }

    public static void main(String[] args) {
        System.out.println("== 1. Saga orquestrada: pagar 100 (saldo 500). Falhas injetadas em cada passo ==");
        execute("caminho feliz", -1, true, 0, 500, false);
        execute("saldo insuficiente (saldo 50)", -1, true, 0, 50, false);
        execute("antifraude recusa", -1, false, 0, 500, false);
        execute("falha em 'creditar'", 2, true, 0, 500, false);
        execute("falha em 'notificar' tratada como compensavel", 3, true, 0, 500, false);
        System.out.println("   log: " + execute("idem, e o estorno de 'creditar' falha 3x", 3, true, 3, 500, false));
        execute("falha em 'notificar' REPETIDA (apos o pivo)", 3, true, 0, 500, true);
        System.out.println("\nlog da ultima saga: " + log);

        System.out.println("\n== 2. Queda do orquestrador no meio: recuperacao pelo log de passos ==");
        payerBalance = 500; merchantBalance = 0; reserved = 0; fraudOk = true; compensationFailuresLeft = 0;
        List<String> durable = new ArrayList<>(); var st = steps();
        for (int i = 0; i < 2; i++) { st.get(i).run(); durable.add("OK " + st.get(i).name()); }
        System.out.println("QUEDA apos 'antifraude'. Estado: pagador=" + payerBalance + " reservado=" + reserved + " lojista=" + merchantBalance + " ; log durável=" + durable);
        // novo processo: le o log e retoma do primeiro passo que nao esta como OK
        int resume = (int) durable.stream().filter(s -> s.startsWith("OK")).count();
        for (int i = resume; i < st.size(); i++) { st.get(i).run(); durable.add("OK " + st.get(i).name()); }
        System.out.println("apos a recuperacao: pagador=" + payerBalance + " reservado=" + reserved + " lojista=" + merchantBalance + " ; log=" + durable);

        System.out.println("\n== 3. 2PC: coordenador cai DEPOIS do PREPARE; os participantes ficam bloqueados ==");
        int ticks = 1000, blocked = 0, queuedOps = 0; boolean[] prepared = {true, true}; boolean coordinatorUp = false; Random r = new Random(4);
        for (int t = 0; t < ticks; t++) {
            if (t == 500) coordinatorUp = true;   // o coordenador volta no tick 500
            boolean locked = prepared[0] && prepared[1] && !coordinatorUp;
            if (locked) { blocked++; if (r.nextInt(10) < 3) queuedOps++; } // 30% dos ticks chega operacao sobre a conta bloqueada
        }
        System.out.printf("participantes votaram SIM e nao podem decidir sozinhos: bloqueados por %d de %d ticks; %d operacoes novas na mesma conta esperaram (ou falharam) nesse intervalo%n", blocked, ticks, queuedOps);
    }
}
