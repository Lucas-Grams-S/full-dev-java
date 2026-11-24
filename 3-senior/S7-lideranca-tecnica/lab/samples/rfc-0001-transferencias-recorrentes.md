# RFC-0001 — Transferências recorrentes

- **Status:** em revisão
- **Autor:** tech lead do PayFlow (você)
- **Decisor:** líder de engenharia
- **Contribuem:** produto, segurança, SRE, dados
- **Informados:** suporte, atendimento, financeiro
- **Comentários até:** 2026-12-04
- **ADR resultante:** 0031 (proposta; vira aceita ou rejeitada quando o decisor registrar a decisão abaixo)
- **Design doc:** [design-transferencias-recorrentes.md](design-transferencias-recorrentes.md)

## Resumo
Permitir que o usuário **agende uma transferência mensal** (valor fixo, dia D do mês, da conta dele para uma conta PayFlow de destino). Proposta: um **módulo `recurring` dentro do monólito modular** (P4), que **agenda e dispara**; cada ocorrência vira **uma transferência comum** feita pela API pública do módulo `transfer`, **na mesma transação** que registra a ocorrência, com chave de idempotência **derivada** `recurring:<schedule_id>:<due_date>`. **Não** propomos um serviço separado agora.

## Problema
- **Hipótese de produto (a validar com o produto, não é medição):** usuários repetem a mesma transferência todo mês (aluguel, mesada) e esquecem; o suporte recebe pedidos de "agendar".
- **Por que agora:** o desenho já foi esboçado no simulado do Pleno ("Cobranças recorrentes", `recurring-service`); desde então o PayFlow ganhou o monólito modular (P4), idempotência ponta a ponta (S1.08), SLOs (S4), política de autorização com MFA para mover dinheiro (S5.01) e preço por decisão (S6). A resposta sênior **muda** o desenho do Pleno (ver alternativa B).
- **Quem é afetado:** usuários (nova função), suporte (novos casos), SRE (um *job* no caminho do dinheiro), segurança (dinheiro se move **sem o usuário presente**).

## Proposta
1. **Escopo da v1:** mensal; dia 1 a 31 (mês mais curto → **último dia do mês**); disparo às **06:00 de `America/Sao_Paulo`**; valor fixo; **um** destino; cancelar a qualquer momento **antes** do disparo do dia.
2. **Fronteira:** o módulo `recurring` é dono de `recurring_transfers` e `recurring_runs` (migração **V26**, só tabelas novas). Ele **não toca em saldo nem no ledger**: chama `transfer` pela API pública do módulo (dependências permitidas do Spring Modulith e um dono por tabela, decisões 122 e 124 da P4).
3. **Exatamente uma transferência por ocorrência:** `UNIQUE (schedule_id, due_date)` em `recurring_runs` **e** chave `recurring:<schedule_id>:<due_date>` na idempotência da S1.08. O *spike* (`RecurrenceSpike keys`) mostrou **0 duplicadas** com a chave derivada × **208 em 10 mil** com chave nova por tentativa (modelo com 2% de queda depois do *commit*).
4. **Saldo insuficiente é erro de negócio:** **uma** nova tentativa às 18:00 do mesmo dia; depois `FAILED_FUNDS`, aviso ao usuário e **parar** (nunca laço).
5. **Autorização:** criar o agendamento **é** autorizar movimentos futuros: exige **MFA (*step-up*)** como qualquer movimento de dinheiro (decisão 230) e grava o **consentimento** na trilha `audit_events` (V25). Na execução, a política reconfere **dono, conta ativa e agendamento ativo**.
6. **Entrega:** atrás da *flag* `recurring.enabled` (P8.04), primeiro para contas internas.

## Alternativas

### A. Não fazer nada (lembrete mensal por notificação)
Custo quase zero; o usuário continua no controle. **Descartada porque** não resolve o esquecimento (o pedido do produto) e o lembrete já existe como opção barata se o RFC for rejeitado.

### B. Serviço separado `recurring-service` (o desenho do Pleno)
Banco próprio, Client Credentials para chamar o PayFlow, idempotência pela rede. Pelas **5 perguntas da decisão 126**: não há **dor medida** (nenhum problema de escala ou ritmo), a **queda não é tolerável** (é dinheiro), o **dono e o plantão são o mesmo time** e o **menor passo reversível** é um módulo. **Descartada porque** soma rede, *token* de serviço com escopo de escrita e consistência eventual **sem um ganho que pague isso** hoje.

### C. Módulo `recurring` no monólito (proposta)
Uma transação para "ocorrência + transferência"; reaproveita política, idempotência, Outbox e observabilidade. Custo: o *job* roda **nas mesmas tarefas** da API (disputa de conexões: ver Riscos). **Gatilhos para extrair** (como o `notification` na P5): o disparo passar a competir com a API pelo *pool* **mesmo** com o limite de *workers*; outro time assumir o produto; ritmo de entrega diferente.

### D. Agendador gerenciado da nuvem (um agendamento por usuário no EventBridge Scheduler)
**Descartada porque** divide o estado (regra no banco, gatilho na nuvem), cria milhares de recursos para governar, aumenta o *lock-in* de semântica (S6.03) e o banco já precisa ser a fonte da verdade da regra.

## Riscos
- **Disparo duplicado** (dois nós, *retry*, queda depois do *commit*): `UNIQUE` + chave derivada + teste de concorrência com 2 nós (dono: tech lead).
- **Agendador parado em silêncio** (nenhum erro é emitido): alarme de **atraso** `now − due` e de "nenhuma ocorrência processada na janela" (dono: SRE).
- **Pico das 06:00** nos dias populares disputando o *pool* com a API: no máximo **2 *workers*** (o *spike*: 15 mil ocorrências em 150 s, **premissas**), entra na **conta de conexões** (decisão 181) (dono: SRE).
- **Data errada** (dia 31, fuso, "hoje" do servidor em UTC): regra calculada a partir do `YearMonth` e `Clock` com `America/Sao_Paulo`; o *spike* mostrou o `plusMonths(1)` encadeado **preso no dia 28** e o UTC disparando **na véspera às 22:30** (dono: tech lead).
- **Fraude com agendamento** (conta tomada cria agendamento para terceiro): MFA na criação, aviso por notificação e antifraude em **cada** ocorrência (dono: segurança).
- **Transação marcada como *rollback-only*** quando a exceção de negócio atravessa o `@Transactional` do `transfer`: registrar a falha em transação separada; **confirmar com teste** antes de estimar (dono: tech lead).

## Perguntas em aberto
- Há **limite de valor** por agendamento ou por mês? (dono: produto, até 2026-12-02)
- Avisar o usuário **na véspera**? (dono: produto)
- Por quanto tempo guardar `recurring_runs` e o consentimento? (dono: jurídico, junto com a retenção da S3.04)
- Destino **fora** do PayFlow (Pix) entra numa v2? (dono: produto; fora desta RFC)

## Decisão
Pendente até 2026-12-04. O decisor registra aqui **aceita / rejeitada / retirada**, a data, os comentários que mudaram a proposta e o link da ADR 0031 atualizada.
