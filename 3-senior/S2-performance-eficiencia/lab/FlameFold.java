import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * S2.03 - transforma a saida de "jfr print --events jdk.ExecutionSample --stack-depth 64 x.jfr"
 * no formato "colapsado" (o mesmo que o async-profiler escreve com -o collapsed e que o
 * flamegraph.pl consome): "raiz;...;folha contagem". Tambem imprime uma arvore de texto.
 *   java FlameFold.java <jfr-print.txt> [profundidade-da-arvore] [limite-%]
 */
public class FlameFold {
    static class Node {
        final String name; long total; final Map<String, Node> kids = new TreeMap<>();
        Node(String n) { name = n; }
    }

    static String clean(String line) {                // "pkg.Cls.metodo(args) line: 12" -> "Cls.metodo"
        String s = line.trim();
        int p = s.indexOf('(');
        if (p > 0) s = s.substring(0, p);
        int dot = s.lastIndexOf('.', s.lastIndexOf('.') - 1);
        return dot >= 0 ? s.substring(dot + 1) : s;
    }

    public static void main(String[] args) throws Exception {
        int depth = args.length > 1 ? Integer.parseInt(args[1]) : 6;
        double minPct = args.length > 2 ? Double.parseDouble(args[2]) : 2.0;
        Map<String, Long> collapsed = new TreeMap<>();
        Node root = new Node("(todas as amostras)");
        List<String> frames = null;
        for (String line : Files.readAllLines(Path.of(args[0]))) {
            String t = line.trim();
            if (t.startsWith("stackTrace = [")) { frames = new ArrayList<>(); continue; }
            if (frames != null) {
                if (t.equals("]")) {
                    Collections.reverse(frames);                        // raiz primeiro
                    if (!frames.isEmpty()) {
                        collapsed.merge(String.join(";", frames), 1L, Long::sum);
                        Node n = root; n.total++;
                        for (String f : frames) { n = n.kids.computeIfAbsent(f, Node::new); n.total++; }
                    }
                    frames = null;
                } else if (!t.equals("...")) frames.add(clean(t));
            }
        }
        System.out.println("== colapsado (as 5 pilhas mais frequentes) ==");
        collapsed.entrySet().stream().sorted((x, y) -> Long.compare(y.getValue(), x.getValue())).limit(5)
                .forEach(e -> System.out.println(e.getValue() + "  " + e.getKey()));
        System.out.println();
        System.out.println("== arvore (de cima para baixo; so ramos >= " + minPct + "% do total) ==");
        print(root, 0, depth, root.total, minPct);
    }

    static void print(Node n, int lvl, int max, long total, double minPct) {
        System.out.printf(Locale.ROOT, "%s%5.1f%%  %s%n", "  ".repeat(lvl), 100.0 * n.total / total, n.name);
        if (lvl >= max) return;
        n.kids.values().stream().sorted((x, y) -> Long.compare(y.total, x.total))
                .filter(k -> 100.0 * k.total / total >= minPct).forEach(k -> print(k, lvl + 1, max, total, minPct));
    }
}
