package lab;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Os problemas da aula S2.02, agora com o instrumento certo: concatenacao e regex. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class TextBench {
    static final Pattern CODE = Pattern.compile("[A-Z]{3}-\\d{4}");

    @Param({"200", "1500"})
    int lines;

    String code = "ABC-4711";

    @Benchmark
    public String concatPlusEquals() {
        String out = "";
        for (int i = 0; i < lines; i++) out += "line " + i + ";";
        return out;
    }

    @Benchmark
    public String concatStringBuilder() {
        StringBuilder sb = new StringBuilder(lines * 12);
        for (int i = 0; i < lines; i++) sb.append("line ").append(i).append(';');
        return sb.toString();
    }

    @Benchmark
    public boolean regexMatchesString() {
        return code.matches("[A-Z]{3}-\\d{4}");
    }

    @Benchmark
    public boolean regexPrecompiled() {
        return CODE.matcher(code).matches();
    }
}
