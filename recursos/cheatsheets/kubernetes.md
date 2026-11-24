# Cheatsheet — Containers e Kubernetes (P7)

> Valores de imagem e JVM **medidos** em `2-pleno/P7-containers-kubernetes/lab/k8s-lab/jvm-lab` e nas aulas P7.01/P7.02 (JDK 25.0.5, Docker 29.9.2). **Comandos de `kubectl`/`helm` e manifestos: NÃO executados** (conferidos na documentação oficial); confira ao usar.

## Imagem (P7.01), medido no PayFlow Lite
| Forma | Tamanho | Rebuild (1 `.java`) | Observação |
|---|---|---|---|
| Dockerfile (JRE Alpine) | 357 MB | 3,4 s | controle total |
| Jib (JRE Ubuntu) | 527 MB | 4,6 s | sem daemon com `jib:build`; usuário **root** por padrão |
| Buildpacks | 686 MB | 10,7 s | SBOM, calculador de memória, não root |
Regras: tag **imutável** (`versão-sha`), `USER` **numérico**, sem segredo, nada de `latest`.

## JVM em contêiner (P7.02), medido (`--cpus 2`)
| `--memory` | heap padrão (25%) | coletor |
|---|---|---|
| 256m–1g | 121–247 MiB | **Serial** |
| 2g | 512 MiB | **G1** |
`--cpus 0.5`/`1` → 1 CPU (Serial); `1.5` → 2 CPUs; `cpu-shares` **ignorado**. `MaxRAMPercentage=70`; coletor **explícito**; `ExitOnOutOfMemoryError` (exit 3); `MaxDirectMemorySize` com teto.
| Evento | Código | Quem |
|---|---|---|
| `OutOfMemoryError` | 0/3 | JVM |
| **`OOMKilled`** | **137** | **kernel** |
| `Evicted` | – | nó (QoS) |

## kubectl essencial (P7.03, P7.07)
```bash
kubectl config current-context                      # ANTES de apply/delete
kubectl -n payflow get deploy,rs,pods,svc,ingress,hpa,pdb
kubectl -n payflow describe pod <pod>               # eventos, Last State, Reason/Exit Code
kubectl -n payflow logs <pod> --previous            # o contêiner que morreu
kubectl -n payflow rollout status|history|undo deploy/payflow
kubectl -n payflow port-forward svc/payflow 8080:80
kubectl debug -it pod/<pod> --image=eclipse-temurin:25-jdk --target=payflow   # contêiner efêmero
```
## Estados e causas (P7.07)
| Sintoma | Causa provável |
|---|---|
| `ImagePullBackOff` | imagem não existe/`kind load` esquecido/`latest` |
| `CrashLoopBackOff` | processo sai; veja `logs --previous` |
| `Pending` | recursos insuficientes (`FailedScheduling`) |
| `0/1 Running` | *readiness* falhando (banco, config) |
| `CreateContainerConfigError` | Secret/ConfigMap ausente |
| `violates PodSecurity` | manifesto fora do perfil *restricted* |

## Sondas e recursos (P7.05)
`startup` (5 s × 36) → `liveness` (10 s × 3, **só processo**) → `readiness` (5 s × 3, banco). `requests` = reserva/base do HPA; `limits.cpu` = *throttling*; `limits.memory` = **morte**. QoS: Guaranteed > Burstable > BestEffort. `terminationGracePeriodSeconds` ≥ `preStop` + `timeout-per-shutdown-phase` + folga. PDB `minAvailable: 1` com ≥ 2 réplicas.

## HPA (P7.06)
`desejadas = ceil(atuais × atual/alvo)`; tolerância 0,1; padrões: sobe sem janela (+100% ou +4 pods/15 s), desce com janela de 300 s. Utilização = **uso ÷ requests**. `maxReplicas × pool < max_connections`.

## Entrada, config e segredos (P7.04)
Ingress = **declaração** (precisa de controlador); API **congelada**; Gateway API recomendada; **ingress-nginx sem manutenção após mar/2026**. ConfigMap = não secreto; Secret = arquivo + `configtree`; **base64 ≠ criptografia**.

## Helm 4 (P7.08)
`helm lint|template|install|upgrade|rollback`; `--atomic` → **`--rollback-on-failure`**, `--force` → `--force-replace`; `--server-side` verdadeiro por padrão; `checksum/config` força *rollout*; segredo fora do `values.yaml`.
