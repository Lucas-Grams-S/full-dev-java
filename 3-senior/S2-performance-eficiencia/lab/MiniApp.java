import com.sun.net.httpserver.HttpServer;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * S2.07 - um "servico" minimo para medir PARTIDA: sobe um servidor HTTP, chama a si mesmo uma vez,
 * valida e formata a resposta (regex, java.time, streams, lambdas, String.format) e sai.
 * Nao e o PayFlow e nao usa Spring: serve so para comparar formas de iniciar a JVM.
 */
public class MiniApp {
    public static void main(String[] args) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/accounts", ex -> {
            String body = IntStream.range(0, 5).mapToObj(i -> String.format(Locale.ROOT, "{\"id\":%d,\"balance\":\"%d.00\"}", i, 100 * i))
                    .collect(Collectors.joining(",", "[", "]"));
            byte[] b = body.getBytes();
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        s.start();
        HttpClient c = HttpClient.newHttpClient();
        HttpResponse<String> r = c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + s.getAddress().getPort() + "/accounts")).build(),
                HttpResponse.BodyHandlers.ofString());
        Matcher m = Pattern.compile("\"balance\":\"(\\d+\\.\\d{2})\"").matcher(r.body());
        List<String> balances = new ArrayList<>();
        while (m.find()) balances.add(m.group(1));
        String stamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        System.out.println(r.statusCode() + " " + balances.size() + " saldos " + stamp.length());
        s.stop(0);
        c.close();
    }
}
