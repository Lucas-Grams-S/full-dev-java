# Cheatsheet — Performance e eficiência (S2)

| Tema | Regra / comando | Onde |
|---|---|---|
| Método | medir → hipótese → **uma** mudança → medir de novo; sem linha de base não há PR de performance | S2.01 |
| Percentis | p50/p95/p99/p99,9 **e `n`**; nunca só média; **nunca média de percentis** (some histogramas) | S2.01 |
| Cauda × chamadas | `1 − p^N` (100 chamadas a p99 → **63%**) | S2.01 |
| Amdahl na requisição | `ganho = 1 / ((1 − f) + f/k)`; o teto é a fração `f` que a fase ocupa | S2.01 |
| Primeiro número mente | aquecimento do JIT (~6× no 1º); repita ≥ 15×, mediana, mesmo ambiente | S2.01 |
| JFR (na partida) | `java -XX:StartFlightRecording:filename=app.jfr,settings=profile,dumponexit=true -jar app.jar` | S2.02 |
| JFR (processo vivo) | `jcmd <pid> JFR.start name=x settings=profile duration=60s filename=/tmp/x.jfr` · `JFR.check` · `JFR.dump` · `JFR.stop` | S2.02 |
| JFR contínuo | `settings=default,maxage=6h,maxsize=1g,filename=...,dumponexit=true` (a validar) | S2.02 |
| Ler o `.jfr` | `jfr summary` · `jfr view hot-methods\|allocation-by-site\|contention-by-site\|gc-pauses\|memory-leaks-by-site` · `jfr print --events X --stack-depth N` | S2.02 |
| CPU × espera | perfil de CPU **só vê o que executa**; CPU baixa + latência alta ⇒ `contention-by-site`, I/O, pool | S2.02 |
| Evento próprio | `class X extends jdk.jfr.Event` + `@Name/@Label/@Category`; `begin/end/commit`; **sem dado pessoal** | S2.02 |
| Atribuição | `-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints` (JIT guarda informação fora dos *safepoints*) | S2.03 |
| async-profiler | `asprof -d 30 -f flame.html <pid>` (cpu) · `-e wall -t -i 50ms` · `-e alloc` · `-e lock -t` · `-e cpu,alloc,lock -f x.jfr` (a validar) | S2.03 |
| Flame graph | largura = proporção de amostras; platôs largos no topo; eixo X **não** é tempo | S2.03 |
| Tipos de falta de memória | `Java heap space` · `Metaspace` · `Direct buffer memory` · `native thread` · **`OOMKilled`** (kernel, sem *dump*) | S2.04 |
| Vazamento | **piso do heap pós-GC sobe**; 2 histogramas e compare | S2.04 |
| Histograma ao vivo | `jcmd <pid> GC.class_histogram` (só alcançáveis; `-all` inclui o resto) | S2.04 |
| Heap dump | `jcmd <pid> GC.heap_dump [-gz] arq.hprof` · `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=<volume>` · **é dado pessoal** | S2.04 |
| Quem segura | `OldObjectSample` + `path-to-gc-roots=true`; dominador/retido no MAT | S2.04 |
| Ordem do ajuste de GC | medir → alocar menos → folga no heap → coletor → *flags* (uma por vez, A/B) | S2.05 |
| Heap apertado | ~6× as coletas, p99,9 ~60 ms (modelo); ZGC sem folga **falha** | S2.05 |
| Coletores (modelo) | p50 igual; p99: G1 0,28 ms · Parallel ~28 · Serial ~50 · ZGC p99,9 0,3 ms | S2.05 |
| Carga aberta × fechada | aberta = taxa fixa (API pública); fechada **omite** o período ruim (omissão coordenada) | S2.06 |
| Capacidade | `N / W` (pool ÷ tempo de serviço); o joelho vem **antes** (~95%) | S2.06 |
| k6 aberto | `executor: 'constant-arrival-rate', rate, timeUnit, duration, preAllocatedVUs, maxVUs` | S2.06 |
| k6 no CI | `thresholds` (`http_req_duration: ['p(99)<X']`) ⇒ código de saída **99** se falhar | S2.06 |
| k6 em Docker | `docker run --rm --network host -i -e RATE=400 grafana/k6:1.8.3 run - < script.js` | S2.06 |
| Teste realista | dados e volume reais, mistura declarada, token renovado, chave de idempotência única, dublês com latência, gerador fora | S2.06 |
| Cache AOT (JDK 25) | `java -XX:AOTCacheOutput=app.aot -cp ...` (treino) · `java -XX:AOTCache=app.aot -cp ...` | S2.07 |
| Native image | `native-image -cp app.jar Main -o app --no-fallback`; troca **pico por partida** (~2,7× mais lento no modelo) | S2.07 |
| CRaC | `-XX:CRaCCheckpointTo=dir` · `jcmd <pid> JDK.checkpoint` · `-XX:CRaCRestoreFrom=dir`; **fechar sockets antes**; *snapshot* sensível | S2.07 |
| JMH | `@Benchmark` + `@State` + `@Param`; consumir o resultado; comparar com `baseline`; `-f 5` para decidir | S2.08 |
| JMH: armadilhas | código morto (`≈ baseline`), constantes dobradas, um *fork* só, intervalos que se sobrepõem | S2.08 |
| `BigDecimal` | ~4 ns por soma (modelo); **nunca `double`** para dinheiro | S2.08 |
