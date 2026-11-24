package com.payflow.lab.account.adapter.in.web;

import com.payflow.lab.account.AccountApi;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController {

    private final AccountApi accounts;

    AccountController(AccountApi accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/{id}/balance")
    Map<String, String> balance(@PathVariable UUID id) {
        return Map.of("balance", accounts.balanceOf(id).toString());
    }
}
