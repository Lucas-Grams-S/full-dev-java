import java.io.*;
import java.util.*;

/**
 * S2.04 - le um heap dump (.hprof, formato "JAVA PROFILE 1.0.2") e imprime o HISTOGRAMA de
 * classes: instancias e bytes de CAMPOS/ELEMENTOS (sem o cabecalho de objeto).
 * Nao calcula dominadores nem tamanho retido: para isso use o Eclipse MAT ou o VisualVM.
 *   java HprofHisto.java <arquivo.hprof> [N]
 */
public class HprofHisto {
    static int idSize;
    static final Map<Long, String> utf8 = new HashMap<>();
    static final Map<Long, String> classNames = new HashMap<>();
    static final Map<String, long[]> histo = new HashMap<>();   // nome -> {instancias, bytes}

    static long readId(DataInputStream in) throws IOException { return idSize == 4 ? in.readInt() & 0xFFFFFFFFL : in.readLong(); }

    static int typeSize(int t) {
        return switch (t) { case 2 -> idSize; case 4, 8 -> 1; case 5, 9 -> 2; case 6, 10 -> 4; case 7, 11 -> 8;
            default -> throw new IllegalStateException("tipo " + t); };
    }

    static String primName(int t) {
        return switch (t) { case 4 -> "boolean[]"; case 5 -> "char[]"; case 6 -> "float[]"; case 7 -> "double[]";
            case 8 -> "byte[]"; case 9 -> "short[]"; case 10 -> "int[]"; case 11 -> "long[]"; default -> "?[]"; };
    }

    static void add(String name, long bytes) {
        long[] v = histo.computeIfAbsent(name, k -> new long[2]);
        v[0]++; v[1] += bytes;
    }

    static String cname(long id) {
        String n = classNames.get(id);
        return n == null ? "classe#" + Long.toHexString(id) : n.replace('/', '.');
    }

    public static void main(String[] args) throws Exception {
        int top = args.length > 1 ? Integer.parseInt(args[1]) : 12;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(args[0]), 1 << 20))) {
            while (in.readByte() != 0) { /* cabecalho texto */ }
            idSize = in.readInt();
            in.readLong();
            Map<Long, String> pendingClassNameId = new HashMap<>();
            while (true) {
                int tag;
                try { tag = in.readUnsignedByte(); } catch (EOFException e) { break; }
                in.readInt();
                long len = in.readInt() & 0xFFFFFFFFL;
                if (tag == 0x01) {
                    long id = readId(in);
                    byte[] b = new byte[(int) len - idSize];
                    in.readFully(b);
                    utf8.put(id, new String(b, "UTF-8"));
                } else if (tag == 0x02) {
                    in.readInt(); long classId = readId(in); in.readInt(); long nameId = readId(in);
                    classNames.put(classId, utf8.getOrDefault(nameId, "?"));
                } else if (tag == 0x0C || tag == 0x1C) {
                    heap(in, len);
                } else {
                    in.skipNBytes(len);
                }
            }
        }
        long totI = 0, totB = 0;
        for (long[] v : histo.values()) { totI += v[0]; totB += v[1]; }
        System.out.printf(Locale.ROOT, "%-46s %12s %14s%n", "classe", "instancias", "bytes");
        histo.entrySet().stream().sorted((x, y) -> Long.compare(y.getValue()[1], x.getValue()[1])).limit(top)
                .forEach(e -> System.out.printf(Locale.ROOT, "%-46s %,12d %,14d%n", e.getKey(), e.getValue()[0], e.getValue()[1]));
        System.out.printf(Locale.ROOT, "%-46s %,12d %,14d%n", "TOTAL (todas as classes)", totI, totB);
    }

    static void heap(DataInputStream in, long len) throws IOException {
        long read = 0;
        while (read < len) {
            int st = in.readUnsignedByte(); read++;
            switch (st) {
                case 0xFF, 0x05, 0x07 -> { readId(in); read += idSize; }
                case 0x01 -> { readId(in); readId(in); read += 2L * idSize; }
                case 0x02, 0x03, 0x08 -> { readId(in); in.readInt(); in.readInt(); read += idSize + 8; }
                case 0x04, 0x06 -> { readId(in); in.readInt(); read += idSize + 4; }
                case 0x20 -> {
                    long id = readId(in); in.readInt(); readId(in); readId(in); readId(in); readId(in); readId(in); readId(in);
                    in.readInt(); read += 7L * idSize + 8;
                    int cp = in.readUnsignedShort(); read += 2;
                    for (int i = 0; i < cp; i++) { in.readUnsignedShort(); int t = in.readUnsignedByte(); in.skipNBytes(typeSize(t)); read += 3 + typeSize(t); }
                    int sf = in.readUnsignedShort(); read += 2;
                    for (int i = 0; i < sf; i++) { readId(in); int t = in.readUnsignedByte(); in.skipNBytes(typeSize(t)); read += idSize + 1 + typeSize(t); }
                    int inf = in.readUnsignedShort(); read += 2;
                    for (int i = 0; i < inf; i++) { readId(in); in.readUnsignedByte(); read += idSize + 1; }
                }
                case 0x21 -> {
                    readId(in); in.readInt(); long cid = readId(in); long n = in.readInt() & 0xFFFFFFFFL;
                    in.skipNBytes(n); read += 2L * idSize + 8 + n;
                    add(cname(cid), n);
                }
                case 0x22 -> {
                    readId(in); in.readInt(); long n = in.readInt() & 0xFFFFFFFFL; long cid = readId(in);
                    in.skipNBytes(n * idSize); read += 2L * idSize + 8 + n * idSize;
                    add(cname(cid), n * idSize);
                }
                case 0x23 -> {
                    readId(in); in.readInt(); long n = in.readInt() & 0xFFFFFFFFL; int t = in.readUnsignedByte();
                    in.skipNBytes(n * typeSize(t)); read += idSize + 9 + n * typeSize(t);
                    add(primName(t), n * typeSize(t));
                }
                default -> throw new IllegalStateException("sub-registro 0x" + Integer.toHexString(st));
            }
        }
    }
}
