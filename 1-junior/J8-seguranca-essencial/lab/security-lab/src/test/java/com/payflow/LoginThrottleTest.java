package com.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.security.LoginThrottle;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {

    /** Relógio que anda quando o teste manda (Clock.fixed não avança). */
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-13T12:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
        void advance(Duration d) { now = now.plus(d); }
    }

    final MutableClock clock = new MutableClock();
    final LoginThrottle throttle = new LoginThrottle(clock, 3, Duration.ofMinutes(15));

    @Test
    void locksAfterTheMaximumNumberOfFailures() {
        throttle.failure("a@x.com");
        throttle.failure("a@x.com");
        assertThat(throttle.isLocked("a@x.com")).isFalse();
        throttle.failure("a@x.com");
        assertThat(throttle.isLocked("a@x.com")).isTrue();
        assertThat(throttle.isLocked("b@x.com")).isFalse();              // outra chave não é afetada
    }

    @Test
    void unlocksWhenTheLockDurationPasses() {
        for (int i = 0; i < 3; i++) {
            throttle.failure("a@x.com");
        }
        clock.advance(Duration.ofMinutes(14));
        assertThat(throttle.isLocked("a@x.com")).isTrue();
        clock.advance(Duration.ofMinutes(2));
        assertThat(throttle.isLocked("a@x.com")).isFalse();
    }

    @Test
    void aSuccessClearsTheCounter() {
        throttle.failure("a@x.com");
        throttle.failure("a@x.com");
        throttle.success("a@x.com");
        throttle.failure("a@x.com");
        assertThat(throttle.isLocked("a@x.com")).isFalse();
    }

    @Test
    void keepsAtMostTenThousandKeys() {
        for (int i = 0; i < 10_050; i++) {
            throttle.failure("k" + i + "@x.com");
        }
        for (int i = 0; i < 3; i++) {
            throttle.failure("k10049@x.com");                              // a mais recente continua rastreada
        }
        assertThat(throttle.isLocked("k10049@x.com")).isTrue();
    }
}
