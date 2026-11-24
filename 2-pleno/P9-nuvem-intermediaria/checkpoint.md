# P9.CP — Checkpoint do módulo P9

> ⏱️ ~1h30 · 🔵 Pleno · Pré-requisitos: P9.01 a P9.08

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** nesta geração (Linux, Docker 29.9.2, Terraform 1.17.1, AWS provider 6.73.0, AWS CLI 2.39.5, moto, AWS SDK for Java 2.59.1, JDK 25.0.5, PostgreSQL 18.6, Trivy 0.76.2, `actionlint` 1.7.12): (1) o **Terraform** do PayFlow contra o **moto** (**96 recursos**: rede, ALB, ECS/Fargate, SNS/SQS/EventBridge, Secrets Manager/Parameter Store, 12 alarmes, RDS, Lambda; `apply`, 2º `plan`, `destroy` e conferência de "o que sobrou"), **5 módulos com `terraform test`** (22 casos) e **3 mutantes** que reprovam, e o **EKS** só como forma; (2) o **AWS SDK for Java** contra o moto: **5 testes** (fan-out SNS → SQS com filtro, mensagem veneno → DLQ, duplicata por *visibility timeout*, FIFO, cache de segredo com rotação) e **2 mutantes**; (3) a **prova no *state***: segredo com 0 ocorrências (*write-only*) × 2 (jeito óbvio); (4) uma **replicação assíncrona real** entre dois PostgreSQL 18 e o **cache de DNS da JVM** (30 s × 5 s); (5) a **JVM sob limites** de contêiner (718 MiB de *heap* em 1 GiB; Serial × G1) e uma **medição de *cold start* local** (344 → 170 → 108 ms); (6) `CidrLab` (Java puro), `actionlint`, `shellcheck`, `ecs-deploy.sh` contra o moto. **NÃO foram executados:** a **AWS real** (💸), **nenhuma tarefa ECS** (o moto não executa contêineres), *health check* do ALB, ***circuit breaker*** disparando, **autoescala**, **SnapStart**, **EKS** real, **RDS real** (Multi-AZ, *failover*, PITR, Proxy), **rotação** de segredos, **avaliação real de alarmes e de filtros de log**, o **GitHub Actions** e **todo o PayFlow do aluno** (revisado por leitura).

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. O que torna uma sub-rede "pública"? Como você **provou** que as de aplicação e dados do PayFlow não são? O que o `check-network.sh` acusou quando alguém "consertou" um *deploy* pelo console?
2. O PayFlow **não tem NAT**. Quais são os **três destinos** de saída da aplicação e **por que a AWS-0104 sumiu**? Que custo e que obrigação cada serviço AWS novo traz?
3. Diferencie ***task role*** de ***execution role*** no ECS e diga **o que cada um do PayFlow pode fazer**. Por que `desired_count` e `task_definition` ficam com `ignore_changes`?
4. O que o ***deployment circuit breaker*** do ECS pega e o que **não** pega? Compare com o ***canary* por versão** da P8.03.
5. **ECS ou EKS** para o PayFlow? Dê os **números** (custo fixo do EKS na página de preços) e os **quatro critérios** para reavaliar.
6. Descreva o ***fan-out* SNS → SQS** do PayFlow. Para que servem `visibility_timeout`, `maxReceiveCount`, a **retenção da DLQ** e a **entrega crua**? O que acontece com uma mensagem **sem** o atributo `eventType`?
7. SQS padrão entrega **exatamente uma vez**? Mostre **como o laboratório provou** a duplicata e a proteção, e **por que o consumidor apaga depois** de processar.
8. Do que é feito um ***cold start* de Java**? Use a **medição** (padrão, AppCDS, AOT) e diga **o que ela não prova**. Por que a API do PayFlow **não** vai para Lambda e o exportador mensal **sim**?
9. Como o PayFlow guarda e entrega segredos? Mostre a **prova** de que o valor **não** está no *state*. O que acontece com as tarefas quando o RDS **rotaciona** a senha mestra?
10. **Réplica de leitura**: o que a **medição** mostrou sobre a leitura imediata? O que lê da réplica e o que **nunca**? Qual a conta do ***pool* durante o *deploy*** e o que a P9.07 faz com a **DLQ** que a P6 deixou em aberto?

