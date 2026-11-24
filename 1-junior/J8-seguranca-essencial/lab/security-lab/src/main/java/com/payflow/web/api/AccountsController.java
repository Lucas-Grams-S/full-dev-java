package com.payflow.web.api;

import com.payflow.domain.Account;
import com.payflow.domain.NotFoundException;
import com.payflow.repository.AccountRepository;
import com.payflow.web.CurrentUser;
import com.payflow.web.api.dto.AccountResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountsController {

    private final AccountRepository accounts;

    public AccountsController(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<AccountResponse> list(CurrentUser user) {
        return accounts.findByOwnerId(user.id()).stream().map(AccountResponse::of).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(CurrentUser user, @PathVariable UUID id) {
        return AccountResponse.of(owned(user, id));
    }

    /** Autorização por OBJETO (IDOR): a conta existir não basta, ela precisa ser do usuário. 404 nos dois casos. */
    private Account owned(CurrentUser user, UUID id) {
        return accounts.findById(id).filter(a -> a.getOwnerId().equals(user.id()))
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
    }
}
