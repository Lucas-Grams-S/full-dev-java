# Design doc — Transferências recorrentes (módulo `recurring`)

- **Status:** em revisão (acompanha a [RFC-0001](rfc-0001-transferencias-recorrentes.md); só vira "aprovado" se a RFC for aceita)
- **Autor:** tech lead do PayFlow (você)
- **Revisores:** produto, segurança, SRE, dados
- **Última atualização:** 2026-11-23
- **Estimativa:** `estimativa-recorrentes.md` (S7.03)

## Contexto e escopo
O PayFlow é um monólito modular (P4) com o módulo `notification` extraído (P5), Outbox (P3), idempotência ponta a ponta (S1.08), SLOs (S4), política de autorização com MFA para mover dinheiro (S5.01) e estimativa de custo por decisão (S6). A RFC-0001 propõe **transferências mensais agendadas** como **módulo `recurring`** no monólito. Este documento detalha **como**: dados, fluxo, falhas, segurança, operação, custo e entrega. **Fora deste documento:** a decisão de produto sobre limites e aviso prévio (perguntas em aberto).

## Objetivos
- **Exatamente uma** transferência por ocorrência, **0 duplicadas** (invariante verificada por *job* diário de conciliação, S1.12).
- **99,5%** das ocorrências **processadas** (sucesso ou falha de negócio registrada) em até **60 min** do horário devido, medido em 30 dias (SLO **proposto**, a negociar com produto como na S4.01).
- Pico do dia mais popular drenado em **< 15 min** com no máximo **2 *workers*** (premissas do *spike*: 15 mil ocorrências, 20 ms cada → **150 s**).
- **Nenhuma** mudança no contrato `/api/v1` existente (só *endpoints* novos, aditivos).

## Não-objetivos
- Recorrência semanal, diária ou por calendário de dias úteis.
- Valor variável, múltiplos destinos, destino fora do PayFlow (Pix).
- Mais de **uma** nova tentativa por saldo insuficiente.
- Serviço separado (ver Alternativas e os gatilhos de extração da RFC).
- Mudar o ledger, o saldo ou a transferência: o `recurring` **usa** o `transfer` como está.

## Desenho

```mermaid
sequenceDiagram
    autonumber
    participant J as Agendador (recurring, 2 workers)
    participant DB as PostgreSQL
    participant T as transfer (API do módulo)
    participant O as Outbox
    J->>DB: SELECT ... WHERE next_due_at <= now() AND status='ACTIVE' FOR UPDATE SKIP LOCKED LIMIT 100
    loop cada agendamento devido (uma transação por ocorrência)
        J->>DB: INSERT recurring_runs (schedule_id, due_date) ON CONFLICT DO NOTHING
        J->>T: transfer(owner, from, to, amount, key = recurring:<id>:<due_date>)
        T->>DB: trava contas em ordem, ledger, saldo (decisões 96 e 104)
        J->>DB: run = SUCCEEDED; next_due_at = próxima pela regra
        J->>O: RecurringRunSucceeded (mesma transação)
    end
```

### Dados (migração V26, só tabelas novas: *expand* puro, sem trava em tabela existente)
```sql
CREATE TABLE recurring_transfers (
    id                 UUID PRIMARY KEY DEFAULT uuidv7(),
    owner_id           UUID NOT NULL REFERENCES users (id),
    source_account_id  UUID NOT NULL REFERENCES accounts (id),
    target_account_id  UUID NOT NULL REFERENCES accounts (id),
    amount             NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    day_of_month       SMALLINT NOT NULL CHECK (day_of_month BETWEEN 1 AND 31),
    time_zone          VARCHAR(40) NOT NULL DEFAULT 'America/Sao_Paulo',
    next_due_at        TIMESTAMPTZ NOT NULL,
    status             VARCHAR(20) NOT NULL,
    consent_event_id   UUID NOT NULL,             -- evento RECURRING_CREATED em audit_events (V25)
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at       TIMESTAMPTZ,
    CONSTRAINT ck_recurring_transfers_status CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CONSTRAINT ck_recurring_transfers_accounts CHECK (source_account_id <> target_account_id)
);
CREATE INDEX ix_recurring_transfers_due ON recurring_transfers (next_due_at) WHERE status = 'ACTIVE';

CREATE TABLE recurring_runs (
    id           UUID PRIMARY KEY DEFAULT uuidv7(),
    schedule_id  UUID NOT NULL REFERENCES recurring_transfers (id),
    due_date     DATE NOT NULL,                   -- data LOCAL da ocorrência (fuso do agendamento)
    attempt      SMALLINT NOT NULL DEFAULT 1,
    status       VARCHAR(20) NOT NULL,
    transfer_id  UUID REFERENCES transfers (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_recurring_runs_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED_FUNDS', 'FAILED')),
    CONSTRAINT uq_recurring_runs_occurrence UNIQUE (schedule_id, due_date)
);
```
- `next_due_at` é um **instante** (UTC no banco); a **regra** (dia + fuso) gera o próximo: `YearMonth.atDay(min(dia, tamanho do mês))` às 06:00 no fuso. O *spike* mostrou que `plusMonths(1)` **encadeado** fica preso no dia 28 e que `plusDays(30)` erra 10 de 12 meses.
- **Um dono por tabela** (decisão 124): só o `recurring` lê e escreve essas tabelas.

