# Laboratório do P5

Tudo foi **executado** em Linux, JDK **25.0.5** (`/usr/lib/jvm/java-25-openjdk-amd64`), Maven **3.9.12**, 12 núcleos **compartilhados** (a *load average* varia: tempos oscilam). **Sem Docker.** Os "serviços" externos são servidores falsos (`com.sun.net.httpserver`). As saídas reais estão em `out-*.txt` e são **o que as aulas mostram**.

## `integration-lab/` (Maven multimódulo; P5.03 a P5.10)
Spring Boot **4.1.2** (pai), **Resilience4j 2.4.0** (`resilience4j-spring-boot4`), **Spring Cloud 2025.1.4** (só no `gateway-lab`), **Spring gRPC/grpc-java/protobuf** do BOM do Boot (1.1.2 / 1.84.1 / 4.36.2), **Pact JVM 4.7.7**.

```bash
cd integration-lab
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./run-all.sh   # roda `mvn test` e regrava os out-*.txt
```

| Módulo | Aulas | Testes | O que prova |
|---|---|---|---|
| `resilience-lab` | P5.07, P5.08, P5.10 | 17 | `TimeoutsTest` (padrão do JDK **sem** timeout de leitura), `RetryTest` (backoff, **27** chamadas, 4xx, POST duplicado), `CircuitBreakerTest` (3 estados, lentidão, 404 abre no padrão), `BulkheadRateLimiterTest` (5 de 20; 10 de 25; `TimeLimiter` não cancela), `AnnotationsOrderTest` (ordem, fallback na anotação errada), `CascadeFailureTest` (Tomcat de 10 *threads*: `/ping` para sem proteção) |
| `gateway-lab` | P5.05 | 6 | roteamento por caminho, `StripPrefix`, cabeçalhos, `X-Forwarded-*` só com `trusted-proxies`, destino lento/fora do ar → **500**, `Retry` de POST, `CircuitBreaker` com `statusCodes` e `fallbackUri` |
| `grpc-lab` | P5.03, P5.04 | 10 | `.proto` + serviço + cliente, tamanho e latência REST × gRPC, `DEADLINE_EXCEEDED`, `INVALID_ARGUMENT`, `UNAVAILABLE`, sem prazo = espera; evolução do `.proto` (campo novo, número reutilizado, enum novo) |
| `contract-lab` | P5.04, P5.09 | 8 | Pact consumidor (gera o pacto) e provedor (verifica); `EventCompatibilityTest` (Jackson 3: campo novo, renomeado, enum novo, dinheiro como número) |

`run-all.sh` também roda a **verificação do provedor "quebrado"** (`-Dlab.break=additive|rename|type`) e grava `out-contract-break.txt`.

### Saídas gravadas
`out-suite-resumo.txt`, `out-T1.txt` (timeouts), `out-T2.txt` (retry), `out-T3.txt` (circuit breaker), `out-T4.txt` (bulkhead, rate limiter, TimeLimiter), `out-T5.txt` (gateway), `out-T6.txt` (compatibilidade de JSON), `out-T7.txt` (anotações), `out-T10.txt` (cascata), `out-T3G.txt` (gRPC), `out-T4G.txt` (evolução do `.proto`), `out-contract-break.txt`, `out-pact.json` (o pacto gerado).

### Surpresas que o laboratório achou
Ver o registro do P5 em `00-guia/progresso.md` (lista numerada).

### Limpeza
Não há contêineres, redes, volumes nem imagens. `target/` e `out-suite.raw` são removidos ao fim.
