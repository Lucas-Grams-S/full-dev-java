# P4 — Arquitetura de aplicações

> 🔵 Pleno · ⏱️ 18h (12 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **laboratório executado (Spring Boot 4.1.2, Spring Modulith 2.1.2, ArchUnit 1.4.2, JDK 25.0.5; 40 testes, sem Docker); o PayFlow do aluno foi revisado apenas por leitura; testes práticos no fim do treinamento, por decisão do aluno**

## 🎯 Objetivo do módulo
Estruturar o PayFlow como um **monólito modular bem desenhado**: separar responsabilidades, aplicar **hexagonal**, **SOLID**, **padrões GoF** e **DDD** (tático e estratégico) **com critério**, organizar em **módulos** (Spring Modulith) e **impor** as regras por **testes de arquitetura** (ArchUnit), sem mudar o contrato HTTP nem o esquema.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [P4.01](P4.01-separacao-de-responsabilidades-mvc-vs-camadas.md) | Separação de responsabilidades: MVC × camadas | 1h30 |
| [P4.02](P4.02-arquitetura-hexagonal-e-clean-architecture.md) | Arquitetura hexagonal e Clean Architecture | 1h30 |
| [P4.03](P4.03-solid-com-exemplos-em-spring.md) | SOLID com exemplos em Spring | 1h30 |
| [P4.04](P4.04-padroes-gof-i-strategy-factory-e-builder.md) | Padrões GoF I: Strategy, Factory e Builder | 1h30 |
| [P4.05](P4.05-padroes-gof-ii-adapter-decorator-observer-e-template-method.md) | Padrões GoF II: Adapter, Decorator, Observer e Template Method | 1h30 |
| [P4.06](P4.06-ddd-tatico-i-entidades-value-objects-e-agregados.md) | DDD tático I: entidades, *value objects* e agregados | 1h30 |
| [P4.07](P4.07-ddd-tatico-ii-repositorios-e-eventos-de-dominio.md) | DDD tático II: repositórios e eventos de domínio | 1h30 |
| [P4.08](P4.08-ddd-estrategico-bounded-contexts-e-linguagem-ubiqua.md) | DDD estratégico: *bounded contexts* e linguagem ubíqua | 1h30 |
| [P4.09](P4.09-monolito-modular-com-spring-modulith.md) | Monólito modular com Spring Modulith | 1h30 |
| [P4.10](P4.10-archunit-testes-de-arquitetura.md) | ArchUnit: testes de arquitetura | 1h30 |
| [P4.11](P4.11-estudo-de-caso-o-modulo-que-sabia-demais.md) | 🏗️ Estudo de caso: o módulo que sabia demais | 1h30 |
| [P4.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): `lab/architecture-lab/` (Maven, Spring Boot **4.1.2**, Spring Modulith **2.1.2**, ArchUnit **1.4.2**, **40 testes**, **sem Docker**).

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório** (módulos hexagonais, `verify()`, `@ApplicationModuleTest`, eventos síncronos × assíncronos, regras de ArchUnit e a **prova** de que elas falham, padrões GoF, acoplamento por SQL) **foi executado** nas versões acima; **as saídas mostradas nas aulas são as reais**. **Não foi executado:** o **PayFlow do aluno** (tudo o que é "por leitura": a reorganização em módulos, `Money` no JPA, `TransferPolicy`, regras no repositório real), o **registro de publicações** do Modulith (`events-jdbc`), `@Externalized`, `archunit-junit5`, `FreezingArchRule`, permissões por *schema* no PostgreSQL. Cada aula diz o que foi e o que não foi executado.

> **Avisos de ordem:** a P4.01 **retoma** as camadas da J5 e o *pacote por camada* da decisão 29; a P4.02 usa o Outbox (P3.09) e o PSP (P3.10); a P4.05 usa `@Async`/`@Transactional` por *proxy* (P3.02, J5.11); a P4.06 reaproveita a **exceção** da transferência (travas ordenadas, P2.02); a P4.07 fecha a **lacuna** do Outbox × `@TransactionalEventListener` (citado na P3.09); `FreezingArchRule`, registro de publicações e `@Externalized` são **citados e marcados** como não executados. **Nenhum conceito de P5+ é pré-requisito** (microsserviços só como "o passo seguinte").

## 🏗️ Entrega no PayFlow
PayFlow reorganizado em **módulos hexagonais** (`shared`, `identity`, `account`, `transfer`, `payment`, `notification`, `antifraud`, `reporting`, `outbox`) com **Spring Modulith** (`verify()` no CI, diagrama gerado), **regras de ArchUnit** (domínio sem framework, camadas, SQL só no adaptador do dono, sem injeção em campo) **provadas** por exemplos ruins, `TransferPolicy` (Strategy), `Money` (value object), `TransferCompleted` (evento de domínio) e **um dono por tabela**; **contrato HTTP e esquema inalterados**. Tag `payflow-P4`. Decisões: ADR [0016](../../projeto/adrs/0016-monolito-modular-hexagonal-ddd-e-regras-de-arquitetura.md).

## ➡️ Próximo
[P5 — Microsserviços e integração](../P5-microsservicos-integracao/README.md)
