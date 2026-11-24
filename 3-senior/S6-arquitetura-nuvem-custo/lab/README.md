# Laboratório do S6 — Arquitetura em nuvem e custo

Programas, amostras e **saídas reais** usadas nas aulas. **Tudo é modelo com premissas declaradas**, alimentado por **preços reais** da AWS Price List: **não é a AWS, não é uma fatura real e não é o PayFlow**.

## Requisitos
- **JDK 25** (`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`). Os programas usam o **lançador de código-fonte com vários arquivos** (JEP 458): `java FinOpsLab.java` acha `PriceBook.java` (e `WellArchitectedLab.java`) na mesma pasta.
- **Python 3** só para `fetch-prices.py` (biblioteca padrão; rode com `python3 -I`).
- **Terraform** (opcional) só para validar `terraform/finops` e `terraform/organization` (`terraform validate`, **sem** conta AWS). Para não baixar o provedor de novo, `TF_PLUGIN_CACHE_DIR` pode apontar para o cache da P8.
- Nada de Docker, nada de conta na nuvem.

## Como rodar
```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
./run-all.sh                     # regrava os 18 out-s60*.txt (~20 s); ONLY=s4 ./run-all.sh roda uma seção
python3 -I fetch-prices.py --cache ~/.cache/s6-pricing    # opcional: atualiza prices.csv com os preços de HOJE
```
**Atenção:** `fetch-prices.py` lê **em fluxo** os arquivos do EC2 (~300 MB por região) e dos *Savings Plans* (~300 MB por região), guardando só as linhas usadas: leva alguns minutos e **muda os números** das saídas (os preços mudam). As aulas usam o `prices.csv` gravado em **2026-11-23** (ofertas publicadas de 08 a 23/11/2026). As saídas são **determinísticas** (sementes fixas, sem tempo de relógio): rodar de novo com o mesmo `prices.csv` dá o mesmo texto.

## Programas e saídas
| Arquivo | Aula | Saídas | O que mostra |
|---|---|---|---|
| `fetch-prices.py` → `prices.csv` | todas | `prices.csv` | 100 preços (2 regiões) da **Price List Bulk API** (sem credenciais) + economia/interrupção do **Spot Instance Advisor**, com data de publicação e `usageType` |
| `PriceBook.java` | todas | — | leitor do `prices.csv` (preço ausente = erro, nunca zero) |
| `WellArchitectedLab.java` + `samples/wa-review-*.csv` | S6.01 | `out-s601-tradeoff.txt`, `out-s601-review.txt` | preço das opções de folga A/B/C/B2 da S4.06 e do Multi-AZ do RDS; razão de preços entre regiões; *linter* de revisão Well-Architected (boa passa, ruim falha com 6 problemas) |
| `ComputeLab.java` | S6.02 | `out-s602-curve.txt`, `out-s602-curve-sa.txt`, `out-s602-memory.txt` | custo da API em Lambda × Fargate × EC2 por tráfego (cruzamento) e do Lambda por memória (CPU × I/O) |
| `LockInLab.java` + `samples/lockin/{good,leaky}` | S6.03 | `out-s603-scan.txt`, `out-s603-exit.txt`, `out-s603-compile.txt` | SDK do provedor só nos adaptadores (2 vazamentos detectados); gravidade dos dados; sair × ativo/ativo; as classes de exemplo **compilam** contra o AWS SDK v2 2.59.1 (se os JARs estiverem no `~/.m2`) |
| `FinOpsLab.java` + `samples/bom-payflow.csv`, `samples/resources.csv` | S6.04 | `out-s604-{estimate,nat,tags,commit,spot}.txt` | estimativa (BOM × preço), NAT × *endpoints*, *linter* de tags, *Savings Plan* e reservas, Spot com e sem *checkpoint* (Monte Carlo) |
| `terraform/finops`, `terraform/organization` | S6.04, S6.05 | `out-s604-terraform.txt` | `terraform validate` dos dois módulos + **mutante** que precisa falhar |
| `LandingZoneLab.java` + `samples/scp/*.json` | S6.05 | `out-s605-scp.txt` | **modelo** da avaliação de SCPs: 17 casos, 5 mutantes/variantes, regras estruturais (≥ 1 e ≤ 10 SCPs por entidade) |
| `BillLab.java` → `samples/cur-synthetic.csv` | S6.06 | `out-s606-{generate,analyze,detect}.txt` | fatura diária **sintética** (preços reais, causas plantadas), análise do total ao tipo de uso, custo por unidade, detectores por serviço × total × orçamento |

## O que NÃO é
- **Não é a AWS:** a avaliação de SCP é um **modelo** (subconjunto de ações e condições; sem RCP, políticas de recurso e limites de permissão); nada foi criado em conta nenhuma.
- **Não é uma fatura real:** `cur-synthetic.csv` é inventado (as quantidades e as cinco causas), só os **preços unitários** são reais. O detector é uma regra simples, **não** o AWS Cost Anomaly Detection.
- **Não é o PayFlow:** a demanda por hora, o tráfego, o tamanho dos logs, os GB processados e o tamanho do banco são **premissas** (escritas no código e no BOM). Rode com os números **medidos** do seu sistema.
- **Não inclui** cota gratuita, impostos, plano de suporte, descontos negociados nem o custo de **pessoas**.

## Limpeza
Nada a limpar: sem contêineres, sem recursos. `run-all.sh` remove as pastas `.terraform` que o `terraform init` cria. O cache de preços (`~/.cache/s6-pricing`, ~40 MB depois do filtro, quase tudo os CSVs do RDS) pode ser apagado quando quiser.
