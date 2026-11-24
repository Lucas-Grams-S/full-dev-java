# S7.CP — Checkpoint do módulo S7

> ⏱️ ~1h30 · 🟣 Sênior · Pré-requisitos: S7.01 a S7.07

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, JDK 25.0.5.1, Maven 3.9.12, git 2.53): `lab/DocLab.java` (*linter* de ADR/RFC/design doc sobre as **33 ADRs reais** (29 até a S6 + 4 da S7), as amostras boa e ruim e **mutantes**), `lab/RecurrenceSpike.java` (datas, fusos com *tzdata* 2026f, pico das 06:00 por premissas, chaves de idempotência), `lab/EstimateLab.java` (três pontos, Monte Carlo, riscos, vazão), `lab/HotspotLab.java` (histórico **real** do `spring-petclinic`, 1.047 *commits*, *commit* `500158f`), `lab/StranglerLab.java` (leitura em paralelo com 2 mutantes), **OpenRewrite** 6.46.1 em *dry run* (`UpgradeToJava25`, `JavaxMigrationToJakarta`), `lab/review-lab` (**JaCoCo 0.8.16 × PIT 1.30.2** com JUnit 6.1.3), `lab/FlakyLab.java`, `lab/PriorityLab.java` (CD3 conferido por força bruta) e `lab/ai-lab` (**Spring AI 2.0.2** + Boot 4.1.2, **6 testes** com modelo falso). **Números de estimativa, risco, vazão, custo do atraso e pico são PREMISSAS declaradas.** **NÃO executados:** qualquer processo com pessoas (RFC em revisão, reunião de design, mentoria), chamadas a um LLM real, o PayFlow do aluno (módulo `recurring`, V26, troca do extrato, PIT no CI).

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. O que uma ADR registra, qual a regra de Nygard sobre **quantas** decisões por ADR, e o que se faz quando uma decisão aceita muda? O que o *linter* achou nas ADRs do próprio PayFlow?
2. Numa RFC, para que servem o **prazo de comentários** e o **DACI**? Quantos *Approvers*? O que é o *advice process*?
3. Qual a diferença entre **objetivo negado** e **não-objetivo**? Dê um exemplo de cada para a recorrência. Por que o design doc precisa de **reversão**?
4. O que o *spike* da recorrência descobriu sobre `plusMonths(1)` encadeado, `plusDays(30)` e `LocalDate.now()` num servidor em UTC?
5. Por que a soma dos "mais prováveis" (20,5 dias) teve **0,8%** de chance no laboratório? O que é **p85** e por que usá-lo no compromisso? Quanto a previsão "30 ÷ média" atrasa com o *backlog* crescendo 20%?
6. Qual a diferença entre **juros** e **principal** da dívida? Por que "só revisões" engana no ranking de hotspots (dê o exemplo do `Pet.java`)?
7. Na leitura em paralelo do extrato, por que `equals()` gerou 11,6% de "diferenças" com o projetor **correto**? Como a comparação por versão resolve, e quantas leituras sem *bug* justificam desligar o caminho antigo (regra de três)?
8. O que o OpenRewrite **fez** e **não fez** no projeto legado? Qual a licença das receitas e por que isso importa?
9. Uma suíte com **100% de linhas e ramos** matou quantos mutantes? Por que a nova tentativa automática de testes instáveis é perigosa (use o número do `FlakyLab`)? Que fluxo de *branches* o PayFlow adota e por quê?
10. Por que CD3 venceu "o mais valioso primeiro"? O que mudou quando a dívida foi **fatiada**? No `ai-lab`, quais **três** defesas não dependem do modelo, e quais **duas** surpresas de configuração do Spring AI apareceram só na execução?

<details><summary>Gabarito</summary>

