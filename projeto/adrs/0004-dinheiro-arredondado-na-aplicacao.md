# ADR 0004 — O arredondamento do dinheiro é feito na aplicação; o banco só armazena

- **Status:** aceita (J3, 2026-11-12)
- **Contexto:** a decisão 4 do projeto define `BigDecimal` com 2 casas e `RoundingMode.HALF_EVEN` (Java) e `NUMERIC(19,2)` (banco). Mas o PostgreSQL arredonda **de outro modo** ao gravar em uma coluna com escala 2: o teste da J3.11 gravou `10.005` e o banco guardou **`10.01`** (medido na J3.04: `round(2.5)` em `numeric` deu `3`, enquanto `round(2.5::float8)` deu `2`), enquanto `HALF_EVEN` em Java daria `10.00`.
- **Opções:** (1) deixar o banco arredondar; (2) arredondar na aplicação e **rejeitar** valores com mais de 2 casas na borda do banco; (3) usar `NUMERIC` sem escala fixa.
- **Decisão:** opção 2. A aplicação arredonda com `BigDecimal` (`HALF_EVEN`) **antes** de enviar; as classes de acesso a dados (`Amounts.requireValid`) **lançam `IllegalArgumentException`** para escala maior que 2 ou valor não positivo. O banco apenas armazena o que recebe.
- **Consequências:**
  - (+) Uma única regra de arredondamento, a do Java (testável); nenhuma conversão silenciosa no banco.
  - (+) Um valor com 3 casas decimais é erro de programação, e falha alto.
  - (−) Toda entrada de dinheiro (API, J6) deve passar pelo arredondamento **antes** de chegar às classes de acesso a dados.
  - Cálculos em SQL com `round()` (relatórios) seguem a regra do PostgreSQL: só para exibição, nunca para gravar saldo.
