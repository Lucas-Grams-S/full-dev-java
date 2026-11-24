# Laboratório do S5 — Segurança avançada

Programas, scripts e **saídas reais** usadas nas aulas. **Tudo é um modelo ou um laboratório descartável** (CA e certificados criados na hora, PostgreSQL 18 num contêiner, advisories sintéticos), **não o PayFlow**.

## Requisitos
- **JDK 25** (`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`; o `java` padrão da máquina pode ser o 17 e **muda o resultado**: no JDK 17 a requisição em conexão reaproveitada levou ~42 ms, no 25 levou ~0,5 ms). `keytool` vem com o JDK.
- **Docker** só para `run-roles.sh` (PostgreSQL 18), `run-trivy.sh` (Trivy 0.76.2), `run-promtool.sh` (`prom/prometheus`) e o `actionlint` (`rhysd/actionlint`).
- Python 3 com PyYAML só para `check-waivers.py` e `trivy-summary.py`.
- Nada de Maven.

## Como rodar
```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
./run-all.sh                 # regrava os out-*.txt dos programas Java (s1…s7); ONLY=s2 ./run-all.sh roda um só
./run-roles.sh > out-s501-roles.txt 2>&1   # papéis do banco e trilha append-only (PostgreSQL 18 em Docker)
./run-promtool.sh > out-s506-promtool.txt 2>&1   # promtool: check + test + 2 mutantes
ONLY=iac ./run-trivy.sh      # sca | secret | iac  (o primeiro uso baixa o banco de vulnerabilidades: ~1,7 GB em ~/.cache/s5-trivy)
python3 -I check-waivers.py samples/waivers-bad.yml 2026-11-23
```

## Programas e saídas
| Arquivo | Aula | Saídas | O que mostra |
|---|---|---|---|
| `DefenseLab.java` | S5.01 | `out-s501-layers.txt`, `out-s501-policy.txt` | camadas independentes × causa compartilhada (Monte Carlo); política de autorização com 16 casos e **3 mutantes** |
| `roles.sql`, `roles-*.sql`, `run-roles.sh` | S5.01, S5.06 | `out-s501-roles.txt` | menor privilégio no PostgreSQL 18: app sem `UPDATE`/`DELETE` no ledger, função de expurgo com piso, leitura por coluna, `audit_events` *append-only*, *break-glass* com `VALID UNTIL` |
| `MtlsLab.java` | S5.02 | `out-s502-cases.txt`, `out-s502-cost.txt` | **mTLS real** (CA e folhas do `keytool`, `HttpsServer` com `needClientAuth`, `HttpClient`): 11 casos, parâmetros negociados, validade; custo do *handshake* |
| `CryptoLab.java` | S5.03 | `out-s503-envelope.txt`, `-nonce.txt`, `-blind.txt`, `-cost.txt` | criptografia envelope AES-256-GCM com um KMS **falso**, contexto, adulteração, rotação por *re-wrap*, reuso de nonce, índice cego, custo |
| `PipelineLab.java`, `samples/sast/` | S5.04 | `out-s504-sast.txt`, `-dast.txt`, `-sca.txt`, `-policy.txt` | SAST por regras de linha (verdadeiros, falsos positivos/negativos), DAST contra um app local, SCA com advisories **sintéticos**, portão com isenções que vencem |
| `run-trivy.sh`, `samples/trivy-*` | S5.04 | `out-s504-trivy-*.txt` | **Trivy 0.76.2 de verdade**: SCA (`log4j-core 2.14.1`), segredos, Dockerfile e Terraform |
| `check-waivers.py`, `samples/waivers-*.yml`, `samples/workflows/security.yml` | S5.04 | `out-s504-waivers.txt`, `out-s504-actionlint.txt` | verificador de isenções (bom passa, ruim falha) e `actionlint` no *workflow* (com mutante) |
| `prometheus/audit-rules.yml` + `.test.yml`, `run-promtool.sh` | S5.06 | `out-s506-promtool.txt` | 2 alertas sobre `payflow_audit_events_total` (rajada de recusas com **piso de tráfego**; uso de *break-glass*); **5 testes**; **2 mutantes** (sem o piso; `for` encurtado) |
| `PiiLab.java` | S5.05 | `out-s505-luhn.txt`, `-scrub.txt`, `-cpf.txt`, `-inventory.txt` | Luhn × falsos positivos, mascarador de logs, dígitos do CPF e pseudônimo HMAC, inventário de dados com *linter* |
| `AuditLab.java` | S5.06 | `out-s506-chain.txt`, `out-s506-lint.txt` | cadeia de hash/HMAC *tamper-evident* × 6 ataques (com e sem âncora externa); *linter* de evento de auditoria |
| `ReviewLab.java` | S5.07 | `out-s507-score.txt`, `-lint.txt`, `-coverage.txt` | matriz de severidade, *linter* de relatório (auto-teste) e cobertura de escopo STRIDE |

## O que NÃO é
Não é o PayFlow, não é a AWS (o KMS é um **objeto Java**; certificados e CA são descartáveis), **não é medição de produção** e não substitui Semgrep/CodeQL/ZAP. Cada aula diz o que é medido, o que é modelo e o que **não** foi executado (ZAP, AWS KMS/CloudTrail/ACM PCA/ALB mTLS, Keycloak com mTLS, PayFlow do aluno).

## Limpeza
O banco de vulnerabilidades do Trivy (~1,7 GB em `~/.cache/s5-trivy`) **foi removido** ao fim da geração do módulo: o primeiro `./run-trivy.sh` o baixa de novo (alguns segundos aqui) e **você** o apaga quando terminar. Os contêineres (`lab-s5-pg`) se removem sozinhos; `run-promtool.sh` e o `actionlint` usam `--rm`; as pastas temporárias do `MtlsLab` são apagadas ao fim. **Atenção:** rodar o Trivy numa pasta com `pom.xml` o faz consultar o Maven Central (levei `HTTP 429`); por isso o SCA está em `samples/trivy-sca` e `ONLY=sca` roda **sozinho**.
