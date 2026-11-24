package com.payflow.lab.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {

    public static final String CACHE = "balances";

    // Campos PRIVADOS + métodos: o bean é um proxy de classe (CGLIB) por causa do @Cacheable; campo público lido no proxy é null/0.
    private final AtomicInteger dbLoads = new AtomicInteger();
    private volatile long loadDelayMs = 100;

    private final JdbcClient jdbc;

    /** Quantas vezes o BANCO foi consultado (o que o cache deve reduzir). */
    public int dbLoads() {
        return dbLoads.get();
    }

    public void resetLoads() {
        dbLoads.set(0);
    }

    /** Atraso artificial da consulta (simula uma consulta lenta). */
    public void loadDelayMs(long ms) {
        this.loadDelayMs = ms;
    }

    public BalanceService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Cache-aside declarativo; SEM sync: vários misses simultâneos vão todos ao banco (stampede). */
    @Cacheable(cacheNames = CACHE, key = "#accountId")
    public BalanceView balance(UUID accountId) {
        return load(accountId);
    }

    /** Com sync = true: só UMA thread desta JVM carrega; as outras esperam o resultado. */
    @Cacheable(cacheNames = CACHE, key = "#accountId", sync = true)
    public BalanceView balanceSync(UUID accountId) {
        return load(accountId);
    }

    /** sync = true com o escritor COM lock (lockingCacheManager): UMA carga, mesmo entre instâncias. */
    @Cacheable(cacheNames = CACHE, key = "#accountId", sync = true, cacheManager = "lockingCacheManager")
    public BalanceView balanceSyncLocked(UUID accountId) {
        return load(accountId);
    }

    /** Igual a balance(), mas não tenta guardar null (conta inexistente): cada consulta volta ao banco. */
    @Cacheable(cacheNames = CACHE, key = "#accountId", unless = "#result == null")
    public BalanceView balanceUnlessNull(UUID accountId) {
        return load(accountId);
    }

    /** Escrita: o banco muda e o cache é invalidado DEPOIS (ver P2.09 sobre a janela entre os dois). */
    @CacheEvict(cacheNames = CACHE, key = "#accountId")
    public void deposit(UUID accountId, BigDecimal amount) {
        jdbc.sql("UPDATE accounts SET balance = balance + :a, version = version + 1 WHERE id = :id")
                .param("a", amount).param("id", accountId).update();
    }

    private BalanceView load(UUID id) {
        dbLoads.incrementAndGet();
        // LÊ primeiro e demora depois: é a ordem que expõe a corrida "leitor lento × escritor" (P2.09)
        Optional<BigDecimal> balance = jdbc.sql("SELECT balance FROM accounts WHERE id = :id").param("id", id)
                .query(BigDecimal.class).optional();
        try {
            Thread.sleep(loadDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return balance.map(b -> new BalanceView(id, b, Instant.now())).orElse(null);
    }

    /**
     * Depósito DENTRO de uma transação cujo commit demora (beforeCommit dorme): mostra se o evict acontece antes ou depois do commit.
     * Evict pelo gerenciador padrão (não ciente de transação).
     */
    @org.springframework.transaction.annotation.Transactional
    @CacheEvict(cacheNames = CACHE, key = "#accountId")
    public void depositSlowCommit(UUID accountId, BigDecimal amount, long commitDelayMs) {
        slowCommit(accountId, amount, commitDelayMs);
    }

    /** O mesmo, mas o evict passa pelo gerenciador transactionAware(): só acontece DEPOIS do commit. */
    @org.springframework.transaction.annotation.Transactional
    @CacheEvict(cacheNames = CACHE, key = "#accountId", cacheManager = "txAwareCacheManager")
    public void depositSlowCommitTxAware(UUID accountId, BigDecimal amount, long commitDelayMs) {
        slowCommit(accountId, amount, commitDelayMs);
    }

    private void slowCommit(UUID accountId, BigDecimal amount, long commitDelayMs) {
        jdbc.sql("UPDATE accounts SET balance = balance + :a, version = version + 1 WHERE id = :id")
                .param("a", amount).param("id", accountId).update();
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void beforeCommit(boolean readOnly) {
                        try {
                            Thread.sleep(commitDelayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
    }

    /** Evict pelo gerenciador transactionAware(): se houver transação em andamento (a do chamador), só acontece após o commit dela. */
    @CacheEvict(cacheNames = CACHE, key = "#accountId", cacheManager = "txAwareCacheManager")
    public void depositEvictTxAware(UUID accountId, BigDecimal amount) {
        jdbc.sql("UPDATE accounts SET balance = balance + :a, version = version + 1 WHERE id = :id")
                .param("a", amount).param("id", accountId).update();
    }

    /** Usa um Redis inexistente (queda do cache). */
    @Cacheable(cacheNames = CACHE, key = "#accountId", cacheManager = "deadCacheManager")
    public BalanceView balanceWithDeadRedis(UUID accountId) {
        return load(accountId);
    }
}
