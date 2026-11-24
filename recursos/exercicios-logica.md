# Exercícios de lógica

> Consolidado a cada módulo. Soluções comentadas ficam na aula de origem.

## 🟢 Júnior
| Aula | Problema | Conceito |
|---|---|---|
| J1.01 | Contador `int` de transações: quando estoura? | overflow, `long` |
| J1.02 | Paginação de 23 itens, 10 por página (off-by-one) | laços, divisão inteira |
| J1.03 | Palíndromo recursivo | recursão, caso-base |
| J1.04 | Detectar duplicidade com dois laços | O(n²) |
| J1.05 | Saldo como soma de lançamentos | acumulador, regra 1 |
| J1.06 | Escolher a tarifa por valor | polimorfismo |
| J1.07 | Testar relatório com hora injetada | `Clock`, composição |
| J1.08 | `HashMap` por dentro: gavetas e colisões | hash, `equals` |
| J1.09 | `parseAmount` com mensagens claras | exceções |
| J1.10 | Detectar duplicidade em O(n) | `Set` |
| J1.11 | Cache LRU com `LinkedHashMap` | `Map` |
| J1.12 | Inverter array genérico in-place | generics |
| J1.13 | Saldo por conta com `toMap` | streams |
| J1.14 | Taxa por tipo de pagamento com `sealed` | pattern matching |
| J1.15 | Dividir R$ 100,00 em 3 sem perder centavo | `BigDecimal`, resto |
| J1.16 | Big-O de três versões de `hasDuplicate` | complexidade |
| J2.01 | `isPrivate(ipv4)`: faixas RFC 1918 | parsing, intervalos |
| J2.02 | Parser de requisição HTTP crua | strings, `Map` |
| J2.03 | Dias até o certificado expirar (armadilha do `DAYS.between`) | `Clock`, truncamento |
| J2.04 | `purgeExpired` de sessões | `Map`, `removeIf` |
| J2.05 | Percentil p50/p95 (nearest-rank) | ordenação, estatística |
| J2.06 | Casar `/accounts/{id}` com o caminho | roteamento, `split` |
| J3.01 | Ordem de criação (e de remoção) de tabelas respeitando FKs | ordenação topológica, grafo |
| J3.02 | `determines(A, B)`: uma coluna determina a outra? | dependência funcional, `Map` |
| J3.03 | Espera com tentativas e backoff exponencial | laço, `sleep`, tetos |
| J3.04 | Simular `ON CONFLICT DO NOTHING` (seed idempotente) | `putIfAbsent` |
| J3.05 | Paginação: total de páginas sem `double`, offset e keyset com desempate | divisão inteira, cursor |
| J3.06 | Anti-join por *nested loop* x *hash*, contando comparações | O(n·m) × O(n+m), `Set` |
| J3.07 | Conferência saldo × lançamentos com Streams e `BigDecimal` | `groupingBy`, `compareTo` |
| J3.08 | Busca em largura no grafo de transferências (ciclos) | BFS, fila, visitados |
| J3.09 | Busca linear x binária contando comparações (1 mil a 100 milhões) | O(n) × O(log n) |
| J3.10 | Consulta de verificação: toda transferência `COMPLETED` tem débito = crédito = valor | SQL, `HAVING`, `LEFT JOIN` |
| J3.11 | `orderByClause` com lista permitida | validação, `Map` |
| J3.12 | Comparar versões de migração numericamente (`1.2` < `1.10`) | parsing, comparador |
| J4.01 | Dinheiro em `double` no formulário: por que `0.1 + 0.2` quebra e como tratar (HTML e servidor) | ponto flutuante, `BigDecimal`, `inputmode` |
| J4.02 | Alinhar a marca à esquerda e o "Sair" à direita só com CSS | flexbox, `margin-left: auto` |
| J4.03 | Quantas colunas o `auto-fill/minmax` cria em 1000 px? E `auto-fit` com 2 cartões? | aritmética de layout (medido: 3 colunas; 309 × 472 px) |
| J4.04 | Por que `'' == 0` é verdadeiro e `'abc' == 0` não: reescrever a validação sem coerção | coerção, `===`, regex |
| J4.05 | Prever a ordem de saída de `console.log`, `setTimeout`, `fetch` e `Promise.resolve().then` | laço de eventos, microtarefas |
| J4.06 | F5 depois de um `POST` que devolveu 200: o que acontece? PRG e por que 400 e não 200 | idempotência, 303, status |
| J4.07 | Arquitetura de front-end para "app + painel rico + tempo real" com 4 devs de Java | SSR × SPA, contrato de API, ADR |
| J4.08 | Contraste de `#7aa7ff` sobre branco (2,39:1) e o limite para texto grande | luminância relativa, WCAG |
| J5.01 | Ordenação topológica das dependências dos beans, com detecção de ciclo (`A -> B -> C -> A`) | grafos, DFS, O(V+E) |
| J5.02 | `resolvePort`: argumento > variável > arquivo > 8080, com erro que cita a fonte | precedência, `Optional`, validação |
| J5.03 | Registro que fecha os beans na ordem inversa e relata todos os erros (*suppressed*) | pilha, `try/catch`, `addSuppressed` |
| J5.04 | Avaliador de condições (`onClass`, `onMissingBean`...) com relatório Positive/Negative e efeito da ordem | predicados, ordem de avaliação |
| J5.05 | Resolver uma propriedade em várias fontes com grafias diferentes (relaxed binding) e a armadilha da normalização | mapas, normalização, colisões |
| J5.06 | Roteador com variáveis de caminho, rota mais específica e 404 × 405 com `Allow` | *pattern matching*, `sealed`, especificidade |
| J5.07 | UUID versão 7: extrair o instante, ordem textual × criação e o efeito no índice | bits, `UUID`, árvore B |
| J5.08 | Mini contexto de persistência: mapa de identidade, retrato e `flush` com só os campos alterados | identidade, dirty checking |
| J5.09 | Transformar `findByStatusAndOwnerId...` em JPQL (parser de nomes de método) | regex, *lookahead*, gramática |
| J5.10 | Aritmética de paginação (total múltiplo do tamanho, total 0, `int` que estoura) e `Slice` por `size + 1` | off-by-one, `long`, borda |
| J5.11 | Gerenciador de transações de brinquedo: *undo log*, commit e as regras de rollback do Spring | log de desfazer, ordem inversa |
| J5.12 | Contar consultas de cada estratégia (ingênuo `1 + A + U`, `JOIN FETCH`, batch `⌈A/B⌉`) e como crescem | Big-O, conjuntos, divisão com teto |
| J5.13 | Validação de CPF (módulo 11, dígitos repetidos) | aritmética modular, regex |
| J5.14 | Logger de brinquedo: níveis, `{}`, exceção como último argumento e MDC por thread | `ThreadLocal`, formatação, descarte antes de formatar |
| J5.15 | Agregador de saúde (o pior status vence) e grupos liveness × readiness | `enum` ordenado, agregação |
| J6.01 | Operações sobre saldo: quais são idempotentes (1, 2 e 3 vezes) | idempotência, funções |
| J6.02 | Teste por reflexão: nenhum componente do `record` nulo | reflexão, `record` |
| J6.03 | Invariantes após 300 transferências aleatórias (conservação e livro-razão) | propriedades, semente fixa |
| J6.05 | Deriva do offset × cursor com inserção entre páginas | paginação, keyset |
| J6.06 | Efeito exatamente uma vez com 8 threads (`computeIfAbsent`) | concorrência, atomicidade |
| J6.08 | Vazão com e sem timeout (Lei de Little) | filas, capacidade |
| J7.01 | Classes de equivalência e valores-limite de `Amounts.requireValid` (inclui `1E+2`, escala negativa) | partição, fronteiras, `BigDecimal` |
| J7.02 | Relógio que avança a cada chamada para provar que `instant()` é consultado uma só vez | fake de `Clock`, ordem de chamadas |
| J7.03 | `401` ou `400`? A ordem interceptor, resolução de argumentos e validação | cadeia de execução |
| J7.04 | Quantos contextos o Spring cria para 5 classes de teste (chave do cache)? | chave composta, cache |
| J7.05 | Quantos containers o `mvn test` cria com `@DirtiesContext` e quando compartilhar um | custo × contextos |
| J7.06 | Linhas × *branches* de `? :` e `&&` (4 branches, 3 testes); `Masking` por TDD | cobertura, complexidade ciclomática |
| J7.07 | Ordem de avaliação de argumentos e *golden master* (legado × refatorado × `BigDecimal`) | ordem de avaliação, caracterização |
| J7.08 | Por que o Checkstyle não vê `a == b` e o SpotBugs vê (árvore × *bytecode*) | análise estática |
| J7.09 | `ReleaseBump`: o próximo SemVer a partir do histórico de commits | regex, enum ordenado, máximo |
| J8.01 | A ordem dos filtros: o `AuthorizationFilter` antes do filtro de login | ordem de execução, `SecurityContext` |
| J8.02 | Dois usuários com a mesma senha têm o mesmo hash? | sal, hash lento |
| J8.03 | Um JWT de 15 minutos foi roubado: que medidas limitam o dano? | TTL, escopo, revogação |
| J8.04 | Regra geral antes da regra de admin: quem acessa a rota de admin? | ordem das regras, sombreamento |
| J8.05 | Um `<img src=".../deposit?amount=100">` altera o saldo? E se houvesse um `@GetMapping`? | métodos seguros, CSRF |
| J8.06 | `403` ou `404` para a transferência de outra pessoa: o que o atacante aprende? | enumeração, IDOR |
| J8.07 | Segredo comitado e removido 10 minutos depois: o que falta fazer, em ordem? | segredo comprometido, rotação |
| J9.01 | `docker rm -f db` e o banco voltou vazio: por quê e como corrigir? | camada de escrita, volume |
| J9.02 | Dockerfile com `COPY . .` primeiro e `ENTRYPOINT` shell: cinco problemas e a ordem de correção | cache de camadas, sinais, segurança |
| J9.03 | Depois de `down -v`, o site falha nos primeiros segundos: três causas, uma por camada | `--wait`, `healthcheck`, Flyway |
| J9.04 | O disco do servidor enche uma vez por dia e `docker system df` não mostra nada: onde está? | driver `json-file`, rotação |
| J10.01 | `concurrency`: duas PRs diferentes se cancelam? E dois `push` na `main` sem cancelamento? | filas, `group` |
| J10.02 | Minutos/mês com e sem `concurrency` (6 `push` por PR, 12 PRs) | estimativa, custo |
| J10.03 | VM usada 8 h por dia útil: fração usada e economia ao desligar | `730 h`, custo por tempo |
| J10.04 | Orçamento de US$ 10 com gasto de US$ 1,50/dia: quando chega cada alerta e a previsão? | limiares, previsão linear |
| J10.05 | Banco com `0.0.0.0/0` na 5432 "por 5 minutos": o que muda e o que ainda protege? | defesa em profundidade |
| J10.06 | `needs` em cadeia: o que roda se o `test` falha? E se o `deploy` falha depois do push da imagem? | dependências, imutabilidade |
| J10.07 | Por que o `try` do teardown não envolve o `delete-db-instance`? | erro silenciado, `set -e` |

