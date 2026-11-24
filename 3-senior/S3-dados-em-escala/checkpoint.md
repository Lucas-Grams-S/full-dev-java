# S3.CP — Checkpoint do módulo S3

> ⏱️ ~1h30 · 🟣 Sênior · Pré-requisitos: S3.01 a S3.09

> ✅ **Validação final (2026-11-24):** módulo executado no PayFlow reconstruído; resultado e o que continua não executado no [README do módulo](README.md). Os avisos "não executado" / "por leitura" abaixo descrevem a escrita original.

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, **PostgreSQL 18** em Docker, **máquina compartilhada de 12 núcleos**; JDK 25.0.5): `lab/run-s301.sh` a `lab/run-s309-generated.sh` (migrações sem downtime, particionamento e *swap* com carga, réplica de *streaming* + roteamento em Java, arquivamento com restauração, busca em texto no PostgreSQL, *logical decoding* e *slots*, BRIN, analytics, migração de coluna com carga concorrente e mutante). **NÃO executados:** o **PayFlow do aluno** (Spring, JPA, Flyway), **OpenSearch/Elasticsearch**, **Debezium/Kafka Connect**, **RDS** (réplicas, Multi-AZ), **S3/Object Lock/Athena**, armazéns colunares, MongoDB/DynamoDB. Tudo isso é **leitura de documentação** (links conferidos) e **está marcado** nas aulas. Os **milissegundos mudam a cada execução**; as **ordens de grandeza** e os **mecanismos** não.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Por que um `ALTER TABLE` "de 2 ms" pode parar as **leituras** de uma tabela? O que a **fila de *locks*** fez com o leitor C no laboratório e **como** o `lock_timeout` a desfez?
2. Quais são os **três** passos para colocar `NOT NULL` numa tabela grande **sem** varrer sob `ACCESS EXCLUSIVE`? Qual *lock* o `VALIDATE` pede? O que o **PostgreSQL 18** acrescentou?
3. Por que o `UPDATE` único de 1 milhão de linhas foi pior que os **lotes de 20 mil**, **embora mais rápido no total**? Que **número** mostrou isso? E por que o **ledger** nunca tem *backfill*?
4. Por que a **chave primária** de uma tabela particionada por `created_at` vira `(id, created_at)`? Que **consequência** isso tem para `findById` no ledger?
5. No laboratório, o extrato de **uma conta sem filtro de data** leu **quantas partições**? E com filtro **aberto** e **fechado**? Por que o particionamento **não acelera** esse extrato?
6. Como se converte a tabela **existente** em particionada **sem parar**? Que **duas coisas** o `ATTACH PARTITION` exigiu e o que o `CHECK` validado fez no tempo?
7. O que acontece quando uma consulta **longa** roda na réplica enquanto o `VACUUM` do primário remove versões de linhas? Compare `hot_standby_feedback` **desligado** e **ligado** (o que cada um custou, com o que foi **medido**).
8. Por que o roteamento por `AbstractRoutingDataSource` mandou **0 de 10** transações `readOnly` à réplica **sem** o `LazyConnectionDataSourceProxy`? Como o **token LSN** garantiu *read-your-writes* e **quanto custou** com a réplica 2 s atrasada?
9. Quais são as **etapas e a ordem** do arquivamento de uma partição (e **por que** a restauração vem **antes** do `DROP`)? Por que o **saldo** precisa de `period_balances`? Quem **pode** remover dados?
10. **CDC:** o que o `peek` provou sobre a entrega e o que o *slot* **abandonado** fez com o WAL? E na **migração da coluna** `phone`: o que o **mutante** provou e por que a *trigger* só sai na contração?

<details><summary>Gabarito</summary>

