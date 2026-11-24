# S5.CP — Checkpoint do módulo S5

> ⏱️ ~1h30 · 🟣 Sênior · Pré-requisitos: S5.01 a S5.07

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, JDK 25.0.5, **máquina compartilhada de 12 núcleos**): `lab/DefenseLab.java` (camadas e política com mutantes), `lab/MtlsLab.java` (**mTLS real** com certificados do `keytool`), `lab/CryptoLab.java` (envelope, *nonce*, índice cego), `lab/PipelineLab.java` (SAST/DAST/SCA em miniatura, portão), `lab/PiiLab.java`, `lab/AuditLab.java`, `lab/ReviewLab.java`; **Docker:** **PostgreSQL 18** descartável (`run-roles.sh`: papéis, função de expurgo, trilha *append-only*, *break-glass*), **Trivy 0.76.2** (dependências, segredos, Dockerfile, Terraform), **`promtool` 3.16.0** (regras de auditoria, 5 testes, 2 mutantes) e **`actionlint` 1.7.12** (com mutante). **NÃO executados:** o **PayFlow do aluno**, **AWS** (KMS, CloudTrail, RDS, S3 Object Lock, ACM PCA, ALB mTLS, IAM/SSO), **Keycloak** com mTLS, **ZAP/Semgrep/CodeQL**, **GitHub Actions** real, **teste de invasão**, qualquer incidente real. **Todo número do PayFlow é PROPOSTA ou "a medir"**; o cenário do simulado é **SINTÉTICO**. **Não é parecer jurídico.**

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Com cinco camadas que **deixam passar 30%** cada, qual a chance de um ataque atravessar **se forem independentes**? E se **uma causa comum** de **1%** derruba todas ao mesmo tempo? O que isso diz sobre **contar caixas** no diagrama?
2. O que **zero trust** é e **não** é? Na tabela de política do laboratório (16 casos), **o que cada um dos 3 mutantes removeu** e quantos casos falharam? Por que **a tabela sozinha não basta** para um endpoint novo?
3. Como o desenho de papéis **fecha o `DELETE` sem `WHERE`** da S4.05? O que a **função de expurgo** recusa? E qual foi a **armadilha da imagem do PostgreSQL** que quase deu um falso positivo no teste do *break-glass*?
4. No laboratório de **mTLS**, por que o `reporting` (certificado **válido**) recebeu **403**? Por que o certificado do atacante com o **mesmo `CN=api`** não entra? Por que **não** se deve comparar a **mensagem de erro** do cliente em um teste?
5. Cite **três erros de PKI** que quebram em produção e **a decisão proposta** para o PayFlow sobre mTLS interno (**com os gatilhos** que a mudariam).
6. Na **criptografia envelope** do laboratório: o que **impediu** mover o telefone de uma linha para outra? **O que muda e o que não muda** ao **rotacionar** a chave-mestra, e **quantas chamadas** ao KMS custam 1.000 registros? O que o **reuso de *nonce*** permitiu fazer **sem a chave**?
7. O **índice cego** do telefone vazou o quê (700/200/100)? Por que o PayFlow **não** cifra `phone_e164` por campo **agora**?
8. Nas ferramentas do pipeline: o que o **SAST de miniatura** **não** viu no `Tricky` e o que viu a mais (falso positivo)? Que **duas coisas** o **Trivy** **não** pegou nas amostras? Por que o portão deu **PASS** em 23/11 e **FAIL** em 20/12 com o mesmo código?
9. **LGPD e PCI:** qual o **prazo** de comunicação de incidente à ANPD e quais os **três critérios**? Quanto do **Luhn** passa em strings **aleatórias** e quanto passa com **prefixo + tamanho**? Quantas linhas **com cartão** o mascarador **deixou vazar**? Quanto tempo leva **inverter um SHA-256 de CPF** (e o que **resolve**)?
10. **Auditoria e revisão:** quais dos **6 ataques** à cadeia de HMAC **só a âncora externa** detecta? O que o **dono do banco** conseguiu fazer com o gatilho *append-only*? Na matriz de cobertura do laboratório, **quantas células** estavam em silêncio e por que a revisão **não pode terminar**?

<details><summary>Gabarito</summary>

