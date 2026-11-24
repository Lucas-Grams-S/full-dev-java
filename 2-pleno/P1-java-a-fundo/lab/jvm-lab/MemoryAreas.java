import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/** P1.01 — áreas de memória e classloading. Uso: java [flags] MemoryAreas.java <loaders|init|stack|heap|metaspace|numbers> */
public class MemoryAreas {

    static class Lazy {
        static { System.out.println("  [Lazy] inicialização estática executada"); }
        static final int CONSTANT = 42;          // constante de compilação: não dispara a inicialização
        static int counter = 1;
    }

    static int depth = 0;
    static void recurse() { depth++; recurse(); }

    public static void main(String[] args) throws Exception {
        switch (args.length == 0 ? "loaders" : args[0]) {
            case "loaders" -> loaders();
            case "init" -> init();
            case "stack" -> stack();
            case "heap" -> heap();
            case "metaspace" -> metaspace();
            case "numbers" -> numbers();
            default -> System.out.println("experimento desconhecido");
        }
    }

    static void loaders() {
        System.out.println("String      -> " + String.class.getClassLoader());
        System.out.println("java.sql    -> " + java.sql.Driver.class.getClassLoader());
        System.out.println("esta classe -> " + MemoryAreas.class.getClassLoader());
        ClassLoader cl = MemoryAreas.class.getClassLoader();
        while (cl != null) { System.out.println("  cadeia: " + cl.getName() + " (" + cl.getClass().getName() + ")"); cl = cl.getParent(); }
        System.out.println("  cadeia: bootstrap (null)");
    }

    static void init() throws Exception {
        System.out.println("1) Class.forName(..., false, ...): carrega, não inicializa");
        Class<?> c = Class.forName("MemoryAreas$Lazy", false, MemoryAreas.class.getClassLoader());
        System.out.println("  carregada: " + c.getName());
        System.out.println("2) ler uma constante de compilação:");
        System.out.println("  CONSTANT = " + Lazy.CONSTANT);
        System.out.println("3) ler um campo estático comum:");
        System.out.println("  counter = " + Lazy.counter);
    }

    static void stack() {
        try { recurse(); } catch (StackOverflowError e) {
            System.out.println("StackOverflowError após " + depth + " chamadas");
        }
    }

    static void heap() {
        List<byte[]> keep = new ArrayList<>();
        try { while (true) keep.add(new byte[1024 * 1024]); }
        catch (OutOfMemoryError e) {
            int n = keep.size(); keep.clear();
            System.out.println("OutOfMemoryError: " + e.getMessage() + " (após " + n + " MiB retidos)");
        }
    }

    /** ClassLoader descartável: cada instância define uma classe nova (e a segura viva enquanto o loader viver). */
    static class OneShotLoader extends ClassLoader {
        Class<?> define(String name, byte[] bytes) { return defineClass(name, bytes, 0, bytes.length); }
    }

    static void metaspace() {
        // gera classes novas (Class-File API, java.lang.classfile) e as segura, cada uma no seu ClassLoader
        List<Object> keep = new ArrayList<>();
        String prefix = "Gen";
        String msgHead = String.valueOf("Parou depois de ");       // evita concatenação (invokedynamic) dentro do laço
        int n = 0;
        try {
            while (true) {
                String name = prefix + Integer.toString(n);
                byte[] bytes = ClassFile.of().build(ClassDesc.of(name), cb -> cb.withFlags(ClassFile.ACC_PUBLIC));
                OneShotLoader loader = new OneShotLoader();
                keep.add(loader.define(name, bytes));
                n++;
            }
        } catch (Throwable t) {
            int count = n;
            keep.clear();
            System.out.println(msgHead + count + " classes: " + t);
        }
    }

    static void numbers() {
        var mem = ManagementFactory.getMemoryMXBean();
        var h = mem.getHeapMemoryUsage(); var nh = mem.getNonHeapMemoryUsage();
        System.out.printf("heap      usado=%d MiB  comprometido=%d MiB  máximo=%d MiB%n", h.getUsed() >> 20, h.getCommitted() >> 20, h.getMax() >> 20);
        System.out.printf("non-heap  usado=%d MiB  comprometido=%d MiB%n", nh.getUsed() >> 20, nh.getCommitted() >> 20);
        for (var pool : ManagementFactory.getMemoryPoolMXBeans())
            System.out.printf("  %-30s %-8s usado=%d KiB%n", pool.getName(), pool.getType() == java.lang.management.MemoryType.HEAP ? "heap" : "non-heap", pool.getUsage().getUsed() >> 10);
        System.out.println("processadores=" + Runtime.getRuntime().availableProcessors());
    }
}
