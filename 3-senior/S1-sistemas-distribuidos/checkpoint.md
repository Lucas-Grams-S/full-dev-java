# S1.CP — Checkpoint do módulo S1

> ⏱️ ~1h30 · 🟣 Sênior · Pré-requisitos: S1.01 a S1.13

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, OpenJDK **25.0.5**, **Java puro, sem Maven e sem Docker**): os **13 programas** de [`lab/`](lab/README.md) (`java X.java`; saídas em `lab/out-s101.txt` … `out-s113.txt`; `lab/run-all.sh` as regrava). **São modelos e simulações** (filas, quóruns, anel de *hash*, projeção, saga, idempotência, limitadores, relógios, colisões, *fan-out*, conciliação): mostram **mecanismos e ordens de grandeza**, **não** o desempenho de PostgreSQL, Kafka, Redis ou ALB. **Não executado:** **tudo** o que toca o PayFlow do aluno (`V13`–`V17`, projetor do extrato, orquestrador da saga, `idempotency_keys`, filtro de *rate limit*, `Clock` injetado, `ReconciliationJob`), Redis/Lua, Testcontainers, ALB, CloudFront, PSP. Os **números** das aulas de projeto (S1.11–S1.13) vêm de **premissas inventadas** (declaradas).

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Por que **dobrar as máquinas** não dobra a vazão? Com **p = 0,95** paralelizável, qual o **limite** e quantas máquinas dão **10×**? No laboratório de balanceamento, por que o ***round-robin*** falhou com **75%** de carga total?
2. Enuncie o **CAP** corretamente e o que o **PACELC** acrescenta. Por que `W=2, R=1` com `N=3` **ainda** deu leituras velhas e `W=2, R=2` **não**?
3. **Hash mod N** × **anel com nós virtuais**: o que cada um moveu ao ir de 4 para 5 nós e qual o efeito dos **nós virtuais** no equilíbrio? Por que **uma conta** quente **não** se resolve com mais *shards*?
4. Descreva a **corrida do *cache-aside*** e **duas** mitigações. Por que um cache de **10%** das chaves acertou **73,5%** num caso e **9,9%** noutro? Por que **nunca** pôr `public` num extrato?
5. **CQRS × *event sourcing***: a diferença. Como o projetor do extrato fica **idempotente** e **detecta lacuna**? O que o **token de versão** garante e qual o **fallback**?
6. Por que o PayFlow **não adota *event sourcing*** (e o que adota dele)? Cite **3 custos** do padrão.
7. Por que o **2PC bloqueia**? Quais os **três tipos de passo** de uma saga e por que **"notificar" não se compensa**? Por que **timeout ≠ falha**?
8. Por que a **chave de idempotência** vem do **cliente**? O que acontece se **efeito e registro** da chave **não** estão na **mesma transação**? O que o laboratório mediu **sem** e **com** chave (10.000 pedidos, 30% de respostas perdidas)?
9. **Janela fixa** × **deslizante** × **balde de fichas** (o que cada um deixou passar na virada) e por que o **limite por instância** de 5 nós deu **500/s**. Qual a **política de falha** do PayFlow para o Redis do limitador?
10. Por que `ORDER BY created_at` **entre instâncias** é perigoso e **o que ordena** o PayFlow? **Lamport × vetorial × HLC** em uma frase cada. Em *system design*, por que "**código curto ≠ segredo**" e por que `CAPTURED ≠ SETTLED`?

<details><summary>Gabarito</summary>

