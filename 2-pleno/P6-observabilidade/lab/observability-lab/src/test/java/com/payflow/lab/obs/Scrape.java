package com.payflow.lab.obs;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lê o texto do /actuator/prometheus como o Prometheus leria. */
final class Scrape {
    private final List<String> lines;

    Scrape(int mgmtPort) {
        this(Http.get(mgmtPort, "/actuator/prometheus", Map.of()).body());
    }

    Scrape(String body) {
        this.lines = body.lines().filter(l -> !l.startsWith("#") && !l.isBlank()).toList();
    }

    static Scrape openMetrics(int mgmtPort) {
        return new Scrape(Http.get(mgmtPort, "/actuator/prometheus", Map.of("Accept", "application/openmetrics-text; version=1.0.0")).body());
    }

    /** Linhas que começam com o nome e contêm todos os pedaços (ex.: outcome="completed"). */
    List<String> lines(String name, String... mustContain) {
        return lines.stream().filter(l -> l.startsWith(name + "{") || l.startsWith(name + " "))
                .filter(l -> java.util.Arrays.stream(mustContain).allMatch(l::contains)).toList();
    }

    double value(String name, String... mustContain) {
        List<String> found = lines(name, mustContain);
        if (found.isEmpty()) {
            return 0;
        }
        String l = found.getFirst();
        Matcher m = Pattern.compile("\\s([-0-9.eE+]+)(\\s#.*)?$").matcher(l);
        return m.find() ? Double.parseDouble(m.group(1)) : Double.NaN;
    }

    List<String> all() {
        return lines;
    }
}
