// DocLab.java — S7.01/S7.02: linters for ADRs (and their index), RFCs and design docs.
// Run with JDK 25 (source launcher):
//   java DocLab.java adr ../../../projeto/adrs
//   java DocLab.java rfc samples/rfc-0001-recurring-transfers.md
//   java DocLab.java design samples/design-recurring-transfers.md
// A linter checks FORM (sections, owners, dates, vocabulary), never whether the decision is right.
// -Dtoday=YYYY-MM-DD fixes "today" so the output is deterministic.

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class DocLab {

    static final LocalDate TODAY = LocalDate.parse(System.getProperty("today", "2026-11-23"));
    static int errors = 0, warnings = 0;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("usage: DocLab adr <dir> | rfc <file> | design <file>");
            System.exit(2);
        }
        switch (args[0]) {
            case "adr" -> lintAdrs(Path.of(args[1]));
            case "rfc" -> lintRfc(Path.of(args[1]));
            case "design" -> lintDesign(Path.of(args[1]));
            default -> { System.out.println("unknown mode " + args[0]); System.exit(2); }
        }
        System.out.printf("%nResult: %d error(s), %d warning(s) -> %s%n",
                errors, warnings, errors == 0 ? "PASS" : "FAIL");
        System.exit(errors == 0 ? 0 : 1);
    }

    // ---------------------------------------------------------------- ADRs
    // ADR fields are bullet lines like "- **Status:** ..." (the format used since ADR 0001).
    static final Pattern FIELD = Pattern.compile("^- \\*\\*([^*:(]+)");
    static final Set<String> ADR_STATUS = Set.of("proposta", "aceita", "rejeitada", "substituída", "obsoleta");
    static final int BUNDLE_RULES = 3, BUNDLE_WORDS = 800;   // proposal: one decision per ADR

    static Map<String, String> fields(List<String> lines) {
        Map<String, String> out = new LinkedHashMap<>();
        String current = null;
        StringBuilder value = new StringBuilder();
        for (String line : lines) {
            Matcher m = FIELD.matcher(line);
            if (m.find()) {
                if (current != null) out.put(current, value.toString());
                current = m.group(1).trim();
                value = new StringBuilder(line);
            } else if (current != null) {
                value.append('\n').append(line);
            }
        }
        if (current != null) out.put(current, value.toString());
        return out;
    }

    static Optional<String> field(Map<String, String> f, String prefix) {
        return f.entrySet().stream().filter(e -> e.getKey().startsWith(prefix)).map(Map.Entry::getValue).findFirst();
    }

    static String firstWordAfterColon(String fieldText) {
        String rest = fieldText.substring(fieldText.indexOf("**", 4) + 2).replace(":", " ").trim();
        String w = rest.split("[\\s,(]+")[0].replace("*", "").toLowerCase(Locale.ROOT);
        return w;
    }

    static void lintAdrs(Path dir) throws IOException {
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString().matches("\\d{4}-.*\\.md")).sorted().toList();
        }
        // index rows: | [0001](0001-....md) | title | module | status |
        Map<String, List<String>> index = new HashMap<>();
        Path readme = dir.resolve("README.md");
        for (String line : Files.readAllLines(readme)) {
            Matcher m = Pattern.compile("^\\| \\[(\\d{4})\\]\\(([^)]+)\\)").matcher(line);
            if (m.find()) {
                List<String> cells = Arrays.stream(line.split("\\|", -1)).map(String::trim).toList();
                index.put(m.group(1), cells.subList(1, cells.size() - 1)); // drop outer empties
            }
        }
        int bundles = 0;
        System.out.printf("ADR  | words | rules | findings%n");
        for (Path p : files) {
            String name = p.getFileName().toString();
            String num = name.substring(0, 4);
            List<String> lines = Files.readAllLines(p);
            List<String> found = new ArrayList<>();
            int words = String.join(" ", lines).split("\\s+").length;
            int rules = (int) lines.stream().filter(l -> l.matches("^ {2}\\d+\\. .*")).count();

            if (lines.isEmpty() || !lines.get(0).startsWith("# ADR " + num + " ")) found.add("E title-number");
            Map<String, String> f = fields(lines);
            for (String req : List.of("Status", "Contexto", "Decisão", "Opções", "Consequências")) {
                if (field(f, req).isEmpty()) found.add("E missing:" + req);
            }
            String status = field(f, "Status").map(DocLab::firstWordAfterColon).orElse("?");
            if (!ADR_STATUS.contains(status)) found.add("E status:" + status);
            if (field(f, "Revisar quando").isEmpty() && field(f, "Revisão prevista").isEmpty())
                found.add("W no-review-trigger");
            String cons = field(f, "Consequências").orElse("");
            if (!cons.contains("(−)") && !cons.contains("(-)")) found.add("W no-negative-consequence");
            if (rules > BUNDLE_RULES || words > BUNDLE_WORDS) { found.add("W bundle"); bundles++; }

            List<String> row = index.get(num);
            if (row == null) found.add("E index-missing");
            else {
                if (!row.get(0).contains("(" + name + ")")) found.add("E index-link");
                if (row.size() != 4) found.add("E index-cells=" + row.size());
                else if (!row.get(3).toLowerCase(Locale.ROOT).startsWith(status)) found.add("E index-status:" + row.get(3));
            }
            for (String x : found) { if (x.startsWith("E")) errors++; else warnings++; }
            System.out.printf("%s | %5d | %5d | %s%n", num, words, rules, found.isEmpty() ? "OK" : String.join(", ", found));
        }
        System.out.printf("%nADRs: %d · bundles (> %d rules or > %d words): %d%n", files.size(), BUNDLE_RULES, BUNDLE_WORDS, bundles);
    }

    // ---------------------------------------------------------------- markdown sections
    // Returns heading (## level 2, lowercased) -> body until the next level-2 heading.
    static Map<String, String> sections(List<String> lines) {
        Map<String, String> out = new LinkedHashMap<>();
        String current = null;
        StringBuilder b = new StringBuilder();
        boolean inFence = false;
        for (String line : lines) {
            if (line.startsWith("```")) inFence = !inFence;
            if (!inFence && line.startsWith("## ")) {
                if (current != null) out.put(current, b.toString());
                current = line.substring(3).trim().toLowerCase(Locale.ROOT);
                b = new StringBuilder();
            } else if (current != null) b.append(line).append('\n');
        }
        if (current != null) out.put(current, b.toString());
        return out;
    }

    static Optional<String> section(Map<String, String> s, String startsWith) {
        return s.entrySet().stream().filter(e -> e.getKey().startsWith(startsWith)).map(Map.Entry::getValue).findFirst();
    }

    static Map<String, String> meta(List<String> lines) {      // "- **Key:** value" lines before the first "## "
        Map<String, String> out = new LinkedHashMap<>();
        for (String l : lines) {
            if (l.startsWith("## ")) break;
            Matcher m = Pattern.compile("^- \\*\\*([^*]+?):\\*\\*\\s*(.*)$").matcher(l);
            if (m.find()) out.put(m.group(1).trim().toLowerCase(Locale.ROOT), m.group(2).trim());
        }
        return out;
    }

    static void report(String level, String code, String msg) {
        if (level.equals("E")) errors++; else warnings++;
        System.out.printf("%s %-22s %s%n", level, code, msg);
    }

    static long bullets(String body) {
        return body.lines().filter(l -> l.matches("^\\s*([-*]|\\d+\\.) .*")).count();
    }

    // Two bugs found by running the linter on real Portuguese text (kept here as a lesson):
    //  1) "(?i)TODO" matched the Portuguese word "todo" ("todo mês" = every month) -> TODO/TBD are case-SENSITIVE;
    //  2) Java's \b is ASCII-only by default: "Método" has a "boundary" between "é" and "todo"
    //     -> UNICODE_CHARACTER_CLASS makes accented letters word characters.
    static final Pattern PLACEHOLDER = Pattern.compile("\\b(TODO|TBD)\\b|(?i:\\ba definir\\b)", Pattern.UNICODE_CHARACTER_CLASS);
    static final Set<String> RFC_STATUS = Set.of("rascunho", "em revisão", "aceita", "rejeitada", "retirada", "substituída");

    // ---------------------------------------------------------------- RFC
    static void lintRfc(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        Map<String, String> m = meta(lines), s = sections(lines);
        String text = String.join("\n", lines);
        System.out.println("RFC: " + file.getFileName() + " (" + text.split("\\s+").length + " words)");

        for (String req : List.of("resumo", "problema", "proposta", "alternativas", "riscos", "perguntas em aberto", "decisão"))
            if (section(s, req).isEmpty()) report("E", "missing-section", req);

        String status = m.getOrDefault("status", "?").replace("*", "").toLowerCase(Locale.ROOT).split(" \\(| —")[0].trim();
        if (!RFC_STATUS.contains(status)) report("E", "status", "'" + status + "' not in " + new TreeSet<>(RFC_STATUS));

        String approver = m.getOrDefault("decisor", "");
        if (approver.isBlank()) report("E", "no-approver", "DACI: exactly one Approver (\"Decisor\")");
        else if (approver.contains(",") || approver.contains(" e ")) report("E", "many-approvers", approver);

        String until = m.getOrDefault("comentários até", "");
        Matcher d = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})").matcher(until);
        if (!d.find()) report("E", "no-comment-deadline", "a review without a date never ends");
        else if (status.equals("em revisão") && LocalDate.parse(d.group(1)).isBefore(TODAY))
            report("W", "deadline-passed", d.group(1) + " < " + TODAY + ": decide or extend");

        section(s, "alternativas").ifPresent(alt -> {
            long n = alt.lines().filter(l -> l.startsWith("### ")).count();
            if (n < 2) report("E", "alternatives", n + " option(s); need >= 2");
            if (!alt.toLowerCase(Locale.ROOT).contains("não fazer")) report("W", "no-do-nothing", "\"não fazer nada\" is always an option");
        });
        section(s, "riscos").ifPresent(r -> {
            long withoutOwner = r.lines().filter(l -> l.matches("^\\s*[-*] .*") && !l.contains("dono:")).count();
            if (withoutOwner > 0) report("E", "risk-without-owner", withoutOwner + " risk(s) without \"dono:\"");
        });
        if (status.equals("aceita")) {
            String adr = m.getOrDefault("adr resultante", "");
            if (!adr.matches(".*\\d{4}.*")) report("E", "accepted-without-adr", "an accepted RFC must point to its ADR");
        }
        Matcher ph = PLACEHOLDER.matcher(text);
        int placeholders = 0; while (ph.find()) placeholders++;
        if (placeholders > 0) report(status.equals("aceita") ? "E" : "W", "placeholders", placeholders + " TODO/TBD/a definir");
        int words = text.split("\\s+").length;
        if (words > 2500) report("W", "too-long", words + " words: details belong in the design doc");
    }

    // ---------------------------------------------------------------- design doc
    static void lintDesign(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        Map<String, String> s = sections(lines);
        String text = String.join("\n", lines);
        System.out.println("Design doc: " + file.getFileName() + " (" + text.split("\\s+").length + " words)");

        for (String req : List.of("contexto e escopo", "objetivos", "não-objetivos", "desenho", "alternativas consideradas",
                "segurança", "observabilidade", "custo", "plano de entrega", "riscos", "perguntas em aberto"))
            if (section(s, req).isEmpty()) report("E", "missing-section", req);

        section(s, "não-objetivos").ifPresent(b -> {
            if (bullets(b) == 0) report("E", "empty-non-goals", "say what you will NOT do");
        });
        section(s, "objetivos").ifPresent(b -> {
            if (!b.matches("(?s).*\\d.*")) report("W", "goals-without-number", "a goal without a number cannot be checked");
        });
        section(s, "alternativas consideradas").ifPresent(b -> {
            List<String> alts = Arrays.stream(b.split("(?m)^### ")).skip(1).toList();
            if (alts.size() < 2) report("E", "alternatives", alts.size() + " alternative(s); need >= 2");
            long noWhy = alts.stream().filter(a -> !a.toLowerCase(Locale.ROOT).contains("porque")).count();
            if (noWhy > 0) report("W", "alternative-without-why", noWhy + " alternative(s) without \"porque\"");
        });
        section(s, "plano de entrega").ifPresent(b -> {
            if (!b.toLowerCase(Locale.ROOT).contains("reversão")) report("E", "no-rollback", "how do we go back?");
        });
        section(s, "riscos").ifPresent(b -> {
            long withoutOwner = b.lines().filter(l -> l.matches("^\\s*[-*] .*") && !l.contains("dono:")).count();
            if (withoutOwner > 0) report("E", "risk-without-owner", withoutOwner + " risk(s) without \"dono:\"");
        });
        section(s, "perguntas em aberto").ifPresent(b -> {
            long withoutOwner = b.lines().filter(l -> l.matches("^\\s*[-*] .*") && !l.contains("dono:")).count();
            if (withoutOwner > 0) report("W", "question-without-owner", withoutOwner + " question(s) without \"dono:\"");
        });
        section(s, "observabilidade").ifPresent(b -> {
            if (!b.contains("SLO") || !b.matches("(?s).*\\d+(,\\d+)?\\s?%.*")) report("W", "no-slo-number", "SLO with a number");
        });
        section(s, "custo").ifPresent(b -> {
            if (!b.contains("US$") && !b.contains("R$")) report("W", "cost-without-money", "cost in currency, dated");
        });
        if (!text.contains("```mermaid")) report("W", "no-diagram", "one diagram saves a meeting");
        Matcher ph = PLACEHOLDER.matcher(text);
        int placeholders = 0; while (ph.find()) placeholders++;
        if (placeholders > 0) report("W", "placeholders", placeholders + " TODO/TBD/a definir");
        int words = text.split("\\s+").length;
        if (words > 6000) report("W", "too-long", words + " words: split or summarize");
    }
}
