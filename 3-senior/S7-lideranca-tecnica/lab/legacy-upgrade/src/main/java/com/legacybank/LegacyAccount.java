package com.legacybank;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "accounts")
public class LegacyAccount {

    @Id
    private Long id;
    private String owner;

    public Long getId() { return id; }
    public String getOwner() { return owner; }
}
