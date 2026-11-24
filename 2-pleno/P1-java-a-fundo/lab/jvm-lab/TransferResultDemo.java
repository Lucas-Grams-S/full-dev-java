import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

public class TransferResultDemo {
    record Money(BigDecimal amount) {}
    sealed interface TransferResult permits Completed, InsufficientFunds, AccountBlocked, Duplicate {}
    record Completed(UUID id) implements TransferResult {}
    record InsufficientFunds(Money missing) implements TransferResult {}
    record AccountBlocked(UUID account) implements TransferResult {}
    record Duplicate(UUID original) implements TransferResult {}

    static int httpStatus(TransferResult r) {
        return switch (r) {
            case Completed _ -> 201;
            case InsufficientFunds _, AccountBlocked _ -> 422;
            case Duplicate _ -> 200;
        };
    }
    public static void main(String[] a) {
        UUID u = UUID.randomUUID();
        System.out.println(httpStatus(new Completed(u)) + " " + httpStatus(new InsufficientFunds(new Money(BigDecimal.TEN))) + " " + httpStatus(new AccountBlocked(u)) + " " + httpStatus(new Duplicate(u)));
    }
}
