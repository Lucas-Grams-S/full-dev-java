# Laboratório do S4 — Confiabilidade e operação (SRE)

Programas, regras e **saídas reais** usadas nas aulas. **Tudo é um modelo** (tráfego sintético, servidores locais, premissas declaradas), **não o PayFlow**.

## Requisitos
- **JDK 25** (`/usr/lib/jvm/java-25-openjdk-amd64`); nada de Maven e nada de Docker para os programas Java (`java X.java`).
- **Docker** só para o `promtool` (imagem `prom/prometheus`; `run-promtool.sh`).
- ~1 min para tudo (o `ChaosLab` mede tempo: **rode um módulo por vez** numa máquina compartilhada).

## Como rodar
```bash
./run-all.sh                 # regrava todos os out-*.txt
ONLY=s4 ./run-all.sh         # só o experimento de caos (s1, s2, s4, s5, s6, s7, pt = promtool)
java SloLab.java alerts      # um programa isolado
./run-promtool.sh            # promtool: check + test + mutantes (--write grava out-s401/out-s407-promtool.txt)
```

## Programas, regras e saídas
| Arquivo | Aula | Saídas | O que mede |
|---|---|---|---|
| `SloLab.java` | S4.01 | `out-s401-budget.txt`, `out-s401-alerts.txt` | orçamento (minutos e requisições), *burn rate*, cadeia em série; **5 incidentes sintéticos** × 2 estratégias de alerta |
| `prometheus/slo-rules.yml` + `.test.yml` | S4.01 | `out-s401-promtool.txt` | 17 regras (gravação 5m–3d, alertas Fast/Slow/Drift de disponibilidade e latência); **5 testes**; **2 mutantes** |
| `DocLint.java` + `samples/` | S4.02/S4.03 | `out-s402-lint.txt` | verificador de *runbook* e de postmortem com **auto-teste** (bom passa, ruim falha) |
| `check-runbooks.sh` + `samples/docs/` | S4.02 | `out-s402-runbooks.txt` | cada `runbook:` das regras (P6.04 + SLO) aponta para um arquivo existente e que passa no lint |
| `ChaosLab.java` | S4.04 | `out-s404-no-timeout.txt`, `-timeout.txt`, `-timeout-async.txt`, `-timeout-100rps.txt`, `-timeout-async-100rps.txt` | latência de 3 s injetada em `notifications`; 16 *threads*; hipótese `p99 < 400 ms ∧ sucesso ≥ 99%`; **condição de parada** |
| `RpoRtoLab.java` | S4.05 | `out-s405-rpo.txt`, `out-s405-rto.txt` | perda de transações por mecanismo (**modelo**); RTO como **soma de passos** (**premissas**) |
| `AzLab.java` | S4.06 | `out-s406-capacity.txt`, `out-s406-chain.txt` | CPU ao perder 1 de 2 AZs; conta de conexões no *deploy*; disponibilidade em série × paralelo |
| `CapacityLab.java` | S4.07 | `out-s407-queue.txt`, `-little.txt`, `-forecast.txt` | fila M/M/4 (utilização × p99); Lei de Little; regressão linear em várias janelas |
| `prometheus/capacity-rules.yml` + `.test.yml` | S4.07 | `out-s407-promtool.txt` | `predict_linear` (ticket 14 d / página 24 h); **3 testes**; **mutante** |

## O que NÃO é
Não é o PayFlow, não é a AWS e **não é medição de produção**: os números de capacidade, RPO e RTO são **premissas** ou resultados de **modelos**. Cada aula diz qual é qual.
