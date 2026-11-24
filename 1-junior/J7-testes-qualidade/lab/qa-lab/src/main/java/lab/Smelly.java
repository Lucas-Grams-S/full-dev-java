package lab;

import java.math.BigDecimal;
import java.util.*;
import java.util.List;

public class Smelly {

    public static BigDecimal addFee(BigDecimal balance, BigDecimal fee) {
        balance.add(fee);
        return balance;
    }

    public static boolean sameCurrency(String a, String b) {
        return a == b;
    }

    public static int length(String s, boolean flag) {
        String t = flag ? null : s;
        return t.length();
    }

    public static String read(int n) {
        try {
            return "x".substring(n);
        } catch (RuntimeException e) {
        }
        if (n > 0) return "positive";
        return "other";
    }
}
