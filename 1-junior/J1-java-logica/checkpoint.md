# J1.CP — Checkpoint do módulo J1

> ⏱️ ~1h30 · 🟢 Júnior · Pré-requisitos: J1.01 a J1.16

## Parte 1 — Quiz (10 questões)
Responda sem consultar; depois confira o gabarito.

1. Por que `0.1 + 0.2` não dá `0.3` em `double` e qual tipo usar para dinheiro? Como construí-lo?
2. Qual a diferença entre `==` e `equals` em `String`? Por que `"pay" == "pay"` pode dar `true`?
3. Java passa parâmetros por valor ou por referência? O que acontece com um `List` passado a um método que faz `l = new ArrayList<>()`?
4. Qual a diferença entre classe abstrata e interface, e por que "composição em vez de herança"?
5. Qual o contrato entre `equals` e `hashCode` e o que acontece com um `HashSet` se ele for violado?
6. `IOException` e `IllegalArgumentException`: qual é *checked*? Quando usar cada tipo? O que `try-with-resources` garante?
7. Escolha a coleção: (a) ids já processados, sem repetição; (b) extrato em ordem de inserção; (c) saldo por id de conta; (d) fila de transferências pendentes.
8. Por que `for (String s : lista) lista.remove(s);` falha e qual a forma correta?
9. Para que serve `sealed` + `switch` sem `default`? Dê dois motivos para `Optional` ser retorno e não campo.
10. Qual a complexidade de `List.contains`, `HashSet.contains` e de ordenar uma lista? Como você testaria um método que usa `Instant.now()`?

<details><summary>Gabarito</summary>

1. `double` é binário e não representa 0,1 exatamente; use `BigDecimal` construído a partir de `String` (`new BigDecimal("0.10")`), com escala 2 e `RoundingMode.HALF_EVEN`.
2. `==` compara endereços; `equals` compara conteúdo. Literais iguais compartilham o mesmo objeto no *string pool*, então `==` dá `true` só nesse caso.
3. Sempre por valor; para objetos o valor é a referência. A reatribuição só afeta a cópia local: o chamador continua com a lista original.
4. Abstrata: estado e implementação parcial, herança simples. Interface: contrato, várias por classe. Composição reduz acoplamento, permite trocar a peça em execução e facilita testes (injeção de dependência).
5. Objetos iguais devem ter o mesmo `hashCode`. Se violado, `contains`/`get` procuram na gaveta errada e o elemento "some".
6. `IOException` é *checked*; `IllegalArgumentException` é *unchecked* (erro de uso ou de regra). `try-with-resources` fecha cada recurso `AutoCloseable`, em ordem inversa, mesmo com exceção.
7. (a) `HashSet`; (b) `ArrayList`; (c) `HashMap` (ou `LinkedHashMap`); (d) `ArrayDeque` como `Queue`.
8. O iterador detecta modificação externa e lança `ConcurrentModificationException`. Use `removeIf` (ou `Iterator.remove`).
9. `sealed` fecha as variantes e o compilador exige que o `switch` cubra todas (sem `default`); `Optional` como retorno deixa "pode não haver valor" explícito, mas em campo ou parâmetro só adiciona ruído e não é serializável de forma simples.
10. `List.contains` O(n); `HashSet.contains` O(1) em média; ordenação O(n log n). Injeta-se um `Clock` (fixo no teste).
</details>

## Parte 2 — Entrega prática (tag `payflow-J1`)
Mini-projeto **`mini-bank`** em console: contas, depósito, saque e extrato, com testes. Ele será reaproveitado como domínio do PayFlow (pacote base `com.payflow`).

Critérios de pronto:
- [ ] Projeto Maven com **Java 25**, JUnit 6 e AssertJ (`pom.xml` da J1.16) e `mvn test` verde.
- [ ] `Money` (record, escala 2, `HALF_EVEN`), `Account`, `Bank`, `Entry`, `EntryType`, `InsufficientFundsException` e `Main`.
- [ ] Saldo nunca negativo; valores não positivos rejeitados; extrato imutável; `Clock` injetado.
- [ ] Extrato exportado e lido em CSV (`java.nio.file`), somando com `BigDecimal`.
- [ ] Pelo menos 10 testes, com ao menos um parametrizado, sem `Instant.now()`.
- [ ] Nenhum `double` para dinheiro, nenhum `catch` vazio, nenhum `Optional.get()` sem checagem.
- [ ] Repositório com commits em Conventional Commits (em inglês), um PR mesclado e a tag enviada:
```bash
git tag payflow-J1
git push origin payflow-J1
```
*(Se o `mini-bank` vive dentro do repositório `payflow`, coloque-o em uma pasta `mini-bank/`; ele será movido para a estrutura Spring em J5.)*

## Parte 3 — Autoavaliação (baseada em `00-guia/matriz-de-competencias.md`)
Marque 0 (não sei), 1 (faço com ajuda), 2 (faço sozinho):

