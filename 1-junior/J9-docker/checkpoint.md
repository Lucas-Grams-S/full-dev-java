# J9.CP — Checkpoint do módulo J9

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J9.01 a J9.04

> ✅ **Validação final (2026-11-24):** este módulo foi executado no PayFlow reconstruído; o resultado e o que continua não executado estão no [README do módulo](README.md). Os avisos "não executado" / "por leitura" abaixo descrevem a escrita original.

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, Docker 29.9.2, Compose v5.6.1, Temurin 25.0.5, Spring Boot 4.1.2, PostgreSQL 18.6): o laboratório `lab/docker-lab` (PayFlow Lite): `Dockerfile` multi-stage (build frio 48 s, sem mudança 1,4 s, mudando um `.java` 3,7 s; imagens de 639 MB, 527 MB e 357 MB), usuário `10001`, `docker stop` de 0,15 s (exit 143) × 10,19 s (exit 137), falha rápida (exit 1), limites de memória, `compose.yaml` (rede, DNS, volume, segredos por `configtree`, `healthcheck`, `restart`, perfil `tools`), os erros de permissão do segredo e de senha do banco com volume antigo, `logs`/`inspect`/`stats`/`top`/`diff`/`cp`, *thread dump* por `SIGQUIT`, depuração por JDWP (`jdb`) e o `.dockerignore` (contexto de 47,38 MB × 1,08 kB, segredo visível na imagem sem ele). **Revisados por leitura, não executados:** o `Dockerfile`, o `compose.yaml`, o `.dockerignore` e o `compose.debug.yaml` **do PayFlow do aluno** (o repositório dele não existe em disco), o `POST /api/v1/auth/token` e o painel dentro do Compose, o cookie `Secure` por HTTP em navegadores, Docker Desktop (Windows/macOS), varredura de imagem (Docker Scout/Trivy), Hadolint e `spring-boot:build-image`.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. O que container e máquina virtual compartilham e **não** compartilham? Que dois recursos do kernel fazem o isolamento e o limite de um container?
2. Diferencie imagem de container. O que acontece com os dados gravados dentro de um container quando ele é **parado**? E quando é **removido**? Por que `postgres:18` e não `postgres:latest`?
3. O que `-p 127.0.0.1:8080:8080` faz e por que o `127.0.0.1`? O `EXPOSE` publica a porta?
4. Por que o `Dockerfile` do PayFlow é **multi-stage** e por que o `COPY pom.xml` vem **antes** do `COPY src`? Para que servem `--mount=type=cache` e o JAR em camadas (`jarmode=tools extract --layers`)?
5. Por que rodar como usuário não root (`USER`, uid fixo)? Por que o `ENTRYPOINT` na forma *exec*, e o que significam os códigos de saída **143** e **137**?
6. Como a JVM dimensiona o *heap* em um container? O que faz `-XX:MaxRAMPercentage=75` e por que não 100?
7. Como a aplicação encontra o banco no Compose? Por que `localhost` na URL do banco **não** funciona dentro do container? O banco precisa publicar a porta 5432?
8. O que `down` apaga e o que `down -v` apaga a mais? Por que trocar `POSTGRES_PASSWORD` no `compose.yaml` com o volume já criado **não** troca a senha, e qual erro aparece?
9. Por que o segredo vai por **arquivo** (`/run/secrets` + `configtree`) e não por `-e`? Qual erro aparece com o arquivo `chmod 600`, por que `uid`/`mode` do Compose não resolvem e qual a saída (só em desenvolvimento)? O que vence se a variável de ambiente **e** o arquivo existem?
10. Um container sai logo depois de subir. Descreva o diagnóstico (comandos e o que cada **código de saída** diz). Para que serve o `.dockerignore` (dois efeitos), e por que o JDWP nunca vai para produção?

<details><summary>Gabarito</summary>