## 🔵 Pleno
| Aula | Problema | Conceito |
|---|---|---|
| P1.01 | 1 milhão de `LedgerEntry` numa `List` cabe em `-Xmx256m`? E como exportar em heap constante? | custo de objeto, *streaming* |
| P1.02 | Em quanto tempo o `fee()` "amadurece" a 2 000 chamadas/s? | contadores do JIT, ordem de grandeza |
| P1.03 | Quando o cache LRU de 60 000 entradas para de crescer no log de GC? | conjunto vivo, `removeEldestEntry` |
| P1.04 | `Allocation Stall` no ZGC com 1 GiB: por quê e duas correções | taxa de alocação × folga de heap |
| P1.05 | Dimensionar o pool de exportação (80% de espera, 4 núcleos, 10 conexões) | CPU × I/O-bound, recurso escasso |
| P1.06 | `convertedBalance` com 3 provedores, prazo de 400 ms e mediana | `CompletableFuture`, `handle`, `allOf` |
| P1.07 | `LoginThrottle` com `ConcurrentHashMap`: o que `compute` resolve e o que não resolve | atomicidade por chave |
| P1.08 | Flag de desligamento sem `volatile` e contador lido por outra thread | *happens-before* |
| P1.09 | `idempotency_key` com `find` + `save`: o que dá errado e por que o `UNIQUE` é a proteção real | *check-then-act* |
| P1.10 | 1 000 requisições, Hikari de 10 conexões, 3 consultas de 20 ms: tempo mínimo | recurso escasso com threads virtuais |
| P1.11 | `sealed TransferResult` + `switch` exaustivo; o que o compilador faz ao acrescentar um caso | modelagem com tipos |
| P1.12 | `Rounding` interno do módulo: duas formas de usar de fora e qual é melhor | encapsulamento, API pública |
| P1.13 | "Tire o `@Version` e os 409 somem": por que é desastroso | corrupção silenciosa × erro honesto |
| P2.01 | Limite de 3 contas por usuário: *write skew* e duas formas de impor sem `SERIALIZABLE` (índice único parcial; trava do usuário) | anomalias, restrição declarativa |
| P2.02 | Fila de transferências `PENDING` com vários *workers*: como cada um pega o próximo sem repetir nem esperar (`FOR UPDATE SKIP LOCKED`) | trava, fila em tabela |
| P2.03 | Lote com 3 transferências cruzadas: ordem por par basta? (coletar, ordenar e travar o conjunto) | ordem global de travas |
| P2.04 | Plano com `rows=10` estimado e `rows=250000` real (`Nested Loop`, `loops=250000`): causa e primeiro passo | estatística, leitura de plano |
| P2.05 | Índice para as `FAILED` de uma conta nos últimos 7 dias: parcial × composto e a ordem das colunas | índices, seletividade |
| P2.06 | 3 réplicas × pool 30 com `max_connections = 100`: quantas conexões, o que acontece no *rolling deploy* e um pool defensável | dimensionamento |
| P2.07 | Tela "Minhas contas" com os 3 últimos lançamentos de cada uma: por que `join fetch` + `subList` é errado e como fazer em 2 consultas | coleções, projeção |
| P2.08 | Importar 200 000 transações por noite respeitando regras 3 e 5 e idempotência: `saveAll`, *batch*, `batchUpdate` ou `COPY`? | carga em volume |
| P2.09 | Depósito de R$ 100 com cache de 30 s: quando o cliente ainda vê o saldo antigo, o que não pode usar o cache e o que acontece com o Redis fora | consistência de cache |
| P2.10 | Mover `accounts` e `ledger_entries` para o DynamoDB: que garantias perde, que acessos pioram e o que propor | escolha de armazenamento |
| P2.11 | 10× mais lançamentos e o resumo de 2 anos em 3 s: o que tentar, em ordem de custo, e o que medir entre cada passo | investigação de desempenho |
| P3.01 | 150 req/s, 40 ms → +800 ms de antifraude: simultâneas, saturação e teto do *pool* | Lei de Little |
| P3.02 | `fixedRate` de 60 s com *job* de 90 s; `lockAtMostFor` de 1 min: o que acontece | *scheduling*, exclusão mútua |
| P3.03 | 3 consumidores, *prefetch* 250 × 1, 1 000 tarefas de 2 s: tempo total | balanceamento |
| P3.04 | 6 partições e 4 consumidores; chaves `A` e `B` na mesma partição: ordem? | partições e ordem |
| P3.05 | 2 000 eventos/s × 500 B: GiB/dia, disco com replicação 3 e retenção 14 dias; *lag* × retenção | dimensionamento |
| P3.06 | Ordem 1-2-3 (ler, debitar, confirmar): onde há perda e onde há duplicata? | semânticas de entrega |
| P3.07 | `if (!seen(id)) { debit(); markSeen(id); }`: dois cenários de falha e uma correção | *check-then-act*, atomicidade |
| P3.08 | *Backoff* 1 s ×2 por 6 tentativas: tempo total, partição bloqueada, *jitter* e retries em cascata (3×3×3) | progressão geométrica |
| P3.09 | Relé publica 100, cai após 60 antes do *commit*: quantas mensagens e quantas processadas | at-least-once |
| P3.10 | Reenvio a cada 5 min por 10 min de falha; evento fora de ordem; dinheiro como `number` | idempotência e estados |
| P3.11 | 20 000 conexões SSE: conexões abertas, o que limita e reconexões por segundo após uma queda | Little, escala |
| P3.12 | 3 chamadas de 120/80/200 ms: sequencial × paralelo × *timeout* de 150 ms | composição |
| P3.13 | 10 000 pagamentos e 12 duplicados: taxa, saldo bate?, valor a mais, por que o teste de carga não acha | conciliação |
| P5.01 | SLO de 99,95% com duas chamadas síncronas de 99,9% a mais: o SLO se mantém? | produto de probabilidades |
| P5.02 | Pontuar 0–2 os 6 critérios de fronteira para `notification`, `antifraud` e `reporting` | decisão com critérios |
| P5.03 | 2.000 consultas/s de 133 B (protobuf) × 239 B (JSON): MB/s e por que importa pouco | unidades, ordem de grandeza |
| P5.04 | Classificar 4 mudanças de evento como compatíveis para trás/frente | compatibilidade |
| P5.05 | Prazo do cliente 10 s, *read timeout* 8 s, 3 retentativas: o que o cliente vê? | orçamento de tempo |
| P5.06 | Config Server com 99,9% e 6 serviços na partida: com e sem cache local | dependência na partida |
| P5.07 | Prazo 3 s, *timeout* 800 ms, *backoff* 100/200 ms: quantas tentativas cabem? | soma de tempos sequenciais |
| P5.08 | Janela 10, mínimo 5, 50%: em qual chamada o *breaker* abre? | janela deslizante |
| P5.09 | 5 versões de provedor contra um pacto: quais passam? | contratos e matchers |
| P5.10 | 200 *threads*, 50 req/s, serviço de 10 s: tempo até esgotar; com *timeout* de 300 ms | Lei de Little |
| P6.01 | 50 req/s × 3 linhas de 450 B: GB/dia, 14 dias, sem o `INFO` do relé, amostrando `INFO` a 10% | estimativa de capacidade |
| P6.02 | 1.000 contas × 12 rotas × 3 status × 77 séries por histograma | produto de cardinalidades |
| P6.03 | p95 de duas instâncias (100 ms e 900 ms): média dos p95 × p95 do serviço | percentil não é aditivo |
| P6.04 | `for: 5m`, condição de 3 a 7 min e depois de 8 min: quando dispara? | estados do alerta |
| P6.05 | 50 req/s, 1 falha em 1.000, amostra de 10%: quantas falhas para 95% de chance? | probabilidade complementar |
| P6.06 | Banco, Kafka, *deadlock*, aquecimento, risco lento, disco: qual sonda reage? | decisão com critérios |
| P6.07 | Que fração de requisições lentas faz o p95 e o p99 passarem de 1 s? | percentis e segmentos |
| P7.02 | Limite de 512 Mi, `MaxRAMPercentage=75`, 250 threads de 1 MiB: o que sobra e há risco? | orçamento de memória |
| P7.03 | `maxSurge 1`, `maxUnavailable 0` e o pod novo nunca fica pronto: o que acontece? | estratégia de rollout |
| P7.05 | `startupProbe` de 30 s e partida de 50 s: o que acontece? | laços de reinício |
| P7.06 | `requests 500m`, meta 50%, 3 pods a 400m: quantas réplicas? | fórmula do HPA |
| P7.09 | Log com `OutOfMemoryError` e exit 3: aumentar o limite resolve? | diagnóstico por evidência |

