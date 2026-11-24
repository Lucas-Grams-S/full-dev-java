// OfferLab.java — S8.05: compare job offers in the SAME unit (money per year), never "monthly gross x monthly gross".
//   java OfferLab.java selftest      -> the tax code against the 5 OFFICIAL examples of the Receita Federal (Lei 15.270/2025)
//   java -Dmutant=reductionOnBase OfferLab.java selftest   -> mutant: reduction computed on the base, not on the salary
//   java -Dmutant=alwaysSimplified OfferLab.java selftest  -> mutant: always the simplified discount
//   java OfferLab.java clt 4500      -> CLT monthly breakdown (INSS 2026 + IRRF 2026 with the reduction)
//   java OfferLab.java compare       -> CLT x PJ: naive monthly comparison, annual money, break-even rate and minimum invoice
//
// OFFICIAL TABLES (read on 2026-11-23):
//   INSS, employee, from January 2026: gov.br/inss "Tabela de contribuição mensal" (Portaria Interministerial MPS/MF nº 13, 09/01/2026)
//   IRRF monthly table + reduction table, 2026: gov.br/receitafederal "Tributação de 2026" (Lei 15.191/2025 and Lei 15.270/2025)
//   FGTS: deposits of 8.0% of the salary (gov.br/trabalho-e-emprego, page "FGTS")
// PREMISES (invented, declared; change them for YOUR offers): offer values, meal/health benefits, PJ costs, months billed,
//   PJ effective tax rate (swept: depends on the tax regime, ask an accountant), vacation month taxed as one payment,
//   13th salary taxed alone without the reduction (only used for salaries above R$ 7,350.00, where the reduction is zero anyway),
//   FGTS base including the 13th salary and the vacation third (law text NOT opened: planalto.gov.br did not answer).
// Money: BigDecimal, 2 decimals, HALF_EVEN (training decision 4). Not tax advice.

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class OfferLab {

    static final String MUTANT = System.getProperty("mutant", "none");

    static BigDecimal bd(String s) { return new BigDecimal(s); }
    static BigDecimal money(BigDecimal v) { return v.setScale(2, RoundingMode.HALF_EVEN); }
    static String fmt(BigDecimal v) { return String.format(Locale.US, "%,12.2f", v); }

    // ---- INSS 2026, employee: progressive by bracket, capped at the ceiling ----
    static final BigDecimal[] INSS_UPPER = {bd("1621.00"), bd("2902.84"), bd("4354.27"), bd("8475.55")};
    static final BigDecimal[] INSS_RATE = {bd("0.075"), bd("0.09"), bd("0.12"), bd("0.14")};

    static BigDecimal inss(BigDecimal salary) {
        BigDecimal total = BigDecimal.ZERO, lower = BigDecimal.ZERO;
        for (int i = 0; i < INSS_UPPER.length; i++) {
            if (salary.compareTo(lower) <= 0) break;
            BigDecimal slice = salary.min(INSS_UPPER[i]).subtract(lower);
            total = total.add(slice.multiply(INSS_RATE[i]));
            lower = INSS_UPPER[i];
        }
        return money(total);
    }

    // ---- IRRF 2026, monthly ----
    static final BigDecimal SIMPLIFIED = bd("607.20");                 // 25% of R$ 2,428.80
    static final BigDecimal[] IR_UPPER = {bd("2428.80"), bd("2826.65"), bd("3751.05"), bd("4664.68")};
    static final BigDecimal[] IR_RATE = {bd("0"), bd("0.075"), bd("0.15"), bd("0.225"), bd("0.275")};
    static final BigDecimal[] IR_DEDUCT = {bd("0"), bd("182.16"), bd("394.16"), bd("675.49"), bd("908.73")};

    record Ir(BigDecimal deduction, String deductionKind, BigDecimal base, BigDecimal tableTax, BigDecimal reduction, BigDecimal tax) {}

    /** taxable = salary ("rendimentos tributáveis"); legalDeductions = INSS (+ dependents, not used here). */
    static Ir irrf(BigDecimal taxable, BigDecimal legalDeductions, boolean applyReduction) {
        boolean simplified = SIMPLIFIED.compareTo(legalDeductions) > 0 || MUTANT.equals("alwaysSimplified");
        BigDecimal deduction = simplified ? SIMPLIFIED : legalDeductions;
        BigDecimal base = taxable.subtract(deduction).max(BigDecimal.ZERO);
        int b = 0;
        while (b < IR_UPPER.length && base.compareTo(IR_UPPER[b]) > 0) b++;
        BigDecimal tableTax = money(base.multiply(IR_RATE[b]).subtract(IR_DEDUCT[b]).max(BigDecimal.ZERO));
        BigDecimal reduction = money(BigDecimal.ZERO);
        if (applyReduction) {
            BigDecimal ref = MUTANT.equals("reductionOnBase") ? base : taxable;   // the Receita: use the SALARY, not the base
            if (ref.compareTo(bd("5000.00")) <= 0) reduction = bd("312.89");
            else if (ref.compareTo(bd("7350.00")) < 0)
                reduction = money(bd("978.62").subtract(bd("0.133145").multiply(ref))).max(BigDecimal.ZERO);
            reduction = reduction.min(tableTax);                              // limited to the tax itself
        }
        return new Ir(deduction, simplified ? "simplified" : "legal", base, tableTax, reduction, tableTax.subtract(reduction));
    }

    record Month(BigDecimal gross, BigDecimal inss, Ir ir, BigDecimal net) {}

    static Month month(BigDecimal gross) {
        BigDecimal inss = inss(gross);
        Ir ir = irrf(gross, inss, true);
        return new Month(gross, inss, ir, gross.subtract(inss).subtract(ir.tax()));
    }

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "compare";
        switch (mode) {
            case "selftest" -> selftest();
            case "clt" -> clt(bd(args.length > 1 ? args[1] : "4500"));
            default -> compare();
        }
    }

    // ---- 1. the code against the official examples (Receita Federal, "Exemplos de aplicação da Lei 15.270/2025") ----
    static void selftest() {
        System.out.println("mutant: " + MUTANT);
        // {salary, INSS given in the example, expected IRRF}. The examples use the INSS value they state (2025 table).
        String[][] official = {
                {"3036.00", "257.73", "0.00"}, {"4000.00", "373.41", "0.00"}, {"5000.00", "509.60", "0.00"},
                {"6000.00", "649.60", "382.88"}, {"7607.20", "0.00", "1016.27"}};
        int fail = 0;
        System.out.println("example | salary   | deduction (kind)      | base     | table tax | reduction | IRRF    | expected | result");
        for (int i = 0; i < official.length; i++) {
            Ir ir = irrf(bd(official[i][0]), bd(official[i][1]), true);
            boolean ok = ir.tax().compareTo(bd(official[i][2])) == 0;
            if (!ok) fail++;
            System.out.printf(Locale.US, "%7d | %8s | %8s (%-10s) | %8s | %9s | %9s | %7s | %8s | %s%n", i + 1, official[i][0],
                    ir.deduction(), ir.deductionKind(), ir.base(), ir.tableTax(), ir.reduction(), ir.tax(), official[i][2], ok ? "OK" : "FAIL");
        }
        // INSS 2026: the bracket edges and the ceiling, computed by hand from the official table
        String[][] inssCases = {{"1621.00", "121.58"}, {"2902.84", "236.94"}, {"4354.27", "411.11"}, {"8475.55", "988.09"}, {"20000.00", "988.09"}};
        for (String[] c : inssCases) {
            BigDecimal got = inss(bd(c[0]));
            boolean ok = got.compareTo(bd(c[1])) == 0;
            if (!ok) fail++;
            System.out.printf(Locale.US, "INSS(%9s) = %7s  expected %7s  %s%n", c[0], got, c[1], ok ? "OK" : "FAIL");
        }
        System.out.println(fail == 0 ? "SELFTEST PASS (10 checks)" : "SELFTEST FAIL: " + fail + " of 10 checks");
    }

    // ---- 2. one CLT month ----
    static void clt(BigDecimal gross) {
        Month m = month(gross);
        System.out.println("CLT month, gross " + fmt(gross).trim());
        System.out.println("  INSS 2026 (progressive)            " + fmt(m.inss()));
        System.out.println("  IR deduction (" + m.ir().deductionKind() + ")" + " ".repeat(21 - m.ir().deductionKind().length()) + fmt(m.ir().deduction()));
        System.out.println("  IR base                            " + fmt(m.ir().base()));
        System.out.println("  IR by the table                    " + fmt(m.ir().tableTax()));
        System.out.println("  reduction (Lei 15.270/2025)        " + fmt(m.ir().reduction().negate()));
        System.out.println("  IRRF withheld                      " + fmt(m.ir().tax()));
        System.out.println("  NET in the account                 " + fmt(m.net()));
        System.out.printf(Locale.US, "  net / gross                        %11.1f%%%n", m.net().multiply(bd("100")).divide(gross, 1, RoundingMode.HALF_EVEN));
    }

    // ---- 3. the comparison ----
    static void compare() {
        // PREMISES (invented): an offer of each kind
        BigDecimal cltSalary = bd("12000.00");
        BigDecimal mealPerMonth = bd("1000.00");           // VR/VA paid by the employer (not taxed as salary)
        BigDecimal healthPerMonth = bd("900.00");          // market value of the employer's health plan for one person
        BigDecimal pjInvoice = bd("16000.00");
        int pjMonthsBilled = 11;                            // 30 days off = one month without invoice
        BigDecimal pjAccountant = bd("400.00");            // per month
        if (cltSalary.compareTo(bd("7350.00")) <= 0) throw new IllegalStateException("annual premises only hold above R$ 7,350.00");

        System.out.println("PREMISES (invented): CLT R$ 12,000.00/month + meal R$ 1,000.00 + health plan worth R$ 900.00;");
        System.out.println("                     PJ R$ 16,000.00/month, 11 months billed, accountant R$ 400.00/month, own health plan R$ 900.00");
        System.out.println();
        System.out.printf(Locale.US, "naive (monthly gross): PJ %s vs CLT %s -> PJ %+.1f%%%n", fmt(pjInvoice).trim(), fmt(cltSalary).trim(),
                pct(pjInvoice, cltSalary));
        System.out.println();

        Month normal = month(cltSalary);
        Month vacation = month(cltSalary.add(money(cltSalary.divide(bd("3"), 10, RoundingMode.HALF_EVEN))));   // salary + 1/3, one payment
        BigDecimal inss13 = inss(cltSalary);                                                                  // 13th: alone (INSS page, note 3)
        Ir ir13 = irrf(cltSalary, inss13, false);
        BigDecimal net13 = cltSalary.subtract(inss13).subtract(ir13.tax());
        BigDecimal cash = normal.net().multiply(bd("11")).add(vacation.net()).add(net13);
        BigDecimal fgtsBase = cltSalary.multiply(bd("13")).add(money(cltSalary.divide(bd("3"), 10, RoundingMode.HALF_EVEN)));
        BigDecimal fgts = money(fgtsBase.multiply(bd("0.08")));
        BigDecimal benefits = mealPerMonth.add(healthPerMonth).multiply(bd("12"));
        BigDecimal cltTotal = cash.add(fgts).add(benefits);

        String[][] rows = {
                {"11 months net (" + fmt(normal.net()).trim() + " each)", fmt(normal.net().multiply(bd("11")))},
                {"vacation month net (salary + 1/3 = " + fmt(vacation.gross()).trim() + ")", fmt(vacation.net())},
                {"13th salary net (INSS " + fmt(inss13).trim() + ", IRRF " + fmt(ir13.tax()).trim() + ")", fmt(net13)},
                {"= cash in the account", fmt(cash)},
                {"+ FGTS 8% (restricted withdrawal)", fmt(fgts)},
                {"+ meal + health plan (12 months)", fmt(benefits)},
                {"= CLT total", fmt(cltTotal)}};
        System.out.printf(Locale.US, "%-58s %12s%n", "CLT, one year", "R$");
        for (String[] r : rows) System.out.printf(Locale.US, "  %-56s %s%n", r[0], r[1]);
        System.out.println();

        System.out.println("PJ, one year, by effective tax rate on the invoice (DEPENDS on the regime: ask an accountant)");
        System.out.println("  rate |  invoices (11) |          taxes | accountant+health |       PJ total | PJ - CLT total | PJ - CLT w/o FGTS");
        BigDecimal revenue = pjInvoice.multiply(BigDecimal.valueOf(pjMonthsBilled));
        BigDecimal pjCosts = pjAccountant.add(healthPerMonth).multiply(bd("12"));
        for (String r : List.of("0.06", "0.10", "0.15", "0.20")) {
            BigDecimal taxes = money(revenue.multiply(bd(r)));
            BigDecimal pjTotal = revenue.subtract(taxes).subtract(pjCosts);
            System.out.printf(Locale.US, "  %3.0f%% | %14s | %14s | %17s | %14s | %14s | %17s%n", bd(r).doubleValue() * 100, fmt(revenue), fmt(taxes),
                    fmt(pjCosts), fmt(pjTotal), fmt(pjTotal.subtract(cltTotal)), fmt(pjTotal.subtract(cash.add(benefits))));
        }
        // break-even rate: revenue*(1-r) - costs = cltTotal  ->  r = 1 - (cltTotal + costs)/revenue
        BigDecimal breakEven = BigDecimal.ONE.subtract(cltTotal.add(pjCosts).divide(revenue, 6, RoundingMode.HALF_EVEN));
        System.out.printf(Locale.US, "  break-even: PJ only beats the CLT total if the effective rate is below %.1f%%%n", breakEven.doubleValue() * 100);
        System.out.println();
        System.out.println("minimum PJ invoice per month to MATCH the CLT total (your reservation point for this offer)");
        for (String r : List.of("0.06", "0.10", "0.15", "0.20")) {
            BigDecimal needed = cltTotal.add(pjCosts).divide(BigDecimal.ONE.subtract(bd(r)), 10, RoundingMode.HALF_EVEN)
                    .divide(BigDecimal.valueOf(pjMonthsBilled), 2, RoundingMode.HALF_EVEN);
            System.out.printf(Locale.US, "  at %3.0f%%: R$ %s  (%+.1f%% over the CLT monthly gross)%n", bd(r).doubleValue() * 100, fmt(needed).trim(), pct(needed, cltSalary));
        }
    }

    static double pct(BigDecimal a, BigDecimal b) {
        return a.subtract(b).multiply(bd("100")).divide(b, 4, RoundingMode.HALF_EVEN).doubleValue();
    }
}
