package com.payflow.lab;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** P3.02: executor limitado, handler de exceção para métodos void e propagação do MDC. */
@Configuration
@EnableAsync
class AsyncConfig implements AsyncConfigurer {
    static final List<String> uncaught = new CopyOnWriteArrayList<>();

    @Bean("labExecutor")
    ThreadPoolTaskExecutor labExecutor() {
        var ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2); ex.setMaxPoolSize(2); ex.setQueueCapacity(2);   // 2 rodando + 2 na fila; o resto é rejeitado
        ex.setThreadNamePrefix("lab-async-");
        ex.setTaskDecorator(r -> {                                             // copia o MDC (requestId) para a thread do @Async
            var ctx = MDC.getCopyOfContextMap();
            return () -> {
                if (ctx != null) MDC.setContextMap(ctx);
                try { r.run(); } finally { MDC.clear(); }
            };
        });
        ex.initialize();
        return ex;
    }

    @Override public Executor getAsyncExecutor() { return labExecutor(); }

    @Override public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method m, Object... args) -> uncaught.add(m.getName() + ": " + ex.getMessage());
    }
}
