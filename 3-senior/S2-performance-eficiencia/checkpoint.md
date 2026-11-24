# S2.CP — Checkpoint do módulo S2

> ⏱️ ~1h30 · 🟣 Sênior · Pré-requisitos: S2.01 a S2.08

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, OpenJDK **25.0.5**, 12 núcleos, **máquina compartilhada**): os programas de [`lab/`](lab/README.md) (`java X.java`), o projeto **JMH** (`lab/jmh-lab`, JMH 1.37), o **k6 1.8.3**, o **GraalVM CE 25** e o **Azul Zulu 25 com CRaC** em Docker. **São modelos e programas pequenos** (um "serviço" com `Thread.sleep`, um cache com chave errada, uma aplicação com 3 defeitos plantados): **os números não valem para o PayFlow, o PostgreSQL ou um Spring Boot**. **Não executado:** o **PayFlow do aluno** (`perf/`, `perf-bench/`, `docs/perf/*`, as *flags* de produção, o evento `payflow.Transfer`, o `perf.yml`), o **async-profiler**, o **JMC**, o **MAT/VisualVM**, o **Gatling**, Spring Boot com cache AOT/CRaC/native e a **nuvem**. O **incidente da Parte 4 é SINTÉTICO** (mecanismos reais do laboratório, números **inventados**).

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Quais os **cinco pecados** da otimização sem método? Por que o **primeiro número mente** (que razão o laboratório mediu) e por que **uma** medição não basta?
2. Por que a **média** esconde a cauda? No modelo (98% ~8 ms, 2% ~300 ms), quanto deram **média, p50 e p99**, e que fração das **páginas com 100 chamadas** pegou ao menos uma acima do p99? Por que **não** se faz **média de p99**?
3. Uma fase ocupa **93%** do tempo e outra **0,1%**. O que rende **acelerar cada uma 10×** (fórmula e resultado)? O que isso diz sobre **onde** olhar primeiro?
4. No `ProfileApp`, qual **view do JFR** achou cada defeito (regex recompilada, `String +=`, trabalho dentro do `synchronized`) e por que o **perfil de CPU não viu a espera**? Qual a diferença entre `default` e `profile`?
5. O que o experimento do **viés de amostragem** mostrou **nos três cenários**? Por que "o `jstack` é sempre enganoso" **não** se sustentou e qual *flag* melhora a atribuição?
6. **`OutOfMemoryError` × `OOMKilled`**: a diferença e o que cada um deixa. No `LeakLab`, como você **achou o dono**: que **cadeia** o `OldObjectSample` mostrou e por que o histograma **sozinho** não basta? Por que o *heap dump* é **dado sensível**?
7. No `GcLab`, por que o **p50 não distinguiu** os coletores? O que aconteceu com **G1 em 700 MB** (para 600 MB vivos) e com **ZGC em 700 MB**? Qual a **ordem** do ajuste de GC?
8. **Carga aberta × fechada**: o que a **mesma falha de 3 s** produziu em cada uma (p99) e como se chama o efeito? Como se calcula a **capacidade** do servidor do laboratório, onde ficou o **joelho** e o que o **k6** devolveu a 400 e a 900 req/s?
9. Na escada de **partida**, ordene (padrão, AppCDS, cache AOT, nativo) com os **tempos**; o que o **nativo perdeu** e quanto? Por que o **CRaC** falhou na primeira tentativa?
10. No JMH, por que o `deadCode` deu **~0,23 ns** e o que é o `baseline`? Quanto custou o `BigDecimal` por soma frente ao `long`, por que isso **não** justifica trocar, e por que o **`double`** é proibido para dinheiro? Quando o JMH **não** responde?

<details><summary>Gabarito</summary>