1. Independentes: `0,3⁵ = 0,243%` (simulado **0,2423%**). Com causa comum de 1%: `0,01 + 0,99 × 0,243% ≈ 1,24%` (simulado **1,2384%**): **cinco vezes pior**; com 5%, **~5,2%**. **Conte as causas compartilhadas** (a mesma credencial, rede, pessoa, configuração), **não as caixas**.
2. Zero trust (NIST SP 800-207): **sem confiança implícita por rede**, **autenticar e autorizar cada acesso**, **menor privilégio**, **assumir a violação**; **não** é produto e **não** é tirar o *firewall*. **M1** (sem checar o dono) → **3** casos falham; **M2** (permitir por padrão) → **4**; **M3** (admin sem ticket/MFA) → **2**. A tabela cobre **a função**: falta provar que **todo endpoint a chama** (teste de arquitetura/ArchUnit, teste de fatia com 2 usuários).
3. A **aplicação não tem `DELETE`** em `idempotency_keys`; o expurgo é uma **função `SECURITY DEFINER`** com **piso** (`older_than < 24 h` → `refusing to purge keys younger than 24 hours`): **ninguém pede "apague tudo"**. O ledger **não** tem `UPDATE`/`DELETE`/`TRUNCATE` para a app (`permission denied`). **Armadilha:** a imagem oficial deixa `127.0.0.1` em **`trust`** no `pg_hba.conf` → a senha **não era conferida** e o login "depois da janela" passava; o laboratório agora conecta **pelo IP do contêiner**.
4. **Autenticado ≠ autorizado**: o `reporting` tem certificado **válido da mesma CA**, mas **não está na lista** de `/internal/ping` → **403**. O atacante com `CN=api` de **outra CA**: o servidor valida a **cadeia até uma CA confiável**, e o `CN` é **texto que o atacante escolhe**. A **mensagem do cliente varia** entre execuções (`certificate_required`/`certificate_unknown` × `Connection reset`) no TLS 1.3: asserte "**a conexão falhou**".
5. (a) **`CN` em vez de SAN** (falha de nome); (b) **truststore com as CAs do sistema** (confia no mundo); (c) **`needClientAuth` esquecido** (aceita sem certificado); (d) **certificado de vida longa** ou **expiração sem alerta**. **Decisão proposta:** **não adotar** mTLS interno **agora** (token por serviço, ALB, mesma conta); **gatilhos:** vários times, rede não confiável (outra região/conta/terceiro), exigência de conformidade/parceiro, malha por outro motivo, **incidente de token vazado**.
6. O **contexto** (`tabela|id|coluna`) é **AAD** do GCM: outro contexto → `AEADBadTagException: Tag mismatch`. **Rotação:** **reembrulha as DEKs**; **os dados não mudam** (o *digest* é idêntico); a KEK antiga **continua** até todas serem reembrulhadas; **2.000 chamadas** (1 desembrulhar + 1 embrulhar × 1.000). **Reuso de *nonce*:** `(c1 xor c2) xor p1` **recuperou** `p2` **sem a chave**. **NIST SP 800-38D §8.3:** ≤ **2³²** chamadas por chave com IV aleatório.
7. Mostrou **quem compartilha o valor e quantos** (3 etiquetas: 700, 200 e 100), **sem a chave**. **Não cifrar** o `phone_e164` por campo agora: perde **busca/índice** (S3.09), exige índice cego **que vaza distribuição** e **adiciona o KMS na rota de leitura**; **proteção atual:** volume cifrado + **leitura por coluna** + auditoria. **CPF**, se coletado, é o candidato.
8. **Falso negativo:** o SQL montado em **uma linha** e usado em **outra** (precisa de *taint tracking*); **falso positivo:** `PASSWORD_FIELD = "password"` (um **nome de campo**) foi marcado como segredo. **Trivy:** **não** reclamou da **porta 5432 aberta para `0.0.0.0/0`** (a regra `AWS-0107` é de **SSH/RDP**) e achou **2 de 5** valores com cara de segredo (**não** viu a chave secreta AWS inventada; ignorou as duas chaves "EXAMPLE"). **Portão:** as **isenções vencem** (`SYN-0001` em 15/12, `SYN-0011` em 10/12): hoje **PASS**, depois **FAIL (2)**.
9. **3 dias úteis** (Res. CD/ANPD nº 15/2024), quando o incidente (1) tem **ocorrência confirmada**, (2) envolve **dados pessoais da LGPD** e (3) pode causar **risco ou dano relevante** (**cumulativos**); **comunicar a ANPD não basta**: avise o **titular**. **Luhn:** **~10%** de strings aleatórias; **Luhn + prefixo + tamanho 13–19:** **~2,2%**. **Mascarador:** deixou vazar **2 das 7 linhas com cartão** (número **partido** e **codificado**). **SHA-256 de CPF:** ~**4 min num núcleo** (~3,9 milhões/s medido; extrapolação); **HMAC com chave no KMS** resolve.
10. **A4** (cortar os 50 últimos eventos) e **A6** (quem tem a chave refaz a cadeia): **só a âncora** externa (contagem + *head*) detecta (em 950 e 1.000). **O dono** do banco **desligou o gatilho** (`DISABLE TRIGGER`) e **apagou** a trilha (`0` linhas): **precisa de cópia fora do alcance**. **Cobertura:** **4 de 36 células `??`** (*broker*/repúdio, *pipeline*/vazamento e negação de serviço, *backups*/repúdio) → **FAIL**: **"não achamos nada" só vale com o escopo coberto**.
</details>

