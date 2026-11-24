package com.payflow.cloud;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * SQS (standard) delivers AT LEAST ONCE (docs): the same message can arrive twice. The handler remembers the
 * message id and does the work once. In PayFlow this is the processed_messages table of P3.07 (UNIQUE key);
 * the lab uses a set so the test needs no database.
 */
public final class IdempotentHandler {

    private final Set<String> processed = ConcurrentHashMap.newKeySet();
    private final Consumer<String> action;

    public IdempotentHandler(Consumer<String> action) {
        this.action = action;
    }

    /** @return true if the action ran now, false if it was a duplicate */
    public boolean handle(String messageKey, String body) {
        if (!processed.add(messageKey)) {
            return false;
        }
        try {
            action.accept(body);
            return true;
        } catch (RuntimeException e) {
            processed.remove(messageKey); // failed: allow the redelivery to try again
            throw e;
        }
    }
}
