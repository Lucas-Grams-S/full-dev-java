package com.payflow.client;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Chama o endpoint interno com o token do cliente OAuth2 escolhido (notifications ou reporting). */
@Component
public class InternalApiClient {

    private final RestClient client;

    public InternalApiClient(RestClient.Builder builder, OAuth2ClientHttpRequestInterceptor interceptor) {
        this.client = builder.requestInterceptor(interceptor).build();
    }

    public ResponseEntity<Map<String, Object>> notifyTransfer(String baseUrl, String registrationId, Map<String, Object> event) {
        return client.post().uri(baseUrl + "/internal/notifications/transfers")
                .attributes(clientRegistrationId(registrationId))
                .body(event)
                .retrieve()
                .onStatus(status -> true, (request, response) -> { })      // quem chama decide o que fazer com 403/401
                .toEntity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() { });
    }
}
