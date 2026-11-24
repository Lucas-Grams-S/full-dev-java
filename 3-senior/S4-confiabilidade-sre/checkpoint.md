# S4.CP — Checkpoint do módulo S4

> ⏱️ ~1h30 · 🟣 Sênior · Pré-requisitos: S4.01 a S4.07

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, JDK 25.0.5, **máquina compartilhada de 12 núcleos**; Docker só para o `promtool` da imagem `prom/prometheus`): `lab/SloLab.java` (orçamento e comparação de estratégias de alerta), `lab/ChaosLab.java` (experimento de caos em miniatura com latência injetada), `lab/DocLint.java` + `lab/check-runbooks.sh` (verificação de *runbooks* e postmortems, com auto-teste), `lab/RpoRtoLab.java`, `lab/AzLab.java`, `lab/CapacityLab.java` (modelos) e as **regras Prometheus** `slo-rules.yml` e `capacity-rules.yml` com **`promtool check/test rules`** e **3 mutantes** (`lab/run-promtool.sh`). **NÃO executados:** o **PayFlow do aluno**, **Alertmanager** e qualquer **plantão**, **AWS** (FIS, RDS, AWS Backup, PITR, cópia entre regiões, Route 53, ECS/ALB), **teste de carga real**, **qualquer incidente real**. **Todo SLO, RPO/RTO, severidade, prazo e número de capacidade do PayFlow é PROPOSTA ou "a medir"**; os cenários de postmortem e do simulado são **SINTÉTICOS**.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Para um SLO de **99,9%** em 30 dias: quantos minutos de falha total cabem? Uma taxa de erro constante de **1,44%** é **quantas vezes** o ritmo exato e **em quanto tempo** gasta o orçamento? Quanto do mês **uma hora** nessa taxa consome (e por que a página "rápida" usa esse número)?
2. O alerta da P6.04 ("5xx > 2% por 5 min") **não** disparou em dois incidentes do laboratório. **Quais** eram, **quanto do orçamento** cada um gastou e **o que** a estratégia de **múltiplas janelas** fez diferente?
3. Para que serve a **janela curta** num alerta de *burn rate*? Qual **teste** do laboratório a prova e **o que aconteceu** quando você a removeu (mutante)?
4. Num incidente, por que **mitigar antes de diagnosticar**? Quais são os **quatro** papéis e o que o **comando não** faz?
5. O que tem um bom *runbook* (as **5 seções**)? O que o `DocLint` **consegue** verificar e o que **não consegue**? Quem valida de verdade?
6. Reescreva **sem culpa**: "O João esqueceu de criar o índice." Por que "erro humano" **não** é causa raiz? Cite **três gatilhos** de postmortem.
7. No experimento do caos, o que cada variante mostrou: **sem *timeout***, ***timeout* de 200 ms** a 40 e a 100 req/s, e ***timeout* + assíncrono**? Qual é a fórmula da capacidade com *timeout* e **o preço** da variante assíncrona?
8. O **primeiro defeito** da condição de parada do experimento só apareceu ao rodar. **Qual era** e como foi corrigido?
9. Por que o **Multi-AZ não** é *backup*? No modelo, um envio de log a cada **300 s** perde quantas transações **em média e no pior caso** (a 5 commits/s)? O que **a restauração não desfaz**?
10. Com **2 AZs** e CPU de **60%**, quanto sobra de CPU ao perder uma AZ? Por que **escalar para 6 tarefas** com `maximum_percent = 200` pode derrubar o banco (premissa `max_connections = 100`, reserva 10, pool 10)? E por que o **p99 a 95%** de utilização é muito pior que a 80%?

<details><summary>Gabarito</summary>

