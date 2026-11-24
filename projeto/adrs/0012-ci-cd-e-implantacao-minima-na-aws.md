# ADR 0012 — CI no GitHub Actions e implantação mínima na AWS (EC2 + RDS), sem chave de acesso e sem porta aberta

- **Status:** aceita (J10, 2026-11-13). **Não houve conta AWS**: o laboratório (`lab/ci-lab`) foi executado com o AWS **emulado** (moto) e com PostgreSQL 18 local; o GitHub Actions de verdade, o RDS, a EC2 e o SSM **não** foram executados.
- **Contexto:** a J9 deixou uma imagem e um Compose de desenvolvimento; faltava (a) um portão automático de qualidade em cada PR e (b) um destino barato, reproduzível e **destruível**. Restrições: aluno iniciante, conta nova (plano Free), custo mínimo, segurança como critério de pronto.
- **Opções:** (1) deploy manual por SSH com chave; (2) pipeline com **chave de acesso** guardada nos *secrets*; (3) pipeline com **OIDC** + SSM, sem SSH; (4) PaaS (App Runner/Beanstalk); (5) ECS/Fargate + ALB (P9).
- **Decisão:**
  - **CI:** `./mvnw -B -ntp verify` (Maven 3.9.12 fixo) no `ubuntu-24.04`, `permissions: contents: read`, `concurrency`, `timeout-minutes`; *ruleset* exige PR e o check; título da PR em Conventional Commits (por `env:`, nunca `${{ }}` em `run:`); *actions* fixadas por SHA; `security.yml` (segredos, `dependency-review`, Dependency-Check semanal); Dependabot; sem `pull_request_target`.
  - **Nuvem:** AWS, `us-east-1` (estudo), plano Free; MFA na raiz → orçamento → usuário IAM com MFA + `aws login`; **sem** Organizations/Identity Center no plano Free; tag `project=payflow` em tudo.
  - **Implantação (opção 3):** EC2 `t3.small` (Amazon Linux 2023, IMDSv2 obrigatório, **sem entrada no SG, sem SSH**) com Docker Compose rodando **só a aplicação**; **RDS PostgreSQL 18** privado (SG aceita só o SG da EC2), TLS `verify-full`; **ECR** com tags imutáveis (tag = SHA do commit); **S3** privado com o pacote `deploy/` (expira em 7 dias); segredos no **SSM Parameter Store** (SecureString), entregues ao container por arquivos `chown 10001`/`chmod 400`; GitHub assume um papel por **OIDC** (`sub` do ambiente `production`, com aprovação) e dispara o `deploy.sh` por **SSM Run Command**; acesso do aluno por **túnel SSM**; Caddy/HTTPS com domínio é **opcional** (teto do corpo 64 KB).
  - **Primeiro usuário:** `bootstrap-admin` (hash BCrypt gerado fora do servidor), porque produção não tem seed.
  - **Destruição:** `90-teardown.sh` (com `CONFIRM`) + `91-verify-empty.sh`; `--skip-final-snapshot` e `--no-deletion-protection` **só** em estudo.
- **Consequências:**
  - (+) Nenhum segredo de longa duração no GitHub nem no repositório; sem porta de entrada na instância; deploy auditável e reversível; destruição provável.
  - (+) Cumpre promessas: título da PR (J7), Dependency-Check e `check-secrets` no CI (J8), testar antes da imagem (J9), teto do corpo na borda (decisão 63).
  - (−) **Sem alta disponibilidade** (uma instância, uma AZ) e com **alguns segundos de indisponibilidade** a cada deploy.
  - (−) O `SendCommand` roda como **root** na instância: o papel do GitHub é poderoso (mitigado por `sub` restrito, aprovação e condição de tag).
  - (−) O `sub` do OIDC mudou de formato para repositórios novos (IDs imutáveis): é preciso **conferir** o valor real.
  - (−) A reversão do `deploy.sh`, o `user-data` no AL2023, as políticas IAM e o login do painel pelo túnel (cookie `Secure`) **não foram exercitados**.
  - (−) Scripts `*.sh` não têm estado: o Terraform (P8) os substitui.
- **Revisão prevista:** P8 (Terraform), P9 (ECS/Fargate, ALB, Multi-AZ, Secrets Manager), P10 (OIDC/Keycloak), S6 (custo e Well-Architected).
