package com.payflow.lab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Version
    private long version;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    // Só para as demonstrações de fetch (P2.07): o PayFlow da J5 NÃO mapeia estas coleções.
    @OneToMany(mappedBy = "account")
    private List<LedgerEntry> entries = new ArrayList<>();

    @OneToMany(mappedBy = "source")
    private List<Transfer> sent = new ArrayList<>();

    protected Account() {
    }

    public Account(User owner, BigDecimal balance) {
        this.owner = owner;
        this.balance = balance.setScale(2);
    }

    public void withdraw(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(id);
        }
        balance = balance.subtract(amount);
    }

    public void deposit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public UUID getId() { return id; }
    public User getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
    public long getVersion() { return version; }
    public List<LedgerEntry> getEntries() { return entries; }
    public List<Transfer> getSent() { return sent; }
}
