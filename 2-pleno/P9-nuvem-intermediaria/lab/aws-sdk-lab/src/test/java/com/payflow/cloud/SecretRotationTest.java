package com.payflow.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

class SecretRotationTest {

    static SecretsManagerClient sm;

    @BeforeAll
    static void setUp() {
        MotoSupport.requireLocalEmulator();
        sm = AwsClients.secrets();
    }

    /** A clock the test moves by hand (same idea as the Clock of J1/J5: never Thread.sleep in a TTL test). */
    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-14T12:00:00Z");

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    void aCachedSecretKeepsTheOldValueUntilTheTtlEndsAndPreviousStaysReadable() {
        String id = MotoSupport.unique("payflow/study/jwt-key");
        sm.createSecret(b -> b.name(id).secretString("key-v1"));
        MutableClock clock = new MutableClock();
        CachedSecret cached = new CachedSecret(
                () -> sm.getSecretValue(GetSecretValueRequest.builder().secretId(id).build()).secretString(),
                Duration.ofMinutes(5), clock);

        String t0 = cached.get();
        sm.putSecretValue(b -> b.secretId(id).secretString("key-v2")); // ROTATION: a new version becomes AWSCURRENT
        String t1 = cached.get();                                       // still inside the TTL
        clock.now = clock.now.plus(Duration.ofMinutes(6));
        String t2 = cached.get();                                       // TTL over: reloads
        String previous = sm.getSecretValue(b -> b.secretId(id).versionStage("AWSPREVIOUS")).secretString();

        System.out.println("T9.6 before rotation          : " + t0 + " (API calls so far: 1)");
        System.out.println("T9.6 after rotation, in TTL   : " + t1 + "  <- STALE, no API call");
        System.out.println("T9.6 after rotation, TTL over : " + t2 + " (API calls so far: " + cached.loads() + ")");
        System.out.println("T9.6 AWSPREVIOUS still readable: " + previous);

        cached.invalidate();
        sm.putSecretValue(b -> b.secretId(id).secretString("key-v3"));
        System.out.println("T9.6 after invalidate()       : " + cached.get() + " (reloaded at once)");

        assertThat(t0).isEqualTo("key-v1");
        assertThat(t1).isEqualTo("key-v1");
        assertThat(t2).isEqualTo("key-v2");
        assertThat(previous).isEqualTo("key-v1");
        assertThat(cached.loads()).isEqualTo(3);
    }
}
