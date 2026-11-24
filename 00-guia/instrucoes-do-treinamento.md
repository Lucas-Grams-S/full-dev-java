# Prompt: Treinamento Dev Java Web — do Júnior ao Sênior

> Antes de colar, ajuste a seção **0. Parâmetros**. Depois cole tudo abaixo da linha em uma sessão do Claude Code aberta na pasta onde o treinamento deve ficar.

---

Você é um **engenheiro de software principal (staff/principal) especializado em Java e Spring, e instrutor técnico experiente**. Sua tarefa é **criar um treinamento completo em arquivos Markdown (.md)**, nesta pasta, que leve o aluno **do zero até o nível sênior** em desenvolvimento de sistemas web com Java, com base no que o mercado (Brasil e exterior) realmente exige em cada nível.

## 0. Parâmetros (ajuste antes de usar)

- **Pasta do treinamento:** a pasta atual desta sessão.
- **Perfil do aluno:** tem noções iniciais de programação (ou nenhuma) e nunca trabalhou como dev. *(Se o aluno já for júnior ou pleno, ele pode pular os módulos de nível já dominado depois de passar no checkpoint do nível.)*
- **Disponibilidade:** 1h30 por dia, 5 dias por semana.
- **Stack principal:** **Java (LTS mais recente) + Spring Boot (versão estável mais recente)**, Maven como padrão (Gradle mostrado como alternativa).
- **Banco de dados:** **PostgreSQL** (H2 só em exemplos rápidos, nunca como substituto em testes de integração).
- **Cloud:** **AWS como principal**, com uma **tabela de equivalência para Azure e GCP** em cada tema de nuvem.
- **Idioma:** explicações em português do Brasil. Termos técnicos em inglês quando forem o padrão do mercado (com tradução na primeira ocorrência). Código, nomes e commits em inglês.

## 1. Antes de escrever qualquer coisa

1. **Confira as versões atuais** com busca na web: Java LTS, Spring Boot, Spring Framework, Spring Security, Spring Data, Hibernate, PostgreSQL, JUnit, Testcontainers, Docker/Compose, Kubernetes, Kafka, Terraform e os serviços de nuvem. **Não confie na memória para APIs:** o ecossistema Spring muda entre versões maiores (ex.: `javax` → `jakarta`, configuração do Spring Security, `RestTemplate` → `RestClient`). Registre tudo em `00-guia/versoes.md`, com a data da verificação.
2. **Pesquise o que o mercado pede** em cada nível (vagas reais de júnior, pleno e sênior Java, roadmap.sh de Java/Backend/Spring Boot, pesquisas como a JetBrains Developer Ecosystem e a Stack Overflow Developer Survey, e matrizes de carreira públicas de empresas de tecnologia). Use isso para **validar e completar** a trilha da seção 4, e registre em `00-guia/o-que-o-mercado-pede.md`.
3. Crie primeiro o **índice, a matriz de competências, o plano, o cronograma e o histórico** e só depois gere os módulos, **um por execução**, seguindo as seções 9 a 11.

## 2. O que significa cada nível

Escreva em `00-guia/matriz-de-competencias.md` uma matriz detalhada. Use esta base e complete com a pesquisa:

| Nível | Escopo | Autonomia | Foco técnico |
|---|---|---|---|
| **Júnior** | Tarefas bem definidas dentro de um sistema | Precisa de orientação e revisão | Escrever código correto, testado e legível com Java, Spring, SQL e Git |
| **Pleno** | Funcionalidades completas, de ponta a ponta | Resolve sozinho a maioria dos problemas | Desempenho, concorrência, mensageria, arquitetura de um serviço, observabilidade, deploy |
| **Sênior** | Sistemas e decisões que afetam o time ou vários times | Define o caminho e orienta outras pessoas | Sistemas distribuídos, trade-offs, confiabilidade, segurança, custo, liderança técnica |

Para cada competência, descreva **o que se espera em cada nível**, com exemplos de comportamento observável. Exemplo: em "banco de dados", o júnior escreve consultas com JOIN e entende transações; o pleno lê planos de execução e resolve deadlocks; o sênior desenha estratégias de particionamento e migrações sem downtime.

## 3. Princípios pedagógicos (obrigatórios)

1. **Ordem de construção conceitual.** Nunca use um conceito antes de ensiná-lo. Cada aula declara seus pré-requisitos. O caminho vai do concreto ao abstrato e do simples ao distribuído: *programa no terminal → dados em banco → servidor web → API → front-end → testes → segurança → container → nuvem → concorrência → mensageria → arquitetura → sistemas distribuídos → liderança técnica.*
2. **Primeiro a intuição, depois o rigor.** Todo conceito começa com uma **analogia do dia a dia** e só então vêm a definição técnica, os detalhes e o código. Exemplos esperados:
   - Injeção de dependência como um restaurante em que o chef recebe os ingredientes prontos em vez de plantar a horta.
   - Transação ACID como uma transferência bancária: ou o dinheiro sai de uma conta e entra na outra, ou nada acontece.
   - Síncrono como um telefonema; assíncrono como e-mail ou fila com senha.
   - Kafka como um diário que todos podem ler no próprio ritmo; RabbitMQ como uma agência dos correios que entrega e esquece.
   - Circuit breaker como o disjuntor da sua casa.
   - Garbage collector como o serviço de limpeza que passa recolhendo o que ninguém usa mais.
