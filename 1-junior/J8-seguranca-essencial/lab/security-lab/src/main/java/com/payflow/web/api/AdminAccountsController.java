package com.payflow.web.api;

import com.payflow.domain.Account;
import com.payflow.domain.NotFoundException;
import com.payflow.repository.AccountRepository;
import com.payflow.web.api.dto.AccountResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Só ADMIN. Duas barreiras: a regra da cadeia (/api/v1/admin/**) e o @PreAuthorize (defesa em profundidade). */
@RestController
@RequestMapping("/api/v1/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountsController {

    private final AccountRepository accounts;

    public AdminAccountsController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/{id}/block")
    public AccountResponse block(@PathVariable UUID id) {
        Account account = find(id);
        account.block();
        return AccountResponse.of(account);
    }

    @PostMapping("/{id}/unblock")
    public AccountResponse unblock(@PathVariable UUID id) {
        Account account = find(id);
        account.unblock();
        return AccountResponse.of(account);
    }

    private Account find(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("Account not found: " + id));
    }
}
