package com.payflow.lab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id")
    private Account source;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_account_id")
    private Account target;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status = "COMPLETED";

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Transfer() {
    }

    public Transfer(Account source, Account target, BigDecimal amount, String idempotencyKey, Instant createdAt) {
        this.source = source;
        this.target = target;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public Account getSource() { return source; }
    public Account getTarget() { return target; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
}