3. **Profundidade proporcional ao nível.** No júnior, explique o *o quê* e o *como*, com o *porquê* essencial. No pleno, explique *como funciona por dentro*. No sênior, explique *trade-offs, limites e quando NÃO usar*. Exemplos de profundidade esperada:
   - **Júnior:** o ciclo de uma requisição (DNS → TCP → TLS → HTTP → Tomcat → `DispatcherServlet` → controller → service → repository → SQL → resposta), o que o Spring Boot configura sozinho, o ciclo de vida de uma entidade JPA.
   - **Pleno:** proxies do Spring e por que `@Transactional` não funciona em chamadas internas, problema N+1, níveis de isolamento e anomalias, modelo de memória do Java e `happens-before`, threads de plataforma vs virtual threads, coletores de lixo (G1, ZGC).
   - **Sênior:** CAP e PACELC, consistência eventual, padrão Outbox, sagas (orquestração vs coreografia), idempotência ponta a ponta, estratégias de cache e invalidação, SLOs e error budgets, custo de cada decisão de arquitetura.
4. **Exemplo prático para CADA conceito.** Nenhum conceito fica só na teoria: cada um tem código executável e curto, com o comando para rodar e a saída esperada.
5. **🧠 Pausas de lógica e algoritmos (nível júnior e pleno).** Insira caixas **"🧠 Pausa para lógica"** que usam um problema real do sistema para treinar lógica, estruturas de dados e algoritmos. Exemplos: arredondamento de dinheiro com `BigDecimal` vs `double`; paginação e "off-by-one"; `HashMap` por dentro (hash, colisões, `equals`/`hashCode`); busca binária vs índice de banco; ordenação com `Comparator`; detectar duplicidade com `Set`; filas e pilhas; Big-O medido com JMH. Cada pausa tem enunciado, dica e solução comentada em `<details>`, e uma variação mais difícil.
6. **🏗️ Estudos de caso (nível pleno e sênior).** Insira caixas **"🏗️ Estudo de caso"** com um problema de design realista (ex.: "o extrato ficou lento com 50 milhões de linhas", "o pagamento foi debitado duas vezes", "o serviço de notificações caiu e derrubou o checkout"). Mostre o diagnóstico, as opções, os trade-offs e a decisão, no formato de um ADR (Architecture Decision Record).
7. **Aprender fazendo.** Toda aula prática termina com um exercício que **evolui o projeto central** (seção 5) e tem critérios de "pronto" verificáveis.
8. **Mentalidade de mercado.** Inclua armadilhas comuns, boas práticas de produção, como o tema aparece no dia a dia de um time e **perguntas de entrevista do nível correspondente** com respostas-modelo.
9. **Texto direto.** Denso em conteúdo, sem frases motivacionais vazias. Use Mermaid para diagramas, tabelas para comparações e `<details>` para soluções.

## 4. Trilha (ordem de importância e de construção)

Use esta trilha como base. **Complete o que faltar** com a pesquisa de mercado (etapa 1.2), mantendo a ordem de construção. Distribua as horas de forma realista e justifique no plano.

---

### 🟢 NÍVEL 1 — JÚNIOR

#### Módulo J0 — Ambiente e ferramentas do dev
Terminal (Linux/WSL), sistema de arquivos e permissões; IntelliJ IDEA (atalhos, debugger, refatorações); instalação do JDK (SDKMAN); **Maven** (ciclo de vida, dependências, plugins) e visão de Gradle; **Git e GitHub** (commit, branch, merge, rebase, pull request, resolução de conflitos, `.gitignore`); como ler stack traces, documentação e pedir ajuda.

#### Módulo J1 — Java e lógica de programação
Tipos primitivos e por referência, operadores, condicionais, laços, métodos; arrays e Strings (imutabilidade, `StringBuilder`); **orientação a objetos** (classes, encapsulamento, herança, polimorfismo, interfaces, classes abstratas, composição vs herança); `equals`/`hashCode`/`toString`; exceções (checked vs unchecked, try-with-resources); **Collections** (`List`, `Set`, `Map`, `Queue` e quando usar cada uma); **generics**; **lambdas, Streams e `Optional`**; records, enums, sealed classes e pattern matching; `java.time`; `BigDecimal` para dinheiro; leitura e escrita de arquivos; pacotes e modificadores de acesso; noções de Big-O; **JUnit 5 desde aqui**. Pausas de lógica progressivas em todo o módulo.

