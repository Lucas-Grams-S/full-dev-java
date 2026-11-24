# Setup do ambiente

> Rascunho do guia. A versão passo a passo e testada será escrita na aula J0.02, quando as versões forem reconferidas. Versões adotadas: ver `versoes.md`.

## O que você vai precisar
| Item | Para quê | Status na sua máquina (2026-11-12) |
|---|---|---|
| Linux (ou WSL2 no Windows) | Terminal e Docker | Linux ✔ |
| JDK 25 (LTS) via [SDKMAN](https://sdkman.io/) | Compilar e rodar | ⚠️ só há OpenJDK 17 → instalar o 25 |
| Maven 3.9+ | Build | 3.9.12 ✔ |
| Git + conta no GitHub | Versionamento | verificar |
| Docker + Compose | PostgreSQL, Kafka, etc. | 29.9.2 ✔ |
| IntelliJ IDEA (Community ou Ultimate) | IDE | verificar |
| Bruno, Postman ou HTTPie | Chamar APIs | instalar na J2 |
| Conta AWS (só no J10) | Nuvem | 💸 criar só quando chegar lá |

## Por sistema operacional
- **Linux:** gerenciador de pacotes da distribuição para Git e Docker; SDKMAN para JDK e Maven.
- **Windows:** instalar WSL2 com Ubuntu, trabalhar dentro do WSL (não em `C:\`), Docker Desktop com integração WSL.
- **macOS:** Homebrew para Git e Docker Desktop; SDKMAN para JDK.

## Instalar o JDK 25 com SDKMAN
```bash
curl -s "https://get.sdkman.io" | bash        # instala o SDKMAN (confira o comando em sdkman.io antes de rodar)
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk list java                                  # veja o identificador exato do Temurin 25
sdk install java <identificador-do-25>
java -version                                  # deve mostrar 25
```
O identificador exato muda a cada patch; **use o que `sdk list java` mostrar**.

## Verificação rápida
```bash
java -version && mvn -v && git --version && docker run --rm hello-world
```
