# S3 — Dados em escala

> 🟣 Sênior · ⏱️ 15h (10 sessões de 1h30) · Status: ✅ concluído (2026-11-14) — **validado na execução final (2026-11-24); veja o bloco abaixo**


> ✅ **Validação final (2026-11-24, PayFlow reconstruído em `~/docs/payflow`, tag `payflow-S3`).** **Executado:** `run-all.sh` completo (PostgreSQL 18, 200 mil usuários e **3 milhões de lançamentos**, ~15 min, numa cópia): **16 de 22 saídas idênticas (sem números)** e 6 que diferem só por pids, LSN, *hashes* de dados aleatórios e tempos (fila de *locks*, partições, arquivo, *logical decoding*, BRIN, migração do telefone); a migração de `phone` e o mutante reproduzem. **Corrigido nas aulas:** nenhuma divergência de conteúdo. **Ainda não executado:** OpenSearch, Debezium, RDS e S3 reais, as migrações **no PayFlow** (V18–V24: o SQL de cada uma só existe parcialmente nas aulas) e a réplica real com o `reporting` do PayFlow. Os avisos "não executado" / "revisado por leitura" nas aulas descrevem a escrita original.

## 🎯 Objetivo do módulo
**Evoluir dados de produção sem parar o sistema**: planejar e executar **migrações com *expand/contract*** (conhecendo o *lock* de cada comando e a **fila de *locks***), **particionar** tabelas que só crescem (e **converter a existente** sem parar), usar **réplicas** com **roteamento explícito** e **consistência de sessão**, **arquivar** com **restauração provada**, decidir **busca** (PostgreSQL × OpenSearch) **com medição**, entender **CDC** (e o perigo do ***slot***), **escolher armazenamento por padrão de acesso** e organizar **analytics** sem competir com o caminho do dinheiro. O fio é **medir e registrar gatilhos**: cada técnica aparece com **números do laboratório** e **regras de revisão**.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [S3.01](S3.01-migracoes-sem-downtime-expand-contract.md) | Migrações sem downtime (expand/contract) | 1h30 |
| [S3.02](S3.02-particionamento-no-postgresql.md) | Particionamento no PostgreSQL | 1h30 |
| [S3.03](S3.03-replicas-de-leitura-e-roteamento.md) | Réplicas de leitura e roteamento | 1h30 |
| [S3.04](S3.04-arquivamento-de-dados.md) | Arquivamento de dados | 1h30 |
| [S3.05](S3.05-opensearch-elasticsearch.md) | OpenSearch/Elasticsearch | 1h30 |
| [S3.06](S3.06-cdc-com-debezium.md) | CDC com Debezium | 1h30 |
| [S3.07](S3.07-escolha-de-banco-por-caso-de-uso.md) | Escolha de banco por caso de uso | 1h30 |
| [S3.08](S3.08-nocoes-de-dados-para-analytics.md) | Noções de dados para analytics | 1h30 |
| [S3.09](S3.09-estudo-de-caso-migrar-uma-coluna-com-o-sistema-no-ar.md) | Estudo de caso: migrar uma coluna com o sistema no ar | 1h30 |
| [S3.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Laboratório pronto em [`lab/`](lab/README.md): **scripts** (`run-s301.sh` … `run-s309-generated.sh`, orquestrados por `run-all.sh`) sobre **um PostgreSQL 18 em Docker** com o esquema do PayFlow simplificado (**200 mil usuários e contas, 3 milhões de lançamentos em 24 meses, 1 milhão de transferências**), mais dois programas Java (`RoutingLab.java`, com **spring-jdbc 7.0.10**; `PhoneNormalizer.java`, Java puro) e as **saídas reais** (`out-s30*.txt`).

> ⚠️ **Aviso de verificação (leia antes de estudar):** o **laboratório foi executado de verdade** (PostgreSQL **18**, JDK **25.0.5**, Linux, **12 núcleos, máquina compartilhada**) e **as saídas das aulas são as reais**; os **milissegundos mudam a cada execução** (as aulas citam **ordens de grandeza** e mostram a saída). **São modelos e programas pequenos, não o PayFlow:** tabelas **simplificadas**, dados **sintéticos**, **uma** máquina. **Não foi executado:** **OpenSearch/Elasticsearch** (JSON e consultas da S3.05 são **por leitura**), **Debezium/Kafka Connect/Kafka** (S3.06: o **mecanismo** do *logical decoding* **foi** executado; o conector, **não**), **RDS** e réplicas gerenciadas, **S3/Object Lock/Athena**, **armazéns colunares**, MongoDB/DynamoDB, Flyway e Spring Boot **sobre o PayFlow**. Onde a aula cita um **número de outro produto**, ele vem de **documentação com link** (e **nenhum** *benchmark* entre produtos foi inventado). **Prazos de retenção e cenários de incidente são PREMISSAS/SINTÉTICOS** e estão rotulados.

> **Avisos de ordem:** a S3.01 retoma **Flyway** (J3.12), **`CONCURRENTLY`** e a **decisão 97** (P2.05) e usa o método da S2 (linha de base, medir); a S3.02 **cumpre** a decisão 186 (S1.03: "particionar a **tabela** antes de *shardar*") e **estende** o ledger append-only; a S3.03 **endurece** a decisão 171 (P9.08) e usa o `X-Min-Seq` da S1.05; a S3.04 usa o invariante da S1.05 e a conciliação da S1.12; a S3.05/S3.06 usam o Outbox (P3.09), a idempotência (S1.08) e os esquemas (P5.04); a S3.08 retoma a visão materializada da P2.11; a S3.09 **junta** S3.01 + S3.02 + S1.08.

## 🏗️ Entrega no PayFlow
**Tag `payflow-S3`** (por leitura): **migrações V18–V24** (rascunhos: expandir `users.phone_e164`, **preparar e trocar** o ledger para **particionado** `V19`/`V19_1`, manifesto de arquivo e **saldos de fechamento**, publicação do CDC, resumo diário, contração da coluna), **procedimentos** em `docs/dados/*` (migrações, particionamento, réplicas, arquivamento, busca, CDC, armazenamento, analytics, migração de `phone`), **roteamento explícito** do módulo `reporting` (segundo `DataSource` + *gate* + teste de arquitetura), **`PhoneNormalizer`** com teste de concordância e **ADR 0026**. **Nada foi compilado nem executado no PayFlow.**

> **Migrações:** a **`V18`** é a primeira livre (V13–V17 foram reservadas pela S1, por leitura); o contrato HTTP `/api/v1` **não muda**.

## ➡️ Próximo
[S4 — Confiabilidade e operação (SRE)](../S4-confiabilidade-sre/README.md)
