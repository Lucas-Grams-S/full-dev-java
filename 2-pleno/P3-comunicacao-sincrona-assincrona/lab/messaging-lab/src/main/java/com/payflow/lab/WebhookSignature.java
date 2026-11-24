package com.payflow.lab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** P3.10: assinatura HMAC-SHA256 de webhook, no formato "t=<epoch>,v1=<hex>", com janela anti-replay. */
public final class WebhookSignature {
    private WebhookSignature() {}

    public static String sign(String secret, long timestamp, byte[] body) {
        return "t=" + timestamp + ",v1=" + hmacHex(secret, timestamp, body);
    }

    static String hmacHex(String secret, long timestamp, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            mac.update(body);                                  // os BYTES recebidos, não um JSON reserializado
            return HexFormat.of().formatHex(mac.doFinal());
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static boolean verify(String secret, String header, byte[] body, Clock clock, long toleranceSeconds) {
        if (header == null) return false;
        Long t = null; String v1 = null;
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) continue;
            if (kv[0].equals("t")) { try { t = Long.parseLong(kv[1]); } catch (NumberFormatException e) { return false; } }
            if (kv[0].equals("v1")) v1 = kv[1];
        }
        if (t == null || v1 == null) return false;
        if (Math.abs(clock.instant().getEpochSecond() - t) > toleranceSeconds) return false;   // replay antigo
        byte[] expected = hmacHex(secret, t, body).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, v1.getBytes(StandardCharsets.UTF_8));         // tempo constante
    }
}
