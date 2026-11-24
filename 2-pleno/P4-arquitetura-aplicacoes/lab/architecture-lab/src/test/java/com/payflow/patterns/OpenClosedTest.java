package com.payflow.patterns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OCP + DIP + Strategy com o contêiner do Spring: acrescentar uma regra = acrescentar um bean. */
class OpenClosedTest {

    /** A abstração: cada política decide se recusa e devolve o motivo. */
    interface TransferPolicy {
        Optional<String> reject(BigDecimal amount);
    }

    /** NUNCA muda quando surge uma regra nova: está FECHADO para modificação. */
    static class TransferChecker {
        private final List<TransferPolicy> policies;

        TransferChecker(List<TransferPolicy> policies) {
            this.policies = policies;
        }

        void check(BigDecimal amount) {
            policies.stream().map(p -> p.reject(amount)).flatMap(Optional::stream).findFirst()
                    .ifPresent(reason -> { throw new IllegalArgumentException(reason); });
        }
    }

    static class MinimumPolicy implements TransferPolicy {
        public Optional<String> reject(BigDecimal amount) {
            return amount.compareTo(new BigDecimal("0.01")) < 0 ? Optional.of("below minimum") : Optional.empty();
        }
    }

    static class SingleLimitPolicy implements TransferPolicy {
        public Optional<String> reject(BigDecimal amount) {
            return amount.compareTo(new BigDecimal("5000.00")) > 0 ? Optional.of("above single limit") : Optional.empty();
        }
    }

    /** A regra nova: só este bean é acrescentado, nenhuma linha do TransferChecker muda. */
    static class NightLimitPolicy implements TransferPolicy {
        public Optional<String> reject(BigDecimal amount) {
            return amount.compareTo(new BigDecimal("1000.00")) > 0 ? Optional.of("above night limit") : Optional.empty();
        }
    }

    @Configuration
    static class BaseConfig {
        @Bean TransferPolicy minimum() { return new MinimumPolicy(); }
        @Bean TransferPolicy singleLimit() { return new SingleLimitPolicy(); }
        @Bean TransferChecker checker(List<TransferPolicy> policies) { return new TransferChecker(policies); }
    }

    @Configuration
    static class WithNightLimitConfig extends BaseConfig {
        @Bean TransferPolicy nightLimit() { return new NightLimitPolicy(); }
    }

    @Test
    void addingAPolicyDoesNotTouchTheChecker() {
        try (var base = new AnnotationConfigApplicationContext(BaseConfig.class)) {
            base.getBean(TransferChecker.class).check(new BigDecimal("2000.00"));        // passa
        }
        try (var extended = new AnnotationConfigApplicationContext(WithNightLimitConfig.class)) {
            assertThatThrownBy(() -> extended.getBean(TransferChecker.class).check(new BigDecimal("2000.00")))
                    .hasMessage("above night limit");                                      // mesma classe, regra nova
            assertThat(extended.getBeansOfType(TransferPolicy.class)).hasSize(3);
        }
    }
}
