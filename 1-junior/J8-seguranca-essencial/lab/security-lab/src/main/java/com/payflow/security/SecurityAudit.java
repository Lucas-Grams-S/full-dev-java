package com.payflow.security;

import com.payflow.web.Masking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Trilha de auditoria de login (OWASP A09): quem entrou, quem falhou e por quê, sem senha e sem e-mail inteiro. */
@Component
public class SecurityAudit {

    private static final Logger log = LoggerFactory.getLogger(SecurityAudit.class);
    private static final int MAX_LOGGED = 80;

    @EventListener
    void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof PayflowUserDetails user) {
            log.info("Login succeeded: userId={}", user.currentUser().id());
        }
    }

    @EventListener
    void onFailure(AbstractAuthenticationFailureEvent event) {
        if (!(event.getAuthentication() instanceof UsernamePasswordAuthenticationToken)) {
            return;                                   // token Bearer inválido também gera evento; aqui só e-mail + senha
        }
        log.warn("Login failed: user={} reason={}", maskedName(event.getAuthentication().getName()),
                event.getException().getClass().getSimpleName());
    }

    /**
     * O que vai para o log vem do atacante. Mascara como e-mail e, ainda assim, neutraliza caracteres de controle
     * (uma quebra de linha no "domínio" forjaria uma linha de log falsa, CWE-117) e limita o tamanho.
     */
    static String maskedName(String name) {
        String masked;
        try {
            masked = Masking.email(name);
        } catch (IllegalArgumentException e) {
            return "(not an email)";                  // o que foi digitado pode ser lixo; nunca o repita no log
        }
        masked = masked.replaceAll("\\p{Cntrl}", "?");
        return masked.length() > MAX_LOGGED ? masked.substring(0, MAX_LOGGED) + "..." : masked;
    }
}
