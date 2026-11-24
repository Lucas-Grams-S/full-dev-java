package com.payflow.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;

class PasswordPolicyTest {

    final AtomicInteger lookups = new AtomicInteger();
    final PasswordPolicy clean = new PasswordPolicy(p -> {
        lookups.incrementAndGet();
        return new CompromisedPasswordDecision(false);
    });

    @Test
    void aShortPasswordIsRefused() {
        assertThat(clean.check("curta123", "ana@payflow.dev")).containsExactly("too-short");
    }

    @Test
    void aPassphraseWithoutSymbolsOrDigitsIsAccepted() {
        assertThat(clean.check("correct horse battery staple", "ana@payflow.dev")).isEmpty();      // sem regras de composição
    }

    @Test
    void sixtyFourAsciiCharactersAreAccepted() {
        assertThat(clean.check("a".repeat(30) + "b".repeat(30) + "cdef", "ana@payflow.dev")).isEmpty();
    }

    @Test
    void moreThanSeventyTwoBytesIsRefusedEvenIfFewerCharacters() {
        String thirtySevenAccents = "é".repeat(37);                       // 37 caracteres, 74 bytes
        assertThat(clean.check(thirtySevenAccents, "ana@payflow.dev")).containsExactly("too-long");
    }

    @Test
    void aCommonPasswordIsRefusedRegardlessOfCase() {
        assertThat(clean.check("Password1234", "ana@payflow.dev")).containsExactly("common");
    }

    @Test
    void aPasswordContainingTheEmailNameIsRefused() {
        assertThat(clean.check("bruno-costa-2026!!", "bruno@payflow.dev")).containsExactly("contains-email");
    }

    @Test
    void aBreachedPasswordIsRefused() {
        PasswordPolicy breached = new PasswordPolicy(p -> new CompromisedPasswordDecision(true));
        assertThat(breached.check("a perfectly long passphrase", "ana@payflow.dev")).containsExactly("compromised");
    }

    @Test
    void theNetworkLookupOnlyRunsWhenEverythingElsePassed() {
        clean.check("curta", "ana@payflow.dev");
        assertThat(lookups).hasValue(0);
        clean.check("correct horse battery staple", "ana@payflow.dev");
        assertThat(lookups).hasValue(1);
    }

    @Test
    void theResultNeverEchoesThePassword() {
        assertThat(clean.check("segredo-super-secreto", "x@y.com").toString()).doesNotContain("segredo");
        assertThat(clean.check("short", null)).containsExactly("too-short");
        assertThat(clean.check(null, null)).containsExactly("too-short");
    }
}
