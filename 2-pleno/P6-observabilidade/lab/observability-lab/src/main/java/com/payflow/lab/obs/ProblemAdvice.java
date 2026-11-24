package com.payflow.lab.obs;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Problem Details (decisão 43) + traceId na resposta: o usuário "lê" ao suporte o que o log e o trace indexam. */
@RestControllerAdvice
class ProblemAdvice extends ResponseEntityExceptionHandler {   // 404, 405, 400 do Spring MVC mantêm o status correto e NÃO viram ERROR
    private static final Logger log = LoggerFactory.getLogger(ProblemAdvice.class);
    private final Tracer tracer;

    ProblemAdvice(Tracer tracer) {
        this.tracer = tracer;
    }

    @ExceptionHandler(Rejection.class)
    ProblemDetail rejection(Rejection e) {
        return withTrace(ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        log.error("unexpected failure", e);   // ERROR uma vez, na borda, com a stack
        return withTrace(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error"));
    }

    private ProblemDetail withTrace(ProblemDetail pd) {
        Span span = tracer.currentSpan();
        if (span != null) {
            pd.setProperty("traceId", span.context().traceId());
        }
        return pd;
    }
}