1. O `ALTER` pede `ACCESS EXCLUSIVE` e **espera** a leitura longa (A); o PostgreSQL **enfileira** os pedidos para o `ALTER` não passar fome, então o leitor C (que **não** conflita com A) **espera atrás do `ALTER`**: C esperou **~4,6 a 5,5 s** nas execuções que fiz (A segurava por ~6 s). Com `lock_timeout = 1s` o `ALTER` **desistiu** (`canceling statement due to lock timeout`) e C esperou **~0,2–0,6 s**: **falhar rápido é a técnica**; esperar "até conseguir" é o incidente.
2. **(a)** `ADD CONSTRAINT ... CHECK (col IS NOT NULL) NOT VALID` (ms, só linhas novas), **(b)** `VALIDATE CONSTRAINT` (varre; só `SHARE UPDATE EXCLUSIVE`: **não bloqueia** escrita), **(c)** `SET NOT NULL` (**sem varredura**, ~ms contra ~300 ms) e remover o `CHECK`. O **PG 18** aceita `ADD CONSTRAINT ... NOT NULL col NOT VALID` direto (linha nova com `NULL` é recusada de imediato) + `VALIDATE`. A versão antiga continua valendo.
3. O único `UPDATE` segurou as linhas por **~9–14 s**: o `MAX` das atualizações pontuais concorrentes foi da ordem de **10 s** (**4–5 acima de 1 s**), contra **~0,3–0,7 s** e **zero acima de 1 s** em lotes; o total em lotes foi **maior** (pausas e *overhead*), e o **WAL** foi **o mesmo** (~380 MB). O ledger **recusa** `UPDATE` (a *trigger* append-only, regra 5): expandir é só `ADD COLUMN` com *default* **constante**.
4. Cada partição tem **seu** índice; a unicidade global só é garantível se a **chave de partição** fizer parte da restrição (`unique constraint on partitioned table must include all partitioning columns`). `findById(id)` **sonda o índice de cada partição**; o extrato usa **conta + período**, e a unicidade global (`idempotency_key`) **fica em outra tabela**.
5. **26** (25 mensais + a `DEFAULT`) com **planejamento ~7 ms** contra ~0,3–0,5 ms e execução ~4 ms × **~0,3 a 1,7 ms** na tabela única; **aberto** (`>=`): **5**; **fechado** (`>=` e `<`): **3**. O índice `(account_id, created_at DESC)` **já** fazia o extrato; o particionamento só ajuda **consulta por período** (mês: ~17 ms × ~200 ms) e **manutenção/retenção**.
6. **Preparar online** (`CREATE UNIQUE INDEX CONCURRENTLY (id, created_at)` e um `CHECK` **igual à faixa**, `NOT VALID` + `VALIDATE`) e, numa **transação curta** com `lock_timeout`: renomear, criar a particionada, **anexar a antiga**. O `ATTACH` exigiu **(i) o mesmo índice único** (a PK antiga `(id)` teve de ser **trocada** com `USING INDEX`) e **(ii) as `CHECK` com o mesmo nome**; o `CHECK` validado fez o `ATTACH` **sem varredura** (**~1 ms** × ~80 ms em 1,5 milhão de linhas). O *swap* levou **~150–190 ms** com **0 falhas**.
7. **Sem feedback:** a consulta de 12 s foi **cancelada** após o `max_standby_streaming_delay` (3 s) com `canceling statement due to conflict with recovery`, e o primário **limpou** (`150010 removed`). **Com feedback:** a consulta **sobreviveu**, **mas** o primário **não pôde limpar** (`150010 are dead but not yet removable`): *bloat* no primário. Não há opção grátis.
8. A conexão é pedida **antes** de a flag `readOnly` existir no contexto: sem o *proxy* preguiçoso, tudo foi ao primário (**0 de 10**), **sem erro**; com ele, **10 de 10**. O token (`pg_current_wal_lsn()` pós-*commit*) obriga a réplica a **já ter aplicado** o LSN: com **2 s de atraso**, **sem token 20 de 20 leram errado; com token 0** (todas caíram no primário), ao custo de **~330 ms** por leitura (os 300 ms de espera).
9. **Impressão digital** (contagem, soma, `md5`) → **`DETACH ... CONCURRENTLY`** (sem partição `DEFAULT`) → **exportar** + `sha256` → **restaurar num teste e comparar** → **manifesto** → `DROP`. A restauração vem **antes** porque depois do `DROP` o **arquivo é o único** exemplar. Sem `period_balances`, `Σ ledger vivo ≠ saldo` e o invariante dispara **alarme falso**. **Remove quem é dono** (o papel de arquivamento): a aplicação deu `permission denied` e `must be owner`.
10. O `peek` devolveu **os mesmos 4 eventos** duas vezes (só o `get` os consumiu): CDC é **pelo menos uma vez**, consumidor **idempotente**. O *slot* sem consumidor **reteve ~370 MB** de WAL por **1 milhão de linhas** atualizadas e, com `max_slot_wal_keep_size`, foi **invalidado** (`wal_removed`). O **mutante** (sem *trigger*) deixou **~100 mil linhas** com a coluna nova velha e o **portão 2 acusou**; a *trigger* só sai na F5 porque, enquanto a versão antiga puder escrever (ou for preciso reverter), ela mantém a nova correta.
</details>

