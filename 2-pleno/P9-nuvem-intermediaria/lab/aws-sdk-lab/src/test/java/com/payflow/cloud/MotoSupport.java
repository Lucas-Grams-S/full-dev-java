package com.payflow.cloud;

import java.util.UUID;
import org.junit.jupiter.api.Assumptions;

/** Safety: these tests CREATE and DELETE resources. They run ONLY against a local emulator, never against real AWS. */
final class MotoSupport {

    private MotoSupport() {
    }

    static void requireLocalEmulator() {
        String endpoint = System.getenv("AWS_ENDPOINT_URL");
        Assumptions.assumeTrue(endpoint != null
                        && (endpoint.startsWith("http://localhost") || endpoint.startsWith("http://127.0.0.1")),
                "set AWS_ENDPOINT_URL=http://localhost:<port> (moto), AWS_REGION and fake credentials to run this test");
    }

    static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