1. **Uma** decisão significativa: contexto, opções, decisão, status e consequências (positivas **e negativas**), com gatilho de revisão. Aceita **não se edita**: cria-se outra que a **substitui**. O *linter*: **6 erros** no índice (linhas de 0023–0025 e 0027–0029 com 2 colunas), **15 ADRs-ônibus** (7–15 regras, > 800 palavras) e 17 sem gatilho de revisão em campo próprio (a 0001 tem o gatilho dentro das consequências: falso positivo de forma).
2. O prazo faz a revisão **terminar**; o DACI separa quem conduz (D), quem **decide** (A: **um só**), quem contribui (C) e quem é informado (I). *Advice process*: qualquer pessoa pode decidir **depois de consultar** os afetados e os especialistas, e registra a decisão.
3. Objetivo negado é o contrário de um objetivo ("não pode duplicar" é o objetivo "0 duplicadas"); não-objetivo é algo **razoável** que se escolheu **não** fazer ("recorrência semanal", "Pix externo"). Sem reversão, o plano não diz **como voltar** quando o SLO ou a conciliação falharem (a *flag* desligada, nada apagado).
4. `plusMonths(1)` encadeado **perde o dia 31 para sempre** (28/03, 28/04…); `plusDays(30)` erra **10 de 12** meses; `LocalDate.now()` em UTC às 01:30 acha que já é dia 5 quando em São Paulo é **22:30 do dia 4**. Solução: regra pelo `YearMonth`, `Clock` injetado e fuso do agendamento.
5. As distribuições são **tortas para a direita** e todas precisariam sair do lado bom ao mesmo tempo. **p85** = em 85% dos cenários termina até ali: um compromisso com folga **declarada**. "30 ÷ média" = 8 semanas = a **mediana** (45,4% de atraso); com +20% de *backlog*, **89,7%** de atraso.
6. **Juros**: custo recorrente em cada mudança naquela área (tempo, *bugs*, incidentes); **principal**: esforço para pagar. `Pet.java` tem 38 revisões e **0** pontos de decisão: muda muito e barato; o hotspot real é revisão × complexidade (`PetController`, `Owner`, `OwnerController`).
7. A projeção é **assíncrona** (atraso médio de 150 ms no modelo): o novo ainda não viu as escritas recentes. Comparar só até o `seq` que o novo já viu separa **LAG** (esperado; vira *fallback* com `X-Min-Seq`) de **BUG**. Regra de três: **0** *bugs* em *n* leituras → taxa < **3/n** (95%); o plano pede **≥ 30 mil** leituras (< 0,01%), não "duas semanas".
8. Fez: `source/target` 1.8 → 25, `new Integer` → `Integer.valueOf`, `instanceof` com padrão; `javax.persistence` → `jakarta.persistence` (com `jakarta.persistence-api` **3.0.0**, enquanto o Boot 4.1.2 gerencia a **3.2.0**). Não fez: `release`, `StringBuffer`, *diamond*, `List.copyOf`. As receitas usam a **Moderne Source Available License** (não Apache): ler com o jurídico antes de adotar.
9. **6 de 18** (33%; a forte, 18/18). Com 20 testes a 1%, **18,2%** dos *builds* corretos ficam vermelhos; a nova tentativa baixa para 0,2%, **mas** um *bug* real que falha 10% das vezes aparece em só **1%**. ***Trunk-based*** com *branches* curtos e *flags*: o PayFlow é entregue continuamente e não mantém várias versões (o próprio autor do Git Flow recomenda algo mais simples nesse caso, nota de 2020).
10. CD3 põe primeiro o que perde **mais por semana de espera dividido pela duração**: os itens pequenos e caros (T21, *patch*) não ficam parados atrás de 10 semanas de recorrência (R$ 215,5 mil × 317 mil FIFO, premissas). Fatiada, a primeira parte da dívida (`Deprecation`/`Sunset`, meia semana) foi para o **3º lugar** e o total caiu para R$ 204,5 mil. Defesas: **validar a saída** (`DraftGuard` + humano), **ferramentas só de leitura com dono conferido pela ferramenta** e **nenhuma ferramenta de dinheiro**. Surpresas: `NoClassDefFoundError` do Jackson **fora do BOM do Boot**; argumento **nulo sem `-parameters`** e o erro **enviado ao modelo** (`throw-exception-on-error = false`); também o padrão `claude-haiku-4-5` e o 2.1.0 exigir o Boot 4.2.
</details>

