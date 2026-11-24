package com.payflow.domain;

import java.util.UUID;

/** Versão reduzida da entidade da J5.07 (sem JPA), com os mesmos acessores. */
public class User {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final Role role;

    public User(UUID id, String email, String fullName, String passwordHash, Role role) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
}
