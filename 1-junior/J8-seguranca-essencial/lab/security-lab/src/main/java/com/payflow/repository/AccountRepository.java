package com.payflow.repository;

import com.payflow.domain.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Optional<Account> findById(UUID id);

    List<Account> findByOwnerId(UUID ownerId);
}
