package com.payflow.lab.grpc;

import com.payflow.lab.grpc.antifraud.v1.AntifraudServiceGrpc;
import com.payflow.lab.grpc.antifraud.v1.ScoreRequest;
import com.payflow.lab.grpc.antifraud.v1.ScoreResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** Bean que implementa o serviço gerado: o starter o registra sozinho no servidor gRPC. */
@Service
public class AntifraudGrpcService extends AntifraudServiceGrpc.AntifraudServiceImplBase {

    public volatile long delayMs = 0; // só para o laboratório simular um servidor lento

    @Override
    public void score(ScoreRequest req, StreamObserver<ScoreResponse> out) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(req.getAmount());
        } catch (NumberFormatException e) {
            out.onError(Status.INVALID_ARGUMENT.withDescription("amount invalido: " + req.getAmount()).asRuntimeException());
            return;
        }
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        int score = ScoringRules.score(amount);
        out.onNext(ScoreResponse.newBuilder().setScore(score).setDecision(ScoringRules.decision(score)).build());
        out.onCompleted();
    }
}
