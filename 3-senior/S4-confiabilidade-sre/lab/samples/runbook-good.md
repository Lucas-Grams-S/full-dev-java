# Runbook: PayflowHighErrorRatio

- Dono: @plataforma
- Revisado em: 2026-11-14
- Severidade do alerta: page

## Impacto
Transferências falhando (5xx acima de 1,44% por 1 h e 5 min). Dinheiro **não** é perdido: a transferência é atômica.

## Diagnóstico (nesta ordem)
1. Abra o painel RED e confirme o escopo (todas as rotas ou só /transfers?).
2. Verifique o último deploy: `gh run list --limit 3`.
3. Procure o traceId de um erro e abra o traço.

## Mitigação
1. Houve deploy nos últimos 30 min? Faça o rollback (`./scripts/ecs-deploy.sh rollback`).
2. Banco saturado? Veja `hikaricp_connections_pending`.

## Escalar
Se o dinheiro pode estar envolvido ou 5xx > 2% por 15 min: acione @lider-plantao.

## Verificação
O erro volta abaixo de 0,1% por 10 minutos.
