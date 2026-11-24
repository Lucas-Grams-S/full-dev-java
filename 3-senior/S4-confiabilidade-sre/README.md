# S4 — Confiabilidade e operação (SRE)

> 🟣 Sênior · ⏱️ 12h (8 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **laboratório executado (Java puro: orçamento e *burn rate*, experimento de caos em miniatura, verificação de *runbooks*/postmortems, modelos de RPO/RTO, de perda de AZ e de capacidade; `promtool` sobre as regras de SLO e de capacidade, com mutantes); AWS (FIS, RDS/PITR, AWS Backup, cópia entre regiões, Route 53), Alertmanager e plantão, teste de carga e qualquer incidente real NÃO executados; o PayFlow do aluno foi revisado apenas por leitura; testes práticos no fim do treinamento, por decisão do aluno**

## 🎯 Objetivo do módulo
**Operar com metas de confiabilidade e aprender com as falhas**: transformar "confiável" em **SLOs** e **orçamento de erro** com **alertas por taxa de queima**; **conduzir incidentes** com papéis, severidades e ***runbooks* verificados por máquina**; escrever **postmortems sem culpa** com ações verificáveis; **provar a resiliência** com **experimentos de caos** (hipótese, raio, condição de parada); definir **RPO/RTO** e **ensaiar a restauração** (inclusive **o que fica fora do banco**); **dimensionar Multi-AZ** (a conta do N+1 e a das conexões) e decidir **multi-região por gatilho e custo**; e fazer **capacity planning** com a **curva de utilização × latência**, **Little** e **previsão com janela consciente**. O fio é **medir e provar que cada verificação pode falhar** (mutantes, casos negativos): **todo número do PayFlow é "a medir"**.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [S4.01](S4.01-sli-slo-e-error-budgets.md) | SLI, SLO e error budgets | 1h30 |
| [S4.02](S4.02-gestao-de-incidentes-e-runbooks.md) | Gestão de incidentes e runbooks | 1h30 |
| [S4.03](S4.03-postmortems-sem-culpa.md) | Postmortems sem culpa | 1h30 |
| [S4.04](S4.04-engenharia-do-caos.md) | Engenharia do caos | 1h30 |
| [S4.05](S4.05-backup-rpo-e-rto.md) | Backup, RPO e RTO | 1h30 |
| [S4.06](S4.06-multi-az-e-multi-regiao.md) | Multi-AZ e multi-região | 1h30 |
| [S4.07](S4.07-capacity-planning.md) | Capacity planning | 1h30 |
| [S4.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega, autoavaliação e simulado) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): **7 programas de Java puro** (`SloLab`, `DocLint`, `ChaosLab`, `RpoRtoLab`, `AzLab`, `CapacityLab` + `check-runbooks.sh`), **as regras Prometheus** (`prometheus/slo-rules.yml`, `capacity-rules.yml` e seus testes), o `run-all.sh`/`run-promtool.sh` e as **saídas reais** (`out-s40*.txt`).

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório foi executado de verdade** (JDK **25.0.5**, Linux, **12 núcleos, máquina compartilhada**; `promtool` da imagem `prom/prometheus`) e **as saídas das aulas são as reais**; os **milissegundos mudam a cada execução** (as aulas citam **ordens de grandeza**). **São modelos e programas pequenos, não o PayFlow:** o `ChaosLab` usa **dois servidores HTTP locais**; os modelos de RPO/RTO, de AZ e de capacidade usam **premissas declaradas** (5 req/s, `max_connections = 100`, 120 GB livres etc.). **Não foi executado:** **AWS** (FIS, RDS/PITR, AWS Backup, Route 53, ECS/ALB), **Alertmanager**/plantão, **teste de carga** do PayFlow, o **PayFlow do aluno**. Comandos `aws rds …` foram **conferidos na ajuda da AWS CLI 2.39.5** (não executados); fatos da AWS (ECS, ALB, FIS, Backup Vault Lock, PITR) foram **conferidos na documentação**. **SLOs, RPO/RTO, severidades, prazos, retenção e catálogos de caos são PROPOSTAS**; **os cenários de incidente e de postmortem são SINTÉTICOS** e estão rotulados.

> **Avisos de ordem:** a S4.01 **cumpre** a promessa da P6.04/decisão 141 ("SLO e *burn rate* = S4") e **substitui** o limiar fixo "5xx > 2%" **como indicador de orçamento** (o alerta da P6.04 **continua** como alerta de sintoma); a S4.02 **cumpre** "um *runbook* por alerta" (decisão 146) com verificação por máquina; a S4.03 **estende** a P3.13/P6.07 (postmortem) com gatilhos e ações verificáveis; a S4.04 **ensaia** os mecanismos da P5 (*timeout*, *breaker*) e da S4.02; a S4.05 **usa** a S3.04 (arquivar ≠ *backup*) e a P9.08 (PITR); a S4.06 **corrige a conta** do Multi-AZ da P9.01/P9.08 (2 AZs a 60% = 120% ao perder uma) e **reforça** as decisões 171/181; a S4.07 **calibra** o `max_slot_wal_keep_size` (S3.06) e usa a Lei de Little da S4.04.

## 🏗️ Entrega no PayFlow
**Tag `payflow-S4`** (por leitura): **`slo-rules.yml`** e **`capacity-rules.yml`** com testes (`promtool`), **`docs/slo/*`** (SLOs propostos e política de *error budget*), **`docs/incidentes/*`** (processo, critérios e postmortem), **`docs/runbooks/*`** (11 + perda de AZ + disco), **`docs/caos/*`** (catálogo E1–E6 e registro), **`docs/continuidade/*`** (*backup*/RPO/RTO, ensaio de restauração, Multi-AZ, DR regional), **`docs/capacidade/modelo.md`**, **`DocLint`** + **`check-runbooks.sh`** no CI e a **ADR 0027**. **Nada foi compilado nem executado no PayFlow.**

> **Migrações:** **nenhuma** (a **V25** continua livre); o contrato HTTP `/api/v1` **não muda**.

## ➡️ Próximo
[S5 — Segurança avançada](../S5-seguranca-avancada/README.md)
