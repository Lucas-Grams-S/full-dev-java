package com.payflow.lab.obs;

import java.time.Duration;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Cliente de um serviço "de risco" (aqui, no próprio processo, para o traço atravessar uma chamada HTTP de verdade). */
@Component
class RiskClient {
    private final RestClient.Builder builder;
    private final ApplicationContext ctx;
    private volatile RestClient client;

    RiskClient(RestClient.Builder builder, ApplicationContext ctx) {
        this.builder = builder;   // o builder INJETADO é o que recebe a instrumentação (traceparent, métrica http.client.requests)
        this.ctx = ctx;
    }

    /** ERRADO de propósito: RestClient.create() NÃO usa o builder instrumentado (sem traceparent, sem métrica). */
    int scorePlain(String from) {
        int port = ((WebServerApplicationContext) ctx).getWebServer().getPort();
        Integer s = RestClient.create("http://localhost:" + port).get().uri("/internal/v1/risk?account={a}", from).retrieve().body(Integer.class);
        return s == null ? 0 : s;
    }

    int score(String from) {
        if (client == null) {
            int port = ((WebServerApplicationContext) ctx).getWebServer().getPort();
            var factory = new JdkClientHttpRequestFactory();
            factory.setReadTimeout(Duration.ofSeconds(2));   // pior caso: 2 s por transferência   // decisão 132: sempre com prazo
            client = builder.clone().baseUrl("http://localhost:" + port).requestFactory(factory).build();
        }
        Integer s = client.get().uri("/internal/v1/risk?account={a}", from).retrieve().body(Integer.class);
        return s == null ? 0 : s;
    }
}
