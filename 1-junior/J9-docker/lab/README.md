# Laboratório da J9

Tudo aqui foi **executado** (Linux, Docker Engine 29.9.2, Compose v5.6.1, Temurin 25.0.5, Spring Boot 4.1.2, PostgreSQL 18.6). O JDK 25 só é necessário se você quiser compilar fora do Docker (`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`); **o `Dockerfile` constrói dentro da imagem**.

| Item | O que é | Como rodar |
|---|---|---|
| `docker-lab/` | O **PayFlow Lite**: Spring Boot + Flyway (migração e seed) + PostgreSQL + Actuator + segredo do JWT obrigatório (falha rápida) + um endpoint que mostra o que a JVM enxerga no container. É o que as aulas J9.02 a J9.04 usam para medir tudo. | `cd docker-lab && bash scripts/make-secrets.sh && docker compose up -d --build --wait` |
| `docker-lab/Dockerfile` | multi-stage, JRE Alpine, uid 10001, JAR em camadas, `ENTRYPOINT` exec | `docker build -t payflow-lite:1 .` e `docker build --check .` |
| `docker-lab/compose.yaml` | `db` + `app` + `psql` (perfil `tools`), volume, rede, segredos por `configtree`, `healthcheck`, `restart`, rotação de log | `docker compose ps`, `docker compose --profile tools run --rm psql -c 'select * from accounts'` |
| `docker-lab/compose.debug.yaml` | sobreposição que liga o JDWP em `127.0.0.1:5005` (só dev) | `docker compose -f compose.yaml -f compose.debug.yaml up -d --wait` |
| `docker-lab/.dockerignore` | o arquivo da J9.04 | compare `docker build --progress=plain` com e sem ele |

Endereços do laboratório: `http://localhost:8080/actuator/health`, `/api/v1/runtime` (usuário, memória, CPUs, host), `/api/v1/accounts` (`GET` lista; `POST` com `{"owner":"x@y.dev","balance":"10"}` cria).

**Limpeza** (o laboratório cria containers, rede, volume e imagem):
```bash
cd docker-lab && docker compose --profile tools down -v --remove-orphans && docker rmi payflow-lite:dev
rm -rf secrets target          # os segredos são só de desenvolvimento; `secrets/` está no .gitignore
```

O que o laboratório **não** é: o PayFlow. Ele não tem Spring Security, JPA, Thymeleaf nem os testes; serve para provar o que é do **Docker**. O `Dockerfile` e o `compose.yaml` do PayFlow do aluno são os mesmos, com as três marcas `← PayFlow` descritas na J9.02 e na J9.03.

**O que a execução achou** (e que a leitura sozinha não achava): (1) sem `@DefaultValue` nos *records* aninhados da `PayflowProperties`, a aplicação **subiu sem o segredo do JWT** (o Boot não cria o objeto aninhado, o construtor que valida nunca roda): o PayFlow da J8 já tem os `@DefaultValue`; (2) segredo com `chmod 600` + usuário não root = `AccessDeniedException` e `uid`/`mode` do Compose ignorados; (3) trocar a senha do banco com volume antigo = `password authentication failed`; (4) a imagem Ubuntu do JRE **não tem** `curl` nem `wget` (o *healthcheck* exigiu o Alpine); (5) `docker cp` de um *symlink* copia o link; (6) o BuildKit atual **não cita** o `.dockerignore` quando o `COPY` não acha o arquivo.
