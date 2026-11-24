import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * S2.05 - carga ABERTA (as requisicoes chegam no ritmo combinado, mesmo se o servidor engasgar)
 * sobre um "servico" com conjunto vivo (cache) e muito lixo de vida curta.
 *   java [flags de GC] GcLab <segundos> <req/s> <conjuntoVivoMB> [aquecimentoSeg]
 * Latencia = fim - instante em que a requisicao DEVERIA ter comecado (inclui fila e pausas).
 */
public class GcLab {
    static final int CHUNK = 8 * 1024;
    static volatile long sink;
    static final int CHURN = Integer.getInteger("churn", 50);

    public static void main(String[] a) throws Exception {
        int secs = Integer.parseInt(a[0]), rate = Integer.parseInt(a[1]), liveMb = Integer.parseInt(a[2]);
        int warm = a.length > 3 ? Integer.parseInt(a[3]) : 5;
        int n = liveMb * 1024 * 1024 / CHUNK;
        AtomicReferenceArray<byte[]> live = new AtomicReferenceArray<>(n);
        for (int i = 0; i < n; i++) live.set(i, new byte[CHUNK]);

        int workers = 8;
        BlockingQueue<long[]> q = new LinkedBlockingQueue<>();      // {instante pretendido}
        long[] lat = new long[(secs + 1) * rate];
        AtomicInteger cnt = new AtomicInteger();
        long start = System.nanoTime();
        long warmEnd = start + warm * 1_000_000_000L;
        long end = start + secs * 1_000_000_000L;

        List<Thread> ws = new ArrayList<>();
        for (int w = 0; w < workers; w++) {
            ws.add(Thread.ofPlatform().daemon().start(() -> {
                Random r = new Random();
                try {
                    while (true) {
                        long[] due = q.take();
                        if (due[0] < 0) return;
                        // lixo de vida curta: ~48 objetos de 1-5 KB (~140 KB) + leitura do conjunto vivo
                        List<byte[]> tmp = new ArrayList<>();
                        long s = 0;
                        for (int i = 0; i < 48; i++) { byte[] b = new byte[1024 + r.nextInt(4096)]; b[0] = (byte) i; tmp.add(b); s += b.length; }
                        for (int i = 0; i < 20; i++) s += live.get(r.nextInt(n))[7];
                        // 1 em CHURN requisicoes trocam um pedaco do conjunto vivo (promocao/envelhecimento)
                        if (r.nextInt(CHURN) == 0) live.set(r.nextInt(n), new byte[CHUNK]);
                        sink = s;
                        long done = System.nanoTime();
                        if (due[0] >= warmEnd) lat[cnt.getAndIncrement()] = done - due[0];
                    }
                } catch (InterruptedException e) { /* fim */ }
            }));
        }
        // gerador de chegadas (ritmo fixo)
        long i = 0;
        while (true) {
            long due = start + i * 1_000_000_000L / rate;
            if (due >= end) break;
            long wait = due - System.nanoTime();
            if (wait > 0) LockSupport_parkNanos(wait);
            q.add(new long[]{due});
            i++;
        }
        while (!q.isEmpty()) Thread.sleep(10);
        Thread.sleep(200);
        for (int w = 0; w < workers; w++) q.add(new long[]{-1});

        int m = cnt.get();
        long[] v = Arrays.copyOf(lat, m);
        Arrays.sort(v);
        double secsMeasured = secs - warm;
        System.out.printf(Locale.ROOT, "req=%d (%.0f/s)  p50=%.2f ms  p99=%.2f  p99.9=%.2f  max=%.1f%n",
                m, m / secsMeasured, p(v, 50), p(v, 99), p(v, 99.9), v[m - 1] / 1e6);
        long gcs = 0, gct = 0;
        for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (b.getCollectionCount() > 0) { gcs += b.getCollectionCount(); gct += b.getCollectionTime(); }
        }
        MemoryUsage u = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long cpuMs = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getProcessCpuTime() / 1_000_000;
        System.out.printf(Locale.ROOT, "heap max=%d MB  coletas(total)=%d  tempo de GC (MXBean)=%d ms  CPU do processo=%d ms%n", u.getMax() >> 20, gcs, gct, cpuMs);
    }

    static double p(long[] s, double pct) { return s[Math.max(0, (int) Math.ceil(pct / 100 * s.length) - 1)] / 1e6; }
    static void LockSupport_parkNanos(long ns) { java.util.concurrent.locks.LockSupport.parkNanos(ns); }
}
