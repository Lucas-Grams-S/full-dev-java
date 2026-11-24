package com.payflow.web;

import com.payflow.security.PayflowUserDetails;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Entrega o CurrentUser a qualquer método de controller que o declare, venha o login do formulário ou do JWT. */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PayflowUserDetails details) {
            return details.currentUser();                                    // login pelo formulário (sessão)
        }
        if (authentication instanceof JwtAuthenticationToken token) {
            return fromJwt(token.getToken());                                // login pela API (Bearer)
        }
        throw new IllegalStateException("No authenticated user: is this path protected by the security chain?");
    }

    static CurrentUser fromJwt(Jwt jwt) {
        return new CurrentUser(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("email"), jwt.getClaimAsString("name"));
    }
}
