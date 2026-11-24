// HotspotLab.java — S7.04: where is the debt that costs money? Hotspots = change frequency x complexity,
// plus change coupling (files that always change together), from a REAL git history.
//   java HotspotLab.java <repo> <commit> [path-prefix]
// Example (run-all.sh clones it): spring-petclinic pinned at 500158f732419217507c7656904b8e6aa1bcc0d6, prefix src/main/java
// Needs only `git` on the PATH. Read-only: runs `git log` and `git show`, never checks out or executes the repo's code.

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class HotspotLab {

    static final int MAX_FILES_PER_COMMIT = 20;   // bigger commits (mass renames, formatting) are ignored for coupling
    static final Pattern DECISION = Pattern.compile("\\b(if|for|while|case|catch)\\b|&&|\\|\\||\\?");

    public static void main(String[] args) throws Exception {
        String repo = args[0], commit = args[1], prefix = args.length > 2 ? args[2] : "";
        List<String> log = git(repo, "log", "-M", "--numstat", "--format=@%H|%ad", "--date=short", commit);

        Map<String, String> alias = new HashMap<>();             // old path -> newer path (we walk newest -> oldest)
        Map<String, Integer> revisions = new HashMap<>(), churn = new HashMap<>();
        Map<String, Integer> pairs = new HashMap<>();
        List<String> current = new ArrayList<>();
        int commits = 0, bigCommits = 0;
        String firstDate = "", lastDate = "";
        for (String line : log) {
            if (line.startsWith("@")) {
                commits++;
                if (current.size() <= MAX_FILES_PER_COMMIT) addPairs(current, pairs); else bigCommits++;
                current = new ArrayList<>();
                String date = line.substring(line.indexOf('|') + 1);
                if (lastDate.isEmpty()) lastDate = date;
                firstDate = date;
                continue;
            }
            String[] parts = line.split("\t");
            if (parts.length < 3) continue;
            String path = parts[2];
            if (path.contains(" => ")) {                          // rename: "a/{old => new}/F.java" or "old => new"
                String[] r = expandRename(path);
                alias.put(r[0], resolve(alias, r[1]));
                path = r[1];
            }
            path = resolve(alias, path);
            int added = parts[0].equals("-") ? 0 : Integer.parseInt(parts[0]);
            int deleted = parts[1].equals("-") ? 0 : Integer.parseInt(parts[1]);
            revisions.merge(path, 1, Integer::sum);
            churn.merge(path, added + deleted, Integer::sum);
            current.add(path);
        }
        if (current.size() <= MAX_FILES_PER_COMMIT) addPairs(current, pairs); else bigCommits++;

        Set<String> alive = new HashSet<>(git(repo, "ls-tree", "-r", "--name-only", commit));
        record Row(String path, int revs, int churn, int loc, int decisions, long score) {}
        List<Row> rows = new ArrayList<>();
        for (String path : revisions.keySet()) {
            if (!alive.contains(path) || !path.startsWith(prefix) || !path.endsWith(".java")) continue;
            String code = String.join("\n", git(repo, "show", commit + ":" + path));
            String stripped = code.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "").replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\"");
            int loc = (int) stripped.lines().filter(l -> !l.isBlank()).count();
            Matcher m = DECISION.matcher(stripped);
            int d = 0; while (m.find()) d++;
            rows.add(new Row(path, revisions.get(path), churn.get(path), loc, d, (long) revisions.get(path) * Math.max(d, 1)));
        }
        System.out.printf("Repository at %s: %d commits (%s .. %s); %d files under '%s' alive at the commit; %d commits with > %d files ignored for coupling%n",
                commit.substring(0, 12), commits, firstDate, lastDate, rows.size(), prefix, bigCommits, MAX_FILES_PER_COMMIT);

        Comparator<Row> byRevs = Comparator.comparingInt(Row::revs).reversed();
        Comparator<Row> byScore = Comparator.comparingLong(Row::score).reversed().thenComparing(byRevs);
        System.out.println("\nTop 8 by revisions only (what 'git log | sort' shows):");
        rows.stream().sorted(byRevs).limit(8).forEach(r -> System.out.printf("  %3d revs  %4d decisions  %s%n", r.revs(), r.decisions(), shortName(r.path())));
        System.out.println("\nTop 8 hotspots (revisions x decision points):");
        System.out.println("  revs | churn | LOC | decisions | score | file");
        rows.stream().sorted(byScore).limit(8).forEach(r -> System.out.printf("  %4d | %5d | %3d | %9d | %5d | %s%n",
                r.revs(), r.churn(), r.loc(), r.decisions(), r.score(), shortName(r.path())));

        int totalRevs = rows.stream().mapToInt(Row::revs).sum();
        int topN = Math.max(1, rows.size() / 10);
        int topRevs = rows.stream().sorted(byRevs).limit(topN).mapToInt(Row::revs).sum();
        System.out.printf("%nConcentration: top %d of %d files (10%%) = %.0f%% of all revisions of alive files%n",
                topN, rows.size(), 100.0 * topRevs / totalRevs);

        System.out.println("\nChange coupling (pairs changed together in >= 8 commits; degree = shared / revisions of the less changed file):");
        Set<String> aliveJava = rows.stream().map(Row::path).collect(Collectors.toSet());
        pairs.entrySet().stream()
                .filter(e -> e.getValue() >= 8)
                .filter(e -> Arrays.stream(e.getKey().split("\\|")).allMatch(aliveJava::contains))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(6)
                .forEach(e -> {
                    String[] ab = e.getKey().split("\\|");
                    int min = Math.min(revisions.get(ab[0]), revisions.get(ab[1]));
                    System.out.printf("  %3d shared (%3.0f%%)  %s <-> %s%n", e.getValue(), 100.0 * e.getValue() / min, shortName(ab[0]), shortName(ab[1]));
                });
    }

    static void addPairs(List<String> files, Map<String, Integer> pairs) {
        List<String> f = files.stream().distinct().sorted().toList();
        for (int i = 0; i < f.size(); i++) for (int j = i + 1; j < f.size(); j++) pairs.merge(f.get(i) + "|" + f.get(j), 1, Integer::sum);
    }

    static String resolve(Map<String, String> alias, String path) {
        String p = path; int guard = 0;
        while (alias.containsKey(p) && guard++ < 50) p = alias.get(p);
        return p;
    }

    static String[] expandRename(String path) {                 // returns {old, new}
        Matcher m = Pattern.compile("^(.*)\\{(.*) => (.*)\\}(.*)$").matcher(path);
        if (m.find()) {
            String oldP = (m.group(1) + m.group(2) + m.group(4)).replace("//", "/");
            String newP = (m.group(1) + m.group(3) + m.group(4)).replace("//", "/");
            return new String[]{oldP, newP};
        }
        String[] s = path.split(" => ");
        return new String[]{s[0], s[1]};
    }

    static String shortName(String path) {
        return path.replaceFirst("^src/main/java/org/springframework/samples/petclinic/", "…/");
    }

    static List<String> git(String repo, String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("git", "-C", repo));
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(false).start();
        List<String> out;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            out = r.lines().toList();
        }
        if (p.waitFor() != 0) throw new IllegalStateException("git failed: " + cmd);
        return out;
    }
}
