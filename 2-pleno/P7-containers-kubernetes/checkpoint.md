# P7.CP — Checkpoint do módulo P7

> ⏱️ ~1h30 · 🔵 Pleno · Pré-requisitos: P7.01 a P7.09

> ✅ **Validação final (2026-11-24):** módulo executado no PayFlow reconstruído; resultado e o que continua não executado no [README do módulo](README.md). Os avisos "não executado" / "por leitura" abaixo descrevem a escrita original.

> ⚠️ **Estado de verificação do módulo:** por decisão do aluno, os testes práticos do PayFlow ficam para o **fim do treinamento**. **Executados de verdade** nesta geração (Linux, Docker 29.9.2, Maven 3.9.12, JDK 25.0.5): (1) as **três formas de construir a imagem** (Dockerfile, Buildpacks, Jib) sobre o PayFlow Lite da J9 (tamanhos, camadas, tempos, usuário, calculador de memória); (2) o **laboratório da JVM sob limites** (`lab/k8s-lab/jvm-lab`: heap, coletor, CPUs, `OOMKilled` × `OutOfMemoryError`). **Não executado:** **todo o Kubernetes** (`kind`, `kubectl`, `helm`: o ambiente **não permitiu instalá-los**), o `metrics-server`, o HPA, o Ingress, o `helm lint/template`. Os **manifestos** e o **chart** foram **escritos por leitura**: YAML validado, conteúdo **conferido** na documentação oficial, mas **sem um cluster que os aplique**. Isso está listado em "Correções pendentes" do `progresso.md` como **a validar no fim do treinamento**.

## Parte 1: Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Por que as **camadas** da imagem importam no Kubernetes? Compare **Dockerfile**, **Buildpacks** e **Jib** (tamanho e tempo de reconstrução **medidos**) e diga qual o PayFlow adota e por quê.
2. O que a JVM decide ao ler os limites do contêiner? Que **coletor** ela escolhe com 768 Mi e por quê isso importa? O que acontece com `--cpus 1` e `--cpus 1.5`?
3. Qual a diferença entre `OutOfMemoryError`, `OOMKilled` e `Evicted`? Que **código de saída** cada um tem (medido)?
4. Monte o **orçamento de memória** de um pod de 768 Mi com `MaxRAMPercentage=70`. Por que o *heap* não é todo o limite? Qual o papel de `MaxDirectMemorySize` e `ExitOnOutOfMemoryError`?
5. Pod, ReplicaSet, Deployment e Service: o que cada um faz? Por que não se cria Pod solto? Como o Service decide quem recebe tráfego?
6. O que `maxSurge: 1` e `maxUnavailable: 0` garantem? Em um *rollout* travado (o pod novo nunca fica `Ready`), o que acontece com o tráfego?
7. O que o `Ingress` faz **sozinho**? Por que o **ingress-nginx** não é a escolha para um projeto novo em 2026? Quando a **Gateway API** entra?
8. Por que **base64 não é criptografia**? Cite quatro controles reais para Secrets. Por que o PayFlow monta o Secret **como arquivo** (`configtree`)?
9. Defina `startupProbe`, `livenessProbe` e `readinessProbe` e o que cada falha provoca. Por que a *liveness* **nunca** verifica o banco? Que parâmetros do PayFlow dão **180 s** de partida e **30 s** para detectar travamento?
10. Calcule as réplicas do HPA (`requests.cpu 250m`, meta 60%, 3 pods usando 300m cada). Por que escalar a aplicação pode **piorar** um incidente de banco? O que mudou no **Helm 4** que afeta scripts?

<details><summary>Gabarito</summary>

