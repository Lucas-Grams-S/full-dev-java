package com.payflow.app;

import com.payflow.fee.FeePolicy;
import com.payflow.ledger.Ledger;
import java.math.BigDecimal;
import java.util.ServiceLoader;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("fee(100.00) = " + Ledger.fee(new BigDecimal("100.00")));
        System.out.println("módulo desta classe: " + Main.class.getModule() + " | nomeado? " + Main.class.getModule().isNamed());
        long n = ServiceLoader.load(FeePolicy.class).stream().count();
        System.out.println("implementações de FeePolicy encontradas: " + n);
        ServiceLoader.load(FeePolicy.class).forEach(p -> System.out.println("  " + p.name() + " -> " + p.fee(new BigDecimal("100.00"))));
        if (args.length > 0) {   // reflexão sobre o pacote não exportado
            Class<?> c = Class.forName("com.payflow.ledger.internal.Rounding");
            System.out.println("classe achada: " + c);
            c.getMethod("money", BigDecimal.class).invoke(null, BigDecimal.ONE);
        }
    }
}
