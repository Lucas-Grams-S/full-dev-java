# P8 — CI/CD e infraestrutura como código

> 🔵 Pleno · ⏱️ 13.5h (9 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **parcialmente executado: o Terraform (contra o moto, sem conta AWS), o Trivy, os mecanismos de deploy (Docker/nginx e PostgreSQL 18), o avaliador de *feature flags*, e `actionlint`/`shellcheck` foram executados de verdade; o GitHub Actions, a AWS real, o Kubernetes, o GitOps e o Jenkins NÃO foram; testes práticos do PayFlow no fim do treinamento, por decisão do aluno**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído em `~/docs/payflow`, tag `payflow-P8`).** **Executado:** `iac-lab` (Terraform 1.17.1 contra o **moto**, sem conta AWS): os 10 experimentos (plan, apply, segundo plan, *drift*, `moved`, bloqueio de *state*, `import`, substituição forçada, `terraform test`, destroy, Trivy de configuração) **reproduzem**; só mudam ruídos (ids, tempos) e o provedor foi **6.74.0** (a aula citava 6.73.0). `deploy-lab` (nginx): *recreate* (janela de falha 6,08 s × 6,12 s), *rolling* 0 falhas, *blue/green* e *canary* bom/ruim iguais às aulas. `FlagLab.java`: *ALL EXPERIMENTS PASSED*. `actionlint` e `shellcheck --severity=style` do `pipeline-lab`: sem saída. **Trivy 0.76.2 na imagem do PayFlow reconstruído: 0 HIGH/CRITICAL** (1 MEDIUM no Alpine 3.24.4; Java sem achados). **Corrigido nas aulas:** nenhuma divergência entre as aulas e as saídas; só a versão do provedor AWS (6.74.0 hoje). **Ainda não executado:** GitHub Actions de verdade, AWS real, os workflows `ci/release/deploy/infra/drift` **no repositório do PayFlow** (só no `pipeline-lab`), `FeatureFlags` dentro do PayFlow, o *state* remoto em S3 real e o `terraform apply` em conta AWS. Os avisos "não executado" / "revisado por leitura" nas aulas descrevem a escrita original.

## 🎯 Objetivo do módulo
Automatizar entrega e infraestrutura do PayFlow: **pipeline** completo (análise estática, testes, dependências, IaC e *chart* em paralelo, com **um** check obrigatório), **scan** de segurança e **publicação** de imagem (SBOM, *digest*, proveniência), **estratégias de implantação** (*rolling*, *blue/green*, *canary*) e **reversão**, **migração de banco sem parada** (*expand/contract*), ***feature flags***, **Terraform** (providers, *state*, módulos, *state* remoto com bloqueio, testes), a **infraestrutura do PayFlow como código** (com *plan*/*apply* por papéis, *drift* e `destroy` provado), **GitOps** (visão geral) e **Jenkins** para legado.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [P8.01](P8.01-pipeline-completo-build-testes-e-analise-estatica.md) | Pipeline completo: build, testes e análise estática | 1h30 |
| [P8.02](P8.02-scan-de-seguranca-e-publicacao-de-imagem.md) | Scan de segurança e publicação de imagem | 1h30 |
| [P8.03](P8.03-rolling-blue-green-e-canary.md) | Rolling, blue/green e canary | 1h30 |
| [P8.04](P8.04-feature-flags.md) | Feature flags | 1h30 |
| [P8.05](P8.05-terraform-i-providers-recursos-e-state.md) | Terraform I: providers, recursos e state | 1h30 |
| [P8.06](P8.06-terraform-ii-modulos-e-state-remoto.md) | Terraform II: módulos e state remoto | 1h30 |
| [P8.07](P8.07-infraestrutura-do-payflow-com-terraform.md) | 💸 Infraestrutura do PayFlow com Terraform | 1h30 |
| [P8.08](P8.08-gitops-visao-geral-e-jenkins-para-legado.md) | GitOps (visão geral) e Jenkins para legado | 1h30 |
| [P8.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório em [`lab/`](lab/README.md): `lab/iac-lab/` (**executado** contra o moto), `lab/deploy-lab/` (**executado**), `lab/flags-lab/` (**executado**) e `lab/pipeline-lab/` (**`actionlint` e `shellcheck` executados**; GitHub, `helm`, GitOps e Jenkins **não**).

> ⚠️ **Aviso de verificação (leia antes de estudar):** **foi executado** (Linux, Docker 29.9.2, Terraform 1.17.1, AWS provider 6.73.0, Trivy 0.76.2, `actionlint` 1.7.12, JDK 25.0.5, PostgreSQL 18): as saídas mostradas nas aulas são **as reais**. **Não foi executado:** o **GitHub Actions** de verdade (nada foi publicado), a **AWS real** (💸), o **OIDC**, o **ECR**, a **atestação**, o **Kubernetes** (`kind`/`kubectl`/`helm`), Gateway API, Argo Rollouts, Argo CD/Flux, Jenkins, OpenFeature e **todo o PayFlow do aluno** (revisado por leitura). O **moto** (emulador) prova sintaxe e ordem, **não** o comportamento da AWS; os **tempos** do emulador (RDS "1 min 20 s") **não** valem para a AWS. A **lista "a validar no fim do treinamento"** está em `00-guia/progresso.md`.

> **Avisos de ordem:** a P8.01 retoma a J10.01–02 (GitHub Actions) e a J7.06–08 (JaCoCo, Checkstyle, SpotBugs); a P8.02 retoma P7.01 (tag imutável/digest) e J8.07 (dependências); a P8.03 usa as sondas e o `RollingUpdate` da P7.03/P7.05 e as migrações da J3.12/P2.05; a P8.04 usa `AtomicReference`/visibilidade (P1.07–P1.08); a P8.05–P8.07 retomam a J10.04–J10.07 (IAM, EC2/RDS, destruir tudo); a P8.07 aponta para a **P9** (rede privada, VPC *endpoints*, ECS/EKS) e a **P10** (políticas de IAM finas, política em código, assinatura de imagens); a P8.08 **não** instala Argo CD, Flux nem Jenkins.

## 🏗️ Entrega no PayFlow
**Pipeline** (`ci`, `release`, `deploy`, `infra`, `drift`), **scripts** de scan/deploy/*smoke*, `.trivyignore.yaml`, *chart* com `image.digest` e `values-*.yaml`, `FeatureFlags`, **Terraform** (módulos, *state* remoto, testes), `docs/{pipeline,delivery,infra}.md`, *runbook* de reversão e **ADR 0020**. Tag `payflow-P8`. **Nenhuma migração nova** (a próxima livre continua V12).

## ➡️ Próximo
[P9 — Nuvem intermediária](../P9-nuvem-intermediaria/README.md)