<details><summary>Gabarito</summary>

1. Uma **rota** `0.0.0.0/0` para o **Internet Gateway** na tabela de rotas da sub-rede (o nome não importa). Provado pela **rede real** (`aws ec2 describe-route-tables`): só a tabela **pública** tem rota para o IGW; as de aplicação e dados têm a rota **local** e o prefixo do S3. Depois da rota criada à mão, o `check-network.sh` imprimiu `VIOLATION: app subnet ... has a route to the internet gateway` e saiu com **código 1**; ao remover, voltou a `ok`.
2. **Os *endpoints* de interface** (SG deles: `ecr.api`, `ecr.dkr`, `logs`, `secretsmanager`, `sqs`, `sns`), **o S3** (lista de prefixos do *endpoint* de gateway) e **o banco** (SG dele, 5432). A **AWS-0104** (SG com saída irrestrita) deixou de existir porque **não há mais `0.0.0.0/0`** na saída. Cada serviço novo exige **um *endpoint* novo** (ou a saída volta a ser aberta) e *endpoints* de interface **cobram por hora e por AZ** (+ por GB); o gateway do S3 é **grátis**.
3. O ***execution role*** é do **agente**: ECR + logs + **ler os dois segredos injetados** (`GetSecretValue` nos ARNs exatos). O ***task role*** é do **código**: `sns:Publish` em **1 tópico** e 5 ações de consumo (**2 filas**); **nada de segredo**. `desired_count`: o **autoscaler** é o dono (um `apply` o resetaria); `task_definition`: Terraform cria a revisão 1 e o **pipeline** (`ecs-deploy.sh`, por *digest*) as seguintes; **um dono por coisa**.
4. Pega **tarefas que não ficam saudáveis** (não sobem / reprovam nos *health checks*): interrompe e **volta** à revisão anterior. **Não** pega uma versão que **passa** no *health check* e **erra** 30% das respostas (a P8.03 mediu **364 falhas em 4.000** com o *rolling* concluindo): isso é do ***canary* por versão** e do alarme de 5xx. ⚠️ O *circuit breaker* disparando **não foi executado**.
5. **ECS/Fargate agora**: sem custo de orquestração (a página do ECS: "sem cobrança adicional pela orquestração"); EKS cobra **US$ 0,10 por cluster por hora** (≈ **US$ 72/mês**: 0,10 × 24 × 30) **só pelo plano de controle**, mais atualização de versão a cada ~14 meses (ou US$ 0,60/h no suporte estendido). **Reavaliar** com: (1) **≥ 3 serviços** que precisem de malha/operadores, (2) requisito de **portabilidade**, (3) **equipe de plataforma** com Kubernetes, (4) ferramentas **só** como operadores. O `deploy.yml` (Helm) da P8 vale **se e quando** o EKS entrar.
6. **Um** tópico (`payflow-transfers`) e **uma fila por consumidor** (notificações, antifraude), cada uma com **DLQ**; o relé do Outbox publica por `SnsEventPublisher` com `eventType`/`aggregateId` como atributos; **filtro** por `eventType`; **entrega crua** (o corpo é só o evento; máx. 10 atributos). `visibility_timeout` (60 s) > **pior caso** de processamento; `maxReceiveCount` (5) = quantas vezes pode ser **recebida** antes da DLQ; DLQ com **14 dias** e a retenção **conta do envio original**. Sem `eventType` o filtro **descarta em silêncio** (medido: as filas ficaram em 3 e 2).
7. **Não**: *at-least-once* e pode **reordenar**. `VisibilityTimeoutTest`: o consumidor A recebe e "morre"; invisível; depois do *timeout* o B recebe a **mesma** mensagem (`receiveCount = 2`); o `IdempotentHandler` executa **uma vez** (`effects=[transfer-42]`), um consumidor ingênuo **duas**; o **mutante** sem a checagem de id **reprovou** o teste. Apaga **depois** para que a mensagem **reapareça** se a JVM cair; o **mutante 2** (apagar mesmo quando o *handler* falha) **reprovou** `PoisonMessageTest` (o veneno nunca chegava à DLQ).
8. **Init** (JVM + carregar classes + construir o cliente do SDK) + **primeira chamada**; a **mesma chamada aquecida levou ~4 ms**. Medido (mediana de 7): padrão **344 ms**, `C1` só **297**, sem CDS **380**, **AppCDS 170** (~2×), **AOT 108** (~3×). **Não prova** tempos de Lambda (é uma JVM local) **nem** do Spring Boot (**não medido**). A API fica no Fargate (cold start de segundos, *pool* de conexões × concorrência, JIT aquecido); o exportador (1× por mês, tolera latência, idempotente) é o **candidato** (decisão 168).
9. **Secrets Manager** para credenciais (senha mestra do RDS **gerenciada pelo RDS**; chave JWT) e **Parameter Store padrão** (grátis) para configuração; o ECS injeta por **`valueFrom` com ARN** (`...:password::`), **como variável de ambiente** (exceção à regra "arquivo montado", com mitigações). **Prova:** `ephemeral "random_password"` + `secret_string_wo`: **0** ocorrências do valor no `terraform.tfstate` e no plano; o jeito óbvio: **2**. **Rotação:** o ECS injeta **só na partida**; o RDS **muda a senha no banco**; as **conexões novas** das tarefas velhas **falham** (*authentication failed*) enquanto as abertas seguem: **forçar novo *deployment*** na rotação (decisão A), recarregar ao falhar (B), ou usuário de aplicação (C, P10.05).
10. **Medido (PostgreSQL 18 real):** a leitura **imediata** na réplica **perdeu a escrita em 288 de 300 vezes (96%)** com **~3 ms** de atraso médio; esperando a **posição do WAL**: **0** defasadas; com atraso simulado de 2 s, **5 de 5** perdidas e espera de ~2 s. **Da réplica:** só relatórios que toleram atraso (`reporting`); **do primário, sempre:** saldo, transferência e o que o usuário **acabou de fazer**. **Pool no *deploy*:** 6 tarefas × `maximum_percent` 200 = **12 × 10 = 120** conexões > `max_connections` de exemplo (100): limitar o excedente ou o *pool*. **DLQ:** o alarme `dlq-<fila>-not-empty` (`ApproximateNumberOfMessagesVisible > 0`, página) **fecha a pendência da P6.04**, junto do de **idade da fila**.
</details>

