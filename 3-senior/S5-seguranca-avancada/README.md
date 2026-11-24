# S5 — Segurança avançada

> 🟣 Sênior · ⏱️ 12h (8 sessões de 1h30) · Status: ✅ concluído (2026-11-23) — **validado na execução final (2026-11-24); veja o bloco abaixo**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído em `~/docs/payflow`, tag `payflow-S5`).** **Executado:** `run-all.sh` numa cópia (~47 s: mTLS **real** com `keytool`, envelope com KMS falso, SAST/DAST/SCA em miniatura, cadeia de HMAC, papéis e trilha *append-only* no PostgreSQL 18, **Trivy 0.76.2** real, `promtool`/`actionlint` com mutantes): **27 de 29 saídas idênticas**; a diferença é a **mensagem do TLS 1.3** no caso 9 (`SSLHandshakeException` × `SocketException: Connection reset`; a aula já avisa que ela varia) e a taxa do CPF (3,93 × 3,89 milhões/s). **Corrigido nas aulas:** nenhuma divergência de conteúdo. **Ainda não executado:** AWS (KMS, CloudTrail), ZAP/Semgrep/CodeQL, Keycloak com mTLS, o PayFlow com `AuthorizationPolicy`, papéis do banco e V25 `audit_events` (o DDL só aparece como trecho), e o texto da LGPD/PCI (não conferido). Os avisos "não executado" / "revisado por leitura" nas aulas descrevem a escrita original.

## 🎯 Objetivo do módulo
**Aplicar defesa em profundidade e conduzir revisões de segurança**: **contar causas compartilhadas** (não caixas) e traduzir **zero trust** em **política de autorização testada com mutantes**, **papéis de banco com menor privilégio** e ***break-glass*** com prazo; **decidir mTLS** (identidade do canal, **sem confundir com autorização**) com **gatilhos e rotação**; **cifrar** em trânsito, em repouso e **por campo** com **criptografia envelope e KMS** (contexto, rotação por *re-wrap*, **o reuso de *nonce*** e a **chave do *backup***); **pôr SAST, DAST e SCA no pipeline** com **portão que expira isenções** e saber **o que cada ferramenta não vê**; **traduzir LGPD e PCI-DSS em requisitos técnicos** (inventário, eliminação, **3 dias úteis**, **não tocar no PAN**); **auditar com trilha *tamper-evident*** (cadeia de HMAC e **âncora externa**) e **alertas testados**; e **conduzir uma revisão** com escopo, evidência, régua de severidade e **cobertura**. O fio é **provar que cada verificação pode falhar** (mutantes, casos negativos, auto-testes) e **dizer o que não foi coberto**.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [S5.01](S5.01-defesa-em-profundidade-e-zero-trust.md) | Defesa em profundidade e zero trust | 1h30 |
| [S5.02](S5.02-mtls-entre-servicos.md) | mTLS entre serviços | 1h30 |
| [S5.03](S5.03-criptografia-em-transito-e-em-repouso-kms.md) | Criptografia em trânsito e em repouso; KMS | 1h30 |
| [S5.04](S5.04-sast-dast-e-sca-no-pipeline.md) | SAST, DAST e SCA no pipeline | 1h30 |
| [S5.05](S5.05-lgpd-e-nocoes-de-pci-dss.md) | LGPD e noções de PCI-DSS | 1h30 |
| [S5.06](S5.06-auditoria-e-trilhas-de-acesso.md) | Auditoria e trilhas de acesso | 1h30 |
| [S5.07](S5.07-como-conduzir-uma-revisao-de-seguranca.md) | Como conduzir uma revisão de segurança | 1h30 |
| [S5.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega, autoavaliação e simulado) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): **7 programas de Java puro** (`DefenseLab`, `MtlsLab`, `CryptoLab`, `PipelineLab`, `PiiLab`, `AuditLab`, `ReviewLab`), **scripts de Docker** (`run-roles.sh` com PostgreSQL 18, `run-trivy.sh` com o Trivy, `run-promtool.sh` com as regras de auditoria), `check-waivers.py`, o *workflow* `security.yml` conferido com `actionlint`, o `run-all.sh` e as **saídas reais** (`out-s50*.txt`).

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório foi executado de verdade** (JDK **25.0.5**, Linux, **12 núcleos, máquina compartilhada**) e **as saídas das aulas são as reais**; os **milissegundos mudam a cada execução** (e **as etiquetas HMAC do índice cego mudam**: a chave é aleatória). **São modelos e laboratórios descartáveis, não o PayFlow:** o **KMS é um objeto Java**; a CA do mTLS e os certificados são **descartáveis**; as **vulnerabilidades do SCA em miniatura são SINTÉTICAS** (`SYN-*`; o Trivy usa **CVEs reais**, impressas pela ferramenta); os **achados do relatório de exemplo e do simulado são FICTÍCIOS**; os **números de cartão são os de teste públicos** e os **CPFs são gerados** pelo algoritmo. **Não executado:** **AWS** (KMS, CloudTrail, RDS, S3 Object Lock, ACM PCA, ALB mTLS, IAM/SSO), **Keycloak** com mTLS, **ZAP/Semgrep/CodeQL**, o **GitHub Actions** real, **teste de invasão** e **o PayFlow do aluno**. **O texto da Lei 13.709/2018 (o `curl` não alcançou o Planalto) e o do PCI DSS (exige aceite) NÃO foram conferidos**; **o prazo de 3 dias úteis** foi conferido na página da ANPD. **Isto é material técnico, não parecer jurídico.** **Papéis, políticas, SLAs e números do PayFlow são PROPOSTAS.**

> **Avisos de ordem:** a S5.01 **fecha** o `DELETE` sem `WHERE` e o acesso humano da S4.05 (menor privilégio, função com piso, *break-glass*) e **estende** os papéis da P10.05; a S5.02 **retoma** o `/internal/**` da P10.04 (token × canal) e a S5.03 **fecha** a **chave do *backup*** da S4.05; a S5.04 **estende** o *scan* de imagem/SBOM da P8.02/P10.06 e usa o `actionlint` da P8; a S5.05 **aprofunda** a LGPD básica da J8.07 e retoma o *heap dump* (S2.04) e o *backup* (S4.05); a S5.06 **usa** o piso de tráfego e o `promtool` da S4.01 e o Outbox da P3; a S5.07 **fecha** o STRIDE da P10.07.

## 🏗️ Entrega no PayFlow
**Tag `payflow-S5`** (por leitura): **`AuthorizationPolicy` + teste com mutantes**, **papéis do banco** (`roles.sql`, função `purge` com piso, *break-glass*), **`audit_events` (V25)** *append-only* com `AuditService` e *linter*, **alertas de auditoria testados**, **decisão de mTLS** (hoje não, com gatilhos) e *runbook* de expiração, **tabela de criptografia** (chave do *backup* na conta de *backup*, Object Lock para âncoras), **`security.yml`** + isenções com validade, **inventário LGPD, eliminação e plano de incidente (3 dias úteis)**, **mascarador de logs com teste**, **`revisao-s5.md`**, o **checklist PCI/LGPD** e a **ADR 0028**. **Nada foi compilado nem executado no PayFlow.**

> **Migrações:** **uma**, **V25** (`audit_events`, **por leitura**; o DDL **foi executado** no PostgreSQL 18 do laboratório); a **V26** fica **livre** (V18–V24 seguem reservadas pela S3). O contrato HTTP `/api/v1` **não muda**.

## ➡️ Próximo
[S6 — Arquitetura em nuvem e custo](../S6-arquitetura-nuvem-custo/README.md)
