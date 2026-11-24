# Perguntas de entrevista

> Consolidado a cada módulo; respostas-modelo estão em cada aula, na seção "🎤 Na entrevista".

## 🟢 Júnior
| Módulo | Aula | Pergunta |
|---|---|---|
| J0 | J0.06 | O que é `Caused by`? Como você investiga uma exceção? |
| J1 | J1.01 | `==` vs `equals`? Por que não usar `double` para dinheiro? O que é overflow? |
| J1 | J1.02 | O que é curto-circuito? `switch` clássico vs expressão? |
| J1 | J1.03 | Java é por valor ou por referência? Sobrecarga vs sobrescrita? |
| J1 | J1.04 | Por que `String` é imutável? `String` vs `StringBuilder`? |
| J1 | J1.05 | O que é encapsulamento? Quando usar `static`? |
| J1 | J1.06 | O que é polimorfismo? Classe abstrata vs interface? |
| J1 | J1.07 | Por que composição em vez de herança? O que é injeção de dependência? |
| J1 | J1.08 | Contrato `equals`/`hashCode`? Como funciona um `HashMap`? |
| J1 | J1.09 | Checked vs unchecked? Para que serve `try-with-resources`? |
| J1 | J1.10 | `ArrayList` vs `LinkedList`? Por que `ConcurrentModificationException`? |
| J1 | J1.11 | `HashMap` vs `TreeMap`? Fila vs pilha? |
| J1 | J1.12 | O que é type erasure? O que é PECS? |
| J1 | J1.13 | O que é um `Stream`? `map` vs `flatMap`? |
| J1 | J1.14 | Para que serve `Optional`? Record vs classe? O que ganho com `sealed`? |
| J1 | J1.15 | `BigDecimal` vs `double`? `HALF_UP` vs `HALF_EVEN`? `Instant` vs `LocalDateTime`? |
| J1 | J1.16 | Níveis de acesso? O que é O(n log n)? Como testar código que usa a hora atual? |
| J2 | J2.01 | O que acontece ao digitar uma URL? IP vs porta? O que é `Connection refused`? TCP vs UDP? |
| J2 | J2.02 | `PUT` vs `PATCH` vs `POST`? O que é idempotente e por que importa em pagamentos? 401 vs 403? |
| J2 | J2.03 | O que o TLS garante? Explique o handshake. O que é CA/cadeia? O que fazer com `PKIX path building failed`? |
| J2 | J2.04 | Como o servidor lembra o usuário? Sessão vs JWT? Para que servem `HttpOnly`/`Secure`/`SameSite`? Por que dinheiro como string em JSON? |
| J2 | J2.05 | Como investigar uma API com erro intermitente? Por que p95 e não a média? Funciona no Postman e falha no navegador: por quê? |
| J2 | J2.06 | O que o Spring MVC faz que você fez à mão? Por que timeouts no cliente HTTP? `HttpClient` lança exceção em 404? |
| J3 | J3.01 | Como se implementa um N:N? Onde fica a FK em um 1:N? O que é cardinalidade e opcionalidade? |
| J3 | J3.02 | Explique 1FN, 2FN e 3FN. Chave natural ou substituta? Quando desnormalizar? Perigo do `ON DELETE CASCADE`? |
| J3 | J3.03 | Por que Docker para o banco? O que acontece com os dados ao remover o container? Database vs schema? |
| J3 | J3.04 | `DELETE` vs `TRUNCATE` vs `DROP`? Por que `NUMERIC` e não `FLOAT`? Para que serve `RETURNING`? O que é upsert? |
| J3 | J3.05 | Por que `= NULL` não funciona? Ordem lógica de um `SELECT`? Problemas do `OFFSET`? Por que `ORDER BY` precisa de desempate? |
| J3 | J3.06 | `INNER` vs `LEFT JOIN`? Como achar registros sem correspondente? Por que o `SUM` inflou após um `JOIN`? `ON` ou `WHERE`? |
| J3 | J3.07 | `WHERE` vs `HAVING`? `COUNT(*)` vs `COUNT(col)`? Subquery correlacionada? Como conferir saldo × extrato? |
| J3 | J3.08 | O que é CTE? `GROUP BY` vs função de janela? `ROW_NUMBER` vs `RANK` vs `DENSE_RANK`? Últimos N por grupo? |
| J3 | J3.09 | O que é um índice e qual o custo? Como investigar consulta lenta? Por que o banco ignora um índice? Ordem das colunas no composto? |
| J3 | J3.10 | Explique ACID. E se o servidor cair no meio da transferência? Isolamento padrão do PostgreSQL? Transação abortada? Chamada externa dentro de transação? |
| J3 | J3.11 | `Statement` vs `PreparedStatement`? O que é SQL injection e como evita? Por que `BigDecimal`? Pool de conexões? Transação em JDBC? |
| J3 | J3.12 | O que é o Flyway? Por que não editar migração aplicada? Versionada vs repetível? Colisão de versões? Mudança sem downtime? |
| J4 | J4.01 | `GET` vs `POST` em formulário? Para que serve HTML semântico? A validação HTML5 basta? Por que um campo some do `POST`? |
| J4 | J4.02 | O que é o box model? Como a cascata resolve conflitos? Como centralizar? Flexbox ou grid? |
| J4 | J4.03 | Grid ou flexbox? O que é design responsivo? Para que serve o `viewport`? Como testa responsividade? |
| J4 | J4.04 | `==` ou `===`? O que é o DOM? Bubbling e delegação? O que é XSS? Por que não `Number` para dinheiro? |
| J4 | J4.05 | O que é uma Promise? `async`/`await` bloqueia? O `fetch` rejeita em 404? O que é CORS? Como o JS single-thread atende várias coisas? |
| J4 | J4.06 | O que é o Thymeleaf e "natural templates"? `th:text` ou `th:utext`? O que é PRG? SSR ou SPA? Por que 404 e não 403? |
| J4 | J4.07 | O que é uma SPA? React ou Angular? O que o TypeScript traz? Como o back-end Java se relaciona com um front SPA? Quando não faria SPA? |
| J4 | J4.08 | O que é acessibilidade e como garante? O que é ARIA? Como testar? O que é o nível AA do WCAG? |
| J5 | J5.01 | O que é IoC/DI? Injeção por construtor, *setter* ou campo? O que é um bean? O que causa dependência circular? |
| J5 | J5.02 | O que é Spring Boot × Spring Framework? O que faz `@SpringBootApplication`? O que é um starter? Como entregar um app Boot? |
| J5 | J5.03 | Escopos de bean e o padrão? `@Component` × `@Service` × `@Repository`? Ciclo de vida? `NoSuchBean` × `NoUniqueBean`? |
| J5 | J5.04 | Como funciona a autoconfiguração? Como descobrir por que um bean existe? Como substituir/desligar uma autoconfiguração? |
| J5 | J5.05 | Como configurar vários ambientes? Ordem de precedência das propriedades? `@Value` × `@ConfigurationProperties`? Como tratar segredos? |
| J5 | J5.06 | Fluxo de uma requisição no Spring MVC? `@Controller` × `@RestController`? Filtro × interceptor? Tratamento de exceções? Por que PRG? |
| J5 | J5.07 | JPA × Hibernate? Como mapear um `enum`? Para que serve `@Version`? `ddl-auto`? Por que UUID v7? |
| J5 | J5.08 | Lazy × eager e o padrão? `LazyInitializationException`? Contexto de persistência? Estados de uma entidade? O que é OSIV? |
| J5 | J5.09 | O que é Spring Data JPA? Como funcionam as consultas derivadas? `save` insere ou atualiza? `findById` × `getReferenceById`? |
| J5 | J5.10 | Como paginar? `Page` × `Slice`? Problemas do `OFFSET`? O que é JPQL e quando usar `@Query`? Ordenação estável? |
| J5 | J5.11 | Como o `@Transactional` funciona? Quando não funciona? `REQUIRED` × `REQUIRES_NEW`? `readOnly`? Onde colocar a transação? |
| J5 | J5.12 | O que é N+1 e como resolver? Por que não `EAGER`? Como detectar? `JOIN FETCH` × `@EntityGraph`? O que é batch fetching? |
| J5 | J5.13 | O que é Bean Validation? `@NotNull` × `@NotEmpty` × `@NotBlank`? Como tratar erros de validação? Onde validar? |
| J5 | J5.14 | SLF4J × Logback? Níveis de log? Por que `{}`? O que é MDC? O que não logar? Como mudar o nível sem reiniciar? |
| J5 | J5.15 | O que é o Actuator? Health e Kubernetes (liveness/readiness)? O que expor em produção? DevTools em produção? |
| J6 | J6.01 | O que é REST? `PUT` × `PATCH`? `POST` é idempotente? O que é *stateless*? |
| J6 | J6.02 | Por que DTO em vez de entidade? O que é *mass assignment*? MapStruct × ModelMapper? |
| J6 | J6.03 | Como garantir atomicidade numa transferência? `422` × `400` × `409`? Dois saques simultâneos? |
| J6 | J6.04 | O que é Problem Details? Por que um `type` estável? |
| J6 | J6.05 | Offset × cursor? Filtros opcionais em JPA? |
| J6 | J6.06 | Como versionar uma API? Como tornar `POST` seguro para repetir? E chamadas simultâneas? |
| J6 | J6.07 | O que é OpenAPI? Contract-first × code-first? |
| J6 | J6.08 | O que é CORS e preflight? Por que timeouts? |
| J6 | J6.09 | O que é HATEOAS? Sua API é REST? |
| J7 | J7.01 | O que é a pirâmide de testes? Como escolher os casos? Por que não comparar `BigDecimal` com `equals`? |
| J7 | J7.02 | Mock, stub, fake e spy? Quando não usar Mockito? O que é `UnnecessaryStubbingException`? |
| J7 | J7.03 | `@WebMvcTest` × `@SpringBootTest`? O que é `@MockitoBean`? Como testar que um endpoint exige autenticação? |
| J7 | J7.04 | `@DataJpaTest` × `@SpringBootTest`? Por que banco real em vez de H2? O que é o cache de contexto? |
| J7 | J7.05 | Por que Testcontainers? Como ligar a aplicação ao container? Qual o custo e como controlar? |
| J7 | J7.06 | O que a cobertura mede e não mede? Qual piso exigir? O que é TDD e quando usar? |
| J7 | J7.07 | O que é refatorar e como garantir que nada quebrou? Cite três *smells*. Por que não `double` para dinheiro? |
| J7 | J7.08 | Para que servem Checkstyle, SpotBugs e SonarLint? O que fazer com um falso positivo? Como introduzir análise estática em legado? |
| J7 | J7.09 | O que olhar em um code review? Como reagir a discordância? Por que Conventional Commits? |
| J8 | J8.01 | Autenticação × autorização? `401` × `403`? Onde o Spring Security entra? O que acontece só com o starter? |
| J8 | J8.02 | Como guardar senha? Para que serve o `UserDetailsService`? Por que a mesma mensagem para e-mail inexistente e senha errada? |
| J8 | J8.03 | Sessão ou JWT? O que é *session fixation*? É seguro colocar dados em um JWT? Como revogar um JWT? |
| J8 | J8.04 | Papel × autoridade? URL × `@PreAuthorize`? Por que meu `@PreAuthorize` não funciona? O que é controle de acesso quebrado? |
| J8 | J8.05 | Como prevenir SQL injection e XSS? O que é CSRF e como o Spring o previne? Para que serve a CSP? |
| J8 | J8.06 | O que é IDOR e por que `404`? Como proteger o login contra força bruta e *credential stuffing*? O que logar e o que nunca logar? |
| J8 | J8.07 | Onde guardar segredos? Vazou uma chave: o que fazer? Como cuidar das vulnerabilidades das dependências? O que a LGPD muda no código? |
| J9 | J9.01 | Container × VM? Imagem × container? Onde ficam os dados de um banco em container? Por que não `latest`? |
| J9 | J9.02 | O que é multi-stage e por que usar? Como fazer o cache de camadas funcionar com Maven? Por que não rodar como root? Por que o `docker stop` demora 10 s? |
| J9 | J9.03 | Para que serve o Compose e quando não usar? Como a aplicação acha o banco? Como garantir a ordem de subida? Como passar a senha do banco? |
| J9 | J9.04 | Um container sai logo depois de subir: como investigar? Por que logar no console? Como depurar Java em container? Para que serve o `.dockerignore`? |
| J10 | J10.01 | O que é CI e qual a diferença para CD? O que é *script injection* no Actions? Por que `permissions` mínimas e `ubuntu-24.04` fixo? |
| J10 | J10.02 | Como acelerar um pipeline? Por que fixar *actions* por SHA? Por que `pull_request_target` é perigoso? Dependency-Check × `dependency-review`? |
| J10 | J10.03 | IaaS × PaaS × SaaS? O que é responsabilidade compartilhada? Como escolher a região? |
| J10 | J10.04 | O que fazer primeiro em uma conta AWS nova? Usuário × papel? Como evitar chave de acesso vazada? |
| J10 | J10.05 | Security Group × NACL? Como proteger um banco no RDS? Por que IMDSv2 e `verify-full`? |
| J10 | J10.06 | Por que OIDC em vez de chave de acesso? Como fazer rollback? Por que sem SSH? Onde fica o limite do corpo da requisição? |
| J10 | J10.07 | Como evitar custos esquecidos? O que é FinOps? Você usou outra nuvem? |

