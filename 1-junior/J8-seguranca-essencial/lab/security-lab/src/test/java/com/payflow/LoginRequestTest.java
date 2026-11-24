package com.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.web.api.dto.LoginRequest;
import org.junit.jupiter.api.Test;

class LoginRequestTest {

    record Naive(String email, String password) { }

    @Test
    void theDefaultRecordToStringLeaksThePassword() {
        System.out.println("[toString] record comum : " + new Naive("ana@payflow.dev", "Payflow#Dev2026"));
        assertThat(new Naive("ana@payflow.dev", "Payflow#Dev2026").toString()).contains("Payflow#Dev2026");
    }

    @Test
    void ourLoginRequestNeverPrintsThePasswordOrTheEmail() {
        String printed = new LoginRequest("ana@payflow.dev", "Payflow#Dev2026").toString();
        System.out.println("[toString] LoginRequest : " + printed);
        assertThat(printed).doesNotContain("Payflow#Dev2026").doesNotContain("ana@payflow.dev");
    }
}
