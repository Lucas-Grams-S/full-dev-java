# J4 — Front-end essencial para dev backend

> 🟢 Júnior · ⏱️ 13.5h (9 sessões de 1h30) · Status: ✅ concluído (aulas geradas em 2026-11-13)

## 🎯 Objetivo do módulo
Construir telas simples que consomem a API e ler código Angular/React sem medo.

## 📚 Aulas
| Aula | Título | Tempo |
|---|---|---|
| [J4.01](J4.01-html-semantico-formularios.md) | HTML semântico e formulários | 1h30 |
| [J4.02](J4.02-css-box-model-flexbox.md) | CSS: box model e flexbox | 1h30 |
| [J4.03](J4.03-css-grid-responsividade.md) | CSS: grid e responsividade | 1h30 |
| [J4.04](J4.04-javascript-dom-eventos.md) | JavaScript essencial: DOM e eventos | 1h30 |
| [J4.05](J4.05-promises-async-await-fetch.md) | Promises, `async`/`await` e `fetch` | 1h30 |
| [J4.06](J4.06-thymeleaf-renderizacao-no-servidor.md) | Thymeleaf: renderização no servidor | 1h30 |
| [J4.07](J4.07-angular-react-typescript-visao-geral.md) | Visão de Angular, React e TypeScript | 1h30 |
| [J4.08](J4.08-acessibilidade-e-revisao-do-painel.md) | Acessibilidade básica e revisão do painel | 1h30 |
| [J4.CP](checkpoint.md) | Checkpoint do módulo (quiz de 10 questões, entrega e autoavaliação) | 1h30 |

> Ambiente de referência: JDK 25, Maven 3.9.12, PostgreSQL 18 (Docker `postgres:18`), Thymeleaf 3.1.7.RELEASE, slf4j-simple 2.0.20 (ver `00-guia/versoes.md`). Para os laboratórios de JavaScript: **Node 22+** e um navegador Chrome/Chromium (axe-core 4.14.0 e puppeteer-core 25.17.0 só nos laboratórios de teste). As aulas J4.01 a J4.05 usam **protótipos estáticos** em `web-prototype/` (abertos direto no navegador ou servidos pelo `ApiStub`); a J4.06 transforma os protótipos em templates Thymeleaf. As aulas J4.06 em diante pressupõem o container `payflow-pg` no ar (J3.03).
>
> **Aviso de ordem:** a J4.06 usa `switch` com *pattern matching* (`case String s`, `case null`) no `JsonWriter`, visto só em forma de visão na J1.14; está sinalizado na aula. A J4.07 usa npm/Node sem curso próprio (os comandos são explicados na aula).

## 🏗️ Entrega no PayFlow
Painel web do PayFlow (login provisório, contas, extrato, depósito) com **Thymeleaf** sobre o servidor JDK e o JDBC da J3, e um trecho de JavaScript com `fetch` (o link "Mais antigos" do extrato); 21 testes. Tag `payflow-J4`.

## ➡️ Próximo
[J5 — Spring Framework e Spring Boot](../J5-spring-boot/README.md)