> 🟢 **Simulado de entrevista júnior** (20 perguntas com respostas-modelo, cruzando os módulos J0–J10): veja a Parte 4 de [`1-junior/checkpoint-junior.md`](../1-junior/checkpoint-junior.md).

## 🔵 Pleno
> 🔵 **Simulado de entrevista pleno** (20 perguntas técnicas com respostas-modelo **e** o design de um serviço de 45 min, com rubrica): veja a Parte 4 de [`2-pleno/checkpoint-pleno.md`](../2-pleno/checkpoint-pleno.md).

| Módulo | Aula | Pergunta |
|---|---|---|
| P1 | P1.01 | Diferença entre heap e stack? O que é o metaspace? Por que `-Xmx` não é a memória do processo? Contêiner morto com 137: como investigar? |
| P1 | P1.02 | O que é o JIT e por que a 1ª requisição é lenta? O que é desotimização? Como fazer um benchmark confiável? |
| P1 | P1.03 | Como funciona um GC geracional? O que significa *Garbage First*? Quando Parallel em vez de G1? O que é um objeto *humongous*? |
| P1 | P1.04 | Por que o ZGC tem pausas tão curtas e qual o preço? Como medir pausa corretamente? Serviço lento após trocar para ZGC: o que olhar? |
| P1 | P1.05 | Thread × Runnable × ExecutorService? O que acontece quando a fila do pool enche? Por que `submit` engole exceções? Como dimensionar um pool? |
| P1 | P1.06 | `thenApply` × `thenCompose`? Em que thread roda cada etapa? Riscos do `commonPool`? `cancel()` interrompe a tarefa? |
| P1 | P1.07 | `synchronized` × `ReentrantLock`? CAS e ABA? `LongAdder` × `AtomicLong`? Por que `containsKey`+`put` falha no `ConcurrentHashMap`? |
| P1 | P1.08 | O que faz `volatile`? O que é *happens-before*? Por que o double-checked locking precisa de `volatile`? Seu teste de concorrência passou: prova algo? |
| P1 | P1.09 | *Data race* × *race condition*? O que é *check-then-act*? Como nasce e como se previne um *deadlock*? Como diagnosticá-lo em produção? |
| P1 | P1.10 | O que é uma virtual thread e quando não ajuda? O que é *pinning* e o que mudou no Java 24? `ScopedValue` × `ThreadLocal`? O que é *structured concurrency*? |
| P1 | P1.11 | O que mudou do Java 8 ao 17/21? O que é `sealed` e por que combina com `switch`? Quando usar `record`? |
| P1 | P1.12 | O que são módulos do Java e que problema resolvem? Classpath × module path? Por que aparece `--add-opens`? Spring Boot usa JPMS? |
| P1 | P1.13 | Dois saques simultâneos com saldo para um: como evitar o saldo negativo? Como o `@Version` evita a *lost update*? `409` sob carga: o que fazer? Por que lock em memória não resolve? |
| P2 | P2.01 | Quais anomalias existem e o que cada nível do PostgreSQL permite? O que é *write skew* e por que `REPEATABLE READ` não o impede? Por que dois `SELECT`s de um relatório podem não bater? |
| P2 | P2.02 | O que é *lost update* e como evitar (4 defesas)? Otimista × pessimista? Por que `UPDATE … WHERE balance >= ?` é seguro em `READ COMMITTED`? O que é `SKIP LOCKED`? Onde fica o *retry* e o que ele repete? |
| P2 | P2.03 | Como nasce um *deadlock* no banco e o que o PostgreSQL faz? Como prevenir (ordem global) e diagnosticar (`pg_blocking_pids`)? Por que `ORDER BY id` na mesma consulta do `FOR UPDATE`? |
| P2 | P2.04 | `EXPLAIN` × `EXPLAIN ANALYZE`? Como investigar uma consulta lenta? Por que o banco ignora um índice? Por que `OFFSET` grande é lento e a alternativa? `Index Only Scan`? |
| P2 | P2.05 | Como escolher a ordem das colunas de um índice composto? Índice parcial e de cobertura: quando e riscos? Custo de um índice a mais? Como criar um índice em produção sem parar as escritas (e a armadilha com o Flyway)? |
| P2 | P2.06 | Por que um pool pequeno rende mais? O que significa `Connection is not available, request timed out`? Como dimensionar com várias réplicas? Threads virtuais resolvem o pool? |
| P2 | P2.07 | O que é N+1 e como resolver (`JOIN FETCH`, `@EntityGraph`, *batch fetching*, projeção)? `MultipleBagFetchException`? Quando usar projeção? `LazyInitializationException` e `open-in-view`? |
| P2 | P2.08 | Como acelerar inserções em massa com JPA? O que é o cache de 2º nível e quando evitá-lo? `JdbcClient` × `JdbcTemplate`? Quando jOOQ? Posso misturar JPA e JDBC? |
| P2 | P2.09 | O que é *cache-aside* e seu problema de consistência? *Cache stampede*? Por que invalidar depois do *commit*? Quando cachear não compensa? O que acontece se o Redis cair? |
| P2 | P2.10 | Quando NoSQL em vez de PostgreSQL? O que é modelar por padrões de acesso? MongoDB tem transações? Idempotência no DynamoDB? O que é partição quente? |
| P2 | P2.11 | Uma tela está lenta em produção: como você investiga? Cache resolve? Como paginar milhões de linhas? O que fazer quando o índice não basta para uma agregação? Como saber que não piorou a escrita? |
| P3 | P3.01 | Síncrono × assíncrono × bloqueante? Como dimensiona *threads* (I/O × CPU)? O que é a Lei de Little e o que acontece quando um parceiro fica lento? |
| P3 | P3.02 | Por que meu `@Scheduled` rodou duas vezes? `@Async` é seguro para enviar e-mail? O que mudam as *threads* virtuais? |
| P3 | P3.03 | Fila ou tópico? O que acontece se o consumidor cai no meio? Para que serve a DLX? |
| P3 | P3.04 | Como o Kafka garante ordem? O que é *consumer group* e qual o limite de paralelismo? Meu consumidor processou duas vezes: por quê? |
| P3 | P3.05 | Retenção × compactação? Como preservar a ordem de uma conta? O consumidor ficou fora mais que a retenção: e agora? |
| P3 | P3.06 | O Kafka é *exactly-once*? Como evitar cobrar duas vezes? *At-most* ou *at-least-once* para notificações? |
| P3 | P3.07 | Como faz um consumidor idempotente? Por que `if (!existe) { processa; grava }` falha? E se o efeito é um e-mail? |
| P3 | P3.08 | O que é uma DLQ? Bloqueante ou tópicos de retry? O que é *poison message*? |
| P3 | P3.09 | O que é Outbox e que problema resolve? O consumidor recebe duplicatas? Consulta ou CDC? |
| P3 | P3.10 | Como protege um webhook? O PSP reenviou: e agora? Webhook ou *polling*? |
| P3 | P3.11 | SSE ou WebSocket? Como escalar tempo real em várias instâncias? Riscos de segurança do WebSocket? |
| P3 | P3.12 | WebFlux ou MVC com *virtual threads*? Por que bloquear no *event loop* é ruim? O que é *backpressure*? |
| P3 | P3.13 | Cliente cobrado duas vezes: como investiga? Por que o *exactly-once* do Kafka não resolveu? Como reverter respeitando o livro-razão imutável? |
| P4 | P4.01 | Qual a diferença entre MVC e arquitetura em camadas? Pacote por camada ou por funcionalidade? Por que o controller não chama o repositório? |
| P4 | P4.02 | O que é arquitetura hexagonal? Hexagonal × Clean × Onion? Quando você **não** usaria? O adaptador de saída participa da transação? |
| P4 | P4.03 | Explique o D de SOLID e a diferença para injeção de dependência. Exemplo de violação do Liskov? Quando não abstrair (regra de três)? |
| P4 | P4.04 | Strategy × State? Quando usar Builder? Onde o Spring usa Factory? |
| P4 | P4.05 | Adapter × Decorator? Como o Spring implementa `@Transactional` e por que a auto-chamada o ignora? Observer síncrono × assíncrono? |
| P4 | P4.06 | Entidade × value object? O que é um agregado e por que referenciar outro por id? Uma transação pode alterar dois agregados? |
| P4 | P4.07 | Repositório (DDD) × DAO? Evento de domínio × de integração? `@TransactionalEventListener` perde evento? O que o ouvinte faz quando falha? |
| P4 | P4.08 | O que é *bounded context* e como achou as fronteiras? Núcleo, suporte e genérico? O que é *anticorruption layer*? |
| P4 | P4.09 | O que é monólito modular e por que não começar com microsserviços? O que o Spring Modulith verifica? Como evitar que o acoplamento volte? |
| P4 | P4.10 | Como você garante que a arquitetura não degrada? Como introduzir regras num legado cheio de violações? O que o ArchUnit não pega? |
| P4 | P4.11 | Como evitar que dois módulos se acoplem pelo banco? Por que banco compartilhado entre serviços é problema? Como renomear uma coluna com segurança? |
| P5 | P5.01 | Monólito ou microsserviços? O que é monólito distribuído? Como a disponibilidade muda numa cadeia síncrona? |
| P5 | P5.02 | Como decidir onde cortar? O que é banco por serviço e o que custa? Como migrar sem reescrever (estrangulamento, modo sombra)? |
| P5 | P5.03 | REST ou gRPC? Como evoluir um `.proto`? O que é *deadline* e por que importa? |
| P5 | P5.04 | Compatibilidade para trás e para frente? Como evoluir um evento sem quebrar consumidores? Para que serve um *schema registry*? |
| P5 | P5.05 | O que um API gateway faz e **não** faz? Gateway é ponto único de falha? Spring Cloud Gateway ou gateway de nuvem? |
| P5 | P5.06 | Como os serviços se encontram? Config Server ou ConfigMap? O que acontece se o serviço de configuração cair? |
| P5 | P5.07 | Qual a primeira defesa contra um serviço lento? Quando usar *retry*? O que é amplificação de *retry*? Por que o *fallback* pode desligar o *retry*? |
| P5 | P5.08 | Como funciona um circuit breaker e o que conta como falha? *Bulkhead* × *rate limiter*? Como dimensionar um *bulkhead*? |
| P5 | P5.09 | O que é teste de contrato? Pact × Spring Cloud Contract? O que um bom contrato contém? |
| P5 | P5.10 | Um serviço lento derrubou o seu: como diagnostica e corrige? Por que aumentar o *pool* não resolve? Como garante que não volta? |
| P6 | P6.01 | Os três pilares e quando usa cada um? Log estruturado: por que importa? Como correlaciona a queixa do usuário com o evento? O que nunca vai em log? |
| P6 | P6.02 | Actuator × Micrometer? O que é cardinalidade e por que derruba o Prometheus? Counter, gauge, timer? Como expor métricas com segurança? |
| P6 | P6.03 | Como o Prometheus coleta? O que é RED e USE? Por que não alertar na média? Como se calcula o p95 e qual a limitação? |
| P6 | P6.04 | O que é um bom alerta? Como evita fadiga? Como testa um alerta? O que faz o Alertmanager? |
| P6 | P6.05 | Como funciona a propagação de contexto no tracing e como ela quebra? Como escolher a amostragem? Trace, log e métrica: quando cada um? O que é OpenTelemetry? |
| P6 | P6.06 | *Liveness* × *readiness*? O que acontece se a *liveness* checa o banco? Como garante desligamento sem derrubar requisições? Como monitora erros? |
| P6 | P6.07 | Como investiga uma lentidão em produção? Por que percentis e não a média? O que é um postmortem sem culpa? Como ensaia incidentes? |
| P7 | P7.01 | Como deixa uma imagem Java pequena e rápida de atualizar? Dockerfile, Buildpacks ou Jib? Por que não `latest`? |
| P7 | P7.02 | A JVM respeita o limite do contêiner? O que ela escolhe sozinha? Por que o pod é morto com 137 se o heap está bem? |
| P7 | P7.03 | Pod × ReplicaSet × Deployment? Como um Service encontra os pods? O que é `rolling update`? |
| P7 | P7.04 | Ingress ou Gateway API? Secrets são seguros? ConfigMap como env ou volume? |
| P7 | P7.05 | Liveness × readiness × startup? `requests` × `limits`? Como evitar erro durante um deploy? |
| P7 | P7.06 | Como o HPA decide? Por que não escalou? Escalar resolve a lentidão? |
| P7 | P7.07 | Como testa manifestos localmente? `CrashLoopBackOff`: e agora? |
| P7 | P7.08 | Helm, Kustomize ou manifestos puros? Como o pod reinicia quando o ConfigMap muda? |
| P7 | P7.09 | O pod foi `OOMKilled`; como investiga? Por que o HPA não resolve? |

