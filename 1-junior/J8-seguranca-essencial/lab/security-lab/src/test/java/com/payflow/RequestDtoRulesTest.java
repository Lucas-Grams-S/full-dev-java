package com.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.constraints.Size;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/** J8.07: toda String (e coleção) de entrada tem um limite de tamanho: o que cresce sem limite vira vetor de ataque. */
class RequestDtoRulesTest {

    record BadRequest(String note, java.util.List<String> tags) { }

    static List<String> offenders(Class<?> type) {
        List<String> offenders = new ArrayList<>();
        for (RecordComponent c : type.getRecordComponents()) {
            boolean mustBeBounded = c.getType() == String.class || Collection.class.isAssignableFrom(c.getType());
            // a anotação do Jakarta Validation vai para o ACESSOR (não para o RecordComponent), por causa do @Target
            Size size = c.getAccessor().getAnnotation(Size.class);
            if (mustBeBounded && (size == null || size.max() == Integer.MAX_VALUE)) {
                offenders.add(type.getSimpleName() + "." + c.getName());
            }
        }
        return offenders;
    }

    @Test
    void everyRealRequestRecordIsBounded() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*Request")));
        List<String> all = new ArrayList<>();
        int checked = 0;
        for (var definition : scanner.findCandidateComponents("com.payflow.web.api.dto")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            if (type.isRecord()) {
                checked++;
                all.addAll(offenders(type));
            }
        }
        System.out.println("[dto] " + checked + " record(s) *Request verificado(s); sem limite: " + all);
        assertThat(checked).isPositive();
        assertThat(all).isEmpty();
    }

    @Test
    void theRuleDoesCatchAnUnboundedDto() {
        System.out.println("[dto] BadRequest sem limite: " + offenders(BadRequest.class));
        assertThat(offenders(BadRequest.class)).containsExactly("BadRequest.note", "BadRequest.tags");
    }
}