## Parte 2: Entrega prática (tag `payflow-S7`)
O `payflow` ganha **o processo de decisão e a próxima evolução documentada**: RFC e design doc das transferências recorrentes, estimativa e riscos, registro de dívida e o plano de modernização do extrato, regras de revisão e de testes, e a política de IA. **Nada disto muda o código da aplicação** (o módulo `recurring` é **plano**); a migração **V26** fica **reservada** para `recurring_transfers`/`recurring_runs`.

```
payflow/
├── CONTRIBUTING.md                         fluxo trunk-based, tamanho de PR, prazo e rótulos de revisão (S7.05)
├── .github/CODEOWNERS                      donos de transfer, payment, recurring, infra/, ADRs        (S7.05)
├── .github/pull_request_template.md        o que muda, como testei, risco, como reverter, partes geradas por IA (S7.05, S7.07)
├── docs/rfcs/0001-transferencias-recorrentes.md   RFC com DACI, prazo, 4 alternativas e a SUA decisão  (S7.01)
├── docs/rfcs/TEMPLATE.md
├── docs/design/transferencias-recorrentes.md      design doc (V26, fluxo, SLO, custo, plano com reversão) (S7.02)
├── docs/design/recorrentes-estimativa.md          três pontos, p50/p85/p95, foco, premissas, data de reestimativa (S7.03)
├── docs/riscos.csv                                 registro de riscos com dono e gatilho          (S7.03)
├── docs/divida-tecnica.csv                         registro de dívida com juros, dono e gatilho   (S7.04)
├── docs/modernizacao/plano-extrato.md              /entries → /statement: abstração, paralelo por versão, Sunset, remoção (S7.04)
├── docs/engenharia/{testes-instaveis,onboarding,times,politica-de-ia}.md                        (S7.05–S7.07)
├── docs/produto/{one-pager-recorrentes,prioridades}.md                                            (S7.06)
└── .github/workflows/docs.yml                      DocLab (ADR, RFC, design) + PIT nos pacotes de dinheiro (S7.01, S7.05)
projeto/adrs/0030…0033                              (já redigidas nesta geração: processo, recorrentes, extrato, IA)
```

