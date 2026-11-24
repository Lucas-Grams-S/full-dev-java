package com.payflow.lab.grpc;

import com.payflow.lab.grpc.antifraud.v1.Decision;
import java.math.BigDecimal;

/** A regra de negócio, igual para REST e gRPC (o protocolo é só o transporte). */
public final class ScoringRules {

    private ScoringRules() { }

    public static int score(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("10000.00")) >= 0) return 90;
        if (amount.compareTo(new BigDecimal("1000.00")) >= 0) return 40;
        return 5;
    }

    public static Decision decision(int score) {
        return score >= 80 ? Decision.DENY : score >= 30 ? Decision.REVIEW : Decision.ALLOW;
    }
}
