package com.payflow.lab.resilience;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** O "checkout" (transferência) do PayFlow, com uma chamada SÍNCRONA ao serviço de notificações no caminho crítico. */
@RestController
public class CheckoutController {

    private final NotificationClient naive;
    private final PreferencesGateway protectedCall;

    public CheckoutController(@Qualifier("naiveClient") NotificationClient naive, PreferencesGateway protectedCall) {
        this.naive = naive;
        this.protectedCall = protectedCall;
    }

    private void localWork() {
        try {
            Thread.sleep(5); // o trabalho local do checkout (banco etc.)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PostMapping("/checkout-naive")
    public String checkoutNaive() {
        localWork();
        return "{\"checkout\":\"ok\",\"prefs\":" + naive.preferences("u1") + "}";
    }

    @PostMapping("/checkout-protected")
    public String checkoutProtected() {
        localWork();
        return "{\"checkout\":\"ok\",\"prefs\":" + protectedCall.preferences("u1") + "}";
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
