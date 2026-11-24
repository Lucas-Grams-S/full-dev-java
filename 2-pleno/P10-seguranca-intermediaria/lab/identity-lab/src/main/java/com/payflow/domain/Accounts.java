package com.payflow.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Dados em memória (a persistência não é o assunto da P10). Os donos usam o "sub" que o Keycloak emite. */
@Component
public class Accounts {

    public static final UUID ALICE = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID BOB = UUID.fromString("22222222-2222-4222-8222-222222222222");
    public static final UUID ALICE_ACCOUNT = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");
    public static final UUID BOB_ACCOUNT = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000002");

    private final Map<UUID, Account> byId = new ConcurrentHashMap<>();

    public Accounts() {
        reset();
    }

    /** Só para os testes: volta ao estado inicial (um teste que bloqueia uma conta não pode contaminar o seguinte). */
    public void reset() {
        byId.clear();
        byId.put(ALICE_ACCOUNT, new Account(ALICE_ACCOUNT, ALICE, new BigDecimal("100.00"), "ACTIVE"));
        byId.put(BOB_ACCOUNT, new Account(BOB_ACCOUNT, BOB, new BigDecimal("50.00"), "ACTIVE"));
    }

    public List<Account> ownedBy(UUID owner) {
        return byId.values().stream().filter(a -> a.ownerId().equals(owner)).toList();
    }

    public Optional<Account> find(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Account setStatus(UUID id, String status) {
        return byId.computeIfPresent(id, (k, a) -> a.withStatus(status));
    }
}
