package com.payflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** J8.06: o que vai para o log vem do atacante. Quebra de linha no "e-mail" forjaria uma linha de log falsa (CWE-117). */
class SecurityAuditTest {

    @Test
    void anOrdinaryEmailIsMasked() {
        assertThat(SecurityAudit.maskedName("ana@payflow.dev")).isEqualTo("a**@payflow.dev");
    }

    @Test
    void garbageIsNeverEchoed() {
        assertThat(SecurityAudit.maskedName("<script>alert(1)</script>")).isEqualTo("(not an email)");
    }

    @Test
    void lineBreaksInTheDomainCannotForgeALogLine() {
        String attack = "ana@x.com\n2026-11-13 INFO  SecurityAudit : Login succeeded: userId=10000000-0000-0000-0000-000000000005";
        String logged = SecurityAudit.maskedName(attack);
        System.out.println("[log] valor que iria ao log: " + logged.replace("\n", "<LF>").replace("\r", "<CR>"));
        assertThat(logged).doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    void controlCharactersInTheLocalPartAreAlsoNeutralized() {
        assertThat(SecurityAudit.maskedName("a\rb@x.com")).doesNotContain("\r");
    }

    @Test
    void aHugeValueIsTruncated() {
        assertThat(SecurityAudit.maskedName("a@" + "x".repeat(5_000) + ".com").length()).isLessThanOrEqualTo(83);
    }
}