| P8.01 | Quatro *jobs* de 1, 2, 2 e 1 min em paralelo e um de 9 min que precede um de 3: qual o caminho crítico? | caminho crítico |
| P8.02 | `--ignore-unfixed`, `fixed` e `EOSL`: o que reprova e o que passa? | triagem de achados |
| P8.03 | 5% de canário e 20 req/min: quantas requisições por minuto o canário vê? A decisão de 2% de erro tem significado? | tamanho de amostra |
| P8.04 | Dois *flags* de 50% com o mesmo *hash*: que fração recebe ambos? E com sal? | independência |
| P8.05 | `count` com `["a","b","c"]`, remove `"a"`: que recursos mudam? E com `for_each`? | identidade por posição × chave |
| P8.06 | Dois `apply` simultâneos com `use_lockfile`: quem recebe o `412`? | escrita condicional |
| P8.07 | `plan` com `Plan: 2 to add, 3 to change, 2 to destroy` e nenhum `destroy` esperado: o que investigar? | leitura de plano |
| P9.01 | Aritmética de CIDR: `/24` tem quantos IPs úteis na AWS? Quantos o *deploy* consome? Um `/28` serve? | bits e máscara; `CidrLab.java` |
| P9.04 | `visibility_timeout = 60 s` e `maxReceiveCount = 5`: em quanto tempo uma mensagem veneno chega à DLQ? E a retenção dela lá? | tempo de retentativa; retenção desde o envio |
| P9.05 | 100 execuções/s por 5 s, cada uma de 2 s: quantos *cold starts*? Qual a concorrência em regime? | lei de Little `L = λ × W` |
| P9.06 | Rotação da chave de JWT com *deploy* de 6 min e token de 15 min: quando revogar a velha? | janela de coexistência |
| P9.07 | Alarme `period 300 s × 2` com tráfego mínimo: em quanto tempo avisa? E com 20 req no período? | tempo de detecção; mínimo de tráfego |
| P9.08 | 6 tarefas × `maximum_percent 200` × pool 10 contra `max_connections 100`: cabe no *deploy*? | pico de conexões |

