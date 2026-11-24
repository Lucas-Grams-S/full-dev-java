# P1 — Java a fundo

> 🔵 Pleno · ⏱️ 21h (14 sessões de 1h30) · Status: ✅ concluído (2026-11-13) — **laboratório executado (JDK 25.0.5); o PayFlow do aluno foi revisado por leitura; os testes práticos ficam para o fim do treinamento**

## 🎯 Objetivo do módulo
Entender a JVM e a concorrência a ponto de **diagnosticar problemas reais**: de onde vem cada memória, por que a primeira requisição é lenta, como ler um log de GC, como uma condição de corrida **destrói dinheiro sem lançar uma exceção** e como as *virtual threads* mudam o modelo.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [P1.01](P1.01-classloading-e-areas-de-memoria.md) | Classloading e áreas de memória (heap, stack, metaspace) | 1h30 |
| [P1.02](P1.02-jit-e-aquecimento.md) | JIT e aquecimento | 1h30 |
| [P1.03](P1.03-garbage-collectors-i-g1-e-parallel.md) | Garbage collectors I: G1 e Parallel | 1h30 |
| [P1.04](P1.04-garbage-collectors-ii-zgc-e-leitura-de-logs.md) | Garbage collectors II: ZGC e leitura de logs de GC | 1h30 |
| [P1.05](P1.05-threads-e-executorservice.md) | Threads e `ExecutorService` | 1h30 |
| [P1.06](P1.06-completablefuture.md) | `CompletableFuture` | 1h30 |
| [P1.07](P1.07-java-util-concurrent-locks-atomicos-e-colecoes-concorrentes.md) | `java.util.concurrent`: locks, atômicos e coleções concorrentes | 1h30 |
| [P1.08](P1.08-java-memory-model-volatile-e-happens-before.md) | Java Memory Model, `volatile` e `happens-before` | 1h30 |
| [P1.09](P1.09-condicoes-de-corrida-e-deadlocks-que-falham-de-verdade.md) | Condições de corrida e deadlocks que falham de verdade | 1h30 |
| [P1.10](P1.10-virtual-threads-e-structured-concurrency.md) | Virtual threads e structured concurrency | 1h30 |
| [P1.11](P1.11-recursos-modernos-de-java-8-ate-25.md) | Recursos modernos de Java 8 até 25 | 1h30 |
| [P1.12](P1.12-java-modules-visao-geral.md) | Java Modules (visão geral) | 1h30 |
| [P1.13](P1.13-estudo-de-caso-o-saldo-que-nao-bate.md) | 🏗️ Estudo de caso: o saldo que não bate | 1h30 |
| [P1.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório pronto em `lab/jvm-lab/` (Java puro, **sem Maven e sem Docker**: `java Arquivo.java`); veja [`lab/README.md`](lab/README.md).

> ⚠️ **Aviso de verificação (leia antes de estudar):** os **experimentos** (memória, JIT, os cinco coletores, threads, `CompletableFuture`, `java.util.concurrent`, memória e corridas, *deadlock*, threads virtuais e *pinning* no Java 21 × 25, *structured concurrency*, recursos modernos, módulos e `jlink`) foram **executados de verdade** no JDK 25.0.5 (Linux, 12 núcleos, **máquina compartilhada**: tempos oscilam). **Não executado:** **tudo** o que toca no **PayFlow do aluno** (classes e testes novos: revisados **por leitura**), o **PostgreSQL** (o estudo de caso P1.13 é um **modelo em memória**), `spring.threads.virtual.enabled` e a JVM **em contêiner** com limites. A *structured concurrency* é **preview** no Java 25.

> **Avisos de ordem:** P1.01 a P1.04 usam só J1/J9; P1.05–P1.07 pedem J5.11 (`@Transactional` preso à thread) e J6.08 (`RestClient`); P1.09 e P1.13 reutilizam o `LoginThrottle` (J8.06), o `@Version` (J5.08) e a idempotência (J6.03); P1.12 só **antecipa** `Spring Modulith`/`ArchUnit` (P4) como menção. **Nenhum conceito de P2+ é pré-requisito** (locking pessimista, isolamento, Redis, mensageria aparecem só como destino).

## 🏗️ Entrega no PayFlow
`docs/jvm-notes.md`, `concurrency-notes.md`, `java-modern-notes.md`, `java-modules-notes.md`; `ExecutorConfig`, `WarmupRunner`, `ExchangeService` (paralelo), `FxCallLimiter`, `ExchangeRateCache`, `LoginThrottle` corrigido, `TransferResult` (*sealed*); testes `LoginThrottleConcurrencyTest`, `ConcurrentDepositsTest`, `ConcurrentTransfersInvariantsTest` (condição de corrida **reproduzida, corrigida e provada** com invariantes); opções de JVM no `compose.yaml`; ADR 0013. Tag `payflow-P1`. **Tudo por leitura** (ver checkpoint).

## ➡️ Próximo
[P2 — Banco de dados e persistência avançados](../P2-banco-persistencia-avancados/README.md)