### API (aditiva, `/api/v1`, Problem Details, dinheiro como *string*)
| Método e rota | Regra |
|---|---|
| `POST /api/v1/recurring-transfers` | `SCOPE_transfers:write` + **MFA (*step-up*)**; `AuthorizationPolicy.decide(chamador, CREATE_RECURRING, dono da origem)`; `Idempotency-Key` obrigatório (S1.08); `201` com `Location` |
| `GET /api/v1/recurring-transfers?cursor=…` | `SCOPE_transfers:read`; só os do `sub`; paginação por cursor |
| `DELETE /api/v1/recurring-transfers/{id}` | dono; alheio = **404** (IDOR, J8.06); cancela **antes** do disparo do dia; idempotente |

### Execução
1. O agendador (`@Scheduled` a cada minuto + **ShedLock**, P3.02) busca até 100 agendamentos devidos com `FOR UPDATE SKIP LOCKED` e os distribui a **2 *workers*** (limite fixo: protege o *pool*).
2. **Uma transação por ocorrência:** insere o `recurring_runs` (`ON CONFLICT DO NOTHING`: se já existe, outro nó ganhou), chama `transfer` com a chave derivada, marca `SUCCEEDED`, avança `next_due_at`, grava `RecurringRunSucceeded` no Outbox.
3. **Saldo insuficiente:** a exceção de negócio **não** pode marcar a transação do `transfer` como *rollback-only* e depois ser engolida (o Spring lança `UnexpectedRollbackException` no *commit*): a ocorrência registra `FAILED_FUNDS` numa transação **separada** (`REQUIRES_NEW`, como o `DENIED` da decisão 242), agenda **uma** nova tentativa às 18:00 e publica `RecurringRunFailed`. **Testar antes de implementar** (pergunta técnica 1).
4. **Antes de mover dinheiro**, a política reconfere: agendamento `ACTIVE`, dono ainda dono da conta de origem, contas `ACTIVE`. Falhou → `FAILED` + evento, sem transferência.

## Alternativas consideradas

### Serviço separado `recurring-service`
Descartada **porque** as 5 perguntas da decisão 126 não mostram dor medida e a rede traria *token* de serviço com escrita, consistência eventual e idempotência entre processos sem ganho hoje. Gatilhos de extração na RFC.

### Fila de ocorrências pré-geradas (um registro por mês, criado com 12 meses de antecedência)
Descartada **porque** cancelar ou mudar o valor exige apagar ou reescrever o futuro (mais estados, mais *bugs*); a regra + `next_due_at` gera **uma** ocorrência por vez.

### Calcular a próxima data somando 30 dias ou encadeando `plusMonths(1)`
Descartada **porque** o *spike* mostrou datas erradas (o dia 31 vira 28 para sempre; 30 dias erra 10 de 12 meses).

### `LocalDate.now()` do servidor (UTC) para decidir "hoje"
Descartada **porque** às 01:30 UTC ainda é o dia anterior em São Paulo: o *job* dispararia "o dia 5" às 22:30 do dia 4. Usar `Clock` injetado e o fuso do agendamento.

