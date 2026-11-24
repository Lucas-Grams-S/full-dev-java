# Cheatsheet: liderança técnica (S7)

> Os números de exemplo vêm do laboratório da S7 com **premissas declaradas** (estimativas, riscos, custo do atraso). O método vale; os valores, não.

## Qual documento?
| Situação | Artefato |
|---|---|
| decisão local e barata de reverter | descrição do PR |
| decisão significativa (cara de reverter, muda estrutura, outro time herda) | **ADR** (1 página, uma decisão) |
| afeta outros times, é polêmica ou cara | **RFC** (prazo + um decisor) → ADR |
| "como" de uma mudança com risco (dinheiro, dados, segurança, > ~2 semanas) | **design doc** (+ *spike*) |

## ADR (formato do PayFlow)
`Status` (`proposta|aceita|rejeitada|substituída|obsoleta`) · `Contexto` (fatos) · `Opções` (≥ 2, "não fazer nada") · `Decisão` (voz ativa) · `Consequências` **(+) e (−)** · `Revisar quando` (gatilho verificável). **Imutável**: mudou → ADR nova que substitui.

## RFC
DACI: **D**river · **A**pprover (**um só**) · **C**ontributors · **I**nformed · prazo de comentários · alternativas · riscos e perguntas **com dono** · seção "Decisão" → ADR. Ciclo: rascunho → em revisão → aceita | rejeitada | retirada → (substituída).

## Design doc
Contexto e escopo · **objetivos com número** · **não-objetivos** (razoáveis, não negados) · desenho + diagrama · alternativas **com porquê** · segurança (STRIDE) · observabilidade (SLO) · custo datado · plano com **reversão** · riscos e perguntas com dono. Revisão: contratos cedo; comentários antes; reunião só com perguntas abertas.

## Estimativa
- **Estimativa ≠ meta ≠ compromisso.** Responda com **faixa + confiança + premissas**; compromisso no **p85**; reestime nos marcos.
- Tarefa: quebre em passos ≤ 1–2 dias; **o, m, p**; inclua teste, revisão, *deploy*, espera; diga o desconhecido.
- PERT ≈ `(o + 4m + p) / 6`; a soma dos "mais prováveis" quase nunca acontece (**0,8%** no exemplo).
- Vazão: sorteie semanas do histórico até completar o *backlog*; "total ÷ média" = **mediana** (atrasa ~metade das vezes).
- Calendário = dias ideais ÷ fator de foco (declarado).

## Riscos
`exposição = P × impacto` · resposta (evitar, mitigar, transferir, aceitar) · **dono** · **gatilho** · mitigação **se paga** se `exposição antes − depois > custo`. Aconteceu → é problema, não risco.

## Dívida e legado
- Quadrante: prudente/imprudente × deliberada/inadvertida. **Juros** (custo recorrente) × **principal**.
- **Hotspot** = revisões × complexidade (o Git dá revisões; só revisões engana). **Acoplamento de mudança** = mudam juntos.
- Histórico para hotspots:
  ```bash
  git log -M --numstat --format='@%H|%ad' --date=short <commit>
  ```
- Modernizar: abstração (*branch by abstraction*) → novo atrás → **paralelo comparado por versão** → *flag* gradual → `Deprecation`/`Sunset` → **remoção** (arquitetura de transição tem data).
- Desligar o velho: **0 bugs em n leituras → taxa < 3/n** (regra de três, 95%).
- OpenRewrite *dry run* (uma receita por PR; licença das receitas: Moderne Source Available):
  ```bash
  mvn org.openrewrite.maven:rewrite-maven-plugin:6.46.1:dryRun \
    -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-migrate-java:3.42.1 \
    -Drewrite.activeRecipes=org.openrewrite.java.migrate.UpgradeToJava25
  ```

## Revisão, *branches* e testes
- PR ~100 linhas (1.000 = grande demais) · resposta em **1 dia útil** · `issue (blocking):` / `nitpick (non-blocking):` · `CODEOWNERS` · máquina revisa estilo, segredos, SCA, ArchUnit, documentos.
- ***Trunk-based***: *branches* ≤ 1–2 dias, *flags*, CI obrigatório. Git Flow só com várias versões mantidas.
- **Cobertura mede execução; mutação (PIT) mede verificação** (100% de cobertura com 6/18 mutantes no exemplo).
  ```bash
  mvn test org.pitest:pitest-maven:mutationCoverage
  ```
- Instáveis: `P(vermelho) = 1 − (1 − p)^N`; **sem nova tentativa silenciosa**; quarentena com dono e prazo.

## Mentoria
Mentor (experiência) · *coach* (perguntas) · patrocinador (portas) · *onboarding* com PR pequeno na semana 1 · crescimento pela matriz · *feedback* **SBI** · delegação crescente.

## Produto e negócio
- ***One-pager***: recomendação primeiro · por quê em dinheiro/risco/prazo com premissa · opções (com "não fazer") · o que precisamos · quando revisitar.
- **CD3** = custo do atraso ÷ duração (maior primeiro); **fatiar** a dívida para entregar cedo a parte que reduz o risco.
- *Error budget* = contrato assinado por produto; exceção **por escrito**.
- *Team Topologies*: colaboração (com data de fim) · X-como-serviço · facilitação. Dependência = contrato.

## IA
- Desenvolvimento: sem segredo/dado pessoal em ferramenta não aprovada · código gerado = código de terceiro · verificar APIs e versões · agentes com permissão mínima.
- Produto (Spring AI 2.0.2 + Boot 4.1): modelo **explícito**, sem `temperature` no Opus 5.5, `spring.ai.tools.throw-exception-on-error: true`, ferramentas **só de leitura** com dono conferido pela ferramenta, **nenhuma** que mova dinheiro, saída validada, humano no circuito.
  ```yaml
  spring.ai.anthropic.chat.model: claude-opus-5-5
  spring.ai.tools.throw-exception-on-error: true
  ```