1. A parte **serial** (lock, banco único) limita o ganho (**Amdahl**): `S(n) = 1/((1−p) + p/n)`. Com `p = 0,95` o **limite é 20×** (1024 máquinas dão **19,6×**); **10×** pedem **19** máquinas (`0,95/0,05`). O *round-robin* mandou **25%** (62,5 req/s) ao servidor que aguenta **33 req/s**: a **fila dele é instável** (p99 de ~1,3 milhão de ms no fim da simulação; o valor depende da duração, o que importa é a **instabilidade**), enquanto **menos requisições em andamento** (p99 **116 ms**) e **P2C** (**244 ms**) **descarregaram** o lento (40 mil e 51 mil pedidos). O que precisa ser estável é **cada fila**, não a média.
2. **CAP:** **durante uma partição**, escolher entre **linearizabilidade (C)** e **disponibilidade (A)**; o "C" **não** é o C do ACID; **P não é opção**. **PACELC:** **senão** (sem partição), **latência × consistência**. Com `W=2,R=1` só 2 das 3 réplicas têm a escrita, então a leitura de 1 cai na que **não** tem com prob. **1/3** (medido **33,34%**); `R + W > N` garante **interseção** (medido **0%** em 100 mil).
3. `hash mod N` moveu **80%** (79.979/100.000); o anel moveu **~20%** (ideal 1/5). **1 nó virtual:** maior nó **2,85×** o ideal; **16:** 1,21×; **128:** 1,10×; **512:** 1,07×. **Chave quente:** a **mesma chave** cai sempre no **mesmo** *shard*; uma conta tinha **15,1%** dos acessos; trate com **cache**, **dividir a chave**, **fila por chave**.
4. Leitor lê o banco (velho) → escritor grava e **invalida** → leitor **preenche** com o velho: **cache velho até o TTL** (`banco=40.00 cache=100.00`). **Mitigações:** **TTL curto**, **preenchimento condicional por versão**, não cachear o que decide dinheiro. **73,5%** com **Zipf** (poucas chaves quentes) × **9,9%** com acesso **uniforme**: o cache só ajuda com **localidade**. `public` num CDN serve o extrato de **A** para **B**; use **`no-store`**.
5. **CQRS** separa **modelos de comando e de consulta** (pode ser com banco relacional); ***event sourcing*** guarda **eventos como verdade**. **Projetor:** `PRIMARY KEY (account_id, account_seq)` + `ON CONFLICT DO NOTHING` (**idempotente**), `applied_seq` por conta e **lacuna ⇒ recusa e retry/rebuild**; soma, inserção e *checkpoint* na **mesma transação**. **Token de versão:** a leitura **só devolve depois** de aplicar aquele `seq` (*read-your-writes*; medido **0,00 → 50,00 em 151 ms**); **fallback** ao `ledger_entries` se estourar o tempo (**nunca** dado velho nem erro).
6. O **ledger imutável** (regra 5) + `balance = Σ lançamentos` (regra 3) + compensação (decisão 114) **já dão** o principal (auditoria e reconstrução). **Custos:** **consultas** exigem projeção, **evolução** de eventos (*upcasting*), **privacidade** (LGPD em log imutável), consistência eventual e curva de aprendizado. **Adota:** eventos **imutáveis versionados**, **correção por evento**, **reconstrução** de projeções. Gatilhos para reabrir estão na ADR 0024.
7. Depois do `PREPARE` o participante **prometeu** e só o **coordenador** decide: se ele cai, **ninguém pode decidir** (simulação: **500 de 1000** ticks bloqueados); soma de disponibilidades; Kafka/HTTP não falam XA; no PostgreSQL `max_prepared_transactions = 0` por padrão. **Passos:** **compensável**, **pivô** (sem volta), **repetível** (depois do pivô). **Notificar** é **repetível**: compensar dinheiro por causa de **e-mail** estorna o que **estava certo** (medido: a saga que o tratou como compensável **desfez** o pagamento; a que o **repetiu** completou). **Timeout** = **resultado desconhecido** (o efeito pode ter ocorrido): **repetir com a mesma chave** ou **consultar**.
8. Só o **cliente** sabe que duas chamadas são a **mesma intenção**; o servidor geraria **chave nova** a cada repetição. **Sem a mesma transação:** a queda **entre o efeito e a chave** faz o *retry* **repetir o efeito** (medido: `debitos = 2`). **Medido:** sem chave, **14.253** débitos para **10.000** pedidos (saldo −**R$ 42.530,00**); **com** chave, **10.000** (saldo exato) apesar de **4.223** repetições.
9. **Janela fixa:** **200** em ~0,2 s (limite 100/min); **deslizante:** **100**; **balde:** **100** (capacidade = rajada máxima). **Por instância:** cada nó limita **sozinho** → **N × limite** (**500/s**); **cota local** só com tráfego **uniforme** (cliente em 2 nós: **40/s**). **PayFlow (decisão 192):** **modo degradado** (cota local conservadora **e alerta**) quando o Redis cai; **fail-open puro** deixa passar 10% do abuso (simulação) e **fail-closed** derruba 10% dos legítimos.
10. Relógios de máquinas **divergem e saltam**; a escrita **mais nova** pode ter o **timestamp menor** (LWW descarta). O PayFlow ordena por **`account_seq`** (um dono: o banco), **chave Kafka = `accountId`**, `created_at` **do banco** e **`Clock` injetado**. **Lamport:** ordem **consistente com a causalidade**, **não** detecta concorrência; **vetorial:** **detecta concorrência** (vetores incomparáveis), cresce com os processos; **HLC:** tempo físico + contador, **respeita causalidade entre mensagens**, **não** conserta escritas independentes. **Código curto não é segredo:** com 1 bilhão de URLs em 62^7 códigos, **0,0284%** por chute: **varredura** acha links; **autentique o destino**. **`CAPTURED`** é o que **nós** registramos; **`SETTLED`**, o que o **PSP** confirmou (**conciliação**).
</details>

