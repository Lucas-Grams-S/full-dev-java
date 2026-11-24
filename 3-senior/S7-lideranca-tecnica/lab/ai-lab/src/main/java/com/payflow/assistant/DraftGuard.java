package com.payflow.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic checks on what the model wrote. The model is a component that can be wrong or manipulated,
 * so its output goes through rules we can test, like any input from outside.
 */
public final class DraftGuard {

    private static final List<String> PROMISES = List.of("estorno", "estornar", "reembolso", "devolveremos", "vamos devolver");
    private static final Pattern MONEY = Pattern.compile("R\\$\\s?([0-9.]+,[0-9]{2})");

    private DraftGuard() {}

    public static List<String> check(SupportDraft draft, TransferSnapshot transfer) {
        List<String> reasons = new ArrayList<>();
        String reply = draft.reply() == null ? "" : draft.reply();
        String lower = reply.toLowerCase(Locale.ROOT);
        for (String p : PROMISES) {
            if (lower.contains(p)) { reasons.add("promise of money back (\"" + p + "\") needs a human"); break; }
        }
        Matcher m = MONEY.matcher(reply);
        String expected = transfer.amount().replace(".", ",");
        while (m.find()) {
            if (!m.group(1).replace(".", "").equals(expected)) reasons.add("amount R$ " + m.group(1) + " is not the transfer amount R$ " + expected);
        }
        if (reply.isBlank()) reasons.add("empty reply");
        return reasons;
    }
}
