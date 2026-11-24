# ADR 0031 — Transferências recorrentes como módulo `recurring` do monólito (não como serviço)

- **Status:** proposta (S7, 2026-11-23): resultado esperado da RFC-0001, **em revisão até 2026-12-04**; vira aceita ou rejeitada quando o decisor registrar a decisão na RFC. Evidência: *spike* `RecurrenceSpike` (datas, fusos, pico por premissas, chaves) e estimativa `EstimateLab` (premissas).
- **Contexto:** o produto quer transferências mensais agendadas. O simulado do Pleno desenhou um `recurring-service` separado. Desde então o PayFlow ganhou o monólito modular (ADR 0016), idempotência ponta a ponta (ADR 0024), SLOs (ADR 0027) e a política de autorização com MFA para mover dinheiro (ADR 0028).
- **Opções:** (A) não fazer (lembrete mensal); (B) serviço separado com Client Credentials; (C) módulo `recurring` no monólito; (D) um agendamento por usuário no EventBridge Scheduler.
- **Decisão:** (C), se a RFC for aceita. O módulo é dono de `recurring_transfers` e `recurring_runs` (**V26 reservada**, só tabelas novas), chama a API pública do `transfer` numa transação por ocorrência, com `UNIQUE (schedule_id, due_date)` e chave `recurring:<schedule_id>:<due_date>`; regra de datas pelo `YearMonth` com `Clock` e fuso do agendamento; no máximo 2 *workers*; MFA e consentimento em `audit_events` na criação; *flag* `recurring.enabled`. Decisão 262.
- **Consequências:**
  - (+) "Ocorrência + transferência" numa transação ACID; nenhum segredo novo que mova dinheiro; reaproveita política, Outbox e observabilidade.
  - (−) O *job* disputa o *pool* com a API (limitado a 2 *workers*, entra na conta de conexões); extrair depois custa uma migração de dados.
- **Revisar quando:** o disparo competir com a API mesmo com o limite; outro time assumir o produto; a v2 pedir destino fora do PayFlow (Pix).
