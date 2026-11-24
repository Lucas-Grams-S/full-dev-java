# Laboratório do módulo S1

Programas de **Java puro** (um arquivo cada, sem dependências): `java X.java`. Ambiente em que foram executados: Linux, **OpenJDK 25.0.5** (`/usr/lib/jvm/java-25-openjdk-amd64`), 12 núcleos, máquina **compartilhada**. **Sem Maven e sem Docker.**

```bash
cd 3-senior/S1-sistemas-distribuidos/lab
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./run-all.sh      # regrava out-s101.txt … out-s113.txt (~1 min)
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 $JAVA_HOME/bin/java LoadBalancerLab.java   # um programa só
```

| Programa | Aula | O que simula | Saída |
|---|---|---|---|
| `LoadBalancerLab.java` | S1.01 | 4 servidores (um 3× mais lento): aleatório, *round-robin*, menos requisições em andamento, P2C; Amdahl | `out-s101.txt` |
| `ConsistencyLab.java` | S1.02 | quórum `N=3` (R, W), leituras que voltam no tempo, partição CP × AP com LWW | `out-s102.txt` |
| `ShardingLab.java` | S1.03 | `hash mod N` × anel com nós virtuais, chave quente (Zipf), chave monotônica | `out-s103.txt` |
| `CacheLab.java` | S1.04 | LRU × Zipf/uniforme, corrida do *cache-aside*, *stampede* (200 *threads*), *write-behind* | `out-s104.txt` |
| `CqrsLab.java` | S1.05 | projetor com atraso, token de versão, reconstrução de 1 milhão, projeção com bug | `out-s105.txt` |
| `EventSourcingLab.java` | S1.06 | fluxo com `expectedVersion`, *snapshot*, *upcasting*, evento de correção | `out-s106.txt` |
| `SagaLab.java` | S1.07 | saga de 4 passos com falhas, compensação que falha, queda e retomada; 2PC bloqueado | `out-s107.txt` |
| `IdempotencyLab.java` | S1.08 | 10.000 débitos com 30% de respostas perdidas, mesma chave concorrente, queda no meio | `out-s108.txt` |
| `RateLimitLab.java` | S1.09 | janela fixa × deslizante × balde, limite por instância × central × cota, falha do armazenamento | `out-s109.txt` |
| `ClockLab.java` | S1.10 | LWW com relógio atrasado, Lamport, vetorial, HLC | `out-s110.txt` |
| `ShortenerLab.java` | S1.11 | base62, colisões de 7 caracteres (até 50 milhões), Feistel, conta de armazenamento | `out-s111.txt` |
| `ReconciliationLab.java` | S1.12 | conciliação de 100.000 pagamentos com anomalias plantadas; partidas dobradas | `out-s112.txt` |
| `FeedLab.java` | S1.13 | *fan-out* push × híbrido (Zipf), dedupe e agrupamento de notificações | `out-s113.txt` |

## O que estes programas **não** são
São **modelos**: filas exponenciais, réplicas em memória, relógios inventados, parâmetros sintéticos. Servem para **ver o mecanismo e a ordem de grandeza**; **não** substituem medir o PostgreSQL, o Kafka, o Redis ou o balanceador reais. Onde a semântica de transação é **simulada** (`IdempotencyLab`), a aula diz e aponta a aula que a **provou** em banco real (P3.07). As **sementes** são fixas (resultados **reprodutíveis** em estatística, **não** em tempo: os milissegundos variam entre execuções).

## Limpeza
Nada a limpar: não há contêineres, `target/` nem arquivos temporários (os programas **não** escrevem fora de `out-*.txt`).
