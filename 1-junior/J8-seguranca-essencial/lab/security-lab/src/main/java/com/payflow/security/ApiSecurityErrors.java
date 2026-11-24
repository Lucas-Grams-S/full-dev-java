package com.payflow.security;

import com.payflow.web.api.ApiProblems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Os erros que acontecem NO FILTRO (antes de qualquer controller): 401 e 403 da API em Problem Details.
 * O @RestControllerAdvice não alcança aqui, porque o filtro roda antes do DispatcherServlet.
 */
@Component
public class ApiSecurityErrors implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ApiProblems problems;
    private final BearerTokenAuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();

    public ApiSecurityErrors(ApiProblems problems) {
        this.problems = problems;
    }

    /** Sem token, token expirado, adulterado ou de outro emissor: 401 + o cabeçalho WWW-Authenticate: Bearer (RFC 6750). */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) {
        bearer.commence(request, response, e);                      // grava o WWW-Authenticate (com error="invalid_token" quando há token)
        problems.write(response, HttpStatus.UNAUTHORIZED, "not-authenticated", "Not authenticated", "Log in to use this API.");
    }

    /** Autenticado, mas sem o papel exigido: 403. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) {
        problems.write(response, HttpStatus.FORBIDDEN, "forbidden", "Forbidden", "You do not have permission to do this.");
    }
}
