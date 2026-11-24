# Cheatsheet — CI/CD, scan, deploy, flags e Terraform (P8)

> Valores **medidos** em `2-pleno/P8-cicd-iac/lab/` (Terraform 1.17.1 contra o **moto**, Trivy 0.76.2, nginx/Docker, JDK 25.0.5, PostgreSQL 18). **GitHub Actions, AWS real, Kubernetes, GitOps e Jenkins: NÃO executados.**

## Pipeline (P8.01)
| Regra | Detalhe |
|---|---|
| Único check obrigatório | `ci-passed` com `if: always()`; aceita **só** `success` (um *job* pulado conta como aprovado) |
| Falhar rápido | Checkstyle em `validate`; jobs independentes em paralelo |
| Verificar o pipeline | `actionlint .github/workflows/*.yml` e `shellcheck`; **mutante** para provar |
| Segurança | ações por SHA, `ubuntu-24.04`, `permissions: contents: read`, nada de `${{ github.head_ref }}` em `run:` |

## Scan e imagem (P8.02)
```bash
trivy image --input image.tar --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 --ignorefile .trivyignore.yaml
trivy config --severity MEDIUM,HIGH,CRITICAL --exit-code 1 deploy/terraform
trivy fs --scanners vuln --severity HIGH,CRITICAL --ignore-unfixed .
trivy image --input image.tar --format cyclonedx --output sbom.cdx.json
```
Imagem **antes do push** (`docker save | --input`, sem *socket*); Trivy por **digest**; exceção com `statement` + `expired_at`; inline: `# trivy:ignore:AWS-0104 <motivo>` **numa linha**, colado ao recurso.

## Estratégias de deploy (P8.03), medido em nginx
| | Janela | Versões | Capacidade | Reversão |
|---|---|---|---|---|
| Recreate | **6,12 s** (startup simulado 5 s) | 1 | 1× | novo deploy |
| Rolling (com readiness) | 0 | 2 | ~1× | novo rolling |
| Blue/green | 0 (troca **0,09 s**) | 2 isoladas | **2×** | trocar de volta |
| Canary | 0 | 2 em proporção | ~1× | peso → 0 |
*Readiness não pega "sobe e erra 30%"* (9,1% de erro no *rolling*); o *canary* compara **canário × estável** (32,3% × 1,6% do total). ***Expand/contract***: acrescenta → migra → remove; `rename` num passo = `column "x" does not exist` na versão antiga.

## *Feature flags* (P8.04)
Precedência: *kill switch* > testadores > regra > percentual; padrão **do chamador**; balde = `SHA-256("flag:usuario") % 10000`; **sal** = chave do *flag* (sem ele: 50,2% em comum em vez de 25%); **`view()`** por decisão; *flag* de release com **dono e vencimento**; nunca contorna invariante do *ledger*.

## Terraform (P8.05 a P8.07)
```bash
terraform init && terraform validate && terraform fmt -recursive -check
terraform plan -out=tfplan && terraform apply tfplan       # apply do plano revisado
terraform plan -detailed-exitcode                          # 0 sem mudanças · 1 erro · 2 mudanças/drift
terraform init -migrate-state -backend-config=backend.hcl  # local -> S3 (use_lockfile = true)
terraform test                                             # mock_provider; prove com mutantes
```
| Situação | Faça |
|---|---|
| Renomear recurso/módulo | bloco `moved` (sem ele: `2 to add, 2 to destroy`) |
| Adotar o que já existe | bloco `import` (aparece no `plan`) |
| `forces replacement` num banco | **pare**; `prevent_destroy` + `deletion_protection` + *snapshot* final |
| `count` × `for_each` | `for_each` por chave; `count` só para 0/1 |
| Bloqueio preso | `terraform force-unlock <ID>` **só** depois de confirmar que ninguém aplica (⚠️ não executado) |
*State* com segredos: remoto, cifrado, versionado, acesso restrito, **nunca** no Git; `.terraform.lock.hcl` **vai** para o Git; `.terraform/` ~772 MB (use `TF_PLUGIN_CACHE_DIR`).

## GitOps e Jenkins (P8.08), só leitura
GitOps = declarativo + versionado/imutável + **puxado** + reconciliado continuamente (OpenGitOps v1.0.0). Reavalie com **> 3 ambientes**, incidente por *drift* ou **mais de um time**. Jenkins legado: versionar como `Jenkinsfile`, migrar serviço a serviço.