#### Módulo J2 — Como a web funciona
Cliente-servidor, IP, DNS, portas, TCP, HTTP/HTTPS e TLS; métodos, status codes, headers, corpo; cookies e sessões; JSON; ferramentas (DevTools, `curl`, Postman/Bruno/HTTPie). Construir um **servidor HTTP mínimo com `com.sun.net.httpserver`** e chamar APIs com `java.net.http.HttpClient`, para ver o que o framework esconde.

#### Módulo J3 — Bancos de dados relacionais
Modelagem (entidades, relacionamentos 1:1, 1:N, N:N, diagrama ER); chaves; **normalização** (1FN a 3FN) e quando desnormalizar; **SQL** completo para júnior (DDL, DML, JOINs, agregações, subqueries, `ORDER BY`/`LIMIT`, CTEs); constraints; **índices** e `EXPLAIN` básico; **transações e ACID**; PostgreSQL na prática (com Docker); **JDBC puro** e **SQL injection** com `PreparedStatement`; migrações com **Flyway**.

#### Módulo J4 — Front-end essencial para dev backend
HTML semântico, formulários, CSS (box model, flexbox, grid, responsividade), JavaScript essencial (DOM, eventos, `fetch`, Promises, `async`/`await`); consumir a API do projeto; renderização no servidor com **Thymeleaf**; visão geral de **Angular e React** (muito comuns em empresas Java) e de TypeScript, no nível de saber conversar e ler código simples; acessibilidade básica.

#### Módulo J5 — Spring Framework e Spring Boot
**Inversão de controle e injeção de dependência** (com um mini-container feito à mão antes do Spring); beans, escopos e ciclo de vida; estereótipos (`@Component`, `@Service`, `@Repository`, `@Controller`); como funciona a **autoconfiguração** do Spring Boot e os starters; `application.yml`, profiles e `@ConfigurationProperties`; **Spring MVC** e o padrão **MVC**; **Spring Data JPA e Hibernate** (entidades, relacionamentos, lazy vs eager, ciclo de vida do `EntityManager`, `@Transactional`, consultas derivadas, JPQL, paginação, problema N+1); **Bean Validation**; logging com SLF4J/Logback; Spring Boot DevTools e Actuator (básico).

#### Módulo J6 — APIs REST
Princípios REST (recursos, verbos, status codes, idempotência, statelessness); design de endpoints e convenções; **DTOs vs entidades** e mapeamento (manual e MapStruct); tratamento global de erros com `@RestControllerAdvice` e **Problem Details (RFC 9457)**; paginação, filtros e ordenação; versionamento; **OpenAPI/Swagger com springdoc**; CORS; consumir APIs externas com `RestClient` (timeouts!); HATEOAS (só conceito).

#### Módulo J7 — Testes e qualidade
Pirâmide de testes; **JUnit 5, AssertJ e Mockito**; testes de unidade de services; testes de fatia (`@WebMvcTest`, `@DataJpaTest`) com **MockMvc**; `@SpringBootTest`; **Testcontainers** com PostgreSQL real; cobertura com JaCoCo; TDD (introdução); Clean Code e code smells; Checkstyle/SpotBugs/SonarLint; como dar e receber code review; Conventional Commits.

#### Módulo J8 — Segurança essencial
Autenticação vs autorização; **Spring Security** (cadeia de filtros, `SecurityFilterChain`, `UserDetailsService`, `PasswordEncoder` com BCrypt/Argon2); sessão vs **JWT** (riscos de cada um); autorização por papéis e por método (`@PreAuthorize`); **OWASP Top 10** com o código vulnerável e o corrigido lado a lado (SQL injection, XSS, CSRF, IDOR, falhas de autenticação); segredos fora do Git; validação de entrada; dependências vulneráveis (OWASP Dependency-Check); LGPD básica para devs.

#### Módulo J9 — Docker essencial
O problema que o Docker resolve; imagem vs container; **Dockerfile** para uma app Spring Boot (multi-stage, imagem enxuta, usuário não root); **Docker Compose** com app + PostgreSQL; volumes, redes e variáveis de ambiente; logs e depuração; `.dockerignore`.

#### Módulo J10 — CI e introdução à nuvem
**GitHub Actions** rodando build, testes e análise a cada pull request; conceitos de nuvem (IaaS, PaaS, SaaS, regiões, zonas, responsabilidade compartilhada); **alerta de custos e MFA antes de tudo**; IAM; EC2, RDS, S3; Security Groups; **deploy do projeto** de forma simples e barata; **como destruir tudo para não gerar cobrança**. Tabela de equivalência AWS ↔ Azure ↔ GCP.

