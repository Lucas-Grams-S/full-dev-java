# J2.CP — Checkpoint do módulo J2

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J2.01 a J2.06

## Parte 1 — Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Qual a diferença entre IP, porta e DNS? O que significa `127.0.0.1`?
2. `Connection refused`, `Address already in use` e `UnknownHostException`: o que cada um indica?
3. O que o TCP garante e o que ele não garante?
4. Monte uma requisição HTTP/1.1 `POST` com corpo JSON. Quais headers são necessários e qual é a função da linha em branco?
5. Qual o status para: (a) criado com sucesso; (b) não autenticado; (c) autenticado sem permissão; (d) saldo insuficiente; (e) método não permitido?
6. O que significa "seguro" e "idempotente"? Classifique `GET`, `POST`, `PUT` e `DELETE`.
7. O que o TLS garante? O que acontece se você desligar a validação do certificado "só para funcionar"?
8. Como um servidor "lembra" do usuário se HTTP não tem estado? Explique o papel de `HttpOnly`, `Secure` e `SameSite`.
9. Por que o PayFlow envia dinheiro como string em JSON? O que muda no `HttpClient` entre um 404 e uma falha de rede?
10. Para que serve `curl --fail`? Por que p95 diz mais que a média ao medir latência?

<details><summary>Gabarito</summary>

1. IP identifica a máquina/interface; porta identifica o programa naquela máquina; DNS traduz nome em IP. `127.0.0.1` é o *loopback*: a própria máquina.
2. `Connection refused`: a máquina respondeu, mas ninguém escuta na porta. `Address already in use`: outra instância já ocupa a porta ao subir o servidor. `UnknownHostException`: o DNS não resolveu o nome.
3. Garante conexão, entrega **em ordem**, detecção e retransmissão de perdas. Não garante prazo nem que o outro lado **processou** o dado.
4. Linha de requisição (`POST /caminho HTTP/1.1`), `Host`, `Content-Type: application/json`, `Content-Length` (bytes do corpo), linha em branco, corpo. A linha em branco marca o fim dos headers e o início do corpo.
5. (a) 201 (+ `Location`); (b) 401; (c) 403; (d) 422 (regra de negócio; 409/400 são escolhas aceitáveis se o time for consistente); (e) 405 com `Allow`.
6. Seguro: não altera o estado do servidor. Idempotente: repetir deixa o servidor no mesmo estado que uma vez. `GET`: seguro e idempotente; `POST`: nenhum; `PUT`: idempotente; `DELETE`: idempotente.
7. Confidencialidade, integridade e autenticidade do servidor (via cadeia de certificados). Sem validar o certificado, qualquer intermediário se passa pelo servidor (*man-in-the-middle*) e o cifrado deixa de proteger. Correto: importar a CA no truststore.
8. Pelo cookie com um id (sessão no servidor) ou um token enviado a cada requisição. `HttpOnly`: JavaScript não lê; `Secure`: só por HTTPS; `SameSite`: limita o envio em requisições vindas de outro site (defesa contra CSRF).
9. JSON `number` pode virar `double` no consumidor e perder exatidão (`0.1 + 0.2`, ids grandes). No `HttpClient`, 404 e 500 **voltam como resposta** (`statusCode()`); falha de rede/timeout lança exceção (`IOException`, `HttpTimeoutException`).
10. Faz o `curl` sair com código ≠ 0 para respostas 4xx/5xx (código 22), permitindo que scripts e CI percebam falhas. A média esconde a cauda lenta; p95/p99 mostram a experiência dos piores casos.
</details>

## Parte 2 — Entrega prática (tag `payflow-J2`)
Servidor HTTP mínimo (`com.sun.net.httpserver`) expondo as contas do `mini-bank` em JSON, mais um cliente com `HttpClient` (aula J2.06).

Critérios de pronto:
- [ ] `BankServer`, `BankClient` e `MiniJson` no pacote `com.payflow.minibank.web`; `Bank.all()` adicionado.
- [ ] Rotas da tabela da J2.06 funcionando, com os status 200, 201, 400, 404, 405 e 422 (e 415 para `Content-Type` inválido).
- [ ] `AccountNotFoundException` no lugar da comparação de texto, tratada como 404.
- [ ] Filtro `GET /accounts?owner=...` com teste.
- [ ] `BankClient` com timeout de conexão **e** de resposta; sem `catch` vazio.
- [ ] Testes: os 7 da J1 + ao menos 4 do servidor (porta `0`, sem `Thread.sleep`), tudo verde em `mvn test` com JDK 25.
- [ ] Dinheiro como string com 2 casas no JSON; nenhum `double`.
- [ ] `scripts/smoke.sh` e coleção Bruno/Postman no repositório, sem segredos.
- [ ] README do `mini-bank` com as rotas e como subir o servidor.
- [ ] Commits em Conventional Commits (inglês), PR mesclado e tag enviada:
```bash
git tag payflow-J2
git push origin payflow-J2
```

## Parte 3 — Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Explico IP, DNS, porta, TCP e o ciclo cliente-servidor | ☐ | ☐ | ☐ |
| Leio e escrevo requisições/respostas HTTP (métodos, status, headers) | ☐ | ☐ | ☐ |
| Escolho o status e o método corretos para cada operação | ☐ | ☐ | ☐ |
| Explico TLS, certificado e cadeia de confiança, e diagnostico `SSLHandshakeException` | ☐ | ☐ | ☐ |
| Explico cookies, sessão e as armadilhas de JSON (números, datas, dinheiro) | ☐ | ☐ | ☐ |
| Depuro APIs com `curl`, DevTools e Bruno/Postman | ☐ | ☐ | ☐ |
| Escrevo servidor e cliente HTTP em Java e os testo com JUnit | ☐ | ☐ | ☐ |

**Regra:** se ficou com mais de duas linhas em 0, repita as aulas correspondentes antes de seguir para o [J3](../J3-banco-de-dados/README.md). Atualize `00-guia/progresso.md` e o `cronograma.md`.

## Referência do que foi acrescentado ao `mini-bank`
O código completo e testado (`Bank.all()`, `MiniJson`, `BankServer`, `BankClient`, `BankServerTest`) está na [J2.06](J2.06-servidor-minimo-e-httpclient.md). Use-o para **conferir** a sua versão depois de tentar; o objetivo é você escrever a sua.
