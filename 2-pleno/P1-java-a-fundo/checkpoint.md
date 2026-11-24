# P1.CP — Checkpoint do módulo P1

> ⏱️ ~1h30 · 🔵 Pleno · Pré-requisitos: P1.01 a P1.13

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, JDK 25.0.5 e 21 do Ubuntu, 12 núcleos): todo o `lab/jvm-lab/` (`MemoryAreas`, `Warmup`, `GcLoad` com G1/Parallel/Serial/ZGC/Shenandoah, `Threads`, `Futures`, `Concurrent`, `Visibility`, `Races`, `Virtual`, `Pinning`, `Structured`, `Modern`, `Hello25`, `TransferResultDemo`, `ReflectJdk`, `StudyCase`, `modules-lab` com `javac --module-source-path`/`jdeps`/`jlink`), mais `jcmd`/`jstat`/NMT numa JVM viva. **Não executado:** **tudo** o que toca no PayFlow do aluno (as classes e testes novos descritos abaixo foram **revisados por leitura**), o **PostgreSQL** (o estudo de caso é um modelo **em memória**), o `spring.threads.virtual.enabled` e o comportamento da JVM **dentro de um contêiner** (Docker) com limites. Lista consolidada em "a validar no fim do treinamento" (`00-guia/progresso.md`).

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Quem carrega as classes (*bootstrap*, *platform*, *application*) e em que ordem de delegação? Diferencie **carregar** de **inicializar**. Associe **heap**, **pilha** e **metaspace** aos erros `OutOfMemoryError: Java heap space`, `StackOverflowError` e `OutOfMemoryError: Metaspace`, e diga um **vazamento real** que causa cada um.
2. Por que `-Xmx512m` **não** significa 512 MiB de memória do processo? O contêiner do PayFlow morre com código **137** com o heap abaixo do `-Xmx`: como investigar (NMT)? Que três *flags* você acrescenta para o **próximo** OOM deixar evidência e reiniciar sozinho?
3. Por que as primeiras requisições de um *pod* novo são mais lentas? Explique interpretador, C1, C2, OSR e **desotimização** (e o que é `made not entrant`). Por que um `nanoTime()` solto **não** é *benchmark*?
4. Compare **Parallel**, **G1** e **ZGC** (vazão, pausa, memória, CPU). Que coletor a JVM escolhe com **1 CPU**? Qual critério **mensurável** você usaria antes de trocar o G1 pelo ZGC?
5. Leia `GC(25) Pause Full (G1 Compaction Pause) 95M->67M(96M) 31.989ms` e `GC(19) Minor Collection (Allocation Rate) 322M(63%)->212M(41%) 0.024s`. O que cada linha diz? Por que o "tempo total em GC" do `GarbageCollectorMXBean` **engana** no ZGC? O que é um objeto *humongous*?
6. Qual a ordem de decisão de um `ThreadPoolExecutor` ao receber uma tarefa? Por que `newFixedThreadPool` é perigoso em produção? Onde some a exceção de uma tarefa enviada com `submit()`? Como você dimensiona um pool para tarefas que passam 80% do tempo esperando o banco (e o que **limita** de verdade)?
7. Diferencie `thenApply`, `thenCompose` e `thenCombine`. Como `join()` e `get()` reportam um erro? Por que **sempre** passar um `Executor` a tarefa bloqueante, e por que `cancel()` **não** pára o trabalho? Em `java.util.concurrent`: quando `LongAdder` em vez de `AtomicLong`, e por que `map.get(k)` + `map.put(k, v)` num `ConcurrentHashMap` **perde** atualizações?
8. Enuncie **cinco** regras de *happens-before*. O que `volatile` garante e o que **não** garante (`n++`)? Descreva o laço `while (!stop)` que nunca termina, o teste de *litmus* `(r1, r2) == (0, 0)` e por que o *double-checked locking* precisa de `volatile`.
9. O PayFlow tem 2 saques simultâneos de uma conta com saldo para um só. Diferencie *data race* de *race condition* e *read-modify-write* de *check-then-act*. Quais são as **quatro condições** do *deadlock*, como preveni-lo (ordem global) e como detectá-lo (`jcmd Thread.print`)? Por que um **lock em memória não resolve** com 2 instâncias, e qual é o papel do `@Version`, do *retry* com a **mesma** `idempotency_key` e do `UNIQUE (idempotency_key)`?
10. O que é uma *virtual thread* e quando **não** ajuda? O que mudou no *pinning* do `synchronized` entre o Java 21 e o 25? Por que `ScopedValue` em vez de `ThreadLocal`, e por que a *structured concurrency* do Java 25 **não** vai para produção? Em um `sealed interface` + `switch` por padrões, por que **não** se escreve `default`? Para que servem `jlink` e `--add-opens`, e por que o PayFlow **não** terá `module-info.java`?

