# S7 — Liderança técnica

> 🟣 Sênior · ⏱️ 12h (8 sessões de 1h30) · Status: ✅ concluído (2026-11-23) — **laboratório executado (*linter* de ADR/RFC/design doc sobre as 33 ADRs reais com mutantes, *spike* de datas e fusos, estimativa por Monte Carlo e registro de riscos, hotspots no histórico real do `spring-petclinic`, leitura em paralelo com mutantes, OpenRewrite em *dry run*, JaCoCo × PIT com JUnit 6, testes instáveis, CD3 com força bruta, Spring AI 2.0.2 com modelo falso); processos com pessoas, LLM real e o PayFlow do aluno NÃO executados; testes práticos no fim do treinamento, por decisão do aluno**

## 🎯 Objetivo do módulo
**Conduzir decisões técnicas e desenvolver pessoas, com evidência**: registrar decisões em **ADRs curtas** e decidir em público com **RFCs** (prazo, um decisor); escrever **design docs** com não-objetivos, reversão e um ***spike*** que tira o risco antes de estimar; **estimar com faixa e confiança** (Monte Carlo, vazão) e manter **riscos com dono**; **medir a dívida técnica** pelo histórico do Git e **modernizar sem parar** (abstração, leitura em paralelo por versão, remoção com critério numérico, receitas automatizadas); definir a **cultura de revisão e de testes** do time (PR pequeno, *trunk-based*, mutação × cobertura, testes instáveis) e **mentorar**; **falar a língua do negócio** (custo do atraso, CD3, *error budget* como contrato, *one-pager*) e **trabalhar entre times**; e usar **IA** no desenvolvimento e no produto **sem confiar nela para segurança**. A evolução que atravessa o módulo é a das **transferências recorrentes** (o simulado do Pleno, agora com RFC, design, estimativa e decisão sênior).

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [S7.01](S7.01-adrs-e-rfcs.md) | ADRs e RFCs | 1h30 |
| [S7.02](S7.02-design-docs.md) | Design docs | 1h30 |
| [S7.03](S7.03-estimativas-e-riscos-tecnicos.md) | Estimativas e riscos técnicos | 1h30 |
| [S7.04](S7.04-divida-tecnica-e-modernizacao-de-legado.md) | Dívida técnica e modernização de legado | 1h30 |
| [S7.05](S7.05-code-review-cultura-de-testes-e-mentoria.md) | Code review, cultura de testes e mentoria | 1h30 |
| [S7.06](S7.06-comunicacao-com-produto-e-negocio.md) | Comunicação com produto e negócio | 1h30 |
| [S7.07](S7.07-ia-no-desenvolvimento-e-spring-ai.md) | IA no desenvolvimento e Spring AI | 1h30 |
| [S7.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega, autoavaliação e simulado) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): **7 programas de Java puro** (`DocLab`, `RecurrenceSpike`, `EstimateLab`, `HotspotLab`, `StranglerLab`, `FlakyLab`, `PriorityLab`), **3 projetos Maven** (`review-lab`: JaCoCo × PIT; `ai-lab`: Spring AI com modelo falso; `legacy-upgrade`: alvo do OpenRewrite), as amostras (**RFC-0001** e o **design doc** das transferências recorrentes, versões ruins), o `run-all.sh` e as **14 saídas reais** (`out-s70*`).

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório foi executado de verdade** (JDK **25.0.5.1**, Maven 3.9.12, git 2.53) e **as saídas das aulas são as reais** (determinísticas: sementes, data e *commit* fixos). **São PREMISSAS declaradas:** as estimativas e os três pontos, os riscos, o histórico de vazão, os custos do atraso, o pico das 06:00 (50 mil agendamentos, 20 ms) e os modelos do extrato. O **histórico do Git é real** (`spring-petclinic`). **Não executado:** nada com pessoas (RFC em revisão, reunião, mentoria), **nenhuma chamada a LLM** (o `ai-lab` usa um modelo falso), receitas de Spring Boot num projeto Spring, PIT no PayFlow inteiro, **o PayFlow do aluno** (módulo `recurring`, V26, troca do extrato). **A RFC-0001 está em revisão: o decisor é você.**

> **Avisos de ordem:** a S7.01 **usa as ADRs 0001–0029** como entrada do *linter* e acha erros no índice (registrados em Correções pendentes, **não corrigidos**); a S7.02 **refaz** o design "Cobranças recorrentes" do simulado do Pleno com a resposta sênior (módulo, não serviço); a S7.03 **fecha a lacuna** "estimativa de tarefas" do Pleno; a S7.04 **estende** o estrangulamento da P5.02, a depreciação da J6.06 e o CQRS da S1.05, e registra como dívida o `--add-opens` da P1.12; a S7.05 **fecha a lacuna** "fluxo de *branches*" do Pleno e aprofunda a revisão da J7.09 e o JaCoCo da J7.06; a S7.06 usa a política de *error budget* da S4.01 e o aceite de risco da S5.07; a S7.07 aplica a LGPD da S5.05 e as camadas da S5.01 a LLMs.

## 🏗️ Entrega no PayFlow
**Tag `payflow-S7`** (por leitura): **RFC-0001 e design doc** das transferências recorrentes (módulo `recurring`, **V26 reservada**), **estimativa** com p85 e premissas, **registro de riscos** e **de dívida**, **plano de modernização do extrato** (`/entries` → `/statement`) com critério numérico, `CONTRIBUTING.md` (*trunk-based*, revisão), `CODEOWNERS`, *template* de PR, **PIT** nos pacotes de dinheiro, política de **testes instáveis**, ***onboarding***, *one-pager* e **prioridades por CD3**, **mapa de times**, **política de IA** e a **RFC-0002** (assistente de suporte) em rascunho; **ADRs 0030–0033** (uma decisão cada). **Nada foi compilado no PayFlow.**

> **Migrações:** **V26 reservada** (por leitura) para `recurring_transfers` e `recurring_runs`; a próxima livre é a **V27**. O contrato HTTP `/api/v1` **não muda** (os *endpoints* de recorrência são **aditivos**).

## ➡️ Próximo
[S8 — Carreira](../S8-carreira/README.md)
