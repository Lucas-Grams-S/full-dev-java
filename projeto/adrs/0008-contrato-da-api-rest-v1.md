# ADR 0008 — Contrato da API REST v1

- **Status:** aceita (J6, 2026-11-13)
- **Contexto:** a API do PayFlow será consumida por clientes que não controlamos; contratos mudam devagar e erros precisam ser interpretáveis por código.
- **Opções:** versão por caminho × cabeçalho; erros `{"error":...}` próprios × Problem Details; `Page` × `Slice`; idempotência por verificação simples × com restrição única; hipermídia completa × nível 2 de Richardson.
- **Decisão:** versão **no caminho** (`/api/v1`, `version = "v1"` e predicado no `WebConfig`); DTOs `record` + **MapStruct** (`unmappedTargetPolicy = ERROR`); erros em **Problem Details** com `type` em URN (`urn:payflow:problem:*`); listas com **`Slice`** (`hasNext`), `size` ≤ 100 e desempate pela PK; `POST /transfers` com **`Idempotency-Key`** (obrigatória, até 63 caracteres, guardada como `userId:chave`; repetição = `200`, corpo diferente = `422`; a corrida é resolvida pelo `UNIQUE` + `TransferSubmitter`); **nível 2 de Richardson** (sem HATEOAS); documentação OpenAPI **desligada em produção**.
- **Consequências:**
  - (+) Contrato estável e testado; clientes decidem pelo `type`.
  - (+) Escopo da chave por usuário evita colisão sem migração.
  - (−) Offset pagina com deriva sob escrita (cursor: P2); o `409` do `@Version` exige retentativa do cliente.
  - (−) `usePathSegment` exige predicado; o springdoc exige o mesmo texto de versão (`v1`) nos dois lados.
