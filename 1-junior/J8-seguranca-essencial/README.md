# J8 — Segurança essencial

> 🟢 Júnior · ⏱️ 12h (8 sessões de 1h30) · Status: ✅ concluído (2026-11-13) — **validado na execução final (2026-11-24); veja o bloco abaixo**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído das aulas em `~/docs/payflow`, tag `payflow-J8`).** **Executado:** `mvn verify` com **256 testes** verdes, Checkstyle **0**, SpotBugs **0**; login do painel (sessão + CSRF) e API com JWT passando. **Corrigido nas aulas:** a J8.04 chamava `account.unblock()` (o método da `Account` é `activate()`); a J8.06 falava em três mudanças na `SecurityConfig` e eram duas; a migração para a J8 muda resultados de testes da J6/J7 (401 em `/api/v2` e `/api/v9`, 302 em páginas sem sessão e no Actuator, springdoc liberado na cadeia do painel, seed com 2 contas da Ana). **Ainda não executado:** os itens (b)–(i) da lista "a validar" da J8: CSP sem violações no navegador, `SameSite` entre `localhost` e `127.0.0.1`, Dependency-Check com chave do NVD, gitleaks, política de senha com o HIBP real, texto da LGPD. Os avisos "não executado" / "revisado por leitura" que aparecem nas aulas descrevem o momento em que elas foram escritas.

## 🎯 Objetivo do módulo
Proteger a API com autenticação, autorização e as defesas do OWASP Top 10.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| J8.01 | Autenticação vs autorização e a cadeia de filtros do Spring Security | 1h30 |
| J8.02 | `UserDetailsService` e `PasswordEncoder` | 1h30 |
| J8.03 | Sessão vs JWT | 1h30 |
| J8.04 | Autorização por papéis e `@PreAuthorize` | 1h30 |
| J8.05 | OWASP Top 10 I: injeção, XSS e CSRF | 1h30 |
| J8.06 | OWASP Top 10 II: IDOR e falhas de autenticação | 1h30 |
| J8.07 | Segredos, validação de entrada, Dependency-Check e LGPD básica | 1h30 |
| J8.CP | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Arquivos: `J8.01-autenticacao-autorizacao-e-cadeia-de-filtros.md`, `J8.02-userdetailsservice-e-passwordencoder.md`, `J8.03-sessao-vs-jwt.md`, `J8.04-autorizacao-por-papeis-e-preauthorize.md`, `J8.05-owasp-top-10-i-injecao-xss-e-csrf.md`, `J8.06-owasp-top-10-ii-idor-e-falhas-de-autenticacao.md`, `J8.07-segredos-validacao-dependency-check-e-lgpd.md`, `checkpoint.md`. Laboratórios prontos em `lab/`: `security-lab/` (projeto Maven com a segurança do PayFlow e **81 testes**), `default-security-lab/` (o que o Boot faz sozinho, ordem das regras, `@PreAuthorize`: **8 testes**), `PasswordLab.java`, `InjectionLab.java`, `check-secrets.sh` e `osv-check.sh`.

> ⚠️ **Aviso de verificação (leia antes de estudar):** por decisão do aluno, o PayFlow **não** foi compilado nem testado aqui (o repositório dele não existe em disco). Foram **executados de verdade** (JDK 25, Spring Boot 4.1.2, Spring Security 7.1.2): os dois projetos de laboratório, `PasswordLab`, `InjectionLab`, `check-secrets.sh` e `osv-check.sh`; todas as saídas coladas nas aulas vêm deles. As classes que tocam no PayFlow (controllers, `UserRepository`, seed, `PayflowWebTest`, `ApiClient`, `TransfersControllerWebTest`) foram **revisadas por leitura** contra as aulas J3 a J7. **Não executados:** OWASP Dependency-Check, gitleaks, a chamada ao Have I Been Pwned, os testes de navegador (CSP, CSRF, `SameSite`) e o texto da LGPD (o site do Planalto não respondeu daqui; marcado ⚠️).

> **Avisos de ordem:** entre os passos 1 e 8 da J8.03 o projeto **não compila** (rode `mvn test` só no passo 9); a ponte `BridgeSecurityConfig` (J8.01) existe só até a J8.03; o limitador de tentativas (J8.06) é acrescentado à `SecurityConfig` que a J8.03 deixa sem ele.

## 🏗️ Entrega no PayFlow
Autenticação por senha (BCrypt com prefixo `{bcrypt}`), **duas cadeias** de segurança (painel com sessão e CSRF; API com JWT), papéis `USER`/`ADMIN` com o primeiro endpoint administrativo, defesas contra injeção, XSS (CSP e cabeçalhos), CSRF e IDOR (matriz de testes), limite de tentativas, trilha de auditoria sem dados pessoais, política de senha, segredos por ambiente, varredura de dependências e o mapa de dados da LGPD. Tag `payflow-J8`.

## ➡️ Próximo
[J9 — Docker essencial](../J9-docker/README.md)