#### ✅ Checkpoint de nível Júnior
Projeto prático avaliado por checklist, quiz por módulo, simulado de entrevista júnior e um **desafio técnico (take-home) com tempo limite**.

---

### 🔵 NÍVEL 2 — PLENO

#### Módulo P1 — Java a fundo
**JVM por dentro**: classloading, áreas de memória (heap, stack, metaspace), JIT; **garbage collectors** (G1, ZGC, Parallel) e como ler logs de GC; **concorrência**: `Thread`, `ExecutorService`, `CompletableFuture`, `java.util.concurrent` (locks, atômicos, coleções concorrentes), **Java Memory Model** e `volatile`, condições de corrida e deadlocks com exemplos que falham de verdade; **virtual threads** e structured concurrency (quando ajudam e quando não); recursos modernos da linguagem desde Java 8 até a LTS atual; Java Modules (visão geral).

#### Módulo P2 — Banco de dados e persistência avançados
**Níveis de isolamento** e anomalias (dirty read, non-repeatable read, phantom, lost update) com experimentos em duas sessões; **locking otimista (`@Version`) e pessimista**; deadlocks; leitura de **planos de execução** e estratégia de índices (compostos, parciais, cobertura); pool de conexões (HikariCP) e dimensionamento; **performance de JPA** (fetch joins, entity graphs, projeções, batch, cache de segundo nível) e quando usar SQL nativo, jOOQ ou JdbcClient; **Redis** para cache (`@Cacheable`, TTL, invalidação, cache stampede); visão de NoSQL (MongoDB, DynamoDB) e quando faz sentido.

#### Módulo P3 — Comunicação síncrona e assíncrona
Bloqueante vs não bloqueante; I/O-bound vs CPU-bound; `@Async` e agendamento (`@Scheduled`, ShedLock); **mensageria**: filas vs tópicos, **RabbitMQ** e **Apache Kafka** (partições, consumer groups, offsets, ordenação, retenção) com Spring AMQP e Spring for Apache Kafka; garantias de entrega (at-most-once, at-least-once, "exactly-once"); **idempotência de consumidores**, retentativas com backoff e **dead-letter queues**; **padrão Outbox**; **webhooks**; WebSockets/SSE; visão de programação reativa (**WebFlux**, Reactor) e comparação com virtual threads.

#### Módulo P4 — Arquitetura de aplicações
Separação de responsabilidades; **MVC** vs **camadas** vs **arquitetura hexagonal (ports and adapters)** vs **Clean Architecture**; **SOLID** com exemplos em Spring; **padrões de projeto GoF** mais usados no dia a dia (Strategy, Factory, Builder, Adapter, Decorator, Observer, Template Method) e onde o próprio Spring os usa; **DDD tático** (entidades, value objects, agregados, repositórios, eventos de domínio) e estratégico (bounded contexts, linguagem ubíqua); **monólito modular** com Spring Modulith; ArchUnit para testar regras de arquitetura.

#### Módulo P5 — Microsserviços e integração
**Monólito vs microsserviços** (quando dividir e quando NÃO dividir); decomposição por domínio; comunicação síncrona (REST, **gRPC**) vs assíncrona (eventos); API Gateway (Spring Cloud Gateway); configuração centralizada e descoberta de serviços (e por que o Kubernetes muitas vezes substitui isso); **resiliência com Resilience4j** (timeouts, retry, circuit breaker, bulkhead, rate limiter); **testes de contrato** (Spring Cloud Contract ou Pact); versionamento e compatibilidade de APIs e de eventos.

#### Módulo P6 — Observabilidade
Os três pilares (logs, métricas, traces); **logs estruturados** e correlation ID; **Spring Boot Actuator e Micrometer**; **Prometheus e Grafana** (métricas RED/USE, dashboards, alertas); **tracing distribuído com OpenTelemetry**; health checks (liveness vs readiness); monitoramento de erros.

#### Módulo P7 — Containers e Kubernetes
Imagens otimizadas para Java (layered jars, Buildpacks, Jib) e a JVM em container (limites de memória e CPU); **Kubernetes** (pods, deployments, services, ingress, configmaps, secrets, probes, requests/limits, HPA); deploy do projeto em um cluster local (kind/minikube); Helm (visão geral).

#### Módulo P8 — CI/CD e infraestrutura como código
Pipeline completo (build, testes, análise estática, scan de segurança, imagem, deploy); estratégias de deploy (rolling, **blue/green**, **canary**); feature flags; **Terraform** (providers, state, módulos) para criar a infraestrutura do projeto; GitOps (visão geral).

#### Módulo P9 — Nuvem intermediária
Redes (VPC, sub-redes públicas e privadas, NAT, load balancers); containers gerenciados (ECS/Fargate e EKS); mensageria gerenciada (**SQS, SNS, EventBridge**); **serverless** com Lambda (e cold start em Java, SnapStart); segredos (Secrets Manager/Parameter Store); CloudWatch; bancos gerenciados e réplicas de leitura. Tabela de equivalência AWS ↔ Azure ↔ GCP em cada tema.