1. Uma imagem é uma lista de **camadas** guardadas **uma vez** no registro; o nó só baixa o que não tem. Dependências embaixo (mudam pouco), classes em cima. **Medido** (PayFlow Lite): Dockerfile (JRE Alpine) **357 MB**, rebuild de um `.java` **3,4 s**; Jib (JRE Ubuntu) **527 MB**, **4,6 s**, 10 camadas, **usuário root por padrão** (passei `10001`); Buildpacks **686 MB**, **10,7 s**, 20 camadas, SBOM e calculador de memória. O tamanho depende **da base e do JRE**, não só da ferramenta. PayFlow: **Dockerfile da J9** (controle, menor imagem, nada novo) + tag imutável; Buildpacks se houver muitos serviços iguais; Jib se o CI não tiver Docker (decisão 147).
2. Lê o cgroup: `memory.max` e `cpu.max`. Decide **heap** (padrão **25%** da memória) e **coletor**: com 768 Mi (e 256 m a 1 g) e 2 CPUs usa **Serial**; com 2 g, **G1**. `--cpus 1` → 1 CPU visível (Serial); `1.5` → **2** CPUs (arredonda para cima, G1). `requests.cpu` (shares) é **ignorado** pela JVM (viu 12 CPUs). Importa porque o **GC muda** sem aviso (P1).
3. `OutOfMemoryError`: a **JVM** reclama (heap, direct buffer, metaspace); exit 0 se tratado ou **3** com `ExitOnOutOfMemoryError` (medido). **`OOMKilled`**: o **kernel** matou o contêiner por exceder `limits.memory`: exit **137**, `OOMKilled=true` (medido: `-Xmx600m` num limite de 256m; memória nativa sem teto). **`Evicted`**: o **nó** sem memória removeu o pod (QoS, BestEffort primeiro).
4. Heap ≤ 70% × 768 = **537 MiB**; sobram **~231 MiB** para metaspace, *code cache*, pilhas (`threads × Xss`), buffers diretos, GC, JIT. `MaxDirectMemorySize` põe um **teto** na memória direta (vira erro tratável, cenário C, em vez de morte silenciosa, cenário D); `ExitOnOutOfMemoryError` faz a JVM **morrer rápido** (exit 3) em vez de ficar meio viva. O calculador do Paketo deu só **~184 MiB** de heap no mesmo limite (reserva para 250 *threads*).
5. **Pod**: unidade efêmera; **ReplicaSet**: mantém N pods; **Deployment**: gerencia ReplicaSets e a atualização; **Service**: nome/IP estável que balanceia entre pods **`Ready`** (seletor de rótulos → EndpointSlice). Pod solto não tem dono: ninguém o recria.
6. Sobe 1 novo antes de derrubar um antigo e **nunca** fica abaixo do desejado. Num *rollout* travado, os **2 antigos continuam atendendo** (o novo não entra no Service por falhar na *readiness*); `rollout status` termina com `exceeded its progress deadline` (600 s por padrão); `rollout undo` desfaz.
7. Nada: é só **declaração**; um **controlador** a executa. O ingress-nginx teve manutenção só **até março de 2026** (sem correções de segurança depois): risco para um componente de borda. A **Gateway API** entra quando precisar de papéis separados, divisão de tráfego, *timeouts* por rota; o Ingress é estável mas **congelado**. No kind, o **Cloud Provider KIND** atende ambos.
8. Base64 só codifica: quem lê o objeto decodifica. Controles: **RBAC** de menor privilégio, **criptografia em repouso** no etcd (KMS), **nunca no Git** em claro, **gerenciador externo + rotação**, `automountServiceAccountToken: false`. Arquivo + `0440` + `fsGroup` é mais controlável que variável de ambiente (`/proc/<pid>/environ`, herança por filhos, *dumps*).
9. **Startup**: terminou de subir? (falha → reinicia; suspende as outras); **liveness**: o processo vive? (falha → reinicia); **readiness**: pode receber tráfego? (falha → sai do Service, sem reiniciar). A *liveness* com banco causa **tempestade de reinícios** quando o banco cai. PayFlow: startup **5 s × 36 = 180 s**; liveness **10 s × 3 = 30 s** (+ `timeout` 2 s).
10. alvo = 150m; média = 300m; `ceil(3 × 300/150) = 6` (limitado por `maxReplicas: 6`). Se o gargalo é o **banco** (conexões em 100%), mais pods = mais conexões → **piora**; a CPU até está baixa (I/O-bound) e o HPA nem escala. **Helm 4:** `--atomic` → **`--rollback-on-failure`**, `--force` → `--force-replace` (as antigas ainda funcionam com aviso), `--server-side` é verdadeiro por padrão, `--wait` com estratégias.
</details>

