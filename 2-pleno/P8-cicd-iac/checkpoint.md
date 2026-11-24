# P8.CP — Checkpoint do módulo P8

> ⏱️ ~1h30 · 🔵 Pleno · Pré-requisitos: P8.01 a P8.08

> ✅ **Validação final (2026-11-24):** módulo executado no PayFlow reconstruído; resultado e o que continua não executado no [README do módulo](README.md). Os avisos "não executado" / "por leitura" abaixo descrevem a escrita original.

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** nesta geração (Linux, Docker 29.9.2, Terraform 1.17.1, AWS provider 6.73.0, Trivy 0.76.2, `actionlint` 1.7.12, JDK 25.0.5): (1) o **Terraform** do PayFlow contra o **moto** (`plan`/`apply`/`destroy` de 20 recursos, *drift*, `moved`, `import`, *state* remoto com **bloqueio**, `terraform test` com **mutantes**, `-detailed-exitcode`, recriação do banco e `prevent_destroy`); (2) o **Trivy** (imagem, configuração, dependências, SBOM e exceção com validade); (3) os **mecanismos de deploy** (recreate, rolling, blue/green, canary) em Docker/nginx e a migração ***expand/contract*** no PostgreSQL 18; (4) o **avaliador de *feature flags*** (Java puro); (5) `actionlint`/`shellcheck` nos *workflows* e *scripts*, com **mutantes**. **NÃO executado:** o **GitHub Actions** (nada foi publicado: o `gh` do ambiente está logado na conta real), a **AWS real**, o **OIDC**, o **ECR**, a **atestação**, o **Kubernetes** (kind/kubectl/helm), **Gateway API/Argo Rollouts/Argo CD/Flux/Jenkins**, o **OpenFeature**, e **todo o PayFlow do aluno** (revisado por leitura). A **lista "a validar no fim do treinamento"** está em `00-guia/progresso.md`.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Por que o pipeline do PayFlow tem **um** *job* `ci-passed` como único check obrigatório? O que acontece com um *job* **pulado** numa regra de proteção de branch?
2. O que o `actionlint` pegou nos três **mutantes**? Por que um verificador precisa de mutantes?
3. Onde o *scan* da imagem entra no `release.yml` e **por que antes do *push***? Diferencie **SBOM** e **atestação de proveniência**.
4. O *gate* do Trivy reprovou `alpine:3.17` e `eclipse-temurin:25-jre`. O que significam `fixed`, `--ignore-unfixed` e `EOSL`? Como uma **exceção** deve ser registrada e o que a **executamos** para provar?
5. Compare *recreate*, *rolling*, *blue/green* e *canary*: janela fora do ar, versões simultâneas, capacidade extra e tempo de reversão (**use os números medidos**).
6. Por que o *rolling* **completou com 9,1% de erro** e como o *canary* o teria pego? Por que a taxa do **total** (1,6%) **enganou**?
7. O que é *expand/contract* e que erro a versão antiga recebe sem ele? Quais são os três *releases*?
8. Em *feature flags*: o que são **precedência**, **estabilidade**, **sal** e **`view()`**? O que acontece sem o sal?
9. O que o *state* do Terraform guarda e por que **nunca** vai para o Git? Como `use_lockfile` impede dois `apply` simultâneos? O que são `moved` e `import`?
10. Como um *pipeline* de infraestrutura impede que uma PR destrua o banco? Cite **cinco** controles e o que `terraform plan -detailed-exitcode` devolve em cada caso.

<details><summary>Gabarito</summary>

