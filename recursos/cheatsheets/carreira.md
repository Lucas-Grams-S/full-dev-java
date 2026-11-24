# Cheatsheet: carreira (S8)

> As ofertas, custos do PJ e volumes de exemplo são **premissas** do laboratório da S8. As tabelas de INSS e IRRF são as **oficiais de 2026** (lidas em 23/11/2026): confira a data antes de usar. Não é consultoria tributária.

## README de portfólio (PT e EN, mesmas seções)
1. O que é (diga que é **de estudo**) · 2. O que demonstra (**competência → onde ver → evidência**) · 3. Arquitetura (um Mermaid) · 4. Como rodar (poucos comandos, testados num **clone limpo**) · 5. Como testar · 6. Decisões (ADRs, RFC, design doc) · 7. **Limitações** · 8. Licença.

## Antes do primeiro *push* público
- `git log -p -S '<texto>'` e uma varredura de segredos em **todos** os *commits* (não só o `HEAD`).
- Achou segredo: **trocar/revogar primeiro**; depois `git-filter-repo`; reescrever não alcança clones anteriores.
- `LICENSE` decidida (sem licença: visível, mas ninguém pode reusar); e-mail dos *commits* decidido (*no-reply*).
- *Topics* (≤ 20, minúsculas, ≤ 50 caracteres), até 6 fixados, README de perfil (repositório com o nome do usuário).

## *Bullet* de currículo
`Verbo na 1ª pessoa` + `o que você fez` + `resultado com número` + `onde foi medido`.
Evite: "Responsável por", "Participei de", "conhecimento em", número sem contexto, "em produção" em projeto de estudo, CPF/nascimento/estado civil/foto.
Por nível: júnior = testado e entregue; pleno = ponta a ponta com operação e números; sênior = **decisão, escopo entre times, pessoas, risco e custo**.

## *Live coding* (~45 min, premissa)
Esclarecer (cada resposta vira exemplo) → exemplos de borda **fora de ordem** também → força bruta (o oráculo) → solução melhor (de onde vem o ganho) → testar falando → complexidade e "não cabe em memória".
Avaliam: **comunicação, resolução de problemas, competência técnica, testes**.
Armadilha Java: `BigDecimal` em chave de `HashMap` → normalize (`stripTrailingZeros()`/`setScale(2)`).

## *System design* (Hello Interview)
Requisitos ~5 · entidades ~2 · API ~5 · (fluxo ~5) · alto nível ~10–15 · aprofundamentos ~10 min. O nível aparece nos **números**, no **gargalo**, nos ***trade-offs*** e no **que você não faria**.
Conta rápida: trabalho = itens × tempo por item; *workers* = trabalho ÷ janela; conexões ≈ vazão × tempo (Little).

## STAR
**S**ituação (curta) · **T**arefa (o que **você** precisava) · **A**ção (a mais longa, com "eu") · **R**esultado (número ou aprendizado). Sem culpa. Banco de 6–10 histórias reais cobrindo: motivação, iniciativa, ambiguidade, perseverança, conflito, empatia, crescimento, comunicação, **liderança de pessoas**, ***stakeholders***.

## Oferta
- Compare **no ano**: CLT = 12 salários + 13º + 1/3 de férias (líquidos) + FGTS 8% + benefícios; PJ = faturas dos meses trabalhados − impostos do regime − contador − benefícios que você paga.
- INSS 2026 (empregado): 7,5% / 9% / 12% / 14% por faixa até R$ 8.475,55 (teto: R$ 988,09).
- IRRF 2026: desconto simplificado R$ 607,20 (se melhor); redução zera até **R$ 5.000** e decresce até **R$ 7.350** (calculada sobre o **salário**, não a base).
- **BATNA** → **ponto de reserva** escrito **antes**. Não dê o número primeiro; na "pretensão", faixa pesquisada do **cargo** (com fonte e data).
- Regras (Haseeb): por escrito · porta aberta · informação é poder · positivo · não seja quem decide · alternativas · razões · mais que dinheiro · entenda o que valorizam · seja "ganhável".

## Depois do sênior
*Staff*/principal (arquétipos: *Tech Lead*, *Architect*, *Solver*, *Right Hand*) × gestão (pessoas, entrega, saúde do time). Gestão é **movimento lateral**, não promoção; o pêndulo vale nos dois sentidos. Promoção pede evidência: ***brag document*** datado.
