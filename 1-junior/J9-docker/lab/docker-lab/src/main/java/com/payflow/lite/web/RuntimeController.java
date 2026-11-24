package com.payflow.lite.web;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mostra o que a JVM enxerga de dentro do container (usuario, memoria, CPUs, host). So para o laboratorio. */
@RestController
public class RuntimeController {

    private final Environment environment;

    public RuntimeController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/api/v1/runtime")
    public Map<String, Object> runtime() throws Exception {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("user", System.getProperty("user.name"));
        info.put("maxHeapMb", Runtime.getRuntime().maxMemory() / (1024 * 1024));
        info.put("cpus", Runtime.getRuntime().availableProcessors());
        info.put("hostname", InetAddress.getLocalHost().getHostName());
        info.put("profiles", environment.getActiveProfiles());
        return info;
    }
}
