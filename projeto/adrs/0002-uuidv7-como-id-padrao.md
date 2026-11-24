# ADR 0002 — `uuidv7()` como valor padrão dos identificadores

- **Status:** aceita (J3, 2026-11-12)
- **Contexto:** a decisão 6 do projeto exige **UUID** para identificadores públicos. Um UUID aleatório (v4) espalha as inserções por todo o índice da chave primária; um sequencial (`BIGINT`) é enumerável (IDOR, J8) e difícil de gerar fora do banco. O PostgreSQL 18 introduziu a função `uuidv7()` (UUID ordenado por tempo), conforme as notas de versão e a documentação de funções UUID.
- **Opções:** (1) `DEFAULT uuidv7()` no banco; (2) `gen_random_uuid()`/`uuidv4()` no banco; (3) gerar UUID v4 na aplicação.
- **Decisão:** opção 1: `id UUID NOT NULL DEFAULT uuidv7()` em todas as tabelas do PayFlow. A aplicação **pode** informar o próprio UUID (por exemplo, para os dados fixos do seed).
- **Consequências:**
  - (+) Não enumerável; valores novos tendem ao fim do índice (melhor localidade que v4); sem coordenação com sequências.
  - (+) O `RETURNING id` obtém o id gerado em uma só ida ao banco (JDBC, J3.11).
  - (−) O UUID v7 **revela aproximadamente quando** a linha foi criada.
  - (−) Exige PostgreSQL **18+**: não existe em versões anteriores (para elas, seria preciso extensão ou geração na aplicação).
  - Na J5, o mapeamento JPA deve respeitar o valor gerado pelo banco (ou gerar o UUID v7 na aplicação); a decisão será verificada lá.
