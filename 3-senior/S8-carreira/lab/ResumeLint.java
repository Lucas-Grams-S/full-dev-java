// ResumeLint.java — S8.02: a resume (Markdown) read the way a recruiter and an interviewer will read it.
//   java ResumeLint.java cv <cv.md>              -> bullets (action verb, number, size, honesty), personal data, skills without evidence,
//                                                  and the LEVEL SIGNALS (impact, scope, decision, people)
//   java ResumeLint.java match <cv.md> <job.md>  -> the job's technologies: proven in a bullet, only listed, or missing
// Heuristics for Portuguese resumes, written for THIS course (not an ATS, not a standard). ERROR = fix before sending.

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class ResumeLint {

    record Bullet(int line, String section, String text) {}

    static final Pattern WEAK_START = Pattern.compile("(?iU)^(responsável|auxiliei|ajudei|participei|atuei|colaborei|trabalhei|"
            + "conhecimento|experiência|desenvolvimento|atividades|suporte)\\b");
    static final Pattern FIRST_PERSON_PAST = Pattern.compile("(?iU)^(\\p{L}+(ei|i|í)|fiz|propus|mantive|tive|fui|pude|trouxe|refiz|obtive|"
            + "compus|expus|dispus)\\b");
    static final Pattern CLAIM = Pattern.compile("(?iU)(em produção|pront[oa]s? para produção|usuários reais|milhares de usuários|"
            + "milhões de usuários|clientes reais|\\bclientes\\b)");
    static final Pattern NEGATION = Pattern.compile("(?iU)\\b(não|nunca|sem)\\b");
    static final Pattern IMPACT_NUMBER = Pattern.compile("\\d+(,\\d+)?\\s*(%|x\\b|vezes)");
    static final Pattern CONTEXT = Pattern.compile("(?iU)(laboratório|modelo|medi|medido|teste|local|simulad|estimad|premissa)");
    static final Map<String, Pattern> PERSONAL = new LinkedHashMap<>();
    static {
        PERSONAL.put("CPF", Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b"));
        PERSONAL.put("RG", Pattern.compile("\\bRG\\b"));
        PERSONAL.put("birth date / age", Pattern.compile("(?iU)data de nascimento|nascid[oa] em|\\bidade\\b"));
        PERSONAL.put("marital status", Pattern.compile("(?iU)estado civil|\\bcasad[oa]\\b|\\bsolteir[oa]\\b"));
        PERSONAL.put("photo", Pattern.compile("!\\["));
    }
    static final Map<String, Pattern> SIGNALS = new LinkedHashMap<>();
    static {
        SIGNALS.put("impact (number)", Pattern.compile("\\d"));
        SIGNALS.put("scope (teams, clients, org)", Pattern.compile("(?iU)\\b(times?|equipes?|áreas|clientes|empresa|organização|squads?)\\b"));
        SIGNALS.put("decision (trade-off, ADR, RFC)", Pattern.compile("(?iU)\\b(decidi|escolhi|propus|defini|recomendei|adrs?|rfcs?|design docs?|trade-?offs?)\\b"));
        SIGNALS.put("people (mentoring, reviews)", Pattern.compile("(?iU)\\b(mentorei|orientei|conduzi|revisei|ensinei|treinei|onboarding|entrevistei)\\b"));
    }
    // canonical technology -> how it is written
    static final Map<String, Pattern> TECH = new LinkedHashMap<>();
    static {
        String[][] t = {{"Java", "\\bjava\\b(?![- ]?script)"}, {"Spring Boot", "spring boot"}, {"JPA/Hibernate", "\\bjpa\\b|hibernate"},
                {"PostgreSQL", "postgres(ql)?"}, {"SQL", "\\bsql\\b"}, {"REST APIs", "\\brest\\b"}, {"automated tests", "junit|testes? (de unidade|automatizad|de integração)|testcontainers"},
                {"Testcontainers", "testcontainers"}, {"Kafka", "kafka"}, {"RabbitMQ", "rabbitmq"}, {"Docker", "docker"},
                {"Kubernetes", "kubernetes|\\bk8s\\b"}, {"AWS", "\\baws\\b"}, {"CI/CD", "ci/cd|github actions|pipeline"},
                {"Terraform", "terraform"}, {"observability", "observabilidade|prometheus|grafana|opentelemetry"},
                {"OAuth2/OIDC", "oauth ?2?|oidc|keycloak"}, {"microservices", "microsservi[cç]os?"}, {"Git", "\\bgit\\b"}};
        for (String[] x : t) TECH.put(x[0], Pattern.compile("(?iU)" + x[1]));
    }

    static List<String> lines;
    static List<Bullet> bullets = new ArrayList<>();
    static Map<String, String> sectionText = new LinkedHashMap<>();
    static int errors, warnings;

    static void read(Path p) throws Exception {
        lines = Files.readAllLines(p);
        bullets.clear(); sectionText.clear();
        String section = "(top)";
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            if (s.startsWith("## ")) { section = s.substring(3).trim(); continue; }
            sectionText.merge(section, s + "\n", String::concat);
            if (s.startsWith("- ") && section.matches("(?iU)experiência.*|projetos.*")) bullets.add(new Bullet(i + 1, section, s.substring(2).trim()));
        }
    }

    static void report(String level, String where, String msg) {
        if (level.equals("ERROR")) errors++; else warnings++;
        System.out.printf("%-5s %-6s %s%n", level, where, msg);
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && args[0].equals("cv")) cv(Path.of(args[1]));
        else if (args.length >= 3 && args[0].equals("match")) match(Path.of(args[1]), Path.of(args[2]));
        else System.out.println("usage: cv <cv.md> | match <cv.md> <job.md>");
    }

    static void cv(Path p) throws Exception {
        read(p);
        errors = 0; warnings = 0;
        String all = String.join("\n", lines);
        System.out.println("== " + p.getFileName() + ": " + all.split("\\s+").length + " words, " + bullets.size() + " bullets in Experiência/Projetos");
        for (Bullet b : bullets) {
            String w = "L" + b.line();
            String first = b.text().split("\\s+")[0].replaceAll("[^\\p{L}]", "");
            if (WEAK_START.matcher(b.text()).find()) report("WARN", w, "weak start '" + first + "': say what YOU did (Implementei, Medi, Reduzi...)");
            else if (!FIRST_PERSON_PAST.matcher(first).find()) report("WARN", w, "does not start with a first-person past verb ('" + first + "')");
            if (!b.text().matches(".*\\d.*")) report("WARN", w, "no number: what changed, and by how much?");
            int words = b.text().split("\\s+").length;
            if (words > 32) report("WARN", w, words + " words: one idea per bullet (<= 32)");
            boolean study = b.section().matches("(?iU)projetos.*");
            if (study && CLAIM.matcher(b.text()).find() && !NEGATION.matcher(b.text()).find())
                report("ERROR", w, "study project claiming '" + first(CLAIM, b.text()) + "': not true, and the interview will ask");
            if (IMPACT_NUMBER.matcher(b.text()).find() && !CONTEXT.matcher(b.text()).find())
                report("WARN", w, "'" + first(IMPACT_NUMBER, b.text()) + "' without context: measured where, against what?");
        }
        for (var e : PERSONAL.entrySet())
            if (e.getValue().matcher(all).find()) report("WARN", "doc", e.getKey() + ": not needed to screen a developer (minimization, S5.05)");
        if (!all.contains("github.com/")) report("WARN", "doc", "no GitHub link: where is the evidence?");
        if (all.split("\\s+").length > 600) report("WARN", "doc", "more than 600 words (premise of this lab: about one page)");
        String skills = sectionText.entrySet().stream().filter(e -> e.getKey().matches("(?iU)competências.*|habilidades.*"))
                .map(Map.Entry::getValue).findFirst().orElse("");
        String bulletText = String.join("\n", bullets.stream().map(Bullet::text).toList());
        List<String> unproven = new ArrayList<>();
        for (var t : TECH.entrySet())
            if (t.getValue().matcher(skills).find() && !t.getValue().matcher(bulletText).find()) unproven.add(t.getKey());
        if (!unproven.isEmpty()) report("WARN", "skills", "listed but never shown in a bullet: " + unproven);
        System.out.println("level signals (bullets with ...):");
        for (var s : SIGNALS.entrySet()) {
            long n = bullets.stream().filter(b -> s.getValue().matcher(b.text()).find()).count();
            System.out.printf("   %-32s %d of %d%n", s.getKey(), n, bullets.size());
        }
        System.out.println((errors == 0 ? "PASS" : "FAIL") + ": " + errors + " error(s), " + warnings + " warning(s)");
    }

    static void match(Path cvPath, Path jobPath) throws Exception {
        String job = Files.readString(jobPath);
        read(cvPath);
        String bulletText = String.join("\n", bullets.stream().map(Bullet::text).toList());
        String everything = String.join("\n", lines);
        int asked = 0, proven = 0, listed = 0;
        System.out.println("== " + cvPath.getFileName() + " x " + jobPath.getFileName());
        System.out.println("technology the job asks for | in the resume");
        for (var t : TECH.entrySet()) {
            if (!t.getValue().matcher(job).find()) continue;
            asked++;
            String status;
            if (t.getValue().matcher(bulletText).find()) { proven++; status = "proven in a bullet"; }
            else if (t.getValue().matcher(everything).find()) { listed++; status = "only listed"; }
            else status = "MISSING";
            System.out.printf("   %-24s | %s%n", t.getKey(), status);
        }
        System.out.printf("coverage: %d of %d proven in a bullet, %d only listed, %d missing%n", proven, asked, listed, asked - proven - listed);
    }

    static String first(Pattern p, String s) { Matcher m = p.matcher(s); return m.find() ? m.group() : ""; }
}
