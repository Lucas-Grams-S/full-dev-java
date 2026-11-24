# S8.CP — Checkpoint do módulo S8

> ⏱️ ~1h30 · 🟣 Sênior · Pré-requisitos: S8.01 a S8.05

> ✅ **Validação final (2026-11-24):** módulo executado no PayFlow reconstruído; resultado e o que continua não executado no [README do módulo](README.md). Os avisos "não executado" / "por leitura" abaixo descrevem a escrita original.

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, JDK 25.0.5.1, git 2.53): `lab/PortfolioLint.java` (README de portfólio PT/EN bom, ruim e **3 mutantes**; **higiene do repositório real deste treinamento**, `HEAD` `7f3d23d`), `lab/history-secrets.sh` (segredo **falso** no histórico de um repositório descartável), `lab/ResumeLint.java` (3 currículos de pessoas **fictícias** e uma vaga **sintética**), `lab/LiveCodingLab.java` (exemplos, teste diferencial com 2.000 casos e 3 mutantes, trabalho contado), `lab/StarLint.java` (4 histórias e a matriz tema × história) e `lab/OfferLab.java` (INSS/IRRF 2026 contra os **5 exemplos oficiais** da Receita, 2 mutantes, CLT × PJ). **São PREMISSAS:** ofertas, benefícios, custos e alíquota efetiva do PJ, velocidade de fala, volumes do *system design*. **NÃO executados:** nada publicado no GitHub ou no LinkedIn, nenhuma entrevista nem negociação, nenhuma faixa salarial pesquisada (o aluno pesquisa, com fonte e data).

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Quais são as 8 seções do README de portfólio, e por que "Limitações" é a que separa portfólio de propaganda? O que o `PortfolioLint` achou no **repositório real** do treinamento?
2. Você apagou `application-local.properties` com uma senha num *commit* novo. Ela sumiu do repositório público? Qual é a **primeira** ação, segundo a documentação do GitHub, e por que reescrever o histórico não basta?
3. Por que o mutante "pronta para produção" passou na primeira versão do *linter*? Que lição da S7 ele repete?
4. Reescreva no formato da aula: "Responsável pelo desenvolvimento de APIs; melhorei a performance em 80%". Que duas perguntas o entrevistador faria ao original?
5. O que é uma tecnologia "provada", "só listada" e "ausente" no `match`? Qual falso positivo o laboratório mostrou e por quê?
6. Quais são as quatro dimensões de avaliação do *live coding*? No problema das duplicadas, que mutante **sobreviveu** aos exemplos à mão e por quê? Como o teste diferencial o pegou?
7. Por que `BigDecimal` cru como chave de `HashMap` perde duplicadas? Em quantos dos 2.000 casos isso aconteceu?
8. Para as recorrentes com 50 milhões de agendamentos (15 milhões no pico, 20 ms cada, SLO de 60 min), quantos *workers* e conexões a conta pede? Que decisão de produto evita escalar a infraestrutura?
9. O que são as quatro partes do STAR e qual deve ser a mais longa? Por que "nós" na Ação é um problema? Quais temas o banco do laboratório deixou descobertos, e por que dois deles são os "sêniores"?
10. A oferta PJ era 33,3% maior no bruto mensal. Por que ela perdeu no ano (premissas do laboratório)? Qual era o ponto de reserva a 6% e a 15%? E por que "virar gerente não é promoção"?

<details><summary>Gabarito</summary>

