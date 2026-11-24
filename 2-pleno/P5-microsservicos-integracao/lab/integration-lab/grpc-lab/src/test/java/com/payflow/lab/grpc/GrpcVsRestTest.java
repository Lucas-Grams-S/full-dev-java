package com.payflow.lab.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payflow.lab.grpc.antifraud.v1.AntifraudServiceGrpc;
import com.payflow.lab.grpc.antifraud.v1.Decision;
import com.payflow.lab.grpc.antifraud.v1.ScoreRequest;
import com.payflow.lab.grpc.antifraud.v1.ScoreResponse;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GrpcVsRestTest {

    static int grpcPort;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            grpcPort = s.getLocalPort();
        }
        r.add("spring.grpc.server.port", () -> grpcPort);
    }

    @LocalServerPort int httpPort;
    @Autowired AntifraudGrpcService service;
    static ManagedChannel channel;

    ManagedChannel channel() {
        if (channel == null) {
            channel = io.grpc.ManagedChannelBuilder.forAddress("127.0.0.1", grpcPort).usePlaintext().build();
        }
        return channel;
    }

    @AfterAll
    static void close() throws InterruptedException {
        if (channel != null) channel.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
    }

    static ScoreRequest request(String amount) {
        return ScoreRequest.newBuilder().setTransferId("0198a1c2-7b3e-7d10-9a55-3f0c2a1b9e01")
                .setSourceAccountId("0198a1c2-7b3e-7d10-9a55-3f0c2a1b9e02").setDestinationAccountId("0198a1c2-7b3e-7d10-9a55-3f0c2a1b9e03")
                .setAmount(amount).setCurrency("BRL").build();
    }

    @Test
    void unaryCallWorksAndDecisionIsAnEnum() {
        var stub = AntifraudServiceGrpc.newBlockingStub(channel());
        ScoreResponse r = stub.score(request("15000.00"));
        System.out.println("T3G Score(15000.00) -> score=" + r.getScore() + " decision=" + r.getDecision());
        assertThat(r.getDecision()).isEqualTo(Decision.DENY);
        assertThat(stub.score(request("10.00")).getDecision()).isEqualTo(Decision.ALLOW);
    }

    @Test
    void payloadSizeProtobufVsJson() throws Exception {
        ScoreRequest req = request("15000.00");
        int proto = req.toByteArray().length;
        String json = "{\"transferId\":\"0198a1c2-7b3e-7d10-9a55-3f0c2a1b9e01\",\"sourceAccountId\":\"0198a1c2-7b3e-7d10-9a55-3f0c2a1b9e02\","
                + "\"destinationAccountId\":\"0198a1c2-7b3e-7d10-9a55-3f0c2a1b9e03\",\"amount\":\"15000.00\",\"currency\":\"BRL\"}";
        int js = json.getBytes(StandardCharsets.UTF_8).length;
        int respProto = ScoreResponse.newBuilder().setScore(90).setDecision(Decision.DENY).build().toByteArray().length;
        int respJson = "{\"score\":90,\"decision\":\"DENY\"}".length();
        System.out.println("T3G tamanho do corpo: requisicao protobuf=" + proto + " bytes, JSON=" + js + " bytes; resposta protobuf=" + respProto + " bytes, JSON=" + respJson + " bytes");
        assertThat(proto).isLessThan(js);
    }

    @Test
    void latencyRestVsGrpcOnLoopbackIsOrderOfMagnitudeOnly() throws Exception {
        var stub = AntifraudServiceGrpc.newBlockingStub(channel());
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        String body = "{\"transferId\":\"t\",\"sourceAccountId\":\"a\",\"destinationAccountId\":\"b\",\"amount\":\"15000.00\",\"currency\":\"BRL\"}";
        HttpRequest hr = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + httpPort + "/api/v1/antifraud/score"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        for (int i = 0; i < 2_000; i++) { stub.score(request("15000.00")); http.send(hr, HttpResponse.BodyHandlers.ofString()); } // aquecimento
        int n = 3_000;
        long best[] = new long[2];
        for (int round = 0; round < 3; round++) {
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) stub.score(request("15000.00"));
            long g = (System.nanoTime() - t0) / n / 1000;
            t0 = System.nanoTime();
            for (int i = 0; i < n; i++) http.send(hr, HttpResponse.BodyHandlers.ofString());
            long h = (System.nanoTime() - t0) / n / 1000;
            System.out.println("T3G rodada " + (round + 1) + ": gRPC " + g + " us/chamada, REST/JSON HTTP/1.1 " + h + " us/chamada (loopback, 1 cliente sequencial, " + n + " chamadas)");
        }
    }

    @Test
    void deadlineExceededWhenServerIsSlow() {
        service.delayMs = 1_000;
        try {
            var stub = AntifraudServiceGrpc.newBlockingStub(channel()).withDeadlineAfter(200, TimeUnit.MILLISECONDS);
            long t0 = System.nanoTime();
            assertThatThrownBy(() -> stub.score(request("10.00"))).isInstanceOfSatisfying(StatusRuntimeException.class, e -> {
                System.out.println("T3G deadline 200 ms contra servidor de 1000 ms: " + e.getStatus().getCode() + " | " + e.getMessage());
                assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
            });
            System.out.println("T3G ... o chamador foi liberado apos " + (System.nanoTime() - t0) / 1_000_000 + " ms");
        } finally {
            service.delayMs = 0;
        }
    }

    @Test
    void domainErrorsAreStatusCodesNotHttpStatus() {
        var stub = AntifraudServiceGrpc.newBlockingStub(channel());
        assertThatThrownBy(() -> stub.score(request("abc"))).isInstanceOfSatisfying(StatusRuntimeException.class, e ->
                System.out.println("T3G amount invalido -> " + e.getStatus().getCode() + " | " + e.getStatus().getDescription()));
    }

    @Test
    void serverDownIsUnavailable() throws Exception {
        ManagedChannel dead = io.grpc.ManagedChannelBuilder.forAddress("127.0.0.1", 9).usePlaintext().build();
        try {
            var stub = AntifraudServiceGrpc.newBlockingStub(dead);
            long t0 = System.nanoTime();
            assertThatThrownBy(() -> stub.score(request("10.00"))).isInstanceOfSatisfying(StatusRuntimeException.class, e ->
                    System.out.println("T3G servidor fora do ar: " + e.getStatus().getCode() + " | " + e.getMessage()));
            System.out.println("T3G ... falhou rapido, em " + (System.nanoTime() - t0) / 1_000_000 + " ms (SEM deadline configurado)");
        } finally {
            dead.shutdownNow();
        }
    }

    @Test
    void noDeadlineByDefaultMeansWaitForever_provedWithLongDelay() {
        service.delayMs = 1_500;
        try {
            var stub = AntifraudServiceGrpc.newBlockingStub(channel()); // sem deadline
            long t0 = System.nanoTime();
            stub.score(request("10.00"));
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.println("T3G SEM deadline: o chamador esperou " + ms + " ms (o padrao do gRPC e nao ter prazo)");
            assertThat(ms).isGreaterThanOrEqualTo(1_500);
        } finally {
            service.delayMs = 0;
        }
    }
}
