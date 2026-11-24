# Laboratório do P9

> ⚠️ **Estado:** **executado** (Linux, Docker 29.9.2, Terraform 1.17.1, AWS provider 6.73.0, AWS CLI 2.39.5, moto 5.2.6.dev, AWS SDK for Java 2.59.1, JDK 25.0.5, PostgreSQL 18.6, Trivy 0.76.2, `actionlint` 1.7.12, `shellcheck` 0.11.0). **O moto é uma emulação de API: não move pacotes, não executa contêineres nem funções, não avalia alarmes nem filtros de log, não impõe políticas.** Ele **travou** em três recursos (`aws_appautoscaling_target`, `aws_sqs_queue_redrive_allow_policy`, réplica do RDS pelo Terraform): o laboratório os **desliga** (`-var emulated=true`, `create_replica=false`). **Nenhuma conta AWS foi usada.** Tudo que depende da AWS real (💸) está marcado **não executado** nas aulas.

```bash
bash lab/run-all.sh        # ~15 min; reexecuta TUDO e regrava os out-*.txt (rode numa máquina quieta)
```

## `cloud-lab/` (P9.01–P9.08): executado contra o moto
```bash
cd lab/cloud-lab && ./run-cloud.sh            # ~10 min; sobe o moto (porta 5089), roda tudo, regrava out-s1…out-s11 e limpa
ONLY=s1 ./run-cloud.sh                        # só os testes de módulo e os mutantes (~2 min)
./jvm-budget.sh > out-jvm.txt                 # JVM sob limites de contêiner (Docker): 718 MiB de heap em 1 GiB
```
`terraform/modules/{network,alb,ecs_service,messaging,secrets,observability,database,lambda_fn,eks}` (**5 com `tests/`: 22 casos**), `terraform/envs/study` (**96 recursos**), `check-network.sh` (a rede **real**), `secret-state-lab/` (a prova do segredo no *state*: `ephemeral/` × `leaky/`). Seções: `s1` testes e **3 mutantes** · `s2` *plan*/*apply* · `s3` rede · `s4` ECS/ALB/IAM e `ecs-deploy.sh` · `s5` SNS/SQS/EventBridge · `s6` segredos · `s7` CloudWatch · `s8` RDS e EKS (forma) · `s9` Lambda · `s10` 2º *plan* e `destroy` · `s11` Trivy.

## `aws-sdk-lab/` (P9.04, P9.06): executado contra o moto
```bash
cd lab/aws-sdk-lab && ./run-sdk.sh            # ~2 min; sobe um moto (porta 5090), roda os 5 testes e 2 mutantes, grava out-sdk.txt
```
Maven, Java 25, SDK v2 **2.59.1**, JUnit **6.0.3**: `EventPublisher` (porta), `SnsEventPublisher`, `QueueWorker`, `IdempotentHandler`, `CachedSecret` e 5 testes (`FanOutTest`, `PoisonMessageTest`, `VisibilityTimeoutTest`, `FifoOrderingTest`, `SecretRotationTest`). Os testes **só rodam contra um emulador local** (`AWS_ENDPOINT_URL=http://localhost:…`): sem isso são **ignorados**, para nunca criarem recursos na AWS real.

## `replica-lab/` (P9.08): executado (PostgreSQL 18 de verdade)
```bash
cd lab/replica-lab && ./run-replica.sh > out-replica.txt 2>&1   # ~40 s: primário + réplica por streaming, 300 leituras imediatas
./run-dns.sh > out-dns.txt 2>&1                                  # ~40 s: cache de DNS da JVM (30 s × 5 s)
```
Cria e **remove** os contêineres `lab-p9-pg-primary`/`lab-p9-pg-replica`, os volumes `lab-p9-primary`/`lab-p9-standby` e a rede `lab-p9-net`.

## `lambda-lab/` (P9.05): executado (JVM local; **não** é Lambda)
```bash
cd lab/lambda-lab && AWS_ENDPOINT_URL=http://localhost:<porta-do-moto> ./run-coldstart.sh   # ~20 s; 7 variantes × 7 execuções
```
`ColdStartHandler.java` (cliente SQS + 1ª chamada), `handler/` (o contrato `RequestHandler`, compilado contra `aws-lambda-java-core` 1.4.1). **O SnapStart de verdade não foi executado.**

## `cidr-lab/` (P9.01): executado
```bash
java lab/cidr-lab/CidrLab.java        # aritmética de CIDR em Java puro; sai com código 1 se uma afirmação falhar
```

## `pipeline-lab/` (P9.02): `actionlint` e `shellcheck` executados; GitHub **não**
`.github/workflows/deploy-ecs.yml`, `scripts/ci/ecs-deploy.sh` (**executado contra o moto**: nova revisão por *digest*, tag recusada com `exit 2`) e `scripts/ci/smoke-test.sh` (o da P8). Saída: `out-lint.txt`.

## O que **limpar** depois
Contêineres `lab-p9-*`, volumes `lab-p9-*` e a rede `lab-p9-net` (os `run-*.sh` já removem); `target/` e `terraform.tfstate*` (os *scripts* removem); o cache de provedores do Terraform (`~/.cache/p8-tf-plugins`, compartilhado com a P8) e o do Trivy (`~/.cache/p8-trivy`) **ficam** (são grandes); o `~/.m2` ganhou o SDK da AWS 2.59.1 e o `aws-lambda-java-core`.
