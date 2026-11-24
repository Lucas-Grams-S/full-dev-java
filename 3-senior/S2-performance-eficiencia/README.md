# S2 — Performance e eficiência

> 🟣 Sênior · ⏱️ 13,5h (9 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **validado na execução final (2026-11-24); veja o bloco abaixo**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído em `~/docs/payflow`, tag `payflow-S2`).** **Executado:** reexecutado (Java 25.0.5): `run-profile.sh` (S2.02/S2.03: os 3 defeitos plantados continuam os métodos dominantes no JFR; contagens de eventos na mesma ordem de grandeza), `run-heap.sh` (S2.04: o cache com chave errada vaza até **cache=48000 / acertos=0 / ~105 MB** e a correção fica em **cache=500 / ~22 MB**; o *heap dump* e o `OldObjectSample` aparecem) e `MoneyCheck` (S2.08: saídas **idênticas**: o `double` acerta a soma de 10 mil valores e erra a de 1 milhão × R$ 0,10). **Corrigido nas aulas:** nenhuma divergência de conteúdo (os números de tempo/contagem variam, como as aulas avisam). **Ainda não executado:** `run-gc.sh` (7 min), `run-load.sh` (k6 em Docker), `run-startup.sh`/`run-peak.sh`/`run-crac.sh` (GraalVM, Zulu CRaC), `run-jmh.sh` e, no PayFlow, a linha de base, o k6 até o joelho, o JFR contínuo e o *async-profiler*. Os avisos "não executado" / "revisado por leitura" nas aulas descrevem a escrita original.

## 🎯 Objetivo do módulo
**Medir, diagnosticar e otimizar com método**: seguir o ciclo **medir → hipótese → mudar uma coisa → medir de novo** (e **recusar** otimização sem linha de base), ler **percentis** em vez de média, **achar** o gargalo com **JFR** e **async-profiler** (CPU **e** espera), **diagnosticar vazamentos** com histogramas, *heap dump* e `OldObjectSample`, **ajustar o GC** só com evidência, **testar carga** com o **modelo aberto** (e evitar a omissão coordenada), **decidir** entre JVM padrão, **cache AOT**, **CRaC** e ***native image*** e **provar** ganhos de trechos de código com **JMH**.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [S2.01](S2.01-metodologia-medir-antes-de-otimizar.md) | Metodologia: medir antes de otimizar | 1h30 |
| [S2.02](S2.02-profiling-com-jfr-e-jdk-mission-control.md) | Profiling com JFR e JDK Mission Control | 1h30 |
| [S2.03](S2.03-async-profiler.md) | async-profiler | 1h30 |
| [S2.04](S2.04-heap-dumps-e-memory-leaks.md) | Heap dumps e memory leaks | 1h30 |
| [S2.05](S2.05-tuning-de-gc.md) | Tuning de GC | 1h30 |
| [S2.06](S2.06-testes-de-carga-com-k6-ou-gatling-e-percentis.md) | Testes de carga com k6 ou Gatling e percentis | 1h30 |
| [S2.07](S2.07-graalvm-native-image-e-crac.md) | GraalVM native image e CRaC | 1h30 |
| [S2.08](S2.08-benchmarks-com-jmh.md) | Benchmarks com JMH | 1h30 |
| [S2.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): programas de **Java puro** (`java X.java`: `MethodLab`, `ProfileApp`, `BiasLab`, `FlameFold`, `LeakLab`, `HprofHisto`, `GcLab`, `LoadLab`, `MiniApp`, `PeakApp`, `CracApp`, `MoneyCheck`, `CustomEventDemo`), o **projeto Maven `jmh-lab`** (JMH 1.37) e **scripts** (`run-profile.sh`, `run-heap.sh`, `run-gc.sh`, `run-load.sh`, `run-startup.sh`, `run-peak.sh`, `run-crac.sh`, `run-jmh.sh`) que regravam as saídas `lab/out-s20*.txt`.

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório foi executado de verdade** (OpenJDK **25.0.5**, Linux, **12 núcleos, máquina compartilhada**) e **as saídas das aulas são as reais**. **São modelos e programas pequenos:** um "serviço" com `Thread.sleep`, um cache com chave errada, uma aplicação com três defeitos plantados, um servidor de 16 *threads*: **nenhum número vale para o PayFlow, para o PostgreSQL ou para um Spring Boot**; as aulas dizem **quando** o resultado **não** se transfere. Os números **variam** entre execuções (as aulas mostram quanto, ou dão **ordens de grandeza**). **Não executado:** o **PayFlow do aluno** (flags de produção, `perf/`, `perf-bench/`, evento `payflow.Transfer`, *workflow* de carga), o **async-profiler** (binário nativo: **não baixado**, só a documentação lida), o **JDK Mission Control**, o **Eclipse MAT/VisualVM**, o **Gatling**, **Spring Boot + cache AOT/CRaC/native**, **PGO**, NMT, CRIU (o CRaC rodou com o motor `warp`) e **qualquer ambiente de nuvem/Kubernetes**. As **imagens Docker** baixadas para o módulo (k6, GraalVM, Zulu CRaC) foram **removidas ao final**.

> **Avisos de ordem:** a S2.01 retoma **Amdahl**, a **Lei de Little** e a **conta de conexões** (S1.01) e a **taxa de acerto** (S1.04); a S2.02 e a S2.04 usam **JFR/GC** (P1.03, P1.04) e **memória em contêiner** (P7.02, P7.09); a S2.05 retoma a **seleção de coletor em contêiner** (P7.02) e o **throttling de CPU** (P7.05); a S2.06 retoma **RED/histogramas** (P6.03), **idempotência** (S1.08), **rate limit** (S1.09) e **tokens** (P10); a S2.07 retoma **cold start** (P9.07/P9.08) e o **HPA** (P7.06); a S2.08 **fecha a lacuna JMH** do Pleno (decisão 183) e retoma a **decisão 4** (dinheiro em `BigDecimal`).

## 🏗️ Entrega no PayFlow
**Tag `payflow-S2`** (por leitura): o **método** (`docs/perf/orcamento.md` com alvos **propostos**, `experimentos.md` com o registro de cada mudança, regra "**sem linha de base não há PR de performance**"), **profiling** (JFR `default` contínuo, roteiro de `jcmd JFR.start`, **evento `payflow.Transfer` sem dado pessoal**, async-profiler **só em réplica de teste**), **memória** (`-XX:+HeapDumpOnOutOfMemoryError` com **volume dedicado**, alerta do **piso do heap**, política do *heap dump* como **dado sensível**, **TTL e tamanho máximo** em todo cache), **GC** (**G1 mantido**, regra de decisão para ZGC com **A/B**), **carga** (`perf/k6/payflow.js` em **modelo aberto**, **limiares**, **dublês** do PSP/antifraude, `perf.yml`: *smoke* na PR e carga noturna), **partida** (JVM padrão; **cache AOT a medir**; nativo e CRaC **não adotados**, com **gatilhos**), **microbenchmarks** (`perf-bench/` com JMH, **fora do CI**) e a **ADR 0025**. **Nenhuma migração** (a **V18** continua livre) e **contrato HTTP `/api/v1` inalterado**.

## ➡️ Próximo
[S3 — Dados em escala](../S3-dados-em-escala/README.md)
