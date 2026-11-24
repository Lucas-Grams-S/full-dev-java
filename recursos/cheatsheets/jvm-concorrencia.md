# Cheatsheet: JVM e concorrência (P1)

## Memória e *flags* (todas conferidas na P1; JDK 25)
| Quero | Opção / comando |
|---|---|
| Teto do heap / % da RAM do contêiner | `-Xmx512m` / `-XX:MaxRAMPercentage=75` (J9) |
| Pilha por thread | `-Xss512k` |
| Teto do metaspace | `-XX:MaxMetaspaceSize=256m` |
| Heap dump + reiniciar no OOM | `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps -XX:+ExitOnOutOfMemoryError` |
| Memória nativa (só para investigar) | `-XX:NativeMemoryTracking=summary` + `jcmd <PID> VM.native_memory summary` |
| Cabeçalho de objeto compacto (Java 25) | `-XX:+UseCompactObjectHeaders` (meça antes) |
| Cache AOT (Java 25; precisa de **JAR**) | `-XX:AOTCacheOutput=app.aot` (treino) · `-XX:AOTCache=app.aot` |
| Ver flags efetivas | `java -XX:+PrintFlagsFinal -version` |

## JIT
`-XX:+PrintCompilation` (colunas: tempo, id, flags `%` OSR, nível 0-4, método, `made not entrant`) · `-Xint` (só interpretador) · `-XX:TieredStopAtLevel=1` (só C1) · benchmark sério = **JMH**.

## GC
| Coletor | Flag | Observação |
|---|---|---|
| G1 (padrão com 2+ CPUs) | `-XX:+UseG1GC` | `-XX:MaxGCPauseMillis=200` é alvo; `-XX:G1HeapRegionSize` |
| Parallel | `-XX:+UseParallelGC` | vazão |
| Serial (escolhido com 1 CPU) | `-XX:+UseSerialGC` | heap pequeno |
| ZGC (geracional) | `-XX:+UseZGC` | precisa de folga de heap |
| Shenandoah | `-XX:+UseShenandoahGC` | concorrente |
Log: `-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=5,filesize=20m` · contêiner: `-Xlog:gc:stdout:time,uptime,level,tags` · pausas do ZGC: `-Xlog:gc,gc+phases=debug`.
Leitura: `Pause Young (Normal) (G1 Evacuation Pause) 48M->4M(302M) 3.862ms` (antes→depois(comprometido) e duração) · `Pause Full` = incidente · `Allocation Stall` (ZGC) = sem folga · pausa ≠ ciclo concorrente.

## Ferramentas numa JVM viva
`jcmd` (lista JVMs) · `jcmd <PID> Thread.print` (e mostra *deadlock*) · `jcmd <PID> GC.heap_info` · `jstat -gcutil <PID> 500 3` · `jcmd <PID> Thread.dump_to_file -format=text <arquivo>` (inclui virtuais) · em contêiner sem JDK: `docker kill --signal=QUIT <ctr>`.

## Escolha rápida de concorrência
| Preciso de | Use |
|---|---|
| rodar tarefas com limite | `ThreadPoolExecutor` com **fila limitada** e política de rejeição; nomeie as threads |
| várias chamadas I/O em paralelo e juntar | `CompletableFuture` (**com Executor próprio**) ou `StructuredTaskScope` (preview) |
| muitas tarefas bloqueantes | `Executors.newVirtualThreadPerTaskExecutor()` + `Semaphore` no recurso escasso |
| exclusão mútua simples / com `tryLock` | `synchronized` / `ReentrantLock` (`try/finally`) |
| contador | `AtomicLong` / `LongAdder` (estatística, alta contenção) |
| mapa compartilhado | `ConcurrentHashMap` + `merge`/`compute`/`putIfAbsent` (nunca `get`+`put`) |
| fila produtor/consumidor | `ArrayBlockingQueue` (limitada) |
| flag de parada | `volatile boolean` / `AtomicBoolean` / interrupção |
| contexto da requisição | `ScopedValue` (Java 25) em vez de `ThreadLocal` |
| invariante entre instâncias | **o banco**: `UNIQUE`, `WHERE balance >= ?`, `@Version`, `FOR UPDATE` (P2) |

## Armadilhas (cada uma foi reproduzida na P1)
`x++` perde incrementos (mesmo `volatile`) · `if (check) act` é *check-then-act* · `HashMap` concorrente corrompe · `submit` engole exceção · `cancel()` não pára o `CompletableFuture` · `commonPool` + I/O bloqueante · fila ilimitada esconde sobrecarga · flag sem `volatile` trava o laço depois do JIT · `synchronized` fixava virtual thread no **Java 21** (não no 25) · lock em memória não vale com 2 instâncias.
