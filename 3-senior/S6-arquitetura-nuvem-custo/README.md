# S6 — Arquitetura em nuvem e custo

> 🟣 Sênior · ⏱️ 10.5h (7 sessões de 1h30) · Status: ✅ concluído (2026-11-23) — **laboratório executado (Java puro com preços reais da AWS Price List: folga de AZ e Multi-AZ com preço, *linter* de revisão Well-Architected, Lambda × Fargate × EC2, varredura de SDK e gravidade dos dados, estimativa do PayFlow, NAT × *endpoints*, tags, compromissos, Spot, modelo de SCPs com casos e mutantes, fatura sintética com detectores; `terraform validate` com mutante; classes de exemplo compiladas contra o AWS SDK v2); nada criado na AWS (Organizations, Control Tower, Identity Center, Budgets, Anomaly Detection, Well-Architected Tool, Savings Plans), sem fatura real, Azure/GCP e o PayFlow do aluno NÃO executados; testes práticos no fim do treinamento, por decisão do aluno**

## 🎯 Objetivo do módulo
**Escolher arquitetura em nuvem pesando risco, desempenho e custo, com número**: conduzir uma **revisão Well-Architected** (6 pilares e as **Cloud Foundations** de 16/11/2026) que sai com **riscos com dono e prazo**; **quantificar *trade-offs*** (folga de AZ, Multi-AZ, região) com **preços datados**; escolher **VM, contêiner ou *serverless*** pelo **cruzamento de custo** e pela **espera de I/O**; **gerenciar o *lock-in*** (SDK só em adaptadores, dados abertos, plano de saída) e recusar multi-cloud ativo/ativo **sem motivo próprio**; praticar **FinOps** (estimativa reproduzível, custo por unidade, tags que valem, orçamentos e anomalias **por serviço**, compromissos **no piso**, Spot **com *checkpoint***); montar uma ***landing zone*** com contas por ambiente e **SCPs testadas com mutantes**; e **investigar uma conta que triplicou** até cada causa virar **mecanismo**. O fio continua o da S2–S5: **medir antes de afirmar**, **verificações que podem falhar** e **dizer o que não foi coberto**.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [S6.01](S6.01-well-architected-framework-e-seus-pilares.md) | Well-Architected Framework e seus pilares | 1h30 |
| [S6.02](S6.02-vms-vs-containers-vs-serverless.md) | VMs vs containers vs serverless | 1h30 |
| [S6.03](S6.03-multi-cloud-e-lock-in.md) | Multi-cloud e lock-in | 1h30 |
| [S6.04](S6.04-finops-estimativa-tags-reservas-e-spot.md) | FinOps: estimativa, tags, reservas e spot | 1h30 |
| [S6.05](S6.05-landing-zones-e-contas-por-ambiente.md) | Landing zones e contas por ambiente | 1h30 |
| [S6.06](S6.06-estudo-de-caso-a-conta-de-nuvem-que-triplicou.md) | Estudo de caso: a conta de nuvem que triplicou | 1h30 |
| [S6.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega, autoavaliação e simulado) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): **`fetch-prices.py`** (Price List Bulk API → `prices.csv` datado), **6 programas de Java puro** (`WellArchitectedLab`, `ComputeLab`, `LockInLab`, `FinOpsLab`, `LandingZoneLab`, `BillLab`, mais o `PriceBook` compartilhado), as amostras (revisão, BOM, inventário de recursos, árvores de código, **8 SCPs em JSON**, fatura sintética), os módulos **`terraform/finops`** e **`terraform/organization`**, o `run-all.sh` e as **18 saídas reais** (`out-s60*.txt`).

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório foi executado de verdade** (JDK **25.0.5.1**, Linux) e **as saídas das aulas são as reais**; elas são **determinísticas** (sementes fixas). **Os preços são reais** (AWS Price List, ofertas publicadas de **08 a 23/11/2026**; Spot Instance Advisor lido em 23/11/2026) e **mudam**: refaça com `fetch-prices.py`. **As quantidades são PREMISSAS** (demanda por hora, tráfego, tamanho de log, GB processados, tamanho do banco) e **a fatura do estudo de caso é SINTÉTICA** (causas plantadas). **A avaliação de SCP é um MODELO** da lógica documentada, não a AWS. **Não executado:** **nada na AWS** (Organizations, Control Tower, IAM Identity Center, Budgets, Cost Anomaly Detection, Well-Architected Tool, compra de *Savings Plans*), **fatura real** (CUR 2.0/FOCUS), **Azure e Google Cloud** (só documentação), **o PayFlow do aluno**. **Latência entre regiões NÃO foi medida**; **exigências regulatórias sobre região NÃO foram conferidas**. **Decisões do PayFlow são PROPOSTAS.**

> **Avisos de ordem:** a S6.01 **precifica** as opções A/B/C da S4.06 (e acha o erro da contagem ímpar) e o Multi-AZ da decisão 171; a S6.02 **confirma com números** as decisões 165 (Fargate ARM) e 168 (Lambda só para *jobs*); a S6.03 **reaproveita** as portas e adaptadores da P4 e a porta `EventPublisher` da P9.04; a S6.04 **fecha a conta NAT × *endpoints*** que a P9.01 deixou aberta e **estende** o alerta de custo da J10.04; a S6.05 **cumpre** a conta de *backup* separada da S4.05/S5.03 e a separação por contas citada na P9.03; a S6.06 usa o formato de postmortem da S4.03 e os *runbooks* da S4.02.

## 🏗️ Entrega no PayFlow
**Tag `payflow-S6`** (por leitura): **revisão Well-Architected** em CSV com *linter* no CI (4 HIGH com dono), **tabela "onde roda cada componente"** com custo e gatilhos, **teste de arquitetura do SDK** e **plano de saída**, **estimativa** (BOM + preços datados + premissas; custo por 1.000 transferências), **política de tags** (`default_tags`, SCP na criação, *linter*, rateio), **orçamento e anomalia por conta e por serviço** (Terraform validado), **decisão de não comprar compromisso agora**, **agenda do *staging***, ***landing zone*** (7 contas, 7 OUs, 8 SCPs testadas com 17 casos e 5 mutantes; Terraform validado), ***runbook* e postmortem de custo** e a **ADR 0029**. **Nada foi aplicado em conta AWS nem compilado no PayFlow.**

> **Migrações:** **nenhuma** (a **V26** continua livre; V18–V24 reservadas pela S3; V25 usada pela S5). O contrato HTTP `/api/v1` **não muda**.

## ➡️ Próximo
[S7 — Liderança técnica](../S7-lideranca-tecnica/README.md)
