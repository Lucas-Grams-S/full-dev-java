package com.payflow.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conta erros de login por chave (o e-mail normalizado) e bloqueia depois de {@code maxFailures} erros seguidos.
 * Em memória e limitado (10 000 chaves): serve para UMA instância; com várias, o estado vai para um Redis (P2).
 */
public class LoginThrottle {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private record Attempts(int failures, Instant lockedUntil) { }

    private final Clock clock;
    private final int maxFailures;
    private final Duration lockDuration;
    private final Map<String, Attempts> entries = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Attempts> eldest) {
            return size() > MAX_TRACKED_KEYS;           // um atacante com milhões de e-mails não esgota a memória
        }
    };

    public LoginThrottle(Clock clock, int maxFailures, Duration lockDuration) {
        this.clock = clock;
        this.maxFailures = maxFailures;
        this.lockDuration = lockDuration;
    }

    public synchronized boolean isLocked(String key) {
        Attempts entry = entries.get(key);
        if (entry == null || entry.lockedUntil() == null) {
            return false;
        }
        if (clock.instant().isBefore(entry.lockedUntil())) {
            return true;
        }
        entries.remove(key);                              // o bloqueio acabou: recomeça do zero
        return false;
    }

    public synchronized void failure(String key) {
        int failures = entries.getOrDefault(key, new Attempts(0, null)).failures() + 1;
        entries.put(key, failures >= maxFailures ? new Attempts(failures, clock.instant().plus(lockDuration)) : new Attempts(failures, null));
    }

    public synchronized void success(String key) {
        entries.remove(key);
    }
}