## 🟣 Sênior (módulo S1)
| Aula | Problema | Conceito |
|---|---|---|
| S1.01 | Com p = 0,95 paralelizável, quantas máquinas dão 10×? E 15×? | Amdahl, retornos decrescentes |
| S1.02 | N = 5: quais pares (W, R) dão leitura sem defasagem e qual minimiza a latência de escrita? E se uma réplica cair? | Quóruns |
| S1.03 | Anel com K nós e v nós virtuais: quanto muda ao remover um nó e para onde vai? | Hash consistente |
| S1.04 | LRU de 1.000 entradas e 10.000 chaves uniformes: taxa de acerto? TTL igual aos 60 s em todas as chaves: o que acontece? | Localidade, expiração em massa |
| S1.05 | Eventos seq 1, 2, 4, 3, 3, 5 para a mesma conta: ordem aplicada e reentregas? | Projetor idempotente, lacuna |
| S1.06 | Fluxo Deposited(100), Withdrawn(30), Withdrawn(50), Deposited(10): saldo, dois saques concorrentes, evento de correção | *Fold*, `expectedVersion` |
| S1.07 | Saga A→B→C(pivô)→D→E: compensações se B falha; o que fazer se D falha; queda entre C e o registro | Saga, pivô, idempotência |
| S1.08 | Mesma chave, corpo `{valor: 100}` × `{valor: 100.00}`: o que o servidor faz e como evitar o falso conflito? | *Hash* do corpo canônico |
| S1.09 | Balde de capacidade 20, 10 fichas/s, 30 pedidos em t = 0 e 1 a cada 0,05 s por 2 s: quantos aceitos? | Balde de fichas |
| S1.10 | Três réplicas A, B, C: vetores de x=1 (A), x=2 (B após receber), x=3 (C isolada); quais pares são concorrentes? | Relógio vetorial |
| S1.11 | Com 7 caracteres base62 e aleatório puro, quantas URLs até 50% de chance de colisão? | Paradoxo do aniversário |
| S1.12 | Tarifa de 1,5% sobre R$ 100,00 dividida entre pagador e lojista em centavos inteiros; e se a tarifa fosse 1,01? | Alocação do centavo, soma zero |
| S1.13 | Autor com 2 milhões de seguidores posta 3×/dia: escritas/dia e tempo de entrega a 50 mil escritas/s | *Fan-out* na escrita |

