package com.payflow.labs;

import com.payflow.repository.InMemoryData;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

/** J8.01: a cadeia de filtros de verdade, na ordem em que a requisição passa. */
@SpringBootTest
@ActiveProfiles("test")
class FilterChainLabTest {

    @Autowired FilterChainProxy proxy;
    @Autowired InMemoryData data;

    @Test
    void printTheChains() {
        int n = 1;
        for (SecurityFilterChain chain : proxy.getFilterChains()) {
            System.out.println("[cadeia " + n++ + "] " + chain.getClass().getSimpleName() + " (" + chain.getFilters().size() + " filtros)");
            int i = 1;
            for (var filter : chain.getFilters()) {
                System.out.println("   " + (i++) + ". " + filter.getClass().getSimpleName());
            }
        }
    }
}
