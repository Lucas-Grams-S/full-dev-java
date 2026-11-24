# Laboratório da S7 — Liderança técnica

Programas pequenos que dão **evidência** às decisões de liderança: verificam documentos, tiram riscos com um *spike*, simulam estimativas, medem o histórico do Git, comparam implementações em paralelo, medem a qualidade dos testes e testam uma integração com LLM **sem** chamar modelo nenhum.

## Requisitos
- JDK 25 (`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`; o `java` padrão da máquina do curso é o 17).
- Maven 3.9 (para `review-lab`, `ai-lab` e `legacy-upgrade`; a primeira execução baixa dependências).
- `git` e rede na **primeira** execução da seção `s704` (clona o `spring-petclinic`; ou aponte `PETCLINIC_DIR` para um clone).

## Rodar tudo
```bash
./run-all.sh                 # regrava as saídas out-s70*
ONLY=s704 ./run-all.sh       # só uma seção (s701 … s707, s704-rewrite)
```
A seção `s704-rewrite` baixa o OpenRewrite e as receitas (~100 MB na primeira vez).

## Conteúdo
| Aula | Arquivo | O que mostra | Saída |
|---|---|---|---|
| S7.01 | `DocLab.java` (modos `adr` e `rfc`) | *linter* das 33 ADRs reais (29 até a S6 + 4 da S7) e das RFCs (boa, ruim, 2 mutantes) | `out-s701-adr.txt`, `out-s701-rfc.txt` |
| S7.02 | `DocLab.java design`, `RecurrenceSpike.java` | *linter* do design doc (boa, ruim, mutante); *spike* de datas, fusos, pico e chaves | `out-s702-design.txt`, `out-s702-spike.txt` |
| S7.03 | `EstimateLab.java` | três pontos e Monte Carlo, registro de riscos, previsão por vazão | `out-s703-estimate.txt` |
| S7.04 | `HotspotLab.java`, `StranglerLab.java`, `legacy-upgrade/` | hotspots e acoplamento de mudança (histórico real), leitura em paralelo com mutantes, OpenRewrite *dry run* | `out-s704-*.txt`, `out-s704-rewrite-*.patch` |
| S7.05 | `review-lab/`, `FlakyLab.java` | JaCoCo × PIT (suíte fraca × forte); conta dos testes instáveis | `out-s705-review.txt`, `out-s705-flaky.txt` |
| S7.06 | `PriorityLab.java` | FIFO × CD3 com força bruta; dívida sem custo, quantificada e fatiada | `out-s706-priority.txt` |
| S7.07 | `ai-lab/` | Spring AI 2.0.2 com `ChatModel` falso: saída estruturada, guarda, ferramentas, injeção | `out-s707-ai.txt` |

`samples/` tem a **RFC-0001** e o **design doc** das transferências recorrentes (as versões "boas", que são a entrega da S7 no PayFlow) e as versões ruins usadas pelo *linter*.

## Limites (leia)
- **Premissas declaradas**, não medições do PayFlow: três pontos, riscos, vazão, custo do atraso, pico das 06:00, atraso do projetor.
- O **histórico do Git é real** (`spring-petclinic`, *commit* `500158f732419217507c7656904b8e6aa1bcc0d6`); a complexidade é uma **aproximação** por expressão regular.
- O `ai-lab` **não chama modelo** (sem chave, sem custo); a configuração do Claude só é **compilada**.
- Nada aqui toca o repositório `payflow` do aluno.
