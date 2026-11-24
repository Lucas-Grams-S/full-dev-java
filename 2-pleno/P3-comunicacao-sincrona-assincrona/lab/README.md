# Laboratórios do P3

Tudo foi **executado** em Linux, JDK **25.0.5** (`/usr/lib/jvm/java-25-openjdk-amd64`), Maven 3.9, Docker, 12 núcleos **compartilhados** (tempos oscilam). As saídas reais estão em `out-*.txt` e são **o que as aulas mostram**.

## `io-lab/` (Java puro; P3.01)
```bash
cd io-lab
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
$JAVA_HOME/bin/java IoLab.java io     # out-io.txt
$JAVA_HOME/bin/java IoLab.java cpu    # out-cpu.txt
$JAVA_HOME/bin/java IoLab.java http   # out-http.txt (-Dhttp.version=HTTP_2 para comparar)
```

## `messaging-lab/` (Maven; P3.02 a P3.13)
Spring Boot **4.1.2**, Spring Kafka **4.1.2**, Spring AMQP **4.1.2**, ShedLock **7.11.0**, `kafka-clients` **4.2.2**, Reactor (BOM). Sobe **3 containers** com portas altas (para não colidir com nada do aluno) e **os remove ao fim**:
```bash
docker run -d --name lab-p3-pg -e POSTGRES_PASSWORD=lab -e POSTGRES_DB=lab -p 55432:5432 postgres:18
docker run -d --name lab-p3-rabbit -p 55672:5672 -p 55673:15672 rabbitmq:4-management-alpine
docker run -d --name lab-p3-kafka -p 59092:59092 \
  -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:59092,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:59092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
  -e KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS=2000 -e KAFKA_LOG_CLEANER_BACKOFF_MS=1000 \
  apache/kafka:latest
cd messaging-lab && JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./run-all.sh
```
> ⚠️ A porta do Kafka é a **mesma dentro e fora** do contêiner (59092): com `-p 59092:9092` o *broker* não consegue falar consigo mesmo pelo endereço anunciado e as **transações** travam (`NOT_ENOUGH_REPLICAS … Unable to verify the partition has been added to the transaction`). `KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS`/`KAFKA_LOG_CLEANER_BACKOFF_MS` curtos servem **só** ao teste de retenção/compactação.
> ⚠️ Os testes **criam tópicos e filas com sufixo aleatório** e **apagam tabelas** do banco `lab` (`delete from …`). **Nunca** aponte o laboratório para um banco que não seja o do contêiner `lab-p3-pg`.

| Classe | Testes | Aula |
|---|---|---|
| `KafkaBasicsIT` | 4 | P3.04 |
| `KafkaDefaultsIT` | 3 | P3.04 |
| `KafkaRetentionIT` | 2 | P3.05 |
| `RabbitBasicsIT` | 5 | P3.03 |
| `AsyncSchedulerIT` | 5 | P3.02 |
| `DeliveryAndIdempotencyIT` | 7 | P3.06, P3.07 |
| `RetryDltIT` | 3 | P3.08 |
| `OutboxIT` | 4 | P3.09 |
| `WebhookIT` | 6 | P3.10 |
| `RealtimeIT` | 2 | P3.11 |
| `ReactiveVsVirtualIT` | 3 | P3.12 |
| `DoubleDebitCaseIT` | 3 | P3.13 |

Limpeza ao fim: `docker rm -f lab-p3-pg lab-p3-rabbit lab-p3-kafka`.
