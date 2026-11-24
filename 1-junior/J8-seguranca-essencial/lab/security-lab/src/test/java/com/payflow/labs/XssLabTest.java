package com.payflow.labs;

import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/** J8.05: o que o Thymeleaf escapa sozinho e o que não escapa. */
class XssLabTest {

    final TemplateEngine engine = new SpringTemplateEngine();

    XssLabTest() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        engine.setTemplateResolver(resolver);
    }

    String render(String template, String value) {
        Context ctx = new Context();
        ctx.setVariable("v", value);
        return engine.process(template, ctx);
    }

    @Test
    void print() {
        String payload = "<img src=x onerror=alert(1)>";
        System.out.println("[xss] th:text   corpo   : " + render("<p th:text=\"${v}\"></p>", payload));
        System.out.println("[xss] th:utext  corpo   : " + render("<p th:utext=\"${v}\"></p>", payload));
        String quote = "\" onmouseover=\"alert(1)";
        System.out.println("[xss] th:value  atributo: " + render("<input th:value=\"${v}\">", quote));
        String js = "javascript:alert(document.cookie)";
        System.out.println("[xss] th:href   javascript: " + render("<a th:href=\"${v}\">x</a>", js));
        System.out.println("[xss] th:inline js      : " + render("<script th:inline=\"javascript\">var n = [[${v}]];</script>", "</script><script>alert(1)</script>"));
    }
}