## Parte 2: Entrega prática (tag `payflow-S3`)
O `payflow` ganha **os procedimentos** de dados em escala, **as migrações V18–V24** (rascunhos, **por leitura**), **o roteamento explícito do `reporting`**, **o arquivamento** e **a ADR 0026**. **Nada foi compilado nem executado no PayFlow**; o laboratório `3-senior/S3-dados-em-escala/lab` é o **modelo executado**.
```
payflow/
├── src/main/resources/db/migration/
│   ├── V18__expand_users_phone_e164.sql             coluna + função + trigger                      (S3.09)
│   ├── V19__ledger_partition_prepare.sql            índice único CONCURRENTLY + CHECK da faixa      (S3.02)
│   ├── V19_1__ledger_partition_swap.sql             renomear + particionar + anexar a antiga       (S3.02)
│   ├── V20__archive_manifest_and_period_balances.sql  archive_manifest, account_period_balances     (S3.04)
│   ├── V21__outbox_cdc_publication.sql              publicação + papel debezium (se CDC adotado)   (S3.06)
│   ├── V22__daily_volume.sql                        resumo diário (carga por janela recalculada)   (S3.08)
│   ├── V23__users_phone_e164_check.sql              CHECK NOT VALID + VALIDATE; remove trigger      (S3.09, contração)
│   └── V24__drop_users_phone.sql                    DROP COLUMN phone (só depois da janela)         (S3.09, contração)
├── docs/dados/
│   ├── migracoes.md           lista de revisão de migração; lock_timeout; backfill fora do Flyway    (S3.01)
│   ├── particionamento.md     chave, partições à frente (12 meses), alerta, DETACH/DROP, sem DEFAULT   (S3.02)
│   ├── replicas.md            roteamento EXPLÍCITO do reporting, token LSN, fallback, alarmes         (S3.03)
│   ├── arquivamento.md        ciclo de 6 passos, papéis, política de retenção (PROPOSTA ao jurídico)    (S3.04)
│   ├── busca.md               onde cada busca acontece, gatilhos G1–G4 (a medir)                       (S3.05)
│   ├── cdc.md                 relé por consulta × CDC, slot, monitoramento, max_slot_wal_keep_size      (S3.06)
│   ├── armazenamento.md       matriz componente → banco, BRIN, plano de saída                          (S3.07)
│   ├── analytics.md           escada 0–5, janela recalculada + reconciliação, o que não entra          (S3.08)
│   └── migracao-phone.md      5 fases, 4 portões, backfill, leitura-sombra, fila de revisão           (S3.09)
├── src/main/java/com/payflow/reporting/ReportingDataSourceConfig.java    segundo DataSource + gate        (S3.03)
├── src/main/java/com/payflow/user/PhoneNormalizer.java                   gêmeo Java da função SQL         (S3.09)
├── src/test/java/com/payflow/user/PhoneNormalizerTest.java               lê phone-cases.tsv (14 casos)    (S3.09)
├── src/test/java/com/payflow/arch/ReportingDataSourceRuleTest.java       ArchUnit: só o módulo reporting  (S3.03)
└── (jobs: partitions-ahead, archive-ledger-month, daily-volume, phone-backfill; ShedLock)               (S3.02/04/08/09)
projeto/adrs/0026-dados-em-escala-do-payflow.md   (já redigida nesta geração)
```
Critérios de pronto:
- [ ] **Migrações:** toda migração com `lock_timeout`/`statement_timeout` no topo; **`CONCURRENTLY` em script próprio**; *backfill* **fora** do Flyway, em **lotes por `id`**, com `COMMIT` por lote; **lista de revisão** no *template* de PR; **teste** que roda a suíte da versão N contra o banco **migrado** (**a executar**).
- [ ] **Particionamento:** `ledger_entries` **`RANGE (created_at)` mensal**, **sem `DEFAULT`**, **12 meses à frente** com alerta (< 90 dias), *trigger* append-only **no pai** (teste de `UPDATE` que falha), **extrato com intervalo fechado**; o *swap* (`V19`/`V19_1`) com `lock_timeout`.
- [ ] **Réplicas:** **roteamento explícito** do `reporting` (**não** global); **token (LSN)** e *fallback* **só** onde houver leitura com consistência de sessão; *fallback* **dimensionado** na conta de conexões; **teste** que prova **qual nó** serviu; **alarmes** de `replay_lag`, réplica fora e conflitos.
- [ ] **Arquivamento:** o **ciclo de 6 passos** com restauração **antes** do `DROP`; `archive_manifest` (com `schema_version`); `account_period_balances` e o **invariante** ajustado; **papel de arquivamento** ≠ papel da aplicação; **política de retenção** como **proposta** (**5 anos = premissa**, jurídico decide).
- [ ] **Busca:** `docs/dados/busca.md` com **onde** cada busca acontece, **gatilhos G1–G4** com "a medir"; **nenhum** índice de texto na tabela do ledger; **autorização por dono no servidor**.
- [ ] **CDC:** **decisão** (relé por consulta × CDC) **com gatilhos**; se adotado: publicação por migração, `max_slot_wal_keep_size`, **alerta de retenção**, `heartbeat`, **colunas excluídas**, consumidor **idempotente**.
- [ ] **Armazenamento:** a **matriz** da S3.07 **corrigida** por você, **sem número inventado**; BRIN em `created_at` **das partições** (decisão **medida**).
- [ ] **Analytics:** `daily_volume` com carga por **janela recalculada (`=`)**, **reconciliação semanal**, *job* na **réplica de relatórios**, **atualizado em**.
- [ ] **Migração de `phone`:** as **5 fases** com **portões**, **teste de concordância** SQL × Java no CI, **leitura-sombra** sem telefone nos *logs*, **fila de revisão**, contração só após a **janela de reversão**.
- [ ] **ADR 0026** revisada; tag `payflow-S3`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Sênior: **≥ 2** em todos e **3** nos marcados com ⭐.
| Competência (matriz) | Evidência no módulo | 0–3 |
|---|---|---|
| ⭐ **Banco de dados:** desenha particionamento e **migrações sem downtime** | S3.01, S3.02, S3.09 (fila de *locks*, `NOT VALID`, *swap* de partição, migração de coluna sob carga) | |
| ⭐ **Sistemas distribuídos:** réplicas, consistência por operação, CDC e entrega "pelo menos uma vez" | S3.03, S3.06 (conflito de recuperação, token LSN, `peek`, *slot*) | |
| ⭐ **Liderança e comunicação:** decisões com gatilhos, planos com portões e reversão | S3.01, S3.07, S3.09 (ADR 0026; fases e portões) | |
| **Performance:** mede com método (planos, poda, BRIN, `EXPLAIN`) | S3.02, S3.05, S3.07 (26 partições, trigrama × *full-text*, BRIN × B-tree) | |
| **Observabilidade e operação:** monitora réplica, *slot*, partições e deriva | S3.02, S3.03, S3.06, S3.09 (alertas e métricas) | |
| **Segurança:** menor privilégio, dado pessoal em arquivo/CDC/busca/analytics | S3.04, S3.05, S3.06, S3.08, S3.09 (papéis; LGPD; autorização por dono) | |
| **Arquitetura:** escolhe armazenamento pelo padrão de acesso | S3.07 (matriz; "PostgreSQL primeiro" com evidência) | |
| **Nuvem:** serviços gerenciados (RDS, S3, OpenSearch Service) e seus limites | S3.03–S3.06 (**leitura**, não executado) | |
| **Linguagem Java:** roteamento de `DataSource`, normalizador, testes de concordância | S3.03, S3.09 (`AbstractRoutingDataSource`, `PhoneNormalizer`) | |
Se **≥ 2** em tudo e **3** nos ⭐ → siga para o **S4**. Linhas em **0–1**: refaça a aula indicada e o **laboratório** correspondente.

