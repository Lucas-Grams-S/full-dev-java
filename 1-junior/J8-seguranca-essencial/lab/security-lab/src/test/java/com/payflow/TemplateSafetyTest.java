package com.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** J8.05: vigia os templates. Saída sem escape e código inline também seriam barrados pela CSP (script-src 'self'). */
class TemplateSafetyTest {

    record Rule(String name, Pattern pattern) { }

    static final List<Rule> RULES = List.of(
            new Rule("th:utext (saída SEM escape)", Pattern.compile("th:utext")),
            new Rule("[( ... )] (saída inline SEM escape)", Pattern.compile("\\[\\(")),
            new Rule("javascript: em atributo", Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE)),
            new Rule("manipulador inline (onclick, onerror...)", Pattern.compile("\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE)),
            new Rule("<script> inline (sem src)", Pattern.compile("<script(?![^>]*\\bsrc=)[^>]*>", Pattern.CASE_INSENSITIVE)),
            new Rule("atributo style inline", Pattern.compile("\\sstyle\\s*=", Pattern.CASE_INSENSITIVE)));

    static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void noTemplateBreaksTheRules() throws IOException {
        Path dir = Path.of("src/main/resources/templates");
        List<String> offenders;
        try (Stream<Path> files = Files.walk(dir)) {
            offenders = files.filter(p -> p.toString().endsWith(".html"))
                    .flatMap(p -> RULES.stream().filter(r -> r.pattern().matcher(read(p)).find()).map(r -> p.getFileName() + ": " + r.name()))
                    .toList();
        }
        System.out.println("[templates] violações: " + offenders);
        assertThat(offenders).isEmpty();
    }

    @Test
    void theRulesDoCatchBadTemplates() {
        String bad = "<p th:utext=\"${x}\">x</p><p>[(${x})]</p><a href=\"javascript:alert(1)\" onclick=\"f()\" style=\"color:red\">y</a><script>alert(1)</script>";
        List<String> hits = RULES.stream().filter(r -> r.pattern().matcher(bad).find()).map(Rule::name).toList();
        System.out.println("[templates] o template ruim viola " + hits.size() + " de " + RULES.size() + " regras");
        assertThat(hits).hasSize(RULES.size());
        assertThat(RULES.stream().filter(r -> r.pattern().matcher("<script src=\"/static/app.js\" defer></script><p th:text=\"${x}\">x</p>").find())).isEmpty();
    }
}
