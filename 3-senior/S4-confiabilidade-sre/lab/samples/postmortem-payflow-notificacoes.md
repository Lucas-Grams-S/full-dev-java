# Postmortem: transferências falhando por lentidão no serviço de notificações (INCIDENTE SINTÉTICO)

- Data do incidente: 2026-11-14 (cenário construído; horários e nomes inventados)
- Autor: @plataforma
- Status: revisado
- Severidade: SEV2

## Resumo
Por 22 minutos, cerca de 80% das transferências falharam com 5xx ou demoraram mais de 5 s. A causa foi a lentidão do serviço de notificações (3 s por chamada), que a transferência chamava de forma síncrona, sem timeout, dentro da requisição. As 16 threads de atendimento ficaram presas esperando as notificações e a transferência parou de responder.

## Impacto
- Usuários: cerca de 80% das transferências durante 22 min; nenhuma divergência apareceu na conciliação (S1.12) depois do incidente.
- Orçamento de erro (SLO de disponibilidade 99,9%): 22 min × 80% × 5 req/s = 5.280 respostas ruins de 12.960 permitidas, ou 40,7% do orçamento do mês.
- Dinheiro: sem divergência na conciliação.

## Linha do tempo (UTC)
- 14:02 o serviço de notificações começa a responder em 3 s (causa externa, ainda desconhecida).
- 14:03 a latência da transferência sobe; o painel RED mostra o p99 acima de 5 s.
- 14:06 alerta `PayflowTransfersAvailabilityBurnFast` dispara (página).
- 14:09 a pessoa de plantão assume o comando e abre o painel; vê 5xx e 16 threads ocupadas.
- 14:14 o traço de uma requisição mostra 3 s dentro da chamada de notificações.
- 14:17 decisão de mitigar: abrir o circuit breaker de notificações por flag (a chamada é pulada).
- 14:24 a taxa de erro volta ao normal; o alerta resolve às 14:31.

## Causa raiz e fatores contribuintes
O sistema permitia que a falha de uma dependência não essencial consumisse todas as threads da requisição:
- a chamada de notificações era síncrona, dentro da requisição, sem timeout (o padrão era esperar até 30 s);
- o pool de 16 threads de atendimento é compartilhado entre todas as rotas;
- o circuit breaker de notificações (P5.08) existia, mas sem timeout do cliente ele só registra a chamada quando ela termina (30 s): as 16 threads travaram antes de o limiar ser atingido;
- nenhum experimento (S4.04) tinha provado esse caminho antes.

## O que funcionou
O alerta por taxa de queima disparou em 4 minutos. O traço (traceId no Problem Details) apontou a chamada lenta em 5 minutos. A flag permitiu mitigar sem deploy.

## O que não funcionou
Do alerta à mitigação passaram 11 minutos, 6 deles até alguém assumir o comando (nenhum plantão secundário). O runbook de SLO não tinha o passo "dependência lenta".

## Ações
- [ ] Timeout de 200 ms e fallback (fila do Outbox) em todas as chamadas a notificações — @backend — até 2026-11-28
- [ ] Bulkhead (pool próprio) para dependências não essenciais, com teste de carga — @backend — até 2026-12-05
- [ ] Incluir "dependência lenta" no runbook slo-queima-rapida.md — @plataforma — até 2026-11-21
- [ ] Definir plantão secundário e o tempo máximo para assumir o comando (5 min) — @lider-plantao — até 2026-12-12
- [ ] Repetir o experimento de latência injetada em notificações (game day) e anexar o resultado — @plataforma — até 2026-12-19
