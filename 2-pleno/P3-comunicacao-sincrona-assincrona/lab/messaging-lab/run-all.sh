#!/usr/bin/env bash
# Roda a suíte inteira e grava a saída "didática" (linhas impressas pelos testes) em out-*.txt.
# O broker do Kafka precisa de KAFKA_LOG_RETENTION_CHECK_INTERVAL_MS=2000 e KAFKA_LOG_CLEANER_BACKOFF_MS=1000 (KafkaRetentionIT).
# Pré-requisito: containers lab-p3-pg (55432), lab-p3-rabbit (55672) e lab-p3-kafka (59092) no ar (ver ../README.md).
set -u
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}
cd "$(dirname "$0")"
mvn -B -o test > out-suite.raw 2>&1
grep -a -E "Tests run:|BUILD" out-suite.raw | sed 's/ -- in / em /' > out-suite-resumo.txt
grep -a -E "^(PARTICAO|ORDEM|CFG|MESMO|OUTRO|COMMITADO|CONSUMIDORES)" out-suite.raw > out-kafka-basics.txt
grep -a -E "^(TOPIC|COMPETING|DLQ|REDELIVERED)" out-suite.raw > out-rabbit-basics.txt
grep -a -E "^(\[kafka|\[rabbit|TEMPO|PROCESS|TENTAT|DLT valor|   kafka_dlt-(exception-cause|original-topic|original-consumer)|RT_|RABBIT_)" out-suite.raw | cut -c1-300 > out-retry-dlt.txt
grep -a -E "^(AT_MOST_ONCE|AT_LEAST_ONCE antes|TX_)" out-suite.raw > out-delivery.txt
grep -a -E "^(SEM_PROTECAO|IDEMPOTENTE|INGENUO|FALHA)" out-suite.raw > out-idempotency.txt
grep -a -E "^(ESCRITA_DUPLA|OUTBOX|CRASH_SIMULADO|AT_LEAST_ONCE|DOIS_RELES)" out-suite.raw > out-outbox.txt
grep -a -E "^(REENVIO)" out-suite.raw > out-webhook.txt
grep -a -E "^CASO" out-suite.raw > out-case-double-debit.txt
grep -a -E "^(PRODUCER_|CONSUMER_|CHAVE|LENTO)" out-suite.raw | cut -c1-300 > out-kafka-defaults.txt
grep -a -E "^(RETENCAO|COMPACTACAO)" out-suite.raw > out-retention.txt
grep -a -E "^(ASYNC_|AUTO_|VOID_|EXECUTOR_|SHEDLOCK)" out-suite.raw > out-async-scheduler.txt
grep -a -E "^(SSE|WS)" out-suite.raw > out-realtime.txt
grep -a -E "^(IO |CPU |STACK|   com|   reactor|   java)" out-suite.raw > out-reactive-vs-virtual.txt
cat out-suite-resumo.txt | tail -3
