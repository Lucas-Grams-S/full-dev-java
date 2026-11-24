package com.payflow.lab.grpc;

import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** A MESMA capacidade, exposta em REST/JSON, para comparar tamanho e latência. */
@RestController
public class AntifraudRestController {

    public record ScoreRequestDto(String transferId, String sourceAccountId, String destinationAccountId, String amount, String currency) { }

    public record ScoreResponseDto(int score, String decision) { }

    @PostMapping("/api/v1/antifraud/score")
    public ScoreResponseDto score(@RequestBody ScoreRequestDto req) {
        int score = ScoringRules.score(new BigDecimal(req.amount()));
        return new ScoreResponseDto(score, ScoringRules.decision(score).name());
    }
}
