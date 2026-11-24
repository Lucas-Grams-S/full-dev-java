package com.payflow.repository;

import com.payflow.domain.Account;
import com.payflow.domain.Role;
import com.payflow.domain.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Só do laboratório: no PayFlow, os dados vêm do PostgreSQL (seed da J3.12). Senha de todos: Payflow#Dev2026. */
@Component
public class InMemoryData implements UserRepository, AccountRepository {

    public static final UUID ANA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID BRUNO = UUID.fromString("10000000-0000-0000-0000-000000000002");
    public static final UUID ADMIN = UUID.fromString("10000000-0000-0000-0000-000000000005");
    public static final UUID ANA_ACCOUNT = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID BRUNO_ACCOUNT = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();

    public InMemoryData(PasswordEncoder encoder) {
        String hash = encoder.encode("Payflow#Dev2026");
        users.put("ana@payflow.dev", new User(ANA, "ana@payflow.dev", "Ana Silva", hash, Role.USER));
        users.put("bruno@payflow.dev", new User(BRUNO, "bruno@payflow.dev", "Bruno Costa", hash, Role.USER));
        users.put("admin@payflow.dev", new User(ADMIN, "admin@payflow.dev", "Eva Admin", hash, Role.ADMIN));
        accounts.put(ANA_ACCOUNT, new Account(ANA_ACCOUNT, ANA, new BigDecimal("830.00")));
        accounts.put(BRUNO_ACCOUNT, new Account(BRUNO_ACCOUNT, BRUNO, new BigDecimal("574.50")));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(users.get(email));
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public List<Account> findByOwnerId(UUID ownerId) {
        return accounts.values().stream().filter(a -> a.getOwnerId().equals(ownerId)).toList();
    }
}
