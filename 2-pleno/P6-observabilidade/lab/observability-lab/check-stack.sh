#!/usr/bin/env bash
# Confere a pilha (Prometheus, Grafana, Jaeger) depois do tráfego. Imprime linhas "T6 ...".
set -uo pipefail
cd "$(dirname "$0")"
echo "T6 versões: $(curl -s localhost:9090/api/v1/status/buildinfo | python3 -c "import sys,json; print('Prometheus', json.load(sys.stdin)['data']['version'])") ; Grafana $(curl -s localhost:3000/api/health | python3 -c "import sys,json; print(json.load(sys.stdin)['version'])") ; Jaeger $(docker logs lab-p6-jaeger 2>&1 | grep -oE 'git-version=v[0-9.]+' | head -1 | cut -d= -f2)"
curl -s localhost:9090/api/v1/targets | python3 -c "
import sys,json
for t in json.load(sys.stdin)['data']['activeTargets']: print('T6 alvo', t['labels']['job'], t['scrapeUrl'], t['health'])"
curl -s localhost:9090/api/v1/rules | python3 -c "
import sys,json
for g in json.load(sys.stdin)['data']['groups']: print('T6 grupo de regras', g['name'], len(g['rules']), 'regras', {r['health'] for r in g['rules']})"
curl -s -u admin:lab-only-admin 'localhost:3000/api/search?query=PayFlow' | python3 -c "
import sys,json
for d in json.load(sys.stdin): print('T6 dashboard provisionado:', d['uid'], '|', d['title'])"
python3 - <<'PY'
import json,urllib.request,base64,urllib.parse
auth='Basic '+base64.b64encode(b'admin:lab-only-admin').decode()
def get(u): return json.load(urllib.request.urlopen(urllib.request.Request(u,headers={'Authorization':auth})))
d=get('http://localhost:3000/api/dashboards/uid/payflow-red')['dashboard']
ok=0; empty=[]
for p in d['panels']:
    for t in p.get('targets',[]):
        e=t['expr'].replace('$instance','.*').replace('$__rate_interval','1m')
        r=get('http://localhost:3000/api/datasources/proxy/uid/prometheus/api/v1/query?'+urllib.parse.urlencode({'query':e}))
        if r['status']=='success' and r['data']['result']: ok+=1
        else: empty.append(p['title'][:40])
print(f"T6 painéis com dado: {ok}; sem série (esperado só o do Hikari, sem banco no laboratório): {empty}")
PY
curl -s 'localhost:16686/api/v3/services' | python3 -c "import sys,json; print('T6 Jaeger serviços:', json.load(sys.stdin).get('services'))"