## Parte 2: Entrega prática (tag `payflow-S5`)
O `payflow` ganha **controles e artefatos de segurança avançada**: política de autorização testada, papéis do banco, trilha de auditoria (**migração V25**), alertas de auditoria, critérios de **mTLS** (decisão), **criptografia** (tabela, chaves), **pipeline de segurança**, **LGPD/PCI** (inventário, eliminação, incidente, checklist) e a **revisão** do módulo, com a **ADR 0028**. **Nada foi compilado nem executado no PayFlow**; o laboratório `3-senior/S5-seguranca-avancada/lab` é o **modelo executado**. **Uma migração: V25** (`audit_events`); a **V26** fica livre; o contrato HTTP `/api/v1` **não muda**.

```
payflow/
├── src/main/java/com/payflow/security/AuthorizationPolicy.java          decide(chamador, ação, dono)       (S5.01)
├── src/test/java/com/payflow/security/AuthorizationPolicyTest.java      16+ casos + mutantes (CI)          (S5.01)
├── src/main/java/com/payflow/audit/AuditService.java                    REQUIRES_NEW / Outbox; só INSERT   (S5.06)
├── src/test/java/com/payflow/audit/AuditEventLintTest.java              rejeita PAN/CPF/e-mail como ator   (S5.06)
├── src/main/resources/db/migration/V25__audit_events.sql                tabela + gatilhos + GRANT          (S5.06)
├── src/main/java/com/payflow/logging/PanMaskingConverter.java (+ teste com mutante)                        (S5.05)
├── db/roles/roles.sql                                                   papéis e GRANTs; função purge      (S5.01)
├── observability/prometheus/audit-rules.yml + audit-rules.test.yml      2 alertas, 5 testes, 2 mutantes    (S5.06)
├── docs/runbooks/ audit-denied-spike.md, break-glass-review.md, certificado-expirou.md                     (S5.02/06)
├── docs/security/
│   ├── papeis-do-banco.md      tabela de papéis, política do break-glass                                   (S5.01)
│   ├── mtls.md                 decisão (hoje não), gatilhos, runbook de expiração                          (S5.02)
│   ├── criptografia.md         dado × trânsito × volume × campo; chave do backup; rotação                  (S5.03)
│   ├── pipeline.md             o que bloqueia, SLA, isenções, onde roda cada coisa                         (S5.04)
│   ├── auditoria.md            vocabulário, DDL V25, selador, âncora, retenção, alertas                    (S5.06)
│   ├── revisao-s5.md           escopo 36 células, relatório H1–H8 reclassificado, "Não coberto"            (S5.07)
│   └── checklist-lgpd-pci.md   o checklist abaixo, com a evidência de cada item
├── docs/lgpd/ inventario.md, eliminacao.md, incidente.md                                                  (S5.05)
├── security/ waivers.yml, check-waivers.py, zap-baseline.conf                                              (S5.04)
├── infra/terraform/ kms.tf (chave da app e chave do backup NA CONTA DE BACKUP), rds storage_encrypted, S3 Object Lock p/ âncoras   (S5.03/06)
└── .github/workflows/security.yml                                                                          (S5.04)
projeto/adrs/0028-seguranca-avancada-do-payflow.md   (já redigida nesta geração)
```
**Checklist PCI/LGPD** (`docs/security/checklist-lgpd-pci.md`: **cada item com a evidência**: arquivo, teste ou saída, **ou "pendente"**):
| # | Item | Aula |
|---|---|---|
| 1 | Inventário de dados pessoais com **base legal, retenção, dono**; *linter* passa e **falha** na cópia quebrada | S5.05 |
| 2 | Fluxo de **eliminação** (banco, Keycloak, Redis, logs, *backups*, *exports*) e **teste de invariantes** | S5.05 |
| 3 | Plano de incidente com **3 dias úteis**, **DPO** e critério de **risco relevante** | S5.05 |
| 4 | **PAN não toca o PayFlow** (campos hospedados do PSP) **ou** escopo PCI **declarado** | S5.05 |
| 5 | **Mascarador** de logs **com teste e mutante**; **allow-list** de campos no log | S5.05 |
| 6 | **Cifra em trânsito** `verify-full` no banco, TLS no Redis/broker; nada de `TrustAll` | S5.03 |
| 7 | **Volume cifrado** (RDS, S3, *backups*) com **chave** definida **antes** de criar | S5.03 |
| 8 | **Cifra por campo** só onde a **ameaça** justifica (**tabela** com ameaça por linha) | S5.03 |
| 9 | **Chave do *backup*** em **outra conta**, política que **nega exclusão**, **alarme**, **restauração ensaiada** | S5.03/S4.05 |
| 10 | Papéis do banco: **app sem `UPDATE`/`DELETE` no ledger**, **migrador só no pipeline**, **leitura por coluna** | S5.01 |
| 11 | **Função de expurgo com piso** no lugar de `DELETE` da app | S5.01 |
| 12 | ***Break-glass*** com ticket, **dois olhos**, **prazo**, **revisão posterior** | S5.01 |
| 13 | **Tabela de política** de autorização **com mutantes** e **teste de arquitetura** (todo endpoint a chama) | S5.01 |
| 14 | **mTLS**: decisão escrita com **gatilhos** (e, se adotado, **alerta de expiração** testado) | S5.02 |
| 15 | **Pipeline**: segredos/IaC/SAST/SCA no PR, **SCA agendado**, DAST **só em teste**, **isenções com validade** | S5.04 |
| 16 | **`audit_events`** (V25) *append-only*, app **só `INSERT`**, `DENIED` gravado com *rollback* | S5.06 |
| 17 | **Âncora externa** (Object Lock, outra conta) e **chave do HMAC fora do banco** | S5.06 |
| 18 | **Alertas de auditoria** com **piso de tráfego**, **testes e mutantes**; *runbook* de cada um | S5.06 |
| 19 | **Revisão** `revisao-s5.md`: **0 células `??`**, achados com **evidência/dono/prazo**, **"Não coberto"** | S5.07 |
| 20 | **Lacunas registradas**: **Pix/regulação do BCB** e o texto oficial da **LGPD/PCI** (a conferir) | S5.05 |

