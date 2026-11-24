package com.payflow.web.api;

import com.payflow.domain.NotFoundException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Versão reduzida da J6.04 com os três handlers que a J8 acrescenta (403, credenciais inválidas, excesso de tentativas). */
@RestControllerAdvice(basePackages = "com.payflow.web.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiErrors {

    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);
    private static final String TYPE_PREFIX = "urn:payflow:problem:";

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found", "No resource at this address.");
    }

    /** Sem este handler, o catch-all abaixo transformaria o 403 do @PreAuthorize em 500. */
    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException e) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", "You do not have permission to do this.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail invalidCredentials(BadCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Invalid credentials", "Invalid e-mail or password.");
    }

    @ExceptionHandler(LockedException.class)
    ProblemDetail tooManyAttempts(LockedException e) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, "too-many-attempts", "Too many attempts",
                "Too many failed attempts. Try again later.");
    }

    /** Como na J6.04: corpo inválido (ex.: senha com mais de 128 caracteres) é 400, antes de qualquer BCrypt. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException e) {
        return problem(HttpStatus.BAD_REQUEST, "validation-failed", "Validation failed", "One or more fields are invalid.");
    }

    /** Como na J6.04 (o ResponseEntityExceptionHandler): JSON ilegível ou com campo desconhecido (modo estrito) é 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request", "The request body is not valid.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception e) {
        log.error("Unexpected error in the API", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal error", "Something went wrong. Try again later.");
    }

    static ProblemDetail problem(HttpStatus status, String code, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_PREFIX + code));
        pd.setTitle(title);
        return pd;
    }
}