## Parte 2: Entrega prática (tag `payflow-P9`)
O `payflow` ganha **rede privada**, **ECS/Fargate** atrás de um ALB, **mensageria gerenciada**, **segredos gerenciados**, **alarmes** e **RDS** de produção, **sem** mudar o contrato HTTP `/api/v1`, o esquema (**nenhuma migração**) nem o dinheiro (uma transação).
```
payflow/
├── deploy/terraform/
│   ├── modules/network/          VPC 10.20.0.0/16 · 3 camadas × 2 AZs · 6 endpoints + S3 · SGs por referência · tests/        (P9.01)
│   ├── modules/alb/              ALB · health check :8081/actuator/health/readiness · deregistration 30 s                      (P9.01–02)
│   ├── modules/ecs_service/      cluster · task def (ARM64, 512/1024) · roles · circuit breaker · autoscaling · tests/          (P9.02)
│   ├── modules/messaging/        SNS · 2 filas + 2 DLQ · EventBridge · policies · validation · tests/                          (P9.04)
│   ├── modules/secrets/          secret_string_wo + ephemeral · Parameter Store                                                (P9.06)
│   ├── modules/observability/    12 alarmes (severidade + runbook + ação) · filtro de log · dashboard · tests/                 (P9.07)
│   ├── modules/database/         RDS (production: proteção + snapshot + Multi-AZ) · réplica opcional · tests/                  (P9.08)
│   ├── modules/lambda_fn/        (opcional) exportador agendado com SnapStart                                                  (P9.05)
│   ├── modules/eks/              (opcional, só forma) cluster privado com access entries                                       (P9.03)
│   └── envs/{study,production}/
├── scripts/ci/ecs-deploy.sh       nova revisão por DIGEST · update-service · wait · rolloutState                               (P9.02)
├── .github/workflows/deploy-ecs.yml   staging → production (aprovação) · OIDC · smoke test                                     (P9.02)
├── scripts/check-network.sh       a REDE REAL: nenhuma sub-rede privada com rota para o IGW                                    (P9.01)
├── src/main/java/com/payflow/…/   EventPublisher (porta) + SnsEventPublisher · QueueWorker · IdempotentHandler · CachedSecret    (P9.04, P9.06)
├── docs/
│   ├── network.md                 desenho, CIDRs, NAT × endpoints (custo), 3 destinos de saída                                  (P9.01)
│   ├── ecs.md                     papéis, orçamento de memória (718 MiB), deploy por digest, o que o circuit breaker não pega   (P9.02–03)
│   ├── messaging.md               fan-out, DLQ, visibility timeout, idempotência, quando FIFO/MSK                               (P9.04)
│   ├── secrets.md                 onde cada dado mora, injeção só na partida, rotação, dívida da conta mestra                    (P9.06)
│   ├── observability-aws.md       tabela dos 9 alertas da P6 × CloudWatch (cobertos / aproximados / ausentes)                    (P9.07)
│   ├── database.md                Multi-AZ × réplica, o que lê de onde, conta do pool no deploy, TTL de DNS                      (P9.08)
│   └── runbooks/{dlq,queue-age,unhealthy-targets,http-5xx,rds-storage,replica-lag}.md                                           (P9.07)
projeto/adrs/0021-nuvem-intermediaria-do-payflow.md   (já redigido nesta geração)
```
Critérios de pronto:
- [ ] **Contrato HTTP `/api/v1`, esquema e dinheiro inalterados**; a suíte de J5–P8 **continua verde**; **nenhuma migração nova** (a próxima livre continua V12).
- [ ] **5 módulos com `terraform test` verdes** (22 casos) e os **3 mutantes** reprovando (*runbook* removido, banco aberto a `0.0.0.0/0`, `deletion_protection` desligada).
- [ ] `check-network.sh` **passa** na rede aplicada e **reprova** depois da rota criada à mão; **nenhum `0.0.0.0/0`** na saída da aplicação; `trivy config` **sem achados**, cada `ignore` com **motivo** (a AWS-0104 **não** aparece).
- [ ] Imagem **por *digest*** (a `validation` recusa tag); `ecs-deploy.sh` cria a **revisão N+1** e o `plan` **não** quer desfazê-la; `actionlint`/`shellcheck` limpos; aprovação em `production`.
- [ ] *Task role* com **menor privilégio** (1 tópico, 2 filas, 5 ações); *execution role* lê **só** os 2 segredos; **nenhum segredo no *state*** (`grep -c` = 0).
- [ ] Os **5 testes Java** verdes e os **2 mutantes** reprovando; o `SnsEventPublisher` **sempre** define `eventType` (teste); consumidor **idempotente** com `processed_messages`.
- [ ] **Alarme por DLQ e por idade de fila**; todo alarme com **severidade, *runbook* e ação**; retenção **definida** em todo *log group*.
- [ ] Banco: **produção** com proteção, *snapshot* final e Multi-AZ; **réplica só para `reporting`**; **`networkaddress.cache.ttl` declarado**; **conta do *pool* no *deploy*** registrada e corrigida.
- [ ] `docs/*.md` e os *runbooks* escritos; **ADR 0021** revisado; tag `payflow-P9`; **`terraform destroy` + conferência** de que não sobrou nada (💸).

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Pleno: **≥ 2** em todos e **3** nos marcados com ⭐.

