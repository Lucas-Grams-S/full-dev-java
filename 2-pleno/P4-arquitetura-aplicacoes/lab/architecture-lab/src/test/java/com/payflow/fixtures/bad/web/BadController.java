package com.payflow.fixtures.bad.web;

import com.payflow.fixtures.bad.repository.BadRepository;

/** Controller que fala direto com o repositório: a "camada" de serviço foi pulada. */
public class BadController {
    private final BadRepository repository = new BadRepository();

    public String get() {
        return repository.find();
    }
}