| P8 | P8.01 | Como deixa um pipeline rápido e confiável? Por que um único check obrigatório? |
| P8 | P8.02 | Onde ficam os scans e por quê antes do *push*? SBOM × atestação? Como trata exceção? |
| P8 | P8.03 | Blue/green × canary? Por que a readiness passou e houve incidente? Como mudar o esquema sem parar? |
| P8 | P8.04 | Deploy × release? Como faz *rollout* percentual estável? Riscos de *feature flags*? |
| P8 | P8.05 | O que é o *state*, *drift*, `count` × `for_each`? |
| P8 | P8.06 | Como guarda o *state* num time? `moved` e `import`? Como testa Terraform? |
| P8 | P8.07 | Como roda Terraform em CI com segurança? O `plan` mostra `destroy` no banco: o que faz? |
| P8 | P8.08 | GitOps ou `helm upgrade` no pipeline? Como migra um Jenkins legado? |
| P9 | P9.01 | Sub-rede pública × privada? SG × NACL? Como uma tarefa privada puxa imagem do ECR? Quanto custa um NAT? |
| P9 | P9.02 | *Task role* × *execution role*? Como faz *deploy* sem derrubar o serviço no ECS? Fargate ou EC2? |
| P9 | P9.03 | ECS ou EKS? O que o EKS gerencia? Como o *pod* chama o SQS? O que é uma *access entry*? |
| P9 | P9.04 | SQS × SNS × EventBridge? O SQS entrega uma vez? Para que serve o *visibility timeout*? Como trata mensagens que sempre falham? |
| P9 | P9.05 | O que é *cold start* e por que Java sofre mais? Como o SnapStart funciona e quais os riscos? `L = λ × W`? |
| P9 | P9.06 | Secrets Manager × Parameter Store? Como o ECS entrega um segredo? Como evitar segredo no *state*? O que a rotação do RDS faz com a aplicação? |
| P9 | P9.07 | Como monitora no ECS com CloudWatch? Por que alarmar a DLQ? O que é `treat_missing_data`? 5xx por contagem ou razão? |
| P9 | P9.08 | Multi-AZ × réplica de leitura? Que problema a réplica traz? O que acontece num *failover* do RDS (DNS da JVM)? Como proteger o banco contra exclusão? |
| P10 | P10.01 | Qual a diferença entre OAuth 2.0 e OIDC? Por que Authorization Code com PKCE e não *implicit*? O que o PKCE impede? O JWT é seguro só por ser assinado? |
| P10 | P10.02 | Cliente público × confidencial? O que é um realm? Como versionar a configuração do Keycloak? Keycloak, Cognito ou construir o próprio servidor? |
| P10 | P10.03 | O que o Spring confere num JWT e o que você acrescenta? 401 × 403 × 404? Como funciona a rotação de chaves sem reiniciar? O usuário fez logout e o token ainda funciona: é bug? |
| P10 | P10.04 | Como um serviço se autentica em outro? Chave de API serve? O gateway barra `/internal`: preciso autenticar? Como o serviço chama o SQS? |
| P10 | P10.05 | Como rotacionar a senha do banco sem derrubar a aplicação? O que fazer com uma chave commitada? Onde guardar segredos? O que é menor privilégio no banco? |
| P10 | P10.06 | O que é um SBOM e para que serve? Por que implantar por digest? Como garantir que só imagens aprovadas rodam? Assinatura com chave ou sem chave? |
| P10 | P10.07 | O que é *threat modeling* e quando fazer? Explique o STRIDE (por elemento). Como evitar que o modelo vire papel de gaveta? Ameaça de baixo risco: corrige? |

