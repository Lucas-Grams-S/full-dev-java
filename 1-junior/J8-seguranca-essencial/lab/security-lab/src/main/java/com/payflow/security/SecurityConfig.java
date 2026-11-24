package com.payflow.security;

import com.payflow.config.PayflowProperties;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/** A segurança do PayFlow: duas cadeias de filtros (API com JWT, painel com sessão), negar por padrão. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "frame-ancestors 'none'; form-action 'self'; base-uri 'self'";

    // ---------- peças comuns ----------

    @Bean
    static PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();      // grava "{bcrypt}$2a$10$..."
    }

    @Bean
    LoginThrottle loginThrottle(Clock clock, PayflowProperties properties) {
        var login = properties.security().login();
        return new LoginThrottle(clock, login.maxFailures(), login.lockDuration());
    }

    @Bean
    AuthenticationProvider authenticationProvider(UserDetailsService users, PasswordEncoder encoder, LoginThrottle throttle) {
        DaoAuthenticationProvider dao = new DaoAuthenticationProvider(users);
        dao.setPasswordEncoder(encoder);
        return new ThrottledAuthenticationProvider(dao, throttle);
    }

    /** Um gerenciador só, usado pelo formulário e pela API; o publicador faz os eventos chegarem ao SecurityAudit. */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationProvider provider, AuthenticationEventPublisher publisher) {
        ProviderManager manager = new ProviderManager(provider);
        manager.setAuthenticationEventPublisher(publisher);
        return manager;
    }

    /** Papéis do token: a claim "roles" (["USER"]) vira a autoridade ROLE_USER. */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    // ---------- cadeia 1: a API (/api/v1/**), sem estado, com JWT ----------

    @Bean
    @Order(1)
    SecurityFilterChain apiChain(HttpSecurity http, JwtAuthenticationConverter jwtConverter, ApiSecurityErrors errors) throws Exception {
        http.securityMatcher("/api/v1/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)           // sem cookie de sessão, não há o que forjar (J8.05)
                .cors(Customizer.withDefaults())                  // usa o addCorsMappings da J6.08; o preflight passa sem token
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(errors).accessDeniedHandler(errors));
        return http.build();
    }

    /**
     * Quem não está logado: fetch/XHR para /api/** (o extrato do painel) recebe 401; qualquer outra
     * requisição é mandada para /login. Montado à mão porque o padrão do formulário só redireciona
     * quando o cabeçalho Accept pede HTML (um curl sem Accept receberia 401).
     */
    private static AuthenticationEntryPoint webEntryPoint() {
        return DelegatingAuthenticationEntryPoint.builder()
                .defaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                .addEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), request -> request.getRequestURI().startsWith("/api/"))
                .build();
    }

    // ---------- cadeia 2: o painel (tudo o mais), com sessão e CSRF ----------

    @Bean
    @Order(2)
    SecurityFilterChain webChain(HttpSecurity http, AuthenticationManager authenticationManager,
                                 @Value("${server.servlet.session.cookie.name:JSESSIONID}") String sessionCookie) throws Exception {
        SeeOtherRedirectStrategy seeOther = new SeeOtherRedirectStrategy();

        SavedRequestAwareAuthenticationSuccessHandler onSuccess = new SavedRequestAwareAuthenticationSuccessHandler();
        onSuccess.setDefaultTargetUrl("/accounts");
        onSuccess.setRedirectStrategy(seeOther);

        AuthenticationFailureHandler onFailure = (request, response, e) ->
                seeOther.sendRedirect(request, response, e instanceof LockedException ? "/login?locked" : "/login?error");

        SimpleUrlLogoutSuccessHandler onLogout = new SimpleUrlLogoutSuccessHandler();
        onLogout.setDefaultTargetUrl("/login?logout");
        onLogout.setRedirectStrategy(seeOther);

        http.authenticationManager(authenticationManager)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/forbidden", "/error", "/static/**").permitAll()
                        .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login").loginProcessingUrl("/login")
                        .usernameParameter("email").passwordParameter("password")
                        .successHandler(onSuccess).failureHandler(onFailure))
                .logout(logout -> logout
                        .logoutUrl("/logout").logoutSuccessHandler(onLogout)
                        .deleteCookies(sessionCookie))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(webEntryPoint())
                        // 403 (inclusive CSRF inválido) com a página de erro do PayFlow, não a do Boot
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            request.getRequestDispatcher("/forbidden").forward(request, response);
                        }))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }
}
