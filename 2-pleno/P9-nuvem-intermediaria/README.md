# P9 — Nuvem intermediária

> 🔵 Pleno · ⏱️ 13.5h (9 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **parcialmente executado: o Terraform (rede, ECS/Fargate, ALB, SQS/SNS/EventBridge, Secrets Manager, CloudWatch, RDS, Lambda e EKS) contra o moto (sem conta AWS), o AWS SDK for Java (SNS/SQS/Secrets Manager contra o moto), uma replicação assíncrona real entre dois PostgreSQL 18, o cache de DNS da JVM, a JVM sob limites de contêiner e a medição de *cold start* local foram executados de verdade; a AWS real, as tarefas ECS (o moto não executa contêineres), a autoescala, o *circuit breaker*, o Lambda/SnapStart, o EKS, a avaliação real dos alarmes, o RDS (Multi-AZ, *failover*, PITR, Proxy) e todo o PayFlow do aluno NÃO foram; testes práticos no fim do treinamento, por decisão do aluno**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído em `~/docs/payflow`, tag `payflow-P9`).** **Executado:** o laboratório inteiro (`run-all.sh`, ~15 min, numa cópia): Terraform contra o **moto** (testes de módulo e mutantes, s1–s11), `aws-sdk-lab` (inclui o teste que **deve falhar** de propósito, igual), `replica-lab` (replicação PostgreSQL 18 real: leitura imediata na réplica perdeu a escrita em **95,3%**, 286 de 300 — a aula diz 96,0%; espera por LSN: 0 leituras velhas), DNS da JVM, `lambda-lab` (*cold start* local) e `actionlint`/`shellcheck`: **11 saídas idênticas e 7 que diferem só por ids, tempos, o provedor 6.74.0 (a aula cita 6.73.0) e o JDK 25.0.5.1**. **Corrigido nas aulas:** nenhuma divergência de conteúdo; só números de medição (96,0% × 95,3% de leituras perdidas na réplica) e versões. **Ainda não executado:** AWS real (ECS, EKS, RDS Multi-AZ, Lambda/SnapStart, CloudWatch de verdade): o moto só emula a API e **não** executa tarefas, funções nem avalia alarmes; Spring Cloud AWS no PayFlow. Os avisos "não executado" / "revisado por leitura" nas aulas descrevem a escrita original.

