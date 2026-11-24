import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * S4.02/S4.03 — lints runbooks and postmortems: required sections, every action has an owner and a date,
 * no blame wording. A LINT, not a judge of quality: it catches what a human reviewer forgets.
 * Run: java DocLint.java runbook FILE | postmortem FILE | selftest DIR
 */
public class DocLint {
    static final List<String> RUNBOOK = List.of("Impacto", "Diagnóstico", "Mitigação", "Escalar", "Verificação");
    static final List<String> POSTMORTEM = List.of("Resumo", "Impacto", "Linha do tempo", "Causa raiz", "O que funcionou", "O que não funcionou", "Ações");
    static final List<String> BLAME = List.of("culpa", "esqueceu", "negligên", "incompetên", "descuido", "falha humana", "erro humano");
    static final List<String> VAGUE = List.of("mais cuidado", "melhorar o", "melhorar a", "prestar atenção", "ficar atento");
    static final Pattern OWNER = Pattern.compile("@[a-z0-9-]+");
    static final Pattern DATE = Pattern.compile("\\b20\\d\\d-\\d\\d-\\d\\d\\b");

    public static void main(String[] a) throws Exception {
        if (a[0].equals("selftest")) { selftest(Path.of(a[1])); return; }
        List<String> p = lint(a[0], Files.readString(Path.of(a[1])));
        System.out.println(a[1] + ": " + (p.isEmpty() ? "OK" : p.size() + " problem(s)"));
        p.forEach(x -> System.out.println("  - " + x));
        System.exit(p.isEmpty() ? 0 : 1);
    }

    static List<String> lint(String kind, String text) {
        List<String> out = new ArrayList<>();
        Map<String, String> sections = sections(text);
        for (String req : kind.equals("runbook") ? RUNBOOK : POSTMORTEM) {
            String body = sections.entrySet().stream().filter(e -> e.getKey().toLowerCase().startsWith(req.toLowerCase()))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            if (body == null) out.add("missing section: " + req);
            else if (body.isBlank()) out.add("empty section: " + req);
        }
        if (!OWNER.matcher(text).find()) out.add("no owner (@name) anywhere");
        String low = text.toLowerCase();
        for (String w : BLAME) if (low.contains(w)) out.add("blame wording: \"" + w + "\" (describe what the SYSTEM allowed)");
        if (kind.equals("runbook")) {
            if (!text.contains("Dono:")) out.add("no 'Dono:' line");
            if (!text.contains("Revisado em:")) out.add("no 'Revisado em:' line");
            String diag = sections.entrySet().stream().filter(e -> e.getKey().startsWith("Diagnóstico")).map(Map.Entry::getValue).findFirst().orElse("");
            if (!diag.isBlank() && !Pattern.compile("(?m)^\\s*\\d+\\.").matcher(diag).find()) out.add("Diagnóstico is not an ordered list of steps");
            String mit = sections.entrySet().stream().filter(e -> e.getKey().startsWith("Mitigação")).map(Map.Entry::getValue).findFirst().orElse("");
            if (!mit.isBlank() && !mit.toLowerCase().contains("rollback") && !mit.contains("`")) out.add("Mitigação has no command or rollback");
        } else {
            String act = sections.entrySet().stream().filter(e -> e.getKey().startsWith("Ações")).map(Map.Entry::getValue).findFirst().orElse("");
            int n = 0;
            for (String line : act.split("\n")) {
                if (!line.trim().startsWith("-")) continue;
                n++;
                for (String v : VAGUE) if (line.toLowerCase().contains(v)) out.add("vague action (not verifiable): " + line.trim());
                if (!OWNER.matcher(line).find()) out.add("action without owner: " + line.trim());
                if (!DATE.matcher(line).find()) out.add("action without date: " + line.trim());
            }
            if (!act.isBlank() && n == 0) out.add("Ações has no list items");
            if (!text.contains("Linha do tempo") || !Pattern.compile("(?m)^-\\s+\\d\\d:\\d\\d").matcher(text).find())
                out.add("timeline has no timestamped entries (- HH:MM ...)");
        }
        return out;
    }

    static Map<String, String> sections(String text) {
        Map<String, String> m = new LinkedHashMap<>();
        String cur = null; StringBuilder sb = new StringBuilder();
        for (String line : text.split("\n")) {
            if (line.startsWith("## ")) { if (cur != null) m.put(cur, sb.toString()); cur = line.substring(3).trim(); sb = new StringBuilder(); }
            else if (cur != null) sb.append(line).append('\n');
        }
        if (cur != null) m.put(cur, sb.toString());
        return m;
    }

    static void selftest(Path dir) throws Exception {
        String[][] cases = {{"runbook", "runbook-good.md", "0"}, {"runbook", "runbook-bad.md", "1"},
                {"postmortem", "postmortem-good.md", "0"}, {"postmortem", "postmortem-bad.md", "1"}};
        int fails = 0;
        for (String[] c : cases) {
            List<String> p = lint(c[0], Files.readString(dir.resolve(c[1])));
            boolean ok = (p.isEmpty() ? "0" : "1").equals(c[2]);
            System.out.printf("%-22s expected %s -> %s (%d problems)%n", c[1], c[2].equals("0") ? "OK  " : "FAIL", ok ? "as expected" : "UNEXPECTED", p.size());
            if (!c[2].equals("0")) p.forEach(x -> System.out.println("     - " + x));
            if (!ok) fails++;
        }
        System.out.println(fails == 0 ? "selftest PASSED (the lint accepts the good ones and rejects the bad ones)" : "selftest FAILED");
        System.exit(fails);
    }
}
