import java.util.concurrent.CountDownLatch;

/** P1.10 — o "pinning" do synchronized. Rode com UM carrier: java -Djdk.virtualThreadScheduler.parallelism=1 -Djdk.virtualThreadScheduler.maxPoolSize=1 Pinning.java */
public class Pinning {
    public static void main(String[] args) throws Exception {
        Object lock = new Object();
        long t0 = System.nanoTime();
        Thread a = Thread.ofVirtual().unstarted(() -> { synchronized (lock) { try { Thread.sleep(500); } catch (InterruptedException e) { } } });
        CountDownLatch bRan = new CountDownLatch(1); long[] bAt = {0};
        Thread b = Thread.ofVirtual().unstarted(() -> { bAt[0] = System.nanoTime(); bRan.countDown(); });
        a.start(); Thread.sleep(100); b.start();
        bRan.await(); a.join();
        double ms = (bAt[0] - t0) / 1e6;
        System.out.printf("Java %s: B começou %.0f ms depois do início (A dorme 500 ms dentro de um synchronized): %s%n",
                System.getProperty("java.specification.version"), ms, ms < 400 ? "B NÃO ficou esperando" : "B FICOU esperando (carrier fixado)");
    }
}