<details><summary>Gabarito</summary>

1. Delegação **pai primeiro**: *application* → *platform* → *bootstrap* (`null`). **Carregar** = achar os bytes e criar a `Class`; **inicializar** = rodar `static {…}`, só no **primeiro uso real** (uma constante de compilação **não** dispara; provado em `MemoryAreas init`). **Heap** (objetos) → `Java heap space` (ex.: cache estático sem teto, lista com um milhão de lançamentos); **pilha** (frames, por thread, `-Xss`) → `StackOverflowError` (recursão sem fim: 16 840 chamadas no teste, 95 429 com `-Xss4m`); **metaspace** (metadados de classes, memória **nativa**) → `Metaspace` (vazamento de *classloader*, geração contínua de classes: parou em 963 com `MaxMetaspaceSize=16m`).
2. O processo usa também metaspace, *code cache*, pilhas de **cada** thread (1 MiB × N), estruturas do GC (≈ 23% do heap no laboratório: 59 MiB para 256 MiB), *buffers* diretos e bibliotecas: com 256 MiB de heap o NMT mostrou **370 MiB comprometidos**. O contêiner é morto pelo *cgroup* pelo **total** (137, `OOMKilled=true`). Investigar: `-XX:NativeMemoryTracking=summary` + `jcmd <PID> VM.native_memory summary` (só para investigar: tem custo), RSS do contêiner (`docker stats`) × `jvm.memory.used` por área. Para o próximo OOM: `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=<volume> -XX:+ExitOnOutOfMemoryError` (o diretório precisa sobreviver ao contêiner).
3. Começa **interpretado** (60× mais lento que o regime neste laboratório: 1 230 ns × 20 ns); contadores levam métodos quentes ao **C1** (rápido, nível 3 coleta perfil) e depois ao **C2** (otimizações especulativas: *inlining*, escape analysis…). **OSR** troca o código **no meio de um laço**. **Desotimização:** a suposição do C2 caiu (*uncommon trap*): o código compilado é descartado (`made not entrant`) e volta ao interpretador até recompilar. As threads do compilador também consomem CPU no começo. `nanoTime()` solto mede o **aquecimento**, deixa o JIT apagar código "morto" e contamina o perfil; use JMH (S2) e descarte o aquecimento.
4. **Parallel:** STW com várias threads, mira **vazão**, pausa cresce com o heap vivo (30 ms aqui). **G1** (padrão, JEP 248): regiões, coleta jovem + ciclo **concorrente** de marcação + coletas **mistas**, alvo de pausa (200 ms padrão; 7–14 ms medidos), mais memória (≈ 23%); *humongous* e *Full GC* são os pontos fracos. **ZGC** (geracional por padrão desde o 23, único modo desde o 24): quase tudo **concorrente**, pausas de **~0,01 ms** (máx. 0,16) **independentes do heap**, pagando **CPU e folga de heap** (512 MiB apertado: 468 coletas; 2 GiB: vazão igual à do G1). Com **1 CPU** a JVM escolhe o **Serial** (verificado com `-XX:ActiveProcessorCount=1`). Critério: medir o **p99/p99,9 sob carga real** com G1; só trocar se a **pausa do G1 aparece na cauda** **e** há memória/CPU de sobra; mudar uma coisa por vez.
5. Linha 1: **Full GC** do G1 (a coleta não acompanhou): o heap está **no teto** (`(96M)`) e o "depois" é **alto e estável** (67 M): **conjunto vivo grande para o heap**, não vazamento; correção: mais heap/menos cache, **não** trocar de coletor. Linha 2: coleta **Minor** do ZGC (causa `Allocation Rate`), heap de 322 M (63%) → 212 M (41%); **0,024 s é o ciclo inteiro, concorrente com a aplicação**, não uma pausa. O MXBean do ZGC soma os **ciclos** (8 190 ms em 8 s), não as pausas: leia as linhas `Pause …` ou `jvm.gc.pause`. *Humongous* (G1): objeto ≥ metade de uma região (aqui ≥ 512 KiB), alocado em regiões contíguas direto na velha: custa caro, fragmenta e **força ciclos** (`G1 Humongous Allocation`; 208 ocorrências em 5 s).
6. (1) Abaixo do `corePoolSize` → cria thread; (2) senão → **fila**; (3) fila cheia **e** abaixo do `maximumPoolSize` → thread extra; (4) senão → **rejeita**. `newFixedThreadPool` usa fila **ilimitada**: a sobrecarga **se esconde** (latência e memória sobem, nunca passa do *core*); `newCachedThreadPool` tem threads ilimitadas. Com `submit()` a exceção fica **guardada no `Future`** e some se ninguém chama `get()` (`isDone=true` sem ninguém saber); com `execute()` vai ao `UncaughtExceptionHandler`. Dimensionamento: 20% CPU/80% espera → razão 4 → `núcleos × (1 + 4)` (4 núcleos → **20** threads); mas o limite real é o **menor** entre o pool de threads e o **pool de conexões** (10 conexões → ~10 em paralelo): dimensione os dois **juntos** e use **fila limitada + rejeição** (contrapressão).
7. `thenApply` (função síncrona `T→U`, como `map`); `thenCompose` (função que devolve **outro** `CompletableFuture`, como `flatMap`); `thenCombine` (junta **dois** independentes). `join()` lança `CompletionException`; `get()`, `ExecutionException`; **sempre** `getCause()`. `supplyAsync` sem executor usa o `commonPool` (≈ núcleos − 1: 11); 22 tarefas bloqueantes levaram **1 006 ms** contra **503 ms** com pool próprio, e o `commonPool` é compartilhado com *streams* paralelos. `cancel()` só completa o futuro: a thread **continua** ("a tarefa cancelada continuou até o fim"); *timeout* de rede continua necessário. `LongAdder` para contadores/estatística com **alta contenção** (32 ms × 211 ms do `AtomicLong` × 1 206 ms do `synchronized` no teste de 16 milhões de incrementos), **não** para limites exatos; `get`+`put` são **duas** operações (perdeu ~9 000 de 400 000 atualizações): use `merge`/`compute`/`putIfAbsent`.
8. Regras (JLS §17.4.5): ordem do programa; `unlock` HB `lock` posterior do mesmo monitor; escrita `volatile` HB leitura posterior; `Thread.start()` HB ações da thread; ações da thread HB o retorno de `join()`; fim do inicializador estático HB o primeiro uso; fim do construtor HB publicação de campos `final`; transitividade (e as garantias do `java.util.concurrent`). `volatile` dá **visibilidade e ordem**, **não** exclusão mútua: `n++` continua perdendo incrementos. Sem `volatile` o JIT **içou a leitura** para fora do laço e o trabalhador **nunca** parou (`-Xint` escondeu o bug). No *litmus*, `(0,0)` ocorreu **2 099 e 25 802 vezes** em 300 000 rodadas com campos comuns e **0** com `volatile`. DCL: `new Holder()` são alocar/construir/atribuir e podem ser **reordenados**; sem `volatile` outra thread vê a referência com campos não inicializados; melhor, o *holder idiom*.
9. *Data race* = acesso concorrente **sem HB** (violação do JMM); *race condition* = resultado depende do **tempo**, mesmo com HB em cada passo. *Read-modify-write* (`balance = balance + v`: perdeu 72% do dinheiro) e *check-then-act* (`if (saldo >= v) debita`: saque duplo em **6,8%** das rodadas, saldo até −100,00; transferências **criaram R$ 46 830**). *Deadlock* = exclusão mútua + segura-e-espera + sem preempção + **espera circular**; prevenir quebrando uma (ordem global por `id`: 4 060 ms; `tryLock` + *jitter*: 9 233 ms e 510 retentativas); detectar: `jcmd <PID> Thread.print` ("Found one Java-level deadlock") ou `ThreadMXBean.findDeadlockedThreads()`. Lock em memória só vale **no processo**: com 2+ instâncias (P7, *rolling deploy*) o árbitro é o **banco**. `@Version` recusa a gravação com versão velha (409); o *retry* com *backoff*/*jitter* e a **mesma** `idempotency_key` evita a tempestade de `409` (de 48–70% para 0 no modelo) sem duplicar; o `UNIQUE (idempotency_key)` é a **garantia** da regra 4 (o `find` antes é só otimização: 8 duplicatas × 1).
10. *Virtual thread*: gerenciada pela JVM, pilha no heap, montada em poucas *carriers*, desmontada ao bloquear (100 000 tarefas de 500 ms em ~0,6 s; pool de 100 levou 10 s para 2 000). **Não ajuda** em CPU-bound (1 512 ms × 1 527 ms) nem quando o **recurso escasso** (pool de conexões) já é o gargalo (só desloca a fila): limite com `Semaphore`/Hikari. *Pinning* do `synchronized`: **Java 21 fixava** (B esperou 506 ms), **Java 25 não** (104 ms; JEP 491); ainda fixam métodos nativos e funções estrangeiras. `ScopedValue` (final no 25, JEP 506) é imutável, com escopo e liberação automática, herdado por subtarefas; `ThreadLocal` é mutável, vaza em pool e custa por thread. `StructuredTaskScope` é **preview** (JEP 505, `--enable-preview`, API mudou: `open()` + *joiners*, sem `ShutdownOnFailure`). Sem `default` no `switch` sobre `sealed` para o compilador acusar o caso esquecido (`the switch expression does not cover all possible input values`). `jlink` monta um *runtime* mínimo (55 MiB × ~583 MiB do JDK completo, sem símbolos de depuração); `--add-opens` libera reflexão em pacotes do JDK (`InaccessibleObjectException`) e é **dívida técnica**. O PayFlow roda no **classpath** (reflexão do Spring/Hibernate, *fat JAR* não modular) e usa Spring Modulith/ArchUnit (P4) para fronteiras de domínio.
</details>

## Parte 2: Entrega prática (tag `payflow-P1`)
O `payflow` ganha **notas e decisões de JVM/concorrência**, **proteções de concorrência** e **testes de invariantes sob carga**. **Tudo por leitura/escrita: nada foi compilado ou executado no PayFlow** (decisão do aluno).
```
payflow/
├── docs/
│   ├── jvm-notes.md             memória por limite de contêiner, coletor por nº de CPUs, regra de diagnóstico de GC,
│   │                            política de --add-opens, decisão sobre virtual threads                   (P1.01, P1.03, P1.04, P1.10, P1.12)
│   ├── concurrency-notes.md     inventário de estado compartilhado, dimensionamento de pools, ordem global de locks,
│   │                            auditoria de happens-before, runbook "saldo não bate", pendências para a P2   (P1.05–P1.09, P1.13)
│   ├── java-modern-notes.md     feature × onde usar no PayFlow × onde NÃO usar                            (P1.11)
│   └── java-modules-notes.md    decisão "classpath, sem module-info"                                       (P1.12)
├── src/main/java/com/payflow/
│   ├── config/ExecutorConfig.java            ThreadPoolTaskExecutor "payflow-export" (fila limitada, AbortPolicy)           (P1.05)
│   ├── config/WarmupRunner.java              ApplicationRunner (payflow.warmup.enabled), só leitura                          (P1.02)
│   ├── exchange/ExchangeService.java         convertedBalance paralelo (CompletableFuture, orTimeout, mediana)             (P1.06)
│   ├── exchange/FxCallLimiter.java           Semaphore(5) com tryAcquire(200 ms) → 503                                       (P1.07)
│   ├── exchange/ExchangeRateCache.java       ConcurrentHashMap + computeIfAbsent (sem rede dentro da função)                (P1.07)
│   ├── security/LoginThrottle.java           tryBegin/endFailure/endSuccess atômicos (corrige o verifica-e-age da J8.06)     (P1.09)
│   ├── security/ThrottledAuthenticationProvider.java   reserva ANTES do BCrypt, libera em finally                           (P1.09)
│   └── transfer/TransferResult.java          sealed interface + records; switch exaustivo em ApiErrors                       (P1.11)
├── src/test/java/com/payflow/
│   ├── security/LoginThrottleConcurrencyTest.java        60 threads, ≤ 5 senhas testadas (falha na versão antiga)             (P1.09)
│   ├── transfer/ConcurrentDepositsTest.java              lição de casa da J5.11: N depósitos simultâneos e 409                (P1.13)
│   └── transfer/ConcurrentTransfersInvariantsTest.java   10 contas, 16 threads, retry com a mesma chave; invariantes 1–4      (P1.13)
├── compose.yaml                 + JAVA_TOOL_OPTIONS: -XX:MaxRAMPercentage=75 -Xlog:gc:stdout:time,uptime,level,tags
│                                  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=<volume> -XX:+ExitOnOutOfMemoryError    (P1.01, P1.03)
└── projeto/adrs/0013-concorrencia-do-saldo.md   (já redigido nesta geração; preencher as medidas reais no fim do treinamento)
```
**Nenhuma dependência nova** no `pom.xml` (só `java.base` e o que já existe). **Não** ligar `NativeMemoryTracking` por padrão (só para investigar).

Critérios de pronto (todos **a validar no fim do treinamento**):
- [ ] **Notas:** os quatro arquivos de `docs/` escritos, com **números medidos no PayFlow** onde a aula pediu medida (tabela de memória, coletor por CPU, tempo de subida com/sem *warmup*).
- [ ] **Pools:** `ExecutorConfig` com fila **limitada**, `AbortPolicy` e `threadNamePrefix`; o endpoint de exportação responde `202`, e `429`/`503` (Problem Details) quando recusa; **nenhuma exceção engolida** (log `WARN` sem dado pessoal).
- [ ] **Câmbio paralelo:** `ExchangeService`/`FxCallLimiter`/`ExchangeRateCache` sem rede dentro de `@Transactional` nem dentro de `computeIfAbsent`; `RestClient` com *timeouts* **menores** que o `orTimeout`; 503 com Problem Details quando há menos de 2 cotações.
- [ ] **`LoginThrottle`:** `LoginThrottleConcurrencyTest` **falha** na versão antiga (confirme!) e **passa** na corrigida; os testes antigos do `LoginThrottle` (J8) continuam verdes; a reserva **não vaza** nos três caminhos (sucesso, `BadCredentialsException`, outra exceção).
- [ ] **Invariantes sob carga:** `ConcurrentTransfersInvariantsTest` verde com `@Version` + *retry*; **falha** quando você remove temporariamente o `@Version` **e** a verificação de saldo (prova de que o teste detecta a corrida); `ConcurrentDepositsTest` conta os `ObjectOptimisticLockingFailureException`.
- [ ] **Conciliação:** consulta SQL da regra 3 (`saldo = Σ lançamentos`) documentada no *runbook* e rodada contra o seed; resultado **0** linhas divergentes.
- [ ] **JVM no contêiner:** `docker compose up` com as novas opções; `Using G1` no log; `--cpus 1` mostra **Serial** (confira); `docker kill --signal=QUIT` imprime *thread dump*; `docker stop` continua < 2 s e `143` (J9).
- [ ] **Virtual threads:** a decisão `spring.threads.virtual.enabled` **documentada** (pré-requisitos: Hikari dimensionado, `Semaphore` no câmbio, nada de rede dentro de transação); se ligar, p99 e conexões ativas **antes/depois** anotados.
- [ ] **Java moderno:** `TransferResult` *sealed* com `switch` **sem `default`** e o contrato HTTP **inalterado** (J6/decisão 43).
- [ ] **Testes existentes continuam como estavam** (esperado; **não executado**).
- [ ] Commits em Conventional Commits (`feat(concurrency): …`, `fix(security): close LoginThrottle check-then-act race`, `docs: …`, `test: …`), PR mesclada e tag:
```bash
git tag payflow-P1
git push origin payflow-P1
```

## Parte 3: Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho). A linha da matriz é **"Linguagem Java — Pleno: explica heap, GC, JIT; usa `CompletableFuture` e `java.util.concurrent`; reproduz e corrige condição de corrida"**; os itens abaixo a detalham.

| Competência (nível Pleno, módulo P1) | 0 | 1 | 2 |
|---|---|---|---|
| Explico carregamento de classes e as áreas de memória e associo cada uma ao seu erro | ☐ | ☐ | ☐ |
| Separo heap de memória do processo e investigo um 137 (NMT, *heap dump*, `MaxRAMPercentage`) | ☐ | ☐ | ☐ |
| Explico o JIT (C1/C2, OSR, desotimização), o aquecimento e por que `nanoTime()` não é *benchmark* | ☐ | ☐ | ☐ |
| Comparo G1, Parallel e ZGC e **leio** um log de GC (pausa × ciclo, *Full GC*, *humongous*) | ☐ | ☐ | ☐ |
| Uso `ExecutorService`/`ThreadPoolExecutor` com fila limitada, rejeição e dimensionamento | ☐ | ☐ | ☐ |
| Componho tarefas com `CompletableFuture` (erros, *timeouts*, executor próprio) | ☐ | ☐ | ☐ |
| Escolho entre `synchronized`, `ReentrantLock`, atômicos, `LongAdder` e coleções concorrentes | ☐ | ☐ | ☐ |
| Raciocino com *happens-before* e `volatile` e reconheço publicação insegura | ☐ | ☐ | ☐ |
| **Reproduzo** e **corrijo** uma condição de corrida e um *deadlock*, e **provo** com invariantes | ☐ | ☐ | ☐ |
| Decido entre threads de plataforma, virtuais e *structured concurrency* (e sei o que é *preview*) | ☐ | ☐ | ☐ |
| Leio e escrevo Java moderno (`record`, `sealed`, `switch` por padrões, `_`, construtor flexível) | ☐ | ☐ | ☐ |
| Explico módulos Java, `--add-opens`, `jdeps` e `jlink`, e por que o PayFlow roda no classpath | ☐ | ☐ | ☐ |

**Regra:** com mais de uma linha em 0, repita as aulas correspondentes antes da [P2](../P2-banco-persistencia-avancados/README.md). Atualize `00-guia/progresso.md`. **Atenção:** a P2 assume que você **entende** `@Version`, *check-then-act*, ordem de locks e `409` (aulas P1.09 e P1.13).

## Próximo
[P2: Banco de dados e persistência avançados](../P2-banco-persistencia-avancados/README.md)