1. 30 × 1.440 × 0,001 = **43,2 min**. 1,44% ÷ 0,1% = **14,4×**; o orçamento acaba em 30 d ÷ 14,4 ≈ **50 h**. Em 1 h consome 14,4 × 1 h ÷ 720 h = **2%** do mês: é o limiar da página rápida (tabela 5-8 do SRE Workbook).
2. **B**: 0,8% de erro por 12 h (**16,8%** do mês) e **C**: 1 min de 100% de erro a cada 30 min por 6 h (**29,2%**). A regra "5xx > 2% por 5 min" **nunca** disparou nos dois. A estratégia **M** (1 h + 5 min a 14,4×; 6 h + 30 min a 6×) chamou página em **~+250 min** (B) e no **primeiro** pico (C); a janela de 3 d gerou o *ticket* da queima 1,5× só após **~64 h**.
3. Para o alerta **apagar** quando o incidente **acabou** (a janela longa de 1 h ainda está alta). O **teste 5** ("janela curta apaga a página depois da recuperação"): o **mutante M2** (sem a janela curta) **reprovou** a suíte (a página continuaria acesa; no teste, **30 min depois** do fim da queda).
4. Porque o **orçamento queima enquanto se investiga**; *rollback*/*flag*/escala estancam em minutos e a causa se estuda depois. Papéis: **comando, operação, comunicação, registro**. O comando **não debuga** (decide, delega, mantém o estado).
5. **Impacto, Diagnóstico (passos numerados), Mitigação (com comando/rollback), Escalar, Verificação**, mais dono e data de revisão. O `DocLint` pega **ausência**, **vagueza óbvia** e **palavras de culpa**; **não** sabe se o passo está **certo**. Quem valida é o **ensaio** (*game day* seguindo o *runbook* ao pé da letra).
6. "O processo permitia chegar à produção uma consulta sem índice; o CI não tinha teste de plano." Qualquer pessoa pode errar: a causa é o que o **sistema permitiu**. Gatilhos (SRE Book): indisponibilidade/degradação acima de um limiar, **perda de dado**, **intervenção do plantão**, resolução lenta, **falha de monitoramento**.
7. **Sem *timeout***: **refutada** (19,8% de sucesso, p50 5 s; `L = 40 × 3 = 120 ≫ 16`). ***Timeout* 200 ms**: **confirmada a 40 req/s** (p99 251 ms), **refutada a 100** (p50 1.125 ms, p99 2.041 ms): capacidade = `threads ÷ timeout = 16 ÷ 0,213 ≈ 75 req/s`. **Assíncrono + *bulkhead***: confirmada, mas **descartou 74 de 240** (a 40) e **556 de 600** (a 100) notificações: perda → **Outbox**.
8. Só contava requisições **terminadas**; como o defeito é **pendurar**, nunca disparava. A correção conta também as **ainda em andamento há mais de 1 s** como ruins.
9. Ele **copia o erro** (um `DELETE` replica em ms); só um ponto anterior volta no tempo. 300 s × 5/s: **média ~750**, **pior caso ~1.500** (medido: 746,9 e máx. 1.601). **Não desfaz** o que aconteceu **fora** do banco (PSP, e-mails, **chaves de idempotência** perdidas): exige **conciliação**.
10. 60% × 2 = **120%** (precisaria de ≤ 42% de regime para ficar ≤ 85%). 6 × 2 × 10 = **120 > 90** conexões; o **teto** com fator 2 é **4 tarefas**. O joelho: no modelo M/M/4, **p99 400 ms a 95%** × **140 ms a 80%** (~3×), máximo ×2.
</details>

