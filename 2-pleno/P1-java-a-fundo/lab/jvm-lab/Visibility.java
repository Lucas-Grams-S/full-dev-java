import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** P1.08 — Java Memory Model. Uso: java [-Xint] Visibility.java <hang|hang-fix|hang-println|litmus|litmus-volatile> */
public class Visibility {

    static boolean stopPlain;                 // sem volatile
    static volatile boolean stopVolatile;

    static int x, y, r1, r2;                  // litmus plano
    static volatile int vx, vy;               // litmus com volatile

    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "hang" : args[0]) {
            case "hang" -> hang(false, false);
            case "hang-fix" -> hang(true, false);
            case "hang-println" -> hang(false, true);
            case "litmus" -> litmus(false);
            case "litmus-volatile" -> litmus(true);
            default -> System.out.println("?");
        }
    }

    /** O trabalhador gira até ver o flag; o main liga o flag depois de 300 ms. */
    static void hang(boolean useVolatile, boolean printInLoop) throws Exception {
        long[] spins = {0};
        Thread worker = Thread.ofPlatform().daemon().start(() -> {
            if (useVolatile) { while (!stopVolatile) spins[0]++; }
            else if (printInLoop) { while (!stopPlain) { spins[0]++; if (spins[0] % 50_000_000 == 0) System.out.println("  (girando...)"); } }
            else { while (!stopPlain) spins[0]++; }
        });
        Thread.sleep(300);
        if (useVolatile) stopVolatile = true; else stopPlain = true;
        System.out.println("main: flag ligado");
        worker.join(2000);
        System.out.println("trabalhador ainda vivo após 2 s? " + worker.isAlive());
    }

    static volatile int gen;                  // "largada": o main incrementa, as duas threads giram esperando
    static final AtomicInteger done = new AtomicInteger();   // "chegada": cada thread incrementa ao terminar (atômico: dois escritores)

    /** Teste de "litmus" (buffer de escrita): (r1, r2) == (0, 0) é impossível numa execução "em ordem". */
    static void litmus(boolean vol) throws Exception {
        int rounds = 300_000, bad = 0;
        Thread t1 = Thread.ofPlatform().daemon().start(() -> {
            for (int g = 1; g <= rounds; g++) { while (gen < g) { } if (vol) { vx = 1; r1 = vy; } else { x = 1; r1 = y; } done.incrementAndGet(); }
        });
        Thread t2 = Thread.ofPlatform().daemon().start(() -> {
            for (int g = 1; g <= rounds; g++) { while (gen < g) { } if (vol) { vy = 1; r2 = vx; } else { y = 1; r2 = x; } done.incrementAndGet(); }
        });
        for (int g = 1; g <= rounds; g++) {
            x = y = vx = vy = 0; r1 = r2 = -1; done.set(0);     // escritas volatile (done, gen) publicam o reset para as threads
            gen = g;
            while (done.get() < 2) { }
            if (r1 == 0 && r2 == 0) bad++;
        }
        System.out.printf("%s: %,d rodadas, (r1,r2) = (0,0) em %d delas%n", vol ? "com volatile" : "campos comuns", rounds, bad);
    }
}
