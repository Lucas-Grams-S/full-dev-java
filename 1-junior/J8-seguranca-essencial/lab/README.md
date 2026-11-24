# Laboratórios da J8

Tudo aqui foi **executado** (Linux, OpenJDK 25.0.5, Maven 3.9.12, Spring Boot 4.1.2, Spring Security 7.1.2). Use o JDK 25: `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`.

| Item | O que é | Como rodar |
|---|---|---|
| `security-lab/` | A configuração de segurança do PayFlow (duas cadeias, JWT, sessão, CSRF, papéis, limitador, auditoria) com **usuários em memória** (senha de todos: `Payflow#Dev2026`), mais os testes que as aulas citam (**81 testes**) | `cd security-lab && mvn test` |
| `default-security-lab/` | O que o Spring Boot faz só com o starter, a "ponte" `permitAll`, a ordem das regras, `@PreAuthorize` e seus buracos (**8 testes**) | `cd default-security-lab && mvn test` |
| `PasswordLab.java` | SHA-256 × BCrypt × `DelegatingPasswordEncoder`, custo, limite de 72 bytes | veja o comando no cabeçalho do arquivo (usa o classpath do `security-lab`) |
| `InjectionLab.java` | SQL injection de verdade em um H2 em memória (só neste exemplo) | veja o comando no cabeçalho do arquivo (baixa o jar do H2) |
| `check-secrets.sh` | Guarda barata contra segredo no Git (`git grep`); copie para `scripts/` do seu repositório | `bash scripts/check-secrets.sh` |
| `osv-check.sh` | Consulta o OSV por `grupo:artefato:versão` (só as coordenadas vão para a rede) | `bash osv-check.sh org.apache.logging.log4j:log4j-core:2.14.1` |

O `security-lab` **não** usa banco: `InMemoryData` faz o papel do PostgreSQL com o seed da J3.12. As classes de `com.payflow.security`, `com.payflow.web.CurrentUser*`, `WebConfig`, `AuthController`, os DTOs de login e o `ApiSecurityErrors` são **as mesmas** que as aulas mandam criar no PayFlow; as de `domain`, `repository` e os controllers de leitura são versões reduzidas.
