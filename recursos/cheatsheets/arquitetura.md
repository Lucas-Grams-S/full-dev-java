# Cheatsheet — Arquitetura de aplicações (P4)

> Valores **executados** em `2-pleno/P4-arquitetura-aplicacoes/lab/architecture-lab/` (Spring Boot 4.1.2, Spring Modulith 2.1.2, ArchUnit 1.4.2, JDK 25). Veja a aula indicada para o contexto.

## Onde cada coisa mora (hexágono pragmático, decisão 116)
| Camada | Pode importar | **Não** pode importar | Exemplo |
|---|---|---|---|
| `domain` | `jakarta.persistence` (só ele), `shared` | `org.springframework.*`, `application`, `adapter` | `Account`, `Transfer`, `Money` |
| `application` | `domain`, portas | `adapter` | `TransferService` (*package-private*), `port.in.*`, `port.out.*` |
| `adapter.in` | `application.port.in` | `adapter.out` | `TransferController`, webhook, *job* |
| `adapter.out` | `application.port.out`, `domain`, outros módulos **só pela API** | — | `AccountStoreJpa`, `OutboxEvents`, `VendorAdapter` |

## Regras de arquitetura (ArchUnit 1.4.2)
| Regra | Decisão |
|---|---|
| domínio sem framework; aplicação sem adaptadores; `adapter.in` sem `adapter.out` | 116 |
| **sem `@Autowired` em campo** (`NO_CLASSES_SHOULD_USE_FIELD_INJECTION` existe na biblioteca) | 30 |
| `@Service` não público | 122 |
| **SQL só em `adapter.out`** | 124 |
| sem ciclos entre módulos (`slices().matching("com.payflow.(*)..")`) | 122 |
| `@Transactional` só em `..application..` | 35 |
| **toda regra tem exemplo ruim que a faz falhar** + `.because(...)` | 123 |

## Spring Modulith 2.1.2
| Quero | Faço |
|---|---|
| Dependências | `spring-modulith-bom` (o Boot **não** gerencia) + `starter-core` + **`events-api`** (para `@ApplicationModuleListener`) + `starter-test` |
| Verificar | `ApplicationModules.of(Application.class).verify()` (ciclo, tipo interno, `allowedDependencies`) |
| Módulo aberto | `@ApplicationModule(type = OPEN)` em `package-info.java` (só `shared`) |
| Lista branca | `@ApplicationModule(allowedDependencies = "core::api")` + `@NamedInterface("api")` no subpacote |
| Testar um módulo | `@ApplicationModuleTest` **no pacote do módulo** (padrão `STANDALONE`); `Scenario` espera o evento |
| Documentar | `new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml().writeModuleCanvases()` |
| Fixtures ruins | `ApplicationModules.of("pacote", location -> true)` (o padrão **exclui testes**) |

## Eventos: o que acontece quando o ouvinte falha
| Mecanismo | Quando roda | Falha do ouvinte |
|---|---|---|
| `@EventListener` | **síncrono**, mesma transação | **desfaz** a transferência (`a=100.00 b=0.00`) |
| `@TransactionalEventListener(AFTER_COMMIT)` | depois do *commit* | o dado **fica** (⚠️ tratamento da exceção não executado) |
| `@ApplicationModuleListener` | **assíncrono**, depois do *commit*, transação própria | **invisível** ao publicador (`b=40.00`) |
| Ouvinte do **Outbox** | **síncrono** (mesma transação) | obrigatório, senão volta a escrita dupla |

## Padrões GoF no PayFlow
| Padrão | Uso | Armadilha |
|---|---|---|
| Strategy | `List<TransferPolicy>` injetada (OCP) | estratégia com estado; ordem implícita |
| Factory | `Money.of`, `Notification.email` | fábrica sem regra = cerimônia |
| Builder | ≥ 4 campos opcionais; dados de teste | `build()` sem validar |
| Adapter | `VendorAdapter` do PSP | vazar o tipo do fornecedor para a porta |
| Decorator | `@Transactional`/`@Async`/`@Cacheable` (proxy), auditoria | **auto-chamada** ignora o proxy |
| Observer | `TransferCompleted` | fluxo implícito (ordem, falha, transação) |
| Template Method | `OncePerRequestFilter`, `JdbcTemplate` | herança acopla: prefira composição no código seu |
