package com.payflow.labs;

import com.payflow.security.PayflowUserDetails;
import com.payflow.security.TokenService;
import com.payflow.domain.Role;
import com.payflow.web.CurrentUser;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** J8.03: um JWT é só Base64 (não é criptografia): qualquer um lê o conteúdo. */
@SpringBootTest
@ActiveProfiles("test")
class TokenAnatomyTest {

    @Autowired TokenService tokens;

    @Test
    void printTheParts() {
        var user = new PayflowUserDetails(new CurrentUser(UUID.fromString("10000000-0000-0000-0000-000000000001"), "ana@payflow.dev", "Ana Silva"), Role.USER, null);
        String token = tokens.issue(user).value();
        String[] parts = token.split("\\.");
        System.out.println("[jwt] partes: " + parts.length + " | tamanho total: " + token.length());
        System.out.println("[jwt] header : " + new String(Base64.getUrlDecoder().decode(parts[0])));
        System.out.println("[jwt] payload: " + new String(Base64.getUrlDecoder().decode(parts[1])));
        System.out.println("[jwt] assinatura (base64url, " + parts[2].length() + " caracteres): " + parts[2].substring(0, 12) + "...");
    }
}
