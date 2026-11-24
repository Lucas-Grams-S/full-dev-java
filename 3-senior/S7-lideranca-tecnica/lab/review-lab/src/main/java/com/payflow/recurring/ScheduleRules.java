package com.payflow.recurring;

import java.math.BigDecimal;

/**
 * Validation of a NEW recurring transfer (design doc, S7.02). The limits are PROVISIONAL:
 * product still has to answer the open question "limit per schedule / active schedules per user".
 */
public final class ScheduleRules {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("5000.00");
    public static final int MAX_ACTIVE = 10;

    public enum Result { OK, INVALID_AMOUNT, ABOVE_LIMIT, INVALID_DAY, TOO_MANY }

    private ScheduleRules() {}

    public static Result validate(BigDecimal amount, int dayOfMonth, int activeSchedules) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            return Result.INVALID_AMOUNT;
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            return Result.ABOVE_LIMIT;
        }
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            return Result.INVALID_DAY;
        }
        if (activeSchedules >= MAX_ACTIVE) {
            return Result.TOO_MANY;
        }
        return Result.OK;
    }
}