#### Módulo P10 — Segurança intermediária
**OAuth 2.0 e OpenID Connect** a fundo (fluxos, tokens, escopos); Spring Security como **Resource Server** e integração com **Keycloak** ou Spring Authorization Server; autenticação entre serviços; gestão e rotação de segredos; segurança da cadeia de suprimentos (SBOM, scan de dependências e imagens); introdução a **threat modeling** (STRIDE).

#### ✅ Checkpoint de nível Pleno
Evolução do projeto avaliada por checklist (incluindo um incidente simulado para investigar com logs, métricas e traces), quiz por módulo e simulado de entrevista pleno (técnica + design de um serviço).

---

### 🟣 NÍVEL 3 — SÊNIOR

#### Módulo S1 — Sistemas distribuídos e system design
Escalabilidade vertical vs horizontal; **CAP e PACELC**; modelos de consistência; replicação e particionamento (sharding); balanceamento de carga; **estratégias de cache** (cache-aside, write-through, write-behind) e CDN; **CQRS** e **event sourcing**; **sagas** e transações distribuídas (por que evitar 2PC); idempotência ponta a ponta; rate limiting distribuído; relógios e ordenação de eventos; **método para entrevistas de system design** com exercícios resolvidos (encurtador de URL, sistema de pagamentos, feed de notícias, sistema de notificações).

#### Módulo S2 — Performance e eficiência
Metodologia (medir antes de otimizar); **profiling** com Java Flight Recorder, JDK Mission Control e async-profiler; análise de heap dumps e memory leaks; tuning de GC; **testes de carga** (Gatling ou k6) e leitura de percentis (p50, p95, p99); **GraalVM native image** e CRaC (quando valem a pena); benchmarks com JMH.

#### Módulo S3 — Dados em escala
**Migrações sem downtime** (expand/contract); particionamento de tabelas no PostgreSQL; réplicas de leitura e roteamento; arquivamento de dados; busca com **OpenSearch/Elasticsearch**; **CDC com Debezium**; escolha de banco por caso de uso (relacional, documento, chave-valor, colunar, séries temporais); noções de dados para analytics.

#### Módulo S4 — Confiabilidade e operação (SRE)
**SLI, SLO e error budgets**; gestão de incidentes e **postmortems sem culpa**; runbooks; engenharia do caos; backup, recuperação de desastres (RPO/RTO) e arquitetura multi-AZ e multi-região; capacity planning.

#### Módulo S5 — Segurança avançada
Defesa em profundidade e zero trust; **mTLS** entre serviços; criptografia em trânsito e em repouso, gestão de chaves (KMS); segurança no pipeline (SAST, DAST, SCA); conformidade (**LGPD**, noções de **PCI-DSS** para sistemas de pagamento); auditoria e trilhas de acesso; como conduzir uma revisão de segurança.

#### Módulo S6 — Arquitetura em nuvem e custo
**Well-Architected Framework** (AWS, com equivalentes no Azure e no GCP) e seus pilares; escolha entre VMs, containers e serverless; multi-cloud e lock-in (trade-offs reais); **FinOps** (estimativa e otimização de custos, tags, reservas, instâncias spot); landing zones e contas/projetos separados por ambiente.

#### Módulo S7 — Liderança técnica
**ADRs e RFCs**; conduzir e escrever design docs; estimativas e gestão de riscos técnicos; **dívida técnica** (como medir, priorizar e negociar); **modernização de legado** (strangler fig, branch by abstraction); cultura de code review e de testes; **mentoria** de júniores e plenos; comunicação de trade-offs para produto e negócio; trabalho entre vários times; uso responsável de IA como ferramenta de desenvolvimento (e visão de Spring AI para integrar LLMs em sistemas).

#### Módulo S8 — Carreira
Portfólio no GitHub (README em português e inglês, diagramas, ADRs, como rodar); currículo e LinkedIn por nível; **entrevistas por nível**: técnica, live coding, system design e comportamental (método **STAR**); como demonstrar senioridade em entrevista; negociação; caminhos depois do sênior (staff/principal vs gestão).

#### ✅ Checkpoint de nível Sênior
Design doc completo de uma evolução grande do projeto (com ADRs, diagramas, SLOs, estimativa de custo e plano de migração), postmortem de um incidente simulado e simulado de entrevista de system design.

---

## 5. Projeto central (fio condutor)

Todo o treinamento constrói e evolui **um único sistema**: uma **carteira digital simplificada ("PayFlow")**. O domínio de pagamentos foi escolhido porque obriga o aluno a lidar com dinheiro, transações, concorrência, idempotência, segurança e consistência, que são temas centrais em vagas Java.

