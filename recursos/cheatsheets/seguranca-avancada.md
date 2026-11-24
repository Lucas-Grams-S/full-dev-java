# Cheatsheet — Segurança avançada (S5)

> Valores de **laboratório** (Java puro, PostgreSQL 18 e Trivy em Docker, `promtool`; certificados e KMS **descartáveis/falsos**): **não** são do PayFlow nem da AWS. Milissegundos variam. **Não é parecer jurídico.**

## Camadas, zero trust e política (S5.01)
| Tema | Regra / número | Onde |
|---|---|---|
| Camadas independentes | `m^n`: 5 camadas a 30% → **0,243%** | S5.01 |
| Causa comum | `c + (1 − c) × m^n`: c = 1% → **1,24%**; c = 5% → **5,23%** (**conte causas, não caixas**) | S5.01 |
| Zero trust (NIST SP 800-207) | sem confiança por rede; autenticar/autorizar **cada** acesso; menor privilégio; assumir a violação | S5.01 |
| Política testável | `decide(chamador, ação, dono)` + **negar por padrão** + MFA em dinheiro + *break-glass*; 16 casos, **3 mutantes** (3/4/2 falhas) | S5.01 |
| Papéis do banco | `owner` NOLOGIN · `migrator` só no pipeline · `app` sem `UPDATE`/`DELETE` no ledger · expurgo por **função com piso** · `readonly` **por coluna** | S5.01 |
| *Break-glass* | ticket + MFA + dois olhos + **prazo** (`VALID UNTIL`, só login por senha) + revisão depois | S5.01 |
| Armadilha de teste | imagem oficial do PostgreSQL: `127.0.0.1` em **`trust`** (senha não conferida) | S5.01 |

