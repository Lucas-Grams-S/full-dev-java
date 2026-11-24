package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** P3.10: webhook assinado, idempotente, com janela anti-replay e pagamento por QR code simulado. */
@SpringBootTest
@AutoConfigureMockMvc
class WebhookIT {
    static final String SECRET = "segredo-de-teste";
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void reset() {
        jdbc.sql("delete from processed_messages").update();
        jdbc.sql("delete from payment_requests").update();
        jdbc.sql("delete from wallets").update();
        jdbc.sql("insert into wallets values ('loja', 0.00)").update();
        jdbc.sql("insert into payment_requests (id, payee_wallet, amount, expires_at) values ('pr-1','loja',50.00, now() + interval '10 minutes')").update();
        jdbc.sql("insert into payment_requests (id, payee_wallet, amount, expires_at) values ('pr-old','loja',50.00, now() - interval '1 minute')").update();
    }

    String body(String eventId, String req, String amount) {
        return "{\"eventId\":\"" + eventId + "\",\"requestId\":\"" + req + "\",\"amount\":\"" + amount + "\"}";
    }

    ResultActions send(String body, long ts, String secret) throws Exception {
        byte[] raw = body.getBytes(StandardCharsets.UTF_8);
        return mvc.perform(post("/webhooks/psp").contentType(MediaType.APPLICATION_JSON).content(raw)
                .header("X-Payflow-Signature", WebhookSignature.sign(secret, ts, raw)));
    }

    long now() { return Instant.now().getEpochSecond(); }
    BigDecimal bal() { return jdbc.sql("select balance from wallets where id='loja'").query(BigDecimal.class).single(); }

    @Test
    void validWebhookCreditsOnce() throws Exception {
        send(body("evt-1", "pr-1", "50.00"), now(), SECRET).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("paid"));
        assertThat(bal()).isEqualByComparingTo("50.00");
    }

    @Test
    void redeliveryOfTheSameEventIsAcknowledgedButNotRepeated() throws Exception {
        for (int i = 0; i < 3; i++) send(body("evt-2", "pr-1", "50.00"), now(), SECRET).andExpect(status().isOk());
        System.out.println("REENVIO_x3 saldo=" + bal());
        assertThat(bal()).isEqualByComparingTo("50.00");
    }

    @Test
    void differentEventForAnAlreadyPaidRequestDoesNotCreditAgain() throws Exception {
        send(body("evt-3", "pr-1", "50.00"), now(), SECRET).andExpect(status().isOk());
        send(body("evt-3b", "pr-1", "50.00"), now(), SECRET).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("already-PAID"));
        assertThat(bal()).isEqualByComparingTo("50.00");
    }

    @Test
    void wrongSecretTamperedBodyAndOldTimestampAreRejected() throws Exception {
        send(body("evt-4", "pr-1", "50.00"), now(), "outro-segredo").andExpect(status().isUnauthorized());
        // corpo adulterado depois de assinado
        byte[] signed = body("evt-5", "pr-1", "50.00").getBytes(StandardCharsets.UTF_8);
        String sig = WebhookSignature.sign(SECRET, now(), signed);
        mvc.perform(post("/webhooks/psp").contentType(MediaType.APPLICATION_JSON)
                .content(body("evt-5", "pr-1", "5000.00")).header("X-Payflow-Signature", sig)).andExpect(status().isUnauthorized());
        // assinatura correta, mas de 1 hora atrás (replay)
        send(body("evt-6", "pr-1", "50.00"), now() - 3600, SECRET).andExpect(status().isUnauthorized());
        // sem cabeçalho
        mvc.perform(post("/webhooks/psp").contentType(MediaType.APPLICATION_JSON).content(body("evt-7", "pr-1", "50.00"))).andExpect(status().isUnauthorized());
        assertThat(bal()).isEqualByComparingTo("0.00");
    }

    @Test
    void amountMismatchIsRejectedAndTheEventIsNotMarkedAsProcessed() throws Exception {
        send(body("evt-8", "pr-1", "49.99"), now(), SECRET).andExpect(status().isUnprocessableEntity());
        int marks = jdbc.sql("select count(*) from processed_messages where message_id='evt-8'").query(Integer.class).single();
        assertThat(marks).isZero();
        assertThat(bal()).isEqualByComparingTo("0.00");
    }

    @Test
    void expiredQrCodeIsNotPaid() throws Exception {
        send(body("evt-9", "pr-old", "50.00"), now(), SECRET).andExpect(status().isConflict());
        assertThat(bal()).isEqualByComparingTo("0.00");
    }
}
