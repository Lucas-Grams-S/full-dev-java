# syntax=docker/dockerfile:1

# ---- Estagio 1: build (JDK + Maven). Nada daqui vai para a imagem final. ----
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /workspace

# 1) So o pom primeiro: a camada com as dependencias so e refeita quando o pom muda.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:resolve dependency:resolve-plugins

# 2) Depois o codigo. Testes ficam para o CI (precisam de Docker, entao nao rodam dentro do build).
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
 && mv target/payflow-lite-*.jar target/app.jar \
 && java -Djarmode=tools -jar target/app.jar extract --layers --destination target/extracted

# ---- Estagio 2: execucao (so o JRE) ----
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

# Usuario sem privilegios (uid fixo, sem shell de login)
RUN addgroup -S -g 10001 payflow \
 && adduser -S -u 10001 -G payflow -H -s /sbin/nologin payflow

# Da camada que menos muda para a que mais muda
COPY --from=build --chown=payflow:payflow /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=payflow:payflow /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=payflow:payflow /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=payflow:payflow /workspace/target/extracted/application/ ./

USER payflow
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
