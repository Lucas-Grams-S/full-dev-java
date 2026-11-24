package com.payflow.fixtures.bad.domain;

import com.payflow.fixtures.bad.adapter.BadAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Tudo errado de propósito: domínio com @Service, com Spring e dependendo de um adaptador + injeção em campo. */
@Service
public class BadDomain {
    @Autowired
    private BadAdapter adapter;

    public void run() {
        adapter.doIt();
    }
}