1. O que é, o que demonstra (competência → evidência), arquitetura (Mermaid), como rodar, como testar, decisões, **limitações**, licença. Limitações diz o que **não** está pronto ou executado (sem produção, AWS não aplicada, números de laboratório): protege você da primeira pergunta. No treinamento: **sem `LICENSE`** (pela documentação do GitHub, ninguém pode reproduzir nem adaptar) e achados de segredo que, conferidos um a um, eram **exemplos documentados** (chave `EXAMPLE` da AWS, senhas de laboratório) e falsos positivos de código.
2. **Não**: o `HEAD` mostra 0, mas o *commit* antigo continua no histórico de quem clona. Primeiro **revogar/trocar** o segredo; reescrever (com `git-filter-repo`) muda todos os *ids*, deixa o segredo em `refs/original` até limpar *reflog* e `gc`, e **não alcança** clones feitos antes (o laboratório mostrou 1 par no clone do colega).
3. A regex conhecia "pront**o**"; o README dizia "carteira… pront**a**". Corrigida para `pront[oa]s?`. Lição: uma regra que nunca falhou não prova nada; teste o próprio *linter* com mutantes (o `DocLab` da S7.01 tinha 2 *bugs* achados assim).
4. "Construí a API REST do PayFlow em Spring Boot 4.1, com erros em Problem Details e contrato OpenAPI testado" e "Medi localmente, com 1.000 linhas, que o índice trigram tirou a busca do *seq scan* (EXPLAIN antes e depois)". Perguntas: "o que **você** fez?" e "80% de quê, medido onde, contra o quê?".
5. Provada: aparece num *bullet* com o que você fez; só listada: só em Competências/Resumo; ausente: em lugar nenhum. Falso positivo: "AWS" provada por "preços da **AWS** Price List" (a palavra está no *bullet*, mas ele não prova operação na AWS). Outro: "99,9%" do sênior é meta de SLO, não medição.
6. Comunicação, resolução de problemas, competência técnica e testes (*Tech Interview Handbook*). O `noSort` (esqueceu de ordenar) sobreviveu porque **todos os exemplos estavam em ordem de tempo**; o diferencial com entradas aleatórias fora de ordem o pegou em **919 de 2.000** casos, com contraexemplo de 2 transferências (e um segundo *bug*: diferença negativa "dentro" da janela).
7. `new BigDecimal("10.0").equals(new BigDecimal("10.00"))` é `false` e os `hashCode` diferem: as duas caem em chaves diferentes. **641 de 2.000** casos. Normalize (`stripTrailingZeros` ou `setScale(2)`).
8. 15 milhões × 0,020 s = 300.000 s; ÷ 3.600 = 83,3 → **84 *workers***; vazão ~4.167/s; Little: 4.167 × 0,02 ≈ **84 conexões**. Espalhar as execuções pela janela de 60 min (o SLO já permite) ou definir "às 06:00" como "entre 06:00 e 07:00" é decisão de **produto** que reduz o pico sem infraestrutura; medir o primário antes de particionar.
9. Situação, Tarefa, Ação (a mais longa: o que **você** fez), Resultado (com número ou aprendizado). "Nós" esconde a sua parte (MIT CAPD pede "eu"). Descobertos: **motivação**, **liderança de pessoas** e ***stakeholders***; os dois últimos são os que o *Tech Interview Handbook* diz que candidatos sêniores esquecem.
10. No ano, a CLT paga 13º, 1/3 de férias, FGTS 8% e benefícios (R$ 154.100,68 no total); o PJ fatura 11 meses e paga impostos, contador e plano: a 6% de alíquota efetiva fica R$ 4.260,68 abaixo (empate só abaixo de 3,6%). Reserva: **R$ 16.412,06** (6%) e **R$ 18.149,81** (15%) por mês. Gestão é um movimento **lateral** para outra trilha, em que você volta a ser júnior em várias habilidades (Sarah Mei, citada por Charity Majors).
</details>

## Parte 2: Entrega prática (tag `payflow-S8`)
O `payflow` vira **portfólio** e você sai com currículo, banco de histórias e plano de carreira. **Nada disto muda o código da aplicação**; o detector de duplicadas da S8.03 é opcional (por leitura, a validar no fim).

