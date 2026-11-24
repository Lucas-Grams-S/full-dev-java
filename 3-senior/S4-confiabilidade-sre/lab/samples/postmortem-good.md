# Postmortem: transferências lentas (SINTÉTICO)

- Data do incidente: 2026-11-14
- Autor: @plataforma
- Status: revisado
- Severidade: SEV2

## Resumo
Por 22 minutos, 7% das transferências levaram mais de 2 s.

## Impacto
~7% das transferências lentas; nenhuma perdida; 0,4% do error budget do mês.

## Linha do tempo (UTC)
- 14:02 deploy da versão 41.
- 14:09 alerta `PayflowTransfersSlow` (ticket).
- 14:16 humano abre o painel e identifica o endpoint.
- 14:24 rollback concluído.

## Causa raiz e fatores contribuintes
O sistema permitia uma consulta sem índice chegar à produção: o CI não tinha um teste de plano de execução.

## O que funcionou
O alerta por sintoma e o rollback com um comando.

## O que não funcionou
O alerta era ticket e ninguém olhou por 7 minutos.

## Ações
- [ ] Adicionar teste de plano de execução no CI — @backend — até 2026-12-05
- [ ] Rever a severidade de TransfersSlow à luz do SLO — @plataforma — até 2026-12-12
