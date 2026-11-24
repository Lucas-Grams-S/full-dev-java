# Laboratório do P8

> ⚠️ **Estado:** **executado** (Linux, Docker 29.9.2, Terraform 1.17.1, AWS provider 6.73.0, Trivy 0.76.2, `actionlint` 1.7.12, `shellcheck` 0.11.0, JDK 25.0.5, PostgreSQL 18): `iac-lab` (contra o **moto**, **sem conta AWS**), `deploy-lab` (Docker/nginx), `flags-lab` (Java puro), `pipeline-lab` (`actionlint`, `shellcheck`, `smoke-test.sh`, Trivy) e `deploy-lab/expand-contract.sql` (PostgreSQL 18). **NÃO executado:** o GitHub Actions (nada foi publicado; o `gh` do ambiente está logado na conta real do aluno), a AWS real, o OIDC, o ECR, a atestação, o Kubernetes, Gateway API/Argo CD/Flux/Jenkins e o OpenFeature. O `Jenkinsfile` e o `Application` do Argo CD só tiveram a **forma** conferida.

## `iac-lab/` (P8.05, P8.06, P8.07): executado
```bash
cd lab/iac-lab && bash run-iac.sh      # ~6 min; sobe o moto, roda tudo, regrava out-*.txt e limpa
```
`terraform/` = `bootstrap/` (balde do *state*) · `envs/study/` (a pilha do PayFlow) · `modules/{registry,app_host,database,github_oidc}/` (os dois últimos **com** `tests/*.tftest.hcl`). Saídas: `out-1-plan` … `out-10-trivy-config`. O `run-iac.sh` **não** usa conta AWS (credenciais falsas + `AWS_ENDPOINT_URL`), usa `TF_PLUGIN_CACHE_DIR` (o *provider* tem ~772 MB) e remove `.terraform/`, *states* e o contêiner `lab-p8-moto` ao fim. **Nunca** aponte este laboratório para uma conta real: o `variables.tf` só usa o emulador quando `emulator_endpoint` é informado.

## `deploy-lab/` (P8.03): executado
```bash
cd lab/deploy-lab && bash run-all.sh   # ~3 min; precisa de Docker e da imagem nginx:1.29-alpine; grava out-*.txt
```
`exp1-recreate.sh`, `exp2-rolling.sh` (`BAD_ROLLOUT=1|2`), `exp3-bluegreen.sh`, `exp4-canary.sh` (`BAD=1`); `load.py` é o gerador de carga (**ciclo fechado**: compare **janelas**, não contagens); `expand-contract.sql` (rode no `postgres:18`: `docker exec -i … psql < expand-contract.sql`). O *startup* da aplicação é **simulado** (`STARTUP=5`). Os "pods" são contêineres nginx e o "Service" é um roteador nginx: provam a **lógica**, não o Kubernetes. Limpeza: os *scripts* removem os contêineres `lab-p8-*` e a rede `lab-p8-net`.

## `flags-lab/` (P8.04): executado
```bash
cd lab/flags-lab && /usr/lib/jvm/java-25-openjdk-amd64/bin/java FlagLab.java   # ~15 s; grava out-flags.txt (use tee)
```
Um avaliador de *feature flags* (precedência, *rollout* percentual estável com sal, `view()`) e seis experimentos com asserções.

## `pipeline-lab/` (P8.01, P8.02, P8.03, P8.07, P8.08): executado em parte
```bash
cd lab/pipeline-lab
docker run --rm -v "$PWD":/repo -w /repo rhysd/actionlint:1.7.12 -color=false .github/workflows/*.yml
docker run --rm -v "$PWD":/mnt koalaman/shellcheck:stable --severity=style -x scripts/ci/*.sh
TRIVY_CACHE=~/.cache/p8-trivy bash scripts/ci/scan-image.sh <imagem-local:tag>   # precisa do Trivy em contêiner (digest fixado no script)
```
`.github/workflows/{ci,release,deploy,infra,drift}.yml` (**passam no `actionlint`**), `scripts/ci/*.sh` (**passam no `shellcheck`**; o `smoke-test.sh` foi rodado contra dois `nginx`: `out-smoke.txt`), `.trivyignore.yaml` (exceção com `expired_at`), `deploy/chart/payflow/` (`_image.tpl` para *digest* e `values-*.yaml`: **só YAML validado**, sem `helm`), `deploy/gitops/` e `Jenkinsfile` (**não executados**). Em um repositório Git, o `actionlint` acha os *workflows* sozinho; fora dele, passe os arquivos.

> **Atenção (custo e segurança):** nenhum recurso real foi criado. Se **você** aplicar o Terraform na AWS real: 💸 faça no mesmo dia em que vai destruir, com o alerta de custo da J10.04, e confira `aws sts get-caller-identity` antes de qualquer `apply`/`destroy`. O estado e o arquivo do plano podem conter segredos: **nunca** os versione.
