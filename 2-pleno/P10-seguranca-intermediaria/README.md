# P10 — Segurança intermediária

> 🔵 Pleno · ⏱️ 12h (8 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **parcialmente executado: o Resource Server do PayFlow (versão mínima, em memória; 54 testes verdes, 8 mutantes), o Keycloak 26.8.2 real (Authorization Code + PKCE, refresh com rotação, Client Credentials, bloqueio por tentativas, política de senha), a rotação de credenciais do PostgreSQL 18 real, a API com o Keycloak indisponível, o SBOM (Maven e imagem), o Trivy com falha plantada, a assinatura e o portão de *deploy* com o cosign (registro local) e o verificador do modelo de ameaças foram executados de verdade; o PayFlow do aluno (revisado por leitura), o painel com `oauth2Login`, o Keycloak em produção, a migração das senhas da J8, a troca de token, o mTLS, o KMS, o Secrets Manager com rotação, a AWS real e a assinatura sem chave NÃO foram; testes práticos no fim do treinamento, por decisão do aluno**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído em `~/docs/payflow`, tag `payflow-P10`).** **Executado:** `run-all.sh` completo (numa cópia): **54/54 testes** (23 sem Docker + 31 contra **Keycloak 26.8.2** e **PostgreSQL 18** reais: `RealmPolicyIT` 3, `ResourceServerKeycloakIT` 7, `SecretRotationIT` 5...), **mutantes 6/8 mortos** (M2 e M4a sobrevivem, como a aula documenta), SBOM/Trivy/cosign num registro local com a atestação verificada (64 componentes), ensaio de queda do Keycloak, verificador de ameaças (`--self-test`); 9 saídas idênticas e 10 que diferem só por digests, tempos e ids. **Corrigido nas aulas:** nenhuma divergência de conteúdo. **Ainda não executado:** no PayFlow reconstruído: o Resource Server do Keycloak no lugar do HS256 da J8, o *realm* como código, os clientes `client_credentials`, a rotação de senha no RDS real, o cosign por *digest* no CI (o `T21` → 503 e o `cosign` por *digest* da lista de pendências do Pleno só valem no laboratório); KMS, mTLS e AWS real. Os avisos "não executado" / "revisado por leitura" nas aulas descrevem a escrita original.

## 🎯 Objetivo do módulo
**Autenticar usuários e serviços com padrões abertos.** O PayFlow **para de emitir os próprios tokens** (o HS256 da J8) e passa a ser **Resource Server** de um servidor de identidade (**Keycloak**): **OAuth 2.0 e OpenID Connect** (Authorization Code + PKCE, access/ID/refresh, escopo × papel × audiência × dono), **validação de tokens** com **provas por mutante**, **autenticação entre serviços** (Client Credentials, um cliente por serviço, escopo + `azp`), **rotação de segredos** (dois usuários alternados, menor privilégio, **a dívida da conta mestra paga**), **cadeia de suprimentos** (SBOM, *scan* com falha plantada, assinatura por digest, portão de *deploy*) e **modelo de ameaças STRIDE** **verificável** (cada mitigação aponta para um teste).

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [P10.01](P10.01-oauth2-e-openid-connect-fluxos-tokens-e-escopos.md) | OAuth 2.0 e OpenID Connect: fluxos, tokens e escopos | 1h30 |
| [P10.02](P10.02-keycloak-com-docker.md) | Keycloak com Docker | 1h30 |
| [P10.03](P10.03-spring-security-como-resource-server.md) | Spring Security como Resource Server | 1h30 |
| [P10.04](P10.04-autenticacao-entre-servicos.md) | Autenticação entre serviços | 1h30 |
| [P10.05](P10.05-gestao-e-rotacao-de-segredos.md) | Gestão e rotação de segredos | 1h30 |
| [P10.06](P10.06-cadeia-de-suprimentos-sbom-e-scan-de-imagens.md) | Cadeia de suprimentos: SBOM e scan de imagens | 1h30 |
| [P10.07](P10.07-threat-modeling-com-stride.md) | Threat modeling com STRIDE | 1h30 |
| [P10.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório em [`lab/`](lab/README.md): `lab/identity-lab/` (Maven + JUnit 6, **executado** contra o Keycloak e o PostgreSQL reais), `lab/keycloak-lab/` (realm como código, **executado**), `lab/secrets-lab/` (PostgreSQL 18, **executado**), `lab/supply-chain-lab/` (Trivy, cosign e registro local, **executado**) e `lab/threat-model/` (**executado**).

> ⚠️ **Aviso de verificação (leia antes de estudar):** **foi executado** (Linux, Docker 29.9.2, JDK 25.0.5, Spring Boot 4.1.2, Spring Security 7.1.2, Keycloak 26.8.2, PostgreSQL 18, Trivy 0.76.2, cosign 3.1.4): as saídas mostradas nas aulas são **as reais**. O laboratório é **uma versão mínima** da API do PayFlow (contas em memória; **a mesma aplicação faz o lado do cliente e o do servidor**). **Não foi executado:** o **PayFlow do aluno**, o painel com `oauth2Login`, o Keycloak **em produção**, a **migração das senhas** (BCrypt → Keycloak), a **troca de token**, o **mTLS**, o **KMS**, o **Secrets Manager** com Lambda de rotação, a **AWS real** e a assinatura **sem chave**. **Os testes práticos acontecem no fim do treinamento.** A **lista "a validar no fim do treinamento"** está em `00-guia/progresso.md`.

> **Avisos de ordem:** a P10.01 retoma J8.03 (o JWT HS256 e o **ADR 0010**, cujo custo — sem *refresh* e sem revogação — este módulo paga); a P10.02 retoma J8.06 (o `LoginThrottle` e a `PasswordPolicy` saem para o realm); a P10.03 retoma J8.01/J8.04/J8.06 e P5.05; a P10.04 retoma P5.02/P5.05 (`/internal` nunca roteado) e P9.02 (*task role*); a P10.05 **fecha as duas dívidas da P9.06** (a conta mestra do banco e a exceção da decisão 169, a chave JWT por variável de ambiente); a P10.06 retoma P8.02 e P9.02 (digest); a P10.07 retoma J8.05/J8.06 (OWASP), P6.01 (auditoria) e P9.01 (fronteiras de rede). **mTLS, KMS aplicado, SAST/DAST e conformidade (PCI/LGPD) são da S5** (apontados, **não** usados como pré-requisito).

## 🏗️ Entrega no PayFlow
**API como Resource Server** do Keycloak (RS256; `iss`, `aud`, `typ`; escopo `transfers:read`/`write` × papel `USER`/`ADMIN`; dono = `sub`), **serviços com Client Credentials** (um cliente por serviço, `/internal` com escopo + `azp`), **senha do banco por dois usuários alternados** com **menor privilégio** (aplicação ≠ migração ≠ mestra; o livro-razão imposto pelo banco), **imagem com SBOM, assinatura por digest e portão de *deploy***, **modelo de ameaças** (26 ameaças, verificador no CI), `docs/{identity,resource-server,service-auth,secrets,supply-chain}.md`, o **realm como código** e a **ADR 0022**. Tag `payflow-P10`. **Uma migração nova no monólito: V12** (`password_hash` nullable; a próxima livre passa a ser **V13**).

## ➡️ Próximo
[✅ Checkpoint de nível Pleno](../checkpoint-pleno.md)
