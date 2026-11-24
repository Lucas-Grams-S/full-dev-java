package com.payflow.lab.obs;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ApiController {
    private final TransferService transfers;
    private final LabState state;
    private final RiskClient risk;

    ApiController(TransferService transfers, LabState state, RiskClient risk) {
        this.risk = risk;
        this.transfers = transfers;
        this.state = state;
    }

    @PostMapping("/api/v1/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, String> create(@RequestBody TransferRequest req) {
        return Map.of("id", transfers.transfer(req), "status", "COMPLETED");
    }

    /** Chama o serviço de risco com um RestClient NÃO instrumentado (demonstração do erro). */
    @GetMapping("/lab/plain-risk")
    Map<String, Integer> plainRisk(@RequestParam String account) {
        return Map.of("score", risk.scorePlain(account));
    }

    @GetMapping("/api/v1/accounts/{id}")
    Map<String, String> account(@PathVariable String id) {
        return Map.of("id", id, "balance", "100.00");
    }

    /** O "serviço de risco" (interno; no PayFlow real NUNCA roteado pelo gateway, decisão 130). */
    @GetMapping("/internal/v1/risk")
    int risk(@RequestParam String account) {
        if (!state.riskUp.get()) {
            throw new IllegalStateException("risk service down");
        }
        if (account.equals(state.riskSlowAccount) && state.riskSlowMs > 0) {
            try {
                Thread.sleep(state.riskSlowMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return Math.floorMod(account.hashCode(), 100);
    }

    /** Incidente simulado: latência só para uma conta (o resto do tráfego continua saudável). */
    @PostMapping("/lab/risk-latency")
    Map<String, Object> riskLatency(@RequestParam String account, @RequestParam int ms) {
        state.riskSlowAccount = account;
        state.riskSlowMs = ms;
        return Map.of("account", account, "ms", ms);
    }

    /** Interruptores do laboratório. */
    @PostMapping("/lab/switch")
    Map<String, Boolean> sw(@RequestParam String what, @RequestParam boolean on) {
        switch (what) {
            case "db" -> state.dbUp.set(on);
            case "risk" -> state.riskUp.set(on);
            case "outbox-stall" -> state.outboxStalled.set(on);
            default -> throw new IllegalArgumentException("unknown switch");
        }
        return Map.of(what, on);
    }
}
