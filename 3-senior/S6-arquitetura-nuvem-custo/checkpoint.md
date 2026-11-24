# S6.CP — Checkpoint do módulo S6

> ⏱️ ~1h30 · 🟣 Sênior · Pré-requisitos: S6.01 a S6.06

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** (Linux, JDK 25.0.5.1): `lab/WellArchitectedLab.java` (opções de folga e Multi-AZ com preço; *linter* de revisão), `lab/ComputeLab.java` (Lambda × Fargate × EC2; memória do Lambda), `lab/LockInLab.java` (varredura de SDK; gravidade dos dados; sair × ativo/ativo), `lab/FinOpsLab.java` (estimativa, NAT × *endpoints*, tags, compromissos, Spot), `lab/LandingZoneLab.java` (17 casos de SCP e 5 mutantes/variantes) e `lab/BillLab.java` (fatura sintética e detectores), todos com **preços reais da AWS Price List** (`lab/prices.csv`, ofertas publicadas de 08 a 23/11/2026, gerado por `lab/fetch-prices.py`); `terraform validate` nos módulos `finops` e `organization` (Terraform 1.17.1, provedor AWS 6.73.0) com **mutante**; as classes de exemplo do *lock-in* **compiladas** contra o AWS SDK v2 2.59.1. **São modelos com premissas declaradas**, não o PayFlow nem a AWS. **NÃO executados:** qualquer recurso na AWS (Organizations, Control Tower, Identity Center, Budgets, Cost Anomaly Detection, Well-Architected Tool, *Savings Plans*), fatura real, Azure e Google Cloud, o PayFlow do aluno.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Quais são os **seis pilares** do AWS Well-Architected e o que é a camada **Cloud Foundations** (nova em 16/11/2026)? Qual a diferença entre **HRI** e **MRI**, e por que **não** existe "LRI"?
2. Na precificação das opções de folga da S4.06, por que a opção **B** deu **110%** de CPU no pior caso, e não os 90% previstos? O que a **B2** muda, quanto custa e por que ela é **hipótese**? Por que a opção **C** custou o dobro?
3. Quanto o **Multi-AZ** do RDS (`db.m7g.large` + 100 GB gp3) acrescenta por mês em `sa-east-1`, e a partir de que **custo por hora parada** ele se paga, com quais premissas? **Quem** assina esse número?
4. Onde fica o **cruzamento** Lambda × Fargate para a API do modelo em `us-east-1` e em `sa-east-1`, e **por quê** ele muda? O que significa o Lambda usar **26%** da CPU paga? Para código **preso à CPU**, o que acontece com o custo de 128 a 1.769 MB?
5. Cite os **seis tipos de *lock-in*** e diga qual a **varredura** pegou nos dois vazamentos do exemplo. Quanto custa mover 1 TB de `sa-east-1` para `us-east-1` e no **sentido contrário**? Por que **ativo/ativo** em duas nuvens **não** se paga como seguro contra *lock-in* no modelo?
6. Na estimativa do PayFlow: total por região, **% fixo**, o que acontece com a conta se o **tráfego dobrar**, e o **custo por 1.000 transferências**. Quais são as **três maiores linhas**?
7. A partir de quantos **GB/mês** os *endpoints* de interface ficam **mais baratos** que 2 NAT em `sa-east-1`? Com 300 GB/mês, quem é mais barato e **por que a decisão 164 se mantém**?
8. Por que `Env=staging` **não** conta como `env`? O que uma ***tag policy*** **não** faz e qual *guardrail* cobre o buraco? Quanto tempo leva para uma tag de custo **aparecer e ativar**? Que parte do custo **nenhuma tag** carrega?
9. Quanto comprometer num ***Compute Savings Plan*** no exemplo (e o que acontece acima disso)? Depois de **quantos meses** a reserva de 1 ano do RDS se paga? No Spot, o que o ***checkpoint*** muda quando a interrupção é frequente?
10. Diga **três coisas que uma SCP não faz** (ou onde não vale). Qual **mutante** quebra o IAM em todas as contas e por quê? No estudo de caso, por que o detector no **total da conta** **não** viu o salto de logs de 10/10, e qual causa **nenhum** detector diário viu?

