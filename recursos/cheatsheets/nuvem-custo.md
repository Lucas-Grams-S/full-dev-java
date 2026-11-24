# Cheatsheet: arquitetura em nuvem e custo (S6)

> Preços de **novembro de 2026** (AWS Price List, ofertas publicadas de 08 a 23/11/2026). **Mudam**: refaça com `3-senior/S6-arquitetura-nuvem-custo/lab/fetch-prices.py`. Quantidades do PayFlow são **premissas**.

## Well-Architected (16/11/2026)
- **6 pilares:** excelência operacional · segurança · confiabilidade · eficiência de desempenho · otimização de custos · sustentabilidade.
- **Cloud Foundations (FND01–07):** contas/OUs · identidade e *guardrails* · observabilidade central · tags e inventário · segurança e conformidade · **gestão financeira** · rede. "*Blueprint* de *landing zone*".
- **Riscos:** HRI (prática fundamental) · MRI (habilitadora) · baixo = progresso. **Todo risco: dono + prazo + ação.**
- ***Trade-off*:** custo mensal (preço **datado** + região) + métrica de sucesso + data de revisão.
- Azure: **5** pilares; Google: **6** ("segurança, privacidade e conformidade", "otimização de desempenho").

## Contas rápidas
| Conta | Fórmula |
|---|---|
| Mês | `730 h` (convenção das páginas de preço) |
| Tarefa Fargate | `vCPU × preço_vCPU_h + GB × preço_GB_h` (ARM é 20% mais barato que x86) |
| Lambda | `requisições × (preço_req + memória_GB × duração_s × preço_GB_s)`; duração arredonda a **1 ms**; **1.769 MB = 1 vCPU** |
| Concorrência do Lambda | `L = λ × W` (um ambiente por requisição simultânea) |
| Multi-AZ RDS | ~**2×** a linha (instância **e** armazenamento) |
| Paga-se se | `custo_extra_ano ÷ (eventos/ano × horas_evitadas) < custo da hora parada` |
| NAT × *endpoints* (2 AZs) | equilíbrio ≈ `(12 × h_endpoint − 2 × h_NAT) × 730 ÷ (GB_NAT − GB_endpoint)` → **~580 GB/mês** (`sa-east-1`), **~626** (`us-east-1`) |
| Entre AZs | **US$ 0,01/GB em cada sentido** |
| Saída entre regiões | `sa-east-1 → us-east-1` **0,138/GB** · `us-east-1 → sa-east-1` **0,02/GB** |
| Custo unitário | `custo do mês ÷ (transferências ÷ 1.000)` |
| Reserva se paga após | `meses_do_prazo × preço_reservado ÷ preço_sob_demanda` (RDS m7g.large Multi-AZ 1a: **9,2 meses**) |

## PayFlow (estimativa da S6.04)
- **US$ 887,89** (`us-east-1`) · **US$ 1.424,28** (`sa-east-1`, ×1,60) por mês; **71% fixo**; tráfego ×2 → ×1,29.
- Maiores linhas: RDS Multi-AZ · 6 *endpoints* × 2 AZs · réplica do *reporting* · banco do *staging*. Computação = **12%**.
- Cruzamento Lambda × Fargate (modelo): **~10 req/s** (`us-east-1`) · **~17** (`sa-east-1`).

## FinOps
- Fases **Informar → Otimizar → Operar**; *showback* antes de *chargeback*.
- Tags: `app`, `env` (`prod|staging|study|shared`), `owner`, `cost-center` (`CC-NNNN`), `data-class`; **minúsculas** (sensível a maiúsculas); ativação **24 h + 24 h**.
- *Tag policy* padroniza; **SCP com `Null` em `aws:RequestTag/...`** exige na criação.
- Orçamento por conta: **previsão 80%** + **real 100%** (atualiza até 3×/dia). Anomalia **por serviço** (o total esconde o próximo salto).
- **Compromisso só no piso** e depois de **3 meses** de uso real. Spot só com ***checkpoint*** (aviso de 2 min).

## SCP em uma tela
- Define o **máximo**; **nunca concede**; **Allow em todos os níveis** (raiz → OUs → conta); **Deny em qualquer nível vence**.
- **Não vale:** conta de **gestão**, papéis **vinculados a serviço**. **Vale:** *root* das contas-membro.
- **1 a 10 SCPs** por raiz/OU/conta; até **10.240** caracteres. Tirar a `FullAWSAccess` = negar tudo abaixo.
- Negar regiões: **`NotAction`** para `cloudfront:*`, `iam:*`, `route53:*`, `support:*` (serviços globais em `us-east-1`).
- Chave ausente: `StringEquals` → falso; `StringNotLike`/`ArnNotLike` → verdadeiro.
- Teste: casos (ALLOW/DENY + motivo) **e mutantes**; aplicar antes numa OU de **Policy Staging**.

## Comandos
```bash
cd 3-senior/S6-arquitetura-nuvem-custo/lab && export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
python3 -I fetch-prices.py --cache ~/.cache/s6-pricing          # preços de hoje → prices.csv
$JAVA_HOME/bin/java FinOpsLab.java estimate samples/bom-payflow.csv
$JAVA_HOME/bin/java FinOpsLab.java nat
$JAVA_HOME/bin/java WellArchitectedLab.java review docs.csv 2026-11-23
$JAVA_HOME/bin/java LandingZoneLab.java                         # 17 casos + mutantes
(cd terraform/finops && terraform init -backend=false && terraform validate)
```

## Equivalências
| AWS | Azure | Google Cloud |
|---|---|---|
| Organizations / OU / conta / SCP | *management groups* / assinatura / Azure Policy | organização / pasta / projeto / Organization Policy |
| Control Tower | Azure landing zones (CAF) | *landing zones* (Architecture Center) |
| Savings Plans / Reserved Instances | savings plan for compute / Reservations | CUDs (+ SUDs automáticos) |
| Spot (2 min) | Spot VMs (30 s) | Spot VMs (aviso configurável) |
| Budgets / tags | Cost Management budgets / tags | budgets (alerta e teto onde suportado) / *labels* |
| EC2 / ECS-Fargate / EKS / Lambda | VMs / Container Apps / AKS / Functions | Compute Engine / Cloud Run / GKE / Cloud Run functions |
