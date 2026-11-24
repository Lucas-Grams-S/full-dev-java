import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** S3.09: the Java twin of the SQL function normalize_phone_br. Brazilian numbers only; anything else -> null (goes to review, never guessed). */
public final class PhoneNormalizer {
    private static final Pattern WITH_COUNTRY = Pattern.compile("^55[1-9][0-9]{9,10}$");
    private static final Pattern WITH_TRUNK   = Pattern.compile("^0[1-9][0-9]{9,10}$");
    private static final Pattern LANDLINE     = Pattern.compile("^[1-9][0-9][2-9][0-9]{7}$");
    private static final Pattern MOBILE       = Pattern.compile("^[1-9][0-9]9[0-9]{8}$");

    private PhoneNormalizer() {}

    /** @return +55DDNNNNNNNNN or null when the input is not a valid Brazilian number. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        String national = WITH_COUNTRY.matcher(digits).matches() ? digits.substring(2)
                        : WITH_TRUNK.matcher(digits).matches()   ? digits.substring(1) : digits;
        return LANDLINE.matcher(national).matches() || MOBILE.matcher(national).matches() ? "+55" + national : null;
    }

    /** modes: cases <file.tsv> (input TAB expected, "-" = null) | crosscheck <file.tsv> (id TAB phone TAB stored_e164) */
    public static void main(String[] a) throws Exception {
        int bad = 0, n = 0;
        for (String line : Files.readAllLines(Path.of(a[1]))) {
            String[] f = line.split("\t", -1);
            if (a[0].equals("cases")) {
                String got = normalize(f[0].equals("-") ? null : f[0]), want = f[1].equals("-") ? null : f[1];
                n++; if (!Objects.equals(got, want)) { bad++; System.out.println("MISMATCH [" + f[0] + "] java=" + got + " expected=" + want); }
            } else {
                String got = normalize(f[1].isEmpty() ? null : f[1]), stored = f[2].isEmpty() ? null : f[2];
                n++; if (!Objects.equals(got, stored)) { if (bad++ < 5) System.out.println("DRIFT id=" + f[0] + " java=" + got + " stored=" + stored); }
            }
        }
        System.out.printf("%s: %d rows checked, %d mismatches%n", a[0], n, bad);
        if (bad > 0) System.exit(1);
    }
}