<details><summary>Gabarito</summary>

1. **Excelência operacional, segurança, confiabilidade, eficiência de desempenho, otimização de custos e sustentabilidade.** As **Cloud Foundations** (FND01–FND07) reúnem as capacidades **da organização** (contas e OUs, identidade e *guardrails*, observabilidade central, tags e inventário, segurança e conformidade, **gestão financeira**, rede): "o *blueprint* de uma *landing zone*". **HRI**: prática fundamental não seguida (ex.: não proteger a conta); **MRI**: prática que habilita e melhora muito (ex.: não auditar/girar credenciais); o nível baixo conta como **progresso**, sem item de risco.
2. O *autoscaling* gera **contagens ímpares** (5, 7 tarefas); a AZ que cai leva `⌈n/2⌉` e as restantes passam de 100% (a S4.06 supôs divisão igual). **B2** (só números pares) volta a **90%** por **+61%** (B: +48%); é hipótese porque o *target tracking* não mantém paridade (precisa de *step scaling* de 2 em 2, **a validar**). **C** dobrou por causa dos **6 *endpoints* de interface na 3ª AZ** (custo fixo por AZ), não da computação.
3. **+US$ 183,23/mês** (US$ 2.198,76/ano): Multi-AZ **dobra** instância e armazenamento. Paga-se se **1 hora sem pagamentos** custar mais que **~US$ 1.118**, com **1 evento de AZ por ano** e RTO de **2 h → 2 min** (premissas; o RTO da restauração é **a medir**, S4.05). Quem assina o custo da hora parada é o **negócio** (decisão 225).
4. **~10 req/s** em `us-east-1` e **~17,2 req/s** em `sa-east-1`: o Fargate é **mais caro** em São Paulo e o Lambda tem o **mesmo preço** nas duas regiões, então o Lambda vence até um tráfego maior. **26%**: de cada vCPU-segundo pago, só 26% é CPU; o resto é **espera de I/O cobrada** (10 ms de CPU + 50 ms de espera). Preso à CPU: o custo por milhão fica **igual (US$ 4,81)** e a latência cai de 2,8 s para 0,2 s; acima de 1 vCPU, um *handler* de uma *thread* só fica mais caro.
5. **Código (SDK), dados (gravidade), semântica do serviço, operação/identidade, contrato/compromissos, pessoas.** Vazamentos: `MonthlyExporter` usando `S3Client` **na aplicação** e `Transfer` com anotações do **DynamoDB no domínio**. 1 TB: `sa-east-1 → us-east-1` **US$ 141,31**; `us-east-1 → sa-east-1` **US$ 20,48** (~7×). Mesmo com **P = 100%** de sair em 3 anos, sair **uma vez** (US$ 4,3 mil nas premissas) custa **8,6× menos** que manter a segunda nuvem (US$ 37 mil): ativo/ativo precisa de **outro motivo** (regulação, contrato, disponibilidade exigida).
6. **US$ 887,89** (`us-east-1`) e **US$ 1.424,28** (`sa-east-1`, ×1,60); **71% fixo** em São Paulo; tráfego ×2 → **×1,29** (US$ 1.841,63); **US$ 0,11** por 1.000 transferências só produção e **US$ 0,14** com todos os ambientes. Maiores linhas: **RDS Multi-AZ** (US$ 322,66), **6 *endpoints* × 2 AZs** (US$ 183,96), **réplica do *reporting*** e **banco do *staging*** (US$ 100,01 cada).
7. Acima de **~580 GB/mês** (`us-east-1`: ~626). Com **300 GB**, **2 NAT** custariam ~**US$ 23/mês a menos**. A 164 se mantém por **segurança** (nenhuma rota à internet, saída restrita a 3 destinos): agora o *trade-off* tem preço e **some** com o crescimento.
8. Chaves e valores são **sensíveis a maiúsculas**. A *tag policy* **padroniza** e pode barrar operações de tag fora do padrão, mas **recurso sem tag não é avaliado**: a **SCP `require-cost-tags`** (condição `Null` em `aws:RequestTag/cost-center`) nega a **criação** sem tag. Até **24 h** para a chave aparecer e **mais até 24 h** para ativar. **~11%** no exemplo (transferência, suporte, impostos): rateio por **regra escrita**.
9. O **piso**: **6 tarefas** (−18,9%); 7 já desperdiça, **9 (o pico) perde dinheiro** (−5%). Reserva do RDS (1 ano, sem adiantamento): **9,2 meses** na mesma classe (e não dá para cancelar). Com interrupção de 50%/h, **sem *checkpoint***: p95 de **34 h** e **61%** fora do prazo; **com *checkpoint*** a cada 15 min: p95 de **5,1 h** e **0%** atrasado.
10. **Não concede** permissão; **não vale** na **conta de gestão**; **não vale** para **papéis vinculados a serviço** (e toda entidade precisa de **pelo menos uma** SCP). **M1**: negar tudo fora de `sa-east-1` **sem** o `NotAction` de serviços globais nega o **IAM** (cujo *endpoint* é `us-east-1`) em todas as contas. Depois dos saltos de 1º–3/10, a **média e o desvio** dos 14 dias do total **inflaram** e o salto de 10/10 ficou "normal"; o **S3** (vazamento lento) nunca alertou (e os *snapshots* de 15/10 foram mascarados).
</details>

