package com.payflow.lite.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountsController {

    private static final Logger log = LoggerFactory.getLogger(AccountsController.class);

    public record AccountResponse(UUID id, String owner, String balance) { }

    public record CreateAccountRequest(String owner, BigDecimal balance) { }

    private final JdbcClient jdbc;

    public AccountsController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<AccountResponse> list() {
        return jdbc.sql("select id, owner, balance from accounts order by created_at, id")
            .query((rs, n) -> new AccountResponse(rs.getObject("id", UUID.class), rs.getString("owner"),
                rs.getBigDecimal("balance").toPlainString()))
            .list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@RequestBody CreateAccountRequest request) {
        BigDecimal balance = request.balance().setScale(2);
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into accounts (id, owner, balance) values (:id, :owner, :balance)")
            .param("id", id).param("owner", request.owner()).param("balance", balance).update();
        log.info("account created id={}", id);
        return new AccountResponse(id, request.owner(), balance.toPlainString());
    }
}
