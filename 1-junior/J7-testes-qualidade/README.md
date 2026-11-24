# J7 — Testes e qualidade

> 🟢 Júnior · ⏱️ 15h (10 sessões de 1h30) · Status: ✅ concluído (2026-11-13) — **revisado por leitura; testes práticos ficam para o fim do treinamento**

## 🎯 Objetivo do módulo
Cobrir o PayFlow com testes confiáveis e manter o código limpo.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| J7.01 | Pirâmide de testes, AssertJ e testes de unidade | 1h30 |
| J7.02 | Mockito e testes de services | 1h30 |
| J7.03 | `@WebMvcTest` e MockMvc | 1h30 |
| J7.04 | `@DataJpaTest` e `@SpringBootTest` | 1h30 |
| J7.05 | Testcontainers com PostgreSQL real | 1h30 |
| J7.06 | Cobertura com JaCoCo e introdução a TDD | 1h30 |
| J7.07 | Clean Code e code smells | 1h30 |
| J7.08 | Checkstyle, SpotBugs e SonarLint | 1h30 |
| J7.09 | Code review e Conventional Commits | 1h30 |
| J7.CP | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Arquivos: `J7.01-piramide-de-testes-assertj-e-testes-de-unidade.md`, `J7.02-mockito-e-testes-de-services.md`, `J7.03-webmvctest-e-mockmvc.md`, `J7.04-datajpatest-e-springboottest.md`, `J7.05-testcontainers-com-postgresql-real.md`, `J7.06-cobertura-com-jacoco-e-introducao-a-tdd.md`, `J7.07-clean-code-e-code-smells.md`, `J7.08-checkstyle-spotbugs-e-sonarlint.md`, `J7.09-code-review-e-conventional-commits.md`, `checkpoint.md`. Laboratórios prontos em `lab/` (`StatementLab.java` e o projeto `qa-lab/`).

> ⚠️ **Aviso de verificação (leia antes de estudar):** por decisão do aluno, os testes JUnit/Spring/Testcontainers e a configuração do JaCoCo foram **revisados por leitura** (contra as aulas J5/J6, a documentação e o conteúdo dos JARs do Boot 4.1.2), **não executados**. Foram executados: `lab/StatementLab.java` (J7.07), `lab/qa-lab` com Checkstyle e SpotBugs (J7.08), o *hook* `commit-msg` e o `ReleaseBump` (J7.09), a lógica pura de `Masking` e o `coverage-summary.sh` (J7.06). A montagem da fatia `@WebMvcTest` (J7.03) e a migração para `@PayflowTest` (J7.05) são as partes mais sujeitas a ajuste na primeira execução; as aulas mostram como ler os erros.

> **Avisos de ordem:** J7.04 usa o banco `payflow_test` do Docker (decisão 40) e só a J7.05 troca por Testcontainers; J7.02 já configura o agente do Mockito com o `argLine` que a J7.06 reaproveita (JaCoCo); J7.08 e J7.09 citam o CI (J10) apenas como destino da checagem.

## 🏗️ Entrega no PayFlow
Suíte em camadas (unidade, fatia web e JPA, integração com Testcontainers), cobertura JaCoCo medida, Checkstyle e SpotBugs no `mvn verify`, `Masking` (TDD), modelo de PR, lista de revisão e *hook* de Conventional Commits. Tag `payflow-J7`.

## ➡️ Próximo
[J8 — Segurança essencial](../J8-seguranca-essencial/README.md)
