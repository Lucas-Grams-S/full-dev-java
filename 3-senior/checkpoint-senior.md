# Checkpoint de nível Sênior

> ⏱️ ~9h (6 sessões de 1h30: ver `00-guia/cronograma.md`, "CK-S.1" a "CK-S.6") · 🟣 Sênior · Pré-requisitos: S1 a S8 (todos os módulos do nível) e o [checkpoint de nível Pleno](../2-pleno/checkpoint-pleno.md)

> ⚠️ **Estado de verificação:** por decisão do aluno, os **testes práticos do PayFlow ficam para o fim do treinamento**. Esta execução foi uma **revisão por leitura e por varredura automática** (links internos, 1.254 URLs, pré-requisitos, referências adiantadas, horas, seções do *template*, nomes, decisões, ADRs e, pela primeira vez, a **numeração das migrações**). **Não** foi compilado nem executado código do PayFlow, e **não** foi tocada a AWS. O **incidente da Parte 5 é sintético** (números inventados e rotulados; os **mecanismos** vêm das aulas em que foram medidos). O **design doc** (Parte 4) e o **simulado** (Parte 6) são **exercícios com premissas declaradas**: os únicos números "reais" que eles citam saem das **saídas dos laboratórios**, com o arquivo indicado ao lado. O que cada módulo executou de verdade está no registro do módulo em `00-guia/progresso.md` e no aviso de cada `checkpoint.md`.

## 🎯 Para que serve
Você terminou 8 módulos (62 aulas, 105 h, mais os 8 checkpoints de módulo). Este checkpoint responde a uma pergunta só: **consigo conduzir uma mudança grande num sistema que movimenta dinheiro (decidir, escrever, estimar, entregar, operar e responder por ela) e explicar isso numa entrevista?** Ele tem sete partes, em seis sessões:

| Parte | O que é | Sessão | Como é avaliada |
|---|---|---|---|
| 1 | Revisão de entrada: **o que a revisão do nível encontrou** e as **pendências** consolidadas (leia antes de começar) | CK-S.1 | leitura |
| 2 | **Projeto PayFlow** avaliado por checklist (tag `payflow-S8`) + o **mapa de migrações** (item obrigatório) | CK-S.1 | autoavaliação com evidência |
| 3 | **Quiz** por módulo (S1–S8) + **quiz integrador** de 20 questões | CK-S.2 | gabarito |
| 4 | **Design doc completo** de uma evolução grande ("PayFlow 10×"), com ADRs, diagramas, SLOs, estimativa de custo e plano de migração | CK-S.3 e CK-S.4 | `DocLab` + rubrica |
| 5 | **Incidente simulado** e **postmortem sem culpa** | CK-S.5 | gabarito, `DocLint` + rubrica |
| 6 | **Simulado de entrevista de *system design*** (60 min) | CK-S.6 (1ª metade) | rubrica |
| 7 | **Autoavaliação pela matriz** (tabela competência → aulas), decisão de concluir e **o que vem depois** (testes práticos finais) | CK-S.6 (2ª metade) | tabela |

---

## Parte 1: O que a revisão do nível encontrou

Revisão feita em 2026-11-24 sobre `3-senior/` (**62 aulas**, 8 `README.md`, 8 `checkpoint.md`, laboratórios) e sobre `00-guia/`, `projeto/`, `recursos/` e o `README.md` raiz, com *scripts* em Python (os mesmos da revisão do Júnior e do Pleno) e `curl` sem trocar o *User-Agent*. Duas verificações são **novas**: as **seções do *template*** de cada aula e a **numeração das migrações** citadas em **todos** os níveis.