Critérios de pronto:
- [ ] **Política:** a tabela de casos **passa** e **cada mutante** (dono, *deny by default*, *break-glass*, MFA) **falha** pelo menos 1 caso; há **teste de arquitetura** que reprova *controller* sem checagem.
- [ ] **Banco:** `roles.sql` revisado; **nenhum papel humano** com escrita **permanente**; ledger **sem** `UPDATE`/`DELETE` para a app; **`purge`** com piso.
- [ ] **Auditoria:** V25 com **gatilhos e `GRANT`**, **teste** de que o `DENIED` é gravado com a transação revertida, **linter** de evento, **selador e âncora** descritos, **retenção** com o jurídico, alertas com **teste e mutante**.
- [ ] **Criptografia:** tabela por dado **com ameaça**, **chave do *backup* na conta de *backup***, rotação **calculada** (chamadas e tempo), **ensaio** de restauração **com a chave de destino**.
- [ ] **mTLS:** decisão com **gatilhos**; se adotada, **runbook** de expiração e **alerta** testado.
- [ ] **Pipeline:** `security.yml` passa no `actionlint`; **isenções** validadas pelo `check-waivers.py` (**caso ruim falha**); SCA **agendado**; **um caso que o pipeline não pega** descrito, com **quem cobre**.
- [ ] **LGPD/PCI:** inventário, eliminação, incidente (**3 dias úteis**), mascarador testado; **PAN fora** do PayFlow.
- [ ] **Revisão:** `revisao-s5.md` com **0 `??`** e **seção "Não coberto"**; **ADR 0028** revisada; tag `payflow-S5`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Sênior: **≥ 2** em todos e **3** nos marcados com ⭐.
| Competência (matriz) | Evidência no módulo | 0–3 |
|---|---|---|
| ⭐ **Segurança:** conduz revisão de segurança, mTLS, KMS, conformidade (LGPD/PCI) | S5.01–S5.07 (camadas, mTLS, envelope e chaves, pipeline, LGPD/PCI, auditoria, revisão) | |
| ⭐ **Arquitetura e design:** decide camadas e trade-offs (causa comum, custo operacional, escopo PCI) | S5.01, S5.02, S5.03, S5.05 (a decisão **"não adotar agora"** com gatilhos) | |
| ⭐ **Testes e qualidade:** testes que **podem falhar** (mutantes, caso negativo, auto-teste do linter) | S5.01, S5.04, S5.05, S5.06, S5.07 | |
| **Banco de dados:** papéis, `GRANT` por coluna, funções com piso, trilha *append-only* | S5.01, S5.06 | |
| **Observabilidade e operação:** alertas de auditoria testados, *runbooks*, expiração de certificados | S5.02, S5.06 | |
| **Nuvem e custo:** KMS, chaves entre contas, Object Lock, CloudTrail (**leitura**) | S5.03, S5.06 | |
| **Liderança e comunicação:** relatório sem culpa, severidade por régua, aceite com prazo, 3 dias úteis | S5.05, S5.07 | |
| **Performance:** custo de *handshake* e de chamadas ao KMS (medir antes de afirmar) | S5.02, S5.03 | |
| **Sistemas distribuídos:** identidade entre serviços, tokens × canal, causa comum | S5.01, S5.02 | |
Se **≥ 2** em tudo e **3** nos ⭐ → siga para o **S6**. Linhas em **0–1**: refaça a aula indicada e o **laboratório** correspondente.

