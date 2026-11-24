# ADR 0032 — Modernização do extrato: `/entries` → `/statement` por abstração, leitura em paralelo por versão e remoção com critério numérico

- **Status:** aceita (S7, 2026-11-23); evidência: `StranglerLab` (modelo: comparação ingênua com 11,6% de falsos alarmes no projetor correto; dois mutantes do projetor achados em menos de 50 leituras) e `HotspotLab` (método de hotspots num histórico real). **Nada executado no PayFlow.**
- **Contexto:** o extrato antigo (`GET /api/v1/accounts/{id}/entries`, *offset* sobre o ledger, ADR 0008) convive com o novo (`/statement`, CQRS da ADR 0024). Há proposta de reescrever o módulo num *branch* de 2 meses.
- **Opções:** (A) reescrita em *branch* longo e troca num dia; (B) manter os dois caminhos; (C) abstração `StatementReader`, leitura em paralelo amostrada e comparada **na mesma versão** (`seq`), *rollout* por *flag*, `Deprecation`/`Sunset` no `/entries` com métrica de uso, e remoção.
- **Decisão:** (C). Desligar o caminho antigo quando: **0** divergências do tipo BUG em **≥ 30 mil** leituras em paralelo (regra de três: taxa < 0,01% com 95% de confiança), *fallback* ao ledger abaixo do limiar acordado e uso do `/entries` zero por 30 dias depois do `Sunset`. A abstração e o comparador são **arquitetura de transição**, com data de remoção no registro de dívida. Decisão 266.
- **Consequências:**
  - (+) Cada passo entrega valor e é reversível; a troca tem critério numérico, não de calendário.
  - (−) Leitura em paralelo dobra a carga de leitura na amostra; dois caminhos convivem por semanas.
- **Revisar quando:** a amostra de leitura em paralelo pressionar o *pool*; um BUG aparecer (parar o *rollout* e corrigir o projetor); o `Sunset` vencer.
