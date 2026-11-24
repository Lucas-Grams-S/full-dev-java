import com.sun.net.httpserver.HttpServer;
import jdk.crac.Context;
import jdk.crac.Core;
import jdk.crac.Resource;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * S2.07 - servidor HTTP minimo que FICA no ar, preparado para o CRaC (Azul Zulu 25 com CRaC).
 * O CRaC NAO deixa um socket de escuta aberto no checkpoint: o recurso fecha o servidor ANTES
 * e o recria DEPOIS do restore. (Frameworks como o Spring fazem isso por voce.) Nao e o PayFlow.
 */
public class CracApp implements Resource {
    static final Pattern BAL = Pattern.compile("\"balance\":\"(\\d+\\.\\d{2})\"");
    HttpServer server;

    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/accounts", ex -> {
            String body = IntStream.range(0, 5).mapToObj(i -> String.format(Locale.ROOT, "{\"id\":%d,\"balance\":\"%d.00\"}", i, 100 * i))
                    .collect(Collectors.joining(",", "[", "]"));
            BAL.matcher(body).results().count();
            byte[] b = body.getBytes();
            ex.sendResponseHeaders(200, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        });
        server.start();
    }

    @Override public void beforeCheckpoint(Context<? extends Resource> ctx) { server.stop(0); System.out.println("antes do checkpoint: servidor parado"); }
    @Override public void afterRestore(Context<? extends Resource> ctx) throws Exception { start(); System.out.println("depois do restore: servidor recriado"); }

    public static void main(String[] a) throws Exception {
        CracApp app = new CracApp();
        Core.getGlobalContext().register(app);
        app.start();
        System.out.println("pronto");
        Thread.currentThread().join();
    }
}
