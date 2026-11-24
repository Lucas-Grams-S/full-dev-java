# P4.CP — Checkpoint do módulo P4

> ⏱️ ~1h30 · 🔵 Pleno · Pré-requisitos: P4.01 a P4.11

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, JDK 25.0.5, Maven 3.9, **sem Docker**): `lab/architecture-lab/` (Spring Boot **4.1.2**, Spring Modulith **2.1.2**, ArchUnit **1.4.2**; **40 testes**; `out-suite-resumo.txt`): módulos `account`/`transfer`/`notification`/`antifraud`/`shared` (hexagonais), `verify()` e `Documenter`, `@ApplicationModuleTest` + `Scenario`, `@ApplicationModuleListener`, regras de ArchUnit **e a prova de que falham** no código ruim, dois adaptadores da mesma porta com **teste de contrato**, ouvinte síncrono × assíncrono que falha, padrões GoF em Java puro e a reprodução do acoplamento por SQL. **Não executado:** o **PayFlow do aluno** (tudo o que está em "Entrega prática"), o registro de publicações do Modulith (`events-jdbc`), `@Externalized`, `archunit-junit5`, `FreezingArchRule`, `onionArchitecture()`, PostgreSQL/permissões por *schema*, e o mapeamento JPA de `Money`.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Diferencie **MVC** de **arquitetura em camadas**. Por que "pacote por camada" **obriga** classes `public` e como "pacote por funcionalidade" resolve? O que significa a mensagem `Layer 'Service' is empty` do ArchUnit?
2. Desenhe o **hexágono**: o que é **porta de entrada**, **porta de saída** e **adaptador**? Para onde apontam as setas de dependência? Por que a porta **não** isola a **transação** (use o Outbox)?
3. Explique o **D** de SOLID e a diferença para **injeção de dependência**. Como o **OCP** foi **provado** no laboratório (`List<TransferPolicy>`) e como o **teste de contrato** dá substância ao **LSP**?
4. Quando **Strategy** vale mais que um `if`/`enum`? Onde o Spring usa **Decorator** (e por que a **auto-chamada** o contorna)? Dê **uma** vantagem e **um** risco do **Observer**.
5. **Entidade × value object**: classifique `Account`, `Money`, `BalanceView`, `LedgerEntry`, `IdempotencyKey`. Por que `Account` não tem `setBalance` e por que `equals` da entidade é por **id**?
6. O que é um **agregado** e quais as 4 regras práticas? Por que a **transferência altera dois `Account` na mesma transação** e **quando** essa exceção deixa de valer?
7. O que um **ouvinte síncrono** (`@EventListener`) faz quando **falha**? E um `@ApplicationModuleListener`? Use o laboratório (`a=100.00 b=0.00` × `b=40.00`). Por que o ouvinte do **Outbox** precisa ser **síncrono**?
8. O que é um **bounded context**? Dê um exemplo de **mesma palavra, modelos diferentes** no PayFlow e cite **3** relações do **mapa de contexto** (com exemplos).
9. O que o Spring Modulith `verify()` reprova (3 itens)? Cite as **três surpresas** do laboratório (dependência `events-api`, `ApplicationModules.of(String)` e pacote do `@ApplicationModuleTest`). Por que `shared` é **módulo aberto**?
10. Por que o `verify()` **passou** no estudo de caso "o módulo que sabia demais"? O que reprova esse problema, qual o **limite** dessa proteção e quais as **três correções** (A/B/C) possíveis?

<details><summary>Gabarito</summary>

