package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.payflow.lab.service.BalanceService;
import com.payflow.lab.service.BalanceView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

/** P2.09: Redis como cache-aside: hit/miss, TTL, JSON, invalidação, janela de dado velho, stampede, null. */
@SpringBootTest
class CacheIT {

    @Autowired BalanceService service;
    @Autowired StringRedisTemplate redis;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void clean() {
        LabSupport.reset(jdbc, "1000.00");
        redis.getConnectionFactory().getConnection().serverCommands().flushDb();
        service.resetLoads();
        service.loadDelayMs(100);
    }

    @Test
    void hitMissTtlAndJsonInRedis() {
        BalanceView first = service.balance(LabSupport.A1);
        BalanceView second = service.balance(LabSupport.A1);
        System.out.println("1ª: " + first + " | 2ª: " + second + " | consultas ao banco: " + service.dbLoads());
        assertThat(service.dbLoads()).isEqualTo(1);
        var keys = redis.keys("payflow:balances::*");
        String key = keys.iterator().next();
        System.out.println("chave Redis: " + key + " | TTL(s): " + redis.getExpire(key) + " | valor: " + redis.opsForValue().get(key));
        assertThat(redis.getExpire(key)).isBetween(1L, 30L);
        assertThat(second.balance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void evictOnWrite_thenFreshValue() {
        service.balance(LabSupport.A1);
        service.deposit(LabSupport.A1, new BigDecimal("50.00"));
        BalanceView after = service.balance(LabSupport.A1);
        System.out.println("depois do depósito + @CacheEvict: " + after.balance() + " (consultas ao banco: " + service.dbLoads() + ")");
        assertThat(after.balance()).isEqualByComparingTo("1050.00");
        assertThat(service.dbLoads()).isEqualTo(2);
    }

    @Test
    void slowReaderVersusWriter_leavesStaleValueInTheCache() throws Exception {
        service.loadDelayMs(400);
        ExecutorService ex = Executors.newFixedThreadPool(1);
        Future<BalanceView> reader = ex.submit(() -> service.balance(LabSupport.A1));     // lê 1000.00 e demora 400 ms para devolver
        Thread.sleep(150);
        service.deposit(LabSupport.A1, new BigDecimal("50.00"));                           // banco = 1050.00 e evict (nada a evictar ainda)
        BalanceView stale = reader.get();                                                  // o leitor grava 1000.00 no cache DEPOIS do evict
        ex.shutdown();
        BigDecimal db = jdbc.sql("SELECT balance FROM accounts WHERE id = :id").param("id", LabSupport.A1).query(BigDecimal.class).single();
        service.loadDelayMs(0);
        BalanceView cached = service.balance(LabSupport.A1);
        System.out.println("banco = " + db + " | cache = " + cached.balance() + " (leitor devolveu " + stale.balance() + ")");
        assertThat(db).isEqualByComparingTo("1050.00");
        assertThat(cached.balance()).isEqualByComparingTo("1000.00");                        // dado velho até o TTL (30 s)
    }

    private int stampede(String mode) throws Exception {
        service.loadDelayMs(300);
        int threads = 20;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<BalanceView>> fs = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            fs.add(ex.submit(() -> {
                go.await();
                return switch (mode) {
                    case "sync" -> service.balanceSync(LabSupport.A1);
                    case "locked" -> service.balanceSyncLocked(LabSupport.A1);
                    default -> service.balance(LabSupport.A1);
                };
            }));
        }
        go.countDown();
        for (Future<BalanceView> f : fs) {
            f.get();
        }
        ex.shutdown();
        return service.dbLoads();
    }

    @Test
    void cacheStampede_withoutSync_withSync_andWithLockingWriter() throws Exception {
        int without = stampede("none");
        System.out.println("STAMPEDE sem sync: 20 threads, chave fria -> " + without + " consultas ao banco");
        clean();
        int sync = stampede("sync");
        System.out.println("STAMPEDE sync=true com o escritor padrão: " + sync + " consultas ao banco");
        clean();
        int locked = stampede("locked");
        System.out.println("STAMPEDE sync=true com lockingRedisCacheWriter: " + locked + " consulta(s) ao banco");
        assertThat(without).isGreaterThan(1);
        assertThat(locked).isEqualTo(1);
    }

    @Test
    void unknownAccount_nullHandling_andPenetration() {
        UUID ghost = UUID.randomUUID();
        Throwable t = catchThrowable(() -> service.balance(ghost));
        System.out.println("null com disableCachingNullValues: " + (t == null ? "nenhum erro" : t.getClass().getName() + ": " + t.getMessage()));
        service.resetLoads();
        for (int i = 0; i < 5; i++) {
            service.balanceUnlessNull(ghost);
        }
        System.out.println("5 consultas a conta inexistente com unless=#result==null -> " + service.dbLoads() + " idas ao banco (penetração)");
        assertThat(service.dbLoads()).isEqualTo(5);
    }

    @Autowired org.springframework.cache.CacheManager cacheManager;                       // o @Primary (escritas imediatas)
    @Autowired org.springframework.data.redis.cache.RedisCacheManager asyncDefaultCacheManager;

    private int missesRightAfterPut(org.springframework.cache.CacheManager manager) {
        var cache = manager.getCache(BalanceService.CACHE);
        int misses = 0;
        for (int i = 0; i < 500; i++) {
            UUID id = UUID.randomUUID();
            cache.put(id, new BalanceView(id, new BigDecimal("1.00"), java.time.Instant.now()));
            if (cache.get(id) == null) {          // lê LOGO depois de gravar
                misses++;
            }
        }
        return misses;
    }

    @Test
    void asyncWritesByDefault_versusImmediateWrites() {
        int async = missesRightAfterPut(asyncDefaultCacheManager);
        int immediate = missesRightAfterPut(cacheManager);
        System.out.println("put + get imediato, 500 chaves: escritor padrão = " + async + " 'misses'; immediateWrites() = " + immediate);
        assertThat(immediate).isZero();
    }

    @Autowired org.springframework.cache.CacheManager txAwareCacheManager;

    private String staleAfter(java.util.function.Consumer<Long> writer) throws Exception {
        service.loadDelayMs(0);
        service.balance(LabSupport.A1);                                    // aquece o cache com 1000.00
        var stop = new java.util.concurrent.atomic.AtomicBoolean();
        var reader = Executors.newSingleThreadExecutor();
        Future<?> loop = reader.submit(() -> {                               // leitor constante, como o tráfego de leitura real
            while (!stop.get()) {
                service.balance(LabSupport.A1);
                try { Thread.sleep(20); } catch (InterruptedException e) { return; }
            }
        });
        writer.accept(400L);                                                // escreve; o commit demora 400 ms
        Thread.sleep(150);
        stop.set(true);
        loop.get();
        reader.shutdown();
        BigDecimal db = jdbc.sql("SELECT balance FROM accounts WHERE id = :id").param("id", LabSupport.A1).query(BigDecimal.class).single();
        BigDecimal cached = service.balance(LabSupport.A1).balance();
        return "banco=" + db + " cache=" + cached;
    }

    @Test
    void evictBeforeCommit_leavesStaleValue_andTransactionAwareFixesIt() throws Exception {
        Object proxy = service;
        var advisors = ((org.springframework.aop.framework.Advised) proxy).getAdvisors();
        System.out.println("advisors do proxy (mais externo primeiro): " + java.util.Arrays.stream(advisors)
                .map(a -> a.getClass().getSimpleName()).toList());
        String plain = staleAfter(ms -> service.depositSlowCommit(LabSupport.A1, new BigDecimal("50.00"), ms));
        System.out.println("@CacheEvict comum + commit lento:        " + plain);
        clean();
        String aware = staleAfter(ms -> service.depositSlowCommitTxAware(LabSupport.A1, new BigDecimal("50.00"), ms));
        System.out.println("@CacheEvict transactionAware + commit lento: " + aware);
        assertThat(aware).isEqualTo("banco=1050.00 cache=1050.00");
    }

    @Autowired com.payflow.lab.service.OuterTxService outer;

    @Test
    void evictInsideCallerTransaction_leavesStaleValue_unlessTransactionAware() throws Exception {
        String plain = staleAfter(ms -> outer.deposit(LabSupport.A1, new BigDecimal("50.00"), ms, false));
        System.out.println("evict dentro da transação do CHAMADOR, comum:           " + plain);
        clean();
        String aware = staleAfter(ms -> outer.deposit(LabSupport.A1, new BigDecimal("50.00"), ms, true));
        System.out.println("evict dentro da transação do CHAMADOR, transactionAware: " + aware);
        assertThat(aware).isEqualTo("banco=1050.00 cache=1050.00");
        assertThat(plain).isEqualTo("banco=1050.00 cache=1000.00");
    }
}
