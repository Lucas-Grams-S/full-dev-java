import module java.base;                                   // Java 25 (JEP 511): importa todos os pacotes exportados de java.base
import module java.net.http;                            // HttpClient mora em OUTRO módulo (java.net.http): não vem junto com java.base

/** P1.11 — recursos da linguagem do Java 8 ao 25, no domínio do PayFlow. Uso: java Modern.java */
public class Modern {

    // ===== Java 16/17: records e sealed =====
    record Money(BigDecimal amount) {
        Money {                                              // construtor compacto: valida e normaliza (decisão 4: 2 casas, HALF_EVEN)
            Objects.requireNonNull(amount);
            amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        }
        Money plus(Money o) { return new Money(amount.add(o.amount)); }
        static Money of(String s) { return new Money(new BigDecimal(s)); }
    }

    sealed interface PaymentEvent permits Deposited, Transferred, Failed {}
    record Deposited(String account, Money amount) implements PaymentEvent {}
    record Transferred(String from, String to, Money amount) implements PaymentEvent {}
    record Failed(String reason, int code) implements PaymentEvent {}

    // ===== Java 21: switch com padrões, record patterns, guardas (when) e exaustividade (sem default!) =====
    static String describe(PaymentEvent e) {
        return switch (e) {
            case Deposited(var acc, var m) when m.amount().compareTo(new BigDecimal("1000")) > 0 -> "depósito GRANDE em " + acc + ": " + m.amount();
            case Deposited(var acc, var m) -> "depósito em " + acc + ": " + m.amount();
            case Transferred(var from, var to, var m) -> from + " -> " + to + ": " + m.amount();
            case Failed(var reason, _) -> "falhou: " + reason;       // Java 22: `_` = variável não usada
        };
    }

    // ===== Java 25 (JEP 513, final): código ANTES do super(...)/this(...) =====
    static class Base { Base(String id) { System.out.println("  Base recebeu id normalizado = " + id); } }
    static class Derived extends Base {
        Derived(String rawId) {
            String id = rawId.strip().toLowerCase();                 // validar/normalizar antes de chamar o super
            if (id.isEmpty()) throw new IllegalArgumentException("id vazio");
            super(id);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("== Java 8: lambdas, Optional, streams, java.time");
        List<Money> ms = List.of(Money.of("10.005"), Money.of("20.015"), Money.of("1.10"));
        System.out.println("  " + ms + " soma=" + ms.stream().reduce(Money.of("0"), Money::plus));   // 10.005 -> 10.00 (HALF_EVEN), 20.015 -> 20.02
        Optional<String> o = Optional.of("x");
        System.out.println("  Optional: " + o.map(String::toUpperCase).orElse("?") + " " + Optional.empty().or(() -> Optional.of("alt")).get() + " " + o.stream().count());
        System.out.println("  Instant/Duration: " + Duration.between(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T06:30:00Z")));

        System.out.println("== Java 9-11: List.of imutável, var, String e HttpClient");
        try { List.of(1).add(2); } catch (UnsupportedOperationException e) { System.out.println("  List.of é imutável: UnsupportedOperationException"); }
        var text = "  PayFlow \n ";
        System.out.println("  strip='" + text.strip() + "' isBlank=" + " ".isBlank() + " repeat=" + "ab".repeat(3) + " lines=" + "a\nb\nc".lines().count());
        System.out.println("  Map.of/entry: " + new TreeMap<>(Map.of("a", 1, "b", 2)) + " " + Map.entry("k", "v"));
        System.out.println("  HttpClient: " + HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build().version());

        System.out.println("== Java 14-15: switch expressão e text block");
        int dia = 3;
        String nome = switch (dia) { case 1, 7 -> "fim de semana"; case 2, 3, 4, 5, 6 -> { String t = "útil"; yield t; } default -> throw new IllegalArgumentException(); };
        String json = """
                {"balance":"110.00",
                 "currency":"BRL"}""";
        System.out.println("  " + nome + " | " + json.replace("\n", "").replace("  ", " "));

        System.out.println("== Java 16-21: records, sealed, pattern matching");
        Object obj = new Deposited("A", Money.of("5"));
        if (obj instanceof Deposited d && d.amount().amount().signum() > 0) System.out.println("  instanceof com padrão: " + d.account());
        for (PaymentEvent e : List.of(new Deposited("A", Money.of("2500")), new Deposited("B", Money.of("5")), new Transferred("A", "B", Money.of("9.999")), new Failed("saldo insuficiente", 422)))
            System.out.println("  " + describe(e));
        System.out.println("  record: equals=" + Money.of("1.00").equals(Money.of("1")) + " hashCode igual=" + (Money.of("1.00").hashCode() == Money.of("1").hashCode()) + " toString=" + Money.of("1"));

        System.out.println("== Java 21: sequenced collections e Math.clamp (21)");
        List<Integer> l = new ArrayList<>(List.of(1, 2, 3));
        System.out.println("  first=" + l.getFirst() + " last=" + l.getLast() + " reversed=" + l.reversed() + " LinkedHashSet.reversed=" + new LinkedHashSet<>(l).reversed() + " clamp=" + Math.clamp(150, 0, 100));

        System.out.println("== Java 12-24: Collectors.teeing, mapMulti, takeWhile e Gatherers (24)");
        var stats = Stream.of(10, 20, 30).collect(Collectors.teeing(Collectors.counting(), Collectors.summingInt(i -> i), (n, s) -> "n=" + n + " soma=" + s));
        System.out.println("  teeing: " + stats);
        System.out.println("  takeWhile: " + Stream.of(1, 2, 3, 10, 4).takeWhile(i -> i < 5).toList());
        System.out.println("  mapMulti: " + Stream.of("a,b", "c").<String>mapMulti((s, sink) -> { for (String p : s.split(",")) sink.accept(p); }).toList());
        System.out.println("  Gatherers.windowSliding(3): " + Stream.of(1, 2, 3, 4, 5).gather(Gatherers.windowSliding(3)).toList());
        System.out.println("  Gatherers.windowFixed(2):   " + Stream.of(1, 2, 3, 4, 5).gather(Gatherers.windowFixed(2)).toList());
        System.out.println("  Gatherers.fold:             " + Stream.of(1, 2, 3).gather(Gatherers.fold(() -> 0, (a, b) -> a + b)).findFirst().get());

        System.out.println("== Java 22 (`_`), 25 (construtor flexível, módulo importado)");
        var m = new HashMap<String, Integer>(); m.put("a", 1);
        for (var _ : m.entrySet()) System.out.println("  `_` em for: não preciso do nome da variável");
        try (var _ = new java.io.StringReader("x")) { System.out.println("  `_` em try-with-resources"); }
        new Derived("  ABC-1  ");
        try { new Derived("   "); } catch (IllegalArgumentException e) { System.out.println("  construtor flexível validou antes do super: " + e.getMessage()); }

        System.out.println("== API pequena e útil");
        System.out.println("  HexFormat: " + HexFormat.of().formatHex(new byte[] {(byte) 0xCA, (byte) 0xFE}) + " | UUID v7 não existe na JDK (J3: gerado pelo Hibernate) | " + Objects.requireNonNullElse(null, "padrão") + " | " + "%s-%03d".formatted("tx", 7));
    }
}