1. **MVC** organiza a **interação com o usuário** (controller recebe, model carrega dados, view mostra); **camadas** organizam a **aplicação inteira** por responsabilidade (web, serviço, domínio, persistência) com a regra de dependência para baixo. Em Java, só `public` atravessa pacotes: **por camada**, o `TransferService` precisa ser `public` para o controller enxergá-lo (então **qualquer** classe pode usá-lo); **por funcionalidade**, controller e serviço **moram juntos** e o serviço pode ser *package-private*: o compilador **protege** a fronteira. `Layer 'Service' is empty`: por padrão o ArchUnit **reprova camada sem classes** (pacote com nome errado ou camada que não existe no código analisado).
2. O núcleo (domínio + casos de uso) define **portas** (interfaces **dele**); **de entrada** = o que o mundo pode pedir (`TransferUseCase`); **de saída** = o que o núcleo precisa (`Accounts`, `DomainEvents`); **adaptadores** (controller, JPA, cliente do PSP, `OutboxEvents`) **implementam/usam** as portas. As setas **apontam para o núcleo**. A porta separa **código**, não **transação**: o adaptador do Outbox **tem de participar da mesma transação** do caso de uso; senão volta a **escrita dupla** (P3.09).
3. **DIP**: o alto nível **não** depende do baixo; **ambos** dependem de abstração, e a abstração **pertence ao alto nível**. **DI** é a técnica de entregar a dependência de fora (dá para ter DI injetando a classe concreta, sem DIP). **OCP** (`OpenClosedTest`): com 2 políticas `2000.00` passa; ao **acrescentar um bean** `NightLimitPolicy`, a **mesma** `TransferChecker` (sem editar) recusa (`above night limit`). **LSP**: o contrato `AccountStoreContract` roda contra `InMemoryAccountStore` e `JdbcAccountStore` (ambos passam) e **reprova** uma implementação que devolve `null` no lugar de `Optional`.
4. **Strategy** vale quando as variantes **crescem**, **mudam por razões diferentes** ou **têm dependências** (com 3 variantes estáveis, um `enum` com método basta). **Decorator**: `@Transactional`, `@Async`, `@Cacheable` são **proxies**; a **auto-chamada** (`this.m()`) chama o **objeto real**, não o proxy, e a decoração some. **Observer**: vantagem = **desacoplamento** (o publicador não conhece os ouvintes); risco = **fluxo implícito** e perguntas escondidas (ordem, falha, transação, quem escuta).
5. **Entidades:** `Account`, `LedgerEntry` (imutável); **value objects:** `Money`, `IdempotencyKey`; **projeção:** `BalanceView`. Sem `setBalance` a **invariante** (saldo ≥ 0) mora **na entidade** (`withdraw`/`deposit`); `equals` por **id** porque o saldo **muda** (por todos os campos, a conta "some" do `HashSet` após um depósito).
6. **Agregado** = grupo tratado como unidade para mudanças, com **raiz** única e **invariantes** garantidas dentro; regras: (1) invariantes verdadeiras **dentro**, (2) agregados **pequenos**, (3) referenciar outros **por id**, (4) **uma transação por agregado**. A transferência altera **dois** `Account` porque a regra de negócio 2 (débito e crédito **juntos ou nenhum**) **proíbe** a consistência eventual entre eles; fica como **exceção documentada** (decisão 119), com **travas ordenadas** (P2.02). Deixa de valer quando a conta de destino estiver em **outro banco/serviço**: vira **saga** (S1).
7. **Síncrono:** a exceção **sobe ao publicador e desfaz** a transação (executado com banco real: `a=100.00 b=0.00`); **assíncrono** (`@ApplicationModuleListener`): roda **depois do *commit***, em transação própria; a falha é **invisível** ao publicador (`b=40.00`, ouvinte chamado 1×, erro só no log). O ouvinte do **Outbox** é **síncrono** porque precisa gravar a linha **na mesma transação** do dinheiro (atomicidade); um ouvinte assíncrono recriaria a **escrita dupla**.
8. **Bounded context** = fronteira dentro da qual **um modelo é consistente** e cada termo tem **um** significado. Exemplo: **`Account`** é *credencial/titular* em `identity`, *saldo + `@Version`* em `account`, *destinatário* em `notification`, *perfil de risco* em `antifraud`. Relações: **Cliente-Fornecedor** (`transfer` → `account`), **Published Language** (eventos), **Anti-Corruption Layer** (`VendorAdapter` do PSP), **Conformist** (PayFlow diante do PSP), **Shared Kernel** (`Money`).
9. **Ciclo** entre módulos, uso de **tipo interno** de outro módulo e dependência **fora de `allowedDependencies`**. Surpresas: (1) `spring-modulith-starter-core` **não** traz `@ApplicationModuleListener` (falta `spring-modulith-events-api`); (2) `ApplicationModules.of("pacote")` **exclui testes** por padrão (precisa `location -> true` para as fixtures); (3) o `@ApplicationModuleTest` **precisa estar no pacote do módulo**, senão nenhum módulo carrega. `shared` é **aberto** (`@ApplicationModule(type = OPEN)`) porque só tem **value objects** usados por todos; **nunca** para módulos de negócio.
10. `verify()` analisa **dependências de tipo Java**; o acoplamento estava numa **`String` de SQL** (`select balance from accounts`), sem `import` do outro módulo. A regra `SQL_ONLY_IN_PERSISTENCE_ADAPTERS` (ArchUnit) reprova o uso de `JdbcClient` **fora de `adapter.out`** (3 violações). **Limite:** ela vê **o uso de `JdbcClient`**, **não qual tabela** a `String` cita: o que fecha a brecha é **permissão no banco** (schema/role por módulo) ou **revisão** ("de quem é essa tabela?"). Correções: **A** chamar a **API** (em lote, `balancesOf`), **B** o **evento carrega o dado**, **C** **cópia própria** alimentada por evento.
</details>

