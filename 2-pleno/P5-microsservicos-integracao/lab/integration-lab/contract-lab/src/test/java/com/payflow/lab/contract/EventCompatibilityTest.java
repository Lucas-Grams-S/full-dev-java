package com.payflow.lab.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** P5.04: o que um CONSUMIDOR de evento (JSON) aguenta quando o PRODUTOR evolui o esquema. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class EventCompatibilityTest {

    /** O que o consumidor (serviço de notificações) entende do evento v1. */
    public record TransferCompletedV1(String eventId, String transferId, String sourceAccountId, BigDecimal amount, Status status) { }

    public enum Status { COMPLETED, REVERSED }

    static final String V1 = "{\"eventId\":\"e1\",\"transferId\":\"t1\",\"sourceAccountId\":\"a1\",\"amount\":\"100.00\",\"status\":\"COMPLETED\"}";

    @Autowired JsonMapper bootMapper;                               // o mapper que o Spring Boot configura
    final JsonMapper factoryMapper = JsonMapper.builder().build();   // o mapper "de fábrica" do Jackson 3
    final JsonMapper strictMapper = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build(); // escolha EXPLÍCITA de rigor

    @Test
    void producerAddsAnOptionalField_toleranceIsAChoice() {
        String v1plus = V1.replace("}", ",\"channel\":\"PUSH\"}");   // campo NOVO que o consumidor não conhece
        TransferCompletedV1 ok = bootMapper.readValue(v1plus, TransferCompletedV1.class);
        System.out.println("T6 campo novo opcional: mapper do Boot le normalmente -> " + ok);
        System.out.println("T6 FAIL_ON_UNKNOWN_PROPERTIES: Boot=" + bootMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                + ", Jackson 3 de fabrica=" + factoryMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                + " (no Jackson 2 o padrao era true)");
        assertThatThrownBy(() -> strictMapper.readValue(v1plus, TransferCompletedV1.class)).satisfies(e ->
                System.out.println("T6 mapper ESTRITO (FAIL_ON_UNKNOWN_PROPERTIES ligado) diante do campo novo: " + e.getClass().getSimpleName() + ": " + e.getMessage().lines().findFirst().orElse("")));
    }

    @Test
    void producerRemovesOrRenamesAFieldTheConsumerNeeds_silentNull() {
        String renamed = V1.replace("\"transferId\"", "\"transfer_id\"");
        TransferCompletedV1 e = bootMapper.readValue(renamed, TransferCompletedV1.class);
        System.out.println("T6 campo renomeado (transferId -> transfer_id): le SEM erro, transferId=" + e.transferId() + " <- null silencioso");
        assertThat(e.transferId()).isNull();
    }

    @Test
    void producerAddsAnEnumValue_consumerBreaksUnlessConfigured() {
        String reversed = V1.replace("COMPLETED", "PARTIALLY_REVERSED");
        assertThatThrownBy(() -> bootMapper.readValue(reversed, TransferCompletedV1.class)).satisfies(e ->
                System.out.println("T6 valor novo de enum (PARTIALLY_REVERSED): " + e.getClass().getSimpleName() + ": " + e.getMessage().lines().findFirst().orElse("")));
        JsonMapper lenient = bootMapper.rebuild().enable(tools.jackson.databind.cfg.EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL).build();
        System.out.println("T6 com EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL: status=" + lenient.readValue(reversed, TransferCompletedV1.class).status() + " (o consumidor decide o que fazer com null)");
    }

    @Test
    void moneyAsNumberVsString() {
        String asNumber = V1.replace("\"100.00\"", "100.10");
        TransferCompletedV1 viaNumber = bootMapper.readValue(asNumber, TransferCompletedV1.class);
        System.out.println("T6 amount como numero JSON 100.10 -> BigDecimal " + viaNumber.amount() + " (ok aqui, mas qualquer biblioteca que passe por double estraga; decisao 11: string)");
        String big = V1.replace("\"100.00\"", "12345678901234567.89");
        System.out.println("T6 amount 12345678901234567.89 como numero via JsonNode/double: " + bootMapper.readTree(big).get("amount").asDouble() + " (perdeu precisao)");
    }
}
