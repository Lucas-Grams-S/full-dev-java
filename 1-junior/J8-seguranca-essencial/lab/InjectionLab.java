import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * J8.05: injeção de SQL de verdade, num H2 em memória (só para este exemplo: nos testes do PayFlow, nunca H2).
 * Rode (JDK 25), com o jar do H2 baixado do Maven Central:
 *   curl -sO https://repo1.maven.org/maven2/com/h2database/h2/2.4.240/h2-2.4.240.jar
 *   java -cp h2-2.4.240.jar InjectionLab.java
 */
public class InjectionLab {

    static List<String> emailsVulnerable(Connection c, String email) throws SQLException {
        String sql = "SELECT email FROM users WHERE email = '" + email + "'";          // ❌ concatenação
        System.out.println("   SQL executado: " + sql);
        List<String> found = new ArrayList<>();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                found.add(rs.getString(1));
            }
        }
        return found;
    }

    static List<String> emailsSafe(Connection c, String email) throws SQLException {
        String sql = "SELECT email FROM users WHERE email = ?";                         // ✅ parâmetro
        List<String> found = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    found.add(rs.getString(1));
                }
            }
        }
        return found;
    }

    /** Nome de coluna não pode ser parâmetro: lista permitida. */
    static String orderBy(String requested) {
        Set<String> allowed = Set.of("email", "created_at");
        if (!allowed.contains(requested)) {
            throw new IllegalArgumentException("sort column not allowed");
        }
        return requested;
    }

    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:lab;DB_CLOSE_DELAY=-1")) {
            try (Statement st = c.createStatement()) {
                st.execute("CREATE TABLE users (email VARCHAR(255), created_at INT)");
                st.execute("INSERT INTO users VALUES ('ana@payflow.dev', 1), ('bruno@payflow.dev', 2), ('admin@payflow.dev', 3)");
            }
            String normal = "ana@payflow.dev";
            String attack = "' OR '1'='1";
            System.out.println("1. Entrada normal (vulnerável): " + emailsVulnerable(c, normal));
            System.out.println("2. Ataque \"" + attack + "\" (vulnerável):");
            System.out.println("   devolveu: " + emailsVulnerable(c, attack));
            System.out.println("3. Ataque com PreparedStatement: " + emailsSafe(c, attack));
            String stacked = "x'; DROP TABLE users; --";
            System.out.println("4. Empilhar comandos (vulnerável): ");
            try {
                emailsVulnerable(c, stacked);
            } catch (SQLException e) {
                System.out.println("   erro: " + e.getMessage().lines().findFirst().orElse(""));
            }
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
                rs.next();
                System.out.println("   a tabela ainda existe? linhas = " + rs.getInt(1));
            } catch (SQLException e) {
                System.out.println("   a tabela FOI APAGADA: " + e.getMessage().lines().findFirst().orElse(""));
                System.out.println("   (o H2 executou vários comandos separados por ';' em uma só chamada; o que o seu banco e driver aceitam varia: não conte com isso como defesa)");
            }
            try (Statement st = c.createStatement()) {                                  // recria para os próximos exemplos
                st.execute("CREATE TABLE users (email VARCHAR(255), created_at INT)");
                st.execute("INSERT INTO users VALUES ('ana@payflow.dev', 1), ('bruno@payflow.dev', 2), ('admin@payflow.dev', 3)");
            }
            String union = "' UNION SELECT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC' --";
            System.out.println("5. UNION para ler o catálogo (vulnerável): " + emailsVulnerable(c, union));
            System.out.println("6. Coluna de ordenação vinda do usuário: ");
            for (String requested : new String[] {"email", "created_at DESC; DROP TABLE users"}) {
                try {
                    System.out.println("   \"" + requested + "\" -> ORDER BY " + orderBy(requested));
                } catch (IllegalArgumentException e) {
                    System.out.println("   \"" + requested + "\" -> recusado: " + e.getMessage());
                }
            }
        }
    }
}
