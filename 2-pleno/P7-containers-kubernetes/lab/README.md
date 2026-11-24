# Laboratório do P7

> ⚠️ **Estado:** só o `jvm-lab` foi **executado** (Linux, JDK 25.0.5, Docker 29.9.2, 12 núcleos compartilhados). `manifests/`, `chart/` e `kind-config.yaml` tiveram a **sintaxe YAML validada** (PyYAML) e foram conferidos na documentação, **sem cluster** (o ambiente não permitiu instalar `kind`, `kubectl` nem `helm`). Os templates do chart **não** passaram por `helm lint`.

## `k8s-lab/jvm-lab/` (P7.02, P7.09): executado
```bash
cd lab/k8s-lab/jvm-lab && JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./run-jvm-lab.sh   # ~1 min; grava out-jvm.txt
```
Usa a imagem `eclipse-temurin:25-jre` (já local ou baixada) e `Probe.java`: heap/coletor/CPUs por limite e quatro formas de estourar a memória. Saída real em `out-jvm.txt`. Limpeza: o script remove os contêineres `p7-*` e o diretório temporário.

## `k8s-lab/manifests/`, `chart/`, `kind-config.yaml`, `scripts/` (P7.03 a P7.08): não executados
Ver o roteiro e a tabela **esperado × obtido** na P7.07. Ordem: `kind create cluster --name payflow --config kind-config.yaml` → `docker build -t payflow:p7 .` + `kind load docker-image payflow:p7 --name payflow` → `scripts/make-k8s-secrets.sh` → `manifests/10…60`. O chart: `helm lint chart/payflow && helm template payflow chart/payflow -n payflow`.

## Imagens (P7.01): medidas, sem arquivos no laboratório
Construídas a partir do PayFlow Lite (`1-junior/J9-docker/lab/docker-lab`) em um diretório temporário: `docker build` (357 MB), `mvn …jib-maven-plugin:3.5.2:dockerBuild -Djib.from.image=eclipse-temurin:25-jre -Djib.container.user=10001` (527 MB) e `mvn spring-boot:build-image` (686 MB). Imagens `p7-lite:*` **removidas** ao final.

> **Atenção (segredos):** `make-k8s-secrets.sh` gera valores **aleatórios** em `secrets/` (que **não** vai para o Git); nada de senha fixa nos manifestos.
