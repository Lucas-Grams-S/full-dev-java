package com.payflow.lab.resilience;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Cliente HTTP do serviço de notificações (o "outro serviço" que o monólito chama). */
public class NotificationClient {

    private final RestClient rest;

    /** {@code read == null} = sem timeout de leitura (o PADRÃO do cliente JDK). */
    public NotificationClient(String baseUrl, Duration connect, Duration read) {
        HttpClient.Builder http = HttpClient.newBuilder();
        if (connect != null) {
            http.connectTimeout(connect);
        }
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http.build());
        if (read != null) {
            factory.setReadTimeout(read);
        }
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public String preferences(String userId) {
        return rest.get().uri("/internal/v1/users/{id}/preferences", userId).retrieve().body(String.class);
    }

    public String send(String userId, String text) {
        return rest.post().uri("/internal/v1/users/{id}/messages", userId).body(text).retrieve().body(String.class);
    }
}