1. **(1)** sem linha de base; **(2)** várias mudanças de uma vez; **(3)** condição diferente da produção (banco vazio, `localhost`); **(4)** uma execução só; **(5)** não parar quando o objetivo foi atingido. A 1ª execução custou **~6×** a de regime (8,8 ms contra ~1,4 ms): a JVM **interpreta e compila** (C1/C2). **Uma** medição pega **aquecimento e ruído** (15 repetições da mesma carga: **mediana 19,2 ms, máximo 30,2 ms, desvio 16%**).
2. A média (**15,3 ms**) **não descreve ninguém**: **p50 = 8,0 ms, p99 = 357 ms**. Com 100 chamadas por página, **63,6%** pegaram ao menos uma acima do p99 (teoria: `1 − 0,99¹⁰⁰ = 63,4%`). **Percentil não é linear**: a média de dois p99 **não é** o p99 do conjunto: some **histogramas**.
3. `ganho = 1/((1−f) + f/k)`: fase de **93%** → **6,2×**; fase de **0,1%** → **1,001×**. O teto é a **fração `f`**: **meça onde está o tempo antes** de otimizar.
4. **`hot-methods`**: `reportBad` (~35%) e a fileira de `java.util.regex.Pattern.*`; **`allocation-by-site`**: `Unsafe.allocateUninitializedArray` (~63%, os `byte[]` do `+=`) e a máquina de regex; **`contention-by-site`**: **uma** linha, `countBad`, **milhares** de esperas de **ms**. O perfil de CPU só vê *threads* **executando**; espera é **outro** evento. `default`: amostra a pilha a **20 ms**; `profile`: **10 ms** e limiares menores (mais completo, mais caro: custo **não medido**).
5. **Padrão:** poor-man (`Thread.getStackTrace`) e JFR **acertaram** (~88%/12%). **Sem *polls* em laços contados** (`-XX:-UseCountedLoopSafepoints`): **os dois erraram** (tudo no lambda: o JIT fundiu os métodos). **Com `-XX:+DebugNonSafepoints`:** o **JFR** voltou a acertar (~89%/11%) e o **poor-man** continuou errado. Medir importa mais que repetir o mito; **`DebugNonSafepoints`** melhora a atribuição (JFR e async-profiler).
6. `OutOfMemoryError` = a **JVM** sem heap (deixa *dump*); `OOMKilled` = o **kernel** matando o contêiner (limite total estourado; **sem *dump***). **Cadeia:** `LeakLab` (classe) → campo estático **`CACHE`** → `HashMap` (**Size 48000**) → `Node[]` → `HashMap$Node` → `Session` → **`byte[2048]`**, e a pilha de alocação (`LeakLab.handle`, linha 27). O histograma diz **o quê**, não **quem** (precisa do **caminho até a raiz** ou do **dominador**). O *dump* tem **tudo que estava na memória** (dados pessoais, tokens): **LGPD**.
7. Quase nenhuma requisição pega pausa: a mediana é o **caminho feliz** (~0,15 ms em todas). **G1 700 MB:** ~**6×** as coletas, **p99,9 ~60 ms**, +45% de CPU (folga **×1,2**). **ZGC 700 MB:** `OutOfMemoryError` e, quando terminou, p99 de **centenas de ms** (sem folga o alocador **espera**). **Ordem:** medir → **alocar menos** → **folga** → **coletor** → *flags* (uma por vez, A/B).
8. **Fechado (12 usuários):** p50 21 ms e **p99 ≈ 200 ms** (só ~12 requisições presas); **aberto (400 req/s):** **p99 ≈ 3 s**. É a **omissão coordenada** (o gerador **espera** o servidor e **omite** as medições do período ruim). **Capacidade:** `N/W = 16/0,0218 ≈ 734 req/s`; **joelho ~700** (p95 46–61 ms); a **800** a fila cresce (p50 ~400–460 ms). **k6:** 400 req/s **aprovou (exit 0)**; 900 req/s **reprovou os limiares (exit 99)**, faltaram VUs e descartou iterações; vazão presa em ~725/s.
9. **Padrão ~227 ms → AppCDS ~159 → cache AOT ~116 → nativo ~11 ms** (RSS 84 → 27 MB). O nativo **perdeu pico**: **~2,7× mais lento no regime** no `PeakApp` (**sem JIT/PGO**, GC Serial) e a compilação levou ~44 s e ~1,9 GB. O CRaC falhou com `BusySelectorException`: **não pode haver *socket* de escuta aberto no checkpoint**; um `Resource` **fecha antes** e **recria depois** (o Spring faz isso com `Lifecycle`).
10. O JIT **apagou** o cálculo (resultado não consumido): `deadCode ≈ baseline` (~0,23 ns, o **piso do arcabouço**). `BigDecimal` custou **~4 ns por soma** (~40 µs para 10.000) × `long` ~0,23 ns (**~17×**) e **aloca 40 B por soma**; num extrato de 50 lançamentos são ~0,2 µs frente a ~1 ms do banco (**Amdahl**). O `double` é mais rápido e **não garante** o resultado: o `MoneyCheck` mostra que a soma de 10 mil valores **acertou**, mas **`10 × 0,1 ≠ 1,0`**, o desvio acumulado de 1.000.000 × R$ 0,10 (**1,3 milionésimo**) e **1,005 arredondado para 1,0** (`BigDecimal`: 1,01): **proibido** (decisão 4). O JMH **não** responde sobre o **sistema** (p99: teste de carga), **concorrência real**, dados reais, partida e outra máquina.
</details>

