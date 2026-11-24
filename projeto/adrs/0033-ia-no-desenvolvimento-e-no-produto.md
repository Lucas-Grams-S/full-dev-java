# ADR 0033 — IA no desenvolvimento (com política) e LLM no produto (não agora; defesas fora do modelo)

- **Status:** aceita (S7, 2026-11-23); evidência: `ai-lab` (Spring AI 2.0.2 sob o BOM do Boot 4.1.2, 6 testes com modelo falso: saída estruturada, guarda, ferramenta com dono, injeção pedindo ferramenta inexistente). **Nenhuma chamada a modelo real.**
- **Contexto:** o time usa assistentes de código; o suporte pede um assistente que responda clientes e estorne até R$ 50. O próprio treinamento registra afirmações de IA que a execução desmentiu. Prompt injection, saída sem validação e agência excessiva estão no OWASP Top 10 para LLM (2025).
- **Opções:** (A) liberar sem regras; (B) proibir IA; (C) política para o desenvolvimento e, no produto, só rascunho para humano, com ferramentas de leitura e validação determinística, depois de gatilhos.
- **Decisão:** (C). Desenvolvimento: nada de segredo ou dado pessoal em ferramenta não aprovada; código gerado com a mesma revisão e testes; APIs e versões verificadas. Produto: **não adotar agora**; se adotar, Spring AI 2.0.2 (linha do Boot 4.1), modelo explícito (`claude-opus-5-5`), sem parâmetros de amostragem, `spring.ai.tools.throw-exception-on-error: true`, ferramentas só de leitura com dono conferido pela ferramenta e **nenhuma** que mova dinheiro. Decisões 274 e 275.
- **Consequências:**
  - (+) O ganho de produtividade fica com regras claras; a segurança do produto não depende do comportamento do modelo.
  - (−) Menos automação no suporte por enquanto; a política exige ferramentas aprovadas e contrato com o provedor (LGPD).
- **Revisar quando:** contrato com um provedor aprovado pelo jurídico; volume de suporte que justifique; conjunto de avaliação aprovado pelo suporte; o PayFlow migrar para o Boot 4.2 (Spring AI 2.1).
