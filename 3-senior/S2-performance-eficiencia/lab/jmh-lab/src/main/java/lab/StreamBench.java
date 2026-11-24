package lab;

import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** "Stream e mais lento que o laco?" Depende do tamanho, do tipo e de quem mede. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class StreamBench {
    @Param({"100", "100000"})
    int n;

    List<Integer> boxed;
    int[] prim;

    @Setup
    public void setup() {
        boxed = new ArrayList<>(n);
        prim = new int[n];
        for (int i = 0; i < n; i++) { boxed.add(i % 1000); prim[i] = i % 1000; }
    }

    @Benchmark
    public long loopBoxed() {
        long s = 0;
        for (int v : boxed) if (v % 2 == 0) s += v;
        return s;
    }

    @Benchmark
    public long streamBoxed() {
        return boxed.stream().filter(v -> v % 2 == 0).mapToLong(Integer::longValue).sum();
    }

    @Benchmark
    public long loopPrimitive() {
        long s = 0;
        for (int v : prim) if (v % 2 == 0) s += v;
        return s;
    }

    @Benchmark
    public long intStream() {
        return java.util.Arrays.stream(prim).filter(v -> v % 2 == 0).asLongStream().sum();
    }
}