| Competência (nível Júnior) | 0 | 1 | 2 |
|---|---|---|---|
| Escrevo Java com tipos, controle de fluxo e métodos | ☐ | ☐ | ☐ |
| Modelo o domínio com classes, encapsulamento e interfaces | ☐ | ☐ | ☐ |
| Implemento `equals`/`hashCode`/`toString` corretamente | ☐ | ☐ | ☐ |
| Trato exceções com critério (checked vs unchecked) | ☐ | ☐ | ☐ |
| Escolho a Collection certa e uso Streams com clareza | ☐ | ☐ | ☐ |
| Uso records, enums, `Optional` e `sealed` | ☐ | ☐ | ☐ |
| Calculo dinheiro com `BigDecimal` e tempo com `java.time` | ☐ | ☐ | ☐ |
| Escrevo testes de unidade com JUnit e AssertJ | ☐ | ☐ | ☐ |

**Regra:** se ficou com mais de duas linhas em 0, repita as aulas correspondentes antes de seguir para o [J2](../J2-como-a-web-funciona/README.md). Atualize `00-guia/progresso.md` e o `cronograma.md`.

## Referência do `mini-bank`
> Código **compilado e testado** (JDK 25.0.5, JUnit 6.1.3, AssertJ 3.27.7): `mvn test` com 7 testes verdes. Use para **conferir** a sua versão depois de tentar; o objetivo é você escrever a sua.

<details><summary>Money.java</summary>

```java
package com.payflow.minibank;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valor monetário: BigDecimal, escala 2, HALF_EVEN (decisão do curso). */
public record Money(BigDecimal value) implements Comparable<Money> {
    public static final Money ZERO = of("0");

    public Money {
        value = value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money of(String text) {
        return new Money(new BigDecimal(text));
    }

    public Money plus(Money other) { return new Money(value.add(other.value)); }
    public Money minus(Money other) { return new Money(value.subtract(other.value)); }
    public boolean isPositive() { return value.signum() > 0; }
    public boolean isNegative() { return value.signum() < 0; }

    @Override
    public int compareTo(Money other) { return value.compareTo(other.value); }

    @Override
    public String toString() { return value.toPlainString(); }
}
```
</details>

<details><summary>EntryType.java</summary>

```java
package com.payflow.minibank;

public enum EntryType { DEPOSIT, WITHDRAWAL }
```
</details>

<details><summary>Entry.java</summary>

```java
package com.payflow.minibank;

import java.time.Instant;

/** Lançamento imutável do extrato. */
public record Entry(EntryType type, Money amount, Instant createdAt) { }
```
</details>

<details><summary>InsufficientFundsException.java</summary>

```java
package com.payflow.minibank;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Money balance, Money requested) {
        super("Insufficient funds: balance=" + balance + ", requested=" + requested);
    }
}
```
</details>

<details><summary>Account.java</summary>

```java
package com.payflow.minibank;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Account {
    private final String id;
    private final String owner;
    private final Clock clock;
    private final List<Entry> entries = new ArrayList<>();
    private Money balance = Money.ZERO;

    public Account(String id, String owner, Clock clock) {
        this.id = Objects.requireNonNull(id);
        this.owner = Objects.requireNonNull(owner);
        this.clock = Objects.requireNonNull(clock);
    }

    public void deposit(Money amount) {
        requirePositive(amount);
        balance = balance.plus(amount);
        entries.add(new Entry(EntryType.DEPOSIT, amount, clock.instant()));
    }

    public void withdraw(Money amount) {
        requirePositive(amount);
        if (amount.compareTo(balance) > 0) {
            throw new InsufficientFundsException(balance, amount);
        }
        balance = balance.minus(amount);
        entries.add(new Entry(EntryType.WITHDRAWAL, amount, clock.instant()));
    }

    private static void requirePositive(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }

    public String id() { return id; }
    public String owner() { return owner; }
    public Money balance() { return balance; }
    /** Cópia somente leitura: quem chama não consegue alterar o extrato. */
    public List<Entry> statement() { return Collections.unmodifiableList(entries); }

    @Override
    public boolean equals(Object o) {
        return o instanceof Account other && id.equals(other.id);
    }
    @Override
    public int hashCode() { return id.hashCode(); }
    @Override
    public String toString() { return "Account[" + id + ", " + owner + ", " + balance + "]"; }
}
```
</details>

<details><summary>Bank.java</summary>

```java
package com.payflow.minibank;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class Bank {
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Clock clock;

    public Bank(Clock clock) { this.clock = clock; }

    public Account open(String id, String owner) {
        if (accounts.containsKey(id)) {
            throw new IllegalArgumentException("Account already exists: " + id);
        }
        Account account = new Account(id, owner, clock);
        accounts.put(id, account);
        return account;
    }

    public Optional<Account> find(String id) { return Optional.ofNullable(accounts.get(id)); }

    public Account get(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    }
}
```
</details>

<details><summary>Main.java</summary>

```java
package com.payflow.minibank;

import java.time.Clock;

public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank(Clock.systemUTC());
        Account ana = bank.open("acc-1", "Ana");
        ana.deposit(Money.of("100.00"));
        ana.withdraw(Money.of("30.25"));
        System.out.println(ana);
        ana.statement().forEach(e -> System.out.println(e.type() + " " + e.amount()));
        try {
            ana.withdraw(Money.of("500"));
        } catch (InsufficientFundsException e) {
            System.out.println(e.getMessage());
        }
    }
}
```
</details>

O `pom.xml` e o `AccountTest` estão na [J1.16](J1.16-pacotes-bigo-junit.md).

> **Nota:** `StatementCsvWriter`/`StatementCsvReader` (J1.15) e `Repository<T, ID>` (J1.12) são exercícios seus e não fazem parte desta referência.
