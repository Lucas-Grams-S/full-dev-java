package com.payflow.lab.contract;

import org.springframework.web.client.RestClient;

/** Cliente do monólito para o serviço de notificações (lado CONSUMIDOR do contrato). */
public class NotificationClient {

    private final RestClient rest;

    public NotificationClient(String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    public Preferences preferences(String userId) {
        return rest.get().uri("/internal/v1/users/{id}/preferences", userId).retrieve().body(Preferences.class);
    }
}
