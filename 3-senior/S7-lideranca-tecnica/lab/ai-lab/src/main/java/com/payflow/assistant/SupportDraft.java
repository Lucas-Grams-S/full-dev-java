package com.payflow.assistant;

import java.util.List;

/** Structured output: the model must fill this record; our code validates it before a human sees it. */
public record SupportDraft(Category category, String reply, boolean needsHuman, List<String> guardReasons) {

    public enum Category { DELAYED, FAILED_FUNDS, BLOCKED_ACCOUNT, OTHER }

    public SupportDraft withGuard(List<String> reasons) {
        return new SupportDraft(category, reply, needsHuman || !reasons.isEmpty(), List.copyOf(reasons));
    }
}
