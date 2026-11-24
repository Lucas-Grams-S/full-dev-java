package com.payflow.lab.obs;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.test.system.CapturedOutput;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Extrai e interpreta as linhas JSON que o aplicativo escreveu no console. */
final class LogLines {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    static List<JsonNode> json(CapturedOutput out, String contains) {
        return Arrays.stream(out.getAll().split("\n")).filter(l -> l.startsWith("{") && l.contains(contains))
                .map(JSON::readTree).toList();
    }

    static List<String> text(CapturedOutput out, String contains) {
        return Arrays.stream(out.getAll().split("\n")).filter(l -> !l.startsWith("{") && l.contains(contains)).toList();
    }

    static void print(String tag, String label, Object value) {
        System.out.println(tag + " " + label + ": " + value);
    }
}
