# J10.CP — Checkpoint do módulo J10

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J10.01 a J10.07

> ✅ **Validação final (2026-11-24):** este módulo foi executado no PayFlow reconstruído (CI local e AWS emulada); resultado e o que continua não executado no [README do módulo](README.md). Os avisos "não executado" abaixo descrevem a escrita original.

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**, e **não há conta AWS neste ambiente**. **Executados de verdade** (Linux, Docker 29.9.2, JDK 25.0.5, Maven Wrapper 3.9.12, AWS CLI 2.39.5): `./mvnw verify` do `lab/ci-lab` (4 testes com Testcontainers `postgres:18`, JaCoCo 72,2% das linhas, piso 0,70; 10,9 s), o build quebrado de propósito e a violação do piso de cobertura, o `actionlint 1.7.12` e o `shellcheck 0.11.0`, o `check-pr-title.sh`, o `deploy.sh` (PostgreSQL 18 com TLS, `sslmode=verify-full`, SSM do moto, `chown 10001` + `chmod 400`), o Caddy 2.11 (413 × 500), o SQL de `bootstrap-admin` com o hash BCrypt validado pelo Spring Security 7.1.2 e a sequência `scripts/aws/*.sh` + teardown + verificação **contra o moto**. **Revisados por leitura, não executados:** o GitHub Actions de verdade (cache, artefatos, OIDC, *environments*), `dependency-review`, Dependency-Check, Dependabot, a trust policy e as políticas do IAM (só JSON válido), o RDS, a EC2 e o `user-data` no Amazon Linux 2023, `ssm send-command`/`start-session`, o login do painel por túnel, a **reversão** do `deploy.sh`, `aws login`, o fechamento da conta e qualquer **custo real**. O PayFlow **do aluno** não existe em disco.

## Parte 1: Quiz (10 questões)
1. O que roda a cada PR no CI do PayFlow e o que **bloqueia** o merge? Por que `permissions: contents: read`, `concurrency` e `timeout-minutes`?
2. Por que o `ci.yml` usa `./mvnw` e `ubuntu-24.04` em vez de `mvn` e `ubuntu-latest`? Por que o Testcontainers funciona no runner?
3. O que é *script injection* no Actions? Mostre o `run:` vulnerável e o corrigido para o título da PR.
4. Para que servem `cache: maven`, `upload-artifact` e `$GITHUB_STEP_SUMMARY`? Por que a `main` precisa rodar o CI para o cache das PRs funcionar?
5. Como o piso do JaCoCo foi escolhido? Que mensagem aparece quando o piso não é cumprido?
6. Diferencie `dependency-review`, `dependency-check` e Dependabot. Por que o segundo roda por agendamento? Por que fixar *actions* por SHA e por que **não** usar `pull_request_target`?
7. Em IaaS/PaaS/SaaS, onde ficam EC2, RDS e GitHub? Divida a responsabilidade entre AWS e você para o RDS. O que é região e AZ?
8. Qual a ordem das primeiras ações em uma conta AWS nova e por quê? Por que **não** habilitar o IAM Identity Center/Organizations no plano Free? O que o orçamento **não** faz?
9. Como a instância do PayFlow fica **sem porta aberta** e ainda é acessada e atualizada? Como o banco é protegido (rede, TLS, segredo)? O que é IMDSv2 e por que `verify-full`?
10. Como o GitHub entra na AWS **sem chave de acesso**? Qual a pegadinha do `sub`? Descreva o `deploy.sh` (segredos, `chown 10001`, `--wait`, reversão) e o `teardown` (ordem, prova de "vazio", o que ainda cobra).

<details><summary>Gabarito</summary>

