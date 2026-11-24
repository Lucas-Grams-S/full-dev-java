// StarLint.java — S8.04: behavioral stories in STAR form (Situação, Tarefa, Ação, Resultado) and a story bank by theme.
//   java StarLint.java story <file.md>   -> the four parts, Ação as the longest part, "eu" x "nós" in the Ação, a result with a
//                                           number or a lesson, blame words, estimated speaking time
//   java StarLint.java bank <dir>        -> every story + a theme x story matrix: which themes have NO usable story
// Themes: the focus areas listed by the Tech Interview Handbook ("Behavioral interview rubrics") plus two that its page for
// senior candidates says are often missing (people leadership, stakeholder management). Heuristics, not a standard.
// PREMISE: 130 spoken words per minute (time yourself; it varies a lot between people).

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class StarLint {

    static final List<String> PARTS = List.of("Situação", "Tarefa", "Ação", "Resultado");
    static final List<String> THEMES = List.of("motivação", "iniciativa", "ambiguidade", "perseverança", "conflito", "empatia",
            "crescimento", "comunicação", "liderança de pessoas", "stakeholders");
    static final Pattern LABEL = Pattern.compile("^\\*\\*(Situação|Tarefa|Ação|Resultado):\\*\\*\\s*(.*)$");
    static final Pattern SINGULAR = Pattern.compile("(?iU)\\b(eu|meu|minha|meus|minhas|\\p{L}+ei|li|escrevi|decidi|corrigi|pedi|medi|reduzi|"
            + "conduzi|defini|assumi|resolvi|aprendi|percebi|entendi|recebi|ouvi|insisti|consegui|propus|fiz)\\b");
    static final Pattern PLURAL = Pattern.compile("(?iU)\\b(nós|nosso|nossa|nossos|nossas|a gente|o time|a equipe|\\p{L}+(amos|emos))\\b");
    static final Pattern LESSON = Pattern.compile("(?iU)\\d|aprendi|passei a|mudei|hoje eu|desde então");
    static final Pattern BLAME = Pattern.compile("(?iU)\\bculpa\\b|culpado|incompetente|preguiçoso|\\bburro\\b|erro d[oa] (colega|outro|fulano)");
    static final double WORDS_PER_MINUTE = 130;   // PREMISE

    record Story(String file, Map<String, String> parts, List<String> themes, List<String> errors, List<String> warnings, int words) {}

    static Story analyze(Path p) throws Exception {
        Map<String, String> parts = new LinkedHashMap<>();
        List<String> themes = new ArrayList<>();
        String current = null;
        for (String line : Files.readAllLines(p)) {
            if (line.startsWith("Temas:")) {
                for (String t : line.substring(6).split(",")) themes.add(t.trim().toLowerCase());
                continue;
            }
            Matcher m = LABEL.matcher(line);
            if (m.find()) { current = m.group(1); parts.put(current, m.group(2)); }
            else if (current != null && !line.isBlank() && !line.startsWith("#")) parts.merge(current, " " + line.trim(), String::concat);
        }
        List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
        for (String part : PARTS) if (!parts.containsKey(part)) errors.add("missing part: " + part);
        for (String t : themes) if (!THEMES.contains(t)) warnings.add("unknown theme '" + t + "'");
        Map<String, Integer> words = new LinkedHashMap<>();
        for (var e : parts.entrySet()) words.put(e.getKey(), e.getValue().trim().split("\\s+").length);
        if (parts.containsKey("Ação")) {
            String longest = Collections.max(words.entrySet(), Map.Entry.comparingByValue()).getKey();
            if (!longest.equals("Ação")) warnings.add("the longest part is '" + longest + "', not 'Ação': tell what YOU did");
            long me = SINGULAR.matcher(parts.get("Ação")).results().count(), we = PLURAL.matcher(parts.get("Ação")).results().count();
            if (we >= me) warnings.add("Ação has " + we + " 'nós/o time' x " + me + " 'eu': use I statements (MIT CAPD)");
        }
        if (parts.containsKey("Resultado") && !LESSON.matcher(parts.get("Resultado")).find())
            warnings.add("Resultado without a number or a lesson");
        String all = String.join(" ", parts.values());
        if (BLAME.matcher(all).find()) errors.add("blame: '" + BLAME.matcher(all).results().findFirst().get().group() + "' (blameless, S4.03)");
        int total = words.values().stream().mapToInt(Integer::intValue).sum();
        if (total / WORDS_PER_MINUTE > 3.0) warnings.add(String.format(Locale.US, "~%.1f min to tell (premise 130 words/min): cut", total / WORDS_PER_MINUTE));
        if (themes.isEmpty()) warnings.add("no 'Temas:' line");
        return new Story(p.getFileName().toString(), parts, themes, errors, warnings, total);
    }

    static void print(Story s) {
        System.out.printf(Locale.US, "== %s: %d words (~%.1f min), parts %s, themes %s%n", s.file(), s.words(), s.words() / WORDS_PER_MINUTE,
                s.parts().keySet(), s.themes());
        s.errors().forEach(e -> System.out.println("   ERROR " + e));
        s.warnings().forEach(w -> System.out.println("   WARN  " + w));
        System.out.println("   " + (s.errors().isEmpty() ? "PASS" : "FAIL"));
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && args[0].equals("story")) print(analyze(Path.of(args[1])));
        else if (args.length >= 2 && args[0].equals("bank")) bank(Path.of(args[1]));
        else System.out.println("usage: story <file.md> | bank <dir>");
    }

    static void bank(Path dir) throws Exception {
        List<Story> stories = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".md")).sorted().toList()) stories.add(analyze(p));
        }
        stories.forEach(StarLint::print);
        System.out.println();
        System.out.printf("%-22s", "theme \\ story");
        for (int i = 0; i < stories.size(); i++) System.out.printf(" | %-2d", i + 1);
        System.out.println(" | usable stories");
        List<String> gaps = new ArrayList<>();
        for (String t : THEMES) {
            System.out.printf("%-22s", t);
            int usable = 0;
            for (Story s : stories) {
                boolean has = s.themes().contains(t);
                if (has && s.errors().isEmpty()) usable++;
                System.out.printf(" | %-2s", has ? (s.errors().isEmpty() ? "x" : "x!") : "");
            }
            System.out.println(" | " + usable);
            if (usable == 0) gaps.add(t);
        }
        System.out.println("(x! = the story covers the theme but FAILS the lint: it does not count)");
        System.out.println(gaps.isEmpty() ? "BANK PASS: every theme has a usable story" : "BANK FAIL: no usable story for " + gaps);
    }
}
