package com.payflow.web.api;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Só do laboratório: um endpoint protegido APENAS por @PreAuthorize (a cadeia só exige "autenticado"). */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportsController {

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> summary() {
        return Map.of("accounts", 2);
    }
}
