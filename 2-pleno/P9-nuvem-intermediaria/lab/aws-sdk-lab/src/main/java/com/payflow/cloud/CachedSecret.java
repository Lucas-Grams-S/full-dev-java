package com.payflow.cloud;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * A secret read once and cached for a TTL. Why: every GetSecretValue is a billed API call (Secrets Manager charges
 * per 10,000 calls) AND a network round trip on the request path. The price: after a rotation the old value lives
 * until the TTL ends, so a rotation needs the old and the new credential both valid for a while (AWSPREVIOUS).
 */
public final class CachedSecret {

    private final Supplier<String> loader;
    private final Duration ttl;
    private final Clock clock;
    private String value;
    private Instant loadedAt;
    private int loads;

    public CachedSecret(Supplier<String> loader, Duration ttl, Clock clock) {
        this.loader = loader;
        this.ttl = ttl;
        this.clock = clock;
    }

    public synchronized String get() {
        Instant now = clock.instant();
        if (value == null || now.isAfter(loadedAt.plus(ttl))) {
            value = loader.get();
            loadedAt = now;
            loads++;
        }
        return value;
    }

    /** Call when the secret was REJECTED (e.g. authentication failed): the next get() reloads. */
    public synchronized void invalidate() {
        value = null;
    }

    public synchronized int loads() {
        return loads;
    }
}
