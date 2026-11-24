# J0.CP — Checkpoint do módulo J0

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J0.01 a J0.06

## Parte 1 — Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. O que significa `-rwxr-xr--` e qual o valor numérico dessas permissões?
2. Qual a diferença entre `>` e `>>`? E o que faz `|`?
3. Qual a diferença entre JDK, JRE e JVM?
4. Cite 5 fases do ciclo de vida padrão do Maven, em ordem, e diga o que acontece ao rodar `mvn package`.
5. O que é uma dependência transitiva e qual comando mostra a árvore delas?
6. No debugger do IntelliJ, qual a diferença entre *Step Over* e *Step Into*? Para que serve um breakpoint condicional?
7. Quais são as três áreas do Git e qual comando move o conteúdo entre elas?
8. Quando usar `merge` e quando `rebase`? Qual a regra de ouro do rebase?
9. Escreva uma mensagem de commit correta (Conventional Commits) para: "corrige o cálculo de taxa que arredondava errado".
10. Ao ler um stack trace com 40 linhas de Spring e 2 do seu pacote, por onde você começa?

<details><summary>Gabarito</summary>

1. Arquivo; dono `rwx`, grupo `r-x`, outros `r--`; `754`.
2. `>` sobrescreve o arquivo com a saída; `>>` acrescenta; `|` liga a saída de um comando à entrada do próximo.
3. JVM executa bytecode; JRE = JVM + bibliotecas; JDK = JRE + ferramentas de desenvolvimento (`javac`).
4. Por exemplo: `validate`, `compile`, `test`, `package`, `verify` (há outras, até `deploy`). `mvn package` executa todas as fases até `package`, incluindo os testes, e gera o `.jar` em `target/`.
5. Dependência das dependências, trazida automaticamente; `mvn dependency:tree`.
6. *Step Over* executa a linha inteira sem entrar no método; *Step Into* entra no método chamado. Breakpoint condicional só pausa quando a condição for verdadeira.
7. Working tree, staging e repositório. `git add` (working → staging), `git commit` (staging → repositório), `git restore`/`git switch` (repositório → working).
8. Merge para juntar preservando o histórico (e em branches compartilhadas); rebase para linearizar *sua* branch local. Nunca fazer rebase de commits já publicados que outras pessoas usam.
9. `fix: correct fee rounding` (a descrição em inglês, imperativo).
10. Ignoro as linhas do framework e começo na primeira linha do meu pacote, lendo antes o tipo e a mensagem da exceção.
</details>

## Parte 2 — Entrega prática (tag `payflow-J0`)
Critérios de pronto:
- [ ] `java -version` mostra **25** e `mvn -v` usa esse JDK.
- [ ] Repositório `payflow` no GitHub, com `.gitignore` (Java/Maven/IntelliJ/.env), `README.md` e commit inicial `chore: initial commit`.
- [ ] Pelo menos **um PR** mesclado com descrição "O quê / Por quê / Como testar".
- [ ] Todos os commits seguem Conventional Commits em inglês.
- [ ] `docs/debugging.md` com o modelo de pedido de ajuda (J0.06).
- [ ] Nenhum arquivo de `target/`, `.idea/` ou segredo no repositório (`git ls-files` para conferir).
- [ ] Tag criada e enviada:
```bash
git tag payflow-J0
git push origin payflow-J0
```

## Parte 3 — Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Navego e manipulo arquivos no terminal | ☐ | ☐ | ☐ |
| Instalo e troco versões do JDK | ☐ | ☐ | ☐ |
| Crio e rodo um projeto Maven | ☐ | ☐ | ☐ |
| Depuro com breakpoints no IntelliJ | ☐ | ☐ | ☐ |
| Uso branches, merge e resolvo conflitos | ☐ | ☐ | ☐ |
| Abro e reviso PR com commits padronizados | ☐ | ☐ | ☐ |
| Leio stack traces e peço ajuda com contexto | ☐ | ☐ | ☐ |

**Regra:** se ficou com mais de duas linhas em 0, repita as aulas correspondentes antes de seguir para o [J1](../J1-java-logica/README.md). Atualize `00-guia/progresso.md` e o `cronograma.md`.
