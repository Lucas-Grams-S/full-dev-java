package com.payflow.web;

import com.payflow.domain.Account;
import com.payflow.domain.Accounts;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final Accounts accounts;

    public ApiController(Accounts accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/accounts")
    public List<Account> list(@AuthenticationPrincipal Jwt jwt) {
        return accounts.ownedBy(CurrentUser.from(jwt).id());
    }

    /** Autorização por OBJETO (IDOR): escopo e papel não bastam; a conta precisa ser do "sub". 404 nos dois casos. */
    @GetMapping("/accounts/{id}")
    public ResponseEntity<Account> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID me = CurrentUser.from(jwt).id();
        return accounts.find(id).filter(a -> a.ownerId().equals(me))
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    public record TransferRequest(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount) { }

    @PostMapping("/transfers")
    public ResponseEntity<Map<String, Object>> transfer(@AuthenticationPrincipal Jwt jwt, @RequestBody TransferRequest request) {
        UUID me = CurrentUser.from(jwt).id();
        boolean mine = accounts.find(request.sourceAccountId()).filter(a -> a.ownerId().equals(me)).isPresent();
        if (!mine) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "COMPLETED", "by", me.toString()));
    }

    /** Duas barreiras: a regra da cadeia (hasRole("ADMIN")) e o @PreAuthorize (defesa em profundidade, J8.04). */
    @PostMapping("/admin/accounts/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> block(@PathVariable UUID id) {
        Account account = accounts.setStatus(id, "BLOCKED");
        return account == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(account);
    }
}