## Parte 2: Entrega prática (tag `payflow-S4`)
O `payflow` ganha **os artefatos de operação**: SLOs e política, regras de alerta de SLO e capacidade (testadas), *runbooks*, processo de incidente, postmortem, catálogo de caos, continuidade (*backup*, Multi-AZ, DR), modelo de capacidade e a **ADR 0027**. **Nada foi compilado nem executado no PayFlow**; o laboratório `3-senior/S4-confiabilidade-sre/lab` é o **modelo executado**. **Nenhuma migração** (a **V25** continua livre).
```
payflow/
├── observability/prometheus/
│   ├── slo-rules.yml                 gravação (5m/30m/1h/6h/3d) + alertas Fast/Slow/Drift   (S4.01)
│   ├── slo-rules.test.yml            5 testes (saudável, queda, 5x, latência, janela curta)   (S4.01)
│   ├── capacity-rules.yml            predict_linear: disco e slot (ticket 14 d / página 24 h) (S4.07)
│   └── capacity-rules.test.yml       3 testes (estável, tendência, degrau)                    (S4.07)
├── docs/slo/
│   ├── slos.md                       SLIs, SLOs PROPOSTOS, o que NÃO conta                     (S4.01)
│   └── politica-error-budget.md      limiares e exceções (assinada por produto)                 (S4.01)
├── docs/incidentes/
│   ├── processo.md                   papéis, severidades, cadências, modelo de atualização, plantão   (S4.02)
│   ├── criterios-de-postmortem.md    gatilhos escritos antes                                    (S4.03)
│   └── postmortem-p6-transferencias-lentas.md   com os números da SUA execução                  (S4.03)
├── docs/runbooks/                    11 (P6.04 + SLO) + perda-de-az.md + disco-do-banco.md      (S4.02/06/07)
├── docs/caos/
│   ├── experimentos.md               catálogo E1–E6: estado estável, hipótese, raio, parada     (S4.04)
│   └── registro.md                   um registro por execução (hipótese, resultado, ação)       (S4.04)
├── docs/continuidade/
│   ├── backup-e-recuperacao.md       RPO/RTO por cenário e por dado, 7 camadas, conciliação     (S4.05)
│   ├── ensaio-de-restauracao.md      6 passos mensais, invariantes, RTO medido                  (S4.05)
│   ├── multi-az.md                   conta do N+1, conta de conexões, opção A/B/C e gatilho     (S4.06)
│   └── dr-regional.md                estratégia (backup e restauração), gatilhos, conta de custo (S4.06)
├── docs/capacidade/modelo.md         tabela por recurso, revisão mensal, degradação              (S4.07)
├── tools/DocLint.java + scripts/check-runbooks.sh   (rodam no CI)                               (S4.02/03)
└── .github/workflows/ci.yml          + promtool (regras e testes) + check-runbooks               (S4.01/02)
projeto/adrs/0027-confiabilidade-do-payflow.md   (já redigida nesta geração)
```
Critérios de pronto:
- [ ] **SLO:** 2 SLIs (disponibilidade e latência da transferência) com **bom/válido/o que não conta**; **SLOs marcados como proposta** com data de revisão e quem assina; `slo-rules.yml` com **tráfego mínimo**, `runbook` e `severity` em todo alerta; **`promtool test` no CI com caso negativo e um mutante**; **política de *error budget*** com limiares.
- [ ] **Incidentes:** `processo.md` com **papéis**, severidades ligadas ao SLO e **dinheiro = SEV1**, cadência e **modelo de atualização**, plantão (primário/secundário, passagem de turno); **todos os *runbooks*** existem, passam no `DocLint` e o **CI reprova** alerta sem *runbook* (prove apagando um).
- [ ] **Postmortem:** gatilhos escritos; o postmortem da P6.07 com **as 7 seções**, **impacto em orçamento calculado**, ações de **prevenir/detectar/mitigar** com **dono e data** e uma que **repete o experimento**; passa no `DocLint postmortem`; **nenhuma frase culpa pessoas**.
- [ ] **Caos:** catálogo E1–E6 com **estado estável (SLI e valor)**, **hipótese**, **raio**, **condição de parada** (que enxerga falhas que **penduram**) e **reversão**; **E1 executado** com **registro**; **custo em orçamento** do E2/E3; regra **"só com orçamento > 50%"**; um *game day* de *staging* agendado.
- [ ] **Continuidade:** **RPO/RTO por cenário e por tipo de dado** assinados; **7 camadas** com quem pode apagar; **procedimento de restauração com a conciliação**; **ensaio mensal** com invariantes e **RTO medido** no lugar das premissas; política de LGPD para *backups*; alarmes de *backup* e de `LatestRestorableTime`.
- [ ] **Multi-AZ/DR:** **conta do N+1** do seu `desired_count`, **conta de conexões** com o `max_connections` **real** e **teto** de `desired_count`; *endpoints* **em todas as AZs** (ou dívida registrada); *runbook* de **perda de AZ**; **E6** planejado; **DR regional** com estratégia, **gatilhos** e premissas assinadas.
- [ ] **Capacidade:** `modelo.md` preenchido (**números medidos** ou "a medir" com data e dono); **o joelho** medido com teste de carga e a folga escolhida **considerando a perda de AZ**; alertas `predict_linear` **testados** (tendência **e** degrau); `max_slot_wal_keep_size` pela **fórmula** com a taxa real de WAL; revisão mensal agendada.
- [ ] **ADR 0027** revisada; tag `payflow-S4`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Sênior: **≥ 2** em todos e **3** nos marcados com ⭐.
| Competência (matriz) | Evidência no módulo | 0–3 |
|---|---|---|
| ⭐ **Observabilidade e operação:** define SLOs, conduz postmortems, planeja capacidade | S4.01, S4.03, S4.07 (orçamento, *burn rate*, postmortem sem culpa, joelho e previsão) | |
| ⭐ **Sistemas distribuídos:** resiliência provada (falha injetada), N+1 e falhas correlacionadas | S4.04, S4.06 (experimento, `threads ÷ timeout`, cadeia em série) | |
| ⭐ **Liderança e comunicação:** papéis no incidente, política assinada, ações com dono | S4.01–S4.03 (comando, cadência, política de orçamento, ações verificáveis) | |
| **Nuvem e custo:** Multi-AZ, DR e RPO/RTO com gatilhos e custo | S4.05, S4.06 (camadas de *backup*, quatro estratégias, decisão por conta) — **leitura** | |
| **Banco de dados:** PITR, conciliação depois de restaurar, conta de conexões | S4.05, S4.07 | |
| **Segurança:** backup imutável, credencial comprometida, LGPD em *backup*/postmortem | S4.03, S4.05 | |
| **Performance:** curva utilização × latência e Little | S4.07 (M/M/4, `L = λ × W`) | |
| **Containers e orquestração:** capacidade por AZ, escala e teto pelo banco | S4.06, S4.07 (ECS, ALB) | |
| **Testes e qualidade:** testes que **podem falhar** (mutantes, caso negativo) | S4.01, S4.02, S4.04, S4.07 | |
Se **≥ 2** em tudo e **3** nos ⭐ → siga para o **S5**. Linhas em **0–1**: refaça a aula indicada e o **laboratório** correspondente.

