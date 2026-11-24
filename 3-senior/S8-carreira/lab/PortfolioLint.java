// PortfolioLint.java — S8.01: is this repository ready to be shown to a recruiter or an interviewer?
//   java PortfolioLint.java readme <README.md> <README.en.md>  -> portfolio README in PT and EN: sections, parity, diagram,
//                                                              "how to run", relative links, claims without evidence, secrets
//   java PortfolioLint.java hygiene <git repo dir>             -> what is PUBLISHED (the HEAD commit): license, README, .gitignore,
//                                                              risky file names, big files, secret-looking content (masked)
// Rules are heuristics written for THIS course (not a standard). ERROR fails the check; WARN and REVIEW ask a human.

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class PortfolioLint {

    record Finding(String level, String rule, String where, String message) {}
    static final List<Finding> findings = new ArrayList<>();

    static void add(String level, String rule, String where, String msg) { findings.add(new Finding(level, rule, where, msg)); }

    public static void main(String[] args) throws Exception {
        if (args.length >= 3 && args[0].equals("readme")) {
            Doc pt = Doc.read(Path.of(args[1])), en = Doc.read(Path.of(args[2]));
            checkReadme(pt, PT);
            checkReadme(en, EN);
            parity(pt, en);
        } else if (args.length >= 2 && args[0].equals("hygiene")) {
            hygiene(Path.of(args[1]));
        } else {
            System.out.println("usage: readme <pt> <en> | hygiene <repo>");
            return;
        }
        long errors = findings.stream().filter(f -> f.level().equals("ERROR")).count();
        for (Finding f : findings) System.out.printf("%-6s %-18s %s: %s%n", f.level(), f.rule(), f.where(), f.message());
        System.out.printf("%s: %d error(s), %d warning(s), %d to review%n", errors == 0 ? "PASS" : "FAIL", errors,
                findings.stream().filter(f -> f.level().equals("WARN")).count(),
                findings.stream().filter(f -> f.level().equals("REVIEW")).count());
    }

    // ---------------------------------------------------------------- README
    record Block(String lang, int line, List<String> lines, String section) {}
    record Line(int number, String text, String section, boolean inCode) {}

    static class Doc {
        Path path; List<Line> lines = new ArrayList<>(); List<String> sections = new ArrayList<>(); List<Block> blocks = new ArrayList<>();

        static Doc read(Path p) throws IOException {
            Doc d = new Doc(); d.path = p;
            String section = "(top)"; Block open = null; int n = 0;
            for (String s : Files.readAllLines(p)) {
                n++;
                if (s.startsWith("```")) {
                    if (open == null) { open = new Block(s.substring(3).trim(), n, new ArrayList<>(), section); }
                    else { d.blocks.add(open); open = null; }
                    d.lines.add(new Line(n, s, section, true));
                    continue;
                }
                if (open != null) { open.lines().add(s); d.lines.add(new Line(n, s, section, true)); continue; }
                if (s.startsWith("## ")) { section = s.substring(3).trim(); d.sections.add(section); }
                d.lines.add(new Line(n, s, section, false));
            }
            if (open != null) add("ERROR", "fence", p + ":" + open.line(), "code block never closed");
            return d;
        }
        String name() { return path.getFileName().toString(); }
    }

    // section id -> heading pattern (case-insensitive, start of the heading)
    static final Map<String, String> PT = new LinkedHashMap<>(), EN = new LinkedHashMap<>();
    static {
        PT.put("what", "o que é"); PT.put("shows", "o que (este projeto|ele) demonstra"); PT.put("architecture", "arquitetura");
        PT.put("run", "como rodar"); PT.put("test", "(como testar|testes)"); PT.put("decisions", "decisões");
        PT.put("limits", "limitações"); PT.put("license", "licença");
        EN.put("what", "what it is"); EN.put("shows", "what (this project|it) shows"); EN.put("architecture", "architecture");
        EN.put("run", "how to run"); EN.put("test", "(how to test|tests)"); EN.put("decisions", "decisions");
        EN.put("limits", "(known )?limitations"); EN.put("license", "license");
    }
    static final Pattern LINK = Pattern.compile("\\[[^\\]]*\\]\\(([^)\\s]+)\\)");
    static final Pattern CLAIM = Pattern.compile("(?i)(em produção|pront[oa]s? para produção|production[- ]ready|in production|milhões de usuários|"
            + "milhares de usuários|millions of users|thousands of users|clientes reais|real customers)");
    static final Pattern NEGATION = Pattern.compile("(?iU)\\b(não|nunca|not|never|no)\\b");
    static final Map<String, Pattern> SECRETS = new LinkedHashMap<>();
    static {
        SECRETS.put("aws-access-key", Pattern.compile("AKIA[0-9A-Z]{16}"));
        SECRETS.put("private-key", Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----"));
        SECRETS.put("github-token", Pattern.compile("gh[pousr]_[A-Za-z0-9]{36}"));
        SECRETS.put("password-value", Pattern.compile("(?i)(password|passwd|senha|secret)\\s*[:=]\\s*[^\\s<${]{6,}"));
    }
    static final Pattern PLACEHOLDER = Pattern.compile("\\bTODO\\b|\\bTBD\\b|(?i:lorem ipsum)");   // case-sensitive TODO (S7.01)
    static final Set<String> MERMAID_TYPES = Set.of("flowchart", "graph", "sequenceDiagram", "erDiagram", "classDiagram",
            "stateDiagram", "stateDiagram-v2", "gantt", "C4Context", "journey");

    static String sectionOf(Doc d, Map<String, String> lang, String id) {
        Pattern p = Pattern.compile("(?iU)^" + lang.get(id));
        return d.sections.stream().filter(s -> p.matcher(s).find()).findFirst().orElse(null);
    }

    static void checkReadme(Doc d, Map<String, String> lang) {
        String f = d.name();
        for (String id : lang.keySet())
            if (sectionOf(d, lang, id) == null) add("ERROR", "missing-section", f, "no section '" + lang.get(id) + "'");
        // diagram: at least one mermaid block, each starting with a known diagram type
        List<Block> mermaid = d.blocks.stream().filter(b -> b.lang().equals("mermaid")).toList();
        if (mermaid.isEmpty()) add("ERROR", "no-diagram", f, "no ```mermaid block (GitHub renders Mermaid in Markdown)");
        for (Block b : mermaid) {
            String first = b.lines().stream().map(String::trim).filter(s -> !s.isEmpty()).findFirst().orElse("");
            if (!MERMAID_TYPES.contains(first.split("\\s+")[0]))
                add("ERROR", "diagram-type", f + ":" + b.line(), "mermaid block starts with '" + first + "'");
        }
        // how to run: a shell block with 1..5 commands
        String run = sectionOf(d, lang, "run");
        if (run != null) {
            List<Block> shell = d.blocks.stream().filter(b -> b.section().equals(run) && b.lang().matches("bash|sh|shell|console")).toList();
            long commands = shell.stream().flatMap(b -> b.lines().stream()).map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.startsWith("#")).count();
            if (commands == 0) add("ERROR", "run-commands", f, "'" + run + "' has no shell commands in a ```bash block");
            else if (commands > 5) add("WARN", "run-commands", f, commands + " commands to run: aim for <= 5");
        }
        boolean badge = false;
        for (Line l : d.lines) {
            if (l.text().contains("/badge.svg")) badge = true;
            if (l.inCode()) {
                for (var s : SECRETS.entrySet())
                    if (s.getValue().matcher(l.text()).find()) add("ERROR", "secret", f + ":" + l.number(), s.getKey() + " in a code block");
                continue;
            }
            Matcher m = LINK.matcher(l.text());
            while (m.find()) {
                String target = m.group(1);
                if (target.matches("(?i)(https?|mailto):.*") || target.startsWith("#")) continue;
                String file = target.replaceAll("#.*", "");
                if (!Files.exists(d.path.toAbsolutePath().getParent().resolve(file)))
                    add("ERROR", "broken-link", f + ":" + l.number(), "relative link to '" + file + "' does not exist");
            }
            if (CLAIM.matcher(l.text()).find() && !NEGATION.matcher(l.text()).find() && !l.text().contains("]("))
                add("ERROR", "claim-no-evidence", f + ":" + l.number(), "'" + firstMatch(CLAIM, l.text()) + "' without evidence (link) or negation");
            for (var s : SECRETS.entrySet())
                if (s.getValue().matcher(l.text()).find()) add("ERROR", "secret", f + ":" + l.number(), s.getKey());
            if (PLACEHOLDER.matcher(l.text()).find()) add("WARN", "placeholder", f + ":" + l.number(), "'" + firstMatch(PLACEHOLDER, l.text()) + "'");
        }
        if (!badge) add("WARN", "no-ci-badge", f, "no CI status badge (actions/workflows/<file>/badge.svg)");
    }

    static void parity(Doc pt, Doc en) {
        long mp = pt.blocks.stream().filter(b -> b.lang().equals("mermaid")).count(), me = en.blocks.stream().filter(b -> b.lang().equals("mermaid")).count();
        if (mp != me) add("ERROR", "parity-diagrams", pt.name() + " x " + en.name(), mp + " diagram(s) in PT, " + me + " in EN");
        int sp = 0, se = 0;
        for (String id : PT.keySet()) { if (sectionOf(pt, PT, id) != null) sp++; if (sectionOf(en, EN, id) != null) se++; }
        if (sp != se) add("ERROR", "parity-sections", pt.name() + " x " + en.name(), sp + " required sections in PT, " + se + " in EN");
        Set<String> lp = relLinks(pt), le = relLinks(en);
        Set<String> onlyPt = new TreeSet<>(lp); onlyPt.removeAll(le);
        Set<String> onlyEn = new TreeSet<>(le); onlyEn.removeAll(lp);
        if (!onlyPt.isEmpty() || !onlyEn.isEmpty())
            add("WARN", "parity-links", pt.name() + " x " + en.name(), "links only in PT " + onlyPt + ", only in EN " + onlyEn);
    }

    static Set<String> relLinks(Doc d) {
        Set<String> out = new TreeSet<>();
        for (Line l : d.lines) {
            if (l.inCode()) continue;
            Matcher m = LINK.matcher(l.text());
            while (m.find()) {
                String t = m.group(1).replaceAll("#.*", "");
                if (!t.isEmpty() && !t.matches("(?i)(https?|mailto):.*") && !t.matches("README(\\.en)?\\.md")) out.add(t);
            }
        }
        return out;
    }

    static String firstMatch(Pattern p, String s) { Matcher m = p.matcher(s); return m.find() ? m.group() : ""; }

    // ---------------------------------------------------------------- repository hygiene (HEAD = what was published)
    static List<String> git(Path repo, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("git", "-C", repo.toString()));
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(false).start();
        List<String> out = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().toList();
        p.waitFor();
        return out;
    }

    static final Pattern RISKY_NAME = Pattern.compile("(^|/)\\.env$|\\.pem$|\\.key$|\\.p12$|\\.jks$|(^|/)id_(rsa|ed25519)$|(^|/)credentials|\\.tfstate");

    static void hygiene(Path repo) throws Exception {
        String head = git(repo, "rev-parse", "--short", "HEAD").getFirst();
        List<String> tree = git(repo, "ls-tree", "-r", "-l", "HEAD");
        System.out.println("repository HEAD " + head + ": " + tree.size() + " tracked files");
        Set<String> paths = new TreeSet<>();
        for (String row : tree) {
            String[] meta = row.substring(0, row.indexOf('\t')).trim().split("\\s+");
            String path = row.substring(row.indexOf('\t') + 1);
            paths.add(path);
            long size = meta[3].equals("-") ? 0 : Long.parseLong(meta[3]);
            if (size > 1024 * 1024) add("WARN", "big-file", path, String.format("%.1f MiB", size / 1048576.0));
            if (RISKY_NAME.matcher(path).find()) add("ERROR", "risky-file", path, "file name that usually holds secrets");
        }
        if (paths.stream().noneMatch(p -> p.matches("(LICENSE|LICENCE|COPYING)(\\.[A-Za-z]+)?")))
            add("ERROR", "no-license", "/", "no LICENSE: by default nobody may reproduce, distribute or create derivative works (GitHub docs)");
        if (!paths.contains("README.md")) add("ERROR", "no-readme", "/", "no README.md at the root");
        if (!paths.contains(".gitignore")) add("WARN", "no-gitignore", "/", "no .gitignore");
        else if (git(repo, "show", "HEAD:.gitignore").stream().noneMatch(s -> s.trim().matches("/?target/?")))
            add("WARN", "gitignore", ".gitignore", "does not ignore Maven's target/");
        Map<String, String> content = new LinkedHashMap<>();
        content.put("aws-access-key", "AKIA[0-9A-Z]{16}");
        content.put("private-key", "-----BEGIN [A-Z ]*PRIVATE KEY-----");
        content.put("github-token", "gh[pousr]_[A-Za-z0-9]{36}");
        content.put("password-value", "(password|passwd|senha|secret)[[:space:]]*[:=][[:space:]]*[^[:space:]<${]{6,}");
        for (var c : content.entrySet()) {
            List<String> hits = git(repo, "grep", "-n", "-I", "-i", "-E", c.getValue(), "HEAD");
            Map<String, Integer> byFile = new TreeMap<>();
            for (String h : hits) {               // HEAD:path:line:content  -> keep path only, never print the value
                String rest = h.substring(5);
                byFile.merge(rest.substring(0, rest.indexOf(':')), 1, Integer::sum);
            }
            if (!hits.isEmpty())
                add("REVIEW", c.getKey(), hits.size() + " line(s) in " + byFile.size() + " file(s)",
                        "values masked; files: " + String.join(", ", byFile.entrySet().stream().limit(6)
                                .map(e -> e.getKey() + " (" + e.getValue() + ")").toList()) + (byFile.size() > 6 ? ", ..." : ""));
        }
    }
}
