# Laboratório da S8 — Carreira

Programas pequenos que dão **evidência** às decisões de carreira: conferem o portfólio e o repositório publicado, mostram o que o histórico do Git guarda, leem um currículo como um recrutador, resolvem um problema de *live coding* contra a força bruta, conferem histórias STAR e comparam ofertas na mesma unidade (com as **tabelas oficiais** de INSS e IR de 2026).

## Requisitos
- JDK 25 (`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`; o `java` padrão da máquina do curso é o 17).
- `git` (para `history-secrets.sh` e `PortfolioLint hygiene`) e `python3` (um passo do `run-all.sh`).
- **Nada** de Maven, Docker ou rede.

## Rodar tudo
```bash
./run-all.sh                 # regrava as saídas out-s80*
ONLY=s805 ./run-all.sh       # só uma seção (s801 … s805)
```

## Conteúdo
| Aula | Arquivo | O que mostra | Saída |
|---|---|---|---|
| S8.01 | `PortfolioLint.java readme` | README de portfólio em PT e EN: seções, paridade, diagrama, "como rodar", links relativos, afirmações sem evidência, segredos; README ruim e **3 mutantes** | `out-s801-readme.txt` |
| S8.01 | `PortfolioLint.java hygiene` | o que está **publicado** (o `HEAD`) do **próprio repositório do treinamento**: licença, README, `.gitignore`, arquivos arriscados, segredos (valores mascarados) | `out-s801-hygiene.txt` |
| S8.01 | `history-secrets.sh` | segredo **falso** apagado num *commit* novo continua no histórico; reescrever não alcança um clone anterior | `out-s801-history.txt` |
| S8.02 | `ResumeLint.java` | currículo júnior ruim × bom × sênior (sintético): verbos, números, honestidade, dados pessoais, competências sem evidência, **sinais de nível**; cobertura de uma vaga sintética | `out-s802-resume.txt` |
| S8.03 | `LiveCodingLab.java` | transferências duplicadas: força bruta × solução rápida, exemplos de borda, **teste diferencial** (2.000 casos) e **3 mutantes**, trabalho contado | `out-s803-livecoding.txt` |
| S8.04 | `StarLint.java` | histórias STAR (3 boas, 1 ruim) e a matriz **tema × história** sem silêncio; mutante contado com "nós" | `out-s804-star.txt` |
| S8.05 | `OfferLab.java` | INSS e IRRF 2026 conferidos contra os **5 exemplos oficiais** da Receita (+ 2 mutantes); CLT × PJ no ano, taxa de empate e fatura mínima | `out-s805-offer.txt` |

`samples/` tem o **README de portfólio do PayFlow** (PT e EN, com `LICENSE` MIT e *stubs* dos arquivos linkados), o README ruim, **três currículos** (júnior ruim, júnior bom e sênior, todos de pessoas **fictícias**), uma **vaga sintética** e **quatro histórias STAR** (três boas, uma ruim).

## Limites (leia)
- As regras dos *linters* são **heurísticas deste curso**, não um padrão de mercado nem um ATS. Elas têm falsos positivos (a meta de SLO "99,9%" no currículo sênior) e já tiveram *bugs* achados por mutante ("pronta" × "pronto para produção").
- `PortfolioLint hygiene` lê o `HEAD` do repositório do treinamento: a saída **muda** quando você faz *commit*. Os achados de segredo são **exemplos documentados** (chave `EXAMPLE` da AWS, senhas de laboratório) e falsos positivos de código; nenhum valor é impresso.
- `OfferLab` **não é consultoria tributária**: as tabelas de INSS e IRRF de 2026 foram lidas nas páginas oficiais (gov.br) em 23/11/2026; as ofertas, benefícios, custos do PJ, meses faturados e a **alíquota efetiva do PJ** são **premissas** (a do PJ depende do regime: pergunte a um contador). O 13º e o mês de férias usam aproximações declaradas no código.
- Os currículos, a vaga e as histórias são **modelos**: no seu currículo e nas suas histórias, só fatos e números **seus**.
