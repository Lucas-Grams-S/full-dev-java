# Design — recurring-service

- **Autor:** time

## Contexto e escopo
Novo serviço de recorrência.

## Objetivos
Ser rápido, confiável e escalável.

## Não-objetivos

## Desenho
O serviço lê os agendamentos e chama o PayFlow. Próxima data = data atual + 30 dias.

## Alternativas consideradas

### Monólito
Não é moderno.

## Segurança
Usar OAuth.

## Observabilidade
Logs e métricas.

## Custo
Baixo.

## Plano de entrega
Subir em produção na sexta e acompanhar.

## Riscos
- Duplicidade de cobrança.
- Performance (dono: a definir)

## Perguntas em aberto
- Fuso horário?