## Parte 2: Entrega prática (tag `payflow-S2`)
O `payflow` ganha **o método de performance**, o **profiling**, a **política de memória**, as **regras de GC**, o **teste de carga em modelo aberto**, o **critério de partida**, os **microbenchmarks** e a **ADR 0025**. **Nenhuma migração** (a **V18** continua livre) e **contrato HTTP `/api/v1` inalterado**.
```
payflow/
├── docs/perf/
│   ├── orcamento.md        alvos de p99/taxa por endpoint (PROPOSTA, a validar na S2.06) + ambiente de referencia      (S2.01)
│   ├── experimentos.md     registro EXP-nnn (sintoma, ambiente, base, hipotese, mudanca, depois, reverter)            (S2.01)
│   ├── profiling.md        roteiro jcmd JFR.start, 4 views, evento payflow.Transfer, quando usar async-profiler       (S2.02/S2.03)
│   ├── memoria.md          flags, volume do heap dump, alerta do piso do heap, politica de dump, TTL+tamanho nos caches (S2.04)
│   ├── gc.md               baseline de GC, regra de decisao para ZGC, procedimento A/B                                (S2.05)
│   ├── carga.md            mistura de trafego (premissa), ambiente, 5 tipos de teste, criterio de pronto da S2        (S2.06)
│   └── partida.md          como medir a partida do Spring Boot, escada (cache AOT -> CRaC -> nativo), gatilhos        (S2.07)
├── perf/
│   ├── README.md
│   ├── k6/payflow.js       cenarios abertos (leituras/escritas), thresholds __A_DEFINIR__, setup() com token          (S2.06)
│   └── stubs/              dubles de PSP/antifraude/e-mail com latencia declarada                                     (S2.06)
├── perf-bench/             modulo Maven SEPARADO (JMH 1.37): BalanceBench, EmailValidationBench, README               (S2.08)
├── src/main/java/com/payflow/shared/perf/TransferEvent.java   evento JFR payflow.Transfer (sem accountId)             (S2.02)
├── src/test/java/com/payflow/shared/perf/TransferEventTest.java   o evento e gravado e NAO tem dado pessoal           (S2.02)
├── k8s/ (manifests)        volume emptyDir do heap dump (sizeLimit >= heap maximo), JAVA_TOOL_OPTIONS com as flags     (S2.04)
└── .github/workflows/perf.yml    smoke (k6, curto) na PR; carga noturna; resultados arquivados                         (S2.06)
projeto/adrs/0025-performance-e-eficiencia-do-payflow.md   (já redigida nesta geração)
```
Critérios de pronto:
- [ ] **Método:** `orcamento.md` com os **5 *endpoints*** e alvos marcados **"proposta, a validar"** (**nenhum número inventado**), `experimentos.md` com **o modelo** e **uma entrada real** (a primeira linha de base do PayFlow) e a **regra** "sem linha de base não há PR de performance" no `CONTRIBUTING`.
- [ ] **Profiling:** JFR **`default` contínuo** nos manifestos; `profiling.md` com o roteiro **num *pod*** (cópia do `.jfr`); o evento **`payflow.Transfer`** **sem `accountId`**, **nome ou saldo** (teste que **falha** se o campo existir).
- [ ] **async-profiler:** o doc **explica por que não vai em produção `restricted`** e o roteiro da **réplica de teste**, com **versão e *checksum***.
- [ ] **Memória:** `-XX:+HeapDumpOnOutOfMemoryError` + `HeapDumpPath` **no volume** (**não** o disco do nó) + `ExitOnOutOfMemoryError`; **alerta do piso do heap** (regra PromQL **escrita e validada**); **política do *heap dump*** (quem acessa, onde guarda, retenção, como apaga); **todo cache** com **TTL, tamanho máximo e taxa de acerto**.
- [ ] **GC:** `gc.md` com a **baseline a coletar**, a **regra de decisão** para ZGC (**X% a definir com dados**) e o **A/B de 3 execuções**; **limite do *pod*** coerente com o **vivo medido** (conta da P7.02/P7.09).
- [ ] **Carga:** `payflow.js` com **modelo aberto** em **todos** os cenários, **limiares** que **reprovam** (exit ≠ 0), **token renovado**, **`Idempotency-Key` única**, **dublês** com latência declarada, **gerador fora** do sistema testado; `perf.yml` com **smoke na PR** e **carga noturna**.
- [ ] **Partida:** `partida.md` com a **medição** do Spring Boot (15 execuções, `readiness` verdadeiro) **e** os **gatilhos** para **cache AOT**, **CRaC** e **nativo** (**números a medir**).
- [ ] **JMH:** `perf-bench/` **separado**, **fora do CI**, com `baseline`, resultado **consumido**, `@State` e `README` com **comando, JDK, máquina e resultado**.
- [ ] **ADR 0025** revisada; tag `payflow-S2`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Sênior: **≥ 2** em todos e **3** nos marcados com ⭐.
| Competência (matriz) | Evidência no módulo | 0–3 |
|---|---|---|
| ⭐ **Performance:** mede com método (JFR, k6, percentis) e otimiza com evidência | S2.01, S2.02, S2.06 (ciclo e registro; `hot-methods`/`contention-by-site`; carga **aberta** e limiares) | |
| ⭐ **Linguagem Java:** diagnostica vazamento com *heap dump*, escolhe GC | S2.04, S2.05 (`OldObjectSample`; folga do heap; G1 × ZGC × Parallel × Serial medidos) | |
| ⭐ **Observabilidade e operação:** investiga com evidência | S2.02, S2.03, S2.04 (CPU × espera; piso do heap; evento próprio) | |
| **Lógica e estruturas de dados:** mede custo com JMH (lacuna do Pleno) | S2.08 (`BigDecimal` × `long` × `double`; armadilhas; `baseline`) | |
| **Testes e qualidade:** define estratégia de carga e de regressão de performance | S2.06, S2.08 (smoke na PR, carga noturna, JMH **fora** do CI) | |
| **Containers e orquestração:** JVM sob limites, partida e memória | S2.04, S2.05, S2.07 (`OOMKilled`; GC em *pod* apertado; escada de partida) | |
| **Banco de dados:** efeito da carga no pool e no banco | S2.06 (capacidade `N/W`; conta de conexões retomada) | |
| **Segurança:** dados sensíveis em artefatos de diagnóstico | S2.04, S2.07 (*heap dump* e *snapshot* CRaC são dados pessoais) | |
| **Liderança e comunicação:** registra decisões com gatilhos e critério de reversão | S2.01, S2.07 (ADR 0025: nativo/ZGC/CRaC **não** adotados, com gatilhos) | |
Se **≥ 2** em tudo e **3** nos ⭐ → siga para o **S3**. Linhas em **0–1**: refaça a aula indicada e o **laboratório** correspondente.

