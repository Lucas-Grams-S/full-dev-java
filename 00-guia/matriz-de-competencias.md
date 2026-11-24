# Matriz de competências

Base: tabela de níveis do prompt, completada com o padrão de vagas em `o-que-o-mercado-pede.md`. Use-a como autoavaliação nos checkpoints. A coluna final aponta onde a competência é ensinada; a tabela "competência → aulas" definitiva será gerada na revisão de cada nível.

| Nível | Escopo | Autonomia | Foco técnico |
|---|---|---|---|
| 🟢 **Júnior** | Tarefas bem definidas dentro de um sistema | Precisa de orientação e revisão | Código correto, testado e legível com Java, Spring, SQL e Git |
| 🔵 **Pleno** | Funcionalidades completas, de ponta a ponta | Resolve sozinho a maioria dos problemas | Desempenho, concorrência, mensageria, arquitetura de um serviço, observabilidade, deploy |
| 🟣 **Sênior** | Sistemas e decisões que afetam o time ou vários times | Define o caminho e orienta pessoas | Sistemas distribuídos, trade-offs, confiabilidade, segurança, custo, liderança técnica |

## Competências por nível (comportamento observável)

| Competência | 🟢 Júnior | 🔵 Pleno | 🟣 Sênior | Módulos |
|---|---|---|---|---|
| **Linguagem Java** | Usa OO, Collections, Streams, exceções e `BigDecimal` corretamente. | Explica heap, GC, JIT; usa `CompletableFuture` e `java.util.concurrent`; reproduz e corrige condição de corrida. | Diagnostica vazamento de memória com heap dump, escolhe GC e justifica virtual threads vs reativo. | J1, P1, S2 |
| **Lógica e estruturas de dados** | Escolhe `List`/`Set`/`Map` e estima Big-O simples. | Mede custo com JMH e explica `HashMap` por dentro. | Modela estruturas para escala (filas, particionamento) e justifica. | J1, P1, S1 |
| **Git e ferramentas** | Branch, PR, resolve conflitos, commits claros. | Define fluxo de branches e revisa PRs com critério. | Estabelece convenções e automação para vários times. | J0, J7, S7 |
| **Web e HTTP** | Explica o ciclo da requisição, status codes e headers. | Depura com `curl`, trace e inspeção de TLS; trata timeouts. | Desenha contratos de API duradouros e políticas de versionamento. | J2, J6, P5 |
| **Banco de dados** | Escreve JOINs, agregações; entende transações e ACID; usa Flyway. | Lê planos de execução, escolhe níveis de isolamento, resolve deadlocks, dimensiona o pool. | Particiona tabelas, migra sem downtime, escolhe banco por caso de uso. | J3, P2, S3 |
| **Spring e persistência (JPA)** | Constrói CRUD com Spring Data JPA, valida entrada, usa profiles. | Evita N+1, entende proxies e `@Transactional`, otimiza consultas. | Define padrões de acesso a dados do time e decide quando abandonar a ORM. | J5, P2 |
| **APIs REST** | Projeta endpoints, DTOs, erros padronizados e documentação OpenAPI. | Garante idempotência, paginação eficiente e contratos testados. | Governa versionamento, compatibilidade e evolução de APIs entre times. | J6, P5, S1 |
| **Testes e qualidade** | Escreve testes de unidade e de fatia; usa Testcontainers. | Equilibra a pirâmide; testes de contrato; TDD quando útil. | Define estratégia de testes e métricas de qualidade para o time. | J7, P5, S7 |
| **Segurança** | Aplica Spring Security, BCrypt, JWT e corrige falhas do OWASP Top 10. | Implementa OAuth2/OIDC, rotação de segredos, STRIDE. | Conduz revisão de segurança, mTLS, KMS, conformidade (LGPD/PCI). | J8, P10, S5 |
| **Front-end** | Lê e ajusta HTML/CSS/JS e Thymeleaf; consome a API com `fetch`. | Colabora com o front com contratos claros. | Avalia trade-offs de renderização e integração front-back. | J4 |
| **Mensageria e assincronia** | Entende fila vs síncrono. | Usa Kafka/RabbitMQ com idempotência, DLQ e Outbox. | Escolhe consistência, ordenação e garantias de entrega por caso de uso; sagas. | P3, S1 |
| **Arquitetura** | Segue a estrutura do projeto. | Aplica hexagonal, SOLID, DDD tático; monólito modular. | Decide limites de serviços, registra ADRs, evita complexidade desnecessária. | P4, P5, S1 |
| **Sistemas distribuídos** | — | Aplica resiliência (timeout, retry, circuit breaker). | Raciocina com CAP/PACELC, consistência eventual, CQRS, event sourcing. | P5, S1 |
| **Observabilidade e operação** | Lê logs e stack traces. | Instrumenta métricas, logs estruturados e traces; investiga incidentes. | Define SLOs, conduz postmortems, planeja capacidade. | P6, S4 |
| **Containers e orquestração** | Containeriza a app com Docker e Compose. | Opera Kubernetes (probes, limites, HPA) e ajusta a JVM no container. | Define padrões de plataforma e estratégias de deploy. | J9, P7, S6 |
| **CI/CD e IaC** | Configura CI com testes por PR. | Pipeline completo, blue/green, canary, Terraform. | Padroniza entrega para vários times e reduz risco de release. | J10, P8 |
| **Nuvem e custo** | Usa EC2, RDS e S3; sabe desligar para não gastar. | Usa ECS/EKS, SQS/SNS, Lambda, VPC. | Aplica Well-Architected, FinOps, multi-AZ e decide lock-in. | J10, P9, S6 |
| **Performance** | Evita consultas óbvias lentas. | Usa profiling básico e lê métricas. | Mede com método (JFR, k6, percentis) e otimiza com evidência. | P2, S2 |
| **Liderança e comunicação** | Pede ajuda, recebe code review. | Revisa código dos colegas e estima tarefas. | Escreve RFCs/design docs, mentora, negocia dívida técnica com o negócio. | J0, J7, S7, S8 |

## Como usar
- No checkpoint de cada módulo, marque ✅ / ⚠️ / ❌ nas linhas relevantes, no nível correspondente.
- Entre no próximo nível quando as linhas do nível atual estiverem em ✅ ou ⚠️ com plano de reforço.