## Parte 4: Simulado: conduzir um incidente e decidir a continuidade (60 min, **com consulta ao cheatsheet**)
> ⚠️ **Cenário SINTÉTICO:** os **números** abaixo são **inventados** para o exercício; os **mecanismos** (orçamento e *burn rate*, janelas, *timeout* e `threads ÷ timeout`, PITR e conciliação, N+1, conta de conexões) vêm das aulas S4.01–S4.07, **medidos** no laboratório. **Nunca** cite este dossiê como medição.

**Cenário.** O PayFlow tem **SLO de disponibilidade de 99,9%** (30 dias) para `POST /api/v1/transfers`, **8 req/s** em média, **4 tarefas** ECS em **2 AZs** (CPU 60%), **pool 10** por tarefa, `max_connections = 100` (reserva 10), `maximum_percent = 200`. Às **09:00** o *deploy* **v57** muda o *timeout* das chamadas ao PSP de **2 s** para **30 s** (erro de configuração). A partir das 09:00, **12%** das transferências retornam 5xx (as *threads* ficam presas). O alerta `PayflowTransfersAvailabilityBurnFast` toca, a pessoa de plantão assume, e às **09:40** o *rollback* termina e o erro volta a ~0. **Às 10:15**, uma correção manual de dados roda `DELETE FROM idempotency_keys` **sem `WHERE`** (**3.000 linhas**). Às **11:00**, o time de produto pede para **passar o teto da escala de 4 para 6 tarefas** para uma campanha.

**Tarefas:**
1. **Orçamento.** Calcule o **orçamento** em requisições, o **gasto** do incidente das 09:00–09:40, a **taxa de queima** e **quanto tempo** levaria para o alerta `Fast` disparar. O que a **política** (S4.01) manda?
2. **Incidente.** Classifique a **severidade**, nomeie os **papéis**, escreva a **mitigação** na ordem e um **modelo de atualização** das 09:20.
3. **Postmortem.** Escreva (em ~15 linhas) as seções **Impacto**, **Causa e fatores contribuintes** e **5 ações** (≥ 1 de cada tipo: prevenir, detectar, mitigar), **todas com dono e data**; **sem culpa**.
4. **Caos.** Que **experimento** (hipótese, estado estável, condição de parada) **teria evitado** o incidente? O que ele **refuta** se o *timeout* é de 30 s?
5. **Continuidade (10:15).** Defina **RPO/RTO** do cenário, a **opção de restauração** (restaurar o banco todo × **só a tabela**), o que **fica fora do banco** e **o que conciliar**.
6. **Capacidade (11:00).** A campanha é viável com **6 tarefas**? Mostre a **conta**, a **alternativa** e o que decide o teto. E: **perdendo uma AZ**, quanta CPU sobra com 4 tarefas?

**Rubrica (100 pontos):** orçamento e *burn* corretos (**15**); incidente (severidade, papéis, mitigação em ordem, comunicação) (**15**); postmortem sem culpa com ações verificáveis (**20**); caos (**10**); continuidade com **conciliação** (**20**); capacidade e N+1 (**10**); ordem de ações e **o que NÃO fazer** (**10**). **Aprovado: ≥ 70.**

<details><summary>Gabarito (resumo)</summary>