| Verificação | Resultado |
|---|---|
| **Links internos** (407 links relativos em 189 arquivos `.md` de `3-senior/`, `00-guia/`, `projeto/`, `recursos/` e `README.md`, fora de blocos de código) | ✅ **0 quebrados**. O único alvo inexistente é `docs/decisoes.md`, citado em `S8-carreira/lab/samples/readme-ruim.md`: **de propósito** (é a amostra ruim que o `PortfolioLint` reprova na S8.01) |
| **Links externos do nível** (456 URLs únicas em `3-senior/`) | ✅ **456 responderam 200** |
| **Links externos dos guias e recursos** (798 URLs de `00-guia/`, `projeto/` e `recursos/` que não aparecem nas aulas do Sênior) | ⚠️ **789 → 200**. **2 → 404:** `docs.docker.com/get-started/docker-overview/` (citado na J9.01 e na `biblioteca.md`; respondia em 2026-11-13 e **saiu do ar**) e `docs.spring.io/s` (sem o resto do endereço), que **não é um link**: é a coluna de descrição **truncada** em 4 linhas da `recursos/biblioteca.md` (209–212). **6 → 403:** os 2 do `rabbitmq.com` (herdados da P3) e **4 buscas do `br.indeed.com`** em `o-que-o-mercado-pede.md` (o site recusa o `curl`). **1 sem resposta:** o texto da LGPD no `planalto.gov.br` (já marcado ⚠️ na S5) |
| **Pré-requisitos no cabeçalho** (62 aulas) | ✅ **todas** têm o campo; **nenhuma** aponta para aula **posterior, igual ou inexistente** (inclusive faixas como `S1.11–S1.13`) nem para **módulo** posterior |
| **Conceito usado antes de ensinado** | ✅ **187** citações a aulas posteriores no corpo das aulas, **todas do mesmo módulo** e no formato de **sinalização** ("→ S1.07", "retomado na S3.08"); **0** entre módulos. As **28** citações a **módulos** posteriores (a varredura achou 37; 9 são os servidores "S1"–"S4" do diagrama da S1.01) são do tipo "a S6 aprofunda", sem código que dependa delas |
| **Horas** | ✅ S1 21 h + S2 13,5 h + S3 15 h + S4 12 h + S5 12 h + S6 10,5 h + S7 12 h + S8 9 h = **105 h** (62 aulas + 8 checkpoints = **70 sessões × 1h30**; as sessões declaradas batem com as linhas das tabelas de cada `README.md`); + **9 h** deste checkpoint = **114 h** (igual ao `README.md` raiz; `cronograma.md`: semana 46, dia 5, até a semana 61). **Treinamento inteiro: 174 + 169,5 + 114 = 457,5 h = 305 sessões** (igual ao `cronograma.md`) |
| **Seções do *template*** (seção 6 das instruções) | ⚠️ **62 de 62** têm ⚖️ *Trade-offs*, 🏗️ Estudo de caso, 🎤 Na entrevista, 📚 e ✅. **23 aulas** (**S3.01–S3.09, S4.01–S4.07, S5.01–S5.07**) **não têm o título "🛠️ Exemplo prático"**: o exemplo executado está **dentro** das subseções de "🔬 Como funciona por dentro" (o **conteúdo** existe; o **título** falta). A **S3.09**, um estudo de caso inteiro, troca também "💡 Intuição" por "🏗️ O caso" |
| **Nomes** | ✅ nenhuma grafia divergente de `PayFlow`, `ledger_entries`, `com.payflow`; todas as **ADRs citadas** existem (0001–0034); as **decisões citadas** vão até a 270 (todas ≤ 284). ⚠️ **um arquivo com dois nomes:** `V20__archive_manifest.sql` (S3.04) × `V20__archive_manifest_and_period_balances.sql` (checkpoint da S3) |
| **Numeração das migrações** (cada `Vn__nome.sql` citado em J, P e S) | ❌ **duas colisões** que nenhuma revisão anterior viu: **V12** é `payment_requests` (**P3.10**) **e** `users_password_hash_nullable` (**P10.03**; os checkpoints da P8 e da P9 diziam "a próxima livre continua V12", sem contar a P3.10); **V13** é "remover `users.password_hash`" (**P10**, decisão 173: a contração, depois que todos os usuários estiverem no Keycloak) **e** `V13__ledger_account_seq.sql` (**S1.05**). Da V14 em diante não há colisão. Tratamento no **item 2.10** |
| **Competências da matriz sem aula** | ⚠️ **nenhuma sem aula**; **2 parciais** no Sênior (**Front-end** e **CI/CD "para vários times"**): Parte 7 |
| **Blocos de código** | ✅ 0 arquivos com cercas (`` ``` ``) desbalanceadas |

### O que o nível deixou **aberto** (consolidado: leia antes dos testes práticos)
Nada abaixo foi corrigido por conta própria (regra do treinamento: **registrar e avisar**). As linhas marcadas **(nova)** foram achadas nesta revisão; as outras já estavam em "Correções pendentes" do `progresso.md`.

| # | Pendência | Onde está | Como tratar |
|---|---|---|---|
| 1 | **(nova) Colisão de migrações V12 e V13** | P3.10, P10.03, P10 (decisão 173), S1.05 | **item 2.10**: mapa "versão do plano → versão real" e a regra da J3.12 (**o segundo a mesclar renumera**); **decisão do aluno**; nenhuma aula foi reescrita |
| 2 | **RFC-0001** (transferências recorrentes) sem decisão: ADR 0031 **proposta**, V26 reservada | S7.01, S7.02 | decidir até **2026-12-04** (prazo da própria RFC) e registrar a ADR |
| 3 | **Hipóteses da S1**: antifraude como **serviço extraído** (decisão 190) × só `notification` extraído (126); `ledger_postings` (partidas dobradas) **não existe** | S1.07, S1.12 | as 5 perguntas da P5.01; uma ADR curta para cada |
| 4 | **Índice das ADRs** com 6 linhas de 2 colunas (0023, 0024, 0025, 0027, 0028, 0029) | `projeto/adrs/README.md` (S7.01, exercício 2) | 4 colunas; `DocLab adr` com 0 erros |
| 5 | **Spring Boot 4.2.0** e **Spring AI 2.1.0** publicados em 19/11/2026 | `versoes.md`, decisão 2 | decidir migrar **antes** ou **depois** dos testes práticos (receitas em *dry run*, uma por PR: S7.04) |
| 6 | **Repositório público do treinamento:** sem `LICENSE` (S8.01) e **(nova)** o arquivo de trabalho `3-senior/S5-seguranca-avancada/urls5.txt` (51 URLs da S5) **publicado** no *commit* `7f3d23d` (o `urls5.txt` da raiz está só no `.git/info/exclude`) | S8.01; raiz | decidir a licença; apagar ou manter o arquivo (não há segredo: são URLs públicas) |
| 7 | **(nova) Links:** `docker-overview` 404; descrição truncada na `biblioteca.md` (linhas 209–212); buscas do Indeed 403 | J9.01, `recursos/biblioteca.md`, `o-que-o-mercado-pede.md` | trocar pelo endereço atual da documentação do Docker; restaurar a descrição; marcar ⚠️ as do Indeed |
| 8 | **(nova) *Template* e nomes:** 23 aulas sem o título "🛠️ Exemplo prático"; dois nomes para a V20 | S3–S5; S3.04 × checkpoint da S3 | aceitar (o conteúdo existe) ou renomear os títulos; escolher **um** nome para a V20 |
| 9 | **Decisões de negócio e jurídicas** abertas: retenção de 5 anos e fuso do "dia" (S3), região (247), B × B2 e quem assina o "custo por hora parado" (S6), assinatura de produto nos SLOs e na política de *error budget* (S4, S7), política de IA (S7) | registros S3, S4, S6, S7 | cada uma vira **pergunta com dono** no design doc da Parte 4 |
| 10 | **Tudo "a validar" do Sênior:** o PayFlow do aluno (V13–V26, projetor, saga, `idempotency_keys`, regras `promtool` com os **rótulos reais**, `AuthorizationPolicyTest`, *pipeline* de segurança, `ProviderSdkRulesTest`, `DocLab` e PIT no CI…), a AWS (Organizations, Budgets, KMS, CloudTrail, RDS/PITR, FIS), OpenSearch, Debezium, async-profiler, Gatling, Keycloak com mTLS, ZAP/Semgrep/CodeQL | "Correções pendentes" S1–S8 | ordem da **Parte 7** (decisões 182 e 288) |
| 11 | **Herdadas do Pleno e do Júnior:** 152 × 171 (conta de conexões), T21 e T13, cosign por *digest*, Kubernetes não executado e o *template* do chart, `Transfer.complete()`, `@WebMvcTest` × `@ConfigurationPropertiesScan`, `ActuatorTest`, J1.05–J1.07 | checkpoint Pleno, Parte 1 | passos 1–5 da decisão 182 |
| 12 | **Disco da máquina do laboratório:** `~/.cache/p8-tf-plugins` (~0,8 GB) | P8, S6 | apagar quando não for mais rodar Terraform |

> **Regra de ouro do nível (a mesma do Pleno, com um acréscimo):** o que foi **executado** num laboratório é evidência **do mecanismo**; o que está escrito sobre o PayFlow é **hipótese** até o teste prático. No Sênior, quase todo número do PayFlow é **premissa** (volume, pico, custo, prazo) e está marcado assim: **substitua cada premissa por uma medição** quando o PayFlow existir e, se a medição desmentir a aula, **corrija a aula e registre**.

---

## Parte 2: Projeto PayFlow avaliado por checklist

**Entrega:** o repositório `payflow` na tag **`payflow-S8`** (e `payflow-S1` … `payflow-S7` no caminho: `git tag -l 'payflow-S*'`). Marque `[x]` só com **evidência** (comando e saída, captura de tela ou link do *commit*). Onde a aula diz **"não executado"**, escreva isso ao lado até executar: é honesto e aceitável. **Premissa não é evidência:** um item cujo número ainda é "a medir" fica em aberto.

> **Estado em que o PayFlow chega aqui (por leitura):** monólito modular com **saga de pagamento** orquestrada (`payment`), **extrato CQRS** (`statement`, com `X-Min-Seq` e reconstrução), **idempotência ponta a ponta** (`idempotency_keys`), **limite** no gateway, **ledger particionado** por mês com **arquivamento** e saldos de fechamento, **réplica dedicada** ao `reporting`, **SLOs propostos** com alertas por taxa de queima, *runbooks* e postmortems verificados por máquina, **política de autorização** testada com mutantes, **papéis do banco** e **trilha de auditoria** (`audit_events`), **estimativa de custo** datada, ***landing zone*** desenhada, **processo de decisão** (ADR, RFC, design doc), **RFC-0001** das recorrentes **proposta** e **README de portfólio**. **Nada disso foi compilado** neste treinamento. O contrato `/api/v1` só cresceu de forma **aditiva**, e o dinheiro continua **uma transação** (decisão 104).

### 2.1 Linha de base
- [ ] `git tag -l 'payflow-S*'` mostra **S1 … S8**; a **suíte inteira** (J5–S8) fica verde **de uma vez** num **clone limpo** (JDK 25, Docker no ar). *(Depende das pendências 10 e 11 da Parte 1.)*
- [ ] O **mapa de migrações** (item 2.10) está resolvido e `flyway migrate` roda **sem erro de validação** num banco **vazio** e num banco **com massa** (a da S3).
- [ ] ADRs 0024–0034 revisadas; `DocLab adr` sem erros (pendência 4); a **RFC-0001** tem decisão registrada (pendência 2).

### 2.2 Sistemas distribuídos (S1)
- [ ] Tabela **operação → consistência → mecanismo**; **nenhuma decisão financeira** lê réplica ou *read model* (decisões 185 e 188).
- [ ] **Extrato CQRS:** projetor idempotente, **lacuna recusada**, `rebuild(accountId)`, invariante `Σ read model = accounts.balance` por *job*, `X-Min-Seq` com *fallback* **testado com o projetor parado**; métricas `statement_projection_lag_seconds`, `statement_gap_total` e `statement_fallback_total` (S1.05).
- [ ] **Saga:** cada passo classificado (compensável, pivô, repetível), **falha injetada em cada passo** com conservação do dinheiro, **timeout = resultado desconhecido**; idempotência **por salto** (`sagaId:passo`, até o PSP) com os 5 testes (S1.07, S1.08).
- [ ] `/api/**` com `no-store` (teste que **falha** sem o cabeçalho); limite com **429 + `Retry-After`** e **modo degradado** testado; ordem por `account_seq` e ArchUnit contra `Instant.now()` no domínio (S1.04, S1.09, S1.10).

### 2.3 Performance (S2)
- [ ] A **primeira linha de base real** do PayFlow em `docs/perf/experimentos.md`; os alvos "a validar" do `orcamento.md` viraram **números medidos** (S2.01).
- [ ] `payflow.js` (k6) em **modelo aberto**, com limiares que **reprovam**; o **joelho** da curva medido; JFR `default` contínuo; *heap dump* em volume próprio + `ExitOnOutOfMemoryError`; política do *heap dump* escrita (S2.02–S2.06).
- [ ] Partida do Spring Boot medida (15 execuções) com os gatilhos de cache AOT, CRaC e nativo; `perf-bench/` **fora** do CI (S2.07, S2.08).

### 2.4 Dados (S3)
- [ ] Toda migração com `lock_timeout`/`statement_timeout` no topo; `CONCURRENTLY` em *script* próprio; *backfill* **fora** do Flyway, em lotes (S3.01).
- [ ] Ledger **`RANGE (created_at)` mensal**, **sem `DEFAULT`**, **12 meses à frente** com alerta (< 90 dias); a troca `V19`/`V19_1` **com carga e 0 falhas** (S3.02).
- [ ] `reporting` com **segundo `DataSource`** + ArchUnit + `LazyConnectionDataSourceProxy`; teste que **prova qual nó** serviu (S3.03).
- [ ] Arquivamento nos **6 passos** com restauração **antes** do `DROP`, `account_period_balances` e o invariante **abertura + Σ vivo = saldo** (S3.04); decisão **CDC × relé** com gatilhos (S3.06); `phone` → `phone_e164` com **5 fases e 4 portões** (S3.09).

### 2.5 Confiabilidade (S4)
- [ ] SLIs e SLOs **assinados** por produto (ou "proposta" com data); `slo-rules.yml` com os **rótulos reais** de `http_server_requests_seconds_*` e `promtool test rules` com os **2 mutantes** reprovando (S4.01).
- [ ] Processo de incidente escrito; **todos** os *runbooks* passam no `DocLint` e no `check-runbooks.sh` **no CI**; gatilhos de postmortem escritos (S4.02, S4.03).
- [ ] Caos: **E1 executado** com registro; RPO/RTO por cenário com **um ensaio de restauração** e o **RTO medido** no lugar da premissa (S4.04, S4.05).
- [ ] Conta do **N+1** e **conta de conexões** com o `max_connections` **real**; teto de `desired_count`; `modelo.md` de capacidade com números **medidos** (S4.06, S4.07).

### 2.6 Segurança (S5)
- [ ] `AuthorizationPolicyTest` passa e **cada mutante falha**; teste de arquitetura reprova *controller* sem checagem (S5.01).
- [ ] `roles.sql`: a aplicação **não** faz `UPDATE`/`DELETE` no ledger (teste que **tenta e falha**); `V25` com o `DENIED` gravado **mesmo com a transação revertida** (S5.01, S5.06).
- [ ] Tabela "o que cifrar" **por ameaça**; chave do *backup* **na conta de *backup***; mTLS **não adotado**, com gatilhos escritos (S5.02, S5.03).
- [ ] `security.yml` no `actionlint`; `check-waivers.py` com o caso ruim **falhando**; inventário LGPD com *linter*; **PAN fora** do PayFlow; `revisao-s5.md` com a seção "Não coberto" (S5.04, S5.05, S5.07).

### 2.7 Nuvem e custo (S6) *(💸 só a parte AWS)*
- [ ] Revisão Well-Architected passando no *linter*; cada HIGH com dono, prazo e ação (S6.01).
- [ ] `ProviderSdkRulesTest` **reprova** a *fixture* vazada e passa no código real (S6.03).
- [ ] BOM e `prices.csv` **refeitos** (`fetch-prices.py`) **na data** dos testes; estimativa conciliada com a fatura **se** houve conta AWS (S6.04).
- [ ] 💸 **Só se você fez a parte AWS:** orçamento por conta com previsão 80% e real 100%, anomalia **por serviço**, SCPs aplicadas **primeiro** na OU de *Policy Staging*, `terraform destroy` e conferência de que **não sobrou nada**. Se não fez, escreva "não executado" (S6.05, S6.06).

### 2.8 Liderança (S7)
- [ ] RFC-0001 decidida; design doc passando no `DocLab design`; estimativa no **p85** e registro de riscos com **dono e gatilho** (S7.01–S7.03).
- [ ] Registro de dívida com os ***hotspots* do seu repositório**; plano do extrato com o **critério numérico** de desligar o velho (0 BUG em ≥ 30 mil leituras) (S7.04).
- [ ] `CONTRIBUTING.md`, `CODEOWNERS`, PIT com **piso medido** nos pacotes de dinheiro e autorização, política de testes instáveis (S7.05); política de *error budget* **assinada** (S7.06); política de IA (S7.07).

### 2.9 Portfólio (S8)
- [ ] README PT/EN passando no `PortfolioLint`; "como rodar" executado num **clone limpo**; `PortfolioLint hygiene` sem `ERROR`; histórico varrido **antes** do primeiro *push* público; licença decidida (S8.01).

### 2.10 O mapa de migrações (item **obrigatório** do nível)
No Pleno, o item que atravessava os módulos era a **conta de conexões**. No Sênior é a **ordem das migrações**: ela atravessa **P3, P10, S1, S3, S5 e S7**, e a revisão achou **duas colisões** (Parte 1). Elas **não** são um erro de SQL: são o caso que a **J3.12** já ensinou, "dois colegas criaram a mesma versão em *branches* diferentes", aqui entre **módulos** escritos em datas diferentes. A J3.12 mostrou o que o Flyway faz quando aparece uma versão **menor** que a última aplicada (a validação **recusa**; `outOfOrder` é **exceção**) e a regra do time: **o segundo a mesclar renumera**.

Preencha a última coluna com o número **real** do seu repositório, **na ordem em que você implementar**:

| Versão nas aulas | Arquivo (nome nas aulas) | Aula | Tipo | Pré-condição (portão) | Seu número |
|---|---|---|---|---|---|
| V12 | `V12__payment_requests.sql` | P3.10 | aditiva | — | ___ |
| **V12** (colide) | `V12__users_password_hash_nullable.sql` | P10.03 | *expand* | — | ___ |
| **V13** (colide) | remover `users.password_hash` (sem nome de arquivo) | P10, decisão 173 | ***contract*** | **todos** os usuários no Keycloak | ___ |
| **V13** | `V13__ledger_account_seq.sql` | S1.05 | *expand* (+ *backfill* fora do Flyway) | — | ___ |
| V14 | `V14__statement_read_model.sql` | S1.05 | aditiva | a anterior | ___ |
| V15 | `V15__payment_saga.sql` | S1.07 | aditiva | — | ___ |
| V16 | `V16__payment_request_short_code.sql` | S1.11 | *expand* | a `payment_requests` (P3.10) | ___ |
| V17 | `V17__idempotency_keys.sql` | S1.08 | aditiva | — | ___ |
| V18 | `V18__expand_users_phone_e164.sql` | S3.09 | *expand* | — | ___ |
| V19 / V19_1 | `V19__ledger_partition_prepare.sql` / `V19_1__ledger_partition_swap.sql` | S3.02 | preparação *online* / troca | índice único `CONCURRENTLY` + `CHECK` validado | ___ |
| V20 | `V20__archive_manifest….sql` (**dois nomes**: escolha um) | S3.04 | aditiva | a troca do ledger | ___ |
| V21 | `V21__outbox_cdc_publication.sql` | S3.06 | aditiva, **só se o CDC for adotado** | decisão 212 | ___ |
| V22 | `V22__daily_volume.sql` | S3.08 | aditiva | — | ___ |
| V23 | `V23__users_phone_e164_check.sql` | S3.09 | ***contract*** (`NOT VALID` + `VALIDATE`) | *backfill* e portões da S3.09 | ___ |
| V24 | `V24__drop_users_phone.sql` | S3.09 | ***contract*** | janela de reversão vencida | ___ |
| V25 | `V25__audit_events.sql` | S5.06 | aditiva | — | ___ |
| V26 | `recurring_transfers`, `recurring_runs` (reservada) | S7.02 | aditiva | RFC-0001 **aceita** | ___ |
| V27+ | livre | — | — | — | — |

**Regras para preencher** (decisão 287):
1. Os números das aulas são **rótulos de plano**, não promessas. O número **real** sai da **ordem de *merge***: quem chega depois **renumera** (J3.12). Se você implementou a P3.10 como está, a P10.03 vira **V13** e todas as seguintes ganham **+1**.
2. As **contrações** (remover `password_hash`, remover `phone`) ficam com o **próximo número livre no dia em que o portão passar**, nunca com um número "guardado" no meio do plano: guardar número vira versão menor que a última aplicada.
3. Registre o mapa em `docs/migracoes.md` (esta tabela, preenchida) e ponha no CI um teste que **lista** `src/main/resources/db/migration` e **falha** se duas migrações tiverem a **mesma versão** (não dependa da mensagem do Flyway para isso: ela **não** foi reproduzida neste treinamento).
4. **Não** reescreva as aulas para "acertar" os números: a aula é o plano; o `docs/migracoes.md` é o que aconteceu.

**Mapa preenchido na validação final (2026-11-24, PostgreSQL 18, PayFlow reconstruído).** Apliquei **em ordem de *merge*** o SQL que as aulas trazem, num banco **vazio** e num banco **com massa** (50.000 usuários e contas, 300.000 lançamentos). Resultado: **16 migrações passam nos dois bancos** (V1–V7 do PayFlow real e as 9 seguintes); a 17ª (**V18**, `phone_e164`) **falha em banco vazio**: `column "phone" of relation "users" does not exist`.

| Seu número (real) | Nome nas aulas | Aula | Resultado |
|---|---|---|---|
| V1–V4 | `create_users` … `create_indexes` | J3.12 | ok |
| **V5** | `V5__ledger_covering_index` | P2.05 | ok (`CONCURRENTLY`) |
| **V6** | `V6__drop_old_ledger_index` | P2.05 | ok |
| **V7** | `V11__outbox_events` (aula) | P3.09 | ok (`outbox_events`, índice parcial) |
| V8 | `V9__shedlock` | P3.02 | ok |
| V9 | `V10__processed_messages` | P3.07 | ok |
| V10 | `V12__payment_requests` | P3.10 | ok |
| V11 | `V12__users_password_hash_nullable` | P10.03 | ok |
| V12 | `V13__ledger_account_seq` | S1.05 | ok (`CREATE UNIQUE INDEX CONCURRENTLY`, 107 ms com massa) |
| V13 | `V14__statement_read_model` | S1.05 | ok |
| V14 | `V15__payment_saga` | S1.07 | ok (`account_holds` + `payment_sagas`) |
| V15 | `V16__payment_request_short_code` | S1.11 | ok |
| V16 | `V17__idempotency_keys` | S1.08 | ok |
| V17 | `V18__expand_users_phone_e164` | S3.01/S3.09 | **falha sem `users.phone`**; com a coluna: **72 ms**, o gatilho preenche `+5511987654321` |
| *(não montadas)* | `V19`, `V19_1`, `V20`–`V25` | S3.02, S3.04, S3.06, S3.08, S3.09, S5.06 | o SQL está em *scripts* do laboratório ou em trechos de texto, **não** em arquivos de migração completos (a `V21` é só `CREATE PUBLICATION`; a `V25` só o DDL da seção 4 da S5.06) |

**Quatro achados do mapa** (nenhuma aula foi reescrita; vão para "Correções pendentes"): (1) **a colisão não é só V12 e V13**: o `V5` do **exercício** da J3.12 (`V5__add_users_phone`) colide com o `V5__ledger_covering_index` da P2.05, e o `V7__ledger_monthly_summary` da P2.11 com o `V7` do *outbox*; (2) **a S3.01/S3.09 dependem de `users.phone`**, que **nenhuma migração do plano cria** (só esse exercício): sem ele o V18 falha; (3) **`payment_sagas.payment_request_id` é `UUID` (S1.07) e `payment_requests.id` é `text` (P3.10)**: não há `FOREIGN KEY`, então o banco **não acusa** o descasamento; (4) **o PayFlow real só tem V1–V7**: o número do plano é um rótulo, o `docs/migracoes.md` (no PayFlow) registra o que aconteceu.

**Nota do projeto:** conte os itens marcados **com evidência** (os "não executado" **não** contam como feitos, mas também não como erro). O item 2.10 é **obrigatório**: sem ele, o nível não fecha.
- **≥ 80%** dos itens **executáveis** ✅ pronto · **60–79%** ⚠️ pronto com plano de reforço (itens abertos e a aula que os cobre) · **< 60%** ❌ refaça os módulos indicados antes de declarar o nível concluído.

---

## Parte 3: Quiz

### 3.1 Quiz por módulo
Cada módulo tem o seu quiz de 10 questões com gabarito no `checkpoint.md` dele. Refaça **sem consultar** e anote a nota. Se alguma ficar abaixo de 7, releia as aulas da coluna da direita **e rode de novo o laboratório** do módulo (no Sênior, o laboratório é onde o número da aula nasceu).

| Módulo | Quiz | Nota (0–10) | Se < 7, releia |
|---|---|---|---|
| S1 | [`S1-sistemas-distribuidos/checkpoint.md`](S1-sistemas-distribuidos/checkpoint.md) | ___ | S1.01–S1.13 |
| S2 | [`S2-performance-eficiencia/checkpoint.md`](S2-performance-eficiencia/checkpoint.md) | ___ | S2.01–S2.08 |
| S3 | [`S3-dados-em-escala/checkpoint.md`](S3-dados-em-escala/checkpoint.md) | ___ | S3.01–S3.09 |
| S4 | [`S4-confiabilidade-sre/checkpoint.md`](S4-confiabilidade-sre/checkpoint.md) | ___ | S4.01–S4.07 |
| S5 | [`S5-seguranca-avancada/checkpoint.md`](S5-seguranca-avancada/checkpoint.md) | ___ | S5.01–S5.07 |
| S6 | [`S6-arquitetura-nuvem-custo/checkpoint.md`](S6-arquitetura-nuvem-custo/checkpoint.md) | ___ | S6.01–S6.06 |
| S7 | [`S7-lideranca-tecnica/checkpoint.md`](S7-lideranca-tecnica/checkpoint.md) | ___ | S7.01–S7.07 |
| S8 | [`S8-carreira/checkpoint.md`](S8-carreira/checkpoint.md) | ___ | S8.01–S8.05 |

### 3.2 Quiz integrador (20 questões, ~50 min, sem consulta)
As questões **cruzam módulos** e pedem **número e *trade-off***, como numa conversa de arquitetura. Cada uma indica as aulas em que o assunto foi **medido ou explicado**. "Depende" sem dizer **de quê** vale zero.

1. Um colega diz: "o PayFlow é um sistema CP". Corrija em quatro linhas: o que é linearizável, o que tem consistência de sessão, o que é eventual e o que **nunca** se usa para resolver conflito de dado de negócio. *(S1.02, S1.05, S1.10)*
2. O primário está a 75% de CPU no pico e alguém propõe *sharding* por `account_id`, com UUIDv7 como chave de faixa. Diga os **gatilhos** que a ADR 0024 exige, o que vem **antes** do *sharding* e por que UUIDv7 é má chave de faixa. *(S1.01, S1.03, S3.02)*
3. Na saga de pagamento, a chamada ao PSP para **capturar** estoura o *timeout*. O orquestrador deve compensar (liberar a reserva)? Explique "*timeout* = resultado desconhecido", o papel do passo **pivô** e a chave que vai ao PSP. *(S1.07, S1.08, S1.12)*
4. A API devolve **400**, **409** e **422** para o cabeçalho `Idempotency-Key`. Diga quando sai cada um, onde a chave mora, por que ela e o efeito vão **na mesma transação** e a diferença medida no modelo com e sem chave. *(S1.08)*
5. Por que `/api/**` sai com `Cache-Control: no-store`, e de onde vem o ganho de leitura? Que dois cuidados o cache do resumo mensal exige, e o que **nunca** vai para *write-behind*? *(S1.04, S1.05)*
6. O p99 do teste de carga de um colega deu 0,2 s; o seu, **com a mesma falha de 3 s injetada**, deu ~3 s. Quem está certo? Explique a omissão coordenada, por que não existe "média de percentis" e a regra "uma mudança por vez". *(S2.01, S2.06)*
7. Um *pod* morreu com `OutOfMemoryError` às 3 h de uma sexta. Que *flags* garantem a evidência, para onde vai o *dump*, por que ele é **dado pessoal** e quando o ZGC seria a resposta (e quando ele **falha**)? *(S2.04, S2.05, S5.05)*
8. Um `ALTER TABLE` que leva 2 ms derrubou o *checkout* por 40 s. Explique a **fila de *locks***, as proteções no topo de toda migração e a sequência para tornar uma coluna obrigatória **sem parar** o sistema. *(S3.01, S3.09)*
9. Você liga `@Transactional(readOnly = true)` e um `AbstractRoutingDataSource` para mandar leituras à réplica. Nada muda e **não há erro**. Por quê? E por que o PayFlow **não** faz roteamento global? *(S3.03)*
10. Arquivar um mês do ledger: qual a ordem dos 6 passos, o que muda no **invariante do saldo** e por que a aplicação **não consegue** fazer o `DROP`? *(S3.04, S3.02)*
11. O consumidor do Debezium parou numa sexta à noite. O que acontece com o disco do primário, qual a fórmula para escolher `max_slot_wal_keep_size` e qual é o **preço** de ter um limite? *(S3.06, S4.07)*
12. O alerta "5xx > 2% por 5 min" **nunca tocou** e, mesmo assim, o mês estourou o orçamento de erro. Como? Escreva as condições de **página** e de ***ticket*** da decisão 218 e diga o que o **14,4×** significa. *(S4.01)*
13. Na revisão de um postmortem alguém escreve "causa raiz: erro humano do operador". Reescreva a frase, diga o que o `DocLint postmortem` recusa, quais são os **gatilhos** de postmortem e como se calcula o **impacto em orçamento**. *(S4.02, S4.03)*
14. Com 2 AZs e 4 tarefas a 60% de CPU, uma AZ cai e, 5 minutos depois, alguém dispara um *deploy*. Conte a **CPU** e as **conexões** e diga a regra. *(S4.06, S4.07)*
15. Cinco camadas **independentes** que falham 30% cada deixam passar quanto? E se compartilharem uma **causa comum** de 1%? Dê duas causas comuns do PayFlow e diga o que o mTLS **garante** e o que **não** garante. *(S5.01, S5.02)*
16. Explique a criptografia **envelope**, o que o **contexto (AAD)** impede, quanto custa **rotacionar** a KEK em 10 milhões de registros e por que a chave do *backup* mora **em outra conta**. *(S5.03, S4.05)*
17. Uma tentativa **negada** de ler o extrato de outra pessoa precisa aparecer na trilha de auditoria, mas a transação da requisição é revertida. Como? O que fica **fora** do evento? Em que **prazo** se comunica um incidente com dados pessoais? *(S5.06, S5.05)*
18. O diretor pergunta: "se o tráfego dobrar, a conta da AWS dobra?". Responda com os números da estimativa da S6.04 e diga **quando** comprar *Savings Plans* e onde Spot é **proibido**. *(S6.04, S6.01)*
19. O time quer trocar `/entries` por `/statement`. Como você **prova** que o novo está certo antes de desligar o velho, quantas leituras bastam e por que **100% de cobertura** de linhas não basta nos testes? *(S7.04, S7.05)*
20. Numa entrevista sênior pedem "conte uma decisão técnica difícil". Use a RFC-0001 (ou uma sua): quem decide (DACI), o que a ADR precisa ter, como apresentar a **estimativa** (o que é o p85) e como contar em **STAR** sem culpa e na primeira pessoa. *(S7.01–S7.03, S8.04)*

<details>
<summary>Gabarito do quiz integrador (respostas-modelo)</summary>

1. **Nem CP nem AP: consistência por operação** (decisão 185). **Saldo e transferência** são linearizáveis **no primário** e **recusam** sem primário (corretude vence disponibilidade no caminho do dinheiro). O **extrato** tem **consistência de sessão**: o `POST` devolve o `ledgerSeq`, a leitura manda `X-Min-Seq`, espera até 500 ms e, se não chegou, cai no ledger (S1.05). **Contadores e resumos** são **eventuais**, com regra de fusão definida. **Nunca** "última escrita vence" por relógio de parede: relógios divergem (S1.10); a ordem vem de `account_seq`, o `created_at` vem do banco e o `Clock` é injetado.
2. **Gatilhos** (decisão 186): primário acima de ~70% de CPU/IOPS **depois** das otimizações e a maior tabela além de um limite **medido** de manutenção. **Antes:** medir a espera por *lock* (a parte serial limita o ganho: Amdahl, decisão 184), índices e consultas, escalar o primário **verticalmente** (estado escala na vertical), réplica para o `reporting`, CQRS do extrato e **particionar a tabela** no mesmo banco (S3.02). **UUIDv7 é ordenado no tempo**: como chave de faixa, **100% das escritas** caem no *shard* mais novo (medido na S1.03). E *sharding* transforma a transferência entre *shards* numa **saga** (perde o ACID de um banco). Nota: a partição mensal **não** acelerou o extrato por conta (planejamento de ~7 ms com 26 partições sem filtro de data); o ganho é **por período** e **retenção** (decisão 208).
3. **Não** compense às cegas (decisões 190 e 191). *Timeout* = **não sei se capturou**: **repita com a mesma chave** (`sagaId:capturar`, propagada ao PSP) ou **consulte** o PSP antes de repetir. **Capturar é o pivô**: depois dele a saga só anda **para frente** (lança no ledger, notifica); compensar dinheiro por falha de um passo **repetível** é proibido (medido no modelo). Se a dúvida persistir, a reserva **expira em 24 h** e o pagamento vai para `NEEDS_ATTENTION`, com alerta e dono; a **conciliação** com o PSP (S1.12) acha o que sobrar (por exemplo, duas capturas para um pagamento). 2PC **não**.
4. **400**: chave ausente. **409**: a mesma chave ainda **em andamento**. **422**: a mesma chave com **corpo diferente** (compara o *hash* do corpo canônico). Escopo `(owner, key)`, tabela `idempotency_keys` (V17), validade de 24 h a 7 dias. **Mesma transação** porque, se o efeito confirmar e a chave não (ou o contrário), a queda entre um e outro **duplica** ou **perde** a resposta. Medido no modelo (decisão 191): **sem chave, 14.253 débitos para 10.000 pedidos; com chave, 10.000**.
5. Um CDN ou *proxy* que guardasse uma resposta da API poderia servir **o extrato de uma pessoa a outra**: `no-store` em `/api/**`, com **teste que falha** sem o cabeçalho; CDN **só** para estáticos com *hash* no nome (decisão 187). O ganho de leitura vem do **modelo de leitura** (CQRS) e do Redis com **chave que contém o `sub`** e um **prefixo de versão**. Cuidados: **TTL com *jitter*** (não expirar tudo junto) e ***single-flight*** (medido: **200 → 1** consultas ao banco no *stampede*). ***Write-behind* nunca** para dinheiro.
6. **Você.** No modelo **fechado** o gerador espera cada resposta: quando o servidor trava, ele **para de mandar** e o teste não registra o atraso que o usuário teria sofrido (omissão coordenada). Medido na S2.06: a **mesma** falha de 3 s deu **p99 ≈ 0,2 s no fechado e ≈ 3 s no aberto**; por isso todo teste de API usa **chegada constante** (`constant-arrival-rate`). Percentis **não** se somam nem se tiram média: some os **histogramas** e calcule de novo (decisão 198). E o método: **medir → hipótese → mudar UMA coisa → medir de novo**, com ≥ 15 repetições e a mediana.
7. `-XX:+HeapDumpOnOutOfMemoryError` com `HeapDumpPath` num **volume próprio** (tamanho ≥ heap máximo; **não** o disco do nó) **e** `-XX:+ExitOnOutOfMemoryError` (medido: o *dump* sai **antes** do `Terminating due to ...`), mais um **alerta do piso do heap** depois do GC (decisão 201). O *dump* contém **dados de clientes** em memória: acesso restrito, cifrado, retenção curta, **nunca** anexado a *ticket*, e entra no **inventário LGPD** (decisão 240). Aumentar o heap **adia**. O G1 fica (decisão 202); o **ZGC** só se a pausa de GC explicar parte do p99 **e** houver folga: medido, ZGC com 700 MB para 600 MB vivos **falhou com `OutOfMemoryError`**; com folga, p99,9 de 0,3 ms.
8. O DDL pede um ***lock* forte** (`ACCESS EXCLUSIVE`) e **espera** atrás de uma transação longa; **todo mundo** que chega depois espera **atrás dele** (fila de *locks*): 2 ms de DDL viram 40 s de fila. Proteção: `SET LOCAL lock_timeout = '2s'` e `statement_timeout` no topo de **toda** migração em tabela quente (**falhar rápido e repetir**), migração como passo próprio (decisão 207). Coluna obrigatória **sem parar**: `CHECK ... NOT VALID` (pede o *lock* forte, mas **não varre**: ms) → `VALIDATE` (varre com um *lock* que **não** bloqueia leitura nem escrita) → `SET NOT NULL` (ou, no PG 18, `NOT NULL ... NOT VALID`). E a sequência geral: ***expand* → *backfill* em lotes fora do Flyway → verificar → trocar → *contract*** (a S3.09 fez isso com `phone` em 5 fases e 4 portões).
9. Sem **`LazyConnectionDataSourceProxy`**, a conexão é pega **antes** de o `readOnly` ser conhecido: medido, **0 de 10** transações `readOnly` foram à réplica, **sem erro** (decisão 209). O PayFlow **não** roteia globalmente: um **segundo `DataSource`** só para o módulo `reporting` (+ ArchUnit), com *gate* e *fallback* **limitado** e contado na **conta de conexões**; saldo, transferência e idempotência **sempre** no primário. Medido: com a réplica 2 s atrasada, **20/20** leituras erradas sem token e **0** com o token (LSN).
10. **Impressão digital → `DETACH CONCURRENTLY` → exportar + `sha256` → restaurar e comparar → manifesto → `DROP`**: a restauração vem **antes** do `DROP` (medido: `md5` idêntico) (decisão 210). **Antes** de tirar o mês, grave os **saldos de fechamento** (`account_period_balances`): o invariante passa a ser **abertura + Σ vivo = saldo**; sem isso, o *job* de conferência dispara um **alarme falso** (ou alguém "corrige" o saldo). A aplicação **não é dona** da tabela (medido: `permission denied`/`must be owner`), e o `DROP` **não passa** pela *trigger* *append-only* (ela vigia **linhas**): por isso o arquivamento tem um **papel próprio**.
11. O *slot* **retém WAL** enquanto o consumidor não confirma: medido, **1 milhão de linhas atualizadas ≈ 375 MB** retidos (decisão 212); sem limite, nas premissas da S4.07, o disco do primário **enche em 40 h** e **para o banco**, ou seja, o dinheiro. Fórmula (decisão 227): **tempo para consertar × taxa de WAL ≤ `max_slot_wal_keep_size` ≤ espaço livre − margem** (8 h × 3 GB/h = 24 GB … 120 − 40 = 80 GB → ~40 GB). **Preço:** passado o limite, o *slot* é **invalidado** (`wal_removed`) e o consumidor **perde o fio** (precisa de um novo *snapshot*). Daí: dono do *slot*, alerta de retenção (bytes e minutos), `heartbeat`, e o relé por consulta **mantido** até haver ≥ 2 consumidores.
12. Porque o orçamento se gasta com **queima lenta** e **picos curtos**, que um limiar fixo não vê: no modelo da S4.01, "5xx > 2% por 5 min" **nunca** chamou num incidente de 0,8% por 12 h (**16,8%** do orçamento do mês) nem num de 1 min a cada 30 min (**29,2%**). Regra (decisão 218): **página** se **1 h e 5 min** queimam acima de **14,4×**, **ou** **6 h e 30 min** acima de **6×**; ***ticket*** se **3 d e 6 h** acima de **1×**; janela curta para o alerta **apagar**; tráfego mínimo de 0,1 req/s. **14,4×** = gastar **2% do orçamento de 30 dias em 1 hora** (nesse ritmo, o orçamento acaba em **50 h**). Para 99,9% a 5 req/s, o orçamento é **43,2 min** ou **12.960 requisições** (decisão 217).
13. "Erro humano" é **sintoma**: escreva **o que o sistema permitia** (um comando sem confirmação, um *runbook* sem pré-checagem, um alerta como *ticket* à noite). O `DocLint postmortem` recusa **seção faltando** (as 7), **palavras de culpa**, **ação vaga** ("ter mais cuidado") e **ação sem dono (@) ou sem data** (decisão 221). **Gatilhos:** SEV1, página de SLO que gastou **> 5%** do orçamento, **perda de dado**, **falha de monitoramento**, quase-acidente com aprendizado. **Impacto em orçamento** = duração × fração afetada × taxa ÷ orçamento (exemplo **sintético** da S4.03: 22 min × 80% × 5 req/s = 5.280 de 12.960 = **40,7%**). Ações: **prevenir > detectar > mitigar**, e **uma delas repete o experimento** que prova a correção.
14. **CPU:** 4 tarefas a 60% em 2 AZs; perder uma deixa 2 tarefas com a carga de 4: **120%** até o *autoscaling* reagir (premissa: ~3 min); para passar por isso a ≤ 85% sem escalar, o regime teria de ser **≤ 42%**; com 3 AZs, 60% vira 90% (decisão 224). **Conexões:** recuperado para 6 tarefas, um *deploy* com `maximum_percent = 200` abre até **6 × 2 × 10 = 120 > 90** (premissa `max_connections = 100`, reserva 10). **Regra:** **sem *deploy* enquanto degradado**, e o **teto do *autoscaling* é a conta de conexões**, não a CPU (decisão 226: 4 tarefas com fator 2).
15. Independentes: **0,3⁵ = 0,243%** (simulado: 0,2423%). Com **causa comum de 1%**: **1,24%** (com 5%: 5,2%), ou seja, **conte causas compartilhadas, não caixas** (decisão 229). Causas comuns do PayFlow: **o mesmo usuário do banco**, **o mesmo segredo entre ambientes**, **a rede como única barreira**, **banco e *backup* na mesma conta**, **a mesma pessoa escreve e aprova**. **mTLS** dá **identidade do canal**, **não autorização**: medido, um certificado válido da mesma CA recebeu **403** (decisão 232); valida cadeia, SAN e `clientAuth`; **não** adotado agora, com gatilhos (outro time, rede não confiável, exigência de parceiro ou PSP).
16. Cada registro tem a **sua** chave de dados (DEK, AES-256-GCM), e a DEK é guardada **embrulhada** pela chave mestra (KEK) do KMS (decisão 235). O **contexto** (`tabela|id|coluna`) entra como **AAD**: **mover** o valor cifrado para outra linha ou coluna, ou **adulterá-lo**, faz a decifragem **falhar**. Rotacionar a KEK = ***re-wrap*** das DEKs (os dados **não** são cifrados de novo): **2 chamadas ao KMS por registro → 20 milhões** de chamadas para 10 milhões. A chave do *backup* fica **na conta de *backup*** (decisão 237), com política que nega `ScheduleKeyDeletion` fora do papel de emergência e restauração **ensaiada com a chave de destino**: se ela morasse na produção, comprometer (ou apagar) a produção tornaria o *backup* **ilegível**, a mesma causa comum da questão 15.
17. Grave o `DENIED` **fora** da transação que vai reverter: `REQUIRES_NEW` (ou Outbox) (decisão 242). O evento tem **ator (id)**, **ação** de vocabulário fechado, **recurso**, **resultado** (inclui `DENIED`) e `request_id`; **sem** PAN, CPF ou e-mail (*linter*). A `audit_events` (V25) é *append-only*, mas **o dono do banco desliga o gatilho** (medido): por isso há uma **cadeia de HMAC** com **âncora externa** (outra conta, selador separado). **Incidente com dados pessoais:** comunicar à ANPD e aos titulares em **3 dias úteis** (Res. CD/ANPD nº 15/2024, conferida na página da ANPD; o texto da lei **não** foi conferido) (decisão 240).
18. **Não dobra.** Na estimativa da S6.04 (`out-s604-estimate.txt`, preços de novembro de 2026), `sa-east-1` custa **US$ 1.424,28/mês**, dos quais **US$ 1.006,93 (71%) são fixos**: tráfego ×2 → **US$ 1.841,63 (×1,29)**; em `us-east-1`, US$ 887,89 → US$ 1.169,53 (×1,32). Custo unitário: **US$ 0,11 por 1.000 transferências** (produção, premissa de 10 milhões por mês). A região é decisão **aberta** (`sa-east-1` = ×1,60). **Compromissos:** nada antes de **3 meses de uso real**; depois, *Compute Savings Plan* de 1 ano, sem adiantamento, **no piso medido** (decisão 254). **Spot:** só trabalho **interrompível com *checkpoint*** (relatórios, reprocessamento, CI); **nunca** no caminho síncrono do dinheiro (decisão 255).
19. ***Branch by abstraction*** (`StatementReader`), **leitura em paralelo** amostrada (10%) e **comparada na mesma versão** (`seq ≤ maxSeq` do novo), métrica `statement_shadow_total{result=match|lag|bug}` (decisão 266). Comparar com `equals()` dá **11,6% de falsos alarmes** no projetor **correto** (é só atraso). Desligar o velho com **0 BUG em ≥ 30 mil leituras** (regra de três: taxa < 3/n, ou seja, **< 0,01%**), uso de `/entries` zerado por 30 dias e `Deprecation`/`Sunset`. Cobertura: medido na S7.05, **100% de linhas e ramos** com só **6 de 18** mutantes mortos; testes de **fronteira** mataram **18/18**: por isso **PIT** nos pacotes de dinheiro e autorização, com **piso medido** (decisão 270).
20. **RFC** quando a decisão cruza módulos ou times, é cara de reverter ou polêmica; **DACI com um só decisor** e prazo de comentários (decisão 260). **ADR curta, uma decisão:** Status, Contexto, Opções (≥ 2, incluindo "não fazer nada" quando couber), Decisão, Consequências **(+) e (−)**, **Revisar quando**. **Design doc** quando a estimativa passa de ~2 semanas **ou** toca dinheiro (decisão 261). **Estimativa ≠ meta ≠ compromisso:** três pontos por tarefa, Monte Carlo, **compromisso no p85** (85% das simulações terminam até ali), premissas escritas e **reestimativa nos marcos**; nunca somar os "mais prováveis" (no modelo da S7.03, essa soma tinha **0,8%** de chance) (decisão 263). **STAR:** situação e tarefa curtas, **Ação na primeira pessoa** (a parte mais longa: o que **eu** fiz), resultado com número ou aprendizado, **sem culpar ninguém** e **sem inventar** (decisão 282).

</details>

**Nota:** 18–20 ✅ · 14–17 ⚠️ (reforce os módulos das questões erradas) · < 14 ❌ (refaça os quizzes por módulo e os laboratórios).

---

## Parte 4: Design doc completo de uma evolução grande ("PayFlow 10×")

> O que a trilha pede ao fim do Sênior: **um design doc completo de uma evolução grande do projeto, com ADRs, diagramas, SLOs, estimativa de custo e plano de migração**. Você já escreveu um design doc **de módulo** na S7.02 (as recorrentes). Este é **de sistema**: ele obriga a juntar S1 a S7 numa decisão só, com números. O cenário é **sintético**; as **premissas** estão rotuladas e os números "reais" saem das saídas dos laboratórios (arquivo citado ao lado).

### 4.1 O que fazer (sessões CK-S.3 e CK-S.4)
1. **CK-S.3 (1h30):** leia o enunciado (4.2) e as premissas (4.3); responda por escrito às perguntas de 4.4 (são as que você faria ao produto) e **anote as respostas como premissas**. Escreva **contexto e escopo**, **objetivos** (com número), **não-objetivos**, o **desenho** com dois diagramas Mermaid (hoje e proposto) e as **alternativas**. Escreva **3 a 5 ADRs curtas** (uma decisão cada, decisão 260).
2. **CK-S.4 (1h30):** **observabilidade e SLOs**, **capacidade**, **custo** (refaça a estimativa com o laboratório da S6), **segurança**, **plano de entrega** (migração de dados, rampa do parceiro e **reversão**), **riscos** e **estimativa** (p85), **perguntas em aberto** com dono. Passe o documento e as ADRs nos *linters* da S7 e escreva o ***one-pager*** para o negócio (S7.06).
3. **Só depois** compare com o roteiro (4.6) e a rubrica (4.8); as armadilhas (4.9) ficam fechadas até você terminar.
4. Registre no PayFlow: `docs/design/payflow-10x.md` e as ADRs na pasta de ADRs do repositório.

### 4.2 Enunciado
**"PayFlow 10×."** Uma parceria com uma rede de varejo (**fictícia**) promete levar o PayFlow de **10 milhões** para **100 milhões de transferências por mês em 12 meses**. O negócio pede, por escrito:
- manter os **SLOs propostos** de `POST /api/v1/transfers`: **99,9%** de respostas não 5xx e **99%** em até 500 ms, em 30 dias (decisão 217);
- que o **custo por 1.000 transferências não suba**;
- **nenhuma janela de manutenção** com o sistema parado;
- o time continua com **4 pessoas**; o primeiro lote de clientes do parceiro entra no **mês 4**.

Escreva o design doc que você levaria à revisão (S7.02): **o que muda, em que ordem, quanto custa, como sabemos que deu certo e como voltamos atrás**.

### 4.3 Premissas (rotuladas)
| Item | Hoje | Meta (mês 12) | De onde vem |
|---|---|---|---|
| Transferências por mês | 10 milhões | 100 milhões | **premissa** da S6.04 (`out-s604-estimate.txt`) × 10 |
| Transferências por segundo, média | 10.000.000 ÷ (30 × 86.400 s) ≈ **3,9/s** | ≈ **38,6/s** | cálculo |
| Pico de transferências (10× a média) | ≈ **39/s** | ≈ **386/s** | **premissa** (o mesmo fator da S1.12) |
| Requisições da API, média (todas as rotas) | ~78 req/s | ~780 req/s | **premissa** da S6.01 (BOM da S6.04) × 10 |
| Linhas novas no ledger | ~20 milhões/mês (2 por transferência) | ~200 milhões/mês | ADR 0001 (débito + crédito) × premissa |
| Produção | Fargate ARM 0,5 vCPU/1 GiB, 2–6 tarefas; *pool* 10 por tarefa; `db.m7g.large` Multi-AZ + réplica `db.t4g.medium` do `reporting` | ? | P9.02, P2.06, BOM da S6.04 |
| `max_connections` do primário | **100** | ? | **premissa** (P9.08, S4.07) |
| Teto de tarefas pela conta de conexões | **4** (fator de *deploy* 2, reserva 10) | ? | `out-s407-little.txt` (S4.07) |
| CPU do primário no pico | 45% | ? | **inventado** para o exercício |
| Custo mensal (todos os ambientes) | **US$ 1.424,28** (`sa-east-1`) / **US$ 887,89** (`us-east-1`) | ? | `out-s604-estimate.txt` (preços de novembro de 2026) |
| Custo por 1.000 transferências | **US$ 0,11** (produção, `sa-east-1`) / **US$ 0,14** (todos os ambientes) | não pode subir | idem |
| Joelho da curva do PayFlow | **não medido** | — | S2.06: "o do PayFlow se mede" |

### 4.4 Perguntas que você **deve** fazer antes de desenhar (anote as respostas como premissas)
- O pico é **10×** a média ou fica **concentrado** (por exemplo, às 06:00, se a RFC-0001 das recorrentes for aceita, ou no almoço)? Quanto do volume é **depósito** e quanto é **transferência**?
- A parceria traz **clientes novos** (cadastro, KYC, suporte) ou só **volume**? O parceiro chama a **nossa API** (máquina a máquina)?
- **Região** (decisão 247, em aberto): o parceiro ou a regulação exigem dados no Brasil?
- **Retenção** do ledger: os 5 anos são **premissa** (S3.04); o jurídico confirmou?
- O "custo por 1.000" inclui **staging** e **compromissos** (*Savings Plans*)? Quem **assina** os SLOs e a política de *error budget* (decisão 219)?
- O que o negócio prefere **perder primeiro** se a capacidade faltar: relatórios, extrato ou transferências (decisão 227)?

### 4.5 Estrutura do documento (o mínimo que o `DocLab design` cobra, mais o que este checkpoint acrescenta)
```markdown
# Design doc — PayFlow 10×
Status: rascunho · Autor: <você> · Revisores: <por assunto> · Decisor: <um só (DACI)> · Prazo de comentários: <data>

## Contexto e escopo
## Objetivos            (com número: SLOs, custo por 1.000, prazo do mês 4)
## Não-objetivos        (bullets: o que é razoável e NÃO faremos agora, com o gatilho para reabrir)
## Desenho
### Hoje                (diagrama Mermaid)
### Proposto            (diagrama Mermaid)
### Dados               (partição, CQRS do extrato, réplica, arquivamento, mapa de migrações)
### Capacidade          (conta de conexões, N+1 de AZ, Lei de Little, joelho medido)
## Alternativas consideradas
### A — <...>           (cada alternativa diz "porque" foi ou não escolhida)
### B — <...>
### C — <...>
## Segurança            (STRIDE do que muda, parceiro, LGPD, auditoria)
## Observabilidade      (SLOs com número e %, alertas por taxa de queima, SLIs de causa)
## Custo                (US$ com data dos preços, região, fixo × variável, custo por 1.000)
## Plano de entrega     (fases com portão e REVERSÃO numérica; rampa do parceiro; migrações)
## Riscos               (bullets com "dono:" e gatilho)
## Perguntas em aberto  (bullets com "dono:")
## ADRs desta mudança   (links para as 3–5 ADRs curtas)
```
Para conferir (JDK 25, a partir de `3-senior/S7-lideranca-tecnica/lab/`):
```bash
java DocLab.java design <caminho>/docs/design/payflow-10x.md   # 0 erros; avisos lidos um a um
java DocLab.java adr <caminho-da-pasta-de-ADRs>                 # as ADRs novas e o índice com 4 colunas
```
E para o custo (a partir de `3-senior/S6-arquitetura-nuvem-custo/lab/`): copie `samples/bom-payflow.csv` para uma BOM sua, ajuste **quantidades e classes** e rode `java FinOpsLab.java estimate <sua-bom.csv>`. Se precisar de um preço que **não está** no `prices.csv` (por exemplo, uma classe de instância maior ou o RDS Proxy), acrescente a chave no `fetch-prices.py` e refaça o arquivo: **preço que não está no arquivo não entra de cabeça**.

### 4.6 Roteiro de resposta (o que a rubrica procura)
| # | Tema | O que um bom design doc diz | Aulas |
|---|---|---|---|
| 1 | **Medir antes** | A primeira fase é **medir**: linha de base, teste de carga **aberto** até o **joelho**, espera por *lock* no primário; nenhuma decisão de escala sem esse número (o enunciado não o dá **de propósito**) | S2.01, S2.06, S1.01 |
| 2 | **Gargalo** | O teto é o **banco**, não a CPU das tarefas: `N × fator de deploy × pool + A ≤ max_connections` já limita a **4 tarefas** hoje; a 10× isso **não fecha** sem mudar algo: *pool* menor por tarefa, *pooler*/RDS Proxy, instância maior (mais `max_connections`, mais custo) ou `maximum_percent` menor | S4.07, P9.08, decisões 171, 181, 226 |
| 3 | **Escala** | Tarefas **horizontais** (sem estado); primário **vertical** primeiro (decisão 184); **réplica** só para o `reporting`, com roteamento explícito (209); **CQRS do extrato** tira a leitura do ledger (188); cache **só de exibição**, chave com `sub` (187) | S1.01, S1.04, S1.05, S3.03 |
| 4 | **Dados** | **Particionar o ledger por mês antes do volume** (a preparação *online* e a troca da S3.02 custam mais com a tabela maior); 12 meses à frente + alerta; **arquivamento** com saldos de fechamento (210); o **mapa de migrações** do item 2.10 | S3.01, S3.02, S3.04 |
| 5 | **Não-objetivos com gatilho** | ***Sharding* não** (gatilhos da decisão 186 não atingidos: medir **depois** das otimizações); **multi-região não** (225); ***event sourcing* não** (189); **multi-cloud não** (249). Cada "não" com o **número** que o reabriria | S1.03, S1.06, S4.06, S6.03 |
| 6 | **Confiabilidade** | Os **2 SLOs** com alertas por **taxa de queima** (218) **e** SLIs de **frescor** (extrato e Outbox: o usuário sente um extrato velho, e ele não vira 5xx); **N+1 de AZ** a 10× (2 AZs a 60% viram 120%: opção B ou 3 AZs, 224); **modelo de capacidade** revisto todo mês (226) | S4.01, S4.06, S4.07 |
| 7 | **Custo** | BOM **refeita** com preços **datados**; o "fixo" tem **degraus** (instância maior, *pooler*, réplica maior, mais *endpoints*?) e **não** escala linear; **custo por 1.000** antes e depois; compromissos **só** depois de 3 meses no piso medido (254); Spot só no que é interrompível (255); *staging* com agenda | S6.04, S6.02 |
| 8 | **Segurança** | **STRIDE** do que muda: o parceiro como cliente **máquina a máquina** (Client Credentials **próprio**, escopo mínimo), **limite por `azp`** (192), mTLS **só se** o parceiro exigir (gatilho da 232), inventário LGPD dos dados **novos** (240), auditoria (242), revisão de segurança antes da rampa (243) | P10.04, S1.09, S5.01–S5.07 |
| 9 | **Entrega e reversão** | Fases com **portão** e **reversão numérica**: (0) medir; (1) conexões; (2) partição com carga; (3) CQRS com leitura em paralelo e *flag* (266); (4) `reporting` e arquivamento; (5) **rampa do parceiro** 1% → 10% → 50% → 100% **governada pela política de *error budget*** (219: abaixo de 25%, congela) | P8.03, P8.04, S3.01, S7.04, S4.01 |
| 10 | **Estimativa e riscos** | Três pontos por tarefa, **Monte Carlo, compromisso no p85**, premissas e data de reestimativa (263); **registro de riscos** com exposição, dono e gatilho (264); um ***spike*** para o maior risco **antes** de estimar (261) | S7.02, S7.03 |
| 11 | **ADRs e comunicação** | 3 a 5 ADRs curtas (por exemplo: *pooler* × *pool* menor; particionar agora; **não** fazer *sharding*; SLI de frescor; região), passando no `DocLab adr`; ***one-pager*** com a **recomendação primeiro**, o efeito em dinheiro, risco e prazo, as opções (inclusive **não fazer**) e o que se precisa do negócio (272) | S7.01, S7.06 |

### 4.7 Números de apoio (contas sobre saídas reais; leia o rótulo de cada uma)
- **Custo, se você extrapolar errado.** A S6.04 separou o custo em **fixo** e **variável** (`out-s604-estimate.txt`). Multiplicar só o variável por 10 dá: `sa-east-1` **US$ 1.006,93 + 10 × 417,35 = US$ 5.180,43/mês (×3,64)**; `us-east-1` **US$ 606,25 + 10 × 281,64 = US$ 3.422,65/mês (×3,85)**. Por 1.000 transferências (todos os ambientes, `sa-east-1`): **US$ 5.180,43 ÷ 100.000 ≈ US$ 0,052**, **menos** que os US$ 0,14 de hoje, porque 71% era fixo. **Essa conta está errada de propósito:** o "fixo" de hoje (o `db.m7g.large`, a réplica, os *endpoints*) **não aguenta** 10× sem trocar de classe, e o variável do modelo foi estimado para ~78 req/s, não para o pico de 10×. O seu número é o da **sua BOM**, com as classes que o teste de carga justificar.
- **Conexões (S4.07).** Com `max_connections = 100`, reserva 10 e fator de *deploy* 2, o teto é **4 tarefas** com *pool* 10. Qualquer plano que diga "escalar para 20 tarefas" sem mexer nessa conta **falha no primeiro *deploy* no pico** (o incidente do checkpoint Pleno).
- **Lei de Little (S4.07).** `L = λ × W`: o número de conexões ocupadas é a **taxa** × o **tempo que cada transação segura a conexão**. A S4.07 mediu o efeito de `W` crescer (50 req/s × 3 s = **150**); o seu `W` real vem do teste de carga, **não** desta página.
- **Latência perto do limite (S4.07, modelo M/M/4 com serviço de 20 ms):** p99 de **100 ms a 60%**, **237 ms a 90%**, **940 ms a 98%** de utilização: operar **abaixo do joelho** é uma decisão de custo **e** de SLO.
- **AZ (S4.06):** 2 AZs a 60% → **120%** ao perder uma; para ≤ 85% sem escalar, regime **≤ 42%**; 3 AZs a 60% → **90%**.

### 4.8 Rubrica (100 pontos)
| Critério | Pontos | O que o avaliador procura |
|---|---|---|
| **Problema, perguntas e premissas** | 10 | perguntou antes de desenhar; premissas **escritas e datadas**; separou medição de premissa |
| **Objetivos e não-objetivos** | 10 | objetivos **com número**; *sharding*, multi-região e *event sourcing* ditos **com o gatilho** que os reabriria |
| **Desenho e alternativas** | 15 | dois diagramas; o **gargalo** identificado **com conta** (conexões, primário); ≥ 2 alternativas com "porque" |
| **Dados e migração** | 15 | partição **antes** do volume; CQRS com leitura em paralelo; arquivamento; **mapa de migrações** com *expand/contract* e portões |
| **Confiabilidade e capacidade** | 15 | SLOs + **frescor**; alertas por taxa de queima; N+1 de AZ; modelo de capacidade com **data e dono** |
| **Custo** | 10 | BOM refeita com preço **datado**; fixo com **degraus**; custo por 1.000 antes e depois; compromissos no tempo certo |
| **Segurança e conformidade** | 10 | STRIDE do parceiro; cliente e limite **por `azp`**; LGPD dos dados novos; auditoria |
| **Entrega, risco e estimativa** | 10 | rampa com **reversão numérica**; p85 com premissas; riscos com dono e gatilho |
| **ADRs e comunicação** | 5 | 3–5 ADRs curtas que passam no `DocLab adr`; *one-pager* com recomendação primeiro |

**Resultado:** **≥ 80** ✅ **e** `DocLab design` sem erros · **65–79** ⚠️ (refaça as seções com menos pontos e passe por uma segunda revisão, de preferência com outra pessoa) · **< 65** ❌ releia S7.02, S4.07 e S6.04 e escreva de novo com o enunciado alternativo **(B)** abaixo.

**Enunciados alternativos** (para repetir ou para outra pessoa avaliar): **(B)** "**Pagamentos a lojistas por PSP com o antifraude extraído**": decide de vez a hipótese da S1 (decisão 190 × 126), com a saga, a conciliação, o PCI e o custo do serviço novo (S1.07, S1.12, S5.05, S6.04); **(C)** "**Transferências recorrentes, versão completa**": o design doc da S7.02 **mais** o custo, o mapa de migrações, os SLOs assinados e a rampa (S7.02, S6.04, S4.01).

<details>
<summary>Armadilhas comuns (leia só depois de tentar)</summary>

- **"Escalar as tarefas"** sem a conta de conexões: o banco é o teto, e mais tarefas = **mais conexões** (e mais *fallback*, se houver).
- **Sharding porque "10×"**: os gatilhos da decisão 186 pedem medição **depois** das otimizações; particionar a **tabela** vem antes, e a transferência entre *shards* vira saga.
- **Usar o "×1,29 para ×2" da S6.04 como regra para ×10**, ou multiplicar o total por 10: o modelo de fixo × variável **não extrapola**, porque o fixo tem degraus.
- **Particionar depois de crescer:** a troca da S3.02 foi medida com 3 milhões de linhas; com 200 milhões por mês, a preparação *online* (índice `CONCURRENTLY` e `CHECK` validado) custa mais e o risco sobe.
- **SLO sem frescor:** um extrato 10 minutos atrasado não aparece em 5xx nem em latência.
- **Rampa sem gatilho numérico de reversão**, ou rampa com o orçamento de erro abaixo de 25% (a política **congela** mudanças).
- **Esquecer o mapa de migrações** (as colisões V12/V13 da Parte 1).
- **Limite por IP** para um parceiro máquina a máquina (decisão 192: por `azp`/`sub`).
- **Tornar o dinheiro assíncrono "para escalar"**: a transferência continua **uma transação** (decisão 104).
- **Custo sem data e sem região**, ou "preço do *site*" sem a chave no `prices.csv`.
</details>

---

## Parte 5: Incidente simulado e postmortem sem culpa

> ⚠️ **Leia isto primeiro.** O dossiê abaixo é **sintético**: foi **escrito para este exercício**, **não** medido. Os **números** (taxas, horários, contagens, conexões) são **inventados e rotulados**, e foram escolhidos para **fechar as contas** que as perguntas pedem. Os **mecanismos** vêm de aulas em que foram medidos ou desenhados: o extrato CQRS com `X-Min-Seq`, *fallback*, reconstrução e invariante horária (**S1.05**), a leitura em paralelo e o mutante **m1** (**S7.04**: deduplicar por `transfer_id` apaga depósitos, que têm `transfer_id` nulo pela ADR 0001), SLOs e taxa de queima (**S4.01**), incidente e postmortem (**S4.02**, **S4.03**), Lei de Little (**S4.07**) e o *pool* (**P2.06**). **Não use as saídas abaixo como referência de comportamento real.** O ensaio **real** deste cenário é uma das ações do postmortem (um *game day*, S4.04).

### 5.1 O que fazer (sessão CK-S.5, ~1h30)
1. Leia **só** o enunciado e os sinais (5.2 e 5.3). **Não** abra o gabarito.
2. Responda às **8 perguntas** (5.4) **por escrito**, na ordem. Cronometre **45 min**.
3. Compare com o gabarito (5.5) e reescreva o que errou.
4. Escreva o **postmortem** no modelo da 5.6 e passe-o no `DocLint`; escreva o ***runbook*** antes de abrir o de referência (5.7).
5. Registre no PayFlow: `docs/postmortems/2026-12-03-extrato-reconstrucao.md`.

### 5.2 Enunciado
**Quinta-feira, 2026-12-03.** O PayFlow roda com o que a tag `payflow-S8` descreve, **implementado**: o extrato é **CQRS** (S1.05): o `POST` devolve o `ledgerSeq`; o `GET /statement` com `X-Min-Seq` **espera até 500 ms** o projetor alcançar aquele número e, se não alcançar, **lê do ledger no primário**; um *job* **de hora em hora** (ShedLock) confere `Σ read model = accounts.balance` e **reconstrói automaticamente** cada conta divergente (`rebuild(accountId)`: apaga as linhas da conta e refaz a partir do ledger). A troca `/entries` → `/statement` está na fase de **leitura em paralelo** (10% das leituras, S7.04), com o alerta `StatementShadowMismatch` (`severity: ticket`). Produção tem **6 tarefas** com *pool* de **10** conexões cada.

Todos os horários estão em **UTC** (decisão 221); em São Paulo, **09:00 UTC = 06:00**, o começo do pico da manhã.

**Na véspera, às 22:10**, saiu a versão `2026.12.02-3`, cuja mudança principal foi uma refatoração do projetor do extrato: ele passou a **"deduplicar por (`account_id`, `transfer_id`)"** dentro de cada lote; um lançamento considerado repetido é **marcado como aplicado** (o `applied_seq` da conta avança) **sem inserir a linha**.

**09:07.** O plantão recebe a página `PayflowTransfersLatencyBudgetBurn` (queima de 1 h e de 5 min acima de 14,4× no SLO de latência) e, dois minutos depois, `PayflowDbPoolSaturated`. O suporte avisa que há **12 chamados** desde as 07:30 com a mesma frase: **"meu depósito sumiu do extrato"**. Você é a pessoa de plantão.

**Restrições de dinheiro (regras do projeto):** **nunca** altere saldo nem lançamentos à mão; **nunca** faça `UPDATE` manual no *read model* (a reconstrução é a cura, decisão 188); reparação só por **lançamento de compensação** (P3.13); **nunca** reinicie o banco "para ver se melhora".

### 5.3 Os sinais (sintéticos)
**Linha do tempo dos alertas e eventos** (UTC):

| Hora | Evento | Severidade |
|---|---|---|
| 22:10 (véspera) | *deploy* `2026.12.02-3` (projetor do extrato) | — |
| 22:41 (véspera) | `StatementShadowMismatch` (`result="bug"` > 0) | *ticket* |
| 23:00 … 08:00 | o *job* horário reconstrói contas divergentes: **37** às 23:00, **41** à 00:00 … **2.910** às 08:00 | — |
| 09:00 | o *job* encontra **41.212** contas divergentes e começa a reconstruir **todas, em sequência** | — |
| 09:07 | `PayflowTransfersLatencyBudgetBurn` | página |
| 09:09 | `PayflowDbPoolSaturated` | página |

**Métricas** (janelas de 1 min; **valores inventados**):

```text
                                        08:30    09:00    09:05    09:10    09:12
POST /transfers p50 (s)                 0.018    0.019    0.150    0.240    0.260
POST /transfers p99 (s)                 0.090    0.110    2.900    4.100    4.300
POST /transfers acima de 500 ms (%)     0.4      0.6      38       52       55
POST /transfers 5xx (%)                 0.0      0.0      0.6      2.1      2.3
GET /statement com X-Min-Seq (req/s)    35       38       44       44       44
statement_fallback_total (por s)        0.1      0.2      31       44       44
statement_projection_lag_seconds        0.4      0.5      95       480      610
statement_gap_total (aumento)           0        0        0        0        0
contas na fila de reconstrução          -        41212    40870    40120    39890
hikaricp_connections_pending (máximo)   0        0        6        13       14
CPU do primário (%)                     41       44       88       95       96
Keycloak: latência do JWKS p99 (ms)     38       40       39       41       40
payflow_outbox_pending                  4        5        7        9        9
```

**Banco** (`pg_stat_activity` às 09:12, agrupado pelo que cada conexão faz; **inventado**):

```text
 o que a conexão está fazendo                                   | conexões | estado
----------------------------------------------------------------+----------+----------------------
 GET /statement: esperando X-Min-Seq (SELECT applied_seq ...)   |    22    | idle in transaction
 GET /statement: fallback (SELECT ... FROM ledger_entries ...)  |    23    | active
 reconciliação: rebuild(accountId)                              |     2    | active
 projetor do extrato: aplicando eventos                         |     3    | active (esperando Lock)
 POST /transfers (lockAllOrdered + INSERT no ledger)            |    10    | active
 administração e monitoração                                    |     3    | active
 total                                                          |    63    | (max_connections = 100)
```

**Logs** de uma tarefa (JSON formatado para leitura; **inventados**):

```text
09:00:00.412 INFO  ReconciliationJob - divergent accounts: 41212 (previous run: 2910); rebuilding all (limit: none)
09:05:41.118 WARN  StatementQueryService - X-Min-Seq not reached in 500 ms; falling back to ledger
09:10:12.874 WARN  HikariPool-1 - Connection is not available, request timed out after 30000ms (total=10, active=10, idle=0, waiting=13)
09:10:12.877 ERROR unexpected failure | error.type=org.springframework.jdbc.CannotGetJdbcConnectionException
```

**Traço** de uma transferência lenta, mas concluída (**inventado**):

```text
http post /api/v1/transfers        4120 ms  status=201  outcome=SUCCESS
  jdbc.connection.acquire           3985 ms
  payflow.transfer                   121 ms
    db.query (for no key update)      88 ms
```

### 5.4 Perguntas (8)
1. **Triagem.** O que o cliente está sentindo **agora**? Isto é "dinheiro errado" ou "dinheiro lento"? Que **severidade** você declara às 09:10 e **o que** faria você mudá-la (S4.02)?
2. **Escopo.** Há **duas** populações afetadas. Quais são, desde quando, e que dado das 5.3 **prova** cada uma?
3. **Mecanismo.** Use a **Lei de Little** para explicar as **22** conexões `idle in transaction` e as **23** de *fallback*. Por que um mecanismo feito para **proteger a consistência de sessão** virou a **maior carga** do *pool*? Descreva o **laço** que mantém o problema mesmo que a reconstrução pare.
4. **Hipóteses.** Liste **cinco** (inclua o Keycloak, o *deploy* da véspera, o *job* das 09:00, uma "consulta lenta nova" e o tráfego normal do pico) e, para cada uma, o **dado** que a **sustenta ou descarta**.
5. **Causa e fatores.** Escreva a **cadeia** do *deploy* das 22:10 até a página das 09:07. Por que **três defesas** existentes não pararam o problema: a regra "**lacuna = erro**", a **leitura em paralelo** e a **reconstrução** como cura?
6. **Mitigação.** O que você faz **agora**, em ordem? Diga o que **não** fazer (três ações tentadoras e **por que** pioram).
7. **Verificação do dano.** Como você **prova** que (a) o **dinheiro** está certo, (b) o **extrato** foi curado e (c) ninguém foi cobrado **duas vezes** por repetir uma transferência que falhou?
8. **Orçamento e ações.** Calcule o **impacto no orçamento** dos dois SLOs (premissa: **30 transferências/s** no pico; orçamento mensal calculado a **5 req/s**, como na S4.01) e escreva as **ações** (prevenir, detectar, mitigar), cada uma com dono e data; **uma** deve repetir o experimento.

### 5.5 Gabarito (leia **depois**)

<details>
<summary>Gabarito do incidente</summary>

1. **Agora:** transferências **lentas** (p99 de 4,3 s; 55% acima de 500 ms) e **2,3% de 5xx** (o *pool* não entrega conexão em 30 s); e, desde a véspera, **extratos sem depósitos**. É "dinheiro **lento**" no caminho da transferência e "dinheiro **exibido errado**" no extrato. **Severidade:** declare **SEV1** às 09:10: a regra "**dinheiro vence qualquer número**" e "na dúvida, suba um nível" (decisão 220), porque, até provar o contrário, você **não sabe** se o ledger está certo. **Rebaixe para SEV2** (página de SLO) **quando** a conciliação **da fonte da verdade** (`saldo = abertura + Σ ledger vivo`) der **0 divergências** (pergunta 7). Papéis: comando (não depura), operação, comunicação (atualização a cada **30 min** em SEV1), registro.
2. **(a) Quem transfere desde ~09:03** (todas as contas): lento e com 5xx. **Prova:** a latência e os 5xx de `POST /transfers`, e o traço com **3.985 ms em `jdbc.connection.acquire`** e só 121 ms de transferência (o tempo é **espera por conexão**, não trabalho). **(b) Quem recebeu depósitos desde 22:10:** extrato sem eles. **Prova:** `StatementShadowMismatch` desde 22:41, as contas divergentes crescendo a cada hora (37 → 2.910 → 41.212) e os chamados "depósito sumiu". **Não afetados:** autenticação (JWKS estável), eventos (Outbox normal).
3. **Little: `L = λ × W`.** Esperando: **44 leituras/s × 0,5 s de espera = 22** conexões. A espera acontece **dentro de uma transação aberta** (`idle in transaction`): a conexão fica **presa** enquanto a requisição só aguarda o projetor. *Fallback*: **44/s × ~0,52 s** (a consulta ao ledger fica lenta com o primário a 96%) **≈ 23**. Juntas, **45 das 60** conexões do *pool* servem o extrato; sobram **10** para transferências e **3** para o projetor, e há **14** esperando. **O laço:** o projetor também precisa do *pool* e do primário → com menos conexões e CPU, ele **atrasa** → o atraso passa de 500 ms → **toda** leitura com `X-Min-Seq` cai no *fallback* (44 de 44) → mais conexões presas e mais CPU → o projetor **atrasa mais** (lag de 0,5 s para 610 s em 12 min). A reconstrução das 09:00 **deu o empurrão**; o laço **se sustenta sozinho**: parar só a reconstrução **não** basta, é preciso **cortar o *fallback***.
4. **(a) Keycloak:** **descartada** (JWKS estável em ~40 ms; nenhum 401/403). **(b) *Deploy* das 22:10:** **sustentada como origem** do extrato errado (o `bug` da leitura em paralelo começa 31 min depois; as divergências crescem desde então), **não** como gatilho da lentidão (que só começa às 09:03). **(c) *Job* das 09:00:** **sustentada como gatilho** (41.212 reconstruções sem limite às 09:00; a CPU do primário vai de 44% para 88% em 5 min). **(d) "Consulta lenta nova":** **descartada como causa** (não há plano novo; as consultas do *fallback* e da reconstrução são **as previstas**, lentas **porque** a CPU está a 96%). **(e) Pico normal:** **fator**, não causa (às 08:30 o mesmo tráfego andava com p99 de 90 ms); o problema é o *job* das 09:00 **coincidir** com o pico (09:00 UTC = 06:00 em São Paulo).
5. **Cadeia:** *deploy* com a deduplicação errada → **depósitos** (`transfer_id` nulo, ADR 0001) da mesma conta no mesmo lote viram "repetidos" → o projetor **avança o `applied_seq` sem inserir** → o extrato perde depósitos e a soma diverge → o *job* horário reconstrói a conta **com o mesmo código** → ela **continua** divergente e volta na hora seguinte → as divergências **se acumulam** (depósitos da manhã) → às 09:00, **41.212** reconstruções **sem limite**, no primário, **no pico** → o projetor atrasa → o *fallback* com espera **dentro da transação** prende o *pool* (pergunta 3) → as transferências esperam conexão → página. **Por que as defesas falharam:** (i) "**lacuna = erro**" pega evento que **falta**; aqui o evento **chegou** e foi **descartado** com o `applied_seq` avançando: **não há lacuna** (`statement_gap_total` = 0); (ii) a **leitura em paralelo** **pegou** o *bug* em 31 min (como o m1 da S7.04 no modelo), mas o alerta era ***ticket*** e ninguém o viu à noite, e o *deploy* **não** dependia dele; (iii) a **reconstrução** usa **o mesmo código** de projeção: a "cura" **reaplica o defeito** e, sem limite, vira **carga**. *O sistema permitia* uma cura automática em massa, no pico, sem limite, com o código suspeito.
6. **Em ordem:** (1) declarar o incidente, papéis e o primeiro aviso (o que está afetado, desde quando, próxima atualização). (2) **Cortar o *fallback*** do `X-Min-Seq` pela *flag* (o extrato responde do *read model* com "atualizado em …", ou `503` com `Retry-After` só nas leituras com token): **protege o caminho do dinheiro** (decisão 227: o dinheiro vence o resto, e **quem perde primeiro** é decisão de produto; o extrato com "atualizado em" é a perda menor) e **quebra o laço**. (3) **Pausar o *job*** de reconciliação (ShedLock/*flag*). (4) **Reverter** o projetor para a versão anterior (para de criar divergência nova). (5) **Observar:** `pending` → 0, p99 e 5xx voltando, lag caindo. (6) Só depois, **reconstruir** as contas com o código corrigido, **com limite** (por exemplo, 200 contas/min: ~3,4 h para 41.212) e pausa automática se houver espera no *pool*, **fora do pico**. **Não faça:** **(a)** aumentar o *pool* ou o número de tarefas (mais conexões presas e mais *fallback*: o teto é o banco); **(b)** `TRUNCATE` no *read model* e "reconstruir tudo agora" (todo extrato cai no *fallback*: piora); **(c)** `UPDATE` manual no *read model* ou "ajuste" de saldo (proibido; a fonte da verdade **não** está errada); **(d)** reiniciar o banco ou matar sessões em massa (derruba transferências **em andamento** de usuários legítimos).
7. **(a) Dinheiro:** a conciliação da **fonte da verdade** (`saldo = abertura + Σ ledger vivo`, S3.04) com **0** divergências. Deve dar zero porque o projetor **só escreve no *read model*** e **nenhuma decisão financeira lê o *read model*** (decisão 188): saldo disponível, limites e antifraude leem o primário. **(b) Extrato:** o invariante `Σ read model = accounts.balance` com **0** contas depois da reconstrução corrigida, e `statement_shadow_total{result="bug"}` em **0** por **≥ 30 mil** leituras (o critério da decisão 266) **antes** de religar a reconstrução automática e o *fallback*. **(c) Cobrança dupla:** as transferências com 5xx podem ser **repetidas com a mesma `Idempotency-Key`** (S1.08: mesmo efeito, nunca dois); procure **duplicidade por intenção** (mesmo dono, mesmo destino, mesmo valor, poucos minutos, **chaves diferentes**) na janela do incidente, por exemplo com o `DuplicateTransferDetector` opcional da S8.03; se houver, **devolução só a pedido** do cliente, por **lançamento de compensação** (P3.13). **Atraso × perda:** nenhum evento perdido (Outbox normal); o extrato estava **atrasado e errado**, não **perdido** (o ledger é a fonte).
8. **Impacto (premissas do enunciado; modelo de mitigação da resposta 6, com o *fallback* cortado às 09:14 e o SLO normal às 09:44):** **latência**: o orçamento de 99% em 30 dias a 5 req/s é 30 × 86.400 × 5 × 1% = **129.600** lentas; o incidente teve ~**41 min** (09:03–09:44) a 30/s com ~**50%** lentas = 41 × 60 × 30 × 0,5 = **36.900** → **28,5%** do orçamento do mês. **Disponibilidade**: ~**25 min** de 2% de 5xx a 30/s = **900** de **12.960** → **6,9%**. Os dois passam do gatilho de **5%** (decisão 221): postmortem obrigatório. Com mais de 25% gastos, a política de orçamento **(219)** pede *ticket* de confiabilidade e plano de reversão revisado para mudanças de risco. **Ações (exemplos):** *prevenir*: (1) reconstrução automática **com limite** de taxa, **disjuntor** ("mais de N contas divergentes em 1 h → parar e paginar, nunca curar em massa") e janela fora do pico; (2) a espera do `X-Min-Seq` **fora da transação** e o *fallback* **com orçamento** (taxa máxima; acima dela, servir com "atualizado em"); (3) teste do projetor **com depósitos** (`transfer_id` nulo) e o m1 como **mutante obrigatório** no PIT (S7.05); (4) *deploy* do projetor **condicionado** a `bug = 0` na leitura em paralelo do *canary*; *detectar*: (5) `StatementShadowMismatch` com `result="bug"` vira **página** (dinheiro exibido errado), **SLI de frescor** do extrato com alerta, alerta de *fallback* e de "divergências por hora"; *mitigar*: (6) *runbook* "extrato atrasado ou *fallback* alto" (5.7) com o **corte do *fallback*** no passo 1; (7) ***game day*** que **repete o experimento** em *staging* (projetor parado + leituras com token sob carga) e prova que o corte devolve o *pool*.

</details>

### 5.6 Modelo do postmortem (preencha **você**)
O modelo abaixo passa no `DocLint postmortem` **como está** (as 7 seções, linha do tempo com `- HH:MM`, ações com `@dono` e data). Substitua o conteúdo e rode de novo, a partir de `3-senior/S4-confiabilidade-sre/lab/` (JDK 25): `java DocLint.java postmortem <seu-arquivo>`. Lembre: o verificador recusa **palavras de culpa** e **ações vagas**; ele **não** julga se a causa está certa.

```markdown
# Postmortem: a reconstrução automática do extrato saturou o pool no pico (CK-S, sintético)

## Resumo
(2 linhas: o que quebrou, por quanto tempo, impacto em usuários e no dinheiro.)

## Impacto
(quem, quantos, de quando a quando; orçamento gasto nos dois SLOs, com a conta; o que NÃO foi afetado e a prova.)

## Linha do tempo (UTC)
- 22:10 deploy 2026.12.02-3 (projetor do extrato)
- 09:07 página de queima do SLO de latência
- HH:MM (cada alerta, cada ação, cada decisão)

## Causa raiz e fatores contribuintes
(a cadeia da resposta 5; o que o sistema permitia; nenhum nome de pessoa.)

## O que funcionou
(leitura em paralelo pegou o defeito; traço mostrou a espera por conexão; idempotência permitiu repetir.)

## O que não funcionou
(alerta de divergência como ticket à noite; cura sem limite com o mesmo código; espera dentro da transação.)

## Ações
- [ ] Reconstrução automática com limite de taxa e disjuntor — @dados — até 2026-12-10
- [ ] Game day: projetor parado com leituras com token sob carga, em staging — @plataforma — até 2026-12-17
```

### 5.7 *Runbook* ("extrato atrasado ou *fallback* alto")
> Escreva o seu primeiro, **depois** compare. O de referência passa no `DocLint runbook` (5 seções, `Dono:`, `Revisado em:`, diagnóstico numerado, mitigação com comando).

<details>
<summary>Runbook de referência</summary>

```markdown
# Runbook: extrato atrasado ou fallback alto (PayflowStatementLag / PayflowStatementFallbackHigh)
Dono: @backend · Revisado em: 2026-12-03

## Impacto
Extrato atrasado ou incompleto; se o fallback cresce, o pool fica preso e as TRANSFERÊNCIAS ficam lentas (o caminho do dinheiro).

## Diagnóstico
1. Lag e fallback: statement_projection_lag_seconds e rate(statement_fallback_total[5m]). Fallback perto da taxa de leituras com X-Min-Seq = laço.
2. Pool: hikaricp_connections_pending > 0? Quem segura: SELECT state, count(*) FROM pg_stat_activity WHERE datname = 'payflow' GROUP BY 1;  ('idle in transaction' alto = espera presa)
3. Há reconstrução em massa? Log do ReconciliationJob ("divergent accounts: N") e a fila de reconstrução.
4. Houve deploy do projetor nas últimas 24 h? statement_shadow_total{result="bug"} > 0?

## Mitigação
1. Cortar o fallback: `curl -X POST .../actuator/flags/statement.min-seq-fallback -d '{"enabled":false}'` (o extrato responde do read model com "atualizado em").
2. Pausar a reconstrução: flag `reconciliation.rebuild.enabled=false`.
3. Se o defeito veio de deploy: rollback do projetor para a versão anterior.
4. NUNCA: aumentar pool ou tarefas, TRUNCATE no read model, UPDATE manual, reiniciar o banco.

## Escalar
Se o p99 de POST /transfers não voltar abaixo de 500 ms em 10 min depois do passo 1, ou se a conciliação da fonte da verdade divergir em qualquer valor: SEV1, chamar @dados e o comando do incidente.

## Verificação
pending = 0; p99 e 5xx de transferências normais; lag caindo; conciliação da fonte da verdade com 0 divergências; só então religar o fallback e reconstruir com limite.
```
</details>

> O comando da mitigação usa uma rota **hipotética** de *flags* (`/actuator/flags/...`): troque pelo mecanismo de *flags* do **seu** PayFlow (P8.04). O `DocLint` só exige que haja **um comando** e o passo de reversão.

**Rubrica (100 pontos):**

| Critério | Pontos | O que o avaliador procura |
|---|---|---|
| **Triagem e severidade** | 10 | separou "lento" de "errado"; SEV1 até provar a fonte da verdade; papéis |
| **Escopo** (as duas populações, com prova) | 10 | transferências pelo traço; extrato pela leitura em paralelo e pelas contas divergentes |
| **Mecanismo** (Little e o laço) | 20 | 44 × 0,5 = 22; a espera **dentro** da transação; por que parar a reconstrução não basta |
| **Hipóteses** (inclusive as descartadas, com o dado) | 15 | Keycloak e "consulta nova" descartados com evidência; origem × gatilho × fator |
| **Mitigação segura** (e o que **não** fazer) | 20 | corta o *fallback* **primeiro**; pausa, reverte, reconstrói com limite; nada de *pool* maior nem `UPDATE` |
| **Verificação** (dinheiro, extrato, duplicidade) | 10 | conciliação da fonte; ≥ 30 mil leituras sem *bug*; mesma chave × duplicidade por intenção |
| **Postmortem e ações** (`DocLint` sem problemas, orçamento calculado) | 15 | 28,5% e 6,9%; prevenir > detectar > mitigar; o *game day* que repete o experimento |

**Resultado:** **≥ 80** ✅ · **65–79** ⚠️ (refaça o postmortem e rode o `ChaosLab` da S4.04 para sentir a Lei de Little de verdade) · **< 65** ❌ releia S1.05, S4.02, S4.03 e S4.07.

---

## Parte 6: Simulado de entrevista de *system design* (60 minutos)

> Em voz alta, sem consulta, **com outra pessoa** no papel de entrevistador (sozinho, grave-se). Use o **método da S1.11** (7 passos) com o **roteiro de tempo da S8.04**: requisitos ~5 min, entidades ~2, API ~5, desenho de alto nível ~10–15, aprofundamentos ~10, e o resto em gargalos, *trade-offs* e evolução. No Sênior, o nível aparece em **números, gargalo, *trade-offs* e "o que eu não faria"** (decisão 281).

### 6.1 Enunciado: "Entrega de *webhooks* a parceiros"
O PayFlow vai avisar **lojistas parceiros** (os **sistemas** deles) a cada evento que os envolve (`payment.captured`, `transfer.completed`, `refund.created`), com uma chamada HTTP ao *endpoint* que cada parceiro cadastrou. Desenhe o serviço que **entrega** esses eventos.

**Premissas que o entrevistador dá quando você pergunta** (inventadas; **não** as use se não perguntou):
- **5.000** parceiros, um *endpoint* cada; **50 milhões** de eventos por dia; pico de **10×** a média; *payload* de até **4 KB**.
- A qualquer momento, **~1%** dos *endpoints* está lento ou fora do ar; um *endpoint* saudável responde em ~**200 ms**.
- O parceiro quer o evento em **até 1 min** (p99) quando o *endpoint* dele está saudável; **retentativas por até 72 h**; **ordem por recurso** é desejável, não obrigatória; **assinatura obrigatória**.

### 6.2 Contas que um bom candidato faz em voz alta (cálculo sobre as premissas)
- **Taxa:** 50.000.000 ÷ 86.400 s ≈ **579 eventos/s** em média; pico ≈ **5.790/s**.
- **Banda e armazenamento:** 579 × 4 KB ≈ **2,3 MB/s** em média (~23 MB/s no pico); **200 GB por dia** de *payload*; 72 h de retenção ≈ **600 GB**, mais o registro de tentativas.
- **Concorrência (Lei de Little, S4.07):** os saudáveis no pico: 5.790 × 99% × 0,2 s ≈ **1.146** chamadas em voo; o **1% ruim**, se recebe 1% do tráfego e segura a conexão até um *timeout* de 10 s: 57,9/s × 10 s ≈ **579** em voo. **Metade do que os 99% saudáveis usam vai para os 50 *endpoints* ruins**: sem isolamento, eles **roubam** a capacidade de todos.

### 6.3 Roteiro de resposta (o que a rubrica procura)
| # | Tema | O que um bom desenho diz | Aulas |
|---|---|---|---|
| 1 | **Garantia** | **Pelo menos uma vez**; o parceiro deduplica pelo **`event_id`** (estável entre tentativas); *exactly-once* de entrega **não** existe | P3.06, P3.07, S1.08 |
| 2 | **Origem** | O evento nasce na **mesma transação** do dado (Outbox) e vai para um tópico com **chave por parceiro** (ou por recurso, se a ordem importar); o dinheiro **nunca** espera o *webhook* | P3.09, P3.03, decisão 105 |
| 3 | **Isolamento** | **Fila ou partição por parceiro**, **limite de concorrência por *endpoint*** (*bulkhead*), *circuit breaker* **por *endpoint*** e "*endpoint* desativado" depois de N falhas, **com aviso** ao parceiro | P5.08, P5.10 |
| 4 | **Retentativas** | *Backoff* exponencial com ***jitter*** até 72 h; depois, **DLQ** e uma **API de reenvio** para o parceiro; respeitar **429 + `Retry-After`** do parceiro | P3.08, S1.09 |
| 5 | **Segurança** | **HMAC sobre os bytes** + *timestamp* + `event_id` (o mesmo esquema que o PayFlow **verifica** na P3.10, agora do lado de quem envia); **dois segredos ativos** na rotação; proteção contra **SSRF** no cadastro do *endpoint* (nada de IP privado nem `169.254.169.254`); *payload* **mínimo** (LGPD) | P3.10, P10.05, J8.07, S5.01, S5.05 |
| 6 | **Ordem** | Número de sequência **por recurso** no *payload*; o parceiro descarta o velho; **nenhuma** promessa de ordem global | S1.10 |
| 7 | ***Payload*** | Evento "**magro**" (id, tipo, *link* para buscar com autorização) × "**gordo**" (dados): privacidade, tamanho e versionamento **aditivo** | P5.04, S5.05 |
| 8 | **Operação** | SLI = **% de eventos entregues em ≤ 1 min** a *endpoints* saudáveis; alerta pela **idade do evento mais antigo** não entregue; **parceiro não é etiqueta de métrica** (5.000 séries): vai em *span*/log, com os **N piores** num painel | S4.01, P9.07, P6.02 |
| 9 | **Capacidade e custo** | Trabalho de **E/S**: cliente HTTP assíncrono ou *virtual threads*; concorrência pela **conta de Little**; tentativas particionadas **por dia** com descarte por partição; tráfego de **saída** entra na estimativa | P1.10, P3.12, S3.02, S6.04 |
| 10 | ***Trade-offs* e "o que não faria"** | **Não** prometer *exactly-once* nem ordem global; **não** transformar o *payload* por parceiro agora; **não** chamar o parceiro de dentro da transação do dinheiro; evolução: filtros por tipo de evento, lotes, painel do parceiro | S1.11, S8.04 |

**Rubrica (100 pontos):** requisitos e perguntas (**10**); contas em voz alta (**10**); API e modelo (**10**); desenho de alto nível com a **origem** certa (**15**); **isolamento e retentativas** (**20**); **segurança** (assinatura, SSRF, rotação, LGPD) (**15**); **operação e SLO** (**10**); *trade-offs* e "o que não faria" (**10**). **≥ 80** ✅ · **65–79** ⚠️ (repita com um enunciado alternativo) · **< 65** ❌ releia S1.11–S1.13 e S8.04.

**Enunciados alternativos** (para repetir): **"Agendador distribuído de tarefas"** (disparo único com vários nós, *leases*, relógios, S1.10 e P3.02) e **"Limites e antifraude em tempo real"** (janelas por conta, Redis, orçamento de latência dentro da saga, S1.07, S1.09 e S2.06).

<details>
<summary>Armadilhas comuns (leia só depois de tentar)</summary>

- **Uma fila só para todos os parceiros:** um *endpoint* lento segura os *workers* (a conta da 6.2) e atrasa os 4.950 saudáveis.
- **Repetir sem *jitter*** ou sem teto: quando o parceiro volta, recebe uma **avalanche** e cai de novo.
- **Assinar o JSON reserializado** em vez dos **bytes** enviados (a P3.10 mostrou por que a verificação precisa dos bytes).
- **Aceitar qualquer URL** no cadastro: SSRF contra a rede interna.
- **Etiqueta `partner_id`** em todas as métricas: 5.000 séries por métrica.
- **"Exactly-once"** prometido em voz alta, sem dizer como (o certo é dizer **o que** o parceiro faz com o `event_id`).
</details>

---

## Parte 7: Autoavaliação pela matriz de competências e o que vem depois

A matriz (`00-guia/matriz-de-competencias.md`) descreve o comportamento observável de um **sênior**. A coluna "Aulas" é a **tabela "competência → aulas que cobrem"** gerada nesta revisão (a partir dos títulos, dos registros por módulo e de buscas no texto das aulas). Marque ✅ (faço sozinho) / ⚠️ (faço com ajuda) / ❌ (não faço).

| Competência (comportamento do sênior) | Aulas que cobrem | Cobertura | ✅ ⚠️ ❌ |
|---|---|---|---|
| **Linguagem Java:** diagnostica vazamento com *heap dump*, escolhe GC e justifica *virtual threads* × reativo | S2.04 (vazamento), S2.05 (GC), S2.02 (JFR), S2.07 (partida); a comparação *virtual threads* × reativo é da **P3.12** e da **P1.10** | ✅ completa | ☐ |
| **Lógica e estruturas de dados:** modela estruturas para escala (filas, particionamento) e justifica | S1.01 (filas e balanceamento), S1.03 (anel de *hash*, chaves), S1.09 (balde de fichas), S1.13 (*fan-out*), S4.07 (Little, M/M/c), S8.03 | ✅ completa | ☐ |
| **Git e ferramentas:** estabelece convenções e automação para vários times | S7.05 (*trunk-based*, `CODEOWNERS`, Conventional Comments), S7.01 (`DocLab` no CI), S7.04 (*hotspots* do Git, OpenRewrite), S8.01 (higiene e histórico) | ✅ completa (GitHub real não executado) | ☐ |
| **Web e HTTP:** desenha contratos de API duradouros e políticas de versionamento | S1.08 (contrato de idempotência), S1.09 (429 + `Retry-After`), S1.04 (`Cache-Control`), S7.04 (`Deprecation`/`Sunset` com métrica de uso); base: J6.06, P5.04 | ✅ completa | ☐ |
| **Banco de dados:** particiona tabelas, migra sem *downtime*, escolhe banco por caso de uso | S3.01–S3.09 | ✅ completa (OpenSearch, Debezium e RDS não executados) | ☐ |
| **Spring e persistência:** define padrões de acesso a dados do time e decide quando abandonar a ORM | S3.03 (segundo `DataSource` + ArchUnit, `LazyConnectionDataSourceProxy`), S3.02 (o que o Hibernate não vê na partição), S1.05 (modelo de leitura em SQL próprio); critério JPQL × SQL × `JdbcClient` × jOOQ: **P2.08** | ✅ completa | ☐ |
| **APIs REST:** governa versionamento, compatibilidade e evolução entre times | S7.04 (depreciação com prazo e métrica), S7.02 ("contratos cedo" na revisão entre times), S7.06 (X-como-serviço), S1.08; base: P5.04, P5.09 | ✅ completa | ☐ |
| **Testes e qualidade:** define estratégia de testes e métricas de qualidade para o time | S7.05 (mutação, instáveis, DORA), S2.06 (carga), S4.04 (caos), S5.04 (SAST/DAST/SCA), mutantes em todos os laboratórios | ✅ completa | ☐ |
| **Segurança:** conduz revisão de segurança, mTLS, KMS, conformidade (LGPD/PCI) | S5.01–S5.07 | ✅ completa (KMS, CloudTrail, ZAP e *pentest* reais não executados; texto da LGPD e do PCI não conferido) | ☐ |
| **Front-end:** avalia *trade-offs* de renderização e integração front-back | J4.06 (renderização no servidor), J4.07 (SPA, visão geral), ADR 0005; no Sênior só tangencia: S1.04 (CDN para estáticos), S1.07 (`202` + SSE), P3.11 (SSE/WebSockets) | ⚠️ **parcial** (nenhuma aula do Sênior **avalia** renderização: SSR × SPA × BFF, custo de CDN, desempenho no navegador; a trilha da seção 4 não previu front-end no Sênior) | ☐ |
| **Mensageria e assincronia:** escolhe consistência, ordenação e garantias por caso de uso; sagas | S1.02, S1.07, S1.08, S1.10, S1.13, S3.06 | ✅ completa | ☐ |
| **Arquitetura:** decide limites de serviços, registra ADRs, evita complexidade desnecessária | S1.06 (*event sourcing* não adotado), S1.12, S6.02, S6.03, S7.01, S7.02 (módulo × serviço nas recorrentes) | ✅ completa | ☐ |
| **Sistemas distribuídos:** CAP/PACELC, consistência eventual, CQRS, *event sourcing* | S1.01–S1.10 | ✅ completa (por modelos de Java puro) | ☐ |
| **Observabilidade e operação:** define SLOs, conduz postmortems, planeja capacidade | S4.01–S4.07; Parte 5 deste checkpoint | ✅ completa (Alertmanager, plantão e incidente real não executados) | ☐ |
| **Containers e orquestração:** define padrões de plataforma e estratégias de *deploy* | S6.02 (VM × contêiner × *serverless*), S6.05 (*landing zone*), S7.06 (time de plataforma), S4.06 (N+1 e "sem *deploy* enquanto degradado"), S2.07 | ✅ completa por **leitura** (Kubernetes não executado desde a P7) | ☐ |
| **CI/CD e IaC:** padroniza entrega para vários times e reduz risco de *release* | **reduz risco:** S3.01 (portões de migração), S4.01 (política de orçamento), S5.04 (*pipeline* de segurança e isenções com validade), S7.05 (*trunk-based*, *flags*), S6.05 (SCPs como guarda-corpos) | ⚠️ **parcial** ("**para vários times**": *pipeline* reutilizável, modelos de repositório, caminho pavimentado **não têm aula**; GitHub real não executado) | ☐ |
| **Nuvem e custo:** aplica Well-Architected, FinOps, multi-AZ e decide *lock-in* | S6.01–S6.06, S4.06 | ✅ completa (nada criado na AWS) | ☐ |
| **Performance:** mede com método (JFR, k6, percentis) e otimiza com evidência | S2.01–S2.08 | ✅ completa (o PayFlow não foi medido) | ☐ |
| **Liderança e comunicação:** escreve RFCs e design docs, mentora, negocia dívida técnica com o negócio | S7.01–S7.07, S8.01–S8.05; Parte 4 deste checkpoint | ✅ completa (processos com pessoas não executados) | ☐ |

**19 competências** exigidas do Sênior: **17 com cobertura completa** (várias com a ressalva de **execução**: AWS, Kubernetes, GitHub, processos com pessoas) e **2 parciais** (**Front-end** e **CI/CD para vários times**); **nenhuma sem aula** (decisão 289). As duas lacunas **não** foram corrigidas: estão em "Correções pendentes" para você **decidir** (acrescentar uma aula, por exemplo "S6.07 — Plataforma interna e *pipelines* reutilizáveis" e uma aula de *trade-offs* de renderização, ou aceitar e registrar).

### Decisão de concluir o nível
| Resultado | Decisão |
|---|---|
| Projeto ≥ 80% dos itens executáveis **com o item 2.10** **e** quiz integrador ≥ 14/20 **e** design doc ≥ 80 com `DocLab design` sem erros **e** incidente ≥ 80 com o postmortem limpo no `DocLint` **e** *system design* ≥ 80, sem ❌ na matriz | ✅ **Nível Sênior concluído** |
| Alguma parte em ⚠️, sem ❌ | ⚠️ **Concluído com plano de reforço** de 2 semanas: 3 itens, a aula, o laboratório e a data |
| Qualquer parte em ❌ | ❌ **Repita os módulos indicados** e refaça o design, o incidente ou o simulado com o **enunciado alternativo** |

> **Enquanto o PayFlow não existir** (testes práticos no fim, por decisão sua), a Parte 2 fica **pendente**, não reprovada: as Partes 3 a 7 decidem o nível **por leitura**, e o nível só fecha **de vez** depois dos testes práticos (ADR 0035). Consolide no `progresso.md`: nota de cada parte, itens em ⚠️/❌ e o plano de reforço.

### Depois deste checkpoint: os testes práticos finais
O treinamento acaba onde começa o que ele adiou: **executar**. A ordem abaixo **continua** a da decisão 182 (Pleno) e acrescenta o Sênior (decisão 288). Cada divergência entre a aula e o resultado **corrige a aula** e vira registro no `progresso.md`.

1. **(182)** Pendências do Júnior (`Transfer.complete()`, `@WebMvcTest`, `ActuatorTest`, J1.05–J1.07) e `./mvnw verify` da suíte **inteira**.
2. **(182)** P7: Kubernetes no kind (`helm lint` → `--dry-run=server` → os 7 experimentos; antes, corrigir o *template* do chart que não aceita *digest*).
3. **(182)** P8: GitHub Actions num repositório de teste, `main` protegida.
4. **(182)** P9: 💸 alerta de custo **antes**; AWS real; `terraform destroy` e conferência.
5. **(182)** P10: Keycloak com o realm real, **T21** → `503`, rotação no RDS, cosign por *digest*.
6. **Mapa de migrações** (item 2.10) e `flyway migrate` num banco **vazio** e num **com massa**.
7. **S1** no PayFlow: saga com falha injetada em cada passo, os 5 testes de idempotência, CQRS com o **projetor parado** (o *fallback* e a espera **fora** da transação, se você adotar a ação do postmortem), limite com Redis.
8. **S2:** a primeira linha de base e o k6 em modelo aberto até o **joelho**: são os números que a S4 e o design doc da Parte 4 pedem.
9. **S3:** partição com carga (`V19`/`V19_1`), arquivamento com restauração, réplica com o teste de "qual nó serviu".
10. **S4:** `promtool` com os rótulos reais, *runbooks* no CI, o E1 de caos e um **ensaio de restauração** com o RTO medido.
11. **S5:** política de autorização com mutantes, papéis do banco, V25 com o `DENIED`, *pipeline* de segurança.
12. **S6:** 💸 **só** com conta, MFA e orçamento **antes**: *landing zone*, SCPs, orçamentos; sem conta, `terraform validate` e a estimativa refeita com preços novos.
13. **S7 e S8:** RFC-0001 decidida, `DocLab` e PIT no CI, README de portfólio executado num **clone limpo**, `LICENSE`.

Os passos **6 a 11** rodam **localmente** (Docker, Testcontainers) e **não** dependem dos passos 2 a 5: se preferir, comece por eles logo depois do passo 1 (decisão sua; a ordem da 182 não muda).

> **Manter o material vivo:** em 11 dias, **1 link** das referências saiu do ar (o da documentação do Docker), o **Spring Boot 4.2.0** saiu e os **preços** da AWS mudam todo mês. Antes de começar os testes práticos, refaça a varredura de links, o `fetch-prices.py` e a conferência de versões (`versoes.md`).

## ➡️ Próximo
Não há módulo novo: o próximo passo são os **testes práticos finais** (lista acima), quando você pedir, **depois** das decisões pendentes da Parte 1. Volte ao [índice do treinamento](../README.md) ou ao [progresso](../00-guia/progresso.md).
