# Laboratório da J10

Nada aqui exige conta AWS. O que **foi executado** (Linux, Docker 29.9.2, JDK 25.0.5, Maven Wrapper 3.9.12, AWS CLI 2.39.5): `./mvnw verify` do `ci-lab`, o `actionlint` e o `shellcheck`, o `check-pr-title.sh`, o `deploy.sh` (com PostgreSQL 18 em TLS e `sslmode=verify-full`), o Caddy (`deploy/Caddyfile`) e a sequência `scripts/aws/*.sh` contra o **moto** (emulador de AWS: prova a sintaxe e a ordem, **não** o comportamento da AWS). O que **não** foi executado: o GitHub Actions de verdade, a OIDC, a SSM, o RDS e a EC2 reais.

| Item | O que é |
|---|---|
| `ci-lab/` | O PayFlow Lite da J9 + testes (Testcontainers `postgres:18`, JaCoCo com piso 0,70), Maven Wrapper, `.github/workflows/*.yml`, `scripts/ci/`, `scripts/aws/`, `deploy/` |
| `ci-lab/.github/workflows/` | `ci.yml`, `pr-title.yml`, `security.yml`, `cd.yml` (actions fixadas por SHA) |
| `ci-lab/scripts/aws/` | `10-budget` … `60-instance` (criam), `90-teardown` e `91-verify-empty` (destroem e provam) |
| `ci-lab/deploy/` | `compose.prod.yaml`, `compose.edge.yaml` + `Caddyfile`, `deploy.sh`, `user-data.sh`, `bootstrap-admin.*`, `iam/*.json` |

```bash
cd ci-lab && export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && ./mvnw -B -ntp verify     # 4 testes, JaCoCo 72,2% (piso 0,70)
docker run --rm -v "$PWD":/repo -w /repo rhysd/actionlint:1.7.12 -no-color .github/workflows/*.yml
```
**Não rode** os `scripts/aws/*.sh` contra a sua conta sem ler as aulas J10.04 a J10.07: eles **criam recursos que cobram** (💸).
