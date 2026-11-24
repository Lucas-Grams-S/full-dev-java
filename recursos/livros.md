# Livros de referência

> Lista inicial (do prompt). O "porquê" de cada um e os capítulos relevantes são preenchidos nos módulos que os usam. Sem links aqui; só títulos.

| Livro | Nível | Quando usar |
|---|---|---|
| *Effective Java* | 🟢→🟣 | J1 em diante |
| *Clean Code* | 🟢 | J7 |
| *Refactoring* | 🟢→🔵 | J7, P4 |
| *Java Concurrency in Practice* | 🔵 | P1 |
| *Domain-Driven Design* (e *Implementing Domain-Driven Design*) | 🔵 | P4 |
| *Designing Data-Intensive Applications* | 🔵→🟣 | P2, S1, S3 |
| *Building Microservices* | 🔵 | P5 |
| *Release It!* | 🔵→🟣 | P5, S4 |
| *Fundamentals of Software Architecture* | 🔵→🟣 | P4, S1, S7 |
| *Site Reliability Engineering* (Google, gratuito online) | 🟣 | S4 |

## Capítulos usados no módulo J1
- *Effective Java* (Bloch): minimizar acessibilidade e mutabilidade (J1.05), composição vs herança (J1.06/J1.07), `equals`/`hashCode`/`toString` (J1.08), exceções (J1.09), coleções, generics e PECS (J1.12), lambdas e streams (J1.13), `Optional` e `enum` (J1.14). Capítulos e itens citados por tema, sem numeração exata (não conferida nesta execução).
- *Clean Code* (Martin): capítulo sobre testes (J1.16).

## Capítulos usados no módulo J2
- *Redes de Computadores e a Internet* (Kurose & Ross; título original *Computer Networking: A Top-Down Approach*): camadas de aplicação e transporte; DNS, TCP e HTTP (J2.01, J2.02). Citação de capítulos específicos não verificada: consulte o sumário da sua edição.

## Capítulos e livros usados no módulo J3
- *Designing Data-Intensive Applications* (Kleppmann): capítulos sobre modelos de dados, armazenamento e índices (J3.01, J3.02, J3.09) e sobre transações (J3.10). A numeração dos capítulos varia entre edições e **não foi conferida**: procure pelos temas no sumário da sua edição.
- *Use The Index, Luke* (Markus Winand; gratuito online): indexação para desenvolvedores (J3.09).
- *Refactoring Databases* (Ambler & Sadalage): refatoração e migração gradual de esquemas (J3.12). Título citado de memória; capítulos não conferidos.
- *SQL Antipatterns* (Karwin) e *Learning SQL* (Beaulieu): leituras complementares de SQL. Citados de memória, sem verificação de capítulos; úteis como segunda fonte para as aulas J3.05 a J3.08.

