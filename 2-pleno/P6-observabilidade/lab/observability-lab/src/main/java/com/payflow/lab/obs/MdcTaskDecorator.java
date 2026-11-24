package com.payflow.lab.obs;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

/** Copia o MDC (requestId, traceId, spanId) da thread que submete para a thread que executa (P3.02). */
@Component
@ConditionalOnProperty(name = "lab.mdc-task-decorator", havingValue = "true")
class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context != null) {
                MDC.setContextMap(context);
            }
            try {
                task.run();
            } finally {
                if (previous != null) {
                    MDC.setContextMap(previous);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