**Critérios de pronto:**
- [ ] **ADRs e RFCs:** índice corrigido; `DocLab adr` sem erros; RFC-0001 com a **sua** decisão registrada; *linter* no CI (S7.01).
- [ ] **Design doc:** passa no *linter*; perguntas abertas com dono; contrato dos eventos acordado com o time de notificações; dia 31 e UTC viraram testes com `Clock` fixo (S7.02).
- [ ] **Estimativa e riscos:** três pontos **seus**, p85 com premissas, data de reestimativa; riscos com dono e gatilho (S7.03).
- [ ] **Dívida e modernização:** ≥ 6 itens no registro (com os hotspots do **seu** repositório); plano do extrato com **critério numérico** de desligar o velho e data de remoção da arquitetura de transição (S7.04).
- [ ] **Revisão e testes:** `CONTRIBUTING.md`, `CODEOWNERS`, *template* de PR; PIT nos pacotes de dinheiro com piso **medido**; política de testes instáveis (S7.05).
- [ ] **Mentoria:** plano de *onboarding* e de crescimento pela matriz (S7.05).
- [ ] **Produto:** *one-pager* da RFC-0001; prioridades com custo do atraso em faixa e CD3; política de *error budget* **assinada** (S7.06).
- [ ] **IA:** política de uso no desenvolvimento; RFC-0002 (assistente de suporte) em **rascunho** com a recomendação de não adotar agora (S7.07).
- [ ] **ADRs 0030–0033** revisadas; tag `payflow-S7`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Sênior: **≥ 2** em todos e **3** nos marcados com ⭐.
| Competência (matriz) | Evidência no módulo | 0–3 |
|---|---|---|
| ⭐ **Liderança e comunicação:** escreve RFCs/design docs, mentora, negocia dívida técnica com o negócio | S7.01, S7.02, S7.05, S7.06 | |
| ⭐ **Arquitetura:** decide limites (módulo × serviço), registra ADRs curtas, evita complexidade desnecessária | S7.01, S7.02 (estudo de caso Pleno × Sênior) | |
| ⭐ **Testes e qualidade:** define estratégia de testes e métricas de qualidade para o time (mutação, instabilidade) | S7.05 | |
| **Git e ferramentas:** estabelece convenções e automação para vários times (fluxo de *branches*, CODEOWNERS, *linters* de documentos) | S7.01, S7.05 | |
| **Liderança (Pleno, lacuna):** estima tarefas com três pontos e diz a incerteza | S7.03 | |
| **Arquitetura e legado:** moderniza sem parar (abstração, paralelo por versão, remoção) e atualiza a pilha com receitas | S7.04 | |
| **Observabilidade e operação:** usa o *error budget* como contrato com produto | S7.06 | |
| **Segurança:** desenha uma funcionalidade com LLM com defesas que não dependem do modelo | S7.07 | |
| **Nuvem e custo:** custo do atraso e custo marginal no *one-pager* | S7.02, S7.06 | |
Se **≥ 2** em tudo e **3** nos ⭐ → siga para o **S8**. Linhas em **0–1**: refaça a aula indicada e o **laboratório** correspondente.

## Parte 4: Simulado: conduzir uma decisão grande (60 min, **com consulta às aulas**)
> ⚠️ **Cenário SINTÉTICO:** pedidos, prazos e valores são **inventados** para o exercício; os **números** do gabarito saem das **saídas reais** do laboratório (com as premissas de lá). **Nunca** cite este simulado como medição do PayFlow.

**Cenário.** É o primeiro dia do trimestre. Chegam ao mesmo tempo: (a) o produto quer as **transferências recorrentes** "até março"; (b) o suporte quer um **assistente de IA** que responda clientes e "estorne até R$ 50"; (c) um dev propõe **reescrever o extrato** num *branch* de 2 meses; (d) o time de plataforma avisa que a *landing zone* (FND01/FND02 HIGH) está parada; (e) a diretoria pede **90% de cobertura** depois de um incidente; (f) o orçamento de erro do mês está em **20%** por causa do T21. Você é o tech lead de um time de 3 pessoas.

**Tarefas:**
1. **Ordem do trimestre.** Monte a lista com custo do atraso (faixas) e duração, ordene por CD3 e explique o que fica fora. Onde entra a dívida do extrato?
2. **Recorrentes.** Que artefatos você produz (RFC, design, estimativa) e em que ordem? Qual compromisso você dá para março, com que confiança e premissas? Que dois riscos você ataca na primeira semana?
3. **Extrato.** Responda ao dev que quer reescrever: o plano, o critério para desligar o velho e o que é arquitetura de transição.
4. **Assistente de IA.** Responda ao suporte: o que sim, o que não, e as condições.
5. **Cobertura.** Responda à diretoria com outra medida para o mesmo objetivo.
6. **Orçamento de erro em 20%.** O que a política diz e o que você faz com as recorrentes e o T21?
7. **Time.** Como você divide o trabalho entre as 3 pessoas (mentoria, revisão, quem é dono de quê) e com quem combina contratos fora do time?

**Rubrica (100 pontos):** ordem por CD3 com faixas e o que fica fora (**15**); artefatos e compromisso com faixa e riscos (**20**); extrato por estrangulamento com critério numérico (**15**); IA com defesas fora do modelo (**15**); cobertura × mutação (**10**); *error budget* como contrato (**10**); time, mentoria e contratos (**15**). **≥ 70** para seguir.