1. Compartilham o **kernel** do host; **não** compartilham processos, rede, sistema de arquivos e usuários (cada container tem a sua visão). A VM não compartilha nem o kernel (tem sistema operacional completo e hipervisor). **Namespaces** (PID, rede, mount, UTS, usuários) isolam; **cgroups** limitam e medem recursos (memória, CPU, processos). No laboratório o `uname -r` foi igual dentro e fora do container, e `--memory 256m` virou `memory.max = 268435456`.
2. **Imagem:** pacote imutável em camadas; **container:** execução dela, com uma camada de escrita descartável. **Parado:** o container e a camada **continuam** (`docker start` os traz de volta); **removido:** tudo se perde (a prova: o arquivo `/dados.txt` sumiu). Os dados do banco vão em **volume**. Tag `latest` é mutável (a versão maior pode mudar sem aviso e o volume de dados pode não ser compatível): reprodutibilidade (decisão 50).
3. Publica a porta 8080 do container **só na interface local** do host. Sem `127.0.0.1`, a porta fica em `0.0.0.0` (rede inteira) e o Docker **contorna** o `ufw`. `EXPOSE` apenas **documenta**; quem publica é o `-p` (ou `ports:` do Compose).
4. **Multi-stage:** o JDK, o Maven e o código-fonte ficam no estágio de build e não vão para a imagem final (só o JRE e o JAR), que fica menor e com menos superfície de ataque. O `pom.xml` vem antes para a camada das dependências só ser refeita quando o `pom` muda (medido: mudar um `.java` refez só a camada de 24,6 kB). O **cache mount** guarda o `.m2` entre builds (o passo caiu para 2,1 s); o **JAR em camadas** separa as 59 bibliotecas (27 MB) do seu código (14 KB) para as bibliotecas não serem reenviadas a cada deploy.
5. O root do container, mesmo limitado, é um processo privilegiado: se a aplicação for comprometida ou houver volume do host, o dano é maior; plataformas como o Kubernetes costumam exigir `runAsNonRoot`; uid fixo (10001) ajuda nas permissões de volume. Forma exec: a **JVM é o PID 1** e recebe o `SIGTERM` do `docker stop`, fazendo o *graceful shutdown* (0,15 s, exit 143 = 128+15); na forma shell (ou `sh -c "...; echo"`) o shell é o PID 1 e não repassa o sinal: 10,19 s e `SIGKILL` (exit 137 = 128+9).
6. A JVM lê os limites do cgroup: sem limite ela vê a máquina toda (14 392 MB de *heap* máximo no laboratório); com `--memory 512m --cpus 2`: 371 MB e 2 CPUs. `MaxRAMPercentage=75` usa 75% da memória do container como *heap* máximo (o padrão é 25%); sobra o resto para *metaspace*, pilhas e memória nativa. Com 100% o container estoura e é morto por OOM (137, `OOMKilled=true`).
7. Pelo **nome do serviço** (`db`), resolvido pelo DNS da rede do Compose (medido: `172.20.0.2 db`). Dentro do container da aplicação, `localhost` é o **próprio** container. O banco **não** precisa (e não deve) publicar a 5432: do host o `connect` deu `Connection refused`, e a aplicação o alcança pela rede interna; para olhar o banco, o perfil `tools` (`psql`) ou o `exec`.
8. `down` remove containers e rede e **mantém os volumes**; `down -v` apaga **também os volumes** (os dados). O PostgreSQL só usa `POSTGRES_PASSWORD` quando **cria** o diretório de dados; com o volume antigo a senha guardada é a velha, e a aplicação (com a senha nova) leva `FATAL: password authentication failed for user "payflow"`.
9. A variável aparece no `docker inspect`, em `/proc/<pid>/environ` e em dumps; o arquivo só existe onde é montado (o `inspect` mostrou só `SPRING_CONFIG_IMPORT`). Com `chmod 600` o dono é o uid 1000 do host e a aplicação (uid 10001) leva `java.nio.file.AccessDeniedException: /run/secrets/spring.datasource.password`; o Compose monta o arquivo por **bind mount** e `uid`/`mode` na sintaxe longa foram **ignorados** (testado); a saída para **dev local** é `chmod 644` (e `secrets/` fora do Git e do contexto). Em produção (Swarm/Kubernetes) uid/mode funcionam e o segredo vem de um cofre. A **variável de ambiente vence** o arquivo (testado com `-e PAYFLOW_SECURITY_JWT_SECRET=curto`).
10. `docker compose ps -a` (estado) → `docker inspect --format 'exit={{.State.ExitCode}} oom={{.State.OOMKilled}}'` → `docker compose logs app` (a **primeira** falha e a `Caused by` mais interna). **0:** terminou sozinho; **1:** falha na subida (ex.: segredo ausente); **137:** `SIGKILL` (`stop` impaciente ou OOM: veja `OOMKilled`); **143:** `SIGTERM` normal. `.dockerignore`: **(a)** reduz o contexto (47,38 MB → 1,08 kB) e **(b)** impede copiar `.git`, `.env`, `secrets/` e chaves para a imagem (sem ele, o segredo estava **legível dentro da imagem**). O JDWP permite **executar código** a quem alcançar a porta; só em `compose.debug.yaml`, em `127.0.0.1`, em desenvolvimento.
</details>

## Parte 2: Entrega prática (tag `payflow-J9`)
O `payflow` ganha uma **imagem de container** e um ambiente completo com **um comando**: `docker compose up -d --build --wait`.

```
payflow/
├── Dockerfile                  multi-stage (maven:3.9.12-eclipse-temurin-25 → eclipse-temurin:25-jre-alpine), uid 10001,
│                               JAR em camadas, ENTRYPOINT exec, MaxRAMPercentage=75, -DskipTests -Dcheckstyle.skip=true      (J9.02)
├── .dockerignore               .git, target/, secrets/, .env*, *.log, *.sql, compose*, Dockerfile*, docs/, scripts/            (J9.04)
├── compose.yaml                db (postgres:18, volume, healthcheck, segredo) + app (depends_on healthy, configtree,
│                               healthcheck, restart, logging) + psql (perfil tools)                                           (J9.03, J9.04)
├── compose.debug.yaml          sobreposição com JDWP em 127.0.0.1:5005 (só dev)                                               (J9.04)
├── scripts/make-secrets.sh     gera secrets/db_password, jwt_secret e pgpass (dev)                                            (J9.03)
├── .gitignore                  + secrets/ e .env
├── src/main/resources/application.yml   + server.shutdown: graceful e spring.lifecycle.timeout-per-shutdown-phase: 20s       (J9.02)
└── docs/docker-notes.md        medidas e achados (J9.01 a J9.04)
```
**Nenhuma** dependência nova no `pom.xml` e **nenhuma** classe Java nova: o módulo empacota o que já existe (a única mudança no código é a configuração do desligamento gracioso).

