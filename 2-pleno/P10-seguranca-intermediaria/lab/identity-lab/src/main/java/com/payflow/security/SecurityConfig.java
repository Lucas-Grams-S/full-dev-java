package com.payflow.security;

import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

/** O PayFlow como Resource Server: sem sessão, sem CSRF (não há cookie), um token por requisição. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    OAuth2TokenValidator<Jwt> tokenTypeValidator() {
        return new TokenTypeValidator();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakAuthoritiesConverter());
        return converter;
    }

    /** Chamadas entre serviços: só token de CLIENTE (máquina), com o escopo exigido e vindo de um cliente autorizado. */
    @Bean
    @Order(1)
    SecurityFilterChain internalChain(HttpSecurity http, JwtAuthenticationConverter converter, SecurityProperties props) throws Exception {
        String scope = "SCOPE_" + props.internal().requiredScope();
        Set<String> allowed = props.internal().allowedClients();
        http.securityMatcher("/internal/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().access((authentication, context) ->
                        new AuthorizationDecision(isAllowedService(authentication.get(), scope, allowed))))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()));
        return http.build();
    }

    static boolean isAllowedService(Authentication authentication, String requiredAuthority, Set<String> allowedClients) {
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        boolean hasScope = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(requiredAuthority));
        boolean knownClient = allowedClients.contains(jwt.getClaimAsString("azp"));
        return hasScope && knownClient;
    }

    /** A API pública: escopo (o que o app pode) + papel (o que o usuário pode) + dono do objeto (no caso de uso). */
    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        http.securityMatcher("/api/v1/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/accounts/**").hasAuthority("SCOPE_transfers:read")
                        .requestMatchers(HttpMethod.POST, "/api/v1/transfers").hasAuthority("SCOPE_transfers:write")
                        .anyRequest().denyAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()));
        return http.build();
    }

    /** Qualquer outra rota: negar. (O Boot, sem esta cadeia, abriria o formulário de login padrão.) */
    @Bean
    @Order(3)
    SecurityFilterChain denyRest(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