## Parte 2: Entrega prática (tag `payflow-S1`)
O `payflow` ganha **a saga de pagamentos** e o **CQRS do extrato**, com **idempotência por passo**, **limite no gateway**, **ordenação por sequência** e a **ADR 0024**. **Contrato HTTP `/api/v1` inalterado** (os novos *endpoints* são **aditivos**); o **dinheiro** continua **numa transação** onde for possível (decisão 104).
```
payflow/
├── src/main/resources/db/migration/
│   ├── V13__ledger_account_seq.sql             accounts.ledger_seq + ledger_entries.account_seq (expand + backfill)   (S1.05)
│   ├── V14__statement_read_model.sql           statement_entries, statement_checkpoints                              (S1.05)
│   ├── V15__payment_saga.sql                   account_holds, payment_sagas                                           (S1.07)
│   ├── V16__payment_request_short_code.sql     payment_requests.short_code                                            (S1.11)
│   └── V17__idempotency_keys.sql               idempotency_keys (owner, key, request_hash, status, response)          (S1.08)
├── src/main/java/com/payflow/
│   ├── statement/…                             projetor Kafka idempotente + detecção de lacuna + rebuild             (S1.05)
│   ├── payment/saga/…                          State, SagaStep, orquestrador, varredura (ShedLock)                    (S1.07)
│   ├── shared/idempotency/…                    filtro + serviço (hash canônico, 422/409/400)                          (S1.08)
│   ├── shared/ids/{Base62,Feistel}.java        domínio puro                                                           (S1.11)
│   └── config/ClockConfig.java                 @Bean Clock; Instant.now() proibido fora de config (ArchUnit)          (S1.10)
├── src/test/java/com/payflow/
│   ├── statement/{StatementProjectorTest,StatementReconciliationTest}.java                                            (S1.05)
│   ├── payment/saga/PaymentSagaTest.java       falha injetada em cada passo + invariante de conservação              (S1.07)
│   ├── shared/idempotency/IdempotencyIT.java   repetição, corpo diferente, concorrência, queda, mutante              (S1.08)
│   ├── gateway/RateLimitFilterTest.java        200/429/Retry-After/modo degradado + mutante                           (S1.09)
│   ├── api/ApiNoStoreTest.java                 toda /api/** com Cache-Control: no-store                               (S1.04)
│   ├── payment/{PaymentStateMachineTest,LedgerInvariantTest}.java + ReconciliationJobTest                             (S1.12)
│   └── arch/NoWallClockInDomainRule.java       fixture ruim prova que a regra falha                                   (S1.10)
├── docs/{escala,consistencia,particionamento,payment-saga,rate-limits}.md   system-design/{url-curta,pagamentos,notificacoes}.md
projeto/adrs/0024-sistemas-distribuidos-do-payflow.md   (já redigida nesta geração)
```
Critérios de pronto:
- [ ] **A conta de conexões** (decisão 181) refeita para o plano de escala (`docs/escala.md`) e a **tabela de teto por componente** existe.
- [ ] **Tabela operação → consistência → mecanismo** (`docs/consistencia.md`) com a **mensagem de erro** de cada linha; **nenhuma decisão financeira** lê réplica ou *read model*.
- [ ] **Particionamento:** `docs/particionamento.md` com a **análise de chaves** de `ledger_entries`, as **operações que cruzam contas** e os **gatilhos numéricos** (a medir) para reabrir o *sharding*.
- [ ] **`/api/**` com `no-store`** (teste que falha sem o cabeçalho); **chave de cache** com `sub`; **TTL com *jitter***; **taxa de acerto** como métrica com alerta.
- [ ] **Extrato CQRS:** projetor **idempotente**, **lacuna recusada**, **reconstrução** por conta, **invariante** `Σ read model = accounts.balance`, `X-Min-Seq` com **fallback** ao ledger (**teste com projetor parado**); **mutante** (sem `ON CONFLICT`) reprova.
- [ ] **Sem *event sourcing***: ADR 0024 com **gatilhos** para reabrir.
- [ ] **Saga:** máquina de estados **sem beco**, cada passo **idempotente** e classificado (**compensável/pivô/repetível**); **falha injetada em cada passo** com **conservação**; **mutante** reprova; **política de timeout do antifraude** com **dono** (quem aprovou) e *runbook* de `NEEDS_ATTENTION`.
- [ ] **Idempotência ponta a ponta:** tabela de **saltos** com a **chave de cada um** (inclusive **PSP** e eventos); 5 testes (repetição, **422**, concorrência, queda, **mutante**).
- [ ] **Rate limit:** tabela rota × chave × algoritmo × taxa × política de falha; **429 + `Retry-After`**; **modo degradado** testado; limite **na OpenAPI**; alertas de `429` e de `ratelimit_degraded_total`.
- [ ] **Ordem:** extrato por **`account_seq`** (teste que **embaralha** `occurred_at`); **regra ArchUnit** contra `Instant.now()` no domínio **com fixture ruim**.
- [ ] **Pagamentos:** máquina de estados, **invariante do ledger** (com mutante), *job* de **conciliação** (4 categorias, alerta em `SO_NO_PSP`), tabela de **falhas do PSP**.
- [ ] Os **3 documentos** de *system design* (`url-curta`, `pagamentos`, `notificacoes`) com **7 passos**, **premissas numéricas** e **trade-offs**; ADR 0024 revisada; tag `payflow-S1`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Sênior: **≥ 2** em todos e **3** nos marcados com ⭐.
| Competência (matriz) | Evidência no módulo | 0–3 |
|---|---|---|
| ⭐ **Sistemas distribuídos:** raciocina com CAP/PACELC, consistência eventual, CQRS, event sourcing | S1.02, S1.05, S1.06 (quórum medido; token de versão; decisão de **não** adotar ES) | |
| ⭐ **Mensageria:** escolhe consistência, ordenação e garantias; **sagas** | S1.07, S1.08 (saga com pivô; idempotência por passo; ordem por `account_seq`) | |
| ⭐ **Arquitetura:** decide limites, registra ADRs, evita complexidade desnecessária | S1.03, S1.05, S1.06 (ADR 0024: **não** fazer *sharding*, **não** adotar ES, com gatilhos) | |
| **Banco de dados:** particiona, escolhe por caso de uso | S1.03 (hash consistente, chave quente, chave monotônica) | |
| **APIs REST:** governa idempotência e compatibilidade | S1.08, S1.09 (`Idempotency-Key`, 422/409, 429 + `Retry-After`) | |
| **Linguagem Java:** modela estruturas para escala; `Clock`, `sealed`/`switch` | S1.01, S1.06, S1.10, S1.11 (Feistel/Base62, `Clock` injetado) | |
| **Lógica e estruturas de dados:** filas, particionamento, justificativa | S1.03, S1.04, S1.11 (LRU/Zipf, anel, colisões) | |
| **Performance:** raciocínio com Amdahl, percentis, cache | S1.01, S1.04 (p99 por algoritmo; hit ratio) | |
| ⭐ **System design (entrevista):** método, estimativa, trade-off em voz alta | S1.11–S1.13 (3 exercícios resolvidos) | |
| **Segurança:** IDOR por código curto; `no-store`; webhook assinado | S1.04, S1.11, S1.12 | |
Se **≥ 2** em tudo e **3** nos ⭐ → siga para o **S2**. Linhas em **0–1**: refaça a aula indicada e o **laboratório** correspondente.

## Parte 4: Simulado de *system design* (45 min, **sem consulta**)
Escolha **um** e conduza os **7 passos** em voz alta (ou por escrito), **com premissas numéricas**:
1. **"Projete um sistema de ingressos (venda de lugares numerados)"**: dica: **estoque finito**, **reserva com expiração** (S1.07), **consistência** (S1.02), **idempotência** (S1.08), **fila de espera** e **rate limit** (S1.09).
2. **"Projete um serviço de *rate limiting* como produto"**: S1.09 + S1.03 (particionar por chave) + política de falha.
3. **"Projete a carteira do PayFlow em 3 regiões"**: S1.02, S1.03, S1.10; o que é **local**, o que é **global**, **onde fica o ledger**.
**Rubrica (100 pontos):** requisitos e premissas (15) · estimativa coerente (15) · desenho com **responsabilidades** (15) · **aprofundamento** em 1–2 pontos difíceis com **mecanismo** (20) · **falhas e resultado desconhecido** (15) · **trade-offs** ditos em voz alta (10) · **o que mede** e **evolução** (10). **Aprovação:** ≥ 70.

## ➡️ Próximo
[S2 — Performance e eficiência](../S2-performance-eficiencia/README.md)