```
payflow/
├── README.md                     portfólio em PT: 8 seções, Mermaid, evidências reais, limitações  (S8.01)
├── README.en.md                  a mesma coisa em inglês (paridade conferida)                      (S8.01)
├── LICENSE                       escolhida de propósito (ou ADR dizendo por que não há)            (S8.01)
├── tools/PortfolioLint.java      (ou o do laboratório) rodando no CI                               (S8.01, desafio)
└── docs/adrs/0034-...            (opcional) a sua ADR de publicação do repositório
fora do repositório (ou num repositório privado):
├── cv.md / cv.en.md              passando no ResumeLint; match contra 3 vagas reais                (S8.02)
├── historias/                    6 a 10 histórias STAR reais; StarLint bank sem tema descoberto    (S8.04)
└── carreira/ofertas.md           ofertas no OfferLab, BATNA, ponto de reserva, faixas com fonte e data, brag document (S8.05)
GitHub: topics (≤ 20), PayFlow fixado, README de perfil; LinkedIn coerente com o currículo          (S8.01, S8.02)
```

**Critérios de pronto:**
- [ ] **Portfólio:** README PT/EN passando no `PortfolioLint`; tabela de evidências com arquivos e testes que **existem**; "como rodar" executado num **clone limpo** (S8.01).
- [ ] **Higiene:** `PortfolioLint hygiene` sem `ERROR`; cada `REVIEW` conferido; histórico varrido (`git log -p -S`) **antes** do primeiro *push* público; licença decidida (S8.01).
- [ ] **Perfil:** *topics*, PayFlow fixado, README de perfil, e-mail dos *commits* decidido (S8.01).
- [ ] **Currículo:** 0 erros no `ResumeLint`; números **seus**; `match` contra 3 vagas reais com plano para as ausências; LinkedIn consistente (S8.02).
- [ ] ***Live coding*:** o problema das duplicadas refeito do zero em 45 min, falando, com exemplos de borda e a ideia do teste diferencial (S8.03).
- [ ] ***System design*:** as recorrentes em 50 milhões ensaiadas em voz alta com as contas (S8.04).
- [ ] **Comportamental:** banco de 6–10 histórias reais, sem tema descoberto (ou lacuna assumida com plano) (S8.04).
- [ ] **Oferta:** BATNA e ponto de reserva escritos; faixas com fonte e data; alíquota do PJ confirmada com contador ou marcada "a confirmar" (S8.05).
- [ ] **Carreira:** *brag document* iniciado; caminho de 3–5 anos (IC ou gestão) com o que provar no próximo degrau (S8.05).
- [ ] Tag `payflow-S8`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Sênior: **≥ 2** em todos e **3** nos marcados com ⭐.
| Competência (matriz) | Evidência no módulo | 0–3 |
|---|---|---|
| ⭐ **Liderança e comunicação:** apresenta o próprio trabalho com evidência, conta decisões e conflitos em STAR, negocia com razões e alternativas | S8.02, S8.04, S8.05 | |
| ⭐ **Git e ferramentas:** publica um repositório revisável (README PT/EN, licença, histórico sem segredo, CI visível) | S8.01 | |
| **Lógica e estruturas de dados:** resolve ao vivo com força bruta, solução melhor, complexidade e testes contra o oráculo | S8.03 | |
| **Sistemas distribuídos / Arquitetura:** conduz um *system design* com números, gargalos, *trade-offs* e o que não faria | S8.04 | |
| **Testes e qualidade:** prova que a solução e os próprios *linters* falham quando devem (mutantes, diferencial) | S8.01, S8.03, S8.05 | |
| **Segurança:** trata segredo publicado como incidente (trocar primeiro) e minimiza dados pessoais no currículo | S8.01, S8.02 | |
| **Nuvem e custo / premissas:** compara alternativas na mesma unidade, com fonte datada e premissas visíveis (ofertas) | S8.05 | |
Se **≥ 2** em tudo e **3** nos ⭐ → siga para o **Checkpoint de nível Sênior**. Linhas em **0–1**: refaça a aula indicada e o **laboratório** correspondente.

## Parte 4: Simulado: o dia de entrevistas (90 min, **com cronômetro e outra pessoa**, se possível)
> ⚠️ **Cenário SINTÉTICO:** a empresa, a vaga e a oferta são **inventadas**; os **números** do gabarito saem das **saídas reais** do laboratório (com as premissas de lá). Faça com alguém no papel de entrevistador; sozinho, grave-se.

