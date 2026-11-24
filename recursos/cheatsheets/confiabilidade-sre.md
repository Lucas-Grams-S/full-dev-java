# Cheatsheet — Confiabilidade e operação (S4)

> Valores de **modelos** (Java puro e `promtool`, premissas declaradas): **não** são do PayFlow nem da AWS. Milissegundos variam.

## SLO e orçamento (S4.01)
| Tema | Regra / número | Onde |
|---|---|---|
| Orçamento | `1 − SLO`; 99,9% / 30 d = **43,2 min**; 99,95% = 21,6; 99,99% = 4,3 min; a 5 req/s: 12.960 requisições (99,9%) | S4.01 |
| *Burn rate* | `taxa de erro ÷ (1 − SLO)`; esgota em `30 d ÷ burn`; **14,4× = 50 h**; 6× = 5 d; 1× = 30 d | S4.01 |
| Gasto | `burn × janela ÷ 30 d`: 14,4× por 1 h = **2%**; 6× por 6 h = 5%; 1× por 3 d = 10% | S4.01 |
| Alertas | **página** 1 h **e** 5 min > 14,4× **ou** 6 h **e** 30 min > 6×; **ticket** 3 d **e** 6 h > 1×; janela curta ≈ 1/12; **tráfego mínimo 0,1 req/s** | S4.01 |
| Série | disponibilidade multiplica: 3 × 99,9% = 99,70% (modelo) | S4.01 |
| SLI bom | razão bons ÷ válidos, perto do usuário; CPU/*lag*/disco = causa (alerta), não SLO | S4.01 |
| Teste | `promtool check rules` + `test rules` + **mutante** (um teste que nunca falhou não prova nada) | S4.01 |

## Incidente, runbook e postmortem (S4.02, S4.03)
| Tema | Regra | Onde |
|---|---|---|
| Ordem | declarar → **mitigar** (rollback, flag, escala) → comunicar → verificar no SLI → aprender | S4.02 |
| Papéis | comando (não debuga), operação, comunicação, registro | S4.02 |
| Severidade | SEV1 dinheiro/indisponibilidade total/perda de dado; SEV2 página de SLO; SEV3 ticket; na dúvida suba | S4.02 |
| *Runbook* | impacto · diagnóstico numerado · mitigação com comando · escalar · verificação · dono · revisão | S4.02 |
| CI | `DocLint` + `check-runbooks.sh`: alerta sem *runbook* reprova | S4.02 |
| Postmortem | gatilhos escritos; 7 seções; "erro humano" é sintoma; ações **prevenir > detectar > mitigar**, dono e data | S4.03 |

## Caos (S4.04)
| Tema | Regra / medido | Onde |
|---|---|---|
| Método | estado estável → hipótese → variável → **refutar** | S4.04 |
| Segurança | *staging* primeiro; raio pequeno; **condição de parada** que enxerga requisições **penduradas**; produção só com orçamento > 50% | S4.04 |
| `threads ÷ timeout` | 16 ÷ 0,213 s ≈ **75 req/s**; acima disso, fila | S4.04 |
| Sem *timeout* | 40 req/s × 3 s = **120** *threads* desejadas: sucesso **19,8%** | S4.04 |
| Assíncrono | disponibilidade ganha, **entrega perde** (74/240; 556/600) → Outbox | S4.04 |
| FIS | `aws:ecs:stop-task`, `aws:rds:reboot-db-instances` (`forceFailover`), `aws:network:disrupt-connectivity`; parada = alarme | S4.04 |

## *Backup*, RPO e RTO (S4.05)
| Tema | Regra / medido | Onde |
|---|---|---|
| RPO / RTO | dados que posso perder / tempo que posso ficar fora; **por cenário e por dado**; RTO = **soma de passos** | S4.05 |
| Cópia × *backup* | Multi-AZ/réplica **copiam o erro**; só o *backup* volta no tempo | S4.05 |
| RPO por mecanismo | log a cada 300 s: média ~750, pior ~1.500 (5 commits/s); síncrono 0 | S4.05 |
| Camadas | Multi-AZ · PITR · *snapshot* manual/final · cópia outra região **e** conta · imutável (Vault Lock/Object Lock) · `pg_dump` · IaC | S4.05 |
| Restaurar | **conciliar** o que está fora do banco (PSP, e-mails, chaves de idempotência) | S4.05 |
| Ensaio mensal | ponto aleatório → instância temporária → invariantes → cronômetro → destruir | S4.05 |
| CLI (conferida na ajuda) | `aws rds restore-db-instance-to-point-in-time --source-db-instance-identifier … --target-db-instance-identifier … (--restore-time … ou --use-latest-restorable-time)` | S4.05 |

## Multi-AZ e DR (S4.06)
| Tema | Regra / modelo | Onde |
|---|---|---|
| N+1 | 2 AZs a 60% → **120%** ao perder uma; para ≤ 85% o regime ≤ **42%**; 3 AZs a 60% → 90% | S4.06 |
| Conexões | `tarefas × (maximum_percent/100) × pool + reserva ≤ max_connections` (6 × 2 × 10 = 120 > 90) | S4.06 |
| Cadeia | 1 AZ 99,75% × 2 AZs 99,95% (premissas); falha **correlacionada** = uma falha | S4.06 |
| DR | backup e restauração · *pilot light* · *warm standby* · multi-site ativo/ativo (do barato/lento ao caro/rápido) | S4.06 |
| PayFlow | Multi-AZ sim; **multi-região não** (gatilhos: SLO > 99,95%, regulatório, custo/hora assinado) | S4.06 |

## Capacidade (S4.07)
| Tema | Regra / modelo | Onde |
|---|---|---|
| USE | utilização, **saturação**, erros por recurso | S4.07 |
| Joelho (M/M/4) | p99 93 ms (30%) · 100 (60%) · 140 (80%) · 237 (90%) · 400 (95%) · 940 (98%) | S4.07 |
| Little | `L = λ × W`: 50 req/s × 3 s = 150 | S4.07 |
| Previsão | `predict_linear(v[janela], t)`: a **janela** é a decisão (degrau → 74 d × 107 d reais) | S4.07 |
| Alerta de prazo | ticket `[3d]`/14 d, página `[6h]`/24 h, `for` | S4.07 |
| *Slot* | `tempo p/ consertar × WAL/h ≤ max_slot_wal_keep_size ≤ livre − margem` | S4.07 |
