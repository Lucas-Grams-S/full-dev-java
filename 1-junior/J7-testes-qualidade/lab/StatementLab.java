import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Laboratório da J7.07: legado -> refatoração (mesmo comportamento) -> correção do dinheiro (comportamento novo). */
public class StatementLab {

    static String[] r(String... fields) {
        return fields;
    }

    static List<String[]> rows(String[]... rows) {
        return Arrays.asList(rows);
    }

    /** Uma entrada do "golden master": o nome, a entrada e a função sob teste. */
    record Case(String name, List<String[]> input, boolean verbose) { }

    static String outcome(Function<Case, String> subject, Case c) {
        try {
            return subject.apply(c);
        } catch (RuntimeException e) {
            return "throws " + e.getClass().getSimpleName();
        }
    }

    public static void main(String[] args) {
        List<String[]> sample = rows(r("C", "100.10", "2026-11-01"), r("D", "30.05", "2026-11-02"), r("C", "5.00", "2026-11-03"));
        List<String[]> messy = new ArrayList<>();
        messy.add(null);
        messy.add(r("C", "10", "2026-11-01"));
        messy.add(r("D", "x"));
        messy.add(r("D", "4.5", "2026-11-04"));
        List<Case> cases = List.of(
                new Case("sample", sample, false),
                new Case("sample verbose", sample, true),
                new Case("empty", rows(), true),
                new Case("null and malformed rows are skipped", messy, true),
                new Case("unknown type", rows(r("X", "1", "d")), false),
                new Case("unparseable amount", rows(r("C", "abc", "d")), false),
                new Case("bad type AND bad amount", rows(r("X", "abc", "d")), false),
                new Case("zero and negative amounts", rows(r("C", "0", "2026-11-01"), r("D", "-3", "2026-11-02")), true),
                new Case("floating point", rows(r("C", "0.10", "a"), r("C", "0.20", "b"), r("D", "0.30", "c")), false));

        int different = 0;
        for (Case c : cases) {
            String legacy = outcome(k -> Legacy.build(k.input(), k.verbose()), c);
            String refactored = outcome(k -> k.verbose() ? Report.summaryWithLargest(k.input()) : Report.summary(k.input()), c);
            String money = outcome(k -> k.verbose() ? MoneyReport.summaryWithLargest(k.input()) : MoneyReport.summary(k.input()), c);
            boolean same = legacy.equals(refactored);
            if (!same) {
                different++;
            }
            System.out.printf("%-38s legacy=%s%n%-38s refactored %s%n%-38s money=%s%n%n", c.name(), legacy, "", same ? "== legacy" : "!= " + refactored, "", money);
        }
        System.out.println("refactor differs from legacy in " + different + " case(s)");
    }
}

/** O código herdado, como chegou (nomes, aninhamento, flag, double para dinheiro). Não editar: é o ponto de partida. */
class Legacy {
    // rows: {type, amount, date}; type: "C" credit, "D" debit
    static String build(List<String[]> rows, boolean v) {
        double c = 0;
        double d = 0;
        int n = 0;
        String big = null;
        double bigv = 0;
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            if (r != null) {
                if (r.length == 3) {
                    double a = Double.parseDouble(r[1]);
                    if (r[0].equals("C")) {
                        c = c + a;
                    } else {
                        if (r[0].equals("D")) {
                            d = d + a;
                        } else {
                            throw new RuntimeException("bad type");
                        }
                    }
                    n++;
                    if (a > bigv) {
                        bigv = a;
                        big = r[2];
                    }
                }
            }
        }
        String s = "credits=" + c + " debits=" + d + " balance=" + (c - d) + " entries=" + n;
        if (v) {
            s = s + " largest=" + bigv + " on " + big;
        }
        if (n == 0) {
            return "empty";
        }
        return s;
    }
}

enum EntryKind {
    CREDIT, DEBIT;

    static EntryKind parse(String code) {
        return switch (code) {
            case "C" -> CREDIT;
            case "D" -> DEBIT;
            default -> throw new IllegalArgumentException("unknown entry type: " + code);
        };
    }
}

/** Passos 1 a 3: mesmo comportamento, código legível. Ainda em double (o bug do dinheiro fica para o passo 4). */
final class Report {

    private Report() {
    }

    private record Row(EntryKind kind, double amount, String date) {

