package com.payflow.lab.obs;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.ApplicationContext;

/** P6.06: liveness × readiness. A pergunta de cada sonda é DIFERENTE; misturá-las derruba o serviço. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.tracing.export.otlp.enabled=false"})
class HealthTest {
    @LocalManagementPort
    int mgmt;
    @Autowired
    LabState state;
    @Autowired
    ApplicationContext ctx;

    private String scenario = "";

    @BeforeEach
    void name(TestInfo info) {
        scenario = info.getDisplayName().replace("()", "");
    }

    @AfterEach
    void restore() {
        state.dbUp.set(true);
        state.riskUp.set(true);
        AvailabilityChangeEvent.publish(ctx, LivenessState.CORRECT);
        AvailabilityChangeEvent.publish(ctx, ReadinessState.ACCEPTING_TRAFFIC);
    }

    private Http.Res probe(String path) {
        Http.Res r = Http.get(mgmt, path, Map.of());
        LogLines.print("T3", "[" + scenario + "] GET " + path, r.status() + " " + r.body());
        return r;
    }

    @Test
    void everythingUp() {
        assertThat(probe("/actuator/health").status()).isEqualTo(200);
        assertThat(probe("/actuator/health/liveness").status()).isEqualTo(200);
        assertThat(probe("/actuator/health/readiness").status()).isEqualTo(200);
    }

    @Test
    void criticalDependencyDownTakesTheInstanceOutOfRotationButDoesNotRestartIt() {
        state.dbUp.set(false);
        assertThat(probe("/actuator/health/readiness").status()).as("readiness: sem banco não atende").isEqualTo(503);
        assertThat(probe("/actuator/health/liveness").status()).as("liveness: o processo está bem, reiniciar não conserta o banco").isEqualTo(200);
    }

    @Test
    void optionalDependencyDownDoesNotRemoveTheInstanceFromRotation() {
        state.riskUp.set(false);
        assertThat(probe("/actuator/health/readiness").status()).isEqualTo(200);
        assertThat(probe("/actuator/health/liveness").status()).isEqualTo(200);
        // mas o /health AGREGADO fica DOWN: quem o usa como sonda tira TODAS as instâncias do ar ao mesmo tempo
        assertThat(probe("/actuator/health").status()).as("agregado inclui o opcional").isEqualTo(503);
    }

    @Test
    void shutdownStyleRefusingTrafficMakesReadinessFailWhileLivenessStaysUp() {
        AvailabilityChangeEvent.publish(ctx, ReadinessState.REFUSING_TRAFFIC);
        assertThat(probe("/actuator/health/readiness").status()).isEqualTo(503);
        assertThat(probe("/actuator/health/liveness").status()).isEqualTo(200);
    }

    @Test
    void brokenLivenessIsTheOnlyWayToAskForARestart() {
        AvailabilityChangeEvent.publish(ctx, LivenessState.BROKEN);
        assertThat(probe("/actuator/health/liveness").status()).isEqualTo(503);
    }

    @Test
    void detailsAreHiddenByDefaultSoTheProbeDoesNotLeakInternals() {
        state.dbUp.set(false);
        Http.Res r = Http.get(mgmt, "/actuator/health", Map.of());
        assertThat(r.body()).doesNotContain("connection refused").doesNotContain("components");
    }
}
