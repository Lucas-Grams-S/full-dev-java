// S2.06 - carga ABERTA com k6: chegadas a taxa constante, independente das respostas.
// Uso: docker run --rm --network host -i -e RATE=400 grafana/k6:1.8.3 run - < k6-open.js
import http from 'k6/http';
import { check } from 'k6';

const RATE = Number(__ENV.RATE || 400);

export const options = {
  scenarios: {
    aberta: {
      executor: 'constant-arrival-rate',   // inicia RATE iteracoes por segundo, aconteca o que acontecer
      rate: RATE,
      timeUnit: '1s',
      duration: '20s',
      preAllocatedVUs: 50,
      maxVUs: 1000,
    },
  },
  thresholds: {                            // criterios de aprovacao (o k6 sai com codigo != 0 se falharem)
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<50', 'p(99)<250'],
  },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const r = http.get('http://127.0.0.1:18080/api');
  check(r, { 'status 200': (x) => x.status === 200 });
}
