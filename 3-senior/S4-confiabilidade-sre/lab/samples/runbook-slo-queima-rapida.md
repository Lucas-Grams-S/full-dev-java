# Runbook: SLO queimando rápido (PayflowTransfersAvailabilityBurnFast / LatencyBurnFast / ...Slow)

- Dono: @plataforma
- Revisado em: 2026-11-14
- Severidade do alerta: page

## Impacto
O orçamento de erro do mês está sendo gasto a 6x ou mais (14,4x = 2% do mês em 1 h). Transferências estão falhando (5xx) ou lentas (> 500 ms). Dinheiro não se perde: a transferência é atômica (decisão 12 e Outbox).

## Diagnóstico (nesta ordem, 10 minutos no máximo)
1. Abra o painel RED e responda: falha ou lentidão? Todas as rotas ou só /transfers? Todas as instâncias ou uma?
2. Houve mudança nas últimas 2 h? `gh run list --limit 5` (deploy) e a lista de migrações da S3.
3. Banco: `hikaricp_connections_pending` e `payflow_outbox_pending` (painel USE). Conexões esgotadas aponta para o pool (conta de conexões, S4.07).
4. Dependência: o breaker de notificações está aberto? (`resilience4j_circuitbreaker_state`).
5. Pegue um traceId de um erro (resposta Problem Details) e abra o traço.

## Mitigação (a mais barata primeiro; mitigar antes de entender a causa)
1. Mudança recente? Faça o rollback: `./scripts/ecs-deploy.sh rollback` (volta ao digest anterior, P9.02).
2. Flag recente? Desligue a flag (P8.04) e confirme no painel em 5 minutos.
3. Saturação? Suba tarefas (limite: a conta de conexões) ou reduza a carga (limitador, S1.09).
4. Banco com problema? Não reinicie sem o líder de plantão; siga o runbook do banco.

## Escalar
Se o dinheiro pode estar envolvido (divergência na conciliação) ou o erro passa de 5% por 15 minutos: acione @lider-plantao e declare SEV1. Se não há melhora em 30 minutos, acione @lider-plantao de qualquer forma.

## Verificação
A taxa de queima volta abaixo de 1x por 15 minutos e o alerta resolve. Registre o orçamento gasto e abra o postmortem se passou de 5% do mês.