## Parte 2: Entrega prática (tag `payflow-P7`)
O `payflow` ganha **imagem otimizada e tagueada**, **manifestos Kubernetes**, **chart Helm** e **documentação**, **sem** mudar o contrato HTTP `/api/v1`, o esquema (**nenhuma migração**) nem o dinheiro (uma transação).
```
payflow/
├── Dockerfile                    (J9) tag <versão>-<sha>; USER numérico; JAVA_TOOL_OPTIONS via ConfigMap                          (P7.01, P7.02)
├── scripts/build-image.sh        build + tag imutável + (no kind) kind load                                                       (P7.01)
├── deploy/
│   ├── kind-config.yaml          1 control-plane + 2 workers                                                                      (P7.07)
│   ├── manifests/
│   │   ├── 00-namespace.yaml     Pod Security "restricted"                                                                        (P7.04)
│   │   ├── 10-postgres.yaml      StatefulSet só para o laboratório                                                                (P7.03)
│   │   ├── 20-configmap.yaml     configuração NÃO secreta + JAVA_TOOL_OPTIONS                                                     (P7.04, P7.02)
│   │   ├── 30-deployment.yaml    probes (startup/liveness/readiness na 8081), requests/limits, preStop, securityContext, /tmp      (P7.05)
│   │   ├── 31-service.yaml       ClusterIP: só a porta http                                                                       (P7.03)
│   │   ├── 40-ingress.yaml       só "/" → Service; sem rota para a gestão                                                         (P7.04)
│   │   ├── 50-hpa.yaml           2–6 réplicas, 60% de CPU, behavior conservador                                                   (P7.06)
│   │   └── 60-pdb.yaml           minAvailable: 1                                                                                  (P7.05)
│   ├── chart/payflow/            Chart.yaml, values.yaml, templates/ (deployment, service, ingress, hpa, pdb, configmap)           (P7.08)
│   └── scripts/make-k8s-secrets.sh   Secrets a partir de arquivos fora do Git                                                     (P7.04)
├── docs/
│   ├── kubernetes.md             diagrama, roteiro do kind, tabela esperado × obtido (7 experimentos), orçamento de memória         (P7.07, P7.02)
│   └── runbooks/pod-reiniciando.md   árvore de decisão OOMKilled/CrashLoopBackOff                                                  (P7.09)
projeto/adrs/0019-containers-e-kubernetes-do-payflow.md   (já redigido nesta geração)
```
Critérios de pronto:
- [ ] **Contrato HTTP `/api/v1`, esquema e dinheiro inalterados**; a suíte de J5–P6 **continua verde**.
- [ ] Imagem com **tag imutável** (versão + SHA), **uid numérico**, **sem segredo** (`docker history`), `ENTRYPOINT` exec; o `.dockerignore` presente.
- [ ] **Orçamento de memória** documentado com números **medidos** (NMT/métricas); `MaxRAMPercentage`, coletor, `MaxDirectMemorySize` e `ExitOnOutOfMemoryError` definidos **de propósito**.
- [ ] `kubectl apply --dry-run=server -f deploy/manifests/` **sem erro**; `helm lint` e `helm template` **sem erro** e **equivalentes** aos manifestos.
- [ ] **Sondas**: startup/liveness/readiness na **8081**, *liveness* = só processo, *readiness* = banco; nenhuma no `health` agregado.
- [ ] `requests`/`limits` definidos (memória limite = requisição); QoS conhecida; `terminationGracePeriodSeconds ≥ preStop + timeout-per-shutdown-phase + folga`.
- [ ] Namespace com **Pod Security `restricted`** e o pod **admitido**; `securityContext` completo; **Secrets fora do Git**, montados como arquivos (`configtree`); **a 8081 não está** no Service nem no Ingress.
- [ ] No kind: **7 experimentos** executados e **esperado × obtido** registrado (cada divergência virou correção ou item em "Correções pendentes"); *rollout* sob tráfego **sem 5xx**; HPA sobe de 2 para > 2 sob carga; `OOMKilled` reproduzido e diagnosticado.
- [ ] `docs/kubernetes.md` e o *runbook* escritos; **ADR 0019** revisado; tag `payflow-P7`.

## Parte 3: Autoavaliação (baseada na matriz de competências)
Marque **0** (não sei), **1** (sei explicar), **2** (sei fazer com consulta), **3** (faço e ensino). Meta do Pleno: **≥ 2** em todos e **3** nos marcados com ⭐.

| Competência | Aula(s) | 0–3 |
|---|---|---|
| **Imagens otimizadas** (camadas, Buildpacks, Jib, tag imutável) | P7.01 | ⭐ |
| **JVM em container**: memória, CPU, coletor, `OOMKilled` × `OutOfMemoryError` | P7.02 | ⭐ |
| **Kubernetes**: pods, Deployment, Service, rótulos, *rollout* | P7.03 | ⭐ |
| **Ingress/Gateway API** e a situação do ecossistema | P7.04 | |
| **ConfigMaps e Secrets** (e o que base64 **não** é) | P7.04 | ⭐ |
| **Sondas** (startup/liveness/readiness) e desligamento gracioso | P7.05 | ⭐ |
| **`requests`/`limits`**, QoS, PDB, espalhamento | P7.05 | ⭐ |
| **HPA** (algoritmo, `behavior`, limite do banco) | P7.06 | ⭐ |
| **Operar** no kind: implantar, diagnosticar, desfazer | P7.07 | ⭐ |
| **Helm** (chart, values, release, Helm 4) e alternativas | P7.08 | |
| **Investigar** `OOMKilled` e escrever o *postmortem* | P7.09 | ⭐ |

**Se algum ⭐ ficou abaixo de 2:** refaça o `jvm-lab` (`./run-jvm-lab.sh`), as contas do HPA e escreva do zero o Deployment antes de seguir para a P8.

## Parte 4: Perguntas de entrevista (simulado rápido)
Responda em voz alta, 2 minutos cada: (1) "Como você deixa uma imagem Java pequena e rápida de atualizar?" (2) "A JVM respeita o limite do contêiner? O que ela escolhe sozinha?" (3) "O pod foi `OOMKilled`; como você investiga?" (4) "Liveness, readiness e startup: diferenças e erros comuns?" (5) "Como evitar erros durante um deploy?" (6) "`requests` × `limits` e as classes de QoS." (7) "Como o HPA decide e por que pode não escalar?" (8) "Secrets do Kubernetes são seguros?" (9) "Ingress ou Gateway API? E o ingress-nginx?" (10) "Helm, Kustomize ou manifestos puros?"

## ➡️ Próximo
[P8 — CI/CD e infraestrutura como código](../P8-cicd-iac/README.md)
