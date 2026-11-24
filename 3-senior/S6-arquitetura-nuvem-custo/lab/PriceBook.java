import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê prices.csv (gerado por fetch-prices.py a partir da AWS Price List).
 * Usado pelos outros programas do laboratório (lançador de código-fonte com vários arquivos, JEP 458).
 * Preço ausente = erro: nenhum número do laboratório é inventado em silêncio.
 */
final class PriceBook {
    record Price(String key, String region, double usd, String unit, String description, String source, String usageType) {}

    private final Map<String, Price> prices = new LinkedHashMap<>();

    PriceBook(Path csv) {
        List<String> lines;
        try {
            lines = Files.readAllLines(csv);
        } catch (IOException e) {
            throw new IllegalStateException("não li " + csv + " (rode fetch-prices.py)", e);
        }
        for (String line : lines.subList(1, lines.size())) {
            List<String> f = split(line);
            prices.put(f.get(0) + "@" + f.get(1),
                    new Price(f.get(0), f.get(1), Double.parseDouble(f.get(2)), f.get(3), f.get(4), f.get(6), f.get(7)));
        }
    }

    static PriceBook load() {
        return new PriceBook(Path.of(System.getProperty("prices", "prices.csv")));
    }

    double usd(String key, String region) {
        Price p = prices.get(key + "@" + region);
        if (p == null) throw new IllegalArgumentException("preço ausente: " + key + " em " + region);
        return p.usd();
    }

    Price price(String key, String region) {
        usd(key, region);
        return prices.get(key + "@" + region);
    }

    /** Data de publicação da oferta (coluna source: "Oferta AAAA-MM-DD"). */
    String published(String key, String region) {
        String s = price(key, region).source();
        return s.substring(s.lastIndexOf(' ') + 1);
    }

    /** Menor e maior data de publicação das ofertas da Price List usadas (ignora os dados do Spot Advisor). */
    String publishedRange() {
        var dates = prices.values().stream().map(Price::source).filter(x -> x.matches(".* \\d{4}-\\d{2}-\\d{2}"))
                .map(x -> x.substring(x.lastIndexOf(' ') + 1)).sorted().toList();
        return dates.getFirst() + " to " + dates.getLast();
    }

    static final double HOURS_PER_MONTH = 730; // convenção das páginas de preço da AWS (8.760 h / 12)

    private static List<String> split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }
}