## Parte 2: Entrega prática (tag `payflow-S6`)
O `payflow` ganha a **visão de custo e de arquitetura em nuvem**: revisão Well-Architected versionada, estimativa reproduzível, política de tags, orçamentos e anomalias, decisão de computação, plano de saída, *landing zone* com SCPs testadas e o *runbook* de custo. **Nada disto exige mudar o código da aplicação** (exceto o teste de arquitetura do SDK); **nenhuma migração** (a **V26** continua livre).

```
payflow/
├── src/test/java/com/payflow/architecture/ProviderSdkRulesTest.java   SDK do provedor só em ..adapter.. (S6.03)
├── docs/cloud/
│   ├── well-architected-review.csv   20+ perguntas, linter no CI, 4 HIGH com dono      (S6.01)
│   ├── computacao.md                 componente → onde roda → custo → gatilho          (S6.02)
│   ├── plano-de-saida.md             ordem, janela de corte, isenção de DTO, exportação restaurada (S6.03)
│   └── landing-zone.md               7 contas, 7 OUs, 8 SCPs, migração da conta única  (S6.05)
├── docs/finops/
│   ├── bom.csv + prices.csv          lista de materiais e preços datados (fetch-prices.py)   (S6.04)
│   ├── estimativa.md                 totais, % fixo, custo por 1.000 transferências, premissas (S6.04)
│   ├── politica-de-tags.md           chaves, valores, rateio do que não leva tag       (S6.04)
│   └── runbook-custo.md              "a conta subiu": do total ao recurso               (S6.06)
├── docs/postmortems/2026-10-custo.md  exemplo sintético da S6.06 (ações com dono e data)
├── infra/terraform/finops/           default_tags, ativação de tags, orçamento, anomalia por serviço (S6.04)
├── infra/terraform/organization/     OUs, SCPs (JSON), anexos — aplicar na conta de gestão     (S6.05)
├── infra/scp/*.json                  as 8 SCPs + LandingZoneLab no CI (17 casos, 5 mutantes)  (S6.05)
└── .github/workflows/finops.yml      linter da revisão, linter de tags no plan, LandingZoneLab, check-network.sh
projeto/adrs/0029-arquitetura-em-nuvem-e-custo-do-payflow.md   (já redigida nesta geração)
```