## Segurança
- **STRIDE do fluxo novo** (P10.07): *Spoofing* (criar agendamento com sessão roubada → MFA), *Tampering* (mudar valor depois → `PATCH` **não existe**: cancelar e criar outro), *Repudiation* (consentimento em `audit_events`, decisão 242), *Information disclosure* (listar só o do `sub`; 404 no alheio), *DoS* (limite de agendamentos ativos por usuário, valor a decidir com produto), *Elevation* (o *job* roda como **principal do sistema**, mas **só** executa o que o dono consentiu; a política confere o dono a cada execução).
- **Sem dado pessoal** em eventos e métricas (decisão 189; LGPD, S5.05); `amount` não vira etiqueta de métrica.
- Linhas novas no `threats.csv` (P10.07) e na matriz de cobertura da revisão de segurança (S5.07).

## Observabilidade
- **SLO (proposto):** 99,5% das ocorrências processadas em até 60 min do horário devido, 30 dias. **SLI:** `processadas_no_prazo / devidas`, das métricas abaixo.
- Métricas: `payflow_recurring_runs_total{outcome}` (`succeeded`, `failed_funds`, `failed`), `payflow_recurring_lag_seconds` (idade da ocorrência devida mais antiga **não processada**), `payflow_recurring_due` (devidas na janela).
- Alertas: **atraso** > 30 min (página na janela 06:00–08:00); **silêncio** (devidas > 0 e nenhuma processada em 15 min); **duplicidade** (conciliação diária ≠ 0 → SEV1, "dinheiro = SEV1", S4.02).
- *Runbook* "agendador atrasado" (S4.02, verificado pelo `DocLint` da S4) e *trace* do *job* até o `transfer` (o `traceId` volta no evento).

## Custo
- **Nuvem (marginal, preços de novembro de 2026, `sa-east-1`):** o *job* roda nas tarefas existentes (sem computação nova); as tabelas cabem no armazenamento já provisionado do RDS (100 GB na estimativa da S6.04: custo marginal **US$ 0** até esgotar); logs: ~50 mil linhas/mês × ~500 bytes ≈ 0,025 GB × **US$ 0,90/GB** ≈ **US$ 0,02/mês** (premissas).
- **Pessoas (o custo real):** ver `estimativa-recorrentes.md` (S7.03), em dias de trabalho com intervalo, **não** um número único.
- **Custo de oportunidade:** o que sai do *roadmap* para caber isto (conversa da S7.06).

## Plano de entrega
1. **V26** (tabelas novas) no pipeline de migração, antes do código (*expand*, S3.01).
2. Código atrás da *flag* `recurring.enabled` (P8.04), **desligada**; o agendador também lê a *flag* a cada ciclo.
3. **Contas internas** por 2 semanas (conciliação diária = 0 duplicadas; SLO medido); depois 5% → 25% → 100% dos usuários por *hash* estável (P8.04).
4. **Critério para avançar:** 0 duplicadas, SLO cumprido na etapa, nenhum SEV1/SEV2 ligado ao `recurring`.
5. **Reversão:** desligar a *flag* para o agendador e esconder os *endpoints*; nada é apagado; ocorrências devidas e não processadas ficam com `next_due_at` no passado e são processadas **uma vez** quando religar (decidir com produto se ocorrências muito atrasadas são **puladas**: pergunta em aberto). A V26 **não** é revertida (tabelas novas não atrapalham).
6. **Limpeza:** remover a *flag* em até 30 dias depois de 100% (dívida de *flags*, P8.04).

## Riscos
- Disparo duplicado por concorrência ou queda depois do *commit* (dono: tech lead) — `UNIQUE` + chave derivada + teste com 2 nós.
- Agendador parado sem erro (dono: SRE) — alertas de atraso e de silêncio.
- Exceção de negócio deixando a transação *rollback-only* (dono: tech lead) — teste antes de implementar; `REQUIRES_NEW` para o registro da falha.
- Pico das 06:00 disputando o *pool* com a API (dono: SRE) — 2 *workers* fixos; entra na conta de conexões (decisão 181).
- Fraude com agendamento criado por conta tomada (dono: segurança) — MFA, aviso, antifraude por ocorrência.
- Adoção abaixo do esperado (dono: produto) — métrica de agendamentos ativos; revisão em 90 dias.

## Perguntas em aberto
- Ocorrência muito atrasada (ex.: > 24 h por incidente) é executada ou pulada? (dono: produto)
- Limite de valor e de agendamentos ativos por usuário? (dono: produto e segurança)
- Retenção de `recurring_runs` e do consentimento? (dono: jurídico)
- O `transfer` atual devolve a transferência existente para uma chave repetida **com o mesmo corpo** (S1.08)? Confirmar no código antes de estimar (dono: tech lead)