## Parte 4: Simulado: planejar uma mudança de dados com o sistema no ar (60 min, **com consulta ao cheatsheet**)
> ⚠️ **Cenário SINTÉTICO:** os **números** abaixo são **inventados** para o exercício; os **mecanismos** (fila de *locks*, `NOT VALID`/`VALIDATE`, *swap* de partição, `lock_timeout`, *slot*, mutante, janela recalculada) vêm das aulas S3.01–S3.09, **medidos** no laboratório. **Nunca** cite este dossiê como medição.

**Cenário.** A tabela `transfers` do PayFlow tem **180 milhões de linhas** (20 meses) e cresce **8 milhões por mês**; o primário tem **72%** de CPU no pico; o *backup* leva **9 h**. O produto pede, **no mesmo trimestre**:
- **(a)** adicionar **`fee_cents BIGINT NOT NULL`** em `transfers`, onde o valor das linhas **existentes** é **`round(amount * 0,5%)` em centavos** (**não** há *default* constante);
- **(b)** **particionar `transfers` por mês** (a PK é `id`; `idempotency_key` é `UNIQUE`);
- **(c)** **arquivar** transferências com **mais de 36 meses** (a política de retenção **ainda não existe**);
- **(d)** o time de dados quer **receber as transferências** num armazém quase em tempo real (**CDC**);
- **(e)** um relatório de **conciliação de 25 minutos** roda à noite e **já falhou** duas vezes na réplica com `conflict with recovery`.
**Restrições:** **sem janela de manutenção**; versão N e N+1 do código convivem; o ledger e as transferências são **dinheiro** (consistência forte); **LGPD**.

