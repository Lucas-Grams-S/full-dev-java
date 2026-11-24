# Cheatsheet — Sistemas distribuídos (S1)

| Tema | Regra / fórmula | Onde |
|---|---|---|
| Amdahl | `S(n) = 1/((1−p) + p/n)`; limite `1/(1−p)` | S1.01 |
| Balanceamento | heterogêneo → menos requisições em andamento ou P2C; *round-robin* só com iguais | S1.01 |
| Conexões | `N × (maximum_percent/100) × pool + admin ≤ max_connections` (por banco) | S1.01, decisão 181 |
| CAP | **durante a partição**: C (linearizável) × A; P não é opção | S1.02 |
| PACELC | senão: latência × consistência | S1.02 |
| Quórum | `R + W > N` ⇒ leitura cruza a escrita | S1.02 |
| Escada | linearizável > sequencial > causal > sessão > eventual | S1.02 |
| *Sharding* | `hash mod N` move ~80%; anel move ~1/N; ≥ 128 nós virtuais; chave quente não some | S1.03 |
| Chave de partição | não monotônica, distribui, mantém a operação num *shard* | S1.03 |
| Cache | *cache-aside*; TTL com *jitter*; *single-flight*; chave com `sub`; **dado privado → `no-store`** | S1.04 |
| Taxa de acerto | depende da **distribuição** (Zipf × uniforme), não do tamanho | S1.04 |
| CQRS | projetor **idempotente** (PK + `ON CONFLICT`), **lacuna = erro**, token de versão + *fallback*, invariante, *rebuild* | S1.05 |
| *Event sourcing* | só se o histórico é o negócio; `expectedVersion`; *upcasting*; correção por evento | S1.06 |
| 2PC | bloqueia se o coordenador cai; `max_prepared_transactions = 0` por padrão | S1.07 |
| Saga | compensável → **pivô** → repetível; **timeout = desconhecido**; estado + comando na mesma transação | S1.07 |
| Idempotência | chave do cliente; `(owner, key)`; *hash* canônico; 422/409/400; efeito + chave na mesma transação; `sagaId:passo` até o PSP | S1.08 |
| *Rate limit* | balde de fichas; janela fixa deixa 2×; limite por instância = N×; **modo degradado**; 429 + `Retry-After` | S1.09 |
| Relógios | parede mente; `nanoTime` para duração; ordem por sequência do dono; Lamport (causal parcial), vetorial (concorrência), HLC | S1.10 |
| *System design* | 7 passos: requisitos 5 · estimativas 5 · API 5 · desenho 10 · aprofundamento 15 · gargalos 5 · evolução | S1.11 |
| Código curto | 62^7 = 3,5 tri; aleatório precisa de `UNIQUE` + *retry* (colisão a 50% com ~2,2 M); contador + Feistel; **≠ segredo** | S1.11 |
| Pagamentos | corretude > disponibilidade; `Σ débitos = Σ créditos`; `CAPTURED ≠ SETTLED`; conciliação diária | S1.12 |
| *Fan-out* | push (comuns) + pull (celebridades) por limiar; dedupe **antes** do envio; prioridade em filas separadas | S1.13 |
