package com.payflow.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;

/**
 * Política de senha para CADASTRO e TROCA (o login não a usa). Segue o NIST SP 800-63B: o que importa é o TAMANHO e
 * a lista de senhas ruins; NADA de "uma maiúscula, um número e um símbolo". Devolve CÓDIGOS, nunca a senha.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;      // em caracteres (code points)
    public static final int MAX_BYTES = 72;       // o limite do BCrypt; permite frases de 64 caracteres ASCII

    /** Só ilustrativa: em produção, uma lista grande (milhões) ou a verificação de vazamentos abaixo. */
    private static final Set<String> COMMON = Set.of("password1234", "123456789012", "qwertyuiop12", "payflow12345",
            "iloveyou1234", "senha1234567", "mudar1234567");

    private final CompromisedPasswordChecker compromised;

    public PasswordPolicy(CompromisedPasswordChecker compromised) {
        this.compromised = compromised;
    }

    /** @return a lista de códigos de problema (vazia = senha aceita): too-short, too-long, common, contains-email, compromised */
    public List<String> check(String password, String email) {
        List<String> problems = new ArrayList<>();
        if (password == null || password.codePointCount(0, password.length()) < MIN_LENGTH) {
            problems.add("too-short");
        }
        if (password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            problems.add("too-long");
        }
        if (password != null) {
            String lower = password.toLowerCase(Locale.ROOT);
            if (COMMON.contains(lower)) {
                problems.add("common");
            }
            String local = localPart(email);
            if (local.length() >= 3 && lower.contains(local)) {
                problems.add("contains-email");
            }
            if (problems.isEmpty() && compromised.check(password).isCompromised()) {      // a consulta de rede só roda se o resto passou
                problems.add("compromised");
            }
        }
        return problems;
    }

    private static String localPart(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        return email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT);
    }
}