**Critérios de pronto:**
- [ ] **Revisão Well-Architected:** o *linter* passa; cada HIGH tem dono, prazo ≤ 90 dias e ação; o *trade-off* B × B2 está registrado com custo, métrica e data (S6.01).
- [ ] **Computação:** a tabela "onde roda" cita custo e premissa e tem **gatilhos numéricos**; a imagem da API é `arm64` com `-XX:+UseG1GC` (S6.02).
- [ ] ***Lock-in*:** `ProviderSdkRulesTest` reprova a *fixture* vazada e passa no código real; a exportação mensal em formato aberto foi **restaurada** fora da AWS ao menos uma vez (S6.03).
- [ ] **Estimativa:** BOM + preços **datados** + premissas; refeita a cada mudança de arquitetura e **conciliada** com a fatura real todo mês (S6.04).
- [ ] **Tags:** `default_tags` + SCP na criação + *linter* no CI; tags de custo **ativadas**; regra de rateio escrita (S6.04).
- [ ] **Orçamento e anomalia:** por **conta/ambiente**, previsão 80% e real 100%, monitor **por serviço**, destino **com dono** (S6.04/S6.06).
- [ ] **Compromissos:** decisão de **não** comprar agora registrada, com data de reavaliação (3 meses de uso real) e a regra "piso" (S6.04).
- [ ] ***Staging*:** agenda de desligamento fora do horário e linha nova no BOM (S6.04).
- [ ] ***Landing zone*:** contas e OUs com dono; SCPs aplicadas **primeiro** na OU *Policy Staging*; humanos pelo IAM Identity Center; *break-glass* com MFA e alerta (S6.05).
- [ ] ***Runbook* e postmortem de custo:** do custo por unidade ao recurso; cada causa → mecanismo com dono e data (S6.06).
- [ ] **ADR 0029** revisada; tag `payflow-S6`.

💸 **Tudo o que cria recurso** (módulos `finops` e `organization`, contas novas) **só na conta de estudo** ou numa organização descartável, com `terraform destroy` no fim e a conferência do dia seguinte (P8.07). **Contas criadas não são apagadas pelo `destroy`**: feche-as pelo processo do Organizations.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Sênior: **≥ 2** em todos e **3** nos marcados com ⭐.
| Competência (matriz) | Evidência no módulo | 0–3 |
|---|---|---|
| ⭐ **Nuvem e custo:** aplica Well-Architected, FinOps, multi-AZ e decide *lock-in* | S6.01–S6.06 (revisão, estimativa, tags, compromissos, Spot, *landing zone*, estudo de caso) | |
| ⭐ **Arquitetura:** decide *trade-offs* com número (folga de AZ, Multi-AZ, região, computação, multi-cloud) e registra ADR | S6.01, S6.02, S6.03 | |
| ⭐ **Observabilidade e operação:** detecção de anomalia de custo, *runbook* e postmortem de custo | S6.04, S6.06 | |
| **Segurança:** *guardrails* de organização (SCP), conta de gestão, *break-glass*, conta de *backup* | S6.05 | |
| **CI/CD e IaC:** módulos `finops` e `organization` validados com mutante; *linters* no CI | S6.04, S6.05 | |
| **Testes e qualidade:** casos e mutantes de SCP; *linter* que reprova a revisão ruim; teste de arquitetura do SDK | S6.01, S6.03, S6.05 | |
| **Performance:** utilização × custo (CPU paga × usada), memória do Lambda | S6.02 | |
| **Sistemas distribuídos:** perda de AZ com contagem real de tarefas; ativo/ativo × saída | S6.01, S6.03 | |
| **Liderança e comunicação:** custo por unidade para produto; postmortem sem culpa; "não comprar compromisso agora" | S6.04, S6.06 | |
Se **≥ 2** em tudo e **3** nos ⭐ → siga para o **S7**. Linhas em **0–1**: refaça a aula indicada e o **laboratório** correspondente.

