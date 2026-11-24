# Cheatsheet — Docker e Docker Compose (J9)

> Comandos conferidos no Docker 29.9.2 / Compose v5.6.1 (Linux). O `docker compose` é **com espaço** (o `docker-compose` com hífen é o antigo).

## Imagens e containers
| Objetivo | Comando |
|---|---|
| Rodar em segundo plano, com nome, variável, porta (só local) e volume | `docker run -d --name x -e VAR=v -p 127.0.0.1:8080:80 -v dados:/caminho imagem:tag` |
| Rodar uma vez e remover ao sair | `docker run --rm imagem:tag comando` |
| Limitar recursos | `--memory 512m --cpus 2` |
| Listar (em execução / todos) | `docker ps` · `docker ps -a` |
| Logs | `docker logs -f --tail 50 x` · `--since 10m` · `--timestamps` |
| Comando dentro de um container que roda | `docker exec -it x sh` (Alpine só tem `sh`) |
| Parar (SIGTERM, 10 s, SIGKILL) / iniciar / remover | `docker stop x` · `docker start x` · `docker rm x` (`-f` força) |
| Mandar um sinal sem matar | `docker kill --signal=QUIT x` (thread dump da JVM no log) |
| Entrar numa imagem sem rodar a aplicação | `docker run --rm -it --entrypoint sh imagem:tag` |
| Imagens: listar / apagar / camadas / inspecionar | `docker images` · `docker rmi i` · `docker history i` · `docker image inspect i` |
| Espaço usado | `docker system df` (cuidado com `prune --volumes`) |
| Limpar o que não está em uso | `docker container prune` · `docker image prune` · `docker builder prune` |

## Build
| Objetivo | Comando |
|---|---|
| Construir | `docker build -t nome:tag .` |
| Verificar o Dockerfile | `docker build --check .` |
| Sem cache | `docker build --no-cache .` |
| Ver o contexto e cada passo | `docker build --progress=plain .` |
| Camadas do JAR do Boot | `java -Djarmode=tools -jar app.jar extract --layers --destination out` |

## Compose
| Objetivo | Comando |
|---|---|
| Validar e ver o arquivo resolvido | `docker compose config -q` · `docker compose config` |
| Subir (constrói, espera ficar saudável) | `docker compose up -d --build --wait` |
| Estado / processos | `docker compose ps -a` · `docker compose top app` |
| Logs de um serviço | `docker compose logs -f --tail 50 --no-log-prefix app` |
| Comando no serviço (`-T` em scripts) | `docker compose exec app sh` · `docker compose exec -T db psql -U payflow -d payflow` |
| Serviço de perfil (`profiles`) | `docker compose --profile tools run --rm psql -c 'select 1'` |
| Vários arquivos | `docker compose -f compose.yaml -f compose.debug.yaml up -d --wait` |
| Derrubar (mantém volumes / **apaga** volumes) | `docker compose down` · `docker compose down -v` |

## Diagnóstico
| Sintoma | Primeiro passo |
|---|---|
| Container saiu | `docker inspect x --format 'exit={{.State.ExitCode}} oom={{.State.OOMKilled}}'` |
| Código 1 · 137 · 143 | falha na subida · `SIGKILL` (OOM ou `stop` impaciente) · `SIGTERM` normal |
| `Restarting` | `docker compose logs app`: a **primeira** falha e a `Caused by` mais interna |
| `unhealthy` | `docker inspect x --format '{{(index .State.Health.Log 0).Output}}'` |
| `AccessDeniedException: /run/secrets/...` | permissão do arquivo do host (uid do container ≠ dono); `chmod 644` só em dev |
| `password authentication failed` | volume antigo com outra senha (`POSTGRES_PASSWORD` só vale ao criar o banco) |
| `Connection refused` ao banco | URL com `localhost` em vez do nome do serviço (`db`) |
| `COPY ... not found` | o `.dockerignore` excluiu o arquivo (a mensagem não o cita) |

Veja J9.01 a J9.04 e `1-junior/J9-docker/lab/docker-lab/`.
