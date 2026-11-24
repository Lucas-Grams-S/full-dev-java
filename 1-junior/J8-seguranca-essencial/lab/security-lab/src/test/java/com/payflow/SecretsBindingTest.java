package com.payflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.config.PayflowProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/** J8.07: de onde o segredo pode vir, e o que acontece quando ele falta. */
class SecretsBindingTest {

    @EnableConfigurationProperties(PayflowProperties.class)
    static class Config { }

    final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    static Throwable root(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    @Test
    void anEnvironmentVariableBecomesTheProperty() {
        runner.withInitializer(ctx -> ctx.getEnvironment().getPropertySources().addLast(new SystemEnvironmentPropertySource(
                        "systemEnvironment", Map.of("PAYFLOW_SECURITY_JWT_SECRET", "x".repeat(40)))))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    int length = ctx.getBean(PayflowProperties.class).security().jwt().secret().length();
                    System.out.println("[segredo] PAYFLOW_SECURITY_JWT_SECRET (40 caracteres) -> payflow.security.jwt.secret com " + length + " caracteres");
                    assertThat(length).isEqualTo(40);
                });
    }

    @Test
    void aMountedSecretFileBecomesTheProperty(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("payflow.security.jwt.secret"), "y".repeat(48));
        runner.withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.import=configtree:" + dir + "/")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    int length = ctx.getBean(PayflowProperties.class).security().jwt().secret().length();
                    System.out.println("[segredo] arquivo payflow.security.jwt.secret via configtree: -> " + length + " caracteres");
                    assertThat(length).isEqualTo(48);
                });
    }

    @Test
    void withoutTheSecretTheApplicationDoesNotStart() {
        runner.run(ctx -> {
            assertThat(ctx).hasFailed();
            Throwable root = root(ctx.getStartupFailure());
            System.out.println("[segredo] sem segredo -> " + root.getClass().getSimpleName() + ": " + root.getMessage());
            assertThat(root.getMessage()).contains("at least 32 bytes");
        });
    }

    @Test
    void aShortSecretDoesNotStartEither() {
        runner.withPropertyValues("payflow.security.jwt.secret=curto").run(ctx -> {
            assertThat(ctx).hasFailed();
            System.out.println("[segredo] segredo de 5 caracteres -> " + root(ctx.getStartupFailure()).getMessage());
        });
    }
}
