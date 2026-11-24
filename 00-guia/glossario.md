# Glossário

> Será ampliado a cada módulo. Termos entram aqui na primeira aula que os ensina. (Vazio de propósito: nada é listado antes de ser ensinado.)

| Termo | Tradução / significado | Aula em que aparece |
|---|---|---|
| Shell / `bash` | Interpretador de comandos do terminal | J0.01 |
| `PATH` | Lista de pastas onde o shell procura programas | J0.01 |
| Pipe (`\|`) | Liga a saída de um comando à entrada do próximo | J0.01 |
| JDK / JRE / JVM | Kit de desenvolvimento / ambiente de execução / máquina virtual que roda bytecode | J0.02 |
| SDKMAN | Gerenciador de versões de JDK e ferramentas | J0.02 |
| Maven | Ferramenta de build e gestão de dependências | J0.02 |
| `pom.xml` | Arquivo que descreve o projeto Maven | J0.02 |
| Coordenadas Maven | `groupId`, `artifactId` e `version`, que identificam um artefato | J0.02 |
| Dependência transitiva | Dependência de uma dependência, trazida automaticamente | J0.02 |
| Breakpoint | Marca que pausa a execução em uma linha durante a depuração | J0.03 |
| Refatoração | Mudar a estrutura do código sem mudar o comportamento | J0.03 |
| Commit | Foto (snapshot) do projeto com mensagem, no histórico do Git | J0.04 |
| Staging area | Área onde se prepara o próximo commit | J0.04 |
| Branch | Ponteiro móvel para um commit; linha paralela de trabalho | J0.04 |
| Merge / Rebase | Juntar histórias / reaplicar commits sobre outra base | J0.04 |
| Conflito de merge | Duas mudanças na mesma região que o Git não consegue combinar sozinho | J0.04 |
| `origin` | Nome padrão do repositório remoto | J0.05 |
| Pull request (PR) | Proposta de integrar uma branch, com revisão, no GitHub | J0.05 |
| Conventional Commits | Padrão `tipo(escopo): descrição` para mensagens de commit | J0.05 |
| Stack trace | Pilha de chamadas impressa quando ocorre uma exceção | J0.06 |
| `Caused by` | Exceção original embrulhada em outra; a causa raiz é a última | J0.06 |
| Primitivo / referência | Valor guardado na própria variável / endereço de um objeto no heap | J1.01 |
| Overflow | Resultado fora da faixa do tipo; em inteiros volta ao outro extremo sem erro | J1.01 |
| String pool | Área onde literais iguais compartilham o mesmo objeto | J1.01 |
| Curto-circuito | `&&` e `||` param de avaliar quando o resultado já é conhecido | J1.02 |
| Passagem por valor | Parâmetro recebe cópia (de número ou de endereço) | J1.03 |
| Sobrecarga / sobrescrita | Mesmo nome com assinaturas diferentes / subclasse redefine método herdado | J1.03, J1.06 |
| `StringBuilder` | Buffer mutável para montar texto | J1.04 |
| Encapsulamento | Esconder o estado e expor operações que protegem as regras | J1.05 |
| Polimorfismo | Mesma chamada, comportamento definido pelo tipo real do objeto | J1.06 |
| Interface | Contrato de métodos que uma classe se compromete a implementar | J1.07 |
| Composição | Classe que guarda e delega a outros objetos ("tem um") | J1.07 |
| Injeção de dependência | Entregar a dependência pronta (ex.: pelo construtor) em vez de criá-la | J1.07 |
| Contrato `equals`/`hashCode` | Objetos iguais têm o mesmo hash; base de `HashMap`/`HashSet` | J1.08 |
| Checked / unchecked | Exceção que o compilador obriga tratar / exceção de runtime | J1.09 |
| `try-with-resources` | Fecha recursos `AutoCloseable` automaticamente | J1.09 |
| Collection | Estrutura para guardar vários elementos (`List`, `Set`, `Map`…) | J1.10 |
| `ConcurrentModificationException` | Coleção alterada durante a iteração | J1.10 |
| `HashMap` | Mapa chave→valor por tabela hash | J1.11 |
| Generics / type erasure | Parâmetros de tipo conferidos na compilação e apagados em execução | J1.12 |
| PECS | Producer Extends, Consumer Super | J1.12 |
| Lambda / interface funcional | Função anônima / interface com um único método abstrato | J1.13 |
| Stream | Pipeline preguiçoso e de uso único sobre uma fonte de dados | J1.13 |
| `Optional` | Contêiner que pode ou não ter valor; para retornos | J1.14 |
| Record | Classe imutável de dados com `equals`/`hashCode`/`toString` gerados | J1.14 |
| `sealed` | Hierarquia fechada; permite `switch` exaustivo | J1.14 |
| `Instant` / `Clock` | Ponto no tempo (UTC) / fonte de "agora" injetável | J1.15 |
| `BigDecimal`, escala, `HALF_EVEN` | Decimal exato; casas decimais; arredondamento do banqueiro | J1.15 |
| Big-O | Notação de como o custo cresce com o tamanho da entrada | J1.16 |
| JUnit 6 / AssertJ | Framework de testes / assertivas fluentes | J1.16 |
| Arrange–Act–Assert | Estrutura de um teste: preparar, executar, verificar | J1.16 |
| IP / loopback | Endereço de uma interface de rede / `127.0.0.1`, a própria máquina | J2.01 |
| DNS (A, AAAA, CNAME, TTL) | Tradução de nome em IP; tipos de registro; tempo de cache | J2.01 |
| Porta / socket | Número que identifica o programa na máquina / par IP:porta | J2.01 |
| TCP | Transporte com conexão, ordem e retransmissão | J2.01 |
| HTTP / método / status code | Protocolo de requisição-resposta; ação pedida; resultado numérico | J2.02 |
| Seguro / idempotente | Não altera estado / repetir tem o mesmo efeito que uma vez | J2.02 |
| Header / `Content-Type` | Metadado da mensagem / formato do corpo | J2.02 |
| TLS / HTTPS | Camada de criptografia e autenticação / HTTP sobre TLS | J2.03 |
| Certificado, CA, cadeia de confiança | Documento assinado com a chave pública do site / quem assina / caminho até uma raiz confiável | J2.03 |
| Truststore | Lista de CAs em que o cliente (JVM, SO) confia | J2.03 |
| Cookie / `HttpOnly` / `Secure` / `SameSite` | Dado guardado pelo cliente e devolvido ao servidor; atributos de segurança | J2.04 |
| Sessão | Dados do usuário guardados no servidor, achados por um id no cookie | J2.04 |
| JSON | Formato de texto com objetos, arrays, strings, números, booleanos e null (RFC 8259) | J2.04 |
| Cookie jar | Armazém de cookies do cliente (`curl -c/-b`, `CookieManager`) | J2.04, J2.05 |
| Smoke test | Verificação rápida de que o essencial responde | J2.05 |
| Percentil (p50, p95, p99) | Valor abaixo do qual está essa % das medidas | J2.05 |
| `HttpServer` / `HttpClient` | Servidor e cliente HTTP do JDK | J2.06 |
| Roteamento | Escolher o código a executar a partir de método e caminho | J2.06 |
| Entidade / atributo / relacionamento | Coisa do negócio (vira tabela) / sua propriedade (coluna) / ligação entre entidades | J3.01 |
| Cardinalidade (1:1, 1:N, N:N) | Quantas instâncias de um lado se ligam às do outro | J3.01 |
| Tabela associativa | Tabela com duas chaves estrangeiras que implementa um N:N | J3.01 |
| Diagrama ER ("pé de galinha") | Desenho de entidades e relacionamentos (Mermaid `erDiagram`) | J3.01 |
| Chave primária / candidata / alternativa | Identificador oficial da linha / qualquer chave mínima possível / candidata não escolhida (vira `UNIQUE`) | J3.02 |
| Chave estrangeira (FK) | Coluna que deve existir na chave de outra tabela | J3.02 |
| Chave natural / substituta | Identificador com significado de negócio (e-mail) / inventado só para identificar (UUID) | J3.02 |
| Constraint | Regra declarada no banco (`NOT NULL`, `UNIQUE`, `PRIMARY KEY`, `FOREIGN KEY`, `CHECK`) | J3.02 |
| `ON DELETE CASCADE` | Apagar o pai apaga os filhos (perigoso com histórico) | J3.02 |
| Dependência funcional | "A determina B": para cada A, um único B | J3.02 |
| 1FN / 2FN / 3FN | Valores atômicos / sem dependência de parte da chave / sem dependência entre colunas não-chave | J3.02 |
| Desnormalizar | Repetir dado de propósito para ganhar leitura, com mecanismo de consistência | J3.02 |
| `uuidv7()` | UUID ordenado por tempo, nativo no PostgreSQL 18 | J3.02 |
| Servidor / database / schema / tabela | Hierarquia do PostgreSQL (schema padrão: `public`) | J3.03 |
| Container / volume (Docker) | Processo isolado e descartável / armazenamento que sobrevive ao container | J3.03 |
| `psql` / meta-comando | Cliente de linha de comando do PostgreSQL / comando iniciado por `\` | J3.03 |
| `pg_isready` | Verifica se o PostgreSQL aceita conexões | J3.03 |
| Backoff exponencial | Esperar o dobro a cada tentativa falha, até um teto | J3.03 |
| DDL / DML / DQL / TCL | Estrutura / dados / consulta / transação | J3.04 |
| `RETURNING` | Devolve as colunas da linha inserida/alterada/apagada | J3.04 |
| `ON CONFLICT` (upsert) | Ignora (`DO NOTHING`) ou atualiza (`DO UPDATE`) em vez de falhar por chave duplicada | J3.04 |
| `NUMERIC` / `TIMESTAMPTZ` | Decimal exato / instante (mostrado no fuso da sessão) | J3.04 |
| `NULL` / lógica de três valores | "Desconhecido"; comparações com ele dão "desconhecido" | J3.05 |
| Ordem lógica de um `SELECT` | `FROM` → `WHERE` → `GROUP BY` → `HAVING` → `SELECT` → `ORDER BY` → `LIMIT` | J3.05 |
| `LIMIT`/`OFFSET` / paginação keyset | Pular e pegar N / continuar a partir do último item visto | J3.05 |
| `INNER` / `LEFT` / `FULL` / `CROSS JOIN` | Só os que casam / todos da esquerda / todos dos dois lados / todas as combinações | J3.06 |
| Anti-join | "Quem **não** tem..." (`LEFT JOIN ... IS NULL` ou `NOT EXISTS`) | J3.06 |
| *Fan-out* | `JOIN` 1:N que repete a linha do lado "1" e infla somas | J3.06 |
| `GROUP BY` / `HAVING` / agregação | Agrupa linhas / filtra grupos / resume (`SUM`, `COUNT`, `AVG`...) | J3.07 |
| Subquery (escalar, `IN`, `EXISTS`, correlacionada) | `SELECT` dentro de outro `SELECT` | J3.07 |
| Reconciliação / conferência | Comparar o saldo guardado com a soma dos lançamentos (regra 3) | J3.07 |
| CTE (`WITH`) / CTE recursiva | Consulta nomeada dentro de outra / que usa o próprio resultado (hierarquias, grafos, séries) | J3.08 |
| Função de janela (`OVER`, `PARTITION BY`) | Calcula sobre um grupo sem colapsar as linhas (acumulado, ranking, `LAG`) | J3.08 |
| `ROW_NUMBER` / `RANK` / `DENSE_RANK` | Numeração sem empates / com empates e saltos / com empates sem saltos | J3.08 |
| Índice B-tree | Árvore balanceada ordenada que acha linhas em O(log n) | J3.09 |
| `EXPLAIN` / `EXPLAIN ANALYZE` | Mostra o plano de execução / executa e mede (cuidado com escritas) | J3.09 |
| Seq Scan / Index Scan / Bitmap Scan | Lê a tabela inteira / segue o índice / junta páginas pelo índice e as lê em ordem | J3.09 |
| Seletividade | Fração pequena da tabela que a condição devolve (índice ajuda) | J3.09 |
| Índice composto / de expressão | Várias colunas (ordem importa) / sobre uma expressão (`lower(email)`) | J3.09 |
| Transação / `BEGIN` / `COMMIT` / `ROLLBACK` / `SAVEPOINT` | Pacote de comandos "tudo ou nada" e seus controles | J3.10 |
| ACID | Atomicidade, Consistência, Isolamento, Durabilidade | J3.10 |
| Autocommit | Cada comando fora de `BEGIN` é uma transação confirmada na hora | J3.10 |
| Transação abortada | Depois de um erro, todos os comandos falham até o `ROLLBACK` | J3.10 |
| `READ COMMITTED` / MVCC | Nível de isolamento padrão do PostgreSQL / várias versões da linha: leitores e escritores não se bloqueiam | J3.10 |
| WAL | Log de escrita antecipada: garante a durabilidade | J3.10 |
| JDBC / driver / `DataSource` | API Java de acesso a bancos / implementação da fornecedora / fábrica de conexões | J3.11 |
| `PreparedStatement` | Consulta com parâmetros (`?`): o SQL é fixo e os valores vão à parte | J3.11 |
| `SQLState` | Código padronizado do erro SQL (`23505`, `40P01`...) | J3.11 |
| SQL injection | Texto do usuário que vira parte do SQL e muda seu significado | J3.11 |
| Lista permitida (*allowlist*) | Só entra no SQL o que você previu (nomes de coluna, direção) | J3.11 |
| Pool de conexões (HikariCP) | Conexões abertas reaproveitadas (visão; Spring Boot e P2) | J3.11 |
| *Deadlock* | Duas transações se esperam mutuamente; o banco derruba uma (`40P01`) | J3.11 |
| Migração (versionada `V`, repetível `R`) | Mudança de esquema em arquivo numerado, aplicada uma vez / reaplicada quando muda | J3.12 |
| `flyway_schema_history` / checksum | Tabela do histórico de migrações / assinatura do arquivo para detectar edição | J3.12 |
| *Roll-forward* | Corrigir com uma migração nova, nunca editando a antiga | J3.12 |
| `clean` / `baseline` / `repair` | Apaga o schema (só teste) / declara a versão de um banco existente / ajusta o histórico | J3.12 |
| DOM (*Document Object Model*) | Árvore de objetos que o navegador monta a partir do HTML; o JavaScript lê e altera | J4.01 |
| HTML semântico | Elementos que dizem o que o conteúdo **é** (`main`, `nav`, `h1`, `button`), não só como parece | J4.01 |
| `name` (campo de formulário) | Nome com que o valor viaja no `POST`; sem ele o campo **não é enviado** | J4.01 |
| `application/x-www-form-urlencoded` | Corpo padrão de formulário: `a=1&b=2` com URL-encoding (`@` → `%40`, espaço → `+`) | J4.01 |
| Validação nativa (`required`, `pattern`, `validity`) | Checagem do navegador antes de enviar; conforto, **não** segurança | J4.01 |
| Cascata / especificidade | Regras de qual declaração CSS vence: origem, especificidade (id > classe > elemento), ordem | J4.02 |
| *Box model* / `box-sizing: border-box` | Conteúdo, padding, borda e margem; `border-box` faz o `width` incluir padding e borda | J4.02 |
| Flexbox | Layout de **uma dimensão** (`display: flex`, `justify-content`, `align-items`, `gap`) | J4.02 |
| Variáveis CSS (`--cor`) | Valores reutilizáveis (`var(--brand)`) definidos em `:root` | J4.02 |
| CSS Grid (`fr`, `minmax`, `auto-fill`) | Layout de **duas dimensões**; `repeat(auto-fill, minmax(16rem, 1fr))` cria quantas colunas couberem | J4.03 |
| `viewport` / *mobile-first* | `<meta name="viewport">` iguala o layout à tela; escrever o estilo base para o celular e acrescentar com `min-width` | J4.03 |
| `textContent` × `innerHTML` | Insere **texto** (seguro) × interpreta **HTML** (risco de XSS) | J4.04 |
| Delegação de eventos | Um ouvinte no ancestral atende os filhos (inclusive criados depois) via `event.target.closest` | J4.04 |
| `<template>` | Elemento HTML não renderizado, clonado pelo JavaScript para repetir uma estrutura | J4.04 |
| Laço de eventos (*event loop*) / microtarefa | Ordem: código síncrono → microtarefas (`then`, `await`) → próxima tarefa (timer, rede, clique) | J4.05 |
| Promise / `async`/`await` | Resultado futuro (pendente, cumprida, rejeitada) e a sintaxe sequencial para usá-la | J4.05 |
| `fetch` (`response.ok`) | API de requisições do navegador; **não rejeita** em 404/500, só em falha de rede, CORS ou abort | J4.05 |
| Origem / CORS | Esquema+host+porta; o navegador bloqueia a leitura entre origens, salvo autorização do servidor (`Access-Control-Allow-Origin`) | J4.05 |
| Thymeleaf / *natural template* | Motor de templates HTML do servidor; o arquivo é HTML válido, aberto direto como protótipo | J4.06 |
| `th:text` × `th:utext` | Troca o texto **escapando** HTML × sem escapar (perigoso com dado externo) | J4.06 |
| Fragmento (`th:fragment`, `th:replace`) | Trecho de template reutilizado (cabeçalho, rodapé) | J4.06 |
| SSR (renderização no servidor) | O servidor devolve a página HTML pronta | J4.06 |
| Post/Redirect/Get (PRG) | Após um `POST`, responder `303` com um `GET`, evitando reenvio e duplicação ao recarregar | J4.06 |
| `303 See Other` | Redirecionamento que manda o cliente fazer `GET` no novo endereço | J4.06 |
| SPA (*Single Page Application*) | Página que baixa JavaScript uma vez e troca as telas buscando JSON de uma API | J4.07 |
| npm / `package.json` / bundler | Gerenciador de pacotes do Node (≈ Maven) / descritor / ferramenta que junta e compila o front | J4.07 |
| TypeScript | JavaScript com tipos estáticos; os tipos são **apagados** na compilação e não validam dados em execução | J4.07 |
| React / Angular | Biblioteca de UI baseada em componentes e hooks / framework completo com TypeScript, DI e CLI | J4.07 |
| Árvore de acessibilidade | Papéis, nomes e estados que o navegador expõe aos leitores de tela | J4.08 |
| WCAG (A/AA/AAA, POUR) | Padrão de acessibilidade: perceptível, operável, compreensível, robusto; AA = alvo de mercado | J4.08 |
| Razão de contraste (4,5:1) | Mínimo do WCAG AA para texto normal (3:1 para texto grande) | J4.08 |
| ARIA (`role`, `aria-*`) | Atributos que completam a árvore de acessibilidade; "sem ARIA é melhor que ARIA errada" | J4.08 |
| axe-core | Motor open source que lista violações de acessibilidade por regra e impacto | J4.08 |
| Inversão de controle (IoC) | O framework controla a criação e a chamada dos objetos, não o seu código | J5.01 |
| Contêiner / bean / `ApplicationContext` | Quem cria e liga os objetos / objeto gerenciado por ele / o contêiner do Spring | J5.01, J5.02 |
| Reflexão | API do Java para inspecionar e criar classes em execução (usada pelo contêiner) | J5.01 |
| Ordenação topológica | Ordem em que dependências vêm antes de quem depende delas; detecta ciclos | J5.01 |
| Spring Initializr / starter / BOM | Gerador de projetos / conjunto de dependências compatíveis / lista de versões gerenciadas pelo parent | J5.02 |
| `@SpringBootApplication` | `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan` | J5.02 |
| JAR executável (*fat jar*) | JAR com as dependências dentro e um *launcher*; `java -jar` | J5.02 |
| Estereótipo (`@Service`, `@Repository`...) | `@Component` com um significado de camada | J5.03 |
| Escopo (singleton, prototype, request) | Quantas instâncias de um bean existem e por quanto tempo | J5.03 |
| `@PostConstruct` / `@PreDestroy` | Callbacks após a injeção / no fechamento do contexto | J5.03 |
| Proxy (`$$SpringCGLIB$$`) | Subclasse gerada que envolve o bean (transações, tradução de exceções, `@Configuration`) | J5.03, J5.11 |
| `@Primary` / `@Qualifier` | Resolvem mais de um candidato a injeção | J5.03 |
| Autoconfiguração | Beans criados pelo Boot conforme classpath, propriedades e beans existentes | J5.04 |
| `@ConditionalOnMissingBean` | O Boot "recua" se você já definiu o bean | J5.04 |
| Relatório de condições (`--debug`) | Lista por que cada autoconfiguração entrou ou não | J5.04 |
| Perfil (*profile*) | Conjunto de configurações/beans ativado por nome (`dev`, `test`) | J5.05 |
| Precedência de propriedades | Arquivo < variável de ambiente < `-D` < argumento | J5.05 |
| *Relaxed binding* | `page-size`, `pageSize` e `PAGE_SIZE` ligam à mesma propriedade | J5.05 |
| `@ConfigurationProperties` | Liga um grupo de propriedades a um objeto tipado (aqui, um `record`) | J5.05 |
| `DispatcherServlet` (*front controller*) | O servlet único do Spring MVC que despacha para os controllers | J5.06 |
| `HandlerInterceptor` | Código antes/depois do controller para várias rotas | J5.06 |
| `@ControllerAdvice` | Tratamento de exceções compartilhado entre controllers | J5.06 |
| 303 *See Other* × 302 | 303 manda trocar para GET (PRG); o `redirect:` do Spring devolve 302 | J5.06 |
| ORM / JPA / Hibernate / Spring Data JPA | Mapeamento objeto-relacional / a especificação / a implementação / a camada de repositórios | J5.07 |
| `@Entity`, `@Id`, `@Version` | Classe mapeada / chave primária / controle de versão (locking otimista) | J5.07 |
| `ddl-auto: validate` | O Hibernate só confere o esquema; não o altera | J5.07 |
| Lado dono / lado inverso (`mappedBy`) | O lado com a FK manda no banco; o outro só navega | J5.08 |
| Lazy × eager | Carregar a associação só quando usada / junto com a entidade | J5.08 |
| Contexto de persistência | Entidades gerenciadas por uma transação: identidade, retrato e dirty checking | J5.08 |
| Dirty checking / flush | O Hibernate compara com o retrato e grava as alterações no flush | J5.08 |
| `LazyInitializationException` | Acesso a um proxy sem sessão aberta | J5.08 |
| *Open Session in View* (OSIV) | Sessão aberta até a renderização; o PayFlow a desliga | J5.08 |
| Repositório Spring Data / consulta derivada | Interface com implementação gerada / consulta pelo nome do método | J5.09 |
| `DataIntegrityViolationException` | Exceção do Spring para violação de constraint | J5.09 |
| JPQL | Linguagem de consulta sobre entidades (`select e from LedgerEntry e ...`) | J5.10 |
| `Slice` × `Page` | Página com `hasNext` (1 consulta) / com total (2 consultas) | J5.10 |
| Paginação *keyset* | Paginar por chave (`where (a, b) < (?, ?)`), sem `OFFSET` | J5.10 |
| `@Transactional` | Abre, confirma ou desfaz a transação em volta de um método (via proxy) | J5.11 |
| Propagação (`REQUIRED`, `REQUIRES_NEW`) | O que fazer se já existe uma transação | J5.11 |
| Auto-invocação | `this.metodo()` não passa pelo proxy: sem transação | J5.11 |
| *Lost update* / locking otimista | Escrita que sobrescreve outra / coluna de versão que recusa a escrita velha | J5.11 |
| Problema N+1 | 1 consulta da lista + 1 por item para associações preguiçosas | J5.12 |
| `JOIN FETCH` / `@EntityGraph` | Carregar associações na mesma consulta | J5.12 |
| Projeção em DTO | Consulta que devolve só as colunas necessárias, sem entidades | J5.12 |
| *Batch fetching* | Carregar proxies em lotes (`in (...)`) | J5.12 |
| Bean Validation (Jakarta Validation) | Validação declarativa por anotações; implementada pelo Hibernate Validator | J5.13 |
| `@Valid` / `BindingResult` | Dispara a validação / guarda os erros para o controller decidir | J5.13 |
| Erro de conversão × violação de regra | `typeMismatch` ("abc") × `@Positive` violada | J5.13 |
| SLF4J / Logback | Fachada de logging / a implementação padrão do Boot | J5.14 |
| Níveis de log | `ERROR` > `WARN` > `INFO` > `DEBUG` > `TRACE` | J5.14 |
| MDC (*Mapped Diagnostic Context*) | Mapa por thread impresso em cada log (ex.: `requestId`) | J5.14 |
| Log estruturado | Log em JSON (campos, não texto livre) | J5.14 |
| DevTools / reinício rápido | Recarrega só as classes do projeto ao compilar; só em desenvolvimento | J5.15 |
| Actuator / endpoint | Endpoints de operação (`health`, `info`, `metrics`, `loggers`...) | J5.15 |
| Liveness × readiness | "O processo está vivo?" (reiniciar) × "pronto para tráfego?" (tirar do balanceador) | J5.15 |
| Micrometer | Fachada de métricas usada pelo Actuator | J5.15 |
| REST / recurso / representação | Estilo de arquitetura: recursos com URI, manipulados por representações e métodos HTTP de semântica fixa | J6.01 |
| Método seguro × idempotente | Seguro não altera estado; idempotente repetido dá o mesmo estado final | J6.01 |
| DTO (Data Transfer Object) | Objeto de entrada/saída da API, separado da entidade | J6.02 |
| *Mass assignment* | Cliente define campos que só o servidor deveria definir | J6.02 |
| MapStruct | Gerador (em compilação) de mapeadores entre objetos | J6.02 |
| Problem Details (RFC 9457) | Formato padrão de erro HTTP (`application/problem+json`) | J6.04 |
| `Specification` | Filtro dinâmico reutilizável do Spring Data JPA | J6.05 |
| Intervalo semiaberto `[from, to)` | Início inclusivo, fim exclusivo | J6.05 |
| `Idempotency-Key` | Cabeçalho com a chave que torna um `POST` seguro de repetir | J6.06 |
| `Deprecation` / `Sunset` | Cabeçalhos que anunciam versão obsoleta e a data de desligamento | J6.06 |
| OpenAPI / springdoc | Contrato legível por máquina e o gerador a partir do código | J6.07 |
| CORS / preflight | Relaxamento controlado da política de mesma origem; `OPTIONS` prévio | J6.08 |
| `RestClient` / timeout | Cliente HTTP síncrono do Spring; limite de espera por conexão/leitura | J6.08 |
| HATEOAS / Richardson | Hipermídia na resposta; escala de maturidade de APIs (0 a 3) | J6.09 |
| Pirâmide de testes | Muitos testes de unidade, alguns de integração, poucos de ponta a ponta | J7.01 |
| F.I.R.S.T. / Arrange–Act–Assert | Qualidades de um bom teste (rápido, isolado, repetível, autovalidado, oportuno) e a estrutura do teste | J7.01 |
| AssertJ / `isEqualByComparingTo` | Biblioteca de afirmações fluentes; compara `BigDecimal` por valor, ignorando a escala | J7.01 |
| Valores-limite / partição de equivalência | Técnicas para escolher casos de teste: as fronteiras e um representante de cada classe de entradas | J7.01 |
| Dublê de teste (dummy, stub, fake, spy, mock) | Objeto que substitui um colaborador no teste | J7.02 |
| Mockito / *strict stubs* | Biblioteca de dublês; modo que falha com stub sem uso ou argumento diferente do programado | J7.02 |
| `ArgumentCaptor` | Guarda o argumento passado a um dublê para inspeção | J7.02 |
| Teste de fatia (*slice*) | Teste que carrega só uma parte do contexto (web, JPA) | J7.03 |
| `@WebMvcTest` / `MockMvc` | Fatia web e requisição simulada, sem servidor | J7.03 |
| `@MockitoBean` | Substitui um bean do contexto por um mock do Mockito | J7.03 |
| `@DataJpaTest` / `TestEntityManager` | Fatia de persistência com transação e rollback por teste | J7.04 |
| Cache de contexto (Spring TestContext) | Reaproveitamento do `ApplicationContext` entre classes com a mesma configuração | J7.04 |
| `@DirtiesContext` | Descarta o contexto cacheado (último recurso) | J7.04 |
| Testcontainers / Ryuk | Containers descartáveis para testes; o Ryuk os remove ao fim | J7.05 |
| `@ServiceConnection` | Cria os detalhes de conexão a partir de um container, com precedência sobre as propriedades | J7.05 |
| `@PayflowTest` | Anotação composta do PayFlow: contexto completo, perfil `test`, container PostgreSQL e `ResetDatabase` | J7.05 |
| JaCoCo / cobertura de linhas e de *branches* | Medidor de cobertura por agente Java; linhas executadas × decisões exercitadas | J7.06 |
| *Quality gate* (portão de qualidade) | Regra que faz o *build* falhar se a qualidade cair abaixo de um limite | J7.06 |
| TDD (vermelho, verde, refatorar) | Escrever o teste antes do código, em ciclos curtos | J7.06 |
| Refatoração / teste de caracterização | Mudar a estrutura sem mudar o comportamento; teste que registra o comportamento atual | J7.07 |
| *Code smell* | Indício de código caro de manter (método longo, número mágico, *flag*...) | J7.07 |
| Checkstyle / SpotBugs / SonarLint | Estilo no código-fonte; padrões de bug no *bytecode*; análise na IDE | J7.08 |
| Falso positivo / *baseline* | Achado que não é problema real; estado aceito para barrar só o código novo | J7.08 |
| *Code review* / `blocking:` `suggestion:` `nit:` | Revisão do código por outra pessoa e rótulos de peso dos comentários | J7.09 |
| Conventional Commits / SemVer | Padrão de mensagens (`feat`, `fix`, `!`) e a relação com a versão MAJOR.MINOR.PATCH | J7.09 |
| Autenticação / autorização | Confirmar **quem** é o usuário / decidir **o que** ele pode fazer | J8.01 |
| `SecurityFilterChain` | Cadeia de *servlet filters* do Spring Security; vale a primeira cadeia cujo `securityMatcher` casa | J8.01 |
| `Authentication` / `SecurityContext` | A tentativa ou o usuário autenticado (principal, autoridades) e o seu contêiner por requisição | J8.01 |
| `GrantedAuthority` / papel (`ROLE_`) | Permissão em forma de texto; papel é a autoridade com prefixo `ROLE_` | J8.01 |
| Hash com sal e custo (BCrypt) | Resumo de senha lento e aleatório por senha; o sal e o custo vão dentro do hash | J8.02 |
| `DelegatingPasswordEncoder` / `{bcrypt}` | Codificador que lê o prefixo `{id}` do hash e delega ao algoritmo certo | J8.02 |
| `UserDetails` / `UserDetailsService` | A visão que o Spring Security tem de um usuário / o ponto que o busca pelo nome de login | J8.02 |
| Enumeração de usuários | Descobrir quais contas existem pela diferença de resposta ou de tempo | J8.02 |
| Sessão (`HttpSession`) e atributos do cookie | Estado no servidor com o cookie como número; `HttpOnly`, `Secure`, `SameSite` | J8.03 |
| *Session fixation* | Fixar um id de sessão conhecido na vítima; prevenida trocando o id no login | J8.03 |
| JWT / claims / HS256 | Token assinado (cabeçalho.claims.assinatura); HMAC-SHA256 com segredo compartilhado; legível, não criptografado | J8.03 |
| *Bearer token* | Credencial enviada no cabeçalho `Authorization: Bearer ...` | J8.03 |
| `@PreAuthorize` / `@EnableMethodSecurity` | Autorização por método, aplicada por proxy (como `@Transactional`) | J8.04 |
| Defesa em profundidade | Várias barreiras independentes (URL, método, objeto): a falha de uma não abre a porta | J8.04 |
| SQL injection / consulta parametrizada | Dado tratado como comando / valores enviados separados da consulta | J8.05 |
| XSS / escape na saída | Script de terceiros executando no seu domínio / transformar `<` em `&lt;` ao exibir | J8.05 |
| CSP (Content Security Policy) | Cabeçalho que lista o que a página pode carregar e executar | J8.05 |
| CSRF / token CSRF | Requisição forjada usando o cookie da vítima / valor secreto por sessão exigido nos `POST` | J8.05 |
| IDOR (BOLA) | Acesso a objeto por id sem conferir o dono; defesa: autorização por objeto e `404` | J8.06 |
| *Mass assignment* | Gravar campos que o cliente não deveria controlar (`role`); defesa: DTO | J8.06 |
| *Brute force* / *credential stuffing* | Tentar muitas senhas / testar pares vazados de outros sites | J8.06 |
| *Log injection* (CWE-117) | Forjar linhas de log com quebras de linha vindas do atacante | J8.06 |
| Segredo / `configtree` | Dado que dá poder a quem o tem; arquivos montados lidos como propriedades pelo Spring Boot | J8.07 |
| CVE / CVSS / SCA | Identificador de falha / nota de gravidade 0 a 10 / análise de dependências | J8.07 |
| LGPD: titular, controlador, operador, encarregado (DPO), ANPD | Lei 13.709/2018: a pessoa, quem decide o tratamento, quem trata por ele, o canal de contato e a autoridade | J8.07 |
| Container | Processo isolado (namespaces) e limitado (cgroups) que compartilha o kernel do host | J9.01 |
| Imagem (Docker) / camada (*layer*) | Pacote imutável com o sistema de arquivos e a configuração / cada passo do build, guardado em cache e compartilhado | J9.01 |
| Registry / tag / digest | Servidor de imagens / rótulo mutável de versão / identidade imutável (hash) | J9.01 |
| Namespace / cgroup | Visão própria de PID, rede, arquivos e hostname / limite e medição de CPU e memória | J9.01 |
| Volume nomeado / *bind mount* | Armazenamento gerenciado pelo Docker, sobrevive ao container / pasta do host montada no container | J9.01 |
| `docker run -p host:container` | Publica a porta; com `127.0.0.1:` só a máquina local alcança | J9.01 |
| Dockerfile / contexto de build | Receita de camadas / a pasta enviada ao daemon no `docker build` | J9.02 |
| Build multi-stage | Dockerfile com vários `FROM`: compila em um estágio e leva só o artefato ao final | J9.02 |
| JAR em camadas (`jarmode=tools extract --layers`) | O JAR do Boot dividido em dependências (raras mudanças) e código (muitas) para o cache de imagem | J9.02 |
| `ENTRYPOINT` forma *exec* × *shell* | Lista JSON (a JVM é o PID 1 e recebe os sinais) × `sh -c` (o shell pode engolir o `SIGTERM`) | J9.02 |
| `SIGTERM` / `SIGKILL` / códigos 143 e 137 | Pedido de parada (tratável) / morte imediata / `128+15` e `128+9` | J9.02 |
| Graceful shutdown | Terminar as requisições em andamento antes de sair (`server.shutdown: graceful`) | J9.02 |
| `MaxRAMPercentage` | Percentual da memória do container usado como *heap* máximo da JVM (padrão 25%) | J9.02 |
| musl × glibc | Biblioteca C do Alpine × a do Ubuntu/Debian; afeta bibliotecas nativas | J9.02 |
| Docker Compose / `compose.yaml` | Descrição versionada de serviços, redes, volumes e segredos de uma aplicação | J9.03 |
| `healthcheck` / `depends_on: service_healthy` | Comando de saúde dentro do container / esperar o `healthy` para iniciar o dependente | J9.03 |
| `docker compose up --wait` / `down -v` | Sobe e espera ficar saudável / derruba **e apaga os volumes** | J9.03 |
| Perfil do Compose (`profiles`) | Serviço que só sobe quando pedido (`--profile tools`) | J9.03 |
| Segredo do Compose (`/run/secrets`) | Arquivo montado (bind mount) no container; a aplicação o lê por `configtree` | J9.03 |
| `restart: unless-stopped` | Religa o container se o processo sair, salvo parada manual | J9.03 |
| Driver de log `json-file` / rotação | Arquivo JSON por container no host / `max-size` e `max-file` | J9.04 |
| `OOMKilled` | Marca de que o kernel matou o container por falta de memória (saída 137) | J9.04 |
| JDWP / depuração remota | Protocolo de depuração da JVM; permite executar código: só em dev, em `127.0.0.1` | J9.04 |
| *Thread dump* por `SIGQUIT` | Lista de threads da JVM impressa no `stdout` ao receber `kill -3` | J9.04 |
| `.dockerignore` | Lista do que não entra no contexto de build (reduz o tamanho e evita vazar segredos) | J9.04 |
| CI / CD / entrega × implantação contínua | Validar toda mudança automaticamente / deixar sempre pronto para ir ao ar / ir ao ar sozinho | J10.01 |
| Workflow, evento, job, runner, passo, action | Arquivo YAML do Actions / o que dispara / conjunto de passos em uma VM / a máquina / comando ou módulo reutilizável | J10.01 |
| `GITHUB_TOKEN` / `permissions` | Credencial criada a cada execução / escopo mínimo que ela recebe | J10.01 |
| `concurrency` / `timeout-minutes` | Fila que cancela execuções obsoletas / limite de tempo do job (padrão 360 min) | J10.01 |
| Maven Wrapper (`mvnw`) | Script versionado que baixa a versão fixa do Maven | J10.01 |
| Ruleset / check obrigatório | Regra da branch que exige PR e CI verde para o merge | J10.01 |
| Injeção de script (Actions) | `${{ }}` com dado de terceiro dentro de `run:`; defesa: `env:` | J10.01 |
| Cache de dependências / artefato / resumo do job | `~/.m2` restaurado / arquivo guardado da execução / Markdown em `$GITHUB_STEP_SUMMARY` | J10.02 |
| Fixar action por SHA | Usar o hash do commit em vez da tag móvel | J10.02 |
| `pull_request` × `pull_request_target` | Código da PR sem segredos em fork / contexto da base com segredos (perigoso) | J10.02 |
| Dependabot / `dependency-review` | PRs de atualização automática / bloqueio de dependência nova vulnerável | J10.02 |
| IaaS / PaaS / SaaS / serverless | Quanto você gerencia da pilha, do máximo ao mínimo | J10.03 |
| Região / zona de disponibilidade (AZ) | Localização independente / data center(s) isolado(s) dentro dela | J10.03 |
| Responsabilidade compartilhada | AWS: segurança **da** nuvem; cliente: segurança **na** nuvem | J10.03 |
| Egress | Tráfego de saída para a internet (cobrado) | J10.03 |
| Plano Free da AWS | Créditos por 6 meses; vira Paid ao entrar no Organizations | J10.03 |
| Conta raiz / MFA / orçamento (Budgets) | Dono da conta, uso raro / segundo fator / alerta de custo com atraso | J10.04 |
| IAM: usuário, grupo, papel, política | Identidade / conjunto / identidade assumida sem senha / JSON Effect-Action-Resource-Condition | J10.04 |
| `aws login` | Credenciais temporárias a partir do login do console, sem chave de acesso | J10.04 |
| EC2 / AMI / EBS / `user-data` | VM / imagem do disco / disco / script do primeiro boot | J10.05 |
| IMDSv2 | Metadados da instância só com token de sessão (mitiga SSRF) | J10.05 |
| Security Group | Firewall com estado na interface; regras só de permissão; origem pode ser outro SG | J10.05 |
| RDS / `--no-publicly-accessible` / Multi-AZ | Banco gerenciado / sem IP público / réplica em outra AZ | J10.05 |
| `sslmode=verify-full` | TLS que valida cadeia **e** nome do servidor (`require` não valida) | J10.05 |
| S3 / Block Public Access / lifecycle | Objetos em buckets / trava contra acesso público / expiração automática | J10.05 |
| ECR / tag imutável | Registro privado de imagens / tag que não pode ser reescrita | J10.05 |
| SSM Parameter Store / Session Manager / Run Command | Parâmetros (SecureString) / acesso e túnel sem porta aberta / execução remota de comandos | J10.05 |
| OIDC (GitHub → AWS) / `sub` / `aud` | Federação sem chave de acesso: claims do token conferidas pela trust policy | J10.06 |
| `environment` do GitHub | Ambiente com aprovação manual e regras de quem pode implantar | J10.06 |
| Rollback (`.env.previous`) | Voltar à imagem anterior quando o `--wait` falha | J10.06 |
| Caddy / `request_body max_size` | Proxy reverso com HTTPS automático / teto do corpo na borda (413) | J10.06 |
| Teardown / verificação de vazio / idempotência | Destruir na ordem inversa / provar que nada ficou / rodar de novo sem efeito colateral | J10.07 |
| Classloader (*bootstrap*/*platform*/*application*) | Quem carrega classes; delegação pai primeiro | P1.01 |
| Inicialização de classe | Execução do `static {…}`, só no primeiro uso real | P1.01 |
| Heap / pilha (*stack*) / metaspace | Objetos (GC) / frames por thread / metadados de classes em memória nativa | P1.01 |
| `StackOverflowError` / `OutOfMemoryError` | Pilha estourada / heap, metaspace ou memória nativa esgotados | P1.01 |
| NMT (*Native Memory Tracking*) | Contabilidade da memória nativa da JVM (`jcmd VM.native_memory`) | P1.01 |
| Cabeçalho de objeto / *Compact Object Headers* | Metadados por objeto (12→8 bytes com JEP 519) | P1.01 |
| JIT / C1 / C2 / compilação em camadas | Compilação de métodos quentes em código nativo, em níveis (0 interpretador, 1-3 C1, 4 C2) | P1.02 |
| OSR (*on-stack replacement*) | Troca para código compilado no meio de um laço | P1.02 |
| Desotimização (*uncommon trap*) / `made not entrant` | Descarte de código compilado cuja suposição caiu | P1.02 |
| *Inlining* / chamada megamórfica | Copiar o corpo do método chamado / chamada virtual com 3+ implementações | P1.02 |
| Cache AOT (JEP 514/515) | Cache de classes e perfis gravado em treino, para partida/aquecimento mais rápidos | P1.02 |
| Hipótese geracional / geração jovem e velha | A maioria dos objetos morre jovem; heap dividido por idade | P1.03 |
| *Stop-the-world* (STW) / *safepoint* | Pausa em que todas as threads da aplicação param | P1.03 |
| G1 / região / coleta mista / *humongous* | Coletor por regiões / coleta que inclui regiões velhas / objeto ≥ metade de uma região | P1.03 |
| *Full GC* | Coleta STW de heap inteiro com compactação: sintoma de heap apertado ou fragmentação | P1.03 |
| Parallel / Serial | Coletores STW multi-thread (vazão) / de uma thread | P1.03 |
| ZGC (geracional) / Shenandoah | Coletores concorrentes de pausa sub-milissegundo | P1.04 |
| Barreira de leitura / *allocation stall* | Verificação a cada leitura de referência no ZGC / thread de alocação bloqueada por falta de folga | P1.04 |
| `-Xlog` (*Unified Logging*) | Sistema de log da JVM: `[decoradores][nível][tags]` | P1.04 |
| Thread de plataforma / estados de thread | Thread 1:1 com o SO / `NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TIMED_WAITING`, `TERMINATED` | P1.05 |
| Interrupção cooperativa | `interrupt()` levanta uma flag; a tarefa precisa tratá-la | P1.05 |
| `ThreadPoolExecutor` (core/fila/max/rejeição) | Pool configurável; fila ilimitada nunca passa do *core* | P1.05 |
| Contrapressão (*backpressure*) | Recusar ou frear o produtor quando o consumidor não acompanha | P1.05 |
| `Future` / `CompletableFuture` | Promessa de resultado / promessa componível (`thenApply`, `thenCompose`, `allOf`…) | P1.05, P1.06 |
| `commonPool` (`ForkJoinPool`) | Pool compartilhado (≈ núcleos − 1) usado por padrão; não para I/O bloqueante | P1.06 |
| `CompletionException` / `ExecutionException` | Embrulhos da falha em `join()` / `get()` | P1.06 |
| `ReentrantLock` / `ReadWriteLock` / `StampedLock` | Locks explícitos (`tryLock`, leitores múltiplos, leitura otimista) | P1.07 |
| CAS (*compare-and-set*) / ABA | Troca atômica condicional sem lock / valor mudou e voltou sem o CAS perceber | P1.07 |
| `LongAdder` | Contador por células para alta contenção; leitura aproximada | P1.07 |
| `ConcurrentHashMap` (`merge`, `compute`) | Mapa concorrente; operações compostas atômicas por chave | P1.07 |
| `BlockingQueue` / `Semaphore` / `CountDownLatch` | Fila bloqueante / N permissões / espera por N eventos | P1.07 |
| Modelo de Memória do Java (JMM) / *happens-before* | Contrato de visibilidade e ordem entre threads (JLS §17) | P1.08 |
| `volatile` | Visibilidade e ordem; não dá atomicidade de `x++` | P1.08 |
| Publicação segura / *double-checked locking* | Tornar um objeto visível por completo a outra thread / idioma que exige `volatile` | P1.08 |
| *Data race* / *race condition* | Acesso sem HB / resultado dependente do tempo | P1.09 |
| *Read-modify-write* / *check-then-act* | Ler-calcular-gravar / verificar e agir supondo que a condição vale | P1.09 |
| *Deadlock* / *livelock* / *starvation* | Espera circular / todos recuam em sincronia / alguém nunca é atendido | P1.09 |
| Ordem global de locks | Sempre adquirir locks na mesma ordem (ex.: por `id`) para impedir espera circular | P1.09 |
| Thread virtual / *carrier* / *pinning* | Thread gerenciada pela JVM / thread de plataforma que a executa / virtual presa à carrier | P1.10 |
| `ScopedValue` | Valor imutável vinculado a um escopo (final no Java 25) | P1.10 |
| *Structured concurrency* (`StructuredTaskScope`) | Tarefas concorrentes como unidade com cancelamento das irmãs (preview no Java 25) | P1.10 |
| `sealed` / *record pattern* / `switch` exaustivo | Hierarquia fechada / desmontar um `record` num padrão / `switch` que o compilador prova completo | P1.11 |
| Construtor flexível / `import module` / arquivo compacto | Código antes de `super(...)` / importar módulo inteiro / `void main()` sem classe (Java 25) | P1.11 |
| *Gatherer* | Operação intermediária customizada de `Stream` (`gather`, Java 24) | P1.11 |
| JPMS / `module-info.java` / `exports` / `opens` | Sistema de módulos do Java e suas diretivas | P1.12 |
| Módulo sem nome (*unnamed*) / classpath | Onde roda o código sem `module-info` (inclusive Spring Boot) | P1.12 |
| `--add-opens` / `jlink` / `jdeps` | Libera reflexão em pacote do JDK / gera runtime mínimo / analisa dependências | P1.12 |
| Invariante / conciliação | Regra que deve sempre valer (regras 1–4) / conferência `saldo = Σ lançamentos` | P1.13 |
| *Lost update* / *retry* com *backoff* e *jitter* | Atualização perdida / repetição com espera crescente e aleatória | P1.09, P1.13 |
| Anomalia (leitura suja, não repetível, fantasma, de serialização) | Resultado que nenhuma execução em série produziria: ler dado não confirmado / ver a mesma linha mudar / ver outro conjunto de linhas / resultado incompatível com qualquer ordem | P2.01 |
| *Snapshot* (MVCC) / `READ COMMITTED` / `REPEATABLE READ` / `SERIALIZABLE` | Foto do que está confirmado: nova a cada comando / uma por transação / com detecção de conflitos (cobra *retry*) | P2.01 |
| *Write skew* | Duas transações leem o mesmo estado e gravam em linhas diferentes, violando uma regra do conjunto; só `SERIALIZABLE` (ou restrição/trava) impede | P2.01 |
| `40001` / `40P01` / `55P03` | SQLSTATEs de falha de serialização / *deadlock* / trava indisponível (`NOWAIT`, `lock_timeout`) | P2.01–P2.03 |
| `ConcurrencyFailureException` | Superclasse do Spring de `ObjectOptimisticLockingFailureException` e `PessimisticLockingFailureException` (*deadlock*, `40001`, `lock_timeout`); o que o *retry* repete | P2.02, P2.03 |
| Locking otimista / pessimista | Detectar a colisão ao gravar (`@Version`, repetir) / trancar antes de ler (`FOR UPDATE`, esperar) | P2.02 |
| `UPDATE` atômico / condicional | `SET x = x ± ?` (e `WHERE x >= ?`) em **um** comando: o banco espera a trava e reavalia sobre a linha nova | P2.02 |
| `FOR UPDATE` / `FOR NO KEY UPDATE` / `NOWAIT` / `SKIP LOCKED` | Trava de linha forte / mais fraca (não bloqueia a verificação de FK; a que o Hibernate usa) / falha em vez de esperar / pula linhas travadas (fila em tabela) | P2.02 |
| `lock_timeout` / `deadlock_timeout` | Limite de espera por uma trava (padrão: sem limite) / tempo antes de verificar ciclo (padrão 1 s) | P2.02, P2.03 |
| `@Retryable` / `@EnableResilientMethods` | *Retry* nativo do Spring Framework 7 (`includes`, `maxRetries`, `delay`, `jitter`, `multiplier`); deve ficar **fora** da transação | P2.02 |
| *Deadlock* (no banco) / `pg_blocking_pids` | Ciclo de espera entre transações; o banco aborta uma / função que diz quem bloqueia um processo | P2.03 |
| Plano de execução / `EXPLAIN (ANALYZE, BUFFERS)` | Estratégia que o planejador escolhe / mostra o plano **real** com tempos, linhas e páginas (no PostgreSQL 18, `BUFFERS` já é automático) | P2.04 |
| `Seq Scan` / `Index Scan` / `Index Only Scan` / `Bitmap Heap Scan` | Varredura completa / índice + heap / só o índice (`Heap Fetches: 0`) / páginas candidatas lidas em ordem física | P2.04 |
| `Rows Removed by Filter` / estimado × real / `Index Searches` | Linhas lidas e descartadas / divergência de estatística / buscas na árvore por nó (campo novo do PostgreSQL 18) | P2.04 |
| `pg_stat_statements` / `auto_explain` | Extensão que soma o custo por consulta / registra o plano das consultas lentas no log | P2.04 |
| Plano genérico / `prepareThreshold` | Plano sem conhecer o valor do parâmetro (pode ignorar índice parcial) / execuções do `pgjdbc` antes de preparar no servidor (padrão 5) | P2.04, P2.05 |
| Paginação por cursor (*keyset*) × `OFFSET` | `WHERE (created_at, id) < (último…)`: custo constante / lê e descarta as linhas puladas | P2.04, P2.07 |
| Índice composto: ordem das colunas | Igualdade primeiro, depois intervalo/ordenação; ordem errada = outro índice (260× mais lento no laboratório) | P2.05 |
| Índice parcial / de cobertura (`INCLUDE`) / de expressão | Indexa só um subconjunto (`WHERE …`) / leva colunas extras na folha para `Index Only Scan` / sobre `lower(col)` | P2.05 |
| *Skip scan* (PostgreSQL 18) | Usar um índice multicoluna sem filtro na 1ª coluna (poucos valores distintos); rede de segurança, não projeto | P2.05 |
| `CREATE INDEX CONCURRENTLY` / índice `INVALID` | Cria sem bloquear escritas (fora de transação, mais lento) / resto de uma criação que falhou, ainda custa escrita | P2.05 |
| `spring.flyway.postgresql.transactional-lock` | Com `false` o Flyway usa trava de **sessão**: necessário para `CONCURRENTLY` (padrão trava a migração) | P2.05 |
| Pool de conexões / HikariCP | Reuso de conexões (cada uma é um processo do PostgreSQL) / pool padrão do Boot (`maximum-pool-size` 10, `connection-timeout` 30 s) | P2.06 |
| `connection-timeout` / `max-lifetime` / `keepalive-time` / `leak-detection-threshold` | Espera por uma conexão / idade máxima / ping periódico / aviso de conexão emprestada por tempo demais | P2.06 |
| `Connection is not available, request timed out` | Pool esgotado (`active = total`): procure quem segura a conexão, não aumente o pool às cegas | P2.06 |
| `max_connections` / réplicas × pool | Limite de conexões do banco / `n × maximum-pool-size` (contando o *rolling deploy*) deve caber nele | P2.06 |
| *Bag* / `MultipleBagFetchException` | `List` sem ordem mapeada por `@OneToMany` / erro ao *fetch join* de duas coleções | P2.07 |
| *Fetch graph* / `@EntityGraph` / *batch fetching* (`default_batch_fetch_size`) | Plano de carga declarativo / carrega associações `LAZY` em lotes `IN (…)` | P2.07 |
| Projeção (interface, `record`, `select new`) | Leitura de só algumas colunas, sem entidade gerenciada | P2.07 |
| `hibernate.generate_statistics` / `Statistics` | Contadores do Hibernate (comandos preparados, hits do cache de 2º nível) usados em testes de contagem | P2.07 |
| *Batching* (`hibernate.jdbc.batch_size`, `order_inserts`, `reWriteBatchedInserts`) | Enviar `INSERT`s em lote (1 `PreparedStatement` em vez de N); não funciona com `IDENTITY` | P2.08 |
| Cache de 1º / 2º nível do Hibernate | Contexto de persistência (por transação) / cache da `SessionFactory` compartilhado (dado velho se alterado por fora) | P2.08 |
| `JdbcClient` | API fluente do Spring 6.1+ para SQL direto (parâmetros nomeados, mapeamento para `record`); não tem `batchUpdate` | P2.08 |
| jOOQ | Biblioteca que gera código do esquema e permite SQL tipado; Community Edition sob Apache 2.0 | P2.08 |
| *Cache-aside* / `@Cacheable` / `@CacheEvict` / `sync` | Ler o cache e, se faltar, o banco e gravar / anotações do Spring Cache / uma carga só (no Redis exige escritor com trava) | P2.09 |
| TTL / *cache stampede* / penetração | Validade de uma entrada / todos faltam ao mesmo tempo e vão ao banco / consultas a chaves que nunca existem | P2.09 |
| `RedisCacheWriter` (`immediateWrites`, `cacheLocking`) / `transactionAware()` | Escritas do cache são **assíncronas por padrão** / forçar imediatas / adiar `put`/`evict` até o *commit* | P2.09 |
| `CacheErrorHandler` | Tratador que faz o cache degradar para o banco quando o Redis cai | P2.09 |
| `maxmemory-policy` (`noeviction`, `allkeys-lru`) | O Redis recusa escritas (padrão) / descarta as menos usadas (indicada para cache) | P2.09 |
| NoSQL (documento, chave-valor, grafo, séries, busca) | Modelos não relacionais, cada um com consistência/consulta/escala trocadas | P2.10 |
| Padrões de acesso / partição quente / `ConditionExpression` | Desenhar o armazenamento a partir das consultas / chave com carga desproporcional / escrita condicional (a `UNIQUE`/`@Version` do DynamoDB) | P2.10 |
| Modelo de leitura / visão materializada (`REFRESH … CONCURRENTLY`) | Cópia derivada e pré-agregada para leitura (defasada) / a do PostgreSQL, recalculada por inteiro | P2.11 |
| Bloqueante × não bloqueante / síncrono × assíncrono | A *thread* fica parada esperando × segue / o chamador precisa da resposta para continuar × não | P3.01 |
| I/O-bound × CPU-bound / Lei de Little (`L = λ × W`) | Tempo gasto esperando × calculando / concorrência = taxa × tempo de cada requisição | P3.01 |
| `@Async` / `TaskDecorator` / `AsyncUncaughtExceptionHandler` | Executar em outra *thread* via *proxy* / copiar contexto (MDC) para ela / tratar exceção de método `void` | P3.02 |
| `@Scheduled` (`fixedRate`, `fixedDelay`, `cron`) / ShedLock (`lockAtMostFor`, `lockAtLeastFor`) | Agendamento local a cada instância / exclusão mútua com tempo via tabela (teto e piso do *lock*) | P3.02 |
| *Exchange*, *binding*, *routing key* (*direct*, *topic*, *fanout*) | Balcão que roteia mensagens às filas conforme a chave e as ligações | P3.03 |
| `ack` / `nack` / `requeue` / *prefetch* / *publisher confirm* | Confirmar, rejeitar, devolver à fila / mensagens não confirmadas por consumidor / o *broker* confirma ao produtor | P3.03 |
| *Dead-letter exchange* (DLX) / DLQ / `x-death` | Destino de mensagens rejeitadas ou expiradas, com o motivo registrado | P3.03, P3.08 |
| Tópico, partição, *offset*, *record* (Kafka) | Log particionado e ordenado só dentro da partição; posição de leitura | P3.04 |
| *Consumer group* / *rebalance* / *lag* / `max.poll.interval.ms` | Consumidores que dividem partições / redistribuição / mensagens ainda não lidas / limite entre `poll()`s antes da expulsão | P3.04 |
| Retenção × compactação (`cleanup.policy`) / *tombstone* | Apagar por tempo/tamanho × manter a última por chave (o segmento ativo não compacta) / valor nulo que apaga a chave | P3.05 |
| `AckMode` / `DefaultErrorHandler` / `KafkaTemplate` / `@KafkaListener` | Quando o Spring Kafka confirma o *offset* / tratador de erro (repete e envia ao DLT) / produtor / ouvinte | P3.05 |
| *At-most-once* / *at-least-once* / *exactly-once* / *effectively-once* | No máximo uma (pode perder) / ao menos uma (pode duplicar) / exatamente uma dentro do Kafka / *at-least-once* + idempotência | P3.06 |
| Produtor idempotente / transação do Kafka / `read_committed` | Deduplica *retries* do produtor / escrita atômica Kafka→Kafka / consumidor que só vê transações confirmadas | P3.06 |
| Idempotência / chave de idempotência / tabela de mensagens processadas | Repetir produz o mesmo efeito / `eventId` do **produtor** / marca única na mesma transação do efeito | P3.07 |
| *Backoff* exponencial / *jitter* / mensagem venenosa (*poison message*) | Esperar cada vez mais / aleatoriedade para não repetir juntos / mensagem que sempre falha | P3.08 |
| `@RetryableTopic` / DLT | Retentativas não bloqueantes em tópicos próprios (perde ordem) / tópico de mensagens esgotadas | P3.08 |
| Padrão Outbox / relé / `FOR UPDATE SKIP LOCKED` / escrita dupla | Evento gravado na mesma transação e publicado depois / quem publica / vários relés sem repetir linhas / banco + *broker* sem transação conjunta | P3.09 |
| CDC (*Change Data Capture*) / Debezium | Ler o log de transações para publicar mudanças | P3.09 |
| Webhook / assinatura HMAC / proteção contra *replay* | O servidor deles chama o seu endpoint / prova de autenticidade sobre os bytes + *timestamp* / janela de tempo | P3.10 |
| `PaymentRequest` (QR code) / `PENDING → PAID \| EXPIRED` | Cobrança com validade e máquina de estados terminal | P3.10 |
| SSE (`text/event-stream`, `Last-Event-ID`) / WebSocket / *heartbeat* | Servidor → cliente sobre HTTP com retomada / canal bidirecional / sinal para manter a conexão viva | P3.11 |
| `Mono` / `Flux` / *backpressure* / *event loop* / `boundedElastic` | Fluxos reativos de 0..1 / 0..N / o consumidor controla o ritmo / poucas *threads* que não podem bloquear / *pool* para chamadas bloqueantes | P3.12 |
| Conciliação de pedido × débitos / lançamento de compensação (`reversal_of`) | Consulta que acha duplicidade / estorno por novo lançamento (regra 5) | P3.13 |
| Arquitetura em camadas / pacote por camada × por funcionalidade | Organização por responsabilidade técnica × por área de negócio (só `public` atravessa pacotes) | P4.01 |
| Regra de dependência / modelo anêmico | Cada camada só conhece a de baixo (ou o núcleo) / domínio sem comportamento | P4.01, P4.02 |
| Arquitetura hexagonal (portas e adaptadores) / Onion / Clean Architecture | Núcleo define **portas**, a infraestrutura as implementa; mesmo princípio, outros desenhos | P4.02 |
| Porta de entrada (*driving*) / porta de saída (*driven*) / adaptador | O que o mundo pede / o que o núcleo precisa / classe de fora que traduz | P4.02 |
| SOLID (S, O, L, I, D) / DIP × DI / regra de três | Cinco princípios de design / quem define a interface × quem entrega a dependência / abstrair só na 3ª repetição | P4.03 |
| Teste de contrato (LSP) | Um teste escrito uma vez e rodado contra toda implementação de uma porta | P4.03, P4.07 |
| Strategy / Factory (estática) / Builder | Algoritmo que varia numa interface / criação com nome e regra / objeto imutável com muitos campos opcionais | P4.04 |
| Adapter (ACL) / Decorator / Observer / Template Method | Traduz interface / acrescenta comportamento por composição (proxies do Spring) / publica-assina / esqueleto fixo com passos variáveis | P4.05 |
| Entidade / *value object* / agregado (raiz) / invariante | Identidade e ciclo de vida / valor imutável / grupo com fronteira de consistência / regra sempre verdadeira dentro dele | P4.06 |
| `Money` / projeção (`BalanceView`) | Value object de dinheiro / modelo de leitura sem invariantes | P4.06 |
| Repositório (DDD) / porta `AccountStore` | Coleção de raízes de agregado definida pelo núcleo / sua interface | P4.07 |
| Evento de domínio × evento de integração | Fato interno em memória entre módulos × contrato durável entre sistemas (Outbox/Kafka) | P4.07 |
| `@EventListener` / `@TransactionalEventListener` / `@ApplicationModuleListener` | Síncrono na mesma transação / depois do commit / assíncrono depois do commit em transação própria | P4.07, P4.09 |
| *Bounded context* / linguagem ubíqua / subdomínio (núcleo, suporte, genérico) | Fronteira de modelo e de significado / vocabulário único do negócio ao código / onde investir | P4.08 |
| Mapa de contexto (cliente-fornecedor, *published language*, ACL, *conformist*, *shared kernel*) | Relações entre contextos | P4.08 |
| Event Storming | Oficina de eventos no passado para achar fronteiras | P4.08 |
| Monólito modular / Spring Modulith (`verify()`, `allowedDependencies`, `@NamedInterface`, `OPEN`) | Um deployável com módulos de fronteira verificada / lista branca de dependências / subpacote exposto / módulo sem esconder | P4.09 |
| `@ApplicationModuleTest` / `Scenario` / `Documenter` | Teste de um módulo isolado / espera de evento sem `sleep` / documentação gerada do código | P4.09 |
| ArchUnit (`ArchRule`, `check` × `evaluate`, `failOnEmptyShould`, `FreezingArchRule`) | Regras de arquitetura como testes / regra que falha quando nada casa / congelar violações antigas | P4.10 |
| Acoplamento por dado / "um dono por tabela" | Módulos ligados pelo schema (SQL em `String`) / só o dono lê e escreve a tabela | P4.11 |
| Microsserviço / monólito distribuído / "taxa" dos microsserviços | Serviço implantável **sozinho** com dados próprios / serviços que não funcionam nem se implantam sozinhos / custo de rede, dados, operação e contratos | P5.01 |
| Disponibilidade em cadeia | Produto das disponibilidades dos componentes **síncronos** (5 × 99,9% ≈ 99,5%) | P5.01 |
| Banco por serviço / estrangulamento (*strangler fig*) / modo sombra | Cada serviço é dono dos seus dados / migrar crescendo ao lado do módulo antigo / novo componente só registra o que faria, para comparar | P5.02 |
| gRPC / Protocol Buffers (`.proto`, `reserved`) / *deadline* | RPC sobre HTTP/2 com contrato gerado / formato binário com campos numerados / prazo da chamada | P5.03 |
| Compatibilidade para trás / para frente / leitor tolerante (*Tolerant Reader*) | Leitor novo lê dado antigo / leitor antigo lê dado novo / consumidor que ignora o desconhecido e valida o que usa | P5.04 |
| Expandir → migrar → contrair | Mudança incompatível em três passos (publicar as duas formas, migrar consumidores, remover a antiga) | P5.04 |
| API Gateway / `trusted-proxies` / `StripPrefix` | Porta de entrada única / quem o gateway aceita como proxy para `X-Forwarded-*` / filtro que remove segmento do caminho | P5.05 |
| Descoberta de serviços (no cliente × na plataforma) / Config Server / `ConfigMap` | Como achar o outro serviço / servidor de propriedades em Git / objeto de configuração do Kubernetes | P5.06 |
| *Timeout* (conexão, leitura, total) / *retry* / *backoff* + *jitter* / amplificação de *retry* | Prazos de espera / repetir após falha transitória / pausa crescente e aleatória / camadas multiplicando as tentativas (3³ = 27) | P5.07 |
| Resilience4j / `@Retry` `@CircuitBreaker` `@Bulkhead` `@RateLimiter` `@TimeLimiter` / `fallbackMethod` | Biblioteca de resiliência / anotações e **ordem** dos aspectos / método chamado quando tudo falha | P5.07, P5.08 |
| *Circuit breaker* (CLOSED / OPEN / HALF_OPEN) | Disjuntor: corta chamadas a um serviço doente e testa a volta | P5.08 |
| *Bulkhead* / *rate limiter* / Lei de Little (`L = λ × W`) | Antepara que limita concorrência / limite de taxa / concorrência = taxa × tempo | P5.08 |
| Teste de contrato / Pact / pacto / `@State` / `can-i-deploy` | Verifica que consumidor e provedor combinam / ferramenta guiada pelo consumidor / arquivo do contrato / precondição do provedor / pergunta ao Broker se pode implantar | P5.09 |
| Falha em cascata | Um serviço lento esgota os recursos do chamador e derruba tudo | P5.10 |
| Observabilidade / três pilares (logs, métricas, traces) | Capacidade de investigar o que você não previu olhando o que o sistema emite / eventos, números agregados e caminhos de uma requisição | P6.01 |
| Log estruturado / ECS / MDC | Evento com campos (JSON) em vez de frase / esquema comum de nomes de campo (Elastic Common Schema) / contexto por *thread* que o Logback põe em toda linha | P6.01 |
| *Correlation ID* (`requestId`) × `traceId` | Chave de suporte, do cliente e validada / chave técnica, do sistema, 32 *hex* (W3C), que liga log, métrica e traço | P6.01 |
| *Log injection* | Dado do atacante com quebra de linha ou JSON forja linhas de log | P6.01 |
| Micrometer / *Counter* / *Gauge* / *Timer* / `Observation` | Fachada de métricas / só sobe / valor agora / tempo e distribuição / uma instrumentação que gera métrica e span | P6.02 |
| Cardinalidade (de etiquetas) / série temporal | Número de combinações de etiquetas / `nome{etiquetas}` com amostras no tempo; o produto das etiquetas × baldes derruba o Prometheus | P6.02 |
| Porta de gestão / papel `METRICS` | Porta separada do Actuator, não publicada / papel só de leitura do `/actuator/prometheus` para o *scrape* | P6.02 |
| Prometheus / *scrape* / PromQL / `up` | Banco de séries temporais por *pull* / a coleta periódica / a linguagem de consulta / métrica 1 ou 0 do *scrape* | P6.03 |
| RED / USE / quatro sinais de ouro | Rate, Errors, Duration (serviços) / Utilization, Saturation, Errors (recursos) / latência, tráfego, erros, saturação (SRE) | P6.03 |
| Histograma / balde (`le`) / percentil por interpolação | Contagem por limite de valor / "menor ou igual a" / estimativa linear dentro do balde (`histogram_quantile`) | P6.03 |
| *Recording rule* / *alerting rule* / `for` | Consulta pré-calculada / condição que dispara / tempo contínuo exigido antes de disparar (*pending* → *firing*) | P6.03, P6.04 |
| Alertmanager / *runbook* / página × ticket / fadiga de alerta | Agrupa, inibe, silencia e roteia alertas / procedimento escrito do alerta / acorda alguém × resolve no expediente / ignorar alertas por excesso de ruído | P6.04 |
| OpenTelemetry / OTLP / *span* / `traceparent` | Padrão aberto de telemetria / protocolo de envio (HTTP 4318, gRPC 4317) / um degrau do traço / cabeçalho W3C que leva o contexto | P6.05 |
| Amostragem (cabeça × cauda) / *exemplar* | Decidir no início × depois quais traços guardar / `traceId` colado a uma amostra de métrica (formato OpenMetrics) | P6.05 |
| *Liveness* / *readiness* / desligamento gracioso | "Preciso reiniciar?" / "posso receber tráfego?" / terminar as requisições em voo antes de sair (`SIGTERM`, código 143) | P6.06 |
| Rastreador de exceções | Serviço que agrupa exceções iguais e mostra frequência e regressão por versão (Sentry, GlitchTip) | P6.06 |
| *Game day* / *postmortem* sem culpa | Ensaio de falha controlada / relato do incidente focado no sistema, com ações com dono | P6.07 |
| Camada (imagem) / tag imutável / *digest* | Pacote de mudanças do sistema de arquivos, guardado uma vez no registro / tag que nunca muda de conteúdo / identificador `sha256` do conteúdo | P7.01 |
| Buildpacks / Jib | Constroem imagem **sem Dockerfile** com camadas e base padronizadas (inclui calculador de memória, SBOM) / monta a imagem de uma app Java sem *daemon* | P7.01 |
| `MaxRAMPercentage` / *container support* | Fração da memória do contêiner usada como *heap* máximo (padrão 25%) / a JVM lê o cgroup (memória e CPU) | P7.02 |
| `OOMKilled` / `Evicted` | O **kernel** matou o contêiner por passar de `limits.memory` (exit 137) / o **nó** sem memória removeu o pod | P7.02, P7.09 |
| NMT (*Native Memory Tracking*) | Recurso da JVM que detalha a memória fora do heap (`jcmd … VM.native_memory`) | P7.02, P7.09 |
| Pod / ReplicaSet / Deployment | Unidade efêmera de contêineres com um IP / mantém N pods iguais / gerencia ReplicaSets e a atualização *rolling* | P7.03 |
| Service / EndpointSlice | Nome e IP estáveis que balanceiam entre pods **prontos** / lista dos endereços dos pods prontos | P7.03 |
| `maxSurge` / `maxUnavailable` | Pods extras / pods a menos permitidos durante um *rollout* | P7.03 |
| Namespace / *label* / *selector* | Espaço de nomes / rótulo chave-valor / consulta por rótulos que liga objetos | P7.03 |
| Ingress / controlador de Ingress / Gateway API | Regras host/caminho → Service (API congelada) / programa que as executa / sucessora (`Gateway`, `HTTPRoute`) | P7.04 |
| ConfigMap / Secret / *Pod Security Standards* | Configuração não secreta / segredo (base64, **não** criptografia) / perfis (*privileged*, *baseline*, *restricted*) | P7.04 |
| `startupProbe` | Sonda de partida: dá tempo à subida e suspende as outras até passar | P7.05 |
| `requests` / `limits` / QoS | Reserva (agendamento, base do HPA) / teto imposto (CPU estrangula, memória mata) / classes Guaranteed, Burstable, BestEffort | P7.05 |
| `preStop` / `terminationGracePeriodSeconds` | Gancho antes do `SIGTERM` (ex.: `sleep`) / tempo total antes do `SIGKILL` | P7.05 |
| PodDisruptionBudget / `topologySpreadConstraints` | Limite de remoções **voluntárias** / espalhamento das réplicas entre nós | P7.05 |
| HPA / `behavior` / *metrics-server* | Escala réplicas por métrica (`ceil(atuais × atual/alvo)`) / velocidade e janelas / serve `metrics.k8s.io` (uso atual) | P7.06 |
| kind / Cloud Provider KIND | Kubernetes com nós em contêineres Docker / dá `LoadBalancer` e Ingress ao kind | P7.07 |
| Helm / chart / release / `values.yaml` | Gerenciador de pacotes (Helm 4) / pacote de templates / instalação com histórico / parâmetros | P7.08 |
| Kustomize | Sobreposições (base + patches) sem templates | P7.08 |

| Pipeline como grafo / caminho crítico / `ci-passed` | Jobs com dependências (`needs`); o tempo é o do caminho mais longo; um único *job* agregador como check obrigatório (recusa `skipped`) | P8.01 |
| `actionlint` / `shellcheck` / mutante | Verificador de *workflows* do GitHub Actions / de *shell scripts* / defeito plantado para provar que um verificador funciona | P8.01 |
| DORA (métricas) | Tempo de entrega da mudança, frequência de implantação, tempo de recuperação de implantação com falha, taxa de falha de mudança, taxa de retrabalho | P8.01 |
| Trivy / *gate* / `--ignore-unfixed` / `EOSL` | Scanner de vulnerabilidades e configuração / portão que reprova o build / omite achados sem correção / fim de suporte da distribuição | P8.02 |
| SBOM / CycloneDX | Inventário do que há na imagem / formato do inventário | P8.02 |
| Proveniência / atestação / SLSA | Registro de quem construiu um *digest* e de onde / publicado pelo `actions/attest` / modelo de níveis de confiança da cadeia | P8.02 |
| *Recreate* / *rolling* / *blue-green* / *canary* | Parar e subir / trocar uma instância por vez / dois ambientes e troca de roteador / fatia de tráfego observada | P8.03 |
| *Expand/contract* (*parallel change*) | Mudar o banco em 3 *releases* compatíveis com a versão antiga e a nova | P8.03 |
| *Feature flag* / *kill switch* / *rollout* percentual / sal | Interruptor de funcionalidade / desligar sem deploy / *hash* estável de `flag:usuário` em baldes / a chave do *flag* no *hash* para independência | P8.04 |
| IaC / Terraform / *provider* / `resource` / `data` | Infraestrutura como código / ferramenta declarativa / *plugin* da nuvem / objeto gerenciado / objeto lido | P8.05 |
| `plan` / `apply` / *state* / *drift* | Diferença calculada / execução do plano / mapa código ↔ objetos reais / divergência entre código e nuvem | P8.05 |
| `count` × `for_each` | Identidade por posição × por chave (remover o 1º item desloca índices no `count`) | P8.05 |
| Módulo / `moved` / `import` / `bootstrap` | Pasta reutilizável / muda o endereço no *state* sem tocar na nuvem / adota recurso existente no `plan` / cria o balde do *state* com *state* local | P8.06 |
| `use_lockfile` / `.tflock` | Bloqueio nativo do S3 por escrita condicional; falha com `412 PreconditionFailed` | P8.06 |
| `terraform test` / `mock_provider` | Testes de módulo sem nuvem / *provider* de mentira que inventa valores calculados | P8.06 |
| `-detailed-exitcode` / `prevent_destroy` | `plan` com código 0 (sem mudanças), 1 (erro), 2 (mudanças) / recusa destruir o recurso | P8.07 |
| GitOps / *pull* × *push* / Argo CD / Flux | Estado desejado no Git reconciliado por um agente / o cluster busca × o pipeline empurra / controladores de GitOps | P8.08 |
| `Jenkinsfile` / agente (*agent*) / *Shared Library* | Pipeline como código do Jenkins / máquina que executa estágios / reuso entre pipelines | P8.08 |
| VPC / sub-rede / tabela de rotas / IGW | Rede virtual isolada / fatia dela numa AZ (a AWS reserva 5 IPs) / diz para onde vai cada destino / porta da VPC para a internet | P9.01 |
| Sub-rede pública × privada | "Pública" = **rota** `0.0.0.0/0` para o IGW (o nome não importa); privada não tem | P9.01 |
| NAT Gateway / *endpoint* de gateway / *endpoint* de interface (PrivateLink) | Saída para a internet de quem não tem IP público (cobra por hora e por GB) / rota privada ao S3 (sem cobrança adicional) / ENI privada para um serviço (cobra por hora e por AZ) | P9.01 |
| Security Group × NACL | Firewall **com estado** na interface (só permite; pode referenciar outro SG) × **sem estado** na sub-rede (permite e nega) | P9.01 |
| ALB / NLB / *target group* / *deregistration delay* | Balanceador de camada 7 / de camada 4 / alvos e *health check* / espera ao tirar uma tarefa do rodízio (padrão 300 s) | P9.01, P9.02 |
| ECS / Fargate / definição de tarefa / tarefa / serviço | Orquestrador de contêineres da AWS / execução sem servidores / "receita" versionada em revisões / uma execução (ENI própria em `awsvpc`) / mantém N tarefas | P9.02 |
| *Task role* × *execution role* | Credencial **do seu código** × credencial **do agente do ECS** (ECR, logs, ler segredos a injetar) | P9.02 |
| *Deployment circuit breaker* | Interrompe e reverte um *deploy* cujas tarefas não ficam saudáveis (não pega "sobe e erra") | P9.02 |
| *Target tracking* / `ignore_changes` | Autoescala que mantém uma métrica no alvo / "este campo tem outro dono" no Terraform | P9.02 |
| EKS / Auto Mode / Pod Identity / *access entry* | Kubernetes gerenciado (cobra por cluster por hora) / a AWS também gerencia os nós / credencial AWS por *pod* sem OIDC / vínculo IAM → RBAC | P9.03 |
| SQS / fila padrão × FIFO / DLQ / `maxReceiveCount` | Fila gerenciada (*at-least-once*, ordem "melhor esforço") × ordem por grupo e deduplicação / fila de mensagens que falharam / nº de recebimentos antes da DLQ | P9.04 |
| *Visibility timeout* / *long polling* | Tempo em que a mensagem recebida fica invisível (30 s a 12 h) / `ReceiveMessage` espera até 20 s | P9.04 |
| SNS / *fan-out* / entrega crua / filtro por atributo | Tópico publica/assina / um tópico, uma fila por consumidor / a fila recebe só o evento / só quem casa recebe | P9.04 |
| EventBridge (clássico × novo) | Barramento de eventos por regras e alvos / o novo tem retenção, entrega ordenada e *replay* | P9.04 |
| Lambda / Init / Restore / *cold start* | Função sem servidor / fase que roda o código estático / fase do SnapStart / custo de criar um ambiente novo | P9.05 |
| SnapStart / *provisioned concurrency* / AppCDS / cache AOT | Restaura um *snapshot* pós-Init (só versão publicada) / N ambientes sempre prontos / arquivo de classes da aplicação / cache de carregamento e ligação do JDK | P9.05 |
| Concorrência (`L = λ × W`) | Requisições em andamento = taxa × duração; cada uma usa um ambiente | P9.05 |
| Secrets Manager / Parameter Store / `valueFrom` | Cofre com versões e rotação / configuração hierárquica (padrão grátis) / injeção de segredo no ECS (só na partida) | P9.06 |
| `AWSCURRENT` / `AWSPREVIOUS` / janela de coexistência | Estágios de versão do segredo / tempo em que a chave velha e a nova valem ao mesmo tempo | P9.06 |
| *Ephemeral* / *write-only* (Terraform) | Valor que existe só durante o `apply` / argumento enviado ao provedor e **não** guardado no *state* (`secret_string_wo`) | P9.06 |
| CloudWatch / alarme (`OK`, `ALARM`, `INSUFFICIENT_DATA`) / `treat_missing_data` | Métricas, logs e alarmes da AWS / estados / o que fazer quando não há ponto | P9.07 |
| Filtro de métrica / EMF / OTLP / alarme composto | Linha de log vira métrica / métrica embutida no log / caminho recomendado para métricas / combina estados de outros alarmes | P9.07 |
| Multi-AZ / réplica de leitura / `ReplicaLag` | *Standby* síncrono (não lê) / cópia assíncrona que atende leitura / atraso da réplica (primário ocioso: até 5 min no PostgreSQL) | P9.08 |
| *Read-your-writes* / LSN | Ler a própria escrita / posição do WAL: esperar a réplica alcançá-la | P9.08 |
| *Failover* / TTL de DNS da JVM (`networkaddress.cache.ttl`) | Troca automática para o *standby* (DNS muda; 60–120 s) / quanto a JVM guarda um endereço (padrão medido: 30 s) | P9.08 |
| PITR / `deletion_protection` / RDS Proxy | Restaurar a qualquer instante da retenção / recusa excluir / *pool* gerenciado na frente do banco | P9.08 |
| OAuth 2.0 / OpenID Connect (OIDC) | Autorização delegada (um cliente acessa uma API em nome do usuário) / camada de **autenticação** sobre ele (ID token, `userinfo`, descoberta) | P10.01 |
| Dono do recurso / cliente / servidor de autorização / servidor de recursos | Os quatro papéis do OAuth 2.0 (a pessoa / o app / o Keycloak / a API) | P10.01 |
| *Access token* / *ID token* / *refresh token* | Escrito para a **API** / para o **app** (quem logou) / para o **servidor de autorização** (renovar) | P10.01 |
| Authorization Code + PKCE | Fluxo com navegador: código de uso único + prova (`code_verifier`) de que quem troca é quem iniciou | P10.01 |
| `state` / `nonce` / `redirect_uri` / `iss` | Anti-CSRF do fluxo / anti-repetição do ID token / endereço de retorno **exato** / quem respondeu (RFC 9207) | P10.01 |
| Escopo / papel / audiência (`aud`) / `azp` | O que o **app** pode / o que a **pessoa** é / para qual **API** o token vale / qual **cliente** pediu o token | P10.01 |
| JWKS / `kid` / RS256 | Conjunto de chaves **públicas** do emissor / qual chave assinou / assinatura assimétrica (quem confere não forja) | P10.01 |
| *Refresh token* com rotação | Cada uso devolve um novo; reusar o antigo derruba a sessão (sinal de roubo) | P10.01 |
| Realm / client / client scope / *protocol mapper* / service account | Espaço isolado do Keycloak / aplicação / pacote de claims / regra que escreve uma claim / usuário de um cliente de serviço | P10.02 |
| Cliente público × confidencial | Não guarda segredo (PKCE) × guarda `client_secret` e o prova no *endpoint* de token | P10.02 |
| Escopo padrão × opcional | Vem sempre × só se o cliente o pedir (e tiver o direito) | P10.02 |
| *Resource Server* / `JwtDecoder` / validador de token | A API que confere tokens / quem verifica assinatura e claims / regra extra (`aud`, `typ`) | P10.03 |
| `SCOPE_x` / `ROLE_x` / `hasAuthority` / `hasRole` | Autoridades do Spring vindas do escopo e do papel do token | P10.03 |
| `WWW-Authenticate` (`invalid_token`, `insufficient_scope`, `resource_metadata`) | Cabeçalho do 401/403 que diz **por que** recusou (RFC 6750) e onde o recurso se descreve (RFC 9728) | P10.03 |
| Mutante (de segurança) | Estraga **uma** regra para provar que algum teste falha; o **sobrevivente** pergunta se o controle é redundante ou o teste é fraco | P10.03 |
| Introspecção de token | Perguntar ao servidor de autorização se o token está ativo (revogação imediata, custo por chamada) | P10.03 |
| *Client Credentials* / token de máquina | Fluxo sem usuário: o serviço troca `client_id` + segredo por um token curto (sem *refresh*) | P10.04 |
| mTLS / troca de token (RFC 8693) | Identidade por certificado de cliente / "em nome de" com escopo reduzido (**não executados**) | P10.04 |
| Usuários alternados (rotação) | Dois usuários do banco que se revezam: altera-se o **inativo**, troca-se, e só depois invalida-se o antigo | P10.05 |
| Janela de coexistência | Tempo em que a credencial velha e a nova valem juntas (releitura + deploy + `max-lifetime`) | P10.05 |
| Menor privilégio (banco) | Aplicação sem DDL e sem `UPDATE`/`DELETE` em `ledger_entries`; migração e mestra são outras contas | P10.05 |
| SCRAM-SHA-256 / `pg_authid` | Verificador de senha do PostgreSQL (não a senha) / catálogo que a aplicação não lê | P10.05 |
| KMS / chave gerenciada pelo cliente | Chave de criptografia com política, auditoria e rotação própria (**não executado**) | P10.05 |
| SBOM / CycloneDX / SPDX / `purl` | Inventário de componentes / formato / outro formato / identificador universal do pacote | P10.06 |
| Atestação / assinatura por digest (cosign) | Afirmação assinada sobre um digest / liga uma chave a **este conteúdo** | P10.06 |
| Portão de *deploy* (`verify-before-deploy.sh`) | Só segue com digest **assinado** (saída `0`; `1` sem assinatura; `2` referência por tag) | P10.06 |
| Assinatura sem chave (*keyless*) / Fulcio / Rekor | Certificado de vida curta pela identidade OIDC do workflow / autoridade / registro de transparência (**não executado**) | P10.06 |
| Build reprodutível (`project.build.outputTimestamp`) | Dois *builds* geram o mesmo artefato (medido só para o JAR) | P10.06 |
| *Threat modeling* / DFD / fronteira de confiança | Pensar como atacante antes de construir / diagrama de fluxo de dados / onde a confiança muda | P10.07 |
| STRIDE (por elemento) | Spoofing, Tampering, Repudiation, Information disclosure, DoS, Elevation; processo STRIDE, armazenamento TRID, fluxo TID, entidade externa SR | P10.07 |
| Mitigar / aceitar / transferir / evitar | Os quatro tratamentos de uma ameaça; **aceitar** exige dono, motivo e data de revisão | P10.07 |
| Escala vertical / horizontal / por partição | Máquina maior / mais cópias iguais / cada cópia cuida de um pedaço dos dados | S1.01 |
| Lei de Amdahl / USL | Ganho de paralelizar limitado pela fração serial / acrescenta o custo de coerência | S1.01 |
| Sem estado (*stateless*) | Nada necessário à próxima requisição mora na instância | S1.01 |
| *Least outstanding requests* / P2C | Menos requisições em andamento / sorteia 2 servidores e escolhe o menos ocupado | S1.01 |
| CAP / PACELC | Durante a partição: linearizabilidade × disponibilidade / senão: latência × consistência | S1.02 |
| Linearizabilidade | Cada operação parece ocorrer num instante, numa ordem única (o C do CAP, não o do ACID) | S1.02 |
| Quórum (`R + W > N`) | Leitura e escrita se cruzam em ao menos uma réplica | S1.02 |
| Consistência de sessão (*read-your-writes*, leituras monotônicas) | Você vê o que escreveu e não vê o tempo voltar | S1.02 |
| LWW (*last write wins*) | Vence a escrita de maior *timestamp*; pode descartar a mais nova | S1.02 |
| Líder-seguidor / multi-líder / sem líder | Quem aceita escrita na replicação | S1.03 |
| *Sharding* / hash consistente / nó virtual | Dividir dados entre nós / anel que move ~1/N ao mudar N / muitos pontos por nó para equilibrar | S1.03 |
| Chave quente | Uma chave concentra o tráfego; mais *shards* não resolvem | S1.03 |
| *Cache-aside* / *write-through* / *write-behind* / *write-around* | Padrões de leitura e escrita do cache | S1.04 |
| *Stampede* / *single-flight* / *jitter* | Manada recarregando a mesma chave / só uma carrega / aleatoriedade no TTL | S1.04 |
| `Cache-Control: no-store` | Proíbe qualquer cache de guardar a resposta (dado privado) | S1.04 |
| CQRS / modelo de leitura / projetor | Modelos separados para decidir e responder / tabela derivada / quem a atualiza a partir de eventos | S1.05 |
| Token de versão (`X-Min-Seq`) | Sequência devolvida pelo comando; a leitura espera até ela, com *fallback* à verdade | S1.05 |
| *Event sourcing* / *upcasting* / *snapshot* | Estado = eventos / converter o evento antigo ao ler / estado salvo para encurtar a reidratação | S1.06 |
| 2PC (*two-phase commit*) | Preparar e confirmar com um coordenador; bloqueia se ele cai | S1.07 |
| Saga / compensação / pivô | Transações locais com desfazer semântico / o desfazer / o passo sem volta | S1.07 |
| Orquestração × coreografia | Um coordenador manda / cada serviço reage a eventos | S1.07 |
| Resultado desconhecido | Timeout: o efeito pode ter ocorrido; repetir com a mesma chave ou consultar | S1.07 |
| `Idempotency-Key` / *hash* canônico | Chave do cliente para repetição segura / hash do corpo normalizado (422 se diferir) | S1.08 |
| Janela fixa / deslizante / balde de fichas | Algoritmos de limitação de taxa | S1.09 |
| Modo degradado / *fail-open* / *fail-closed* | Cota local quando o limitador cai / deixa passar / recusa tudo | S1.09 |
| `429` / `Retry-After` | Muitas requisições / quando tentar de novo | S1.09 |
| Relógio de parede × monotônico | Pode voltar e divergir entre máquinas / só avança, local (`nanoTime`) | S1.10 |
| Lamport / vetorial / HLC | Ordem causal parcial / detecta concorrência / tempo físico + contador | S1.10 |
| Feistel (ofuscação) | Permutação reversível que embaralha um contador sem colisão | S1.11 |
| Paradoxo do aniversário | Colisões aparecem com ~√M códigos (≈2,2 milhões para 62^7) | S1.11 |
| Partidas dobradas / conciliação | Todo evento debita e credita o mesmo valor (soma zero) / comparar seu livro com o do PSP | S1.12 |
| Autorizar / capturar / liquidar | Reservar / mover o dinheiro (pivô) / o PSP repassou | S1.12 |
| *Fan-out* na escrita / na leitura / híbrido | Copiar o post para cada seguidor / buscar na leitura / por limiar de seguidores | S1.13 |
| Linha de base (*baseline*) | Medida **antes** da mudança, no mesmo ambiente, para provar o efeito depois | S2.01 |
| Percentil (p50/p95/p99/p99,9) | Valor abaixo do qual está essa fração das medidas; **não** se faz média de percentis (some histogramas) | S2.01 |
| Cinco pecados da otimização | Sem linha de base; várias mudanças juntas; ambiente diferente; uma execução só; não parar | S2.01 |
| JFR (*Java Flight Recorder*) | Gravador de eventos da JVM (amostras de pilha, alocação, espera, GC); `default` contínuo, `profile` na investigação | S2.02 |
| `jfr view` / `jfr print` | Visões prontas (`hot-methods`, `allocation-by-site`, `contention-by-site`, `gc-pauses`) / eventos crus | S2.02 |
| Perfil de CPU × espera | O perfil de CPU só vê *threads* executando; esperas (lock, pool, I/O) são **outros** eventos | S2.02 |
| Evento customizado do JFR | Classe que estende `jdk.jfr.Event`; mede o negócio no mesmo arquivo (sem dado pessoal) | S2.02 |
| Viés de ponto de segurança (*safepoint bias*) | Amostrador que só vê a *thread* em *safepoints* atribui o tempo ao lugar errado | S2.03 |
| `DebugNonSafepoints` | *Flag* diagnóstica: o JIT guarda informação de depuração fora dos *safepoints* (melhora a atribuição) | S2.03 |
| *Flame graph* / pilha colapsada | Gráfico em que a largura é a proporção de amostras / formato `raiz;...;folha contagem` | S2.03 |
| Piso do heap | Uso do heap **depois do GC**; se sobe com o tempo, há vazamento | S2.04 |
| `OutOfMemoryError` × `OOMKilled` | A JVM sem heap (deixa *dump*) × o kernel matando o contêiner (sem *dump*) | S2.04 |
| *Heap dump* / histograma de classes | Foto do heap (`.hprof`, **dado sensível**) / instâncias e bytes por classe (`jcmd GC.class_histogram`) | S2.04 |
| Tamanho raso × retido | Tamanho do objeto × o que seria liberado se ele fosse coletado (acha o culpado) | S2.04 |
| `OldObjectSample` | Evento do JFR que amostra objetos que sobrevivem e registra o **caminho até a raiz** | S2.04 |
| Folga do heap | Heap maior que o conjunto vivo; sem ela o GC roda o tempo todo (G1) ou o alocador espera (ZGC) | S2.05 |
| *Allocation stall* | Espera da aplicação por memória enquanto o coletor concorrente (ZGC) recicla | S2.05 |
| Carga aberta × fechada | Chegadas em taxa fixa (API pública) × N usuários que só mandam a próxima depois da resposta | S2.06 |
| Omissão coordenada (*coordinated omission*) | O gerador fechado espera o servidor lento e **omite** as medições do período ruim (subestima a cauda) | S2.06 |
| Curva de saturação / joelho | Latência × taxa; o joelho é onde a fila começa (~95% da capacidade `N/W`) | S2.06 |
| `thresholds` do k6 | Critérios de aprovação do teste; falhando, o k6 sai com código **99** | S2.06 |
| CDS / AppCDS / cache AOT | Classes pré-processadas do JDK / do app / cache do JDK 24-25 (classes, ligação e perfis) para partida rápida | S2.07 |
| CRaC | *Checkpoint/restore* do processo da JVM; exige fechar sockets antes; o *snapshot* contém a memória (sensível) | S2.07 |
| *Native image* (GraalVM) | Executável compilado antes: partida e memória baixas, pico menor, mundo fechado | S2.07 |
| JMH / `Blackhole` / `baseline` | *Harness* de microbenchmarks / consumidor de resultado / benchmark vazio (o piso do arcabouço) | S2.08 |
| Eliminação de código morto / dobramento de constantes | O JIT apaga o cálculo não usado / calcula na compilação o que é constante (armadilhas do microbenchmark) | S2.08 |
| *Expand/contract* (*parallel change*) | Mudança incompatível em fases compatíveis: **adicionar** o novo, **migrar** dados e leitura, **remover** o antigo por último | S3.01 |
| Fila de *locks* / `lock_timeout` | Um `ALTER` esperando bloqueia **as consultas novas atrás dele**; `lock_timeout` o faz **desistir** em vez de formar fila | S3.01 |
| `NOT VALID` / `VALIDATE CONSTRAINT` | Restrição que vale **só para linhas novas** (instantânea) e é **validada depois** sem bloquear escrita | S3.01 |
| *Backfill* | Preencher a coluna nova nos dados antigos, **em lotes** por chave, com `COMMIT` por lote e **fora** da migração | S3.01 |
| Particionamento declarativo | Tabela lógica dividida em partições (`RANGE`/`LIST`/`HASH`); **poda** exclui as partições fora do filtro | S3.02 |
| Poda de partições (*pruning*) | O planejador ignora partições que o filtro na **chave** exclui (precisa de limites **fechados**) | S3.02 |
| Partição `DEFAULT` | Recebe o que não cabe em nenhuma faixa; **impede** `DETACH ... CONCURRENTLY` e esconde a falta de partição | S3.02 |
| Troca por *attach* | Converter a tabela existente: **renomear** a antiga, criar a particionada e **anexar** a antiga como partição (com `CHECK` validado) | S3.02 |
| `write`/`flush`/`replay lag` | Atrasos da réplica: WAL **recebido**, **gravado em disco**, **aplicado** (o que o usuário enxerga) | S3.03 |
| Conflito de recuperação | Consulta longa na réplica **cancelada** porque o primário limpou versões de que ela precisava (`hot_standby_feedback` troca isso por *bloat*) | S3.03 |
| `LazyConnectionDataSourceProxy` | Adia a conexão real para a **primeira instrução**, quando a flag `readOnly` já existe (sem ele o roteamento vai sempre ao primário) | S3.03 |
| Token LSN (*read-your-writes*) | Posição do WAL **após o commit**; a leitura só usa a réplica se ela **já aplicou** esse LSN | S3.03 |
| Saldo de fechamento (`period_balances`) | Saldo líquido por conta e período gravado **antes** de arquivar, para o invariante `abertura + vivo = saldo` | S3.04 |
| Arquivamento (camadas quente/morna/fria) | Mover dado antigo para armazenamento barato com **manifesto**, `sha256` e **restauração provada** antes de remover | S3.04 |
| Índice invertido / análise | Termo → documentos; o analisador normaliza (minúsculas, acentos, *stopwords*, *stemming*) | S3.05 |
| `pg_trgm` / `tsvector` | Trigramas (aceita curinga e erro de digitação, índice grande) × *full-text* por palavras (índice pequeno, relevância `ts_rank`) | S3.05 |
| OpenSearch / Elasticsearch | Motores de busca (Lucene) para relevância, facetas e escala; **cópia derivada**, consistência eventual | S3.05 |
| CDC (*Change Data Capture*) | Ler o **log de transações** e emitir as mudanças na ordem do *commit*, sem *polling* | S3.06 |
| *Slot* de replicação lógica / `REPLICA IDENTITY` | Marca até onde o consumidor leu (o banco **retém WAL**) / quanto do valor antigo aparece em `UPDATE`/`DELETE` | S3.06 |
| `unchanged-toast-datum` | Marcador de coluna grande **não alterada** no evento de CDC (o valor não vem) | S3.06 |
| Debezium / Event Router | Conector do Kafka Connect para CDC / transformação que lê a tabela outbox (**só inserções**) e roteia ao tópico | S3.06 |
| BRIN | Índice de **mínimo e máximo por faixa de páginas**: minúsculo, bom para colunas que seguem a ordem física (tempo *append-only*) | S3.07 |
| Escada de analytics | Primário → réplica → visão materializada → resumo incremental → armazém → lago; suba **um degrau** só quando o anterior falhar medido | S3.08 |
| Janela recalculada (`=` × `+=`) | Carga incremental que **recalcula** os dias recentes (idempotente) em vez de somar; a reconciliação pega o que escapa | S3.08 |
| Esquema em estrela / granularidade | Fato (evento com números) cercado de dimensões; a **granularidade** (1 linha = 1 quê) evita somas erradas | S3.08 |
| Portão (*gate*) de migração | Critério **medido** para passar à fase seguinte (deriva = 0, 0 falhas, divergência da leitura-sombra = 0) | S3.09 |
| Mutante (migração) | A mesma execução **sem** a proteção, para provar que a verificação **pode falhar** (~100 mil divergências sem a *trigger*) | S3.09 |
| Leitura-sombra | Ler as duas colunas, devolver a antiga e **contar divergências** (sem logar o dado) antes de trocar a leitura | S3.09 |
| SLI | Indicador de nível de serviço: **razão** de eventos bons ÷ válidos medida perto do usuário (ex.: respostas não 5xx ÷ transferências) | S4.01 |
| SLO / SLA | Meta interna do SLI numa janela (99,9% em 30 d) / contrato com consequência; o SLO é mais exigente que o SLA | S4.01 |
| *Error budget* (orçamento de erro) | `1 − SLO`: a falha aceitável (99,9% = 43,2 min ou 12.960 requisições a 5 req/s); com orçamento entrega-se, sem ele estabiliza-se | S4.01 |
| *Burn rate* (taxa de queima) | Quantas vezes mais rápido que o ritmo exato o orçamento é gasto: 14,4× esgota em 50 h; alerta por **múltiplas janelas** (longa + curta) | S4.01 |
| Política de *error budget* | Documento assinado por produto e engenharia: o que muda a 50%/25%/0% do orçamento | S4.01 |
| Comando (IC) / operação / comunicação / registro | Papéis do incidente; o comando decide e delega, **não debuga** | S4.02 |
| Severidade (SEV1–3) | Nível do incidente que define quem é acionado e a cadência de comunicação; ligada ao SLO; dinheiro = SEV1 | S4.02 |
| Mitigar antes de diagnosticar | Estancar o dano (rollback, flag, escala) antes de entender a causa raiz | S4.02 |
| `DocLint` / `check-runbooks.sh` | Verificadores do laboratório: seções, dono, passos numerados, culpa, ação sem dono/data; alerta sem *runbook* reprova | S4.02 |
| Postmortem sem culpa | Documento focado no que o **sistema** permitiu: linha do tempo, impacto em orçamento, fatores, o que funcionou/não, ações com dono e data | S4.03 |
| Gatilhos de postmortem | Critérios escritos antes: SEV1, orçamento > 5%, perda de dado, falha de monitoramento | S4.03 |
| Ação prevenir / detectar / mitigar | Tipos de ação de um postmortem, nesta ordem de preferência; toda ação tem dono e data | S4.03 |
| Engenharia do caos | Experimentar no sistema com hipótese sobre um estado estável medido, injetando falha realista para tentar refutá-la | S4.04 |
| Raio de impacto / condição de parada | Quem pode sofrer com o experimento / critério que o aborta sozinho (deve enxergar falhas que **penduram**) | S4.04 |
| *Game day* | Ensaio com pessoas: segue os *runbooks* ao pé da letra e gera um postmortem do ensaio | S4.04 |
| `threads ÷ timeout` | Capacidade de um serviço que espera uma dependência com *timeout*: 16 ÷ 0,213 s ≈ 75 req/s | S4.04 |
| RPO / RTO | Quantos dados posso perder / quanto tempo posso ficar fora; por cenário e por tipo de dado; RTO é a soma de passos | S4.05 |
| Cópia × arquivo × *backup* | Réplica copia o erro; arquivo move dado antigo; só o *backup* (ponto no tempo) volta no tempo | S4.05 |
| Imutabilidade (WORM) / Vault Lock | Backup que ninguém apaga antes do prazo (modos *governance* e *compliance*) | S4.05 |
| Ensaio de restauração | Restaurar periodicamente para uma instância temporária, rodar invariantes e cronometrar o RTO | S4.05 |
| Conciliação depois de restaurar | Reconciliar o que o banco restaurado perdeu e o mundo fora dele (PSP, e-mails, chaves de idempotência) já fez | S4.05 |
| N+1 (capacidade) por AZ | Folga para perder uma AZ: 2 AZs a 60% de CPU viram 120% ao perder uma | S4.06 |
| Falha correlacionada | Falha que atinge as cópias ao mesmo tempo (mesmo *deploy*, segredo, *state*, região); redundância não a cobre | S4.06 |
| *Pilot light* / *warm standby* | Estratégias de DR: núcleo mínimo ligado + dados replicados / cópia funcional reduzida sempre ligada | S4.06 |
| Joelho da curva | Ponto em que a latência explode com a utilização (M/M/4: p99 140 ms a 80%, 400 ms a 95%) | S4.07 |
| Folga (*headroom*) | Utilização que ainda cumpre o SLO **depois** da pior perda prevista (uma AZ) | S4.07 |
| Alerta por prazo (`predict_linear`) | Previsão linear de quando um recurso acaba; a **janela** do intervalo é a decisão (degrau × ciclo) | S4.07 |
| Modelo de capacidade | Tabela por recurso: limite, pico, crescimento, prazo até o limite, ação com gatilho, dono; revisto todo mês | S4.07 |
| Defesa em profundidade | Várias camadas de defesa; só valem se **não falharem pela mesma causa** (5 camadas a 30% → 0,243%; com causa comum de 1% → 1,24%) | S5.01 |
| Causa comum (segurança) | Um evento que derruba várias camadas ao mesmo tempo (mesma credencial, rede, pessoa, configuração, conta) | S5.01 |
| Zero trust | Princípios (NIST SP 800-207): sem confiança por rede, autorizar cada acesso, menor privilégio, assumir a violação; **não** é um produto | S5.01 |
| Política de autorização testável / mutante de política | Função `decide(chamador, ação, dono)` com tabela de casos; o **mutante** remove uma defesa e o teste **tem** de falhar | S5.01 |
| *Break-glass* | Acesso humano excepcional (ticket aprovado, MFA, prazo, dois olhos, auditado) em vez de acesso permanente | S5.01 |
| Função com piso (`SECURITY DEFINER`) | Função no banco que executa a operação perigosa com limite embutido (`purge` recusa < 24 h); a app não recebe `DELETE` | S5.01 |
| `VALID UNTIL` | Prazo da **senha** de um papel do PostgreSQL (vale só para login por senha) | S5.01 |
| mTLS (TLS mútuo) | O cliente também apresenta certificado e prova a chave privada; dá **identidade do canal**, **não** autorização | S5.02 |
| CA / *truststore* / *keystore* / SAN / `extendedKeyUsage` | Quem assina certificados / CAs em que confio / minha chave e cadeia / nomes válidos do certificado (não o `CN`) / para que serve (`serverAuth`, `clientAuth`) | S5.02 |
| Certificado de vida curta | Folha de horas a dias renovada automaticamente: reduz a dependência de revogação (CRL/OCSP) | S5.02 |
| *Passthrough* × *verify* (ALB mTLS) | O ALB repassa a cadeia sem verificar / o ALB valida contra um *trust store*; dados do certificado em `X-Amzn-Mtls-*` | S5.02 |
| Criptografia envelope (DEK/KEK) | Chave de dados (DEK) cifra o dado; chave-mestra (KEK, no KMS) embrulha a DEK; rotacionar = **re-wrap**, não recriptografar | S5.03 |
| Contexto de criptografia (AAD) | Dados autenticados e **não secretos** ligados ao cifrado (`tabela|id|coluna`); trocar de linha falha; aparece no CloudTrail | S5.03 |
| AEAD / AES-GCM / *nonce* (IV) | Cifra que também autentica / modo usado / número único por chave (**reuso recupera o texto sem a chave**; limite de 2³² com IV aleatório) | S5.03 |
| Índice cego | `HMAC(segredo, valor normalizado)` guardado ao lado do campo cifrado: busca por igualdade, **vaza a distribuição** | S5.03 |
| Criptografia em repouso: volume × campo | Volume protege contra disco/*snapshot* roubado; campo protege contra *dump*/SQLi/DBA; nenhum protege de aplicação comprometida | S5.03 |
| SAST / DAST / SCA | Análise do código / da aplicação rodando / das dependências | S5.04 |
| Falso positivo / falso negativo (scanner) | Alerta que não é problema / problema que o scanner não vê (ex.: SQL montado em duas linhas sem *taint tracking*) | S5.04 |
| Portão de segurança e isenção (*waiver*) | Regra que bloqueia HIGH/CRITICAL com correção; isenção com id, motivo, dono e validade (≤ 90 dias) que **expira** | S5.04 |
| ZAP *baseline* | Varredura **passiva** do OWASP ZAP (spider + regras passivas), segura para CI; a completa ataca | S5.04 |
| Dado pessoal / base legal / inventário de dados | Informação de pessoa natural / fundamento do tratamento (art. 7º) / tabela tabela.coluna com categoria, base, retenção e dono | S5.05 |
| Pseudonimização × anonimização | Token ligável à pessoa só com a chave (continua dado pessoal) / sem possibilidade de reidentificar | S5.05 |
| Incidente de segurança (ANPD) | Comunicar ANPD e titulares em **3 dias úteis** se confirmado, dado pessoal e risco/dano relevante (Res. CD/ANPD 15/2024) | S5.05 |
| PAN / SAD / Luhn | Número do cartão / dados sensíveis de autenticação (CVV, PIN: nunca após autorização) / dígito verificador (~10% das strings aleatórias passam) | S5.05 |
| Escopo do PCI | Sistemas que armazenam, processam ou transmitem PAN; encolhe se o PayFlow **não toca** no cartão (campos hospedados do PSP) | S5.05 |
| Trilha de auditoria | Registro curado de quem fez o quê (inclui `DENIED`), com esquema, retenção e proteção contra adulteração; não é o log de aplicação | S5.06 |
| Cadeia de HMAC (*tamper-evident*) e âncora externa | Cada evento carrega o MAC do anterior; detecta edição/remoção/troca; **cortar o rabo** e **quem tem a chave** só a âncora (fora do alcance do dono do banco) pega | S5.06 |
| Selador (de auditoria) | Job separado, com a chave do HMAC, que sela eventos em ordem e grava a âncora; a aplicação não tem a chave | S5.06 |
| pgAudit / CloudTrail (validação de integridade) | Auditoria de SQL no PostgreSQL / trilha de chamadas de API da AWS com resumos assinados e encadeados | S5.06 |
| Régua de severidade (L×I) e SLA de correção | Probabilidade × impacto (≥ 20 crítico, ≥ 12 alto, ≥ 6 médio) e prazos 7/30/90/365 dias (propostos) | S5.07 |
| Matriz de cobertura (revisão) | Componentes × STRIDE; cada célula revisada, N/A com motivo ou fora de escopo com aceite; célula em silêncio reprova a revisão | S5.07 |
| Achado, evidência, aceite de risco com validade | Problema reproduzível com dono e prazo / como reproduzir / decisão de conviver com ele, com aprovador e data de expiração | S5.07 |
| ASVS / WSTG / SAMM | Requisitos verificáveis (5.0.0, 30/05/2025) / guia de testes / maturidade do programa (OWASP) | S5.07 |
| AWS Well-Architected Framework / pilares | Conjunto de boas práticas e perguntas para revisar *workloads*: excelência operacional, segurança, confiabilidade, eficiência de desempenho, otimização de custos e sustentabilidade (documento único desde 16/11/2026) | S6.01 |
| Cloud Foundations (FND01–FND07) | Camada do Well-Architected com as capacidades da organização (contas, identidade, observabilidade central, inventário, segurança, gestão financeira, rede): "o *blueprint* de uma *landing zone*" | S6.01 |
| HRI / MRI | *High/medium risk issue*: prática fundamental (alto) ou habilitadora (médio) não seguida; saída da revisão, com dono, prazo e ação | S6.01 |
| *Trade-off* quantificado | Decisão entre pilares registrada com custo (preço datado), métrica de sucesso e data de revisão | S6.01 |
| AWS Price List (Bulk API) | Arquivos públicos de preços por oferta e região (sem credenciais), base de uma estimativa reproduzível | S6.01 |
| Cruzamento (de custo) | Tráfego em que uma opção de computação passa a custar menos que outra (ex.: Lambda × Fargate) | S6.02 |
| Piso de alta disponibilidade | Custo mínimo de manter unidades em 2+ AZs ligadas 24×7, mesmo sem tráfego | S6.02 |
| *Lock-in* (tipos) | Custo de trocar de fornecedor: código (SDK), dados (gravidade), semântica do serviço, operação/identidade, contrato, pessoas | S6.03 |
| Gravidade dos dados | Quanto mais dados num lugar, mais caro (tempo, transferência, risco) é tirá-los de lá | S6.03 |
| Plano de saída | Documento com ordem, janela de corte e dados exportados (e restaurados) para deixar um provedor | S6.03 |
| Mínimo denominador comum | O que sobra quando um sistema só usa recursos que existem em todos os provedores | S6.03 |
| FinOps (Informar, Otimizar, Operar) | Modelo operacional e prática cultural de gestão do custo da nuvem, em três fases que se repetem | S6.04 |
| BOM (lista de materiais) | Linhas `ambiente;serviço;item;chave de preço;quantidade;fixo/variável;premissa` de uma estimativa | S6.04 |
| Custo unitário (*unit economics*) | Custo por unidade de negócio (ex.: US$ por 1.000 transferências); separa crescimento de desperdício | S6.04 |
| Tag de alocação de custo | Tag ativada no faturamento para agrupar custo (até 24 h + 24 h); chaves e valores sensíveis a maiúsculas | S6.04 |
| *Tag policy* | Política do Organizations que padroniza chaves e valores de tags (não avalia recurso sem tag) | S6.04 |
| *Showback* / *chargeback* | Mostrar a cada time o custo dele / cobrar internamente por ele | S6.04 |
| Savings Plans / Reserved Instances | Compromisso de US$ por hora (1 ou 3 anos; EC2, Fargate, Lambda) / reserva por instância (RDS não cancelável) | S6.04 |
| Spot / Fargate Spot | Capacidade ociosa com desconto, interrompível com aviso de 2 minutos | S6.04 |
| *Checkpoint* (de *job*) | Gravar o progresso periodicamente para recomeçar de onde parou depois de uma interrupção | S6.04 |
| AWS Budgets / Cost Anomaly Detection | Orçamento com alerta real e de previsão (atualizado até 3× por dia) / detecção de gasto anômalo por aprendizado de máquina | S6.04 |
| CUR 2.0 / FOCUS | Exportação detalhada de custo e uso da AWS (Data Exports) / especificação aberta de dados de custo (FinOps Foundation) | S6.04 |
| *Landing zone* | Ambiente multicontas bem arquitetado, com segurança, conformidade e custo desde o início | S6.05 |
| AWS Organizations / OU / conta de gestão | Serviço de várias contas / unidade organizacional que agrupa contas / conta que administra a organização (SCP não vale nela) | S6.05 |
| SCP (*service control policy*) | Política da organização que define o **máximo** de permissão das contas-membro; nunca concede; Allow em todos os níveis | S6.05 |
| RCP (*resource control policy*) | Equivalente da SCP do lado do recurso, para um subconjunto de serviços | S6.05 |
| `FullAWSAccess` | SCP gerenciada (Allow de tudo) anexada a toda raiz, OU e conta; tirá-la de uma OU nega tudo abaixo | S6.05 |
| Policy Staging (OU) | OU com contas descartáveis para testar políticas antes de aplicar na organização | S6.05 |
| AWS Control Tower / IAM Identity Center | Orquestrador de *landing zone* com controles prontos / acesso de pessoas a várias contas (antigo AWS SSO) | S6.05 |
| Papel vinculado a serviço | Papel do IAM usado por um serviço da AWS; não é afetado por SCP | S6.05 |
| Vazamento lento (de custo) | Aumento gradual que nenhum detector diário acusa; aparece na revisão mensal e no custo unitário | S6.06 |