## Parte 4: Simulado: revisão de custo e arquitetura antes de ir para produção (60 min, **com consulta ao cheatsheet**)
> ⚠️ **Cenário SINTÉTICO:** a projeção de tráfego e as perguntas da diretoria são **inventadas** para o exercício; os **números** do gabarito saem das **saídas reais** do laboratório (preços da Price List de novembro de 2026 e premissas declaradas). **Nunca** cite este simulado como medição do PayFlow.

**Cenário.** O PayFlow vai para produção em **fevereiro de 2027** com o desenho da S6.04 (opção B da S6.01, RDS `db.m7g.large` Multi-AZ, réplica do *reporting*, 6 *endpoints* × 2 AZs). O produto projeta **10 milhões de transferências por mês** no lançamento e **o dobro em 12 meses**. A diretoria manda seis perguntas: (a) *"quanto vai custar, agora e com o dobro?"*; (b) *"São Paulo ou Virgínia?"*; (c) *"o financeiro quer comprar um Savings Plan de 3 anos já, pelo desconto"*; (d) *"um dev propôs trocar os endpoints por NAT para economizar"*; (e) *"precisamos ser multi-cloud?"*; (f) *"o staging fica ligado o tempo todo?"*. O time de plataforma pergunta ainda: (g) *"o que falta para a conta de produção?"* e (h) *"como saberemos se a conta disparar?"*.

**Tarefas:**
1. **Estimativa.** Total mensal nas duas regiões **agora** e **com o dobro**; o fator; o **custo por 1.000 transferências** nos dois momentos (todos os ambientes). Explique por que **não** dobra.
2. **Região.** Quanto São Paulo custa **a mais** por mês, agora e com o dobro? Que **outros dois eixos** decidem e qual é o estado de cada um?
3. ***Staging*.** Quanto economiza desligar **tarefas e banco** do *staging* fora de **12 h por dia útil** (~261 h de 730)? O que **continua** custando?
4. **Compromissos.** Responda ao financeiro (o quê, quanto e **quando**); e a reserva do RDS?
5. **NAT.** Responda ao dev com números **hoje** (300 GB/mês) e **com o dobro** (600 GB/mês).
6. **Multi-cloud.** Responda à diretoria separando os medos, com o número do modelo.
7. ***Landing zone*.** Liste as contas, as SCPs e a **ordem** de criação; o que proteger **fora** das SCPs.
8. **Detecção.** Que orçamentos e detectores criar, **por quê** por serviço, e o que **nenhum** detector diário pega?

**Rubrica (100 pontos):** estimativa e custo por unidade (**15**); região com os três eixos (**10**); *staging* (**10**); compromissos (**15**); NAT (**10**); multi-cloud (**10**); *landing zone* (**15**); detecção e revisão (**15**). **≥ 70** para seguir.

<details><summary>Gabarito (resumo)</summary>