<details><summary>Gabarito (resumo)</summary>

**1.** Pela força bruta do laboratório (premissas): **T21** e o ***patch*** primeiro (CD3 6,0 e 4,0: meia semana cada), depois a ***landing zone*** (1,33), a **recorrência** (0,80) e o painel; a dívida do extrato **fatiada**: a primeira fatia (`Deprecation`/`Sunset` + métrica de uso, meia semana) sobe para o **3º lugar**, a remoção fica para depois que os clientes migrarem. A ordem por chegada custaria R$ 317 mil × R$ 215,5 mil (CD3). Fica fora do trimestre: o painel novo (ou entra se sobrar capacidade). Dependências (remoção depois da depreciação) são restrições, não CD3.
**2.** RFC-0001 (prazo, DACI, 4 alternativas) → design doc com *spike* → estimativa (três pontos, Monte Carlo) e registro de riscos → ADR. Compromisso: **~10 semanas de uma pessoa no p85** a partir do design aprovado (premissas: foco de 60%, riscos mitigados); "**até março com 85% de confiança, para contas internas; externo depende de R2 e do contrato com notificações; reestimo em 2 semanas**". Semana 1: **R2** (o `transfer` devolve a existente para chave repetida? ler o código + um teste: 0,25 dia, ganho esperado +0,95 dia) e **R4** (contrato do evento com o time de notificações) e, se der, **R1** (*rollback-only*).
**3.** Não reescrever: `StatementReader` como abstração, `/statement` atrás, leitura em paralelo **por versão** (amostrada), *flag* 1% → 100%, `Deprecation`/`Sunset` no `/entries` com métrica de uso. Desligar o velho com **0 BUG em ≥ 30 mil leituras** (regra de três, < 0,01%), *fallback* abaixo do limiar e uso do `/entries` zero por 30 dias. Arquitetura de transição: a abstração e o comparador, **com data de remoção**.
**4.** Sim a um **rascunho para o atendente** (humano no circuito), com ferramentas **só de leitura** e dono conferido pela ferramenta; **não** a responder direto ao cliente e **não** a estornar (nenhuma ferramenta de dinheiro: o modelo manipulado pediu `refundTransfer` no laboratório). Condições: contrato com o provedor (operador LGPD: região, retenção), conjunto de avaliação aprovado pelo suporte, `DraftGuard`, `throw-exception-on-error: true`. Recomendação do PayFlow: **RFC-0002 em rascunho, não adotar agora**.
**5.** Cobertura de 90% é cumprida por testes que não verificam nada (100% de cobertura com 33% de mutantes mortos no laboratório). Proposta: **escore de mutação** com piso medido nos pacotes de dinheiro e autorização, **zero** teste instável fora da quarentena e a **taxa de falha de mudança** (DORA) como resultado acompanhado.
**6.** Com **< 25%** a política (decisão 219, assinada por produto) **congela mudanças não urgentes**: o T21 (que consome o orçamento) é a correção prioritária e cabe na política; as recorrentes seguem em desenvolvimento e **só em contas internas**; liberar para clientes externos exige **exceção por escrito** de quem assinou.
**7.** Uma pessoa (sênior) no caminho crítico da recorrência (T3 → T4) e na revisão do dinheiro; o pleno com T5–T6 e *pair* nas primeiras revisões; o júnior (ou o pleno) com o T21 e o *patch* (pequenos, com mentoria e revisão como aula). `CODEOWNERS` com dois revisores em `transfer`/`recurring`. Contratos fora do time: **notificações** (evento, X-como-serviço), **segurança** (colaboração com data de fim no consentimento), **plataforma** (*landing zone*), **produto** (limites, aviso, atraso, assinatura da política).
</details>

## ➡️ Próximo
[S8 — Carreira](../S8-carreira/README.md)