- **Júnior:** monólito Spring Boot com cadastro e login, contas, depósitos, **transferências entre contas (transação ACID)**, extrato paginado, painel web simples, API REST documentada, testes com Testcontainers, Docker Compose, CI e deploy simples na nuvem.
- **Pleno:** controle de concorrência nas transferências (locking), cache do saldo e do extrato, **notificações e antifraude assíncronos via mensageria com Outbox**, pagamento por QR code simulado com **webhook**, arquitetura hexagonal e monólito modular, observabilidade completa, Kubernetes, Terraform e OAuth2/Keycloak.
- **Sênior:** extração justificada de serviços (ex.: notificações, antifraude, relatórios), **saga** para pagamentos entre serviços, CQRS para o extrato, teste de carga e otimização, SLOs, migração sem downtime, revisão de segurança e estimativa de custo.

Regras:
- Defina o domínio, as entidades e os nomes **uma única vez** em `00-guia/projeto-central.md` (diagrama ER e diagrama de arquitetura de cada nível, em Mermaid) e mantenha-os em todas as aulas.
- Em `projeto/marcos.md`, descreva o que o projeto deve ter ao fim de cada módulo (marco = tag no Git).
- Os primeiros módulos (J1 a J4) podem ter mini-projetos próprios que depois são reaproveitados no PayFlow.
- Inclua **dados de exemplo** (seed) e scripts para gerar volume (milhões de linhas) nos módulos de performance.

## 6. Template obrigatório de cada aula

```markdown
# <Código do módulo>.<Número> — <Título>

> ⏱️ ~<tempo> · 🟢 Júnior | 🔵 Pleno | 🟣 Sênior · Pré-requisitos: <aulas>

## 🎯 Objetivos
Ao final desta aula você consegue: (3 a 5 verbos de ação mensuráveis)

## 🤔 Por que isso importa
Que problema real isso resolve e como aparece no trabalho do nível correspondente.

## 💡 Intuição (explicação simples)
Analogia do cotidiano. Sem jargão.

## 🔬 Como funciona por dentro
Explicação técnica precisa, com profundidade proporcional ao nível.
Diagrama Mermaid quando ajudar (sequência, arquitetura, estados).

## 🛠️ Exemplo prático
Código completo e comentado para cada conceito introduzido,
com dependências, configuração, comando para rodar e saída esperada.

## 🧠 Pausa para lógica  /  🏗️ Estudo de caso   (quando fizer sentido)
Problema tirado do contexto da aula. Dica e solução em <details>.

## ⚖️ Trade-offs e quando NÃO usar   (obrigatório no pleno e no sênior)

## ⚠️ Erros comuns e boas práticas de produção
Inclua mensagens de erro e stack traces reais que o aluno vai encontrar.

## 🧩 Exercícios
1. Fixação
2. Aplicação no PayFlow, com critérios de pronto em checklist
3. Desafio (opcional)
Soluções de referência em <details>, fechadas por padrão.

## 🎤 Na entrevista
2 a 4 perguntas do nível correspondente, com respostas-modelo.

## 📚 Para ir além
- 📄 Documentação oficial: ...
- 🎥 Vídeo (PT-BR e/ou EN): ... (canal, duração aproximada)
- 📑 Artigo, livro ou paper: ...
- 💻 Repositório/exemplo: ...

## ✅ Checklist da aula
- [ ] ...
```

## 7. Estrutura de pastas e arquivos

```
./
├── README.md                          # Visão geral, como usar, mapa da trilha (Mermaid)
├── 00-guia/
│   ├── instrucoes-do-treinamento.md   # Cópia deste prompt: as regras para todas as execuções
│   ├── progresso.md                   # Histórico do que já foi implementado (seção 10)
│   ├── cronograma.md                  # Sessões dia a dia com checkbox [ ], semanas, marcos e checkpoints
│   ├── matriz-de-competencias.md
│   ├── o-que-o-mercado-pede.md
│   ├── setup-ambiente.md              # Instalação de tudo, por sistema operacional
│   ├── versoes.md
│   ├── projeto-central.md
│   ├── como-estudar.md                # Método: ler, rodar, quebrar, consertar, explicar
│   └── glossario.md
├── 1-junior/
│   ├── J0-ambiente-ferramentas/
│   ├── J1-java-logica/
│   ├── J2-como-a-web-funciona/
│   ├── J3-banco-de-dados/
│   ├── J4-front-end-essencial/
│   ├── J5-spring-boot/
│   ├── J6-apis-rest/
│   ├── J7-testes-qualidade/
│   ├── J8-seguranca-essencial/
│   ├── J9-docker/
│   ├── J10-ci-nuvem-intro/
│   └── checkpoint-junior.md
├── 2-pleno/
│   ├── P1-java-a-fundo/
│   ├── ... (P2 a P10)
│   └── checkpoint-pleno.md
├── 3-senior/
│   ├── S1-sistemas-distribuidos/
│   ├── ... (S2 a S8)
│   └── checkpoint-senior.md
├── recursos/
│   ├── biblioteca.md                  # Todos os links por tema e nível, com data da verificação
│   ├── livros.md                      # Livros clássicos por nível, com o porquê de cada um
│   ├── perguntas-entrevista.md        # Banco consolidado por nível, com respostas-modelo
│   ├── exercicios-logica.md           # Todas as pausas de lógica + lista extra por dificuldade
│   ├── estudos-de-caso.md             # Todos os estudos de caso e ADRs
│   └── cheatsheets/                   # Git, SQL, JPA, Spring annotations, HTTP, Docker, kubectl, Kafka, AWS CLI
└── projeto/
    ├── marcos.md
    └── adrs/                          # ADRs do PayFlow, criados ao longo do treinamento
```