1. Porque a regra de proteção exige **um** nome estável: ao acrescentar/renomear *jobs*, muda-se o *workflow*, **não** a configuração do repositório. Um check obrigatório precisa estar **bem-sucedido, ignorado (*skipped*) ou neutro**: um *job* **pulado conta como aprovado**. Por isso o `ci-passed` roda com `if: always()` e **recusa tudo que não seja `success`** (`skipped` e `cancelled` incluídos).
2. **Injeção de comando** (`${{ github.head_ref }}` num `run:`), **`needs` apontando para um *job* inexistente** (`chrat`) e **referência a um *step* que não existe** (`steps.nope`). Um verificador que **nunca falhou** não prova que funciona: o mutante é um **defeito conhecido** que ele **deve** acusar (o mesmo vale para os testes de Terraform: `MUTABLE` no lugar de `IMMUTABLE`, `StringLike repo:acme/*` no lugar do `StringEquals` exato).
3. Depois do **build local** (`load: true`) e **antes** do login e do `push`: uma imagem vulnerável **nunca chega ao registro**. **SBOM** (CycloneDX, 119 componentes no `eclipse-temurin:25-jre`) = **inventário** do que há dentro (responde "estamos afetados?" sem reescanear). **Atestação** = **proveniência** (quem construiu este *digest*, de qual *commit* e *workflow*). Nenhum prova ausência de falha e só valem se **alguém os consome**.
4. `fixed`: já há versão corrigida; `--ignore-unfixed`: omite achados sem correção; `EOSL`: a distribuição **parou de publicar** correções (`alpine 3.17.10`: só trocando a base). Exceção: `.trivyignore.yaml` com `id`, **`statement`** (motivo) e **`expired_at`** (validade). **Executado:** com a exceção válida, `exit=0`; com `expired_at` no passado, `exit=1`; a primeira tentativa (com `paths`) **não funcionou** para pacotes do sistema.
5. **Medido (nginx, 3.000–4.000 req):** *recreate*: **janela de 6,12 s** fora do ar (5 s de *startup* simulado) com 502/504; *rolling* (4 réplicas, com *readiness*): **0 falhas**, versões misturadas (48,6% × 51,4%); *blue/green*: troca em **0,09 s** e **0 falhas** nos **dois** sentidos (inclusive a volta), capacidade **2×**; *canary*: 5% → 25%, divisão observada **5,3%** e **24,9%**, reversão = peso a 0.
6. Uma versão que erra 30% passou na *readiness* porque a sonda **continua tentando** e só precisa de **3 sucessos seguidos uma vez** (10 de 10 rodadas no laboratório); o *rolling* **terminou** com **364 falhas em 4.000 (9,1%)**. No *canary* a decisão olha a taxa **da versão nova**: **32,3%** (63 de 195) > 2% → **abortar**; a taxa do **total** foi só **1,6%**, abaixo de qualquer alerta. Depois do aborto, 500 de 500 requisições voltaram a `v1`.
7. Mudar o banco **em passos compatíveis** com a versão antiga e a nova (*parallel change*). Sem isso, um `rename column` faz a v1, ainda no ar, receber `ERROR: column "owner_name" does not exist`. **Expand** (acrescenta a coluna nova, mantém a antiga e as sincroniza), **Migrate** (a aplicação usa só a nova), **Contract** (remove a antiga **depois** que nenhuma versão antiga roda).
8. **Precedência:** *kill switch* > lista de testadores > regra de segmentação > percentual; flag desconhecido devolve o **padrão de quem chama**. **Estabilidade:** *hash* de (flag:usuário) em baldes → mesmo usuário, mesma resposta; 10% → 25% só **acrescenta** (10.057 de 10.057 permaneceram). **Sal:** a chave do *flag* entra no *hash*; sem ele, dois *flags* de 50% ligam para os **mesmos** 50% (50,2% em vez de 25,0%). **`view()`:** um *snapshot* por decisão; com duas chamadas separadas, 56,7% das leituras em par ficaram **inconsistentes** (cenário extremo); com `view()`, **0**.
9. O mapa código ↔ objetos reais, com **todos** os atributos, inclusive segredos (`sensitive` só esconde da tela): vai para armazenamento **remoto, cifrado, versionado e restrito**. `use_lockfile`: o primeiro `apply` grava `terraform.tfstate.tflock` com escrita **condicional** ("só se não existir"); o segundo recebe `412 PreconditionFailed` ("Error acquiring the state lock"). `moved`: muda o **endereço** no *state* sem tocar na nuvem (sem ele, o rename planejou `2 to add, 2 to destroy`); `import`: adota o recurso existente **no `plan`**, revisável.
10. (1) *Plan* na PR com papel de **leitura** e `apply` só na `main` com papel de **escrita**; (2) `apply` do **plano salvo**; (3) *environment* com **revisores obrigatórios**; (4) **`prevent_destroy`** + `deletion_protection` + *snapshot* final; (5) ler o `plan` (`destroy`, `-/+`, `forces replacement`) e o **Trivy** no Terraform. Códigos: **0** sem mudanças, **1** erro, **2** mudanças/*drift* (medido).
</details>

## Parte 2: Entrega prática (tag `payflow-P8`)
O `payflow` ganha **pipeline completo**, **scan e publicação de imagem**, **estratégia de implantação e reversão**, **mecanismo de *feature flags***, **infraestrutura como código** e **documentação**, **sem** mudar o contrato HTTP `/api/v1`, o esquema (**nenhuma migração**) nem o dinheiro (uma transação).
```
payflow/
├── .github/workflows/
│   ├── ci.yml              static · test · dependencies · iac · chart · ci-passed (único check obrigatório)                 (P8.01)
│   ├── release.yml         build (load) → scan → push → atestação; SBOM como artefato                                      (P8.02)
│   ├── deploy.yml          staging → produção (aprovação); helm upgrade por DIGEST; smoke test; --rollback-on-failure       (P8.03)
│   ├── infra.yml           terraform plan (PR, leitura) → apply do plano salvo (main, aprovação)                           (P8.07)
│   └── drift.yml           terraform plan -detailed-exitcode, todo dia útil                                                 (P8.07)
├── scripts/ci/             scan-image.sh · scan-config.sh · scan-fs.sh · terraform-check.sh · helm-deploy.sh · smoke-test.sh (P8.01–03)
├── .trivyignore.yaml       exceções com statement + expired_at                                                              (P8.02)
├── deploy/
│   ├── chart/payflow/      + templates/_image.tpl (image.digest), values-staging.yaml, values-production.yaml               (P8.03)
│   ├── terraform/          bootstrap/ · envs/study/ · modules/{registry,app_host,database,github_oidc}/ com tests/          (P8.05–07)
│   └── gitops/             (opcional) payflow-production.yaml: só como referência                                           (P8.08)
├── src/main/java/com/payflow/shared/features/   FeatureFlags (interface) + implementação por configuração + testes dos dois ramos (P8.04)
├── docs/
│   ├── pipeline.md         grafo, caminho crítico medido, branch protection (só ci-passed)                                   (P8.01)
│   ├── delivery.md         estratégia, gatilhos de reversão numéricos, expand/contract, decisão de GitOps                    (P8.03, P8.08)
│   ├── infra.md            mapa J10 → Terraform, ambientes/papéis OIDC, checklist de custo 💸 e destruição                    (P8.07)
│   └── runbooks/rollback.md   quando reverter, como (helm history/rollback), o que verificar                                  (P8.03)
projeto/adrs/0020-cicd-e-infraestrutura-como-codigo-do-payflow.md   (já redigido nesta geração)
```
Critérios de pronto:
- [ ] **Contrato HTTP `/api/v1`, esquema e dinheiro inalterados**; a suíte de J5–P7 **continua verde**; **nenhuma migração nova** (a próxima livre continua V12).
- [ ] `actionlint` e `shellcheck` sem erro; **um mutante por verificador** reprovado; `ci-passed` recusa `skipped`/`cancelled`; a `main` exige **só** `ci-passed`.
- [ ] Imagem **escaneada antes do push**, tag imutável `<versão>-<sha7>`, SBOM guardado, Trivy e ações **fixados por *digest*/SHA**; toda exceção com **motivo e validade**.
- [ ] Deploy **por *digest***, com aprovação em produção, *smoke test* e **reversão automática**; o *smoke test* **reprova** um servidor com a segurança desligada.
- [ ] Migrações **aditivas** (*expand/contract*); gatilhos de reversão **numéricos** escritos antes.
- [ ] `FeatureFlags` com precedência, *hash* estável com **sal**, `view()` e **teste dos dois ramos** contra as invariantes do *ledger*; *flags* de release com **dono e vencimento** (teste de CI).
- [ ] Terraform: `fmt`/`validate`/`test` passam; *state* **remoto com bloqueio, versionado e cifrado**; `prevent_destroy` no balde do *state*, no banco e no ECR (produção); `trivy config` limpo (cada `ignore` com motivo); **`destroy` + conferência**.
- [ ] `docs/pipeline.md`, `docs/delivery.md`, `docs/infra.md` e o *runbook* escritos; **ADR 0020** revisado; tag `payflow-P8`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Pleno: **≥ 2** em todos e **3** nos marcados com ⭐.

