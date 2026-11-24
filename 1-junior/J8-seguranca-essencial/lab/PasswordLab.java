import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * J8.02: o que um hash de senha faz de verdade. Rode (JDK 25), a partir da pasta lab/security-lab:
 *   CP=$(mvn -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout)
 *   java -cp "$CP" ../PasswordLab.java
 */
public class PasswordLab {

    public static void main(String[] args) throws Exception {
        String password = "Payflow#Dev2026";

        // 1. Um hash rápido (SHA-256) não serve para senha: bilhões por segundo em GPU.
        byte[] sha = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
        System.out.println("SHA-256 (sem sal, igual para todo mundo): " + java.util.HexFormat.of().formatHex(sha));
        byte[] sha2 = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
        System.out.println("  segunda vez igual? " + MessageDigest.isEqual(sha, sha2));

        // 2. BCrypt: sal aleatório embutido, custo configurável.
        PasswordEncoder bcrypt = new BCryptPasswordEncoder();                 // custo 10 (padrão)
        String h1 = bcrypt.encode(password);
        String h2 = bcrypt.encode(password);
        System.out.println("BCrypt 1: " + h1 + " (tamanho " + h1.length() + ")");
        System.out.println("BCrypt 2: " + h2);
        System.out.println("  hashes iguais? " + h1.equals(h2) + " | ambos conferem? "
                + bcrypt.matches(password, h1) + "/" + bcrypt.matches(password, h2)
                + " | senha errada? " + bcrypt.matches("payflow#dev2026", h1));

        // 3. O custo é tempo: cada +1 dobra o trabalho.
        for (int cost : new int[] {4, 10, 12, 14}) {
            var enc = new BCryptPasswordEncoder(cost);
            long t0 = System.nanoTime();
            enc.encode(password);
            System.out.printf("  custo %2d: %6.1f ms por hash%n", cost, (System.nanoTime() - t0) / 1e6);
        }

        // 4. O codificador delegante: o prefixo {id} diz quem gerou o hash (permite trocar de algoritmo depois).
        PasswordEncoder delegating = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String d = delegating.encode(password);
        System.out.println("Delegating: " + d.substring(0, 30) + "... (tamanho " + d.length() + ")");
        System.out.println("  confere? " + delegating.matches(password, d));
        try {
            delegating.matches(password, h1);                                   // hash SEM prefixo
        } catch (IllegalArgumentException e) {
            System.out.println("  hash sem prefixo -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // 5. BCrypt só lê os primeiros 72 bytes.
        String long73 = "a".repeat(72) + "X";
        String long72 = "a".repeat(72);
        try {
            String hl = bcrypt.encode(long73);
            System.out.println("73 bytes aceitos; 72 'a' confere com o hash de 73? " + bcrypt.matches(long72, hl));
        } catch (IllegalArgumentException e) {
            System.out.println("73 bytes -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