Cada pasta de módulo contém um `README.md` (objetivos, aulas com tempo, entrega do projeto, conexão com o próximo módulo), as aulas numeradas e um `checkpoint.md` (quiz de 10 questões com gabarito em `<details>`, entrega prática e autoavaliação baseada na matriz de competências).

## 8. Regras para links e referências (muito importante)

- **Nunca invente URLs.** Todo link precisa ser verificado na web antes de entrar no material. Se não der para confirmar, não inclua (ou marque como `⚠️ não verificado`).
- Priorize **fontes primárias e reconhecidas**: docs.oracle.com/javase e dev.java, openjdk.org (JEPs), docs.spring.io, spring.io/guides, hibernate.org, postgresql.org/docs, developer.mozilla.org (MDN), docs.docker.com, kubernetes.io, kafka.apache.org, owasp.org, docs.aws.amazon.com, learn.microsoft.com/azure, cloud.google.com/docs, developer.hashicorp.com/terraform, martinfowler.com, baeldung.com, roadmap.sh.
- **Livros de referência** (cite capítulos relevantes quando fizer sentido): *Effective Java*, *Clean Code*, *Refactoring*, *Domain-Driven Design* (e *Implementing DDD*), *Designing Data-Intensive Applications*, *Building Microservices*, *Release It!*, *Java Concurrency in Practice*, *Site Reliability Engineering* (Google, gratuito online), *Fundamentals of Software Architecture*.
- **Vídeos:** prefira canais e palestras reconhecidos, em PT-BR e EN (ex.: Spring I/O, SpringDeveloper, Devoxx, Java oficial, Dan Vega, Josh Long, Marco Behler, Amigoscode, Michelli Brito, Giuliana Bezerra, Rocketseat, AWS Events, ByteByteGo para system design). Informe o canal e a duração aproximada.
- Em cada link, escreva **uma linha dizendo por que vale a pena** e o que olhar nele.
- Consolide tudo em `recursos/biblioteca.md` e `recursos/livros.md`.

## 9. Forma de trabalho: UM MÓDULO POR VEZ

O treinamento é grande demais para ser gerado de uma vez sem perda de qualidade. Por isso, **cada execução gera no máximo UM módulo** e depois para. Isso mantém o contexto pequeno, reduz erros e invenções, e permite revisar cada parte antes de seguir.

### Etapa inicial (só na primeira execução)

1. **Salve este prompt inteiro** em `00-guia/instrucoes-do-treinamento.md`, sem alterações. Ele é a fonte oficial das regras para todas as execuções seguintes, inclusive em novas sessões.
2. Faça a pesquisa de versões e de mercado (seção 1).
3. Crie `README.md`, a pasta `00-guia/` completa (incluindo a matriz de competências), os `README.md` de todos os módulos dos três níveis (só a lista de aulas e horas, **sem o conteúdo das aulas**) e o arquivo de histórico `00-guia/progresso.md` (seção 10), com todos os módulos como `⬜ pendente`.
4. **Pare e me mostre** o índice geral, a carga horária por nível e total, o cronograma e o que você acrescentou à trilha com base na pesquisa de mercado. **Não gere nenhum módulo nesta execução.** Aguarde minha aprovação.

### A cada execução seguinte (quando eu disser "próximo módulo" ou "continue")

1. **Recupere o contexto pelos arquivos, não pela memória.** Leia, nesta ordem:
   - `00-guia/instrucoes-do-treinamento.md` (as regras);
   - `00-guia/progresso.md` (o que já foi feito, as decisões e qual é o próximo módulo);
   - `00-guia/versoes.md`, `00-guia/projeto-central.md` e `00-guia/glossario.md`;
   - `projeto/marcos.md` e o `README.md` do módulo que será gerado;
   - o `README.md` e o `checkpoint.md` do **módulo anterior**, para continuar exatamente de onde ele parou.