## 🎯 Objetivo do módulo
Usar os serviços gerenciados que sustentam o PayFlow em produção na AWS: **rede privada** (VPC de três camadas, *endpoints* no lugar de NAT, **saída restrita**), **ECS/Fargate** atrás de um **ALB** (papéis separados, parada ordenada, *circuit breaker*, autoescala, *deploy* por *digest*), **EKS** como alternativa **decidida e não adotada**, **mensageria gerenciada** (SNS → uma fila SQS por consumidor, DLQ, EventBridge), **Lambda** e o *cold start* de Java (SnapStart), **segredos** (Secrets Manager e Parameter Store, valor *write-only* fora do *state*, rotação), **CloudWatch** (12 alarmes com severidade, *runbook* e ação; a DLQ que a P6 deixou em aberto) e **RDS** (Multi-AZ × réplica de leitura, leitura defasada medida, *failover* e DNS da JVM), com a **tabela de equivalência AWS ↔ Azure ↔ GCP** de todo o módulo.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [P9.01](P9.01-redes-vpc-sub-redes-nat-e-load-balancers.md) | 💸 Redes: VPC, sub-redes, NAT e load balancers | 1h30 |
| [P9.02](P9.02-ecs-e-fargate.md) | 💸 ECS/Fargate | 1h30 |
| [P9.03](P9.03-eks-visao.md) | 💸 EKS (visão) | 1h30 |
| [P9.04](P9.04-sqs-sns-e-eventbridge.md) | 💸 SQS, SNS e EventBridge | 1h30 |
| [P9.05](P9.05-lambda-e-cold-start-em-java.md) | 💸 Lambda e cold start em Java (SnapStart) | 1h30 |
| [P9.06](P9.06-secrets-manager-e-parameter-store.md) | 💸 Secrets Manager e Parameter Store | 1h30 |
| [P9.07](P9.07-cloudwatch.md) | 💸 CloudWatch | 1h30 |
| [P9.08](P9.08-rds-replicas-de-leitura-e-equivalencia-azure-gcp.md) | 💸 RDS, réplicas de leitura e equivalência Azure/GCP | 1h30 |
| [P9.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório em [`lab/`](lab/README.md): `lab/cloud-lab/` (Terraform + `run-cloud.sh`, **executado** contra o moto), `lab/aws-sdk-lab/` (Java + JUnit 6, **executado** contra o moto), `lab/replica-lab/` (PostgreSQL 18 e DNS, **executado**), `lab/lambda-lab/` (*cold start* local, **executado**), `lab/cidr-lab/` (Java puro, **executado**) e `lab/pipeline-lab/` (**`actionlint` e `shellcheck` executados**; GitHub **não**).

> ⚠️ **Aviso de verificação (leia antes de estudar):** **foi executado** (Linux, Docker 29.9.2, Terraform 1.17.1, AWS provider 6.73.0, AWS CLI 2.39.5, moto, AWS SDK for Java 2.59.1, JDK 25.0.5, PostgreSQL 18, Trivy 0.76.2, `actionlint` 1.7.12): as saídas mostradas nas aulas são **as reais**. **O moto é uma emulação de API: não move pacotes, não executa contêineres nem funções, não avalia alarmes nem filtros de log e não impõe políticas**; ele **também travou** em três recursos (`aws_appautoscaling_target`, `aws_sqs_queue_redrive_allow_policy` e a réplica do RDS pelo Terraform), **desligados no laboratório e anotados nas aulas**. **Não foi executado:** a **AWS real** (💸), **nenhuma tarefa ECS**, o *health check* do ALB, o ***circuit breaker*** disparando, a **autoescala**, o **SnapStart**, o **EKS** real, o **RDS** (Multi-AZ, *failover*, PITR, RDS Proxy), a **rotação** de segredos, **KMS**, e **todo o PayFlow do aluno** (revisado por leitura). **Os testes práticos acontecem no fim do treinamento.** A **lista "a validar no fim do treinamento"** está em `00-guia/progresso.md`.

> **Avisos de ordem:** a P9.01 cumpre a promessa da P8.07 (a exceção **AWS-0104**) e usa `cidrsubnet` (P8.05); a P9.02 retoma a JVM em contêiner (P7.02), as sondas (P7.05), o HPA (P7.06) e o *deploy* por *digest* (P8.03); a P9.03 retoma **toda a P7** (Kubernetes **não executado**) e o `aws eks update-kubeconfig` que a P8 deixou como *placeholder*; a P9.04 retoma P3.03–P3.09 (fila, tópico, idempotência, DLQ, Outbox); a P9.05 usa `L = λ × W` (P5.08) e JIT/aquecimento (P1.02); a P9.06 retoma J8.05, J10.06, P7.04 e P8.05 (o *state* guarda tudo); a P9.07 retoma os 9 alertas da P6.04 e **fecha o da DLQ**; a P9.08 retoma P2.01–P2.06 e P2.08. **Keycloak, OAuth2, gestão formal de rotação, KMS e assinatura de imagens são da P10** (apontados, **não** usados como pré-requisito).

## 🏗️ Entrega no PayFlow
**Rede** (VPC `10.20.0.0/16`, 3 camadas × 2 AZs, 6 *endpoints*, saída em 3 destinos, `check-network.sh`), **ECS/Fargate** (0,5 vCPU / 1 GiB, ARM64, 2–6 tarefas, ALB, *circuit breaker*, `ecs-deploy.sh` por *digest*, `deploy-ecs.yml`), **mensageria** (SNS → 2 filas + 2 DLQ, EventBridge, `SnsEventPublisher` e consumidor idempotente), **segredos** (Secrets Manager + Parameter Store, valor *write-only*), **CloudWatch** (12 alarmes), **RDS** (Multi-AZ em produção, réplica **só** para `reporting`), módulos Terraform com `tests/` e **mutantes**, `docs/{network,ecs,messaging,secrets,observability-aws,database}.md` e **ADR 0021**. Tag `payflow-P9`. **Nenhuma migração nova no monólito** (a próxima livre continua V12).

## ➡️ Próximo
[P10 — Segurança intermediária](../P10-seguranca-intermediaria/README.md)
