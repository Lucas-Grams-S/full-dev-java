# O que o mercado pede

> Pesquisa feita em **2026-11-12**. Limitação honesta: as fontes foram agregadores de vagas (Indeed Brasil, InfoJobs, Greenhouse, GeekHunter) e guias de roadmap. As páginas não trazem data de publicação confirmada, então isto é um **padrão qualitativo**, não uma estatística. Nenhum percentual aqui é de vaga; os números de pesquisa vêm de fontes citadas.

## Padrão por nível (vagas brasileiras)

| Nível | O que aparece com frequência | Fonte |
|---|---|---|
| 🟢 Júnior | Java (8+), Spring Boot, SQL básico, Git, APIs REST, JPA/Hibernate. Algumas aceitam experiência de faculdade ou de projetos pessoais. | [Indeed: júnior Java](https://br.indeed.com/q-junior-software-developer-java-vagas.html?start=180), [Indeed: Java Spring júnior](https://br.indeed.com/q-junior-java-spring-developer-vagas.html?start=30) |
| 🔵 Pleno | Java 21, Spring Boot, testes automatizados, **Kafka/RabbitMQ**, **Docker** (quase sempre) e **Kubernetes**, **AWS** (EC2, ECS, EKS, Lambda, S3, RDS) como diferencial ou exigência. | [GeekHunter: Java pleno](https://www.geekhunter.com.br/en/nava-technology-for-business-1/jobs/desenvolvedor-java-pleno-61), [Indeed: Java](https://br.indeed.com/q-desenvolvedor-java-vagas.html?start=120) |
| 🟣 Sênior | Java 21+, Spring Security, microsserviços, Kafka avançado, Kubernetes com CI/CD, AWS/GCP, observabilidade (logs, métricas, traces), OAuth2/JWT. "System design" quase não aparece literalmente; aparece como "arquitetura escalável", resiliência e decisões arquiteturais. | [Indeed: Java backend](https://br.indeed.com/q-java-backend-vagas.html?start=280), [Remotive: Senior Java/Kafka](https://remotive.com/remote/jobs/software-development/senior-java-kafka-developer-5761928) |

## Pesquisas e roadmaps consultados
- [roadmap.sh/java](https://roadmap.sh/java): núcleo Java, JVM e tuning, concorrência, memória, REST, Spring Boot, Hibernate, bancos SQL, testes. (A página do Spring Boot, roadmap.sh/spring-boot, não expôs a lista de tópicos na captura.)
- [JetBrains — State of Developer Ecosystem 2025](https://devecosystem-2025.jetbrains.com/) (24.534 respondentes, abril a junho de 2025) e a página [State of Java 2025](https://lp.jetbrains.com/the-state-of-java-2025): conferir o percentual de uso do Spring direto nos gráficos antes de citar. ⚠️ Não citamos número aqui porque a busca não o trouxe.
- Guias de roadmap de terceiros (Scaler, JavaGuides): tratam Java 17/21/25, Spring Boot 3+, Hibernate 6+ como pilha típica corporativa. São material de blog, não requisito de empregador.

## O que isso muda na trilha (acréscimos e ajustes em relação ao prompt)

| # | Acréscimo | Onde entra | Por quê |
|---|---|---|---|
| 1 | **JUnit 6** em vez de JUnit 5 | J1, J7 | É a geração atual; vagas ainda citam JUnit 5, então a aula mostra as diferenças. |
| 2 | **Java 8/11/17 legado**: o que muda até a LTS atual | J1.16, P1.11 | Muitas vagas pedem "Java 8+". O aluno encontrará código antigo. |
| 3 | **Lombok**: o que é, riscos e alternativa com records | J5.07, J1.14 | Aparece em quase todo código corporativo Java. |
| 4 | **Spring Batch** (visão) | P3 (aula de `@Scheduled`) | Comum em bancos e fintechs brasileiras. |
| 5 | **GraphQL** e **gRPC** (visão) | P5.03 | Citados em vagas pleno/sênior. |
| 6 | **Jenkins** (visão, para legado) | P8.08 | Ainda é comum em empresas grandes. |
| 7 | **SonarQube** na análise estática | J7.08, P8.02 | Padrão de qualidade em muitas empresas. |
| 8 | **Oracle e MySQL**: diferenças de dialeto SQL | J3.12 e P2 | Muitas vagas citam Oracle; o curso usa PostgreSQL. |
| 9 | **Caminho opcional de certificação** (AWS Cloud Practitioner / Developer, Oracle Java) | J10, P9, S6 | Uma vaga pesquisada pede certificação de nuvem. |
| 10 | **Inglês técnico**: ler docs, RFCs e escrever commits | Todo o curso, `como-estudar.md` | Pré-requisito para vagas internacionais. |
| 11 | **Quarkus e Micronaut** em uma nota de comparação | P5.01 | São alternativas citadas em levantamentos do ecossistema. |
| 12 | **Spring AI** (visão) | S7.07 | Já previsto; mantido por relevância atual. |

Nada foi removido da trilha base do prompt.
