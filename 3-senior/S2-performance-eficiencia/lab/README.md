# Laboratório do S2 — Performance e eficiência

Programas de **Java puro** (`java X.java`, JDK 25), o projeto Maven **`jmh-lab`** e **scripts** que regravam as saídas `out-s20*.txt` citadas nas aulas. **Executados em 2026-11-14** (OpenJDK 25.0.5, Linux, **12 núcleos, máquina compartilhada**). **São modelos e programas pequenos: os números não valem para o PayFlow.** Os valores **variam** entre execuções.

| Aula | Programa / script | O que faz | Saídas |
|---|---|---|---|
| S2.01 | `MethodLab.java` | aquecimento, média × percentis, Amdahl na requisição, ruído | `out-s201.txt` |
| S2.02 | `ProfileApp.java`, `CustomEventDemo.java`, `run-profile.sh` | 3 defeitos plantados × corrigidos, lidos com `jfr view`; evento próprio | `out-s202-*.txt` |
| S2.03 | `BiasLab.java`, `FlameFold.java`, `run-profile.sh` | viés de amostragem (poor-man × JFR); pilhas colapsadas e árvore | `out-s203-*.txt` |
| S2.04 | `LeakLab.java`, `HprofHisto.java`, `run-heap.sh` | cache com chave errada (vaza), OOM + dump, histograma, `OldObjectSample`, correção | `out-s204-*.txt` |
| S2.05 | `GcLab.java`, `run-gc.sh` | mesmo serviço sob 7 configurações de GC/heap (carga aberta, ~7 min) | `out-s205.txt` |
| S2.06 | `LoadLab.java`, `k6-open.js`, `run-load.sh` | curva de saturação, omissão coordenada, k6 1.8.3 em Docker (aberto, limiares, exit 99) | `out-s206-*.txt` |
| S2.07 | `MiniApp.java`, `PeakApp.java`, `CracApp.java`, `run-startup.sh`, `run-peak.sh`, `run-crac.sh` | partida (CDS, AppCDS, cache AOT, native image), pico (JVM × nativo), CRaC (Zulu 25, motor `warp`) | `out-s207*.txt` |
| S2.08 | `jmh-lab/`, `MoneyCheck.java`, `run-jmh.sh` | JMH 1.37: código morto, constantes, `BigDecimal` × `long` × `double`, texto, *streams* | `out-s208-*.txt` |

## Como rodar
```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64      # JDK 25
./run-profile.sh        # ~2 min   (S2.02, S2.03)
./run-heap.sh           # ~40 s    (S2.04)
./run-gc.sh             # ~7 min   (S2.05)  — nao rode nada em paralelo
./run-load.sh           # ~3 min   (S2.06)  — precisa de Docker (grafana/k6:1.8.3)
./run-startup.sh        # ~1 min   (S2.07)  — native image so se a imagem ghcr.io/graalvm/native-image-community:25 existir
./run-peak.sh           # ~1,5 min (S2.07)  — idem
./run-crac.sh           # ~1,5 min (S2.07)  — precisa da imagem azul/zulu-openjdk:25-jdk-crac
./run-jmh.sh            # ~7 min   (S2.08)  — Maven 3.9, baixa o JMH do Maven Central
java MethodLab.java > out-s201.txt ;  java MoneyCheck.java > out-s208-moneycheck.txt
```
**Regras:** rode **um script por vez** (as medições disputam CPU); **nada** de abrir outros programas pesados; os scripts **limpam** os temporários (`mktemp -d`).

## Imagens Docker usadas (remova ao terminar)
```bash
docker rmi grafana/k6:1.8.3 ghcr.io/graalvm/native-image-community:25 azul/zulu-openjdk:25-jdk-crac
```
