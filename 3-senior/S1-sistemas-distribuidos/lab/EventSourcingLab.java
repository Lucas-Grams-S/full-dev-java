import java.util.*;

/** S1.06: event sourcing: agregado, concorrencia otimista, snapshots, upcasting e correcao por evento compensatorio. */
public class EventSourcingLab {
    sealed interface Ev permits Opened, Deposited, Withdrawn, DepositedV1 {}
    record Opened(String id) implements Ev {}
    record Deposited(String id, long cents) implements Ev {}          // v2: centavos inteiros
    record Withdrawn(String id, long cents) implements Ev {}
    record DepositedV1(String id, double amount) implements Ev {}     // v1 legado: double (erro de projeto antigo)

    static final class Store {
        final Map<String, List<Ev>> streams = new HashMap<>();
        void append(String id, long expectedVersion, Ev e) {
            var s = streams.computeIfAbsent(id, k -> new ArrayList<>());
            if (s.size() != expectedVersion) throw new IllegalStateException("conflito: esperava versao " + expectedVersion + " mas o fluxo esta na " + s.size());
            s.add(e);
        }
    }
    static Ev upcast(Ev e) { return e instanceof DepositedV1 v ? new Deposited(v.id(), Math.round(v.amount() * 100)) : e; }
    static long apply(long bal, Ev e) {
        return switch (upcast(e)) { case Deposited d -> bal + d.cents(); case Withdrawn w -> bal - w.cents(); case Opened o -> bal; case DepositedV1 v -> throw new AssertionError(); };
    }

    public static void main(String[] args) {
        Store st = new Store();
        System.out.println("== 1. Concorrencia otimista no fluxo (expectedVersion) ==");
        st.append("acc-1", 0, new Opened("acc-1")); st.append("acc-1", 1, new Deposited("acc-1", 10_000));
        // dois comandos leem a versao 2 ao mesmo tempo
        st.append("acc-1", 2, new Withdrawn("acc-1", 6_000));
        try { st.append("acc-1", 2, new Withdrawn("acc-1", 6_000)); } catch (IllegalStateException e) { System.out.println("segundo saque concorrente: " + e.getMessage()); }
        long bal = 0; for (Ev e : st.streams.get("acc-1")) bal = apply(bal, e);
        System.out.println("saldo = " + bal + " centavos (nunca negativo: o segundo saque foi recusado, nao aplicado)");

        System.out.println("\n== 2. Snapshots: carregar um agregado com 1.000.000 de eventos ==");
        List<Ev> big = new ArrayList<>(); big.add(new Opened("x")); Random r = new Random(2);
        for (int i = 0; i < 1_000_000; i++) big.add(r.nextBoolean() ? new Deposited("x", 100) : new Withdrawn("x", 50));
        long t = System.nanoTime(); long full = 0; for (Ev e : big) full = apply(full, e); long tFull = System.nanoTime() - t;
        int snapAt = 999_000; long snapBal = 0; for (int i = 0; i < snapAt; i++) snapBal = apply(snapBal, big.get(i));
        t = System.nanoTime(); long fromSnap = snapBal; for (int i = snapAt; i < big.size(); i++) fromSnap = apply(fromSnap, big.get(i)); long tSnap = System.nanoTime() - t;
        System.out.printf("sem snapshot: %d eventos lidos em %d ms; com snapshot na posicao %d: %d eventos em %d ms; mesmo saldo? %b%n", big.size(), tFull / 1_000_000, snapAt, big.size() - snapAt, tSnap / 1_000_000, full == fromSnap);

        System.out.println("\n== 3. Evolucao do evento (upcasting): v1 com double, v2 com centavos ==");
        List<Ev> old = List.of(new Opened("acc-2"), new DepositedV1("acc-2", 0.10), new DepositedV1("acc-2", 0.20), new DepositedV1("acc-2", 0.30));
        double soma = 0; for (Ev e : old) if (e instanceof DepositedV1 v) soma += v.amount();
        long b = 0; for (Ev e : old) b = apply(b, e);
        System.out.println("somando os double do v1: " + soma + " (imprecisao do double); apos o upcast para centavos inteiros: " + b + " centavos (= 0,60)");

        System.out.println("\n== 4. Corrigir um erro: nunca editar o passado; acrescentar um evento que o compensa ==");
        List<Ev> s = new ArrayList<>(List.of(new Opened("acc-3"), new Deposited("acc-3", 5_000), new Deposited("acc-3", 5_000))); // deposito duplicado por bug
        long before = 0; for (Ev e : s) before = apply(before, e);
        s.add(new Withdrawn("acc-3", 5_000));   // evento de correcao (no PayFlow: lancamento de estorno, decisao 114)
        long after = 0; for (Ev e : s) after = apply(after, e);
        System.out.println("saldo com o deposito duplicado = " + before + "; apos o evento de correcao = " + after + "; o historico (" + s.size() + " eventos) ficou inteiro e auditavel");
    }
}
