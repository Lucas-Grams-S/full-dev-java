package com.payflow.patterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Os 7 padrões da P4.04/P4.05 em Java puro, com o vocabulário do PayFlow. */
class PatternsTest {

    // ---- Strategy: regra que varia, escolhida em tempo de execução
    interface FeePolicy { BigDecimal feeFor(BigDecimal amount); }
    static final FeePolicy FREE = amount -> BigDecimal.ZERO;
    static final FeePolicy ONE_PERCENT = amount -> amount.multiply(new BigDecimal("0.01")).setScale(2, java.math.RoundingMode.HALF_EVEN);

    // ---- Factory: nome com intenção + invariantes num lugar só
    record Notification(String channel, String text) {
        static Notification email(String text) { return new Notification("EMAIL", text); }
        static Notification push(String text) {
            if (text.length() > 40) throw new IllegalArgumentException("push too long");
            return new Notification("PUSH", text);
        }
    }

    // ---- Builder: muitos campos opcionais, objeto final imutável
    record PaymentRequest(String payee, BigDecimal amount, String description, int expiresInMinutes) {
        static Builder builder() { return new Builder(); }
        static final class Builder {
            private String payee; private BigDecimal amount; private String description = ""; private int expires = 30;
            Builder payee(String v) { payee = v; return this; }
            Builder amount(String v) { amount = new BigDecimal(v); return this; }
            Builder description(String v) { description = v; return this; }
            Builder expiresInMinutes(int v) { expires = v; return this; }
            PaymentRequest build() {
                if (payee == null || amount == null) throw new IllegalStateException("payee and amount are required");
                return new PaymentRequest(payee, amount, description, expires);
            }
        }
    }

    // ---- Adapter: traduz a API de fora para a porta de dentro
    interface PaymentGateway { String charge(String payee, BigDecimal amount); }      // porta (linguagem do PayFlow)
    static class VendorSdk { String createCharge(Map<String, Object> body) { return "ch_" + body.get("to"); } }  // API do PSP
    static class VendorAdapter implements PaymentGateway {
        private final VendorSdk sdk;
        VendorAdapter(VendorSdk sdk) { this.sdk = sdk; }
        public String charge(String payee, BigDecimal amount) {
            return sdk.createCharge(Map.of("to", payee, "cents", amount.movePointRight(2).intValueExact()));
        }
    }

    // ---- Decorator: acrescenta comportamento sem tocar na classe (e sem herança)
    static class TimedGateway implements PaymentGateway {
        private final PaymentGateway delegate; final List<String> log = new ArrayList<>();
        TimedGateway(PaymentGateway delegate) { this.delegate = delegate; }
        public String charge(String payee, BigDecimal amount) {
            log.add("before " + payee);
            try { return delegate.charge(payee, amount); } finally { log.add("after " + payee); }
        }
    }

    // ---- Observer: quem publica não conhece quem escuta
    static class EventBus {
        private final List<Consumer<String>> listeners = new ArrayList<>();
        void subscribe(Consumer<String> l) { listeners.add(l); }
        void publish(String event) { listeners.forEach(l -> l.accept(event)); }
    }

    // ---- Template Method: o esqueleto fixo, os passos variáveis
    abstract static class ImportJob {
        final List<String> steps = new ArrayList<>();
        final void run(String line) {           // o esqueleto NÃO muda
            steps.add("parse:" + parse(line));
            validate(line);
            steps.add("save");
        }
        abstract String parse(String line);      // o que varia
        void validate(String line) { if (line.isBlank()) throw new IllegalArgumentException("blank"); }
    }

    @Test
    void strategy() {
        assertThat(FREE.feeFor(new BigDecimal("100.00"))).isEqualByComparingTo("0");
        assertThat(ONE_PERCENT.feeFor(new BigDecimal("100.00"))).isEqualByComparingTo("1.00");
    }

    @Test
    void factory() {
        assertThat(Notification.email("hi").channel()).isEqualTo("EMAIL");
        assertThatThrownBy(() -> Notification.push("x".repeat(41))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builder() {
        var request = PaymentRequest.builder().payee("loja").amount("10.50").build();
        assertThat(request.expiresInMinutes()).isEqualTo(30);
        assertThatThrownBy(() -> PaymentRequest.builder().payee("loja").build()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void adapterAndDecorator() {
        PaymentGateway gateway = new TimedGateway(new VendorAdapter(new VendorSdk()));
        assertThat(gateway.charge("loja", new BigDecimal("10.50"))).isEqualTo("ch_loja");
        assertThat(((TimedGateway) gateway).log).containsExactly("before loja", "after loja");
    }

    @Test
    void observer() {
        var bus = new EventBus();
        var heard = new ArrayList<String>();
        bus.subscribe(heard::add);
        bus.subscribe(e -> heard.add("audit:" + e));
        bus.publish("TransferCompleted");
        assertThat(heard).containsExactly("TransferCompleted", "audit:TransferCompleted");
    }

    @Test
    void templateMethod() {
        var job = new ImportJob() { String parse(String line) { return line.trim().toUpperCase(); } };
        job.run(" ana ");
        assertThat(job.steps).containsExactly("parse:ANA", "save");
        assertThatThrownBy(() -> job.run(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
