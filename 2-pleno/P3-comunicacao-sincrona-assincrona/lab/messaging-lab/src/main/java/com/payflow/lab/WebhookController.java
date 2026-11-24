package com.payflow.lab;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** P3.10: recebe o webhook do "PSP" simulado. Verifica assinatura, deduplica pelo eventId e só então age. */
@RestController
public class WebhookController {
    private final JdbcClient jdbc;
    private final String secret;
    private final Clock clock;
    private final JsonMapper json = JsonMapper.builder().build();

    public WebhookController(JdbcClient jdbc, @Value("${lab.webhook.secret:segredo-de-teste}") String secret, Clock clock) {
        this.jdbc = jdbc; this.secret = secret; this.clock = clock;
    }

    @org.springframework.context.annotation.Configuration
    static class ClockConfig { @Bean Clock clock() { return Clock.systemUTC(); } }

    @PostMapping("/webhooks/psp")
    @Transactional
    public ResponseEntity<Map<String, String>> receive(@RequestBody byte[] body,
                                                       @RequestHeader(value = "X-Payflow-Signature", required = false) String sig) {
        if (!WebhookSignature.verify(secret, sig, body, clock, 300)) return ResponseEntity.status(401).body(Map.of("error", "invalid signature"));
        JsonNode n = json.readTree(body);
        String eventId = n.get("eventId").asString();
        String requestId = n.get("requestId").asString();
        BigDecimal amount = new BigDecimal(n.get("amount").asString());

        int inserted = jdbc.sql("insert into processed_messages (consumer, message_id) values ('webhook', :id) on conflict do nothing")
                .param("id", eventId).update();
        if (inserted == 0) return ResponseEntity.ok(Map.of("status", "duplicate"));      // 2xx: o PSP para de reenviar

        var pr = jdbc.sql("select payee_wallet, amount, status, expires_at < now() as expired from payment_requests where id = :id for update")
                .param("id", requestId).query().singleRow();
        if (pr == null) throw new IllegalStateException();
        if (!"PENDING".equals(pr.get("status"))) return ResponseEntity.ok(Map.of("status", "already-" + pr.get("status")));
        if (((BigDecimal) pr.get("amount")).compareTo(amount) != 0) {
            throw new AmountMismatch();                                                       // rollback: a marca de evento também some
        }
        if ((Boolean) pr.get("expired")) {
            jdbc.sql("update payment_requests set status='EXPIRED' where id=:id").param("id", requestId).update();
            return ResponseEntity.status(409).body(Map.of("status", "expired"));
        }
        jdbc.sql("update payment_requests set status='PAID', paid_at=now() where id=:id").param("id", requestId).update();
        jdbc.sql("update wallets set balance = balance + :a where id=:w").param("a", amount).param("w", pr.get("payee_wallet")).update();
        return ResponseEntity.ok(Map.of("status", "paid"));
    }

    @ResponseStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
    static class AmountMismatch extends RuntimeException {}
}