## Parte 4: Simulado: uma investigação de performance (60 min, **com consulta ao cheatsheet**)
> ⚠️ **Incidente SINTÉTICO:** os **números** abaixo são **inventados** para o exercício; os **mecanismos** (cache com chave errada, trabalho dentro de `synchronized`, alocação por requisição) vêm das aulas S2.02 e S2.04, **medidas** no laboratório. **Nunca** cite este dossiê como medição.

**Cenário.** Após o *release* v1.14 do PayFlow, o p99 de `GET /accounts/{id}/statement` subiu de **~90 ms** para **~650 ms** (o p50 mal mudou). **Dados disponíveis:**
- **CPU** do *pod*: ~22% (estável). **Pausas de GC**: ~12 ms (como antes). **HikariCP:** *pending* 0–3.
- **Heap pós-GC (piso):** de **~480 MB** para **~720 MB** em **6 h** (limite de heap: 1 GiB); o time **reinicia** o *pod* toda noite "porque o heap enche".
- **JFR `profile`, 5 min, em horário de pico:** `hot-methods`: **nada gordo** (topo ~4%); `allocation-by-site`: **`ObjectMapper.<init>` 38%** da pressão; `contention-by-site`: **`StatementFormatter.format` 3.100 esperas, média 18 ms, máximo 160 ms**.
- **Código da v1.14 (resumo):** (a) `StatementFormatter` usa um `SimpleDateFormat` **estático** dentro de `synchronized`; (b) o serializador cria `new ObjectMapper()` **por requisição**; (c) um `static Map<String, Statement> CACHE` com **chave `sub + ":" + requestId`**.
- O time propõe: **"migrar para ZGC e para *native image*"**.

