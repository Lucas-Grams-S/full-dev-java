# S1 — Sistemas distribuídos e system design

> 🟣 Sênior · ⏱️ 21h (14 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **laboratório de Java puro executado (OpenJDK 25.0.5, 13 programas, sem Maven e sem Docker; são modelos e simulações); o PayFlow do aluno foi revisado apenas por leitura; testes práticos no fim do treinamento, por decisão do aluno**

## 🎯 Objetivo do módulo
Projetar sistemas distribuídos **justificando cada trade-off**: escalar (e saber **o que impede**), escolher a **consistência por operação**, **replicar e particionar** com critério, usar **cache** sem vazar dado velho ou privado, separar **leitura e escrita** (CQRS) e decidir **contra** *event sourcing*, coordenar **sagas** sem 2PC, tornar **cada salto idempotente**, **limitar** tráfego de forma distribuída, **ordenar** eventos sem confiar no relógio de parede e **conduzir um *system design*** (encurtador de URL, pagamentos, *feed* e notificações) com **método**, **números** e **trade-offs**.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [S1.01](S1.01-escalabilidade-vertical-vs-horizontal-e-balanceamento-de-carga.md) | Escalabilidade vertical vs horizontal e balanceamento de carga | 1h30 |
| [S1.02](S1.02-cap-pacelc-e-modelos-de-consistencia.md) | CAP, PACELC e modelos de consistência | 1h30 |
| [S1.03](S1.03-replicacao-e-particionamento.md) | Replicação e particionamento | 1h30 |
| [S1.04](S1.04-estrategias-de-cache-e-cdn.md) | Estratégias de cache e CDN | 1h30 |
| [S1.05](S1.05-cqrs.md) | CQRS | 1h30 |
| [S1.06](S1.06-event-sourcing.md) | Event sourcing | 1h30 |
| [S1.07](S1.07-sagas-e-transacoes-distribuidas-por-que-evitar-2pc.md) | Sagas e transações distribuídas (por que evitar 2PC) | 1h30 |
| [S1.08](S1.08-idempotencia-ponta-a-ponta.md) | Idempotência ponta a ponta | 1h30 |
| [S1.09](S1.09-rate-limiting-distribuido.md) | Rate limiting distribuído | 1h30 |
| [S1.10](S1.10-relogios-e-ordenacao-de-eventos.md) | Relógios e ordenação de eventos | 1h30 |
| [S1.11](S1.11-metodo-de-system-design-encurtador-de-url.md) | Método de system design: encurtador de URL | 1h30 |
| [S1.12](S1.12-system-design-sistema-de-pagamentos.md) | System design: sistema de pagamentos | 1h30 |
| [S1.13](S1.13-system-design-feed-de-noticias-e-notificacoes.md) | System design: feed de notícias e notificações | 1h30 |
| [S1.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): **13 programas de Java puro** (`java X.java`), um por aula, com as saídas reais em `lab/out-s101.txt` … `out-s113.txt` e `lab/run-all.sh` para regravá-las.

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório** (balanceamento sobre filas, quóruns e partição, anel de *hash*, cache e *stampede*, projeção e reconstrução, log de eventos, saga e 2PC, idempotência, limitadores, relógios, colisões de códigos, *fan-out* e conciliação) **foi executado** e **as saídas das aulas são as reais**. **São modelos:** **não** medem PostgreSQL, Kafka, Redis, ALB nem CDN, e vários cenários usam **parâmetros inventados** (declarados em cada aula). **Não foi executado:** o **PayFlow do aluno** (migrações **V13–V17**, projetor do extrato, orquestrador da saga, `idempotency_keys`, filtro de *rate limit*, `Clock`, conciliação), **Redis/Lua**, **Bucket4j**, **Testcontainers**, **ALB/CloudFront**, **PSP**, *event store* real, motor de *workflow* (Temporal), `PREPARE TRANSACTION`. A aula S1.07 supõe, **como hipótese de estudo**, o **antifraude extraído** como serviço (decisão 190). Um link (ACM Queue, Pat Helland) **não pôde ser verificado** (o site recusou o `curl`) e está marcado ⚠️.

> **Avisos de ordem:** a S1.01 retoma a **Lei de Little**/disponibilidade em cadeia (P3.01) e a **decisão 152/171** (conta de conexões); a S1.02 retoma isolamento (P2.01) e **atraso da réplica** (P9.08); a S1.04 retoma o **Redis** (P2.09); a S1.05 usa **Outbox**, **consumidor idempotente** e "**um dono por tabela**" (P3.07, P3.09, P4.11); a S1.07 usa **timeout/breaker** (P5.07/P5.08) e **locks** (P2.02); a S1.08 retoma `Idempotency-Key` (J6.06); a S1.09 retoma o **gateway** (P5.05) e **`azp`/`sub`** (P10); as aulas **S1.11–S1.13** usam **os blocos** de S1.01–S1.10 (leia-os antes).

## 🏗️ Entrega no PayFlow
**Tag `payflow-S1`** (por leitura): a **saga de pagamentos** orquestrada no módulo `payment` (reserva → antifraude → captura = **pivô** → notificar, com **compensação**, **timeout = desconhecido** e **política** escrita), o **extrato em CQRS** (projetor idempotente com **sequência por conta**, **lacuna recusada**, **reconstrução**, **token de versão** com *fallback*), **idempotência por passo** até o PSP, **limite global no gateway** (balde de fichas no Redis, **modo degradado**), **ordenação por `account_seq`** (e `Clock` injetado), **`/api/**` com `no-store`**, a **conciliação** e **ADR 0024** com as decisões **de não fazer** (*sharding*, *event sourcing*) e seus **gatilhos**. Migrações **V13–V17**. **Contrato HTTP `/api/v1` inalterado** (os novos *endpoints* são aditivos).

## ➡️ Próximo
[S2 — Performance e eficiência](../S2-performance-eficiencia/README.md)