        /** Linha nula ou com tamanho diferente de 3 é ignorada, como no legado (decisão a revisar: ver Exercícios). */
        static Optional<Row> parse(String[] raw) {
            if (raw == null || raw.length != 3) {
                return Optional.empty();
            }
            double amount = Double.parseDouble(raw[1]);        // o valor é lido ANTES do tipo, como no legado
            return Optional.of(new Row(EntryKind.parse(raw[0]), amount, raw[2]));
        }
    }

    static String summary(List<String[]> raw) {
        return render(parse(raw), false);
    }

    static String summaryWithLargest(List<String[]> raw) {
        return render(parse(raw), true);
    }

    private static List<Row> parse(List<String[]> raw) {
        List<Row> rows = new ArrayList<>();
        for (String[] fields : raw) {
            Row.parse(fields).ifPresent(rows::add);
        }
        return rows;
    }

    private static String render(List<Row> rows, boolean withLargest) {
        if (rows.isEmpty()) {
            return "empty";
        }
        double credits = total(rows, EntryKind.CREDIT);
        double debits = total(rows, EntryKind.DEBIT);
        String text = "credits=" + credits + " debits=" + debits + " balance=" + (credits - debits) + " entries=" + rows.size();
        return withLargest ? text + largest(rows) : text;
    }

    /** Laço simples, na mesma ordem do legado: DoubleStream.sum() usaria soma compensada e poderia mudar os últimos dígitos. */
    private static double total(List<Row> rows, EntryKind kind) {
        double sum = 0;
        for (Row row : rows) {
            if (row.kind() == kind) {
                sum = sum + row.amount();
            }
        }
        return sum;
    }

    /** Preserva a esquisitice do legado: com valores zero ou negativos, nenhuma linha vence ("largest=0.0 on null"). */
    private static String largest(List<Row> rows) {
        Row best = null;
        double bestAmount = 0;
        for (Row row : rows) {
            if (row.amount() > bestAmount) {
                best = row;
                bestAmount = row.amount();
            }
        }
        return " largest=" + bestAmount + " on " + (best == null ? null : best.date());
    }
}

/** Passo 4 (COMPORTAMENTO NOVO, em commit separado): dinheiro em BigDecimal com 2 casas (decisão 4). */
final class MoneyReport {

    private MoneyReport() {
    }

    private record Row(EntryKind kind, BigDecimal amount, String date) {

        static Optional<Row> parse(String[] raw) {
            if (raw == null || raw.length != 3) {
                return Optional.empty();
            }
            BigDecimal amount = new BigDecimal(raw[1]).setScale(2, RoundingMode.UNNECESSARY);   // mais de 2 casas: erro
            return Optional.of(new Row(EntryKind.parse(raw[0]), amount, raw[2]));
        }
    }

    static String summary(List<String[]> raw) {
        return render(parse(raw), false);
    }

    static String summaryWithLargest(List<String[]> raw) {
        return render(parse(raw), true);
    }

    private static List<Row> parse(List<String[]> raw) {
        List<Row> rows = new ArrayList<>();
        for (String[] fields : raw) {
            Row.parse(fields).ifPresent(rows::add);
        }
        return rows;
    }

    private static String render(List<Row> rows, boolean withLargest) {
        if (rows.isEmpty()) {
            return "empty";
        }
        BigDecimal credits = total(rows, EntryKind.CREDIT);
        BigDecimal debits = total(rows, EntryKind.DEBIT);
        String text = "credits=" + credits.toPlainString() + " debits=" + debits.toPlainString()
                + " balance=" + credits.subtract(debits).toPlainString() + " entries=" + rows.size();
        return withLargest ? text + largest(rows) : text;
    }

    private static BigDecimal total(List<Row> rows, EntryKind kind) {
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        for (Row row : rows) {
            if (row.kind() == kind) {
                sum = sum.add(row.amount());
            }
        }
        return sum;
    }

    private static String largest(List<Row> rows) {
        Row best = null;
        BigDecimal bestAmount = BigDecimal.ZERO.setScale(2);
        for (Row row : rows) {
            if (row.amount().compareTo(bestAmount) > 0) {
                best = row;
                bestAmount = row.amount();
            }
        }
        return " largest=" + bestAmount.toPlainString() + " on " + (best == null ? null : best.date());
    }
}