## 🟣 Sênior (módulo S2)
| Aula | Problema | Conceito |
|---|---|---|
| S2.01 | Requisição de 100 ms: 80 ms de SQL, 15 ms de JSON, 5 ms de lógica. Serialização 5× mais rápida × índice que leva o SQL a 20 ms: qual o ganho de cada? | Amdahl na requisição |
| S2.02 | p99 de 900 ms, CPU a 15%, perfil de CPU sem nada gordo: que evento abrir? | CPU × espera |
| S2.03 | `tryAcquire` com 40% das amostras de CPU, mas o serviço está a 15% de CPU: duas explicações | Atribuição × perfil errado |
| S2.04 | Serviço reiniciado toda noite; o histograma mostra `ConcurrentHashMap$Node` e `LinkedList` crescendo juntos: hipótese e evidência | Piso do heap, dono do vazamento |
| S2.05 | G1 com p99 de 120 ms, pausas de ~100 ms a cada 8 s, vivo 900 MB em heap de 1 GB: primeira ação | Folga do heap |
| S2.06 | Teste fechado com 50 usuários reporta 500 req/s e p99 de 180 ms; a produção recebe 700 req/s: o que falta? | Modelo aberto, capacidade |
| S2.07 | HPA sobe de 3 para 12 pods; partida de 20 s; "vamos para nativo": primeira medição e alternativa mais barata | Escada de partida |
| S2.08 | JMH mede 0,3 ns/op para *parse* de JSON de 2 KB: o que há de errado e quais as duas correções? | Código morto, `baseline` |

