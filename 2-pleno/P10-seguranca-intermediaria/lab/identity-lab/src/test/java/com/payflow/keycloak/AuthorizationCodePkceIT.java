package com.payflow.keycloak;

import static org.assertj.core.api.Assertions.assertThat;

import com.payflow.support.KeycloakFixture;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * O fluxo Authorization Code + PKCE inteiro, com um "navegador" de 30 linhas (cookies + redirecionamentos à mão)
 * contra o Keycloak real: cada passo é o que o navegador do usuário faria.
 */
class AuthorizationCodePkceIT {

    private static final String REDIRECT = "http://127.0.0.1:8099/callback";

    @BeforeAll
    static void up() {
        KeycloakFixture.requireRunning();
    }

    /** O estado de uma tentativa de login: o verificador PKCE, o state e o cookie jar do "navegador". */
    private static final class Attempt {
        // Um "navegador": o CookieManager do Java NÃO devolve cookies marcados Secure por HTTP (o Keycloak marca
        // KC_RESTART e outros mesmo em http://127.0.0.1), e o login dá 400. Aqui o jar de cookies é manual.
        final HttpClient browser = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        final Map<String, String> cookies = new LinkedHashMap<>();
        final String verifier = random(48);
        final String state = random(12);
        final String nonce = random(12);

        String challenge() throws Exception {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        }

        HttpResponse<String> authorize(String scope, boolean withPkce, String redirectUri) throws Exception {
            Map<String, String> q = new LinkedHashMap<>();
            q.put("client_id", "payflow-web");
            q.put("response_type", "code");
            q.put("redirect_uri", redirectUri);
            q.put("scope", scope);
            q.put("state", state);
            q.put("nonce", nonce);
            if (withPkce) {
                q.put("code_challenge", challenge());
                q.put("code_challenge_method", "S256");
            }
            return send(HttpRequest.newBuilder(URI.create(KeycloakFixture.OIDC + "/auth?" + KeycloakFixture.encode(q))));
        }

        HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
            if (!cookies.isEmpty()) {
                request.header("Cookie", cookies.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                        .collect(java.util.stream.Collectors.joining("; ")));
            }
            HttpResponse<String> response = browser.send(request.build(), HttpResponse.BodyHandlers.ofString());
            for (String setCookie : response.headers().allValues("Set-Cookie")) {
                String pair = setCookie.split(";", 2)[0];
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && !kv[1].isEmpty()) {
                    cookies.put(kv[0], kv[1]);
                } else {
                    cookies.remove(kv[0]);
                }
            }
            return response;
        }

        /** Preenche o formulário de login que o Keycloak devolveu. Devolve a resposta (302 com ?code= se deu certo). */
        HttpResponse<String> submitLogin(HttpResponse<String> loginPage, String user, String password) throws Exception {
            Matcher m = Pattern.compile("action=\"([^\"]*login-actions/authenticate[^\"]*)\"").matcher(loginPage.body());
            assertThat(m.find()).as("the login form was found").isTrue();
            String action = m.group(1).replace("&amp;", "&");
            Map<String, String> form = new LinkedHashMap<>();
            form.put("username", user);
            form.put("password", password);
            form.put("credentialId", "");
            return send(HttpRequest.newBuilder(URI.create(action)).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(KeycloakFixture.encode(form))));
        }

        KeycloakFixture.Result exchange(String code, String codeVerifier) {
            Map<String, String> form = new LinkedHashMap<>();
            form.put("grant_type", "authorization_code");
            form.put("client_id", "payflow-web");
            form.put("code", code);
            form.put("redirect_uri", REDIRECT);
            form.put("code_verifier", codeVerifier);
            return KeycloakFixture.token(form);
        }
    }

    private static String random(int bytes) {
        byte[] b = new byte[bytes];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static Map<String, String> queryOf(String location) {
        Map<String, String> params = new LinkedHashMap<>();
        String query = URI.create(location).getRawQuery();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(kv[0], URLDecoder.decode(kv.length > 1 ? kv[1] : "", StandardCharsets.UTF_8));
        }
        return params;
    }

    /** Faz o login do usuário e devolve o que veio no redirecionamento para o redirect_uri. */
    private static Map<String, String> login(Attempt a, String scope) throws Exception {
        HttpResponse<String> page = a.authorize(scope, true, REDIRECT);
        assertThat(page.statusCode()).isEqualTo(200);
        HttpResponse<String> done = a.submitLogin(page, "alice@example.com", "alice-lab-pass-1");
        assertThat(done.statusCode()).isEqualTo(302);
        String location = done.headers().firstValue("Location").orElseThrow();
        assertThat(location).startsWith(REDIRECT);
        return queryOf(location);
    }

    @Test
    void happyPath_codeThenTokens_withStateNonceAndIssuerChecked() throws Exception {
        Attempt a = new Attempt();
        Map<String, String> callback = login(a, "openid transfers:read");
        System.out.println("T10.01 callback params=" + callback.keySet());
        assertThat(callback.get("state")).isEqualTo(a.state);                          // anti-CSRF do fluxo: o cliente confere
        assertThat(callback.get("iss")).isEqualTo(KeycloakFixture.ISSUER);              // RFC 9207: quem respondeu
        assertThat(callback.get("code")).isNotBlank();

        KeycloakFixture.Result tokens = a.exchange(callback.get("code"), a.verifier);
        assertThat(tokens.status()).isEqualTo(200);
        JsonNode access = KeycloakFixture.claims(tokens.body().get("access_token").asText());
        JsonNode id = KeycloakFixture.claims(tokens.body().get("id_token").asText());
        System.out.println("T10.01 code length=" + callback.get("code").length() + " expires_in=" + tokens.body().get("expires_in")
                + " refresh_expires_in=" + tokens.body().get("refresh_expires_in"));
        System.out.println("T10.01 access azp=" + access.get("azp").asText() + " scope=" + access.get("scope").asText()
                + " aud=" + access.get("aud"));
        System.out.println("T10.01 id nonce matches=" + id.get("nonce").asText().equals(a.nonce) + " aud=" + id.get("aud"));
        assertThat(access.get("azp").asText()).isEqualTo("payflow-web");
        assertThat(access.get("sub").asText()).isEqualTo("11111111-1111-4111-8111-111111111111");
        assertThat(id.get("nonce").asText()).isEqualTo(a.nonce);
        assertThat(id.get("aud").asText()).isEqualTo("payflow-web");
    }

    @Test
    void stolenCodeIsUselessWithoutTheVerifier() throws Exception {
        Attempt victim = new Attempt();
        Map<String, String> callback = login(victim, "openid");
        KeycloakFixture.Result attacker = victim.exchange(callback.get("code"), random(48));      // o atacante viu o code, não o verifier
        System.out.println("T10.01 wrong verifier -> " + attacker.status() + " " + attacker.body());
        assertThat(attacker.status()).isEqualTo(400);
        assertThat(attacker.body().get("error").asText()).isEqualTo("invalid_grant");
    }

    @Test
    void codeIsSingleUse() throws Exception {
        Attempt a = new Attempt();
        Map<String, String> callback = login(a, "openid");
        assertThat(a.exchange(callback.get("code"), a.verifier).status()).isEqualTo(200);
        KeycloakFixture.Result again = a.exchange(callback.get("code"), a.verifier);
        System.out.println("T10.01 code reuse -> " + again.status() + " " + again.body());
        assertThat(again.status()).isEqualTo(400);
        assertThat(again.body().get("error").asText()).isEqualTo("invalid_grant");
    }

    @Test
    void theClientRequiresPkce_withoutTheChallengeTheFlowDoesNotStart() throws Exception {
        Attempt a = new Attempt();
        HttpResponse<String> response = a.authorize("openid", false, REDIRECT);
        String location = response.headers().firstValue("Location").orElse("(none)");
        System.out.println("T10.01 without PKCE -> " + response.statusCode() + " Location=" + location.replaceAll("&state=.*", "…"));
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(location).contains("error=invalid_request");
    }

    @Test
    void redirectUriMustMatchExactly_noLoginPageForAnAttackersAddress() throws Exception {
        Attempt a = new Attempt();
        HttpResponse<String> response = a.authorize("openid", true, "http://evil.example/callback");
        System.out.println("T10.01 evil redirect_uri -> " + response.statusCode());
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).doesNotContain("kc-form-login");
    }

    @Test
    void wrongPassword_showsTheLoginAgain_withoutRevealingWhichPartWasWrong() throws Exception {
        Attempt a = new Attempt();
        HttpResponse<String> page = a.authorize("openid", true, REDIRECT);
        HttpResponse<String> wrong = a.submitLogin(page, "alice@example.com", "nope-nope-nope");
        System.out.println("T10.01 wrong password -> " + wrong.statusCode());
        assertThat(wrong.statusCode()).isEqualTo(200);
        assertThat(wrong.body()).contains("Invalid username or password");
        HttpResponse<String> unknownUser = a.submitLogin(wrong, "nobody@example.com", "nope-nope-nope");
        assertThat(unknownUser.body()).contains("Invalid username or password");             // a mesma mensagem: não revela se o usuário existe
    }

    @Test
    void thePasswordNeverTravelsToThePayFlow_onlyTheCode() throws Exception {
        // o formulário de login é do Keycloak (outra origem): o PayFlow só vê o código no redirect
        Attempt a = new Attempt();
        HttpResponse<String> page = a.authorize("openid", true, REDIRECT);
        assertThat(page.uri().getPort()).isEqualTo(8180);
        HttpResponse<String> done = a.submitLogin(page, "alice@example.com", "alice-lab-pass-1");
        assertThat(done.headers().firstValue("Location").orElseThrow()).doesNotContain("alice-lab-pass-1");
    }
}