## Parte 2: Entrega prática (tag `payflow-P4`)
O `payflow` é **reorganizado em módulos hexagonais** (Spring Modulith), com **regras de arquitetura no CI** e **sem mudar** o contrato HTTP nem o esquema do banco.
```
payflow/
├── docs/
│   ├── architecture.md           camadas × hexágono × módulos; o que é API pública de cada módulo      (P4.01, P4.02, P4.09)
│   ├── context-map.md            mapa de contexto (Mermaid), donos das tabelas, tipos de subdomínio     (P4.08, P4.11)
│   ├── modules.md                diagrama gerado pelo Documenter (cole) + regras de dependência        (P4.09)
│   ├── events.md                 eventos de domínio × integração; quem ouve o quê; garantias          (P4.07)
│   └── postmortem-modulo-que-sabia-demais.md                                                           (P4.11)
├── src/main/java/com/payflow/
│   ├── shared/                   Money, ids (módulo ABERTO; só value objects)                           (P4.06)
│   ├── identity/  account/  transfer/  payment/  notification/  antifraud/  reporting/  outbox/
│   │   └── <módulo>/             API pública no pacote raiz; domain/, application/ (port/in, port/out), adapter/in, adapter/out;
│   │                              implementações package-private; package-info.java com allowedDependencies
│   ├── transfer/TransferCompleted.java        evento de domínio (record, no raiz do módulo)             (P4.07)
│   ├── transfer/domain/…Policy.java           TransferPolicy: mínimo, limite por transferência, diário (P4.04)
│   └── account/AccountApi.java                balanceOf/withdraw/deposit + balancesOf em lote           (P4.11)
├── src/test/java/com/payflow/
│   ├── ModularityTests.java                   ApplicationModules.of(PayflowApplication.class).verify() + Documenter   (P4.09)
│   ├── ArchitectureRulesTest.java             as regras da P4.10 (domínio sem framework, camadas, SQL só em adapter.out, sem @Autowired em campo, ciclos)
│   ├── architecture/fixtures/…                exemplos RUINS que PROVAM que cada regra falha                       (P4.10)
│   ├── <módulo>/…ModuleTest.java              @ApplicationModuleTest no pacote do módulo (Scenario para eventos)  (P4.09)
│   ├── account/AccountStoreContractTest.java  contrato rodando contra o adaptador em memória E o JPA           (P4.03, P4.07)
│   └── transfer/TransferServiceUnitTest.java  caso de uso com fakes das portas, sem Spring                      (P4.02)
└── projeto/adrs/0016-monolito-modular-hexagonal-ddd-e-regras-de-arquitetura.md   (já redigido nesta geração)
```
Critérios de pronto:
- [ ] **Contrato HTTP e esquema inalterados** (nenhuma migração nova nesta etapa); a suíte de J5–P3 **continua verde**.
- [ ] `ModularityTests.verify()` **passa** e roda no **CI**; todo módulo tem `allowedDependencies` declarado; o `Documenter` gerou o diagrama em `docs/modules.md`.
- [ ] **Nenhum controller** usa repositório; `TransferService` **não importa** `org.springframework.data` nem `jakarta.persistence` (usa portas) (P4.01, P4.02).
- [ ] O `domain` só importa `jakarta.persistence` (decisão 116); **nenhum `@Autowired` em campo** (P4.10).
- [ ] Limites de transferência são `TransferPolicy` (**uma classe por regra**); acrescentar uma regra **não edita** o `TransferService` (teste como o `OpenClosedTest`) (P4.04).
- [ ] `Money` existe em `shared` com testes de arredondamento/escala; `Account` **sem `setBalance`**, `equals` por id (P4.06).
- [ ] `TransferCompleted` é publicado **pelo caso de uso**; o ouvinte do **Outbox é síncrono** (teste: *rollback* não deixa linha) e `notification`/`antifraud` usam `@ApplicationModuleListener` **com idempotência** (P4.07).
- [ ] **Cada regra de ArchUnit** tem um **exemplo ruim** que a faz falhar; o contrato do `AccountStore` roda contra **dois** adaptadores (P4.03, P4.10).
- [ ] **Cada tabela tem um módulo dono**; nenhum SQL fora de `adapter.out`; `reporting` usa `AccountApi.balancesOf` ou cópia própria, **nunca** `select` em `accounts` (P4.11).
- [ ] `docs/` com as 5 notas; ADR 0016 revisada; tag `payflow-P4`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Pleno: **≥ 2** em todos e **3** nos marcados com ⭐.

