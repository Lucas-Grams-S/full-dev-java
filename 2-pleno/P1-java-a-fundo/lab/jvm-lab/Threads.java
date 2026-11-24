import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** P1.05 — threads e ExecutorService. Uso: java Threads.java <states|interrupt|errors|pool|reject|dump> */
public class Threads {

    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "states" : args[0]) {
            case "states" -> states();
            case "interrupt" -> interrupt();
            case "errors" -> errors();
            case "pool" -> pool();
            case "reject" -> reject();
            case "dump" -> dump();
            default -> System.out.println("?");
        }
    }

    static void states() throws Exception {
        Object lock = new Object();
        Thread t = new Thread(() -> {
            try { Thread.sleep(300); } catch (InterruptedException e) { return; }
            synchronized (lock) { /* só entra quando o main soltar */ }
            synchronized (lock) { try { lock.wait(); } catch (InterruptedException e) { /* sai pelo interrupt */ } }
        }, "worker-1");
        System.out.println("antes do start:        " + t.getState());
        t.start();
        Thread.sleep(100);
        System.out.println("dentro do sleep:       " + t.getState());
        synchronized (lock) {
            Thread.sleep(400);                      // o worker acorda do sleep e tenta o monitor que o main segura
            System.out.println("esperando o monitor:   " + t.getState());
        }
        Thread.sleep(100);
        System.out.println("dentro do wait():      " + t.getState());
        t.interrupt(); t.join();
        System.out.println("depois do join:        " + t.getState());
    }

    static void interrupt() throws Exception {
        Thread t = new Thread(() -> {
            try { Thread.sleep(10_000); }
            catch (InterruptedException e) {
                System.out.println("  worker: InterruptedException; flag agora = " + Thread.currentThread().isInterrupted());
                Thread.currentThread().interrupt();                  // restaura o sinal para quem estiver acima
                System.out.println("  worker: flag restaurada = " + Thread.currentThread().isInterrupted());
            }
        });
        t.start(); Thread.sleep(100);
        System.out.println("main: interrompendo");
        t.interrupt(); t.join();
        // laço de CPU não é interrompido sozinho: precisa consultar a flag
        Thread busy = new Thread(() -> { long n = 0; while (!Thread.currentThread().isInterrupted()) n++; System.out.println("  busy: parei após consultar a flag"); });
        busy.start(); Thread.sleep(100); busy.interrupt(); busy.join();
    }

    static void errors() throws Exception {
        System.out.println("1) exceção em Thread comum: vai para o UncaughtExceptionHandler");
        Thread t = new Thread(() -> { throw new IllegalStateException("falhou na thread"); }, "t-crash");
        t.setUncaughtExceptionHandler((th, ex) -> System.out.println("  handler(" + th.getName() + "): " + ex));
        t.start(); t.join();
        ExecutorService ex = Executors.newFixedThreadPool(2);
        System.out.println("2) submit(): a exceção fica guardada no Future");
        Future<?> f = ex.submit(() -> { throw new IllegalStateException("falhou no submit"); });
        Thread.sleep(100);
        System.out.println("  isDone=" + f.isDone() + " (ninguém viu o erro até agora)");
        try { f.get(); } catch (ExecutionException e) { System.out.println("  get(): " + e + "\n  causa: " + e.getCause()); }
        System.out.println("3) execute(): a exceção chega ao handler da thread do pool");
        ex.execute(() -> { throw new IllegalStateException("falhou no execute"); });
        Thread.sleep(200);
        ex.shutdown();
    }

    static void pool() throws Exception {
        AtomicInteger id = new AtomicInteger();
        ThreadFactory tf = r -> { Thread t = new Thread(r, "payflow-pool-" + id.incrementAndGet()); return t; };
        try (ExecutorService ex = Executors.newFixedThreadPool(3, tf)) {          // close() espera as tarefas terminarem (Java 19+)
            List<Callable<String>> tasks = new java.util.ArrayList<>();
            for (int i = 1; i <= 6; i++) { int n = i; tasks.add(() -> { Thread.sleep(100); return "tarefa " + n + " em " + Thread.currentThread().getName(); }); }
            long t0 = System.nanoTime();
            for (Future<String> f : ex.invokeAll(tasks)) System.out.println("  " + f.get());
            System.out.printf("6 tarefas de 100 ms em 3 threads: %.0f ms%n", (System.nanoTime() - t0) / 1e6);
        }
        System.out.println("pool encerrado");
    }

    static void reject() throws Exception {
        // 1 thread + fila de 2 posições: a 4ª tarefa simultânea não cabe
        var pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2));      // política padrão: AbortPolicy
        CountDownLatch release = new CountDownLatch(1);
        for (int i = 1; i <= 5; i++) {
            int n = i;
            try { pool.execute(() -> { try { release.await(); } catch (InterruptedException e) { } }); System.out.println("tarefa " + n + ": aceita"); }
            catch (RejectedExecutionException e) { System.out.println("tarefa " + n + ": RECUSADA -> " + e.getClass().getSimpleName()); }
        }
        release.countDown(); pool.shutdown(); pool.awaitTermination(2, TimeUnit.SECONDS);
        // CallerRunsPolicy: quem submeteu executa a tarefa (freio natural)
        var pool2 = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.CallerRunsPolicy());
        CountDownLatch gate = new CountDownLatch(1);
        for (int i = 1; i <= 4; i++) {
            int n = i;
            pool2.execute(() -> { System.out.println("tarefa " + n + " rodou em " + Thread.currentThread().getName()); try { gate.await(300, TimeUnit.MILLISECONDS); } catch (InterruptedException e) { } });
        }
        gate.countDown(); pool2.shutdown(); pool2.awaitTermination(2, TimeUnit.SECONDS);
    }

    static void dump() throws Exception {
        Object a = new Object();
        Thread holder = new Thread(() -> { synchronized (a) { try { Thread.sleep(60_000); } catch (InterruptedException e) { } } }, "holder");
        Thread waiter = new Thread(() -> { synchronized (a) { System.out.println("nunca"); } }, "waiter");
        holder.setDaemon(true); waiter.setDaemon(true);
        holder.start(); Thread.sleep(100); waiter.start();
        System.out.println("PID=" + ProcessHandle.current().pid() + " — rode: jcmd <PID> Thread.print");
        Thread.sleep(Long.parseLong(System.getProperty("wait", "1500")));
    }
}