## 🟣 Sênior
| Módulo | Aula | Pergunta |
|---|---|---|
| S1 | S1.01 | Como escalar um serviço Java com banco? Por que dobrar as máquinas não dobra a vazão (Amdahl)? *Round-robin* ou menos conexões? O que é serviço sem estado? |
| S1 | S1.02 | Explique o CAP e o PACELC. O PostgreSQL é CP ou AP? O que é `R + W > N`? Quando aceitar consistência eventual? |
| S1 | S1.03 | Replicação × particionamento? Como escolher a chave de partição? Por que hash consistente? Vocês deveriam fazer *sharding*? |
| S1 | S1.04 | *Cache-aside* ou *write-through*? O que é *stampede*? Por que o cache mostra dado velho? Pode pôr o extrato numa CDN? |
| S1 | S1.05 | O que é CQRS e quando usar? Como lidar com a consistência eventual? O *read model* corrompeu: e agora? Exige Kafka/microsserviços? |
| S1 | S1.06 | O que é *event sourcing* e quando **não** usar? Como evoluir um evento? Como corrigir um erro no histórico? |
| S1 | S1.07 | Como fazer uma transação entre dois serviços? Por que não 2PC? A compensação falha: e agora? Saga tem isolamento? |
| S1 | S1.08 | Como evitar cobrança duplicada? Duas requisições com a mesma chave ao mesmo tempo? Mesma chave, corpo diferente? Idempotência é exactly-once? |
| S1 | S1.09 | Projete um *rate limiter* distribuído. Por que não limitar por instância? Janela fixa ou balde? E se o Redis do limitador cair? |
| S1 | S1.10 | Por que não ordenar por *timestamp*? Lamport × vetorial? O que é LWW e seu risco? Como o Spanner ordena? |
| S1 | S1.11 | Projete um encurtador de URL. Como evitar colisão? 301 ou 302? É seguro para links privados? |
| S1 | S1.12 | Projete um sistema de pagamentos. Como evitar cobrança duplicada? O que é conciliação? Como garantir que o dinheiro não some? |
| S1 | S1.13 | Projete o feed de notícias. O que é o problema da celebridade? Projete notificações. Como não duplicar notificação com *at-least-once*? |
| S2 | S2.01 | A API está lenta: o que você faz primeiro? Por que o p99 importa mais que a média? Reduzi um método de 10 ms para 1 ms e o p99 não mudou: por quê? |
| S2 | S2.02 | Como descobrir por que uma aplicação Java está lenta em produção? JFR ou async-profiler? O perfil de CPU não mostra nada mas o serviço está lento: o que olhar? |
| S2 | S2.03 | O que é *safepoint bias*? Como ler um *flame graph*? CPU ou *wall-clock*? |
| S2 | S2.04 | Como investigar um vazamento de memória? `OutOfMemoryError` e `OOMKilled` são o mesmo? Por que *heap dump* é perigoso? |
| S2 | S2.05 | Como você ajusta o GC? G1, ZGC ou Parallel? Por que o heap apertado é ruim? O ZGC elimina pausas? |
| S2 | S2.06 | O que é omissão coordenada? Como validar a capacidade? k6 ou Gatling? O teste passou e a produção caiu: por quê? |
| S2 | S2.07 | Quando usar *native image*? O que é CRaC? Como reduzir a partida de um Spring Boot sem mudar de *runtime*? Por que a JVM é mais rápida no regime? |
| S2 | S2.08 | Por que não medir com `nanoTime()` em volta de um laço? Como saber que o JIT apagou o seu benchmark? `BigDecimal` ou `long` para dinheiro? Um microbenchmark prova que a PR melhora o serviço? |
| S3 | S3.01 | Como renomear uma coluna sem downtime? Por que um `ALTER` rápido derruba o sistema? Como pôr `NOT NULL` em 100 milhões de linhas? `UPDATE` único ou lotes? |
| S3 | S3.02 | Quando particionar? Como converter uma tabela existente sem parar? *Range* ou *hash*? Por que evitar a partição `DEFAULT`? |
| S3 | S3.03 | Como usar réplicas sem servir dado velho? Por que a consulta foi cancelada na réplica? Como o Spring roteia entre primário e réplica? E se a réplica cair? |
| S3 | S3.04 | Como arquivar bilhões de linhas? O que fazer com o saldo ao arquivar o ledger? Como garantir que o arquivo não é alterado? E o pedido de eliminação do titular? |
| S3 | S3.05 | Quando usar OpenSearch/Elasticsearch? Por que `LIKE '%x%'` é lento? Como manter o índice consistente? Como mudar o analisador em produção? |
| S3 | S3.06 | O que é CDC e quando usar? Como evitar que o CDC derrube o banco (*slot*)? Entrega exatamente uma vez? Como funciona o Outbox com Debezium? |
| S3 | S3.07 | Como escolher um banco de dados? Por que não NoSQL no ledger? Quando o PostgreSQL deixa de bastar? O que é BRIN? |
| S3 | S3.08 | Como fazer analytics sem afetar o transacional? Como fazer carga incremental sem perder dados atrasados? O que é esquema em estrela? ETL ou ELT? |
| S3 | S3.09 | Como mudar o formato de uma coluna em produção? Por que não só escrever nas duas colunas no código? Como manter regra em SQL e Java concordando? Quando *não* migrar assim? |
| S4 | S4.01 | Qual a diferença entre SLI, SLO e SLA? O que é *error budget* e como alertar de um SLO? Por que "5xx > 2%" não basta? Seu SLO é 99,99% e o banco é 99,9%: e agora? |
| S4 | S4.02 | Como você conduz um incidente? O que tem um bom *runbook*? O alerta toca toda noite e ninguém age: o que faz? Como evitar que duas pessoas piorem o incidente? |
| S4 | S4.03 | O que é um postmortem sem culpa e quando escrever? Como evitar que o incidente volte? "Fulano causou o incidente": como lidar? |
| S4 | S4.04 | O que é engenharia do caos e como fazer com segurança? Um *circuit breaker* resolve dependência lenta? *Game day* ou automatizado? |
| S4 | S4.05 | RPO × RTO? Já tem Multi-AZ: precisa de *backup*? Como você sabe que o *backup* funciona? Restaurou o banco: acabou? |
| S4 | S4.06 | Multi-AZ basta para alta disponibilidade? Como dimensionar para perder uma AZ? Precisamos de multi-região? Quais as estratégias de DR? |
| S4 | S4.07 | Como você planeja capacidade? Por que não rodar tudo a 90%? Como dimensionar um *pool* de conexões? O disco vai encher? |
| S5 | S5.01 | O que é defesa em profundidade e como você sabe que tem profundidade de verdade? Zero trust é tirar o *firewall*? Como evitar um `DELETE` sem `WHERE` na produção? Seu teste de autorização passa: confia? |
| S5 | S5.02 | Qual a diferença entre TLS e mTLS e o que o mTLS não resolve? Como operar mTLS sem acordar às 3h por certificado expirado? Token ou mTLS entre serviços? Por que o `CN=api` do atacante não entra? |
| S5 | S5.03 | Criptografia em repouso protege de quê? O que é criptografia envelope? O que acontece se reutilizar o *nonce* no GCM? Como proteger o *backup* de quem tem credencial de admin? |
| S5 | S5.04 | SAST × DAST × SCA? Como evitar que o scanner vire ruído? Como saber que o scanner funciona? Passou no scan: é seguro? |
| S5 | S5.05 | O cliente pede para apagar os dados: como o sistema financeiro atende? Como garantir que cartão não aparece no log? O que fazer num vazamento (prazo)? Por que *hash* simples de CPF não anonimiza? |
| S5 | S5.06 | Log × trilha de auditoria? Como garantir que a trilha não foi adulterada? O que alertar a partir da auditoria? Auditoria e LGPD conflitam? |
| S5 | S5.07 | Como conduzir uma revisão de segurança? Como priorizar 40 achados? *Pentest* ou revisão interna? O scanner não achou nada: a revisão pode terminar? |
| S6 | S6.01 | Quais são os pilares do Well-Architected e como você usa numa revisão? Dê um *trade-off* entre pilares que você quantificou. Como evitar que a revisão vire burocracia? |
| S6 | S6.02 | Lambda ou contêiner para uma API? Como escolher a memória de uma função? Quando EC2 ainda faz sentido? |
| S6 | S6.03 | Como você evita *lock-in*? Vale a pena ser multi-cloud? Qual é o custo escondido de mover dados? |
| S6 | S6.04 | Como estimar o custo de um sistema novo? Quando comprar *Savings Plans*? Onde usar Spot? |
| S6 | S6.05 | Por que separar ambientes em contas? O que uma SCP faz e não faz? Como testar um *guardrail*? |
| S6 | S6.06 | A conta de nuvem dobrou: o que você faz? Por que detecção de anomalia por serviço? Como evitar ambientes esquecidos? |
| S7 | S7.01 | Como você registra decisões de arquitetura? O que faz uma RFC dar errado? Quando NÃO escrever uma ADR? |
| S7 | S7.02 | Como é um bom design doc? Como revisar um design com vários times? O que é um *spike* e quando usar? |
| S7 | S7.03 | Como você estima uma tarefa? E um projeto de vários meses? Como gerencia riscos técnicos? |
| S7 | S7.04 | Como convencer o negócio a pagar dívida técnica? Reescrever ou estrangular? Como garantir que o novo faz o mesmo que o velho? |
| S7 | S7.05 | Como fazer uma boa revisão de código? Cobertura de 100% basta? Qual fluxo de *branches* e por quê? Como ajudar um júnior a crescer? |
| S7 | S7.06 | Como explicar uma decisão técnica a alguém de negócio? Como priorizar dívida contra funcionalidade? E lançar com o orçamento de erro esgotado? |
| S7 | S7.07 | Como você usa IA no desenvolvimento? Como proteger uma funcionalidade com LLM contra *prompt injection*? Como testar código que chama um LLM? |
| S8 | S8.01 | Me apresente um projeto do seu GitHub. Você publicou uma senha: o que faz primeiro? Por que eu deveria confiar nesse número do seu README? |
| S8 | S8.02 | Fale sobre você. Este número do seu currículo: como mediu? Por que está mudando de carreira (ou de empresa)? |
| S8 | S8.03 | Como você sabe que sua solução está certa? Qual a complexidade e dá para melhorar? E se não couber em memória? |
| S8 | S8.04 | Conte de quando discordou de uma decisão; conte de um erro seu. Como você começa um *system design*? |
| S8 | S8.05 | Qual sua pretensão salarial? O teto é este: e agora? Onde quer estar em cinco anos (IC ou gestão)? |

