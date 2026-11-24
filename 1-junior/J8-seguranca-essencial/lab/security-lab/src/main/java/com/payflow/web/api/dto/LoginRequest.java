package com.payflow.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Limites de tamanho: um corpo gigante não deve chegar ao BCrypt (custo de CPU). */
public record LoginRequest(@NotBlank @Size(max = 255) String email, @NotBlank @Size(max = 128) String password) {

    /** O toString padrão de um record imprime TODOS os componentes: um log.info("{}", request) vazaria a senha. */
    @Override
    public String toString() {
        return "LoginRequest[password=***]";
    }
}