**Tarefas:**
1. **(a)** A **sequência de migrações** com **o *lock* de cada passo**, o ***backfill***, **como** a versão N continua inserindo e **como** entra o `NOT NULL`.
2. **(b)** O **problema** da PK e da `UNIQUE (idempotency_key)`; a **decisão** (e **onde** a unicidade global passa a viver); o *swap* **sem parar** e **o alerta** de partições.
3. **(c)** O que você **precisa decidir/obter antes** de arquivar; o **ciclo** e **quem** executa; o que **acontece com o invariante do saldo/relatórios**.
4. **(d)** **CDC × relé por consulta**: **a decisão** com critérios, **o risco** do *slot* e a **mitigação**.
5. **(e)** O **diagnóstico** do relatório e **duas** soluções, com o **preço** de cada (inclusive `hot_standby_feedback`).
6. **Ordem de entrega** dos cinco itens (com **portões** e **reversão**) e **o que NÃO fazer** no trimestre.

**Rubrica (100 pontos):** sequência (a) **correta e segura** (`lock_timeout`, `NOT VALID`/`VALIDATE`, *backfill* em lotes, versão N) (**20**); (b) PK/`UNIQUE` e *swap* (**20**); (c) pré-requisitos, ciclo, papéis e saldo (**15**); (d) CDC com critério e *slot* (**15**); (e) diagnóstico e preços (**15**); ordem, portões, reversão e "o que não fazer" (**15**). **Aprovado: ≥ 70.**
<details><summary>Gabarito (resumo)</summary>

