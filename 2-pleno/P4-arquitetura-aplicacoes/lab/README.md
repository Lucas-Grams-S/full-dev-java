# Laboratório do P4

Tudo foi **executado** em Linux, JDK **25.0.5** (`/usr/lib/jvm/java-25-openjdk-amd64`), Maven 3.9, 12 núcleos **compartilhados**. **Sem Docker e sem rede em tempo de execução** (o banco é um H2 em memória, usado só para ter uma transação real). As saídas reais estão em `out-*.txt` e são **o que as aulas mostram**.

## `architecture-lab/` (Maven; P4.01 a P4.11)
Spring Boot **4.1.2**, Spring Modulith **2.1.2** (BOM), ArchUnit **1.4.2** (a mesma do Modulith), H2 (só `runtime`).

```bash
cd architecture-lab
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./run-all.sh      # roda `mvn test` e regrava os out-*.txt
```

### O que há no código
| Pacote | Conteúdo |
|---|---|
| `com.payflow.lab.shared` | `Money` (value object, módulo **aberto**) |
| `com.payflow.lab.account` | `AccountApi` (pública) + `domain/` (`Account`), `application/` (`AccountService`, porta `AccountStore`), `adapter/` (controller, `InMemoryAccountStore`) |
| `com.payflow.lab.transfer` | `TransferCompleted` (evento público) + `domain/Transfer`, `application/` (`TransferService`, portas `TransferUseCase`, `Accounts`, `DomainEvents`), `adapter/` (controller, `AccountsAdapter`, `SpringDomainEvents`) |
| `com.payflow.lab.notification`, `antifraud` | ouvintes `@ApplicationModuleListener` de `TransferCompleted` |

### Os testes (40) e o que cada um prova
| Teste | Aula | Prova |
|---|---|---|
| `DomainTests` | P4.06 | invariantes de `Account`, `Money` por valor, `Transfer` recusa origem = destino |
| `TransferServiceUnitTest` | P4.02 | o caso de uso roda **sem Spring** (fakes das portas) |
| `ArchitectureRulesTest` | P4.01, P4.02, P4.10, P4.11 | 6 regras passam no código real; **falham** no código ruim; camada vazia; *typo* no pacote; Modulith não vê SQL, ArchUnit vê |
| `ModularityTests` | P4.09 | módulos detectados, `verify()`, `Documenter`, ciclo e tipo interno reprovados, `allowedDependencies`/`@NamedInterface` |
| `TransferModuleTest` | P4.09 | `@ApplicationModuleTest` + `Scenario` com `AccountApi` *mockado* |
| `EndToEndTest` | P4.05, P4.07 | transferência síncrona, reações assíncronas, nada publicado se falha (com espera) |
| `SyncListenerFailureTest` / `AsyncListenerFailureTest` | P4.07 | ouvinte síncrono que falha **desfaz** a transferência; assíncrono é **invisível** |
| `AccountStoreContractTest` | P4.03, P4.07 | o **mesmo contrato** contra `InMemory` e `Jdbc`; reprova a implementação que devolve `null` |
| `OpenClosedTest` | P4.03, P4.04 | acrescentar uma `TransferPolicy` sem editar o consumidor |
| `PatternsTest` | P4.04, P4.05 | Strategy, Factory, Builder, Adapter, Decorator, Observer, Template Method |
| `SqlCouplingIncidentTest` | P4.11 | renomear coluna do dono quebra o outro módulo **só em tempo de execução** |

As **fixtures ruins** de propósito ficam em `src/test/java/com/payflow/fixtures/` (não fazem parte da aplicação).

### Saídas gravadas
`out-suite-resumo.txt` (contagem), `out-modules.txt`, `out-module-model.txt`, `out-components.puml` (diagrama gerado), `out-docs.txt`, `out-modulith-bad.txt`, `out-modulith-rules.txt`, `out-archunit-bad.txt`, `out-layered.txt`, `out-event-semantics.txt`, `out-incident.txt`.

## Surpresas que o laboratório achou (e as aulas registram)
1. `spring-modulith-starter-core` **não** traz `@ApplicationModuleListener`: falta `spring-modulith-events-api`.
2. `ApplicationModules.of("pacote")` **exclui** as classes de teste (as fixtures ruins só aparecem com `location -> true`).
3. `@ApplicationModuleTest` **precisa estar no pacote do módulo**; na raiz, nenhum módulo é carregado.
4. `layeredArchitecture` do ArchUnit **reprova camada vazia** (`Layer 'Service' is empty`).
5. O padrão `archRule.failOnEmptyShould = true` faz a regra com *typo* no pacote **falhar** em vez de passar.
6. O `verify()` **não vê** SQL em `String` (acoplamento por dado): só uma regra de ArchUnit e/ou permissão no banco.
7. Um ouvinte **assíncrono** que falha é invisível ao publicador; o **síncrono** desfaz a transação (com adaptador em memória o *rollback* não tem o que desfazer: foi preciso um adaptador JDBC para provar).
8. "Nada chegou" num ouvinte assíncrono só prova algo **esperando** (`await().during(...)`).
9. O Spring Modulith **não é gerenciado** pelo BOM do Spring Boot (importe `spring-modulith-bom`).
10. `NO_CLASSES_SHOULD_USE_FIELD_INJECTION`, `NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS` e `NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING` existem em `GeneralCodingRules` do ArchUnit 1.4.2 (conferidos com `javap`).