## Parte 4: Simulado: revisão de segurança antes de abrir o PayFlow a um parceiro (60 min, **com consulta ao cheatsheet**)
> ⚠️ **Cenário SINTÉTICO:** os **números** abaixo são **inventados** para o exercício; os **mecanismos** (camadas e causa comum, mTLS, envelope, portão, LGPD/PCI, trilha e régua) vêm das aulas S5.01–S5.07, **medidos** no laboratório. **Não é parecer jurídico.** **Nunca** cite este dossiê como medição.

**Cenário.** Um cliente corporativo (**Acme**) vai chamar `POST /api/v1/transfers` **do servidor dele**, via ALB, a partir da **semana que vem**. O time lista **cinco camadas** de defesa (WAF/limitador, OAuth, autorização por dono, papéis do banco, auditoria), cada uma com **chance de falha de 20%** (**premissa**). Mas a **chave de API da Acme** foi guardada **no mesmo cofre e no mesmo caminho** que a senha do banco: uma **causa comum de 3%**. Na revisão você encontra: **(a)** o endpoint do parceiro **não checa o dono** da conta de origem (fácil de explorar; **vê** saldos de **outros** clientes); **(b)** o corpo de um erro de validação **loga o PAN** (a Acme, por engano, enviou **340 cartões reais** de clientes dela **nos últimos 6 dias**; ninguém de fora acessou os logs); **(c)** a página estática não tem CSP (sem conteúdo de usuário); **(d)** a porta 5432 está aberta para o **peering** com a rede do parceiro, com **senha forte e rotacionada**. **Quarta-feira 14:00:** você **confirma** o item (b). O **Trivy** do PR também reporta uma biblioteca **HIGH com correção disponível**, com uma isenção que **venceu ontem**. A migração do banco do parceiro exige **rotacionar a KEK** de **3.000.000** de campos cifrados por envelope (**2** chamadas ao KMS por campo; **500 chamadas/s**).

