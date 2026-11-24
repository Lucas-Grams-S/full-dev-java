package com.payflow.lab;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

class ArchitectureRulesTest {

    static final JavaClasses APP = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.payflow.lab");

    static final JavaClasses BAD = new ClassFileImporter().importPackages("com.payflow.fixtures.bad");

    // ---------- as regras (as mesmas valem para o código real e para o código "ruim")

    static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "..adapter..", "..application..")
            .because("o domínio é puro: regras de negócio não conhecem framework nem infraestrutura");

    static final ArchRule APPLICATION_DOES_NOT_KNOW_ADAPTERS = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..")
            .because("casos de uso falam com PORTAS, nunca com adaptadores");

    static final ArchRule ADAPTERS_DO_NOT_TALK_TO_EACH_OTHER = noClasses().that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
            .because("um controller chama um caso de uso (porta de entrada), não um repositório");

    static final ArchRule NO_FIELD_INJECTION = noFields().should().beAnnotatedWith(Autowired.class)
            .because("injeção só por construtor (decisão 30)");

    static final ArchRule SERVICES_ARE_NOT_PUBLIC = classes().that().areAnnotatedWith(Service.class)
            .should().notBePublic()
            .because("quem está fora do módulo usa a interface, não a implementação");

    static final JavaClasses BAD_SQL = new ClassFileImporter().importPackages("com.payflow.fixtures.badsql");

    static final ArchRule SQL_ONLY_IN_PERSISTENCE_ADAPTERS = noClasses().that().resideOutsideOfPackage("..adapter.out..")
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.jdbc.core.simple.JdbcClient")
            .because("SQL é detalhe de persistência: só um adaptador de saída o escreve (e só do PRÓPRIO módulo)");

    static final ArchRule WEB_ONLY_THROUGH_SERVICES = Architectures.layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Web").definedBy("..web..")
            .layer("Service").definedBy("..application..")
            .layer("Persistence").definedBy("..repository..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Web")
            .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service")
            .because("a tela chama o caso de uso; o caso de uso chama a persistência");

    // ---------- o código real obedece

    @Test
    void domainIsFrameworkFree() { DOMAIN_IS_FRAMEWORK_FREE.check(APP); }

    @Test
    void applicationDoesNotKnowAdapters() { APPLICATION_DOES_NOT_KNOW_ADAPTERS.check(APP); }

    @Test
    void adaptersDoNotTalkToEachOther() { ADAPTERS_DO_NOT_TALK_TO_EACH_OTHER.check(APP); }

    @Test
    void noFieldInjection() { NO_FIELD_INJECTION.check(APP); }

    @Test
    void servicesAreNotPublic() { SERVICES_ARE_NOT_PUBLIC.check(APP); }

    @Test
    void sqlOnlyInPersistenceAdapters() { SQL_ONLY_IN_PERSISTENCE_ADAPTERS.check(APP); }

    @Test
    void modulesAreFreeOfCycles() {
        slices().matching("com.payflow.lab.(*)..").should().beFreeOfCycles().check(APP);
    }

    // ---------- PROVA de que as regras funcionam: contra o código ruim, elas FALHAM

    @Test
    void theRulesFailOnBadCode() {
        for (var rule : new ArchRule[] { DOMAIN_IS_FRAMEWORK_FREE, NO_FIELD_INJECTION, SERVICES_ARE_NOT_PUBLIC }) {
            var result = rule.evaluate(BAD);
            System.out.println("RULE '" + rule.getDescription() + "' -> violations=" + result.getFailureReport().getDetails().size());
            result.getFailureReport().getDetails().forEach(d -> System.out.println("   " + d));
            assertThat(result.hasViolation()).as(rule.getDescription()).isTrue();
        }
    }

    @Test
    void layeredRuleCatchesControllerThatSkipsTheService() {
        var result = WEB_ONLY_THROUGH_SERVICES.evaluate(BAD);
        System.out.println("LAYERED -> violations=" + result.getFailureReport().getDetails().size());
        result.getFailureReport().getDetails().forEach(d -> System.out.println("   " + d));
        assertThat(result.hasViolation()).isTrue();
    }

    @Test
    void modulithDoesNotSeeSqlCouplingButArchUnitDoes() {
        // 1) o Modulith aprova: não há dependência de TIPO entre os módulos
        org.springframework.modulith.core.ApplicationModules.of("com.payflow.fixtures.badsql", location -> true).verify();
        // 2) a regra de ArchUnit reprova
        var result = SQL_ONLY_IN_PERSISTENCE_ADAPTERS.evaluate(BAD_SQL);
        System.out.println("SQL RULE -> modulith.verify() PASSOU; archunit violations=" + result.getFailureReport().getDetails().size());
        assertThat(result.hasViolation()).isTrue();
    }

    @Test
    void aRuleWithATypoInThePackageFailsInsteadOfPassingSilently() {
        var typo = noClasses().that().resideInAPackage("..doman..")      // "doman": nada casa
                .should().dependOnClassesThat().resideInAPackage("org.springframework..");
        var thrown = org.junit.jupiter.api.Assertions.assertThrows(AssertionError.class, () -> typo.check(APP));
        System.out.println("TYPO RULE -> " + thrown.getMessage().lines().findFirst().orElse(""));
        // com a opção desligada, a regra com typo PASSARIA para sempre (falsa segurança):
        // arquivo archunit.properties: archRule.failOnEmptyShould=false
    }
}
