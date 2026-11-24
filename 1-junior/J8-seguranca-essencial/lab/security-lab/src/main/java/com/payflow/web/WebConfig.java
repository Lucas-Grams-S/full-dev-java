package com.payflow.web;

import com.payflow.config.PayflowProperties;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PayflowProperties properties;

    public WebConfig(PayflowProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }

    /** Como na J6.08: só as origens configuradas (lista vazia = nenhuma). */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = properties.cors().allowedOrigins();
        if (!origins.isEmpty()) {
            registry.addMapping("/api/v1/**")
                    .allowedOrigins(origins.toArray(String[]::new))
                    .allowedMethods("GET", "POST", "OPTIONS")
                    .allowedHeaders("Authorization", "Content-Type", "Idempotency-Key")
                    .exposedHeaders("Location")
                    .maxAge(3600);
        }
    }
}
