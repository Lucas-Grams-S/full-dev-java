# Bia Costa (pessoa fictícia; modelo de currículo)
Desenvolvedora Java backend (transição de carreira) · Recife · github.com/bia-exemplo · linkedin.com/in/bia-exemplo · bia@example.com

## Resumo
Desenvolvedora Java em transição de carreira. Construí do zero o PayFlow, uma carteira digital de estudo com Spring Boot 4.1 e PostgreSQL 18: transações, mensageria com Outbox, segurança com OAuth2 e deploy em contêiner. Antes, 4 anos em atendimento financeiro.

## Projetos
### PayFlow — carteira digital de estudo · github.com/bia-exemplo/payflow
- Implementei transferências entre contas numa transação ACID, com travas em ordem fixa e testes contra PostgreSQL 18 real (Testcontainers).
- Escrevi 47 testes de integração de mensageria contra Kafka e RabbitMQ reais (Outbox, consumidor idempotente, reentrega).
- Integrei o login OAuth2 com Keycloak e escrevi 54 testes (31 contra Keycloak 26.8.2 e PostgreSQL 18 reais).
- Medi em laboratório que 100% de cobertura matava só 6 de 18 mutantes (PIT); a suíte reescrita matou 18 de 18.
- Estimei o custo em US$ 887,89/mês em us-east-1 com 100 preços datados da AWS Price List (premissas no repositório).
- Escrevi 33 ADRs e uma RFC com prazo e decisor para a próxima funcionalidade (transferências recorrentes).

## Experiência
### Banco Exemplo (fictício) — Analista de atendimento (03/2021 – 08/2025)
- Automatizei uma planilha de conciliação com fórmulas e macros; o fechamento diário caiu de 2 h para 30 min (medido pela equipe).
- Treinei 6 colegas novos no processo de contestação de cobranças.

## Competências
Java 25, Spring Boot, JPA/Hibernate, PostgreSQL, SQL, Testcontainers, Kafka, RabbitMQ, Docker, OAuth2/Keycloak, Git

## Formação
Treinamento Java Júnior → Sênior (projeto PayFlow, 2026) · Inglês: leitura técnica fluente
