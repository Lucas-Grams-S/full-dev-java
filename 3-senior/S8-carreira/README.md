# S8 — Carreira

> 🟣 Sênior · ⏱️ 9h (6 sessões de 1h30) · Status: ✅ concluído (2026-11-24) — **validado na execução final (2026-11-24); veja o bloco abaixo**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído em `~/docs/payflow`, tag `payflow-S8`).** **Executado:** `run-all.sh` numa cópia (~15 s: `PortfolioLint`, `ResumeLint`, `LiveCodingLab`, `StarLint`, `OfferLab` com INSS/IRRF 2026, `history-secrets.sh` num `git` real em diretório temporário): **6 de 7 saídas idênticas**; a que difere (`out-s801-hygiene`) analisa o **repositório de verdade** (o `HEAD` mudou desde a aula). **Corrigido nas aulas:** nenhuma divergência. **Ainda não executado:** o portfólio no GitHub (README PT/EN num clone limpo, `LICENSE`, histórico varrido antes do *push* público: a **LICENSE continua ausente** no repositório). Os avisos "não executado" / "revisado por leitura" nas aulas descrevem a escrita original.

## 🎯 Objetivo do módulo
**Posicionar-se no mercado e passar em processos seletivos com evidência e sem exagero**: transformar o PayFlow num **portfólio** revisável (README em português e inglês, diagrama, decisões, limitações, licença, histórico sem segredo); escrever **currículo e LinkedIn** que mostram o nível pelo escopo e por números medidos; conduzir **entrevistas técnicas e *live coding*** (esclarecer, força bruta, solução melhor, testes contra o oráculo); conduzir ***system design*** como entrevista e contar **histórias STAR** reais por tema; **negociar** comparando ofertas na mesma unidade (com as tabelas oficiais de 2026) e escolher um **caminho depois do sênior** (*staff*/principal ou gestão).

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [S8.01](S8.01-portfolio-no-github.md) | Portfólio no GitHub | 1h30 |
| [S8.02](S8.02-curriculo-e-linkedin-por-nivel.md) | Currículo e LinkedIn por nível | 1h30 |
| [S8.03](S8.03-entrevistas-tecnica-e-live-coding.md) | Entrevistas: técnica e live coding | 1h30 |
| [S8.04](S8.04-entrevistas-system-design-e-comportamental-star.md) | Entrevistas: system design e comportamental (STAR) | 1h30 |
| [S8.05](S8.05-negociacao-e-caminhos-depois-do-senior.md) | Negociação e caminhos depois do sênior | 1h30 |
| [S8.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega, autoavaliação e simulado do dia de entrevistas) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): **5 programas de Java puro** (`PortfolioLint`, `ResumeLint`, `LiveCodingLab`, `StarLint`, `OfferLab`), o script `history-secrets.sh`, as amostras (**README de portfólio do PayFlow** em PT/EN com `LICENSE`, README ruim, **3 currículos** de pessoas fictícias, uma **vaga sintética**, **4 histórias STAR**), o `run-all.sh` e as **7 saídas reais** (`out-s80*`).

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório foi executado de verdade** (JDK **25.0.5.1**, git 2.53) e **as saídas das aulas são as reais** (determinísticas: sementes e datas fixas; só a higiene do repositório muda quando você faz *commit*). **Conferidos nas fontes:** documentação do GitHub (README, Mermaid, *topics*, fixados, perfil, licença, *push protection*, remoção de dados sensíveis, e-mail dos *commits*), *Tech Interview Handbook*, MIT CAPD (STAR), Hello Interview, tabelas oficiais de **INSS e IRRF de 2026** e os **5 exemplos** da Receita, FGTS de 8%, PON/Harvard (BATNA), StaffEng, Charity Majors, Dropbox, Julia Evans. **São PREMISSAS:** ofertas, benefícios e custos do PJ, **alíquota efetiva do PJ** (varrida de 6% a 20%), velocidade de fala (130 palavras/min) e os volumes do *system design* (os da S7 × 1.000). **Não há números de mercado** (salários, taxas de resposta, "% de currículos descartados"): nenhum tinha fonte primária conferível. **Não é consultoria tributária nem jurídica.** **Não executado:** nada publicado, nenhuma entrevista ou negociação, o PayFlow do aluno.

> **Avisos de ordem:** a S8.01 **mede o próprio repositório deste treinamento** (público) e acha que ele **não tem `LICENSE`** (registrado em Correções pendentes, **não** corrigido: a decisão é do aluno); os achados de segredo são exemplos documentados. A S8.02 usa a matriz de competências e o `o-que-o-mercado-pede.md`; a S8.03 retoma `BigDecimal.equals` (J1.15) e `LAG` (J3.08); a S8.04 escala as recorrentes da S7 e usa os temas do *Tech Interview Handbook*; a S8.05 aplica a S7.06 à negociação. O banco de perguntas de entrevista do curso continua em `recursos/perguntas-entrevista.md`.

## 🏗️ Entrega no PayFlow
**Tag `payflow-S8`** (por leitura): `README.md` e `README.en.md` de **portfólio** (8 seções, Mermaid, tabela competência → evidência, limitações), **`LICENSE`** escolhida de propósito, higiene e histórico conferidos **antes** do *push* público, *topics*, PayFlow fixado e README de perfil; fora do repositório: **currículo** PT/EN (`ResumeLint` sem erros, `match` contra 3 vagas reais), **banco de histórias STAR** sem tema descoberto, **ofertas** no `OfferLab` com BATNA e ponto de reserva, faixas com fonte e data, ***brag document*** e o caminho de 3–5 anos. **ADR 0034** (publicar o PayFlow como portfólio). **Nada foi compilado no PayFlow.**

> **Migrações:** **nenhuma** (a **V26** continua **reservada** pela S7 para as recorrentes; a próxima livre é a **V27**). O contrato HTTP `/api/v1` **não muda**.

## ➡️ Próximo
[Checkpoint de nível Sênior](../checkpoint-senior.md)
