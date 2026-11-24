# Laboratório da P10 — Segurança intermediária

Tudo aqui foi **executado** (Linux, Docker 29.9.2, JDK 25.0.5, Spring Boot 4.1.2). Os `out-*.txt` são as **saídas reais** citadas nas aulas.

| Pasta | O que é | Como rodar |
|---|---|---|
| `keycloak-lab/` | Keycloak 26.8.2 em Docker; o **realm como código** (`payflow-realm.json` + `kc-setup.py`) | `./kc-up.sh` (sobe, espera, cria escopos) · `./kc-down.sh` |
| `identity-lab/` | O PayFlow (versão mínima, em memória) como **Resource Server** + cliente `client_credentials` + testes (23 sem Keycloak, 31 com Keycloak/PostgreSQL) + `run-mutants` + `outage.sh` | `mvn test` (os `*IT` exigem `kc-up.sh` e `pg-up.sh`) |
| `secrets-lab/` | PostgreSQL 18 só para a rotação de credenciais | `./pg-up.sh` · `./pg-down.sh` |
| `supply-chain-lab/` | SBOM (Maven e imagem), Trivy, cosign, registro local, portão de *deploy*, build reprodutível | `./run-supply-chain.sh` · `./repro-check.sh` |
| `threat-model/` | `threats.csv` (26 ameaças) + `check-threat-model.py` | `python3 check-threat-model.py --self-test` |
| `run-all.sh` | regrava **todos** os `out-*.txt` (sobe e remove os contêineres `lab-p10-*`) | `./run-all.sh` · `ONLY=tests\|mutants\|supply\|outage\|threat` |
| `run-mutants.py` | estraga **uma** regra de segurança por vez numa cópia e confere que a suíte **falha** | `python3 run-mutants.py` |

## Requisitos
- JDK 25 (`JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`), Maven 3.9, Docker, `jq`, Python 3.
- Imagens: `quay.io/keycloak/keycloak:26.8.2`, `postgres:18`, `ghcr.io/aquasecurity/trivy` (fixada por digest no *script*), `ghcr.io/sigstore/cosign/cosign:latest` (⚠️ deveria estar fixada), `registry:2`.
- O Trivy usa o cache `~/.cache/p8-trivy` (criado na P8).

## ⚠️ Só do laboratório
Senhas e `client_secret` com `lab-only`/`-pass-` no nome, o administrador `admin/lab-admin-only`, a porta **8180** do Keycloak e o cliente `payflow-lab-direct` (senha direta, **só** para testes automatizados) **não** existem em produção.

## Limpeza
`run-all.sh` remove os contêineres `lab-p10-*`, o `target/` e os arquivos temporários. Sobram as **imagens** baixadas (`docker image rm` se quiser).