**Tarefas:**
1. **Camadas.** Calcule a chance de um ataque atravessar as cinco camadas **(i)** se forem independentes e **(ii)** com a causa comum. O que você muda **primeiro**?
2. **Classificação.** Classifique os achados (a)–(d) com **probabilidade × impacto** (justifique **as notas**), a **severidade** e o **prazo** (SLA proposto da S5.07, a partir de **2026-11-23**). Escreva **o achado (a)** no formato do relatório (id, ativo, evidência, recomendação **com opções**, dono, prazo), **sem culpa**.
3. **Incidente do PAN (b).** O que fazer **em ordem**? **Quando** vence o prazo à ANPD e **quais critérios**? **O que ajuda e o que não** (o mascarador)? Qual a **causa de projeto** e a correção?
4. **mTLS e parceiro.** A Acme quer **mTLS**. Onde terminar (aplicação, ALB *verify*, *passthrough*)? O que o ALB **passa** à aplicação? **O que o mTLS não resolve** (lembre do caso 2)? Quais **riscos operacionais** (expiração) e o **alerta**?
5. **Criptografia e chave.** **Quantas chamadas** e **quanto tempo** a rotação da KEK leva? **O que continua funcionando** durante a rotação? Onde fica a **chave do *backup*** e por quê?
6. **Pipeline.** O portão **bloqueia ou não** a biblioteca HIGH? Que regra própria cobre a **5432 aberta**? O **Trivy** acharia?
7. **Cobertura.** A revisão pode dizer "**sem mais achados**" se a matriz tem **2 células `??`** (*pipeline*/vazamento e *backups*/repúdio)? O que **entra no relatório**?

**Rubrica (100 pontos):** camadas e causa comum (**15**); classificação, SLA e relatório do achado (a) (**20**); incidente do PAN e LGPD (**20**); mTLS e parceiro (**10**); criptografia, rotação e chave do *backup* (**15**); pipeline e regra própria (**10**); cobertura e honestidade do relatório (**10**). **Aprovado: ≥ 70.**

<details><summary>Gabarito (resumo)</summary>