> Simulado de *system design* (45 min, rubrica de 100 pontos): `3-senior/S1-sistemas-distribuidos/checkpoint.md`, Parte 4.
> Simulado de **investigação de performance** (60 min, rubrica de 100 pontos, **incidente sintético**): `3-senior/S2-performance-eficiencia/checkpoint.md`, Parte 4.
> Simulado de **mudança de dados com o sistema no ar** (60 min, rubrica de 100 pontos, **cenário sintético**): `3-senior/S3-dados-em-escala/checkpoint.md`, Parte 4.
> Simulado de **incidente e continuidade** (60 min, rubrica de 100 pontos, **cenário sintético**): `3-senior/S4-confiabilidade-sre/checkpoint.md`, Parte 4.
> Simulado de **revisão de segurança antes de abrir o PayFlow a um parceiro** (60 min, rubrica de 100 pontos, **cenário sintético**): `3-senior/S5-seguranca-avancada/checkpoint.md`, Parte 4.
> Simulado de **revisão de custo e arquitetura antes de ir para produção** (60 min, rubrica de 100 pontos, **cenário sintético**): `3-senior/S6-arquitetura-nuvem-custo/checkpoint.md`, Parte 4.
> Simulado de **conduzir uma decisão grande** (60 min, rubrica de 100 pontos, **cenário sintético**): `3-senior/S7-lideranca-tecnica/checkpoint.md`, Parte 4.
> Simulado do **dia de entrevistas** (apresentação, *live coding*, *system design*, comportamental e oferta; 90 min, rubrica de 100 pontos, **cenário sintético**): `3-senior/S8-carreira/checkpoint.md`, Parte 4.
> 🟣 **Checkpoint de nível Sênior** ([`3-senior/checkpoint-senior.md`](../3-senior/checkpoint-senior.md)): **quiz integrador** de 20 questões que cruzam S1–S8, com respostas-modelo (Parte 3); **design doc "PayFlow 10×"** com rubrica (Parte 4); **incidente sintético e postmortem** (Parte 5); **simulado de *system design*** "entrega de *webhooks* a parceiros", 60 min, rubrica de 100 pontos (Parte 6).
