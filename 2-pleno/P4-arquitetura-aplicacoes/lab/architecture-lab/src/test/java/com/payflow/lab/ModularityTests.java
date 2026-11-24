package com.payflow.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

    static final ApplicationModules MODULES = ApplicationModules.of(Application.class);

    @Test
    void listsTheModules() {
        var names = MODULES.stream().map(m -> m.getIdentifier().toString()).collect(Collectors.toSet());
        System.out.println("MODULES = " + MODULES.stream().map(m -> m.getIdentifier().toString()).sorted().toList());
        assertThat(names).containsExactlyInAnyOrder("account", "transfer", "notification", "antifraud", "shared");
    }

    @Test
    void printsTheModuleModel() {
        MODULES.forEach(System.out::println);   // base package, beans, dependências de cada módulo
    }

    @Test
    void verifiesTheStructure() {
        MODULES.verify();   // falha com Violations se houver ciclo ou acesso a tipo interno
    }

    @Test
    void writesDocumentation() {
        new Documenter(MODULES).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml().writeModuleCanvases();
    }

    @Test
    void badFixtureIsRejected() {
        var bad = ApplicationModules.of("com.payflow.fixtures.badmod", location -> true);  // o padrão EXCLUI classes de teste
        var thrown = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, bad::verify);
        System.out.println("BAD MODULES VERIFY -> " + thrown.getClass().getSimpleName() + "\n" + thrown.getMessage());
        assertThat(thrown.getMessage()).contains("Cycle").contains("internal");
    }

    @Test
    void namedInterfaceAndAllowedDependencies() {
        // controle positivo: usar o subpacote exposto (core::api) é permitido
        ApplicationModules.of("com.payflow.fixtures.modok", location -> true).verify();

        var bad = ApplicationModules.of("com.payflow.fixtures.modbad", location -> true);
        var thrown = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, bad::verify);
        System.out.println("MODBAD VERIFY ->\n" + thrown.getMessage());
        assertThat(thrown.getMessage()).contains("reports").contains("extras").contains("internal");
    }
}
