package com.payflow.lab.contract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Lado PROVEDOR (serviço de notificações). */
@RestController
public class PreferencesController {

    /** "banco" do laboratório; os estados do Pact (@State) o preparam. */
    public static final Map<String, List<String>> STORE = new ConcurrentHashMap<>();

    @Value("${lab.break:none}")
    private String break_;

    @GetMapping("/internal/v1/users/{id}/preferences")
    public ResponseEntity<Map<String, Object>> preferences(@PathVariable String id) {
        List<String> channels = STORE.get(id);
        if (channels == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", id);
        switch (break_) {
            case "rename" -> body.put("notificationChannels", channels); // renomeou o campo: QUEBRA o consumidor
            case "type" -> body.put("channels", String.join(",", channels)); // mudou de lista para string: QUEBRA
            case "additive" -> { body.put("channels", channels); body.put("quietHours", Map.of("from", "22:00", "to", "07:00")); } // só acrescentou: NÃO quebra
            default -> body.put("channels", channels);
        }
        return ResponseEntity.ok(body);
    }
}
