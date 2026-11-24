# Setup do ambiente

> Este guia é o **resumo e a lista de conferência**. A instalação passo a passo está nas aulas **J0.01** (terminal), **J0.02** (JDK e Maven), **J0.03** (IntelliJ), **J0.04** (Git) e **J0.05** (GitHub). As versões adotadas e a data em que foram conferidas estão em [`versoes.md`](versoes.md): **use as de lá**, não as deste arquivo.

## O que você vai precisar
| Item | Para quê | Onde se aprende | Como conferir |
|---|---|---|---|
| Linux (ou WSL2 no Windows, ou macOS) | Terminal, Docker | J0.01 | `uname -a` |
| **JDK 25 (LTS)** via [SDKMAN](https://sdkman.io/) | Compilar e rodar | J0.02 | `java -version` mostra 25 |
| **Maven 3.9+** | Build | J0.02 | `mvn -v` (o `mvn` usa o `JAVA_HOME`; se o `java` padrão do shell for outro, aponte `JAVA_HOME` para o JDK 25) |
| Git + conta no GitHub | Versionamento | J0.04, J0.05 | `git --version` |
| Docker + Compose | PostgreSQL, Redis, Kafka, containers (a partir da J3.03) | J3.03, J9 | `docker run --rm hello-world`, `docker compose version` |
| IntelliJ IDEA (Community ou Ultimate) | IDE | J0.03 | abre o projeto Maven |
| Node.js 22 (LTS) | só a J4 (JavaScript, ferramentas de front-end) | J4 | `node -v` |
| `curl` e `jq` | chamar a API e ler JSON (J2, J6) | J2, J6 | `curl --version`, `jq --version` |
| Bruno, Postman ou HTTPie | chamar APIs | J2 | opcional |
| Conta AWS | só no fim da J10 e nos módulos de nuvem do Pleno e do Sênior | J10 | 💸 **crie só quando chegar lá** e configure alerta de custo primeiro |

> **Atenção ao `java` padrão.** Muitas distribuições trazem o JDK 17 como padrão. Instalar o 25 **não** troca o padrão sozinho: confira com `java -version` **e** `mvn -v` (a linha `Java version`). Os exemplos do treinamento pedem o 25.

## Por sistema operacional
- **Linux:** gerenciador de pacotes da distribuição para Git e Docker; SDKMAN para JDK e Maven.
- **Windows:** instalar WSL2 com Ubuntu, trabalhar **dentro** do WSL (não em `C:\`), Docker Desktop com integração WSL.
- **macOS:** Homebrew para Git; Docker Desktop; SDKMAN para JDK.

## Verificação rápida
```bash
java -version && mvn -v && git --version && docker run --rm hello-world
```
Se algum dos quatro falhar, volte à aula da tabela acima antes de seguir.