## 🟣 Sênior (módulo S3)
| Aula | Problema | Conceito |
|---|---|---|
| S3.01 | `ADD COLUMN daily_limit NOT NULL` com valor inicial = 10% do saldo, sem janela: a sequência de migrações e o *lock* de cada passo | `NOT VALID`, *backfill*, versão N inserindo |
| S3.02 | A consulta do extrato sem data leu 26 partições e ficou mais lenta que a tabela única: por quê e qual é a regra do repositório? | Poda, filtro fechado |
| S3.03 | `readOnly` roteado à réplica: 0 de 10 transações lá. Duas causas possíveis e o teste que prova | Conexão antes da flag |
| S3.04 | O ledger perdeu um mês e `Σ lançamentos ≠ saldo`: o que faltou fazer antes do `DROP`? | Saldo de fechamento |
| S3.05 | `ILIKE '%mercado%'` custa ~220 ms em 1 milhão e **~5 ms** dentro de uma conta: o que decide a arquitetura da busca? | Escopo |
| S3.06 | `UPDATE` de 1 milhão de linhas com *slot* parado reteve ~375 MB: estime o disco em 3 dias de escrita X e o limite seguro | Retenção do *slot* |
| S3.07 | BRIN de 48 kB × B-tree de 64 MB: em que coluna o BRIN **não** serve e por quê? | Correlação física |
| S3.08 | Resumo diário perdeu 501 lançamentos: por que `+=` e marca-d'água falham e o que a janela recalculada garante? | Idempotência, atraso |
| S3.09 | Mutante sem *trigger*: ~100 mil divergências. Quantas esperaria com 50% menos escritas do código antigo? | Deriva proporcional à escrita |
