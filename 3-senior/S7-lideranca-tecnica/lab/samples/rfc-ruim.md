# RFC — Recorrência

- **Status:** aprovada
- **Autor:** time
- **Decisor:** tech lead, produto e arquitetura
- **ADR resultante:** —

## Resumo
Vamos criar o recurring-service com Kafka e um banco próprio, como no desenho do Pleno.

## Problema
Os usuários querem agendar transferências.

## Proposta
Um microsserviço novo que consome eventos e chama o PayFlow. Detalhes TBD.

## Alternativas

### A. recurring-service (proposta)
É o padrão de mercado.

## Perguntas em aberto
- Limite de valor? TODO

## Decisão
Aprovada na reunião de terça.