| Competência | Aula(s) | 0–3 |
|---|---|---|
| **VPC de três camadas**, CIDR (`cidrsubnet`), rotas, SG × NACL, NAT × *endpoints* | P9.01 | ⭐ |
| **Saída restrita** e **verificar a rede real** (não só o código) | P9.01 | ⭐ |
| **ECS/Fargate**: definição de tarefa, serviço, ALB, *health check*, parada ordenada | P9.02 | ⭐ |
| ***Task role* × *execution role***; menor privilégio da aplicação | P9.02 | ⭐ |
| ***Deploy* por *digest***, *circuit breaker* (o que **não** pega), um dono por coisa | P9.02 | |
| **ECS × EKS**: o que a AWS gerencia, custos fixos, critérios de reavaliação | P9.03 | |
| **SNS → SQS** (*fan-out*), DLQ, *visibility timeout*, idempotência, EventBridge | P9.04 | ⭐ |
| **SQS padrão × FIFO × MSK** e o que se perde | P9.04 | |
| **Lambda**, ciclo de vida, *cold start* de Java, SnapStart (cuidados de unicidade), `L = λ × W` | P9.05 | |
| **Secrets Manager × Parameter Store**, injeção no ECS, **rotação** e a janela de coexistência | P9.06 | ⭐ |
| **Segredo fora do *state*** (*write-only*/*ephemeral*) e prova | P9.06 | ⭐ |
| **CloudWatch**: métricas, alarmes, **DLQ**, razão com tráfego mínimo, retenção, mapa dos 9 alertas | P9.07 | ⭐ |
| **RDS**: Multi-AZ × réplica, **leitura defasada**, *read-your-writes*, **conta do *pool*** | P9.08 | ⭐ |
| ***Failover*** (DNS, TTL da JVM), PITR, `deletion_protection`, ler o `plan` | P9.08 | |
| **Equivalência AWS ↔ Azure ↔ GCP** (como mapa, não tradução) | P9.01–P9.08 | |

**Se algum ⭐ ficou abaixo de 2:** refaça `lab/cloud-lab/run-cloud.sh` lendo cada saída, escreva do zero o módulo `messaging` com seus testes e o `check-network.sh`, e reproduza `replica-lab/run-replica.sh` antes de seguir para a P10.

## Parte 4: Perguntas de entrevista (simulado rápido)
Responda em voz alta, 2 minutos cada: (1) "Como você desenha uma VPC para uma aplicação com banco e por que subnets privadas?" (2) "NAT Gateway ou VPC endpoints?" (3) "Task role × execution role." (4) "Como você faz deploy sem derrubar o serviço no ECS e o que pode dar errado mesmo assim?" (5) "ECS ou EKS?" (6) "SQS × SNS × EventBridge." (7) "O SQS entrega uma vez só? E se o consumidor falhar sempre?" (8) "Java em Lambda: o que é cold start e o que o SnapStart muda?" (9) "Como você entrega e rotaciona um segredo?" (10) "Réplica de leitura: quando e quando não?" (11) "O que acontece com a aplicação Java num failover do RDS?" (12) "O que você alarmaria numa fila?" **Desenho (design de um serviço, 20 min):** *"Desenhe a infraestrutura AWS para um serviço de pagamentos com banco, dois consumidores assíncronos e relatórios pesados: rede, computação, mensageria, segredos, observabilidade e o que muda em produção."* Use **o que você mediu** (96% de leitura defasada, 30 s de DNS, 344 → 108 ms).

## ➡️ Próximo
[P10 — Segurança intermediária](../P10-seguranca-intermediaria/README.md)
