#!/usr/bin/env bash
# Incidente simulado (P6.07) contra a pilha de ./run-stack.sh. Imprime linhas "T5 ..." (as aulas mostram essas saídas).
set -uo pipefail
cd "$(dirname "$0")"
APP=localhost:8080; PROM=localhost:9090; JAE=localhost:16686; LOG=target/stack/app.log
q() { curl -s -G "$PROM/api/v1/query" --data-urlencode "query=$1" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print(' ; '.join(f\"{float(r['value'][1]):.4f}\" for r in d['data']['result']) or '(sem série)')"; }
alerts() { curl -s "$PROM/api/v1/alerts" | python3 -c "
import sys,json
a=sorted({(x['labels']['alertname'],x['state']) for x in json.load(sys.stdin)['data']['alerts']})
print(a if a else '(nenhum)')"; }
lat() { # p50 p95 p99 média e fração > 1 s, janela de 1 minuto
  B='sum by (le) (rate(http_server_requests_seconds_bucket{uri="/api/v1/transfers",status="201"}[1m]))'
  echo "T5 $1 p50=$(q "histogram_quantile(0.5,$B)")s p95=$(q "histogram_quantile(0.95,$B)")s p99=$(q "histogram_quantile(0.99,$B)")s"
  echo "T5 $1 média=$(q 'sum(rate(http_server_requests_seconds_sum{uri="/api/v1/transfers",status="201"}[1m]))/sum(rate(http_server_requests_seconds_count{uri="/api/v1/transfers",status="201"}[1m]))')s  fração>1s=$(q '1-sum(rate(http_server_requests_seconds_bucket{uri="/api/v1/transfers",status="201",le="1.0"}[1m]))/sum(rate(http_server_requests_seconds_bucket{uri="/api/v1/transfers",status="201",le="+Inf"}[1m]))')"
}
jaeger_traces() { # $1 = filtros extra da busca v3; imprime contagem por conta
  curl -s "$JAE/api/v3/traces?query.service_name=payflow-obs-lab&query.operation_name=http%20post%20%2Fapi%2Fv1%2Ftransfers&query.start_time_min=$(date -u -d '-30 min' +%FT%TZ)&query.start_time_max=$(date -u -d '+5 min' +%FT%TZ)&query.search_depth=300&$1" \
  | python3 -c "
import sys,json,collections
r=json.load(sys.stdin)
sp=[s for rs in r['result']['resourceSpans'] for ss in rs['scopeSpans'] for s in ss['spans']]
acc=collections.Counter(); tids=set()
for s in sp:
    tids.add(s['traceId'])
    if s['name']=='payflow.transfer':
        a={x['key']:list(x['value'].values())[0] for x in s['attributes']}
        acc[a.get('payflow.source.account')]+=1
print(f'{len(tids)} traços; contas (atributo payflow.source.account): {dict(acc)}')"
}
echo "T5 --- reset"; curl -s -X POST "$APP/lab/risk-latency?account=acc-7&ms=0" >/dev/null; curl -s -X POST "$APP/lab/switch?what=outbox-stall&on=false" >/dev/null
echo "T5 --- FASE A: tráfego normal"; ./traffic.sh 200 0.03; sleep 10; lat "A"; echo "T5 A erros5xx=$(q 'payflow:http_errors:ratio5m')  alertas=$(alerts)"
echo "T5 --- FASE B: o serviço de risco fica LENTO (1500 ms) só para acc-7"; curl -s -X POST "$APP/lab/risk-latency?account=acc-7&ms=1500" >/dev/null
./traffic.sh 150 0.03; sleep 10; lat "B"; echo "T5 B erros5xx=$(q 'payflow:http_errors:ratio5m')  alertas=$(alerts)"
echo "T5 B Jaeger (duração >= 1 s): $(jaeger_traces 'query.duration_min=1s')"
echo "T5 B Jaeger (duração < 1 s, amostra): $(jaeger_traces 'query.duration_max=500ms')"
echo "T5 --- FASE C: a lentidão passa do timeout de 2 s do cliente (2500 ms): 5xx"; curl -s -X POST "$APP/lab/risk-latency?account=acc-7&ms=2500" >/dev/null
./traffic.sh 150 0.03; sleep 12; echo "T5 C erros5xx=$(q 'payflow:http_errors:ratio5m')  alertas=$(alerts)"
RESP=$(curl -s -X POST "$APP/api/v1/transfers" -H 'Content-Type: application/json' -H 'X-Request-Id: ticket-suporte-4711' -d '{"from":"acc-7","to":"acc-x","amount":"10.00","simulate":null}')
echo "T5 C resposta ao usuário: $RESP"
TID=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('traceId',''))")
if [ -n "$TID" ]; then
  sleep 7
  echo "T5 C traço $TID:"
  curl -s "$JAE/api/traces/$TID" | python3 -c "
import sys,json
d=json.load(sys.stdin)['data'][0]
for s in sorted(d['spans'],key=lambda s:s['startTime']):
    t={x['key']:x['value'] for x in s['tags']}
    print(f\"T5 C   {s['operationName']:<30} {s['duration']/1000:8.1f} ms  otel.status_code={t.get('otel.status_code','-'):<5} status={t.get('status','-'):<12} outcome={t.get('outcome','-'):<12} exception={t.get('exception','-')}\")"
  echo "T5 C logs com esse traceId:"
  grep -F "$TID" "$LOG" | python3 -c "
import sys,json
for l in sys.stdin:
    d=json.loads(l); e=d.get('error',{})
    print('T5 C   ',d['@timestamp'][11:23],d['log']['level'],d['message'][:60],'| requestId='+d.get('requestId',''),'| error.type='+e.get('type','-'))"
fi
echo "T5 C requestId do ticket no log: $(grep -cF 'ticket-suporte-4711' "$LOG") linha(s)"
echo "T5 --- FASE D: o relé do outbox para (o 'incidente 2')"; curl -s -X POST "$APP/lab/risk-latency?account=acc-7&ms=0" >/dev/null; curl -s -X POST "$APP/lab/switch?what=outbox-stall&on=true" >/dev/null
./traffic.sh 160 0.02 ""; sleep 25; echo "T5 D payflow_outbox_pending=$(q 'payflow_outbox_pending')  alertas=$(alerts)"
curl -s -X POST "$APP/lab/switch?what=outbox-stall&on=false" >/dev/null
echo "T5 --- fim"
