package com.payflow.support;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Fala com o Keycloak do laboratório (./kc-up.sh). Se ele não estiver no ar, o teste FALHA (nunca passa em silêncio). */
public final class KeycloakFixture {

    public static final String ISSUER = "http://127.0.0.1:8180/realms/payflow";
    public static final String OIDC = ISSUER + "/protocol/openid-connect";
    private static final ObjectMapper JSON = new ObjectMapper();

    private KeycloakFixture() { }

    public static HttpClient client() {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public static void requireRunning() {
        try {
            HttpResponse<String> r = client().send(HttpRequest.newBuilder(URI.create(ISSUER + "/.well-known/openid-configuration")).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                throw new IllegalStateException("status " + r.statusCode());
            }
        } catch (Exception e) {
            throw new AssertionError("Keycloak is not running on 127.0.0.1:8180: run lab/keycloak-lab/kc-up.sh first (" + e + ")");
        }
    }

    public static String encode(Map<String, String> form) {
        return form.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /** POST no endpoint de token; devolve o status e o JSON (tanto sucesso quanto erro). */
    public static Result token(Map<String, String> form) {
        return post(OIDC + "/token", form);
    }

    public static Result post(String url, Map<String, String> form) {
        try {
            HttpResponse<String> r = client().send(HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(encode(form))).build(), HttpResponse.BodyHandlers.ofString());
            return new Result(r.statusCode(), r.body().isBlank() ? JSON.createObjectNode() : JSON.readTree(r.body()));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Token de usuário pelo cliente SÓ DO LABORATÓRIO (senha direta: o fluxo que o OAuth 2.1 removeu). */
    public static JsonNode userTokens(String email, String password, String scope) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "password");
        form.put("client_id", "payflow-lab-direct");
        form.put("username", email);
        form.put("password", password);
        form.put("scope", scope);
        Result r = token(form);
        if (r.status != 200) {
            throw new AssertionError("token endpoint answered " + r.status + ": " + r.body);
        }
        return r.body;
    }

    public static JsonNode serviceToken(String clientId, String secret, String scope) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "client_credentials");
        form.put("client_id", clientId);
        form.put("client_secret", secret);
        if (scope != null) {
            form.put("scope", scope);
        }
        Result r = token(form);
        if (r.status != 200) {
            throw new AssertionError("token endpoint answered " + r.status + ": " + r.body);
        }
        return r.body;
    }

    /** O corpo (claims) de um JWT, sem validar nada: só para os testes olharem o que o Keycloak emitiu. */
    public static JsonNode claims(String jwt) {
        return JSON.readTree(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]));
    }

    public static JsonNode header(String jwt) {
        return JSON.readTree(Base64.getUrlDecoder().decode(jwt.split("\\.")[0]));
    }

    public record Result(int status, JsonNode body) { }

    /** Token do administrador do realm "master" (só do laboratório), para chamar a Admin REST API. */
    public static String adminToken() {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "password");
        form.put("client_id", "admin-cli");
        form.put("username", "admin");
        form.put("password", "lab-admin-only");
        Result r = post("http://127.0.0.1:8180/realms/master/protocol/openid-connect/token", form);
        return r.body.get("access_token").asText();
    }

    public static int admin(String method, String path, String jsonBody) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8180/admin/realms/payflow" + path))
                    .header("Authorization", "Bearer " + adminToken()).header("Content-Type", "application/json");
            HttpRequest request = b.method(method, jsonBody == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(jsonBody)).build();
            HttpResponse<String> r = client().send(request, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() >= 400) {
                System.out.println("T10 admin " + method + " " + path + " -> " + r.statusCode() + " " + r.body());
            }
            return r.statusCode();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String adminBody(String method, String path, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8180/admin/realms/payflow" + path))
                    .header("Authorization", "Bearer " + adminToken()).header("Content-Type", "application/json")
                    .method(method, jsonBody == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(jsonBody)).build();
            return client().send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }
}
