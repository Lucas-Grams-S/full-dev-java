# ADR 0001 — `ledger_entries.transfer_id` é opcional

- **Status:** aceita (J3, 2026-11-12)
- **Contexto:** `00-guia/projeto-central.md` define `LedgerEntry` com `transfer_id` ligado a `Transfer` e o diagrama original desenha `TRANSFERS ||--|{ LEDGER_ENTRIES` (cada lançamento pertence a **uma** transferência). Mas o PayFlow tem **depósitos** (J5, `payflow-J5`): entrada de dinheiro na conta que **não** é uma transferência entre contas e que, ainda assim, precisa virar lançamento imutável para que a regra 3 (saldo = soma dos lançamentos) valha.
- **Opções:**
  1. `transfer_id` **nulo** em depósitos (e, no futuro, em outros movimentos sem transferência).
  2. Uma "conta do sistema" que representa o mundo externo e uma transferência de verdade para cada depósito (partidas dobradas completas).
  3. Uma tabela separada `deposits`, com os lançamentos referenciando ou `transfers` ou `deposits` (duas FKs opcionais, exatamente uma preenchida).
- **Decisão:** opção 1. `ledger_entries.transfer_id UUID NULL` com `FOREIGN KEY` para `transfers (id)`. O diagrama passa a usar `TRANSFERS |o--o{ LEDGER_ENTRIES` (zero ou uma).
- **Consequências:**
  - (+) Simples; cabe no esquema do projeto sem tabelas novas; depósitos aparecem no extrato (com contraparte em branco, J3.06).
  - (−) Menos rigor contábil que as partidas dobradas: um depósito tem só o lado "crédito", então a soma global de créditos e débitos **não** zera (ela é igual ao total depositado).
  - (−) Consultas precisam tratar `NULL` (`LEFT JOIN`, `IS NULL`; J3.05/J3.06).
  - Revisitar se o negócio exigir contabilidade de partidas dobradas (opção 2), ou em S1 (CQRS do extrato).