**Cenário.** A "Fintech Exemplo" tem uma vaga de **dev Java pleno** (Java, Spring Boot, Kafka, PostgreSQL, testes; diferencial: AWS e observabilidade). O dia tem cinco etapas:
1. **Apresentação (5 min):** "fale sobre você e sobre um projeto do seu GitHub".
2. ***Live coding* (25 min):** "dado o extrato de um dia, devolva, por conta, o **maior intervalo** sem nenhum lançamento; os lançamentos podem vir fora de ordem".
3. ***System design* (30 min):** "notificações de 'sua transferência recorrente foi executada' para 15 milhões de execuções às 06:00".
4. **Comportamental (20 min):** "conte de quando você discordou de alguém" e "conte de um erro seu".
5. **Oferta (10 min):** a empresa oferece **PJ R$ 16.000/mês**; você está numa CLT de R$ 12.000 (premissas do laboratório). Negocie.

**Rubrica (100 pontos):** apresentação com evidência e ponte para a vaga (**10**); *live coding*: esclarecimentos, exemplos de borda, força bruta, solução e testes falados (**25**); *system design*: requisitos, conta, desenho, gargalo, *trade-offs* e o que não faria (**25**); comportamental: STAR com "eu", resultado e sem culpa, nas duas histórias (**20**); oferta: comparação anual, ponto de reserva, razões e "não, mas…" (**20**). **≥ 70** para seguir.

<details><summary>Gabarito (resumo)</summary>

**1.** Presente → passado → futuro; o PayFlow em uma frase, **uma** decisão com *trade-off* (ex.: travas em ordem fixa) e **uma** medição com o lugar onde foi feita; aponte a seção de limitações; termine ligando ao que a vaga pede (Kafka, testes).
**2.** Esclarecer: "intervalo dentro do dia ou incluindo meia-noite?", "conta sem lançamento no dia?", "empate?", "fuso?" (`Instant` em UTC). Exemplos: um lançamento só (intervalo do início ao fim do dia, se combinado), dois no mesmo instante (0), fora de ordem. Força bruta: para cada conta, comparar todos os pares (O(n²)). Melhor: agrupar por conta, ordenar por tempo e varrer diferenças consecutivas (O(n log n)); com `LAG` no banco (J3.08). Testes: os exemplos e, falado, o diferencial contra a força bruta com semente fixa.
**3.** Requisitos: canal (e-mail, *push*), prazo aceitável (o SLO da recorrência é 60 min: a notificação pode ser **depois**), deduplicação. Conta: 15 milhões em 60 min ≈ 4.167/s no pico **se** for síncrono; assíncrono por evento (`RecurringRunCompleted`, Outbox, P3) com o time de notificações como **X-como-serviço** (S7.06), consumidor idempotente pela chave da execução (S7), *backpressure* e limite do provedor de e-mail como gargalo real; espalhar o envio dentro da janela; não chamar o provedor dentro da transação do dinheiro.
**4.** STAR com "eu" na Ação, discordância com **dados** e "discordar e se comprometer" (S7.06); erro real com o **mecanismo** que ficou (teste, alerta), sem culpar ninguém (S4.03).
**5.** "Obrigado. Comparei no ano: com a minha CLT atual (R$ 154.100,68 com FGTS e benefícios), o PJ de R$ 16.000 só empata com alíquota efetiva abaixo de 3,6%; com a alíquota que meu contador estimar (por exemplo 6% a 15%), preciso de R$ 16.412 a R$ 18.150 por mês. Vocês conseguem chegar a esse valor, ou a CLT com o salário X?" Tudo por escrito, razões para cada pedido, alternativas (BATNA: ficar), e o que faria você dizer sim.
</details>

## ➡️ Próximo
[Checkpoint de nível Sênior](../checkpoint-senior.md) (a próxima execução é a **revisão do nível**: links, pré-requisitos, nomes, horas e a tabela "competência da matriz → aulas").
