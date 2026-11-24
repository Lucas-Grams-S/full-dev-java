import java.util.*;

/** S1.13: fan-out na escrita x na leitura x hibrido, com seguidores em lei de potencia; deduplicacao de notificacoes. */
public class FeedLab {
    public static void main(String[] args) {
        int users = 100_000; Random r = new Random(21);
        // seguidores por autor ~ Zipf(s=1.0): poucos autores gigantes
        double[] w = new double[users]; double sum = 0; for (int i = 0; i < users; i++) { w[i] = 1.0 / (i + 1); sum += w[i]; }
        long totalFollows = 5_000_000L;                      // 100.000 usuarios seguindo ~50 cada
        long[] followers = new long[users]; for (int i = 0; i < users; i++) followers[i] = Math.max(1, Math.min(users - 1, Math.round(totalFollows * w[i] / sum)));
        System.out.println("== 1. Modelo: 100.000 usuarios, 5.000.000 relacoes de seguir (Zipf s=1,0). Seguidores do maior autor = " + followers[0] + "; do 100o = " + followers[99] + "; do 10.000o = " + followers[9999] + " ==");
        long posts = 1_000_000; double[] pw = new double[users]; double psum = 0; for (int i = 0; i < users; i++) { pw[i] = 1.0 / Math.sqrt(i + 1); psum += pw[i]; } // quem posta mais: menos concentrado
        // contagem esperada de posts por autor
        long[] postsBy = new long[users]; for (int i = 0; i < users; i++) postsBy[i] = Math.round(posts * pw[i] / psum);
        long pushWrites = 0, pushWritesHybrid = 0; int threshold = 10_000; int celebs = 0; long celebFollowerSum = 0;
        for (int i = 0; i < users; i++) { pushWrites += postsBy[i] * followers[i]; if (followers[i] <= threshold) pushWritesHybrid += postsBy[i] * followers[i]; else { celebs++; celebFollowerSum += followers[i]; } }
        System.out.printf("posts: %,d; entregas na escrita (push puro) = %,d (amplificacao %.0fx)%n", posts, pushWrites, (double) pushWrites / posts);
        System.out.printf("hibrido (autores com mais de %,d seguidores = %d 'celebridades' lidos na hora): entregas = %,d (%.1f%% do push puro)%n", threshold, celebs, pushWritesHybrid, 100.0 * pushWritesHybrid / pushWrites);
        double maxPostFan = followers[0]; System.out.printf("um unico post do maior autor gera %,.0f escritas no push puro%n", maxPostFan);
        // leitura: abrir o feed
        long followingAvg = totalFollows / users;
        System.out.printf("%n== 2. Custo de ABRIR o feed: pull puro le os posts recentes de ~%d contas seguidas por abertura; push le 1 lista pronta; hibrido le 1 lista + (celebridades que voce segue, em media %.1f) ==%n", followingAvg, (double) celebFollowerSum / users);

        System.out.println("\n== 3. Notificacoes: 1.000.000 de eventos de 20.000 usuarios, 25% duplicados por reentrega at-least-once; deduplicar por (usuario, tipo, referencia) numa janela ==");
        List<String> evs = new ArrayList<>(); for (int i = 0; i < 750_000; i++) evs.add((r.nextInt(20_000)) + "|like|post-" + i);
        List<String> withDup = new ArrayList<>(evs); for (int i = 0; i < 250_000; i++) withDup.add(evs.get(r.nextInt(evs.size())));
        Collections.shuffle(withDup, r);
        Set<String> seen = new HashSet<>(); int delivered = 0, dropped = 0; for (String e : withDup) if (seen.add(e)) delivered++; else dropped++;
        System.out.printf("eventos=%,d entregues=%,d descartados como duplicados=%,d (esperado 250.000)%n", withDup.size(), delivered, dropped);
        // agrupamento (batching) por usuario: "N curtidas" em vez de N notificacoes
        Map<String, Integer> perUser = new HashMap<>(); for (String e : evs) perUser.merge(e.substring(0, e.indexOf('|')), 1, Integer::sum);
        System.out.printf("sem agrupar: %,d push; agrupando por usuario na janela: %,d push (%.0f%% menos)%n", evs.size(), perUser.size(), 100.0 * (1 - (double) perUser.size() / evs.size()));
    }
}
