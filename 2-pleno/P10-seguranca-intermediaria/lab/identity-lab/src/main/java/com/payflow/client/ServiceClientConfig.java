package com.payflow.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;

/**
 * O PayFlow como CLIENTE OAuth2 de outros serviços (client_credentials): o token de máquina é pedido ao Keycloak,
 * guardado até perto de expirar e renovado sozinho. Quem chama é o serviço, não o usuário que está na requisição.
 */
@Configuration
public class ServiceClientConfig {

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    OAuth2AuthorizedClientManager serviceClientManager(ClientRegistrationRepository registrations, OAuth2AuthorizedClientService service) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, service);
        manager.setAuthorizedClientProvider(new ClientCredentialsOAuth2AuthorizedClientProvider());
        return manager;
    }

    @Bean
    OAuth2ClientHttpRequestInterceptor serviceTokenInterceptor(OAuth2AuthorizedClientManager manager) {
        var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
        // o "principal" do token de máquina é o próprio serviço, nunca o usuário logado na requisição de entrada
        interceptor.setPrincipalResolver(request -> new AnonymousAuthenticationToken(
                "payflow-service", "payflow-service", AuthorityUtils.createAuthorityList("ROLE_SERVICE")));
        return interceptor;
    }
}