**1.** Requisições em 30 d: 8 × 86.400 × 30 = **20.736.000**; orçamento (0,1%) = **20.736**. 09:00–09:40 = 2.400 s → 19.200 requisições × 12% = **2.304 erros** = **11,1%** do orçamento. Taxa = 12% ÷ 0,1% = **120×** (o orçamento inteiro duraria 30 d ÷ 120 = **6 h**). O `Fast` dispara quando a janela de 1 h passa de 14,4 × 0,1% = **1,44%**: 12% × (t ÷ 60 min) > 1,44% → **t > 7,2 min** (**~09:08**, mais o intervalo de avaliação). Política proposta: restante **~89%** (> 50%): fluxo normal, mas **o postmortem registra** os 11,1%.
**2.** **SEV2** (página de SLO, sem dinheiro envolvido; **sobe para SEV1** se a conciliação divergir). **Comando** (plantão), **operação** (quem faz o *rollback*), **comunicação**, **registro**. Mitigação: **(1)** mudança recente? **rollback** (`ecs-deploy.sh rollback`); **(2)** *flag*; **(3)** escala **só dentro da conta de conexões**; escalar com *threads* presas **não** resolve (Little). Atualização: `[SEV2] Transferências com erro — 09:20 UTC / Impacto: ~12% das transferências com 5xx; nenhum dinheiro perdido (a verificar na conciliação) / Estado: rollback em andamento / Próxima: 09:50 / Comando: @plantao`.
**3.** **Impacto:** ~12% das transferências por 40 min; **11,1%** do orçamento; conciliação sem divergência (a confirmar). **Causa:** o sistema **permitia** um *timeout* de 30 s numa dependência no caminho da requisição (sem teto na configuração, sem teste que reprove), com **pool compartilhado**; o CI não valida valores de *timeout*; o canário **não** observava o SLI nos 10 min seguintes. **Ações:** **prevenir** (validação/teste de CI que reprova *timeout* > 2 s — @backend — data); **prevenir** (*bulkhead* por dependência — @backend — data); **detectar** (o *deploy* verifica o SLI por 10 min e reverte — @plataforma — data); **mitigar** (passo "dependência lenta/ *timeout*" no *runbook* — @plataforma — data); **verificar** (repetir o **E1** e anexar o resultado — @plataforma — data). Sem nomes de pessoas como causa.
**4.** **E1 adaptado ao PSP**: *latência injetada* no PSP (estado estável: p99 < 400 ms e ≥ 99% de sucesso; hipótese: **mantém**; parada: erro ≥ 50% **ou** requisições **penduradas** > 1 s). Com *timeout* de 30 s a hipótese seria **refutada** como na variante 1 (`L = λ × W` muito acima das *threads*). Rodar em *staging* **antes** de mudar o *timeout*.
**5.** **RPO ≤ 5 min / RTO da tabela ~30 min (a medir no ensaio)**: restaurar **só a tabela** (PITR para **10:14** numa **instância temporária**, `COPY` das chaves e reinserir no primário vivo) **perde menos** que restaurar o banco inteiro (~75 min no modelo e perde as transferências boas desde 10:14). **Fora do banco:** transferências que **repetiram** entre 10:15 e a restauração (as chaves sumiram → **duplicação**); **PSP** e e-mails. **Conciliar** com o PSP **a partir de 10:15**; **bloquear/validar** repetições até restaurar as chaves; **comunicar**; **registrar** o orçamento. **Ação:** o papel humano **não** pode `DELETE` sem transação e `WHERE` (menor privilégio, revisão).
**6.** **Não** com `maximum_percent = 200`: 6 × 2 × 10 = **120 > 90**; o teto com fator 2 é **4 tarefas**. Alternativas: `maximum_percent = 150` → ⌈6 × 1,5⌉ = 9 × 10 = **90 ≤ 90** (no limite, **sem folga**), **reduzir o pool**, **subir `max_connections`** (instância maior, custo) ou **proxy** (adiado). **O banco decide o teto, não a CPU**; **sem *deploy* durante a campanha**. **Perda de AZ com 4 tarefas a 60%:** 60% × 4 ÷ 2 = **120%** até o *autoscaling* reagir (~minutos): custo em orçamento a medir no **E6**.
**Ordem:** (1) **mitigar** (rollback) → (2) **verificar o SLI** → (3) **conciliar** → (4) **restaurar a tabela** (ensaiada) → (5) **postmortem** → (6) **só depois** a campanha, **com a conta de conexões revisada**. **Não fazer:** reiniciar todas as tarefas ao mesmo tempo; **escalar** para curar *threads* presas; **deploy** durante a recuperação; restaurar o banco inteiro por reflexo; apontar a pessoa que rodou o `DELETE`.
</details>

## ➡️ Próximo
[S5 — Segurança avançada](../S5-seguranca-avancada/README.md)
