# ADR 0009 — Estratégia de testes em camadas, banco real por Testcontainers e portões de qualidade

- **Status:** aceita (J7, 2026-11-13). Revisada por leitura; a validação prática fica para o fim do treinamento.
- **Contexto:** a suíte da J5/J6 (130 testes) é quase toda de integração, depende de um PostgreSQL local subido à mão (`payflow_test`) e roda lenta e acoplada à máquina do aluno. Faltam testes rápidos de regra, medição de cobertura e verificação automática de estilo e de bugs.
- **Opções:** (1) manter só testes de integração; (2) H2 para acelerar; (3) pirâmide com unidade, fatias e integração, com PostgreSQL real em container; (4) mocks em tudo; portões de qualidade rígidos desde o primeiro dia × estreitos e crescentes.
- **Decisão:** **pirâmide em camadas**: unidade (JUnit + AssertJ, sem Spring), unidade de *service* com Mockito (`new` no construtor, sem `@InjectMocks`), fatia web (`@WebMvcTest`, `MockMvc`, `@MockitoBean`), fatia JPA (`@DataJpaTest` com `replace = NONE`, `flush` para provar constraints) e integração (`@SpringBootTest`); **banco sempre PostgreSQL 18 real** por **Testcontainers** (`@Bean` + `@ServiceConnection`, anotação composta `@PayflowTest`), **nunca H2**; `ResetDatabase` continua por teste; **JaCoCo** com piso **medido** (não copiado) e exclusões justificadas; **Checkstyle** (regras de baixo ruído) em `validate` e **SpotBugs** em `verify`, começando por uma rodada de triagem; **Conventional Commits** com *hook* local e a mesma checagem no CI (J10); refatoração separada de mudança de comportamento, protegida por testes de caracterização.
- **Consequências:**
  - (+) Feedback rápido na base da pirâmide; o banco de teste é o mesmo da produção e não depende da máquina; defeitos de dinheiro e `null` são achados antes da revisão humana.
  - (+) O CI (J10) herda `mvn verify` como portão sem mudanças.
  - (−) Cada contexto distinto do Spring cria **um container** (custo de *startup*): padronizar propriedades e `@MockitoBean`; considerar um container compartilhado se medir necessidade.
  - (−) A migração dos testes antigos para `@PayflowTest` é mecânica, mas ampla; exige rodar a suíte inteira.
  - (−) SpotBugs e Checkstyle exigem triagem inicial e supressões justificadas por classe e padrão; um portão rígido demais em código antigo trava o time.