Critérios de pronto:
- [ ] **Imagem:** `docker build -t payflow:J9 .` conclui; `docker build --check .` **sem avisos**; `docker run --rm --entrypoint sh payflow:J9 -c id` → `uid=10001(payflow)`; tamanho anotado.
- [ ] **Cache:** mudar **uma linha** de um controller e reconstruir deixa `COPY pom.xml`, `dependency:resolve` e a camada `dependencies` como `CACHED`; os dois tempos anotados.
- [ ] **Compose:** `docker compose config -q` sem erros; `up -d --build --wait` termina com `db` e `app` **`Healthy`**; `curl localhost:8080/actuator/health` → `UP`; `POST /api/v1/auth/token` (Ana) devolve o JWT; o painel abre em `/login` (⚠️ conferir o cookie por HTTP).
- [ ] **Rede e portas:** o banco **não** está publicado no host (`Connection refused` na 5432); a aplicação só em `127.0.0.1`; `docker compose --profile tools run --rm psql -c 'select count(*) from users'` funciona.
- [ ] **Persistência:** `down` mantém os dados; `down -v` os apaga (anotado).
- [ ] **Segredos:** `docker inspect payflow-app-1` **não** mostra a senha nem o segredo do JWT; sem `secrets/jwt_secret` a aplicação **não sobe** (mensagem anotada); `git status` **não** lista `secrets/` nem `.env`; `.dockerignore` presente.
- [ ] **Erros provocados e consertados** (anotados em `docs/docker-notes.md`): permissão `chmod 600` (`AccessDeniedException`), senha do banco com volume antigo (`password authentication failed`), `ENTRYPOINT` em forma shell (`docker stop` ≈ 10 s, exit 137).
- [ ] **Desligamento:** `docker stop` leva menos de 2 s com `exit=143` e o log mostra `Commencing graceful shutdown` e `Graceful shutdown complete`.
- [ ] **Logs e depuração:** rotação `json-file` 10m × 3 (`docker inspect`); *thread dump* por `SIGQUIT`; depurador conectado pelo `compose.debug.yaml` e **desligado** depois (a 5005 sumiu do `docker compose ps`); contexto de build **antes e depois** do `.dockerignore` anotado.
- [ ] **Testes existentes continuam como estavam** (esperado; **não executado**): o módulo não toca em código de negócio. Os testes com Testcontainers (J7.05) usam o Docker do **host** e seguem iguais.
- [ ] **Pendências herdadas** registradas em `docs/docker-notes.md`: `payflow-pg` e o volume `payflow-pgdata` da J3.03 **não** conflitam com o Compose (o banco do Compose não publica a 5432 e o volume se chama `payflow_pgdata`), mas pare o `payflow-pg` se for rodar a aplicação pela IDE ao mesmo tempo que a do Compose (porta 8080).
- [ ] Commits em Conventional Commits (`feat(docker): ...`, `docs: ...`), PR mesclada e tag:
```bash
git tag payflow-J9
git push origin payflow-J9
```

## Parte 3: Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Explico container × VM e imagem × container (camadas, namespaces, cgroups, volumes) | ☐ | ☐ | ☐ |
| Uso `run`, `ps`, `logs`, `exec`, `stop`, `rm`, `-p`, `-e`, `-v` e publico portas só em `127.0.0.1` | ☐ | ☐ | ☐ |
| Escrevo um `Dockerfile` multi-stage com cache de camadas, JRE, usuário não root e `ENTRYPOINT` exec | ☐ | ☐ | ☐ |
| Dimensiono a memória da JVM no container e entendo `SIGTERM`, *graceful shutdown* e os códigos 137/143 | ☐ | ☐ | ☐ |
| Descrevo app + banco em um `compose.yaml` (rede, volume, `healthcheck`, `depends_on`, `restart`) | ☐ | ☐ | ☐ |
| Entrego configuração por ambiente e **segredos por arquivo**, sem vazá-los na imagem nem no `inspect` | ☐ | ☐ | ☐ |
| Diagnostico um container que cai (estado, código de saída, logs, `inspect`, `exec`, *thread dump*) | ☐ | ☐ | ☐ |
| Uso `.dockerignore` e sei o que um `COPY . .` sem ele pode vazar | ☐ | ☐ | ☐ |

**Regra:** com mais de uma linha em 0, repita as aulas correspondentes antes do [J10](../J10-ci-nuvem-intro/README.md). Atualize `00-guia/progresso.md`.

## Próximo
[J10: CI e introdução à nuvem](../J10-ci-nuvem-intro/README.md)
