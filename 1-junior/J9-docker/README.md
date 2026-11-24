# J9 — Docker essencial

> 🟢 Júnior · ⏱️ 7.5h (5 sessões de 1h30) · Status: ✅ concluído (2026-11-13) — **validado na execução final (2026-11-24); veja o bloco abaixo**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído das aulas em `~/docs/payflow`, tag `payflow-J9`).** **Executado:** `docker compose up -d --build --wait` com `db` e `app` saudáveis; token JWT e login do painel por HTTP funcionam; processo como `uid=10001`; `docker stop` em 0,53 s (exit 143); banco não publicado; `docker inspect` sem valor de segredo; imagem de 445 MB. **Corrigido nas aulas:** o `Dockerfile` do PayFlow precisa de `COPY config ./config` (o Checkstyle da J7 roda em `validate`). **Ainda não executado:** os erros provocados do checkpoint (`chmod 600`, senha do banco com volume antigo, `ENTRYPOINT` em forma *shell*), o depurador JDWP, Docker Desktop, Docker Scout/Trivy, Hadolint, `spring-boot:build-image`, o cookie `Secure` em navegador real. Os avisos "não executado" / "revisado por leitura" que aparecem nas aulas descrevem o momento em que elas foram escritas.

## 🎯 Objetivo do módulo
Empacotar e rodar o PayFlow em containers, com imagem enxuta e segura, ambiente completo por Compose e diagnóstico de problemas.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [J9.01](J9.01-imagem-vs-container-e-comandos-basicos.md) | Imagem vs container e comandos básicos | 1h30 |
| [J9.02](J9.02-dockerfile-multi-stage-imagem-enxuta-e-usuario-nao-root.md) | Dockerfile multi-stage, imagem enxuta e usuário não root | 1h30 |
| [J9.03](J9.03-docker-compose-app-postgresql-volumes-redes-e-variaveis.md) | Docker Compose: app + PostgreSQL, volumes, redes e variáveis | 1h30 |
| [J9.04](J9.04-logs-depuracao-e-dockerignore.md) | Logs, depuração e `.dockerignore` | 1h30 |
| [J9.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório pronto em `lab/docker-lab/` (o **PayFlow Lite**, com `Dockerfile`, `compose.yaml`, `compose.debug.yaml` e `.dockerignore`); veja `lab/README.md`.

> ⚠️ **Aviso de verificação (leia antes de estudar):** por decisão do aluno, o PayFlow **não** foi compilado nem testado aqui (o repositório dele não existe em disco). Foram **executados de verdade** (Docker 29.9.2, Compose v5.6.1, Spring Boot 4.1.2, PostgreSQL 18.6): o `Dockerfile`, o `compose.yaml`, o `.dockerignore`, o `compose.debug.yaml` e todos os comandos do laboratório; todas as saídas coladas nas aulas vêm dele. O `Dockerfile`, o `compose.yaml` e o `.dockerignore` **do PayFlow do aluno** foram **revisados por leitura** contra as aulas J3 a J8. **Não executados:** o painel e o login do PayFlow dentro do Compose, o cookie `Secure` por HTTP em navegadores, Docker Desktop (Windows/macOS), varredura de imagem (Docker Scout/Trivy), Hadolint e `spring-boot:build-image`.

> **Avisos de ordem:** a J9.01 usa só o banco (o PayFlow ainda não vira imagem); a J9.02 antecipa o `.dockerignore` apenas como "destino" (a J9.04 o ensina) e o `healthcheck` como motivo da base Alpine (a J9.03 o ensina); o bloco `logging` do `compose.yaml` é acrescentado na J9.04. Nenhum conceito de J10+ foi usado como pré-requisito (CI e nuvem aparecem só como destino).

## 🏗️ Entrega no PayFlow
`Dockerfile` multi-stage (JRE Alpine, uid 10001, JAR em camadas), `compose.yaml` com app + PostgreSQL (volume, rede interna, segredos por arquivo, `healthcheck`), `.dockerignore`, sobreposição de depuração e `docs/docker-notes.md`. Tag `payflow-J9`.

## ➡️ Próximo
[J10 — CI e introdução à nuvem](../J10-ci-nuvem-intro/README.md)