1. `ci.yml`: `./mvnw -B -ntp verify` (compila, testes de unidade/fatias/integração com Testcontainers, JaCoCo, Checkstyle e SpotBugs) + upload dos relatórios; `pr-title.yml` (Conventional Commits); `security.yml` (segredos, `dependency-review`). O *ruleset* da `main` exige PR e o **check** `build, test and quality gates` (o nome do job). `permissions` mínimas limitam o `GITHUB_TOKEN`; `concurrency` cancela execuções obsoletas das PRs (não na `main`); `timeout-minutes` evita o padrão de **360 min**.
2. O wrapper fixa o Maven em 3.9.12 (o runner traz 3.10.0; o `Dockerfile` usa 3.9.12); `ubuntu-24.04` não muda sozinho quando o `-latest` migrar. O runner traz **Docker 28.0.4**, então o `@PayflowTest` (J7) sobe o `postgres:18` normalmente.
3. Usar `${{ github.event.pull_request.title }}` dentro do `run:` (substituição de texto antes do shell: o título `x"; echo PWNED; echo "` executa). Corrigido: `env: PR_TITLE: ${{ ... }}` e `bash check.sh "$PR_TITLE"`. O `actionlint` acusa o primeiro.
4. Cache: `~/.m2` restaurado pela chave do hash dos `pom.xml`; artefatos: relatórios na página da execução (retenção curta); resumo: Markdown no topo (cobertura 26/36 = 72,2%). A busca vai da branch do workflow para a **branch padrão**: a `main` precisa ter criado o cache.
5. **Medido**: cobertura real 72,2%, piso 0,70 (2 a 3 pontos abaixo, decisão 51). Violação: `Rule violated for bundle ...: lines covered ratio is 0.72, but expected minimum is 0.99` e `BUILD FAILURE`.
6. `dependency-review`: dependências **novas** vulneráveis introduzidas pela PR (público ou GHAS); `dependency-check`: o conjunto todo contra o NVD (lento, serviço externo, CVEs novas aparecem sem mudança: semanal); Dependabot: abre PRs de atualização. SHA: a *tag* pode ser movida, o SHA não. `pull_request_target` roda com segredos e escrita da base: executar código de PR ali é entregar os segredos.
7. EC2: IaaS; RDS: PaaS (gerenciado); GitHub: SaaS. RDS: AWS (hardware, SO, *patches* do motor, disponibilidade da infraestrutura) × você (versão escolhida, SG/acesso público, usuários, esquema, backup/retenção, TLS, dados). Região: localização independente com várias AZs; AZ: um ou mais data centers isolados na região.
8. Conta no plano Free → MFA na raiz (sem chaves de acesso) → **orçamento/alertas** → usuário IAM com MFA e `aws login` → só então recursos. O Identity Center (instância de organização) exige o **AWS Organizations**, e entrar nele **migra o plano Free para Paid**. O orçamento é **aviso com atraso**, não freio.
9. SG da EC2 **sem regras de entrada**; acesso por **SSM Session Manager** (túnel) e atualização por **SSM Run Command**; banco privado (`--no-publicly-accessible`), SG que só aceita o **SG da EC2**, TLS `verify-full` com o `global-bundle.pem`, senha no Parameter Store (SecureString), lida pelo papel da instância e entregue por arquivo ao container. IMDSv2: token por `PUT` antes de ler metadados (mitiga SSRF). `verify-full` valida cadeia **e nome** (`require` aceita qualquer certificado).
10. OIDC: o GitHub emite um token assinado; a AWS confere `aud` e `sub` da trust policy e devolve credenciais **temporárias**; `id-token: write` no job. `sub` em `repo:ORG/REPO:environment:production`; repositórios criados após 15/07/2026 podem ter **IDs imutáveis** no `sub`: imprima as claims. `deploy.sh`: IMDSv2 → segredos do SSM para **arquivos** (`umask 077`) → `chown 10001:10001` + `chmod 400` → login no ECR → `.env`/`.env.previous` → `up --wait` → se falhar, log, **volta** à imagem anterior (não exercitado) e sai com erro. Teardown: EC2 → RDS (sem snapshot final) → SGs (banco primeiro) → ECR/S3 → SSM → IAM; **idempotente**; `91-verify-empty.sh` prova (exit 0). Ainda cobra: EBS órfão, IP público, snapshots, logs, ECR/S3, recursos de outra região.
</details>

