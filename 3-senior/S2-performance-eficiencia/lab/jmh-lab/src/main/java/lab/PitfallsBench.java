package lab;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Armadilhas classicas: (1) resultado nao consumido (o JIT apaga o calculo), (2) entrada constante
 * (o JIT calcula na compilacao). Cada par mostra a medida "errada" ao lado da certa.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class PitfallsBench {
    static final double CONSTANT = 12345.678;
    double variable = 12345.678;

    // (1) eliminacao de codigo morto
    @Benchmark
    public void deadCode() {
        Math.log(variable);                 // resultado descartado: o JIT pode apagar a chamada
    }

    @Benchmark
    public double returned() {
        return Math.log(variable);          // o JMH consome o retorno
    }

    @Benchmark
    public void blackhole(Blackhole bh) {
        bh.consume(Math.log(variable));     // consumo explicito
    }

    // (2) dobramento de constantes
    @Benchmark
    public double logConstant() {
        return Math.log(CONSTANT);          // constante: o JIT PODE calcular na compilacao (depende da operacao)
    }

    @Benchmark
    public double sqrtConstant() {
        return Math.sqrt(CONSTANT);         // sqrt de constante: o JIT calcula na compilacao
    }

    @Benchmark
    public double sqrtFromState() {
        return Math.sqrt(variable);         // sqrt de entrada variavel: calculada de verdade
    }

    @Benchmark
    public void baseline() { }              // o custo do proprio arcabouco de medicao
}