| Competência | Aula(s) | 0–3 |
|---|---|---|
| **Pipeline** como grafo, caminho crítico, check obrigatório único | P8.01 | ⭐ |
| **Validar** o pipeline (`actionlint`, `shellcheck`) e provar com mutantes | P8.01 | |
| **Scan** de dependências, configuração e imagem; *gates* e exceções com validade | P8.02 | ⭐ |
| **SBOM**, proveniência, tag imutável e *digest* (cadeia de suprimentos) | P8.02 | |
| **Estratégias de deploy** (recreate, rolling, blue/green, canary) e reversão | P8.03 | ⭐ |
| **Migração sem *downtime*** (*expand/contract*) | P8.03 | ⭐ |
| **Feature flags** (precedência, estabilidade, sal, dívida) | P8.04 | ⭐ |
| **Terraform**: providers, recursos, *state*, *drift*, `count` × `for_each` | P8.05 | ⭐ |
| **Módulos**, *state* remoto com bloqueio, `moved`/`import`, `terraform test` | P8.06 | ⭐ |
| **Infraestrutura do PayFlow**: pipeline de *plan*/*apply*, papéis, ler o `plan`, destruir e provar | P8.07 | ⭐ |
| **GitOps** (princípios, *push* × *pull*) e **Jenkins** legado | P8.08 | |

**Se algum ⭐ ficou abaixo de 2:** refaça `lab/iac-lab/run-iac.sh` lendo cada saída, escreva do zero o `ci-passed` e o `infra.yml`, e rode `deploy-lab/exp4-canary.sh` com `BAD=1` antes de seguir para a P9.

## Parte 4: Perguntas de entrevista (simulado rápido)
Responda em voz alta, 2 minutos cada: (1) "Como você deixa um pipeline rápido e confiável?" (2) "Onde ficam os scans de segurança e por quê?" (3) "Compare blue/green e canary." (4) "O deploy passou na readiness e deu incidente. Por quê?" (5) "Como mudar o esquema do banco sem parar o sistema?" (6) "Como você usa *feature flags* sem criar dívida?" (7) "O que é o *state* do Terraform e como protegê-lo?" (8) "O `plan` mostra `destroy` no banco. O que você faz?" (9) "Como rodar Terraform em CI com segurança?" (10) "GitOps ou pipeline com `helm upgrade`?"

## ➡️ Próximo
[P9 — Nuvem intermediária](../P9-nuvem-intermediaria/README.md)
