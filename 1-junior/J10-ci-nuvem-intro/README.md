# J10 — CI e introdução à nuvem

> 🟢 Júnior · ⏱️ 12h (8 sessões de 1h30) · Status: ✅ concluído (2026-11-13) — **validado na execução final (2026-11-24); veja o bloco abaixo**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído em `~/docs/payflow`, tag `payflow-J10`).** **Executado:** `./mvnw -B -ntp verify` no PayFlow: **256 testes** verdes, JaCoCo e Checkstyle ok, **BUILD SUCCESS** em 1 min 15 s; `actionlint 1.7.12` sobre os 4 workflows sem saída; `shellcheck` sem avisos de nível *warning* nos `scripts/ci`, `deploy` e `scripts/*.sh` (só 3 notas de estilo e os `SC2034` de variáveis do `env.sh`, usadas por outros scripts); `check-pr-title.sh` (aceita `feat(api): ...`, recusa `Update stuff`); `scripts/aws/*.sh` contra o **moto** (budget, IAM, S3/ECR, rede, RDS, instância): o `91-verify-empty.sh` achou **10 tipos** de recurso, o `90-teardown.sh` sem `CONFIRM=payflow` **recusou** e com ele destruiu tudo, e o `91` depois deu **11 `ok`**; JSON das 4 políticas IAM válidos; `compose.prod.yaml` e `compose.edge.yaml` passam em `docker compose config`. **Corrigido nas aulas:** o `check-secrets.sh` (J8.07, usado pelo `security.yml`) acusava `this.passwordHash = passwordHash;` no PayFlow real: restrito a YAML/properties. **Ainda não executado:** GitHub Actions de verdade (OIDC, cache, artefatos), a AWS real (EC2, RDS, SSM, ECR), `deploy.sh` com PostgreSQL em TLS e o Caddy neste PayFlow (só foram executados no laboratório) e o `aws login`. Os avisos "não executado" nas aulas descrevem a escrita original.

## 🎯 Objetivo do módulo
Automatizar o build no GitHub Actions e colocar o PayFlow no ar de forma barata e segura, **e destruir tudo provando que nada ficou**.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [J10.01](J10.01-github-actions-build-e-testes-por-pull-request.md) | GitHub Actions: build e testes por pull request | 1h30 |
| [J10.02](J10.02-github-actions-analise-cache-e-seguranca-do-pipeline.md) | GitHub Actions: análise, cache e segurança do pipeline | 1h30 |
| [J10.03](J10.03-conceitos-de-nuvem-e-responsabilidade-compartilhada.md) | Conceitos de nuvem e responsabilidade compartilhada | 1h30 |
| [J10.04](J10.04-conta-aws-alerta-de-custos-mfa-e-iam.md) | 💸 Conta AWS: alerta de custos, MFA e IAM | 1h30 |
| [J10.05](J10.05-ec2-rds-s3-ecr-e-security-groups.md) | 💸 EC2, RDS, S3, ECR e Security Groups | 1h30 |
| [J10.06](J10.06-deploy-do-payflow-imagem-oidc-ssm-e-rollback.md) | 💸 Deploy do PayFlow: imagem, OIDC, SSM e rollback | 1h30 |
| [J10.07](J10.07-destruir-tudo-e-equivalencia-aws-azure-gcp.md) | 💸 Destruir tudo e equivalência AWS ↔ Azure ↔ GCP | 1h30 |
| [J10.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório pronto em `lab/ci-lab/` (PayFlow Lite + testes, workflows, scripts de AWS, `deploy/`); veja `lab/README.md`.

> ⚠️ **Aviso de verificação (leia antes de estudar):** **não há conta AWS neste ambiente** e, por decisão do aluno, o PayFlow não foi compilado. **Executados de verdade:** `./mvnw verify` do laboratório (Testcontainers + JaCoCo), o `actionlint`, o `shellcheck`, o `deploy.sh` com PostgreSQL 18 em TLS (`verify-full`), o Caddy (limite do corpo) e o SQL de criação do primeiro ADMIN. **A AWS foi emulada** (moto): isso prova a sintaxe e a **ordem** dos scripts de criação e de destruição, não o comportamento da AWS. **Não executados:** o GitHub Actions de verdade (cache, OIDC, *environments*), RDS, EC2, `user-data`, SSM (`send-command` e túnel), `aws login`, a reversão do deploy e qualquer custo real.

> **Avisos de ordem:** a J10.01 reusa o `Dockerfile` da J9 (que ignora testes); a J10.02 cumpre promessas da J7 (título da PR), da J8 (Dependency-Check e `check-secrets.sh` no CI) e da J9 (testar **antes** de construir a imagem); a J10.06 cumpre a decisão 63 (teto do corpo na borda) com o Caddy opcional. Nenhum conceito de P1+ foi usado como pré-requisito (Kubernetes, ECS e Terraform aparecem só como destino).

## 🏗️ Entrega no PayFlow
`mvnw`, `ci.yml`/`pr-title.yml`/`security.yml`/`cd.yml`, Dependabot, `scripts/ci`, `scripts/aws`, `deploy/` (compose de produção, `deploy.sh`, `user-data.sh`, IAM, Caddy opcional) e `docs/ci-notes.md`, `cloud-notes.md`, `deploy-notes.md`. Tag `payflow-J10`.

## ➡️ Próximo
[Checkpoint de nível Júnior](../checkpoint-junior.md)