## mTLS (S5.02)
| Tema | Regra | Onde |
|---|---|---|
| O que dá | **identidade do canal** (prova da chave privada); **não** autoriza (cert. válido → 403) | S5.02 |
| Valida | **cadeia até CA confiável**, validade, `extendedKeyUsage` (`clientAuth`), **SAN** (não `CN`) | S5.02 |
| Erros | `unable to find valid certification path…` (CA) · `No subject alternative DNS name matching…` (SAN) · TLS 1.1 recusado · `Connection reset`/`certificate_*` (varia: **não assertar**) | S5.02 |
| Custo (lab) | *handshake* ~4–5 ms; conexão reaproveitada < 1 ms; mTLS × TLS dentro do ruído | S5.02 |
| Onde terminar | app (`server.ssl.bundle`, `server.ssl.client-auth=need`, `reload-on-update`) · ALB (*verify*/*passthrough*, `X-Amzn-Mtls-*`) · malha (STRICT/PERMISSIVE) | S5.02 |
| Operação | vida curta + renovação automática; **alerta** < 14 d ticket / < 3 d página (testado); **não** no *readiness*; `want` → `need` | S5.02 |
| PayFlow | **não adotar mTLS interno agora**; gatilhos: vários times, rede não confiável, exigência, malha, token vazado | S5.02 |

## Criptografia e chaves (S5.03)
| Tema | Regra / número | Onde |
|---|---|---|
| Em repouso | volume (disco/*snapshot*) × **campo** (dump/SQLi/DBA); **nenhum** protege de aplicação comprometida | S5.03 |
| Envelope | DEK por registro (AES-256-GCM) embrulhada pela KEK (KMS); **contexto = AAD** (`tabela\|id\|coluna`) | S5.03 |
| Overhead | nonce 12 + tag 16 = **28 B**; DEK embrulhada 60 B | S5.03 |
| Rotação | **re-wrap** das DEKs (dados intactos): **2 chamadas ao KMS por registro** | S5.03 |
| *Nonce* | reuso ⇒ XOR dos cifrados = XOR dos claros; limite **2³²** chamadas/chave com IV aleatório (SP 800-38D §8.3) | S5.03 |
| Índice cego | `HMAC(segredo, valor normalizado)`: busca por igualdade; **vaza a distribuição** (700/200/100) | S5.03 |
| AWS KMS | rotação automática padrão **365 d** (material antigo retido); exclusão **7–30 d** (padrão 30); contexto **não secreto**, vai ao CloudTrail | S5.03 |
| RDS | cifra storage, logs, *backups*, réplicas e *snapshots*; **a chave KMS não muda depois** | S5.03 |
| Chave do *backup* | **conta de *backup***, política que nega exclusão, alarme, restauração com a chave de destino | S5.03 |

## Pipeline (S5.04)
| Tema | Regra / número | Onde |
|---|---|---|
| SAST/DAST/SCA | código (cedo, ruidoso, sem fluxo) · app rodando (só o que exerceu) · dependências (CVE conhecida) | S5.04 |
| Portão | bloqueia **HIGH/CRITICAL com correção**; MEDIUM/sem correção → ticket; **isenção com id, motivo, dono, `until` ≤ 90 d** | S5.04 |
| Onde roda | PR (segredos, IaC, SAST rápido, SCA) · main (DAST **baseline passivo** em teste) · **SCA agendado diário** | S5.04 |
| Trivy 0.76.2 (lab) | `log4j-core 2.14.1` → 9 achados; segredos **2 de 5**; **5432 aberto não reportado** (`AWS-0107` é SSH/RDP); pasta com `pom.xml` → **429** do Maven Central | S5.04 |
| Verificar o verificador | amostra **ruim** + **mutante** + `actionlint` | S5.04 |

## LGPD e PCI (S5.05)
| Tema | Regra / número | Onde |
|---|---|---|
| Incidente (ANPD) | **3 dias úteis** à ANPD e aos titulares se: confirmado, dado pessoal, **risco ou dano relevante** (Res. CD/ANPD 15/2024; complementação em 20 dias úteis) | S5.05 |
| Inventário | tabela.coluna · categoria · **base legal** · **retenção** · dono (inclui *heap dumps* e *backups*) | S5.05 |
| Eliminação | anonimizar `users` no lugar (manter `id`); IdP, cache e cópias; **ledger e trilha ficam** (a confirmar) | S5.05 |
| Luhn | **~10%** de strings aleatórias passam; **~2,2%** com prefixo + tamanho 13–19 | S5.05 |
| Mascarador | rede, não controle: vazou **2 de 7** linhas com cartão (número partido/codificado) | S5.05 |
| PCI | **não tocar no PAN** (campos hospedados do PSP; guardar *token* + últimos 4); SAD nunca depois da autorização; **a conferir no padrão** | S5.05 |
| CPF | dígitos mod 11 → 1% de falso positivo; SHA-256 sem segredo inverte em **~4 min/núcleo**; **HMAC** com chave no KMS | S5.05 |

## Auditoria e revisão (S5.06, S5.07)
| Tema | Regra / número | Onde |
|---|---|---|
| Evento | ator (id), ação (vocabulário fechado), recurso, resultado (**inclui `DENIED`**), `request_id`; sem PAN/CPF/e-mail | S5.06 |
| Gravação | recusa em **transação separada** (`REQUIRES_NEW`) ou Outbox | S5.06 |
| Trilha | `audit_events` (V25) *append-only*; app só `INSERT`; **o dono desliga o gatilho** | S5.06 |
| Cadeia de HMAC | detecta edição, remoção no meio, troca, *append* forjado; **só a âncora** pega **cortar o rabo** e **quem tem a chave** | S5.06 |
| Alertas | recusas > 20% por 5 min **e** > 0,5 evento/s; uso de *break-glass* (5 testes, 2 mutantes) | S5.06 |
| Régua | L×I: ≥ 20 CRÍTICO · ≥ 12 ALTO · ≥ 6 MÉDIO; SLA **7/30/90/365 d** (propostas) | S5.07 |
| Relatório | evidência, recomendação com opções, dono, prazo; **aceite com aprovador e validade** | S5.07 |
| Cobertura | componentes × STRIDE: **nenhuma célula em silêncio** (lab: 32/36 → FAIL) | S5.07 |
