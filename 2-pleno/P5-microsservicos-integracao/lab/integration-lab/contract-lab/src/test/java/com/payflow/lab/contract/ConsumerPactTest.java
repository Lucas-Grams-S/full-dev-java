package com.payflow.lab.contract;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Lado CONSUMIDOR: o monólito declara o que USA; o Pact grava isso em src/test/resources/pacts. */
@ExtendWith(PactConsumerTestExt.class)
@au.com.dius.pact.core.model.annotations.PactDirectory("src/test/resources/pacts")
@PactTestFor(providerName = "notifications", pactVersion = PactSpecVersion.V3)
class ConsumerPactTest {

    @Pact(consumer = "payflow-monolith", provider = "notifications")
    RequestResponsePact preferencesOfExistingUser(PactDslWithProvider builder) {
        return builder
                .given("user u1 has preferences")
                .uponReceiving("a request for the preferences of u1")
                    .path("/internal/v1/users/u1/preferences").method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(new PactDslJsonBody()
                            .stringValue("userId", "u1")
                            .minArrayLike("channels", 1, PactDslJsonRootValue.stringMatcher("EMAIL|PUSH|SMS", "EMAIL")))
                .toPact();
    }

    @Pact(consumer = "payflow-monolith", provider = "notifications")
    RequestResponsePact preferencesOfUnknownUser(PactDslWithProvider builder) {
        return builder
                .given("user ghost does not exist")
                .uponReceiving("a request for the preferences of an unknown user")
                    .path("/internal/v1/users/ghost/preferences").method("GET")
                .willRespondWith().status(404)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "preferencesOfExistingUser")
    void clientReadsTheFieldsItUses(MockServer mock) {
        Preferences p = new NotificationClient(mock.getUrl()).preferences("u1");
        assertThat(p.userId()).isEqualTo("u1");
        assertThat(p.channels()).isNotEmpty();
    }

    @Test
    @PactTestFor(pactMethod = "preferencesOfUnknownUser")
    void clientSurvivesNotFound(MockServer mock) {
        try {
            new NotificationClient(mock.getUrl()).preferences("ghost");
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound expected) {
            assertThat(expected.getStatusCode().value()).isEqualTo(404);
        }
    }
}