**Tarefas:**
1. **Hipóteses ordenadas** pela **evidência** (qual explica o p99? qual explica o heap? qual explica a alocação?) e **o que cada dado descarta** (CPU, GC, Hikari).
2. **Comandos/ferramentas** para **confirmar** cada hipótese (JFR, `jcmd`, histograma, `OldObjectSample`, ...), na ordem e com **o que esperar**.
3. **Plano de experimentos** (**uma mudança por vez**, linha de base, mesmo ambiente, repetição) e **o registro EXP** da primeira correção.
4. **Responda à proposta** de ZGC e nativo com **critério** e **gatilho** (ADR 0025).
5. **O que você faz sobre o reinício noturno** e **sobre o *heap dump*** (política).

**Rubrica (100 pontos):** hipóteses **ligadas à evidência** e **ordenadas** (**30**); **ferramentas e o que esperar** (**20**); **plano de experimentos** com linha de base, **uma mudança por vez**, repetição e critério de sucesso (**20**); **resposta à proposta** de ZGC/nativo com **critério** (**15**); **registro** (EXP) e **política** do dump/reinício (**15**). **Aprovado: ≥ 70.**
<details><summary>Gabarito (resumo)</summary>

**Hipóteses:** **(1) contenção** em `StatementFormatter.format` (`synchronized` + `SimpleDateFormat`): explica o **p99 alto com CPU baixa** e o **p50 quase igual** (só quem pega o *lock* espera; 3.100 esperas de 18 ms, máx. 160 ms); **(2) vazamento** do `CACHE` (chave com `requestId`: **nunca acerta, nunca sai**): explica o **piso do heap subindo** (480 → 720 MB em 6 h) e o **reinício noturno** que "resolve"; **(3) `ObjectMapper` por requisição**: explica os **38%** de alocação (e mais GC/CPU) mas **não** o p99 sozinho. **Descartado:** **CPU** (22%) e **GC** (pausas iguais, 12 ms) e o **pool** (*pending* ≈ 0): o gargalo **não** é capacidade nem coletor.
**Confirmação:** **`jfr view contention-by-site`** (já) + `jfr print --events jdk.JavaMonitorEnter --stack-depth 8` (**quem** segura o *lock*); **2 `jcmd GC.class_histogram`** (5–10 min entre eles: `Statement`/`HashMap$Node` **crescendo**) e **`OldObjectSample` com `path-to-gc-roots=true`** (a cadeia **`CACHE` → `HashMap` → `Statement`**); `allocation-by-site` com **pilha** (`jfr print --events jdk.ObjectAllocationSample`) para o `ObjectMapper`.
**Experimentos (um por vez, ordem pelo impacto no p99):** **EXP-1** `DateTimeFormatter` **imutável** (`static final`, *thread-safe*), **sem `synchronized`**; **EXP-2** chave do cache **só `sub`** (+ versão) com **TTL e tamanho máximo** (Caffeine) e **métrica de acerto**; **EXP-3** `ObjectMapper` **único**. **Cada um:** linha de base (≥ 3 execuções de carga **aberta**, mesma mistura), **mudança**, **depois**, **reverter com**. Sucesso: **p99 volta ≤ alvo** (proposta do `orcamento.md`), **contenção some**, **piso do heap plano por horas**.
**ZGC/nativo:** **não**. O GC **não** é parte do problema (pausas iguais, 12 ms): trocar de coletor **não** atacaria nenhuma das três causas; o **nativo** troca **pico por partida** (medido: ~2,7× mais lento no regime) e **a partida não é o gargalo**. **Gatilhos** para reabrir: pausa de GC explicando parte do p99 **e** folga de memória/CPU; HPA atrasando o p99; *cold start* fora do orçamento.
**Reinício noturno e dump:** é **mitigação que apaga a evidência**: **antes** do próximo reinício, **um `GC.heap_dump -gz`** (numa **réplica**, fora do pico), **tratado como dado pessoal** (acesso restrito, criptografia, retenção curta, **nunca** num *ticket*); depois da correção, **remover o reinício** e **provar** com o piso do heap plano. **EXP-1 (exemplo):** sintoma p99 650 ms; ambiente; base (3 execuções); hipótese **com a evidência da contenção**; mudança **uma**; depois; conclusão; reverter com o *commit*.
</details>

## ➡️ Próximo
[S3 — Dados em escala](../S3-dados-em-escala/README.md)
