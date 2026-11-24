# Treinamento Dev Java Web — do Júnior ao Sênior

Trilha completa para sair do zero e chegar ao nível sênior em desenvolvimento web com **Java 25 (LTS) + Spring Boot 4.1 + PostgreSQL**, construindo um único sistema: a carteira digital **PayFlow**. Nuvem principal: AWS (com equivalência Azure/GCP).

> **Estado (2026-11-24):**
> - **Conteúdo:** gerado por inteiro: J0–J10, P1–P10, S1–S8 e os três checkpoints de nível (≈ 457,5 h, 305 sessões de 1h30).
> - **Validação prática:** em andamento. **J5–J10, P1–P4 e P7 (no k3s) já foram executadas** (PayFlow reconstruído das aulas: 256 testes, Compose na J9, CI e AWS emulada na J10) e as aulas foram corrigidas onde a execução discordou. P5, P6 e P8–P10 só nos laboratórios (reexecutados) e S1–S8 foram reexecutados só nos laboratórios (reproduzem); o mapa de migrações achou colisões novas; ainda estão **por leitura e laboratórios**: as aulas dizem, em cada caso, o que foi executado e o que não foi.
> - **Decisões suas pendentes:** numeração de migrações (V12/V13), RFC-0001, LICENSE, Spring Boot 4.2.0, índice das ADRs. Lista completa em `00-guia/progresso.md` ("Correções pendentes").
> - **Onde ver tudo:** [`00-guia/progresso.md`](00-guia/progresso.md) (histórico e decisões), [`00-guia/cronograma.md`](00-guia/cronograma.md) (fase final de testes práticos) e [`projeto/marcos.md`](projeto/marcos.md).

## Como usar
1. Leia `00-guia/como-estudar.md` e `00-guia/setup-ambiente.md`.
2. Siga `00-guia/cronograma.md` (1h30 por dia, 5 dias por semana).
3. Em cada módulo: aulas na ordem, exercício do PayFlow, `checkpoint.md`.
4. Ao fim de cada nível, faça o checkpoint de nível.

## Carga horária
| Nível | Módulos | Horas | Semanas (7h30/sem) |
|---|---|---|---|
| 🟢 Júnior | J0–J10 | 174h | ~23 |
| 🔵 Pleno | P1–P10 | 169,5h | ~23 |
| 🟣 Sênior | S1–S8 | 114h | ~15 |
| **Total** | 29 módulos | **457,5h** | **61 semanas (~14 meses)** |

## Mapa da trilha
```mermaid
flowchart LR
    J0[J0 Ambiente] --> J1[J1 Java] --> J2[J2 Web] --> J3[J3 Banco] --> J4[J4 Front] --> J5[J5 Spring] --> J6[J6 REST] --> J7[J7 Testes] --> J8[J8 Segurança] --> J9[J9 Docker] --> J10[J10 CI+Nuvem] --> CJ{{Checkpoint Júnior}}
    CJ --> P1[P1 Java a fundo] --> P2[P2 Banco avançado] --> P3[P3 Mensageria] --> P4[P4 Arquitetura] --> P5[P5 Microsserviços] --> P6[P6 Observab.] --> P7[P7 K8s] --> P8[P8 CI/CD+IaC] --> P9[P9 Nuvem] --> P10[P10 Segurança] --> CP{{Checkpoint Pleno}}
    CP --> S1[S1 Distribuídos] --> S2[S2 Performance] --> S3[S3 Dados] --> S4[S4 SRE] --> S5[S5 Segurança] --> S6[S6 Nuvem e custo] --> S7[S7 Liderança] --> S8[S8 Carreira] --> CS{{Checkpoint Sênior}}
```

## Índice
### 🟢 Júnior
[J0 Ambiente](1-junior/J0-ambiente-ferramentas/README.md) · [J1 Java e lógica](1-junior/J1-java-logica/README.md) · [J2 Como a web funciona](1-junior/J2-como-a-web-funciona/README.md) · [J3 Banco de dados](1-junior/J3-banco-de-dados/README.md) · [J4 Front-end](1-junior/J4-front-end-essencial/README.md) · [J5 Spring Boot](1-junior/J5-spring-boot/README.md) · [J6 APIs REST](1-junior/J6-apis-rest/README.md) · [J7 Testes](1-junior/J7-testes-qualidade/README.md) · [J8 Segurança](1-junior/J8-seguranca-essencial/README.md) · [J9 Docker](1-junior/J9-docker/README.md) · [J10 CI e nuvem](1-junior/J10-ci-nuvem-intro/README.md) · [Checkpoint](1-junior/checkpoint-junior.md)

### 🔵 Pleno
[P1 Java a fundo](2-pleno/P1-java-a-fundo/README.md) · [P2 Banco avançado](2-pleno/P2-banco-persistencia-avancados/README.md) · [P3 Mensageria](2-pleno/P3-comunicacao-sincrona-assincrona/README.md) · [P4 Arquitetura](2-pleno/P4-arquitetura-aplicacoes/README.md) · [P5 Microsserviços](2-pleno/P5-microsservicos-integracao/README.md) · [P6 Observabilidade](2-pleno/P6-observabilidade/README.md) · [P7 Kubernetes](2-pleno/P7-containers-kubernetes/README.md) · [P8 CI/CD e IaC](2-pleno/P8-cicd-iac/README.md) · [P9 Nuvem](2-pleno/P9-nuvem-intermediaria/README.md) · [P10 Segurança](2-pleno/P10-seguranca-intermediaria/README.md) · [Checkpoint](2-pleno/checkpoint-pleno.md)

### 🟣 Sênior
[S1 Distribuídos ✅](3-senior/S1-sistemas-distribuidos/README.md) · [S2 Performance ✅](3-senior/S2-performance-eficiencia/README.md) · [S3 Dados em escala ✅](3-senior/S3-dados-em-escala/README.md) · [S4 SRE ✅](3-senior/S4-confiabilidade-sre/README.md) · [S5 Segurança ✅](3-senior/S5-seguranca-avancada/README.md) · [S6 Nuvem e custo ✅](3-senior/S6-arquitetura-nuvem-custo/README.md) · [S7 Liderança ✅](3-senior/S7-lideranca-tecnica/README.md) · [S8 Carreira ✅](3-senior/S8-carreira/README.md) · [Checkpoint ✅](3-senior/checkpoint-senior.md)

## Guia e recursos
[Progresso](00-guia/progresso.md) · [Cronograma](00-guia/cronograma.md) · [Matriz de competências](00-guia/matriz-de-competencias.md) · [O que o mercado pede](00-guia/o-que-o-mercado-pede.md) · [Versões](00-guia/versoes.md) · [Projeto central](00-guia/projeto-central.md) · [Setup](00-guia/setup-ambiente.md) · [Como estudar](00-guia/como-estudar.md) · [Glossário](00-guia/glossario.md) · [Marcos do projeto](projeto/marcos.md) · [Recursos](recursos/biblioteca.md)

## Para continuar em uma nova sessão
> Leia `00-guia/instrucoes-do-treinamento.md` e `00-guia/progresso.md` e gere o próximo módulo pendente, seguindo as regras.
