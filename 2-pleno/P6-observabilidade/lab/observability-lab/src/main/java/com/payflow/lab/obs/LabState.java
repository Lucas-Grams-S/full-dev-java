package com.payflow.lab.obs;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Interruptores do laboratório (em produção NÃO existe nada parecido). */
@Component
public class LabState {
    public final AtomicBoolean dbUp = new AtomicBoolean(true);
    public final AtomicBoolean riskUp = new AtomicBoolean(true);
    public final AtomicBoolean outboxStalled = new AtomicBoolean(false);
    public final AtomicInteger outboxPending = new AtomicInteger();
    /** Incidente simulado (P6.07): o serviço de risco fica lento só para UMA conta. */
    public volatile String riskSlowAccount = "";
    public volatile int riskSlowMs = 0;
}
