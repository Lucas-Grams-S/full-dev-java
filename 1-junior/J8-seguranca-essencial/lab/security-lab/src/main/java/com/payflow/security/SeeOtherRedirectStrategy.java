package com.payflow.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.DefaultRedirectStrategy;

/** O padrão do Spring Security é 302; o PayFlow usa 303 depois de um POST (decisão 25): "agora faça um GET lá". */
public class SeeOtherRedirectStrategy extends DefaultRedirectStrategy {

    @Override
    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        String target = response.encodeRedirectURL(calculateRedirectUrl(request.getContextPath(), url));
        response.setStatus(HttpStatus.SEE_OTHER.value());
        response.setHeader(HttpHeaders.LOCATION, target);
    }
}
