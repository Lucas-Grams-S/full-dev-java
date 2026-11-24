# J6 — APIs REST

> 🟢 Júnior · ⏱️ 15h (10 sessões de 1h30) · Status: ✅ concluído (2026-11-13)


> ✅ **Validação final (2026-11-24, PayFlow reconstruído das aulas em `~/docs/payflow`, tag `payflow-J6`).** **Executado:** 130/130 testes (87 da J5 + 43 da J6); `scripts/api-smoke.sh` com **15 `ok`** contra a aplicação em `dev`; corrida de 8 requisições com a mesma chave = 1×`201` + 7×`200`. **Corrigido nas aulas:** J6.03/J6.04/J6.05 tinham blocos de teste truncados; faltava o código de `PayflowProperties.Exchange/Cors` (J6.08); o *pool* de conexões dos testes precisa de `hikari.minimum-idle` menor (13 contextos × 10 conexões > `max_connections`); a corrida de 6 transferências dá 1×`201` com *pool* aquecido e 2×`201` com o do perfil de teste (o teste confere o invariante do saldo, não a contagem). **Ainda não executado:** CORS em navegador real e a API Frankfurter real (só o servidor falso foi usado); desafios e exercícios opcionais. Os avisos "não executado" / "revisado por leitura" que aparecem nas aulas descrevem o momento em que elas foram escritas.

## 🎯 Objetivo do módulo
Projetar e documentar uma API REST consistente, com erros padronizados.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| J6.01 | Princípios REST e design de endpoints | 1h30 |
| J6.02 | DTOs vs entidades e mapeamento (manual e MapStruct) | 1h30 |
| J6.03 | Transferência entre contas: transação ACID na API | 1h30 |
| J6.04 | Tratamento global de erros e Problem Details (RFC 9457) | 1h30 |
| J6.05 | Paginação, filtros e ordenação | 1h30 |
| J6.06 | Versionamento e idempotência | 1h30 |
| J6.07 | OpenAPI com springdoc | 1h30 |
| J6.08 | CORS e consumo de APIs externas com `RestClient` (timeouts) | 1h30 |
| J6.09 | HATEOAS (conceito) e revisão da API | 1h30 |
| J6.CP | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Arquivos: `J6.01-principios-rest-e-design-de-endpoints.md`, `J6.02-dtos-vs-entidades-e-mapeamento.md`, `J6.03-transferencia-transacao-acid-na-api.md`, `J6.04-tratamento-global-de-erros-e-problem-details.md`, `J6.05-paginacao-filtros-e-ordenacao.md`, `J6.06-versionamento-e-idempotencia.md`, `J6.07-openapi-com-springdoc.md`, `J6.08-cors-e-restclient-com-timeouts.md`, `J6.09-hateoas-e-revisao-da-api.md`, `checkpoint.md`.

## 🏗️ Entrega no PayFlow
API do PayFlow (contas, depósitos, transferências ACID, extrato paginado) documentada com OpenAPI. Tag `payflow-J6`.

## ➡️ Próximo
[J7 — Testes e qualidade](../J7-testes-qualidade/README.md)