## Parte 2: Entrega prática (tag `payflow-J10`)
O `payflow` ganha **integração contínua**, **entrega por pipeline** e uma **implantação mínima na AWS**, com destruição provada.
```
payflow/
├── mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties       Maven 3.9.12 fixo                                   (J10.01)
├── .github/
│   ├── workflows/ci.yml · pr-title.yml · security.yml · cd.yml actions fixadas por SHA, permissions mínimas            (J10.01, J10.02, J10.06)
│   └── dependabot.yml                                          maven, github-actions, docker                       (J10.02)
├── scripts/ci/    check-pr-title.sh, coverage-summary.py, ssm-deploy.sh, check-secrets.sh
├── scripts/aws/   env.sh, 10-budget … 60-instance, 90-teardown, 91-verify-empty                                     (J10.04 a J10.07)
├── deploy/        compose.prod.yaml, compose.edge.yaml, Caddyfile, deploy.sh, user-data.sh, bootstrap-admin.{sh,sql}, iam/*.json
├── pom.xml        + piso do JaCoCo MEDIDO no PayFlow (decisão 51)
└── docs/ci-notes.md, cloud-notes.md, deploy-notes.md
```
Critérios de pronto:
- [ ] **CI:** `ci.yml`, `pr-title.yml` e `security.yml` verdes em uma PR; **um CI vermelho provocado** e corrigido (links anotados); `actionlint` e `shellcheck` limpos; *ruleset* da `main` exigindo PR e o check.
- [ ] **Cache e custo:** tempos frio × quente anotados; minutos usados no mês anotados; o piso do JaCoCo do PayFlow medido e registrado.
- [ ] **Segurança do pipeline:** *actions* por SHA conferido; nenhum `${{ github.event... }}` de texto livre no `run:`; segredo plantado detectado e removido; `NVD_API_KEY` cadastrada e o `dependency-check` rodado ao menos uma vez (⚠️ lento).
- [ ] **Conta:** plano Free, MFA na raiz, **sem** chave de acesso da raiz, orçamento com e-mail **testado**, `aws sts get-caller-identity` com o seu usuário.
- [ ] **Recursos:** `20-iam` a `60-instance` executados; SG da EC2 sem regra de entrada; RDS sem acesso público e criptografado; bucket com Block Public Access; ECR com tags imutáveis; todos com a tag `project=payflow`.
- [ ] **Deploy:** tag `v0.1.0` percorre `test` → `image` → aprovação → `deploy`; `sub` real impresso e usado; **nenhuma chave de acesso** guardada no GitHub.
- [ ] **Acesso e uso:** túnel SSM → `/actuator/health` `UP`; primeiro ADMIN criado e login feito (ou o resultado registrado, se o cookie `Secure` atrapalhar); `docker compose ps` na instância mostra a imagem esperada.
- [ ] **Reversão:** deploy quebrado de propósito e a volta à imagem anterior **provada** (ou registrada como falha).
- [ ] **Destruição provada:** `91-verify-empty.sh` **antes** (lista) e **depois** (exit 0); outras regiões e Cost Explorer conferidos; custo real do módulo anotado.
- [ ] **Testes existentes continuam como estavam** (esperado; **não executado**): o módulo não toca em código de negócio.
- [ ] Commits em Conventional Commits (`ci: ...`, `build: add maven wrapper`, `feat(deploy): ...`, `docs: ...`), PR mesclada e tag:
```bash
git tag payflow-J10
git push origin payflow-J10
```

## Parte 3: Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Escrevo um workflow que compila, testa e analisa a cada PR e o torno obrigatório | ☐ | ☐ | ☐ |
| Uso cache, artefatos e resumo de job; estimo os minutos do mês | ☐ | ☐ | ☐ |
| Protejo o pipeline (token mínimo, SHA, sem *script injection*, sem `pull_request_target`) | ☐ | ☐ | ☐ |
| Explico IaaS/PaaS/SaaS, região/AZ e a responsabilidade compartilhada | ☐ | ☐ | ☐ |
| Abro uma conta com MFA e **alerta de custos antes de tudo** e uso IAM com mínimo privilégio | ☐ | ☐ | ☐ |
| Crio EC2, RDS, S3 e Security Groups de forma reproduzível e privada | ☐ | ☐ | ☐ |
| Entrego o PayFlow por pipeline (OIDC, imagem versionada, segredos por arquivo, reversão) | ☐ | ☐ | ☐ |
| **Destruo tudo e provo**; reconheço o que ainda cobra | ☐ | ☐ | ☐ |
| Mapeio os serviços entre AWS, Azure e GCP | ☐ | ☐ | ☐ |

**Regra:** com mais de uma linha em 0, repita as aulas correspondentes antes do [Checkpoint de nível Júnior](../checkpoint-junior.md). Atualize `00-guia/progresso.md`.

## Próximo
[Checkpoint de nível Júnior](../checkpoint-junior.md)
