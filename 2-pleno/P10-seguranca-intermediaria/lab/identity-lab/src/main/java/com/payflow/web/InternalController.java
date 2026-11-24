package com.payflow.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint entre serviços (nunca roteado pelo gateway, decisão 127): quem chama é um CLIENTE, não uma pessoa. */
@RestController
@RequestMapping("/internal")
public class InternalController {

    @PostMapping("/notifications/transfers")
    public ResponseEntity<Map<String, Object>> notifyTransfer(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, Object> event) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("caller", jwt.getClaimAsString("azp"), "jti", jwt.getId(), "received", event));
    }
}
