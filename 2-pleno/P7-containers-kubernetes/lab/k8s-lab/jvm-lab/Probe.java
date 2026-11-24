import java.lang.management.*;
import java.nio.ByteBuffer;
import java.util.*;
public class Probe {
    public static void main(String[] a) throws Exception {
        String mode = a.length > 0 ? a[0] : "info";
        Runtime r = Runtime.getRuntime();
        if (mode.equals("info")) {
            String gc = ManagementFactory.getGarbageCollectorMXBeans().stream().map(b -> b.getName()).toList().toString();
            System.out.printf("cpus=%d maxHeapMiB=%d gc=%s%n", r.availableProcessors(), r.maxMemory() >> 20, gc);
        } else if (mode.equals("heap")) {
            List<byte[]> l = new ArrayList<>();
            try { while (true) l.add(new byte[10 << 20]); } catch (OutOfMemoryError e) { System.out.println("OOM heap apos " + l.size()*10 + " MiB: " + e.getMessage()); }
        } else if (mode.equals("direct")) {
            List<ByteBuffer> l = new ArrayList<>();
            try { while (true) { l.add(ByteBuffer.allocateDirect(10 << 20)); } } catch (OutOfMemoryError e) { System.out.println("OOM direct apos " + l.size()*10 + " MiB: " + e.getMessage()); }
        } else if (mode.equals("native")) {
            // toca a memoria para o kernel contabilizar (RSS)
            List<ByteBuffer> l = new ArrayList<>();
            for (int i = 0; i < 100000; i++) { ByteBuffer b = ByteBuffer.allocateDirect(10 << 20); for (int p = 0; p < b.capacity(); p += 4096) b.put(p, (byte)1); l.add(b); System.out.println("tocou " + (i+1)*10 + " MiB"); }
        }
    }
}
