package com.payflow.web.api.dto;

import com.payflow.domain.Account;
import java.util.UUID;

public record AccountResponse(UUID id, String balance, String status) {
    public static AccountResponse of(Account account) {
        return new AccountResponse(account.getId(), account.getBalance().toPlainString(), account.getStatus().name());
    }
}