2. **Gere apenas o próximo módulo pendente** da lista, todas as aulas e o `checkpoint.md` dele. Não volte para reescrever módulos concluídos e não adiante conteúdo de módulos futuros. Se perceber um erro em um módulo anterior, **não corrija por conta própria**: registre em "Correções pendentes" no `progresso.md` e me avise.
3. **Verifique antes de entregar** (seção 11).
4. **Atualize o histórico**: o registro do módulo em `00-guia/progresso.md` e também `recursos/`, `glossario.md`, `projeto/marcos.md` e os ADRs.
5. **Pare** e me dê um resumo curto: aulas criadas, entrega do projeto, o que foi testado e o que não pôde ser, links marcados como não verificados e qual é o próximo módulo. **Não comece o próximo módulo sem eu pedir.**
6. Ao terminar o último módulo de um nível, a execução seguinte é só a **revisão do nível**: links quebrados, ordem de pré-requisitos (nenhum conceito usado antes de ser ensinado), consistência de nomes, soma das horas e uma tabela **"competência da matriz → aula(s) que cobrem"**. Registre o resultado no `progresso.md`.

### Para continuar em uma nova sessão

Basta abrir o Claude Code nesta pasta e dizer:

> Leia `00-guia/instrucoes-do-treinamento.md` e `00-guia/progresso.md` e gere o próximo módulo pendente, seguindo as regras.

## 10. Histórico do que já foi implementado (`00-guia/progresso.md`)

Este arquivo é a **memória do treinamento**. Ele precisa permitir que qualquer nova sessão continue o trabalho sem depender da conversa anterior. Mantenha-o sempre atualizado, com esta estrutura:

```markdown
# Progresso do treinamento

## Situação atual
- Último módulo concluído: J3 — Bancos de dados relacionais
- Próximo módulo: J4 — Front-end essencial
- Estado do PayFlow ao fim do último módulo: <o que o projeto já tem, em 3 a 5 linhas>

## Painel de módulos
| Nível | Módulo | Status | Aulas | Horas | Data |
|---|---|---|---|---|---|
| 🟢 | J0 — Ambiente e ferramentas | ✅ concluído | 4 | 6h | 2026-11-12 |
| 🟢 | J4 — Front-end essencial | ⬜ pendente | — | 8h (previsto) | — |
(status possíveis: ⬜ pendente · 🔄 em andamento · ✅ concluído · 🔁 revisado)

## Decisões que valem para todo o treinamento
Lista numerada de decisões já tomadas, para nunca serem contraditas depois.
Ex.: "1. Pacote base do PayFlow: `com.payflow`." · "2. Dinheiro sempre em `BigDecimal`
com 2 casas e `RoundingMode.HALF_EVEN`." · "3. Erros da API seguem Problem Details."

## Registro por módulo
### J3 — Bancos de dados relacionais (✅ 2026-11-14)
- Arquivos criados: lista com caminhos
- Conceitos ensinados: lista curta (para saber o que já pode ser usado como pré-requisito)
- Entrega no PayFlow: o que foi adicionado (tabelas, classes, endpoints, tag no Git)
- Código testado: o que rodou com sucesso e com quais versões
- Não testado / limitações: ...
- Links marcados como ⚠️ não verificados: ...

## Correções pendentes
Problemas encontrados em módulos já concluídos, aguardando minha decisão.
```

- O registro de cada módulo é **acrescentado**, nunca apagado ou reescrito.
- A lista **"Conceitos ensinados"** é a referência para os pré-requisitos: uma aula nova só pode usar conceitos que já aparecem ali (ou que ela mesma ensina).
- Quando um módulo for interrompido no meio, marque-o como `🔄 em andamento` e liste as aulas já prontas, para a próxima execução continuar dele.

## 11. Regras contra alucinação (verificar antes de entregar)

- **Código:** compile e rode os exemplos e as soluções que não dependem de conta na nuvem (use Docker para PostgreSQL, Redis, RabbitMQ, Kafka e Keycloak, e kind/minikube para Kubernetes). Registre no `progresso.md` o que rodou e o que não pôde ser testado, e deixe isso explícito na aula.
- **APIs e configurações:** antes de escrever sobre uma anotação, classe, propriedade do `application.yml` ou comando, confira na **documentação oficial da versão registrada em `versoes.md`**. Na dúvida, não afirme: pesquise ou diga que não tem certeza.
- **Links:** só entram links abertos e conferidos nesta execução (seção 8). Nada de URLs montadas "de cabeça".
- **Números e fatos:** não invente estatísticas, benchmarks, salários ou citações. Todo número precisa ter fonte com link, ou ser medido no próprio exemplo.
- **Consistência:** antes de criar uma classe, tabela, endpoint ou termo, procure nos arquivos existentes (por exemplo, com `grep`) se ele já foi definido, e reutilize o mesmo nome. Siga as "Decisões que valem para todo o treinamento".
- **Nuvem:** em todo passo que pode gerar custo, coloque um aviso **💸** e a forma de desfazer.
