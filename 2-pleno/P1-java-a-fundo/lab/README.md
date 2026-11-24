# Laboratório da P1 — `jvm-lab`

Java **puro** (sem Maven, sem Docker). Requisitos: **JDK 25** (`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`; o `java` padrão do shell pode ser o 17). `Pinning.java` também foi rodado no JDK 21 (comparação). Cada arquivo roda com `java Arquivo.java <modo>` (modo *source launcher*); onde a aula pede, compile antes.

| Arquivo | Aula | Modos / uso |
|---|---|---|
| `MemoryAreas.java` | P1.01 | `loaders` (compile antes: `javac -d /tmp/p101 … && java -cp /tmp/p101 MemoryAreas loaders`), `init`, `stack` (`-Xss`), `heap` (`-Xmx32m`), `metaspace` (`-XX:MaxMetaspaceSize=16m`), `numbers` |
| `ObjectSize.java` | P1.01 | tamanho de `new Object()` (com e sem `-XX:+UseCompactObjectHeaders`) |
| `Warmup.java` | P1.02 | `batches` (também com `-Xint` e `-XX:TieredStopAtLevel=1`; `-XX:+PrintCompilation`), `poly` |
| `GcLoad.java` | P1.03/P1.04 | `java -Xmx512m -XX:+Use{G1,Parallel,Serial,Z,Shenandoah}GC -Xlog:gc:file=x.log GcLoad.java 8 [humongous]` |
| `Threads.java` | P1.05 | `states`, `interrupt`, `errors`, `pool`, `reject`, `dump` (use `jcmd <PID> Thread.print`) |
| `Futures.java` | P1.06 | `chain`, `fanout`, `errors`, `timeout`, `pitfall`, `median` |
| `Concurrent.java` | P1.07 | `locks`, `counters`, `maps`, `lists`, `queue`, `semaphore` |
| `Visibility.java` | P1.08 | `hang`, `hang-fix`, `hang-println` (também com `-Xint`), `litmus`, `litmus-volatile` |
| `Races.java` | P1.09 | `lost-update`, `overdraft`, `conservation`, `deadlock`, `deadlock-fix`, `deadlock-trylock`, `throttle`, `throttle-fix` |
| `Virtual.java` | P1.10 | `basics`, `scale`, `cpu`, `threadlocal`, `scoped`, `semaphore` |
| `Pinning.java` | P1.10 | `java -Djdk.virtualThreadScheduler.parallelism=1 -Djdk.virtualThreadScheduler.maxPoolSize=1 Pinning.java` (rode no 21 e no 25) |
| `Structured.java` | P1.10 | `java --enable-preview --source 25 Structured.java all\|first\|fail\|timeout` (**preview**) |
| `Modern.java`, `Hello25.java`, `TransferResultDemo.java` | P1.11 | `java Modern.java`, etc. |
| `ReflectJdk.java`, `modules-lab/` | P1.12 | ver a aula (`javac --module-source-path modules-lab/src --module payflow.app,payflow.fee.flat`) |
| `StudyCase.java` | P1.13 | `java StudyCase.java all [hot]` (PayFlow em miniatura, **em memória**) |

**Avisos:** (1) os resultados **mudam** de execução para execução (corridas, GC, JIT, máquina compartilhada); (2) `Concurrent.java counters` e `Races.java` usam várias threads e podem deixar a máquina lenta por alguns segundos; (3) `GcLoad.java` aloca centenas de MiB; (4) nada aqui cria contêineres ou deixa processos: não há o que limpar.
