package com.payflow.web.api;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Versão reduzida da J6.04: só o método write (o do PayFlow também tem render). */
@Component
public class ApiProblems {

    private final JsonMapper mapper;

    public ApiProblems(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String code, String title, String detail) {
        response.setStatus(status.value());
        ProblemDetail body = ApiErrors.problem(status, code, title, detail);
        try {
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(mapper.writeValueAsString(body));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