**1.** (i) `0,2⁵ = 0,00032 = 0,032%`. (ii) `0,03 + 0,97 × 0,00032 = 0,0303104 ≈ 3,03%` (**~95× pior**). **Primeiro:** **desfazer a causa comum**: chave do parceiro **em outro cofre/caminho**, **credencial por parceiro** e **de vida curta** (S5.02/P10.05); depois o item (a).
**2.** (a) **L5 × I4 = 20 → CRÍTICO**, prazo **7 dias → 2026-11-30** (**explorável por qualquer parceiro autenticado; vê dado de terceiros**). (b) **L3 × I5 = 15 → ALTO** (dado de cartão de **clientes da Acme** em log; **sem acesso externo conhecido**: L3), **30 dias → 2026-12-23** — **mas** é **incidente** (**tarefa 3**), **não** espera o SLA. (c) **L2 × I2 = 4 → BAIXO**, **365 dias → 2027-11-23**. (d) **L2 × I5 = 10 → MÉDIO** (a senha forte baixa a probabilidade; **premissa registrada**), **90 dias → 2027-02-21**. **Relatório de (a):** `id: F-01`, ativo `payflow-api POST /transfers`, **evidência:** requisição com o *token* do parceiro e `account_id` de **outro** cliente retorna 200 (captura em `evidence/F-01.txt`), recomendação: **opção A** checar o dono no serviço (2 dias) + linha na tabela de política; **opção B** *gateway* também (5 dias); **recomendo A**, **dono** `payments-team`, **prazo** `2026-11-30`, status `OPEN`; **causa:** o endpoint foi construído **sem uma linha na tabela de política** e **sem teste de arquitetura**; **sem apontar pessoas**.
**3.** **Ordem:** (1) **conter** (parar de logar o corpo: *deploy* do **allow-list**; bloquear o campo no parceiro); (2) **escopo** (quais logs/*backups*/réplicas **têm** os PANs; quem **acessou**); (3) **avisar** DPO/jurídico **hoje**; (4) **decidir** a comunicação (**3 dias úteis**, **quarta 14:00 → vence na segunda** se **não houver feriado**; **confirme** a regra de contagem com o jurídico) à **ANPD e aos titulares**: critérios **cumulativos**: **ocorrência confirmada**, **dados pessoais da LGPD**, **risco ou dano relevante** (PAN **é financeiro**; **ninguém externo acessou**: **o jurídico pondera o risco**); (5) **expurgar** os logs afetados (retenção + reescrita com **aprovação**) e **registrar**; (6) **postmortem sem culpa**. **O mascarador:** **ajudaria** no formato simples e **vazaria** número **partido/codificado** (2 de 7 linhas com cartão): **rede, não controle**. **Causa de projeto:** o PayFlow **recebeu PAN** (fora do escopo desejado): **campos hospedados do PSP** + **não aceitar PAN no corpo** (**rejeitar com 422** e **não logar o corpo**).
**4.** **Borda B2B:** **ALB com mutual TLS *verify*** (valida contra o *trust store* da **CA da Acme**) **ou *passthrough*** (a **aplicação** valida a cadeia). O ALB **passa os dados do certificado** em **cabeçalhos `X-Amzn-Mtls-*`** (confiar **só se o ALB for o único caminho**). **mTLS ≠ autorização** (o `reporting` com certificado válido tomou 403): **a tabela de política continua**. **Riscos:** **expiração** (vida curta + renovação, **alerta** `< 14 d` ticket / `< 3 d` página **testado**), **CA do parceiro** (quem a gerencia), **revogação** (vida curta). **Cadeia:** `payflow-ca` **não** é a CA da Acme: **truststore separado** para os clientes B2B.
**5.** `3.000.000 × 2 = 6.000.000` chamadas; a 500/s = **12.000 s ≈ 3,3 h**. **Durante:** leituras **continuam** (cada registro guarda o **`kekId`**; a KEK antiga **permanece** até o fim); **o custo** é cota/latência do KMS (**medir** a cota real). **Chave do *backup*:** **conta de *backup*** (causa comum com o banco evitada), política que **nega** `ScheduleKeyDeletion` fora do papel de emergência (espera de **7 a 30 dias**), **alarme**, **restauração ensaiada com a chave de destino**.
**6.** **Bloqueia**: **HIGH com correção** e **isenção vencida** → **FAIL**: renovar **com nova decisão** (motivo, dono, prazo ≤ 90 dias) **ou corrigir**. **Regra própria:** reprovar `ingress 0.0.0.0/0` em porta ≠ 80/443 num SG (`terraform show -json` + `jq`, ou Conftest). **O Trivy 0.76.2 não achou** o 5432 aberto no laboratório (a regra `AWS-0107` é de **SSH/RDP**); **ele acharia a porta 22**.
**7.** **Não.** **Duas células em silêncio** = **dois lugares onde ninguém olhou**: **revise** (ou declare **N/A com motivo**/fora de escopo **com aceite**). O relatório **inclui** a **matriz de cobertura** e a seção **"Não coberto"** (**com o motivo e quem aceitou**).
**Ordem de ações:** (1) **conter o PAN** (item b) e **avisar DPO/jurídico**; (2) **corrigir o IDOR** (a) **antes** de abrir para o parceiro; (3) **separar a chave da Acme** (causa comum); (4) **decidir mTLS B2B**; (5) **fechar a 5432** e a **isenção**; (6) **só então** liberar. **Não fazer:** liberar o parceiro **antes** do IDOR; **tratar** o PAN como "só mais um achado"; **discutir severidade** antes de **reproduzir**; **confiar** no mascarador; **apagar** os logs **sem decisão** do DPO.
</details>

## ➡️ Próximo
[S6 — Arquitetura em nuvem e custo](../S6-arquitetura-nuvem-custo/README.md)
