package com.payflow.lab.service;

import com.payflow.lab.domain.Transfer;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Retry FORA da transação: cada tentativa chama o proxy @Transactional de outro bean e abre uma transação nova.
 * @Retryable do Spring Framework 7 (nativo, sem Spring Retry). Repete SÓ a falha de versão, com limite, atraso e jitter.
 */
@Service
public class RetryingTransferFacade {

    private final AtomicInteger attempts = new AtomicInteger();
    private final TransferService service;

    public RetryingTransferFacade(TransferService service) {
        this.service = service;
    }

    /** Getter, não campo público: o bean é um proxy de classe (CGLIB) e o campo do PROXY nunca é inicializado (NullPointerException). */
    public int attempts() {
        return attempts.get();
    }

    @Retryable(includes = ObjectOptimisticLockingFailureException.class,
            maxRetries = 6, delay = 20, jitter = 20, multiplier = 2.0, maxDelay = 400)
    public Transfer transfer(UUID from, UUID to, BigDecimal amount, String key, long pauseMs) {
        attempts.incrementAndGet();
        return service.transferOptimistic(from, to, amount, key, pauseMs);
    }

    private final AtomicInteger deadlockAttempts = new AtomicInteger();

    public int deadlockAttempts() {
        return deadlockAttempts.get();
    }

    /**
     * Pessimista SEM ordem (propenso a deadlock) com retry sobre a SUPERCLASSE ConcurrencyFailureException:
     * cobre @Version (otimista), deadlock (40P01), serialização (40001) e lock_timeout (55P03).
     */
    @Retryable(includes = ConcurrencyFailureException.class, maxRetries = 6, delay = 20, jitter = 40, multiplier = 2.0, maxDelay = 400)
    public Transfer transferUnorderedWithRetry(UUID from, UUID to, BigDecimal amount, String key, long pauseMs) {
        deadlockAttempts.incrementAndGet();
        return service.transferPessimisticUnordered(from, to, amount, key, pauseMs);
    }
}