## Livros e leituras usados no módulo J4
- *Eloquent JavaScript* (Marijn Haverbeke; gratuito online em eloquentjavascript.net): leitura de apoio para JavaScript (J4.04, J4.05). Citado pelo título e site; capítulos **não conferidos**.
- Cursos gratuitos da [web.dev](https://web.dev/learn/html) (HTML, CSS) e da [MDN](https://developer.mozilla.org/en-US/docs/Learn_web_development): trilhas oficiais de front-end usadas ao longo de J4.

## Livros e leituras usados no módulo J5
- *Spring in Action* (Craig Walls): introdução ampla ao Spring (contêiner, MVC, dados). Citado pelo título; edição e capítulos **não conferidos**.
- *High-Performance Java Persistence* (Vlad Mihalcea): referência sobre JPA/Hibernate (mapeamento, *fetching*, N+1, transações, locking), útil para J5.07 a J5.12 e P2. Citado de memória; capítulos não conferidos.
- *Effective Java* (Joshua Bloch): itens sobre injeção de dependência, imutabilidade e exceções, que aparecem na J5.01 e J5.11. Já listado no módulo J1; capítulos não conferidos.
- *The Twelve-Factor App* (12factor.net, gratuito): o fator "Config" é a base da J5.05 (verificado: 12factor.net/config, HTTP 200).
- *REST API Design Rulebook* (Mark Massé) e *Designing Web APIs* (Brenda Jin et al.): desenho de APIs. Citados de memória; capítulos não conferidos.
- *Release It!* (Michael Nygard): timeouts, *circuit breakers* e integrações (base da J6.08). Já listado; capítulos não conferidos.

## Livros e leituras usados no módulo J7
- *Clean Code* (Robert C. Martin): nomes, funções pequenas, comentários; as regras são heurísticas (J7.07). Citado pelo título; capítulos **não conferidos**.
- *Refactoring* (Martin Fowler): o catálogo de refatorações (também online em refactoring.com/catalog, HTTP 200 em 2026-11-13) (J7.07).
- *Working Effectively with Legacy Code* (Michael Feathers): testes de caracterização e como mexer em código sem testes (J7.07). Citado pelo título; capítulos **não conferidos**.
- *Growing Object-Oriented Software, Guided by Tests* (Freeman & Pryce) e *Test-Driven Development: By Example* (Kent Beck): referências de TDD e de dublês (J7.02, J7.06). Citados de memória; capítulos **não conferidos**.
- *Unit Testing: Principles, Practices, and Patterns* (Vladimir Khorikov): quando usar mocks e o que testar. Citado de memória; capítulos **não conferidos**.
- *Software Engineering at Google* (livro gratuito online): capítulos sobre testes e code review; leitura de apoio (J7.09). Citado pelo título; **link e capítulos não verificados** nesta execução.

## Livros e leituras usados no módulo J8
- *Spring Security in Action* (Laurențiu Spilcă): arquitetura, autenticação, autorização, JWT e OAuth2 em Spring. Citado de memória; capítulos **não conferidos**; a 2ª edição pode ter sido atualizada para versões mais novas do Spring Security: confira.
- *Alice and Bob Learn Application Security* (Tanya Janca): visão prática de segurança de aplicações para quem desenvolve. Citado de memória; capítulos **não conferidos**.
- *Web Security for Developers* (Malcolm McDonald): XSS, CSRF, injeção e cabeçalhos explicados com exemplos. Citado de memória; capítulos **não conferidos**.
- **Leituras gratuitas e verificadas:** OWASP Top 10:2025 (top10.owasp.org/2025), as *cheat sheets* da OWASP e o ASVS (HTTP 200 em 2026-11-13), o NIST SP 800-63B-4 (pages.nist.gov/800-63-4/sp800-63b.html, HTTP 200) e o *Web Security Academy* da PortSwigger.

## Livros e leituras usados no módulo J9
- *Docker Deep Dive* (Nigel Poulton): imagens, camadas, redes, volumes, Compose e segurança, em linguagem direta. Citado de memória; capítulos **não conferidos**; edições novas cobrem o BuildKit e o Compose v2.
- *Docker in Practice* (Ian Miell e Aidan Hobson Sayers): receitas de uso real e depuração. Citado de memória; capítulos **não conferidos**.
- *Release It!* (Michael Nygard): desligamento gracioso, *health checks* e comportamento sob falha, o que a J9.02 e a J9.03 aplicam em pequeno. Capítulos **não conferidos**.
- **Leituras gratuitas e verificadas:** a documentação do Docker (docs.docker.com: Dockerfile, build, Compose, logs), o guia de imagens eficientes do Spring Boot (docs.spring.io), o *Twelve-Factor App* (12factor.net) e as páginas do manual do Linux `namespaces(7)` e `cgroups(7)` (HTTP 200 em 2026-11-13).
- *Continuous Delivery* (Jez Humble e David Farley): a origem do pipeline como ideia; J10.01 e J10.06 aplicam em pequeno. Citado de memória; capítulos **não conferidos**.
- *Cloud FinOps* (J. R. Storment e Mike Fuller): custo de nuvem como disciplina; J10.03 e J10.07. Citado de memória; capítulos **não conferidos**.
- **Leituras gratuitas e verificadas:** a documentação de segurança do GitHub Actions (docs.github.com) e o modelo de responsabilidade compartilhada da AWS (aws.amazon.com).

## Livros e leituras usados no módulo P1
- *Java Concurrency in Practice* (Goetz et al.): P1.05 (caps. 6–8), P1.07 (caps. 5, 13, 15), P1.08 (caps. 3 e 16), P1.09 (caps. 2 e 10), P1.13 (caps. 2 e 15). Citado de memória; capítulos **não conferidos** com o livro à mão.
- *Java Performance* (Scott Oaks): P1.01 a P1.03 (memória nativa, JIT, GC e *benchmark*). Citado de memória; capítulos **não conferidos**.
- *Optimizing Cloud Native Java* (Ben Evans): P1.03 e P1.04 (leitura de logs e escolha de coletor). Citado de memória.
- *Effective Java* (Bloch): P1.01 (item 7, vazamentos) e P1.11. Citado de memória.
- *Modern Java in Action* (Urma et al.): P1.06 e P1.11. Citado de memória.
- *Designing Data-Intensive Applications* (Kleppmann): P1.09 e P1.13 (cap. 7: *lost update*, *write skew*). Citado de memória.
- *Release It!* (Nygard): P1.13 (*retry*, *timeouts*, *bulkhead*). Citado de memória.
- *The Java Module System* (Nicolai Parlog): P1.12. Citado de memória.
- **Leituras gratuitas e verificadas:** a documentação oficial do Java 25 (docs.oracle.com: guias de GC, de *virtual threads* e *structured concurrency*, API `java.util.concurrent`), os JEPs (openjdk.org/jeps) e os artigos de Aleksey Shipilëv (shipilev.net).

## Livros e leituras usados no módulo P2
- *Designing Data-Intensive Applications* (Kleppmann): P2.01 a P2.03 (cap. 7: *lost update*, *write skew*, fantasmas, SSI, *deadlocks*), P2.09 (cap. 5 e 11: consistência entre sistemas, *dual writes*), P2.10 (cap. 2 e 3: modelos de dados) e P2.11 (visões derivadas). Citado de memória (capítulos **não conferidos**).
- *Release It!* (Nygard): P2.06 (*timeouts*, *bulkheads*: o pool e o `connection-timeout`). Citado de memória.
- *High-Performance Java Persistence* (Vlad Mihalcea): P2.07 e P2.08 (*fetching*, *batching*, cache). Citado de memória; **os exemplos podem ser de outras versões do Hibernate**: confira.
- *SQL Performance Explained* (Markus Winand): P2.04 e P2.05 (a versão em papel de *Use The Index, Luke*). Citado de memória.
- *Java Persistence with Hibernate* (Bauer, King, Gregory): P2.07. Citado de memória.
- *Fundamentals of Software Architecture* (Richards, Ford): P2.10 (escolha de armazenamento como decisão de arquitetura). Citado de memória.
- **Leituras gratuitas e verificadas:** a documentação do PostgreSQL 18 (transaction-iso, mvcc, explicit-locking, using-explain, indexes-*, pgbench, pgstatstatements), o README e a página de dimensionamento do HikariCP, a documentação do Spring Data Redis/Boot/Framework 7 e [Use The Index, Luke](https://use-the-index-luke.com/).
| *Kafka: The Definitive Guide* (Shapira et al.) | 🔵 | P3.04–P3.06 (produtores, consumidores, *rebalance*, semânticas); capítulos de memória, **não conferidos** |
| *Enterprise Integration Patterns* (Hohpe, Woolf) | 🔵 | P3.03, P3.09 (mensageria, *dead letter channel*, *idempotent receiver*); de memória, **não conferido** |
| *Domain-Driven Design* (Evans) | 🔵 | P4.06–P4.08 (blocos táticos, contextos, ACL); partes de memória, **não conferidas** |
| *Implementing Domain-Driven Design* (Vernon) | 🔵 | P4.06, P4.07 (as regras práticas de agregado, repositórios, eventos); de memória, **não conferido** |
| *Clean Architecture* (Robert C. Martin) | 🔵 | P4.02, P4.03 (regra de dependência, princípios); de memória, **não conferido** |
| *Design Patterns* (Gamma, Helm, Johnson, Vlissides) | 🔵 | P4.04, P4.05 (os 7 padrões); de memória, **não conferido** |
| *Get Your Hands Dirty on Clean Architecture* (Tom Hombergs) | 🔵 | P4.02 (hexagonal com Spring); de memória, **não conferido** |
| *Building Evolutionary Architectures* (Ford, Parsons, Kua) | 🔵 | P4.10 (*fitness functions* = testes de arquitetura); de memória, **não conferido** |
| *Learning Domain-Driven Design* (Khononov) | 🔵 | P4.08 (subdomínios, contextos); de memória, **não conferido** |
| *Building Microservices* (Sam Newman) | 🔵 | P5.01, P5.02 (quando **não** dividir, decomposição, estrangulamento); de memória, **não conferido** |
| *Release It!* (Michael Nygard) | 🔵→🟣 | P5.07, P5.08, P5.10 (*timeouts*, *circuit breaker*, *bulkhead*, falhas em cascata); de memória, **não conferido** |
| *Microservices Patterns* (Chris Richardson) | 🔵→🟣 | P5.02, P5.05 (decomposição, API Gateway, sagas); de memória, **não conferido** |
| *The Twelve-Factor App* (Adam Wiggins; online) | 🔵 | P5.06 (configuração no ambiente); de memória, **não conferido** |
| *Observability Engineering* (Majors, Fong-Jones, Miranda) | 🔵 | P6.01 (três pilares, observabilidade × monitoramento, alta cardinalidade nos eventos); de memória, **não conferido** (página da editora: 403 ao `curl`) |
| *Distributed Tracing in Practice* (Parker et al.) | 🔵 | P6.05 (propagação, amostragem, leitura de traços); de memória, **não conferido** |
| *Site Reliability Engineering* e *The Site Reliability Workbook* (Google; online gratuito) | 🔵→🟣 | P6.03, P6.04, P6.07 (monitoramento, alertas, troubleshooting, postmortem): os **capítulos citados** foram abertos (HTTP 200), o texto não foi relido nesta execução |
| *Kubernetes in Action* (Marko Lukša) e *Kubernetes Up & Running* (Burns, Beda, Hightower) | 🔵 | P7.03 a P7.06 (objetos, sondas, recursos, HPA); de memória, **não conferidos** |
| *Production Kubernetes* (Rosso, Lander, Brand, Harris) | 🔵→🟣 | P7 em profundidade (rede, segurança, operação); de memória, **não conferido** |
| *Java Performance* (Scott Oaks) | 🔵 | P7.02 (memória nativa, coletores, ergonomia); de memória, **não conferido** |

| *Continuous Delivery* (Jez Humble e David Farley) | 🔵 | P8.01 a P8.03 (pipeline de implantação, estratégias de liberação); de memória, **não conferido** |
| *Accelerate* (Nicole Forsgren, Jez Humble, Gene Kim) | 🔵→🟣 | P8.01 (métricas DORA e capacidades de entrega); de memória, **não conferido** |
| *Terraform: Up & Running* (Yevgeniy Brikman) | 🔵 | P8.05 a P8.07 (*state*, módulos, uso em equipe); de memória, **não conferido** |
| *The DevOps Handbook* (Kim, Humble, Debois, Willis) | 🔵→🟣 | P8 em visão ampla (fluxo, *feedback*, aprendizado); de memória, **não conferido** |
| *Designing Data-Intensive Applications* (Martin Kleppmann) | 🟣 (já citado) | P9.08 (capítulo de **replicação**: atraso, *read-your-writes*); de memória, **não conferido** |
| *Site Reliability Engineering* (Google; gratuito online) | 🔵→🟣 | P9.07 (alertas por sintoma, ruído); de memória, **não conferido** |
| *OAuth 2 in Action* (Justin Richer, Antonio Sanso) | 🔵 | P10.01 (fluxos, tokens, ataques); de memória, **não conferido** |
| *Spring Security in Action* (Laurențiu Spilcă) | 🔵 | P10.03–P10.04 (Resource Server, cliente OAuth2); de memória, **não conferido** |
| *Threat Modeling: Designing for Security* (Adam Shostack) | 🔵→🟣 | P10.07 (as quatro perguntas, STRIDE); de memória, **não conferido** |

## Livros e leituras usados no módulo S1
- *Designing Data-Intensive Applications* (Kleppmann): S1.01 (cap. 1), S1.02 (cap. 5 e 9), S1.03 (cap. 5 e 6), S1.04 (cap. 5), S1.05 e S1.06 (cap. 11), S1.08 (cap. 11), S1.09/S1.10 (cap. 8 e 9), S1.12 (cap. 12), S1.13 (cap. 1, o *timeline*). Citado de memória (capítulos **não conferidos**).
- *Release It!* (Nygard): S1.01 e S1.09 (estabilidade, limites). Citado de memória.
- *Domain-Driven Design* (Evans) e *Implementing DDD* (Vernon): S1.06 (agregado como unidade de consistência). Citado de memória.
- *System Design Interview* (Alex Xu): S1.11 a S1.13 (encurtador, *feed*, notificações). Citado de memória, **não conferido**.
- Pat Helland, *Life beyond Distributed Transactions* (ACM Queue, 2016): S1.07. ⚠️ link **não verificado** (`queue.acm.org` deu 403); procure pelo título.
- **Leituras gratuitas e verificadas:** os *papers* de Gilbert-Lynch (CAP), Abadi (PACELC), Dynamo, Lamport, Kulkarni (HLC), Spanner, Garcia-Molina (Sagas) e Raft; os **rascunhos IETF** de `Idempotency-Key` e `RateLimit`; a AWS Builders' Library; a documentação do PostgreSQL 18 e do Redis (ver `biblioteca.md`).

## Livros e leituras usados no módulo S2
- *Systems Performance* (Brendan Gregg): S2.01 (metodologia, USE) e S2.03 (*profiling*, *flame graphs*). Citado de memória, **não conferido**.
- *Java Performance* (Scott Oaks): S2.04 (memória), S2.05 (GC), S2.07 (partida), S2.08 (microbenchmarks). Citado de memória, **não conferido**.
- *Release It!* (Nygard): S2.06 (testes de estabilidade e de carga). Citado de memória.
- *The Tail at Scale* (Dean e Barroso, Google), *How NOT to Measure Latency* (Gil Tene, palestra) e o *Google SRE Book* (monitoramento): leituras **gratuitas e verificadas** (links na biblioteca).

## Livros e leituras usados no módulo S3
- *Designing Data-Intensive Applications* (Martin Kleppmann): cap. 3 (armazenamento: OLTP × OLAP, colunar; S3.07/S3.08), cap. 4 (codificação e evolução: compatibilidade; S3.01/S3.09), cap. 5 (replicação, *read-your-writes*; S3.03), cap. 11 (fluxos e CDC; S3.06). **De memória, capítulos não conferidos.**
- *Refactoring Databases* (Ambler e Sadalage): o catálogo de refatorações de banco com transição (S3.01/S3.09). Citado de memória, **não conferido**.
- *The Data Warehouse Toolkit* (Ralph Kimball): modelagem dimensional (S3.08). Citado de memória, **não conferido**.
- *Evolutionary Database Design* e *Parallel Change* (Martin Fowler): **leituras gratuitas e verificadas** (links na biblioteca).

## Livros e leituras usados no módulo S4
- *Site Reliability Engineering* e *The Site Reliability Workbook* (Google): **gratuitos em sre.google** (links na biblioteca): SLOs e alertas por *burn rate* (S4.01), incidentes (S4.02), postmortem (S4.03), sobrecarga e falha em cascata (S4.04/S4.07), integridade de dados (S4.05).
- *Release It!* (Michael Nygard): *timeout*, *circuit breaker* e *bulkhead* (S4.04). Citado de memória, **não conferido** (capítulos).
- *Designing Data-Intensive Applications* (Martin Kleppmann): **1ª edição**, cap. 1 (descrever carga e desempenho; S4.07). Confira a numeração na sua edição.
- *Implementing Service Level Objectives* (Alex Hidalgo): aprofundamento de SLOs (S4.01). Citado de memória, **não conferido**.
- *The Art of Capacity Planning* (John Allspaw): clássico de capacidade (S4.07). Citado de memória, **não conferido**.

## Livros e leituras usados no módulo S5
- *Release It!* (Michael Nygard): capítulos sobre **segurança, *blast radius* e padrões de estabilidade** (S5.01). Citado de memória, **não conferido**.
- *Serious Cryptography* (Jean-Philippe Aumasson): **AEAD (GCM), nonces, gerenciamento de chaves** (S5.03). Citado de memória, **não conferido**.
- *Cryptography Engineering* (Ferguson, Schneier, Kohno): **por que não inventar criptografia** (S5.03). Citado de memória, **não conferido**.
- *Security Engineering* (Ross Anderson): **registros e auditoria**, modelos de ameaça (S5.06). Gratuito online no site do autor; **não conferi o endereço**.
- *Alice and Bob Learn Application Security* (Tanya Janca): SAST/DAST/SCA no ciclo de vida (S5.04). Citado de memória, **não conferido**.
- *Threat Modeling: Designing for Security* (Adam Shostack): o método por trás do STRIDE (S5.07; P10.07). Citado de memória, **não conferido**.
- *The Web Application Hacker's Handbook* (Stuttard e Pinto): o olhar do atacante (S5.07). Citado de memória, **não conferido**.
- *Privacy Engineering* (Dennedy, Fox, Finneran): privacidade como requisito de engenharia (S5.05). Citado de memória, **não conferido**.
- *Site Reliability Engineering* (Google): o laço **alerta → runbook** (S5.06). Já listado no módulo S4.

## Livros e leituras usados no módulo S6
- *Cloud FinOps* (J. R. Storment e Mike Fuller, O'Reilly): o livro da FinOps Foundation; fases, custo unitário, compromissos e anomalias (S6.04, S6.06). Citado de memória, **não conferido**.
- *Fundamentals of Software Architecture* (Richards e Ford): "tudo em arquitetura é um *trade-off*"; características de arquitetura e elasticidade (S6.01, S6.02). Já listado no Pleno.
- *Building Microservices* (Newman): acoplamento e o custo de mudar (S6.03). Já listado no Pleno.
- *Designing Data-Intensive Applications* (Kleppmann): formatos de dados e evolução de esquema, base da portabilidade dos dados (S6.03). Já listado.
- *Site Reliability Engineering* (Google): postmortem sem culpa aplicado a custo (S6.06). Já listado no módulo S4.

## Livros e leituras usados no módulo S7
- *Fundamentals of Software Architecture* (Richards e Ford): decisões de arquitetura e ADRs; *trade-offs* como base do "alternativas consideradas" (S7.01, S7.02). Já listado no Pleno.
- *Software Estimation: Demystifying the Black Art* (Steve McConnell): estimativa × meta × compromisso, cone da incerteza (S7.03). Citado de memória, **não conferido** (o fator de 4× do cone foi conferido na página da Construx).
- *The Mythical Man-Month* (Fred Brooks): mais gente não encurta o prazo na mesma proporção; a lei de Conway citada no livro (S7.03, S7.06). Citado de memória.
- *Your Code as a Crime Scene*, 2ª edição (Adam Tornhill): hotspots e acoplamento de mudança a partir do histórico do Git (S7.04). Página da editora conferida; conteúdo citado de memória.
- *Working Effectively with Legacy Code* (Michael Feathers) e *Refactoring* (Martin Fowler): testes de caracterização e refatorações seguras na modernização (S7.04). Citados de memória.
- *Team Topologies* (Matthew Skelton e Manuel Pais): tipos de time e modos de interação (S7.06). Conceitos conferidos no site; livro citado de memória.
- *The Principles of Product Development Flow* (Donald Reinertsen): a origem do custo do atraso (S7.06). Citado de memória, **não conferido**.
- *Site Reliability Engineering Workbook* (Google, gratuito online): o exemplo de política de *error budget* (S7.06). Conferido online.

## Livros e leituras usados no módulo S8
- *Getting to Yes* (Roger Fisher, William Ury e Bruce Patton): a origem do termo BATNA (S8.05). Citado de memória, **não conferido** (a definição usada na aula vem da página do PON/Harvard).
- *The Staff Engineer's Path* (Tanya Reilly) e *Staff Engineer* (Will Larson; os arquétipos estão conferidos em staffeng.com): o caminho de contribuidor individual depois do sênior (S8.05). Livros citados de memória.
- *The Manager's Path* (Camille Fournier): o caminho de gestão, degrau por degrau (S8.05). Citado de memória, **não conferido** (a página da editora respondeu 403).
- *Tech Interview Handbook* (Yangshun Tay, gratuito online): currículo, *live coding*, comportamental e negociação (S8.02–S8.05). Conferido online.