**1.** Agora: **US$ 1.424,28** (`sa-east-1`) e **US$ 887,89** (`us-east-1`). Com o dobro: **US$ 1.841,63** e **US$ 1.169,53** (**×1,29** e **×1,32**): **~70% é fixo** (HA, Multi-AZ, *endpoints*). Por 1.000 transferências (todos os ambientes): São Paulo **US$ 0,14 → ~US$ 0,09** (1.841,63 ÷ 20.000); Virgínia **~US$ 0,09 → ~US$ 0,06**. O custo unitário **cai** com escala.
**2.** **+US$ 536/mês** agora (1.424,28 − 887,89) e **+US$ 672/mês** com o dobro (1.841,63 − 1.169,53). Os outros eixos: **latência** ao usuário (**a medir**: nenhum número foi medido no treinamento) e **exigências regulatórias e contratuais** (**a confirmar** com o jurídico). A SCP permite as **duas** regiões até a decisão.
**3.** Tarefas (US$ 40,66 vCPU + US$ 8,94 memória) e banco (US$ 100,01) somam **US$ 149,61**; ligados 261 de 730 h (35,8%): economia de **~US$ 96/mês** (~32% do *staging*). **Continuam:** ALB, IPs públicos, **6 *endpoints* (US$ 91,98)** e o armazenamento do banco. (Confira na documentação do RDS o limite de tempo parado antes de automatizar.)
**4.** **Não agora.** Esperar **3 meses de uso real**; então *Compute Savings Plan* de **1 ano, sem adiantamento**, **no piso medido** (no modelo: 6 tarefas, **−18,9%**; acima do piso desperdiça e no pico **perde** 5%). 3 anos é *lock-in* financeiro num sistema que ainda pode mudar de região e de computação. **RDS:** reservar só com certeza de **> 9,2 meses** na mesma classe (a reserva **não** se cancela).
**5.** Hoje (300 GB): 2 NAT **~US$ 23/mês mais baratos** (US$ 163,68 × US$ 186,96). Com 600 GB: **empate** (US$ 191,58 × US$ 189,96: os *endpoints* já ganham por ~US$ 1,60; equilíbrio em **~580 GB**). **Resposta:** manter os *endpoints* (decisão 164: **segurança**, sem rota à internet); a "economia" é pequena e **desaparece** com o crescimento previsto.
**6.** Separe **saída** (portabilidade preparada: SDK só em adaptadores, exportação aberta **restaurada**, plano de saída: quase de graça) de **disponibilidade** (Multi-AZ já; multirregião e multi-cloud não se pagam com SLO de 99,9%, decisão 225). No modelo, sair **uma vez** custa **8,6× menos** que manter a segunda nuvem por 3 anos. **Ativo/ativo só** com exigência regulatória/contratual, com orçamento próprio.
**7.** Contas: **gestão** (sem carga), `log-archive`, `audit`, `payflow-backup`, `payflow-prod`, `payflow-staging`, `study`. SCPs: `deny-leave-org`, `deny-root-user`, `deny-outside-regions` (com o `NotAction`), `protect-audit` (raiz), `protect-backup` (com exceção *break-glass*), `require-cost-tags` (Workloads), `sandbox-limits`, `deny-all` (Suspended). **Ordem:** gestão → *log*/auditoria e *backup* (protegem as demais) → produção **do zero** pelo Terraform → *staging*; SCPs **antes** numa OU de *Policy Staging*. **Fora das SCPs:** a **conta de gestão** (MFA, poucas pessoas, alerta de uso) e o papel ***break-glass***.
**8.** Orçamento **por conta** (previsão 80%, real 100%) e monitor de anomalia **por serviço** (impacto ≥ US$ 50) com destino **com dono**; **por serviço** porque o total "se acostuma" depois de um salto e esconde o próximo (estudo de caso: o salto de logs de 10/10). **Nenhum detector diário** pega **vazamento lento** (o S3 sem ciclo de vida): revisão **mensal** das maiores variações e do **custo por unidade**. E a revisão Well-Architected marca os **4 HIGH** atuais (FND01, FND02, FND06, OPS02) com dono e prazo.
**Ordem de ações:** (1) *landing zone* mínima (*backup* e *log* fora de produção) e SCPs testadas; (2) orçamento e anomalia por conta; (3) agenda do *staging*; (4) ir para produção **on demand**; (5) em 3 meses, *Savings Plan* no piso medido; (6) revisitar região e computação com **latência e tráfego reais**. **Não fazer:** comprar 3 anos agora, trocar *endpoints* por NAT, montar a segunda nuvem.
</details>

## ➡️ Próximo
[S7 — Liderança técnica](../S7-lideranca-tecnica/README.md)
