package com.payflow.lab;

import java.util.concurrent.CompletableFuture;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncDemo {
    @Async
    public CompletableFuture<String> where() {
        return CompletableFuture.completedFuture(Thread.currentThread().getName() + " requestId=" + MDC.get("requestId"));
    }

    @Async
    public void fireAndForgetThatFails() { throw new IllegalStateException("falhou no @Async void"); }

    @Async
    public CompletableFuture<String> failsWithFuture() { throw new IllegalStateException("falhou no @Async com Future"); }

    @Async
    public void slow(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Auto-chamada: NÃO passa pelo proxy, então roda na mesma thread. */
    public String callsAsyncOnSelf() {
        where();                       // ignorado pelo proxy
        return Thread.currentThread().getName();
    }
}