**(a)** `ADD COLUMN fee_cents BIGINT` (nullable, sem *default*; `lock_timeout`; ms) **+ *trigger* `BEFORE INSERT` ** que preenche `round(NEW.amount*0.005*100)` (a **versão N** continua inserindo); ***backfill* fora do Flyway** em lotes de 10–20 mil por `id`, `WHERE fee_cents IS NULL` **só como verificação** (a faixa é por `id`), com pausa adaptativa (réplica/p99); `CHECK (fee_cents IS NOT NULL) NOT VALID` → `VALIDATE` (só `SHARE UPDATE EXCLUSIVE`) → `SET NOT NULL` (sem varredura) → remover o `CHECK`; no **PG 18**, `NOT NULL ... NOT VALID` + `VALIDATE`. O `ADD COLUMN ... NOT NULL DEFAULT 0` seria **uma linha**, mas **daria taxa 0** às antigas: um *default* constante **não substitui** um valor **calculado**. **Contração** só depois de **todo escritor** informar o valor.
**(b)** PK vira `(id, created_at)` e a `UNIQUE (idempotency_key)` **não pode** continuar global (não contém `created_at`): a unicidade passa a viver na **tabela `idempotency_keys`** (S1.08, **não** particionada por tempo, com TTL) **ou** `UNIQUE (idempotency_key, created_at)` (**insuficiente** como garantia global). **Swap:** índice único `(id, created_at)` `CONCURRENTLY` + `CHECK` da faixa `NOT VALID`/`VALIDATE` (online) → **transação curta** com `lock_timeout` (renomear, criar particionada, **anexar a antiga**; trocar a PK com `USING INDEX`; *triggers* e `CHECK`s com **mesmo nome**); **sem `DEFAULT`**; partições **12 meses à frente** + **alerta < 90 dias**; extrato com **intervalo fechado**. A partição legada (180 milhões) **fica** e é **arquivada inteira** quando a retenção alcançar.
**(c)** **Antes:** a **política de retenção assinada** pelo jurídico (**36 meses é premissa**), **base legal**, **saldos de fechamento** (`period_balances`) e **conciliação** que usa transferências antigas. **Ciclo:** impressão digital → `DETACH CONCURRENTLY` (**exige** ausência de `DEFAULT`) → exportar + `sha256` → **restaurar e comparar** → manifesto → `DROP`, por um **papel dono**, **da réplica**; **a aplicação não é dona**. O invariante passa a `abertura + vivo = saldo`.
**(d)** Para **um** consumidor e baixo volume, o **relé por consulta** basta; **CDC** se houver **vários consumidores** ou **latência** (gatilho a medir). **Risco:** *slot* parado **enche o disco do primário** (≈ **370 MB por 1 milhão de linhas** atualizadas no laboratório; aqui, **taxa de escrita × tempo**) — **mitigação:** `max_slot_wal_keep_size` < espaço livre, **alerta de retenção**, `heartbeat`, **dono** do conector, *runbook* (**refazer o *snapshot***); **Event Router**: a outbox vira **só-insere**; consumidor **idempotente**; **colunas excluídas** (LGPD).
**(e)** O relatório de 25 min é **cancelado** porque o `VACUUM` remove versões de que ele precisa (**conflito de recuperação**; medido: cancelado após `max_standby_streaming_delay`). **Soluções:** **(1)** réplica **dedicada** de relatórios com `max_standby_streaming_delay` **alto** (a réplica **atrasa** durante o relatório) e `hot_standby_feedback = off`; **(2)** `hot_standby_feedback = on` (**o relatório termina, mas o primário não limpa**: *bloat* em tabelas de alta rotatividade, como `accounts`; **medido**: `150010 are dead but not yet removable`); **(3)** exportar para o armazém (S3.08). **Escolha (1)**.
**Ordem:** **(e)** primeiro (config; **reversível**), **(a)** (expand + backfill; **reversível** até o `NOT NULL`), **(b)** (swap com portões; **depois** do *backup* validado), **(c)** (só **depois** da política assinada), **(d)** por último (CDC **depois** que a tabela estiver estável e particionada). **Não fazer:** `ALTER ... SET NOT NULL` direto; `UPDATE` único de 180 milhões; **partição `DEFAULT`**; `DELETE` em massa; **slot sem limite nem alerta**; arquivar **sem teste de restauração**; **`hot_standby_feedback` ligado "por garantia"**.
</details>

## ➡️ Próximo
[S4 — Confiabilidade e operação (SRE)](../S4-confiabilidade-sre/README.md)
