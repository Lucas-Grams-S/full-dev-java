# P7 — Containers e Kubernetes

> 🔵 Pleno · ⏱️ 15h (10 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **parcialmente executado: imagens (Dockerfile × Buildpacks × Jib) e JVM sob limites medidas no Docker; o Kubernetes (kind, kubectl, helm) NÃO foi executado (o ambiente não permitiu instalá-los); manifestos e chart escritos por leitura, conferidos na documentação oficial; testes práticos no fim do treinamento, por decisão do aluno**

## 🎯 Objetivo do módulo
Rodar o PayFlow em Kubernetes com configuração segura: **imagens** otimizadas (camadas, Buildpacks, Jib), a **JVM em contêiner** (memória, CPU, coletor, `OOMKilled`), os objetos do Kubernetes (**Pod, Deployment, Service**), **Ingress** (e a Gateway API), **ConfigMaps e Secrets**, **sondas** e **requests/limits**, **HPA**, o **deploy no kind**, **Helm** e o diagnóstico de um **`OOMKilled`**.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [P7.01](P7.01-imagens-otimizadas-layered-jars-buildpacks-e-jib.md) | Imagens otimizadas: layered jars, Buildpacks e Jib | 1h30 |
| [P7.02](P7.02-jvm-em-container-memoria-e-cpu.md) | JVM em container: memória e CPU | 1h30 |
| [P7.03](P7.03-kubernetes-pods-deployments-e-services.md) | Kubernetes: pods, deployments e services | 1h30 |
| [P7.04](P7.04-ingress-configmaps-e-secrets.md) | Ingress, ConfigMaps e Secrets | 1h30 |
| [P7.05](P7.05-probes-e-requests-limits.md) | Probes e requests/limits | 1h30 |
| [P7.06](P7.06-hpa-e-escala.md) | HPA e escala | 1h30 |
| [P7.07](P7.07-deploy-do-payflow-no-kind.md) | Deploy do PayFlow no kind | 1h30 |
| [P7.08](P7.08-helm-visao-geral.md) | Helm (visão geral) | 1h30 |
| [P7.09](P7.09-estudo-de-caso-o-pod-morto-por-oomkilled.md) | 🏗️ Estudo de caso: o pod morto por OOMKilled | 1h30 |
| [P7.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório em [`lab/`](lab/README.md): `lab/k8s-lab/jvm-lab/` (**executado**: JVM sob limites e `OOMKilled`), `lab/k8s-lab/manifests/` e `chart/` (**não executados**: YAML validado e conferido na documentação) e `scripts/make-k8s-secrets.sh` (`bash -n`).

> ⚠️ **Aviso de verificação (leia antes de estudar):** **foi executado** (Linux, Docker 29.9.2, Maven 3.9.12, JDK 25.0.5, Spring Boot 4.1.2, Jib 3.5.2, Paketo): as **três imagens** do PayFlow Lite e o **laboratório da JVM** (as saídas mostradas nas aulas P7.01, P7.02 e P7.09 são as reais). **Não foi executado:** **qualquer comando de Kubernetes** (`kind`, `kubectl`, `helm`: o ambiente **negou** instalá-los), o `metrics-server`, o HPA, o Ingress, o Cloud Provider KIND, o `helm lint/template`, o `kubectl debug`; o **PayFlow do aluno** (revisado por leitura). As **saídas de `kubectl`** mostradas são o **formato típico**, marcadas como tal. A **lista "a validar no fim do treinamento"** está em `00-guia/progresso.md`.

> **Avisos de ordem:** a P7.01 retoma a J9.02 (camadas, usuário não root); a P7.02 retoma a P1 (GC) e a J9.01 (`--memory`/`--cpus`); a P7.03 cumpre a promessa da P5.06 (o `Service` no lugar de Eureka/Config Server); a P7.05 cumpre a da P6.06 (sondas no Kubernetes e o atraso antes do `SIGTERM`); a P7.06 usa o *pool* da P2.06; a P7.08 e a P8 (pipeline de implantação) se complementam; o escalonamento de **nós** e o banco gerenciado ficam para a **P9**; o **RBAC**, a criptografia de Secrets e o *supply chain* aprofundam-se na **P10**.

## 🏗️ Entrega no PayFlow
**Imagem** com tag imutável, **manifestos** (namespace com Pod Security `restricted`, banco de laboratório, ConfigMap, Deployment com sondas/recursos/`preStop`/`securityContext`, Service, Ingress, HPA, PDB), **chart Helm**, **Secrets fora do Git** montados como arquivos, `docs/kubernetes.md`, *runbook* de pod reiniciando e **ADR 0019**. Tag `payflow-P7`. **Nenhuma migração nova** (a próxima livre continua V12).

## ➡️ Próximo
[P8 — CI/CD e infraestrutura como código](../P8-cicd-iac/README.md)