| Competência | Aula(s) | 0–3 |
|---|---|---|
| Separar responsabilidades: MVC × camadas, regra de dependência, pacote por funcionalidade | P4.01 | |
| Arquitetura hexagonal/Clean: portas, adaptadores, caso de uso sem framework | P4.02 | ⭐ |
| SOLID como diagnóstico (S, O, L com teste de contrato, I, D × DI) | P4.03 | |
| Strategy, Factory e Builder (e quando **não** usar) | P4.04 | |
| Adapter, Decorator (proxies do Spring), Observer, Template Method | P4.05 | |
| DDD tático: entidades, value objects, **agregados** e a exceção da transferência | P4.06 | ⭐ |
| Repositórios (DDD) e **eventos de domínio** (síncrono × assíncrono; domínio × integração) | P4.07 | ⭐ |
| DDD estratégico: contextos, mapa de contexto, linguagem ubíqua | P4.08 | ⭐ |
| **Monólito modular** com Spring Modulith (`verify`, `allowedDependencies`, teste de módulo) | P4.09 | ⭐ |
| **Testes de arquitetura** com ArchUnit (e provar que a regra falha) | P4.10 | ⭐ |
| Diagnosticar acoplamento por dado e impor "um dono por tabela" | P4.11 | |

**Se algum ⭐ ficou abaixo de 2:** refaça os experimentos (`lab/architecture-lab`: `./run-all.sh`) e o respectivo exercício de aplicação antes de seguir para a P5.

## Parte 4: Perguntas de entrevista (simulado rápido)
Responda em voz alta, 2 minutos cada: (1) "Explique arquitetura hexagonal e quando você **não** a usaria." (2) "SOLID: dê um exemplo de violação do Liskov." (3) "Entidade, value object e agregado: como você decide?" (4) "Como estruturar um monólito modular e evitar que o acoplamento volte?" (5) "O que são bounded contexts? Como achou as fronteiras?" (6) "`@EventListener`, `@TransactionalEventListener` e `@ApplicationModuleListener`: o que cada um faz quando falha?" (7) "Como você testa arquitetura?" (8) "Como evitar que dois módulos se acoplem pelo banco?"

## ➡️ Próximo
[P5 — Microsserviços e integração](../P5-microsservicos-integracao/README.md)
