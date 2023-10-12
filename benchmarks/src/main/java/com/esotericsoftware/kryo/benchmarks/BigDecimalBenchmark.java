package com.esotericsoftware.kryo.benchmarks;

import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;

import static java.lang.Integer.parseInt;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.openjdk.jmh.runner.options.TimeValue.seconds;

public class BigDecimalBenchmark {

    @State(Scope.Thread)
    public static class MyState {
        final Serializer<BigDecimal> serializer = new DefaultSerializers.BigDecimalSerializer();

        Output output;
        Input input;

        @Param({
                "null", "zero", "one", "0",
                "2", "10", "max_in_long", "20", // twenty is more than the number of digits in Long.MAX_VALUE
                "-2", "-10", "min_in_long", "-20" // twenty is more than the number of digits in Long.MIN_VALUE
        })
        String numOfDigits = "5";
        int scale = 2;

        BigDecimal decimal;

        @Setup(Level.Iteration)
        public void setUp() {
            decimal = newDecimal(numOfDigits, scale);
            output = new Output(2, -1);
            serializer.write(null, output, decimal);
            input = new Input(output.toBytes());
            output.reset();
        }

        private static BigDecimal newDecimal(String numOfDigits, int scale) {
            switch (numOfDigits) {
                case "null": return null;
                case "zero": return ZERO;
                case "one": return ONE;
                case "0": return BigDecimal.valueOf(0, scale);
                case "max_in_long": return BigDecimal.valueOf(Long.MAX_VALUE, scale);
                case "min_in_long": return BigDecimal.valueOf(Long.MIN_VALUE, scale);
                default:
                    int digits = parseInt(numOfDigits.replace("-", ""));
                    BigDecimal d = BigDecimal.valueOf(10, 1 - digits).subtract(ONE).scaleByPowerOfTen(-scale); // '9' repeated numOfDigit times
                    return numOfDigits.charAt(0) != '-' ? d : d.negate();
            }
        }

        @TearDown(Level.Iteration)
        public void tearDown () {
            output.close();
            input.close();
        }
    }

    @Benchmark
    public byte[] write (MyState state) {
        state.output.reset();
        state.serializer.write(null, state.output, state.decimal);
        return state.output.getBuffer();
    }

    @Benchmark
    public BigDecimal read (MyState state) {
        state.input.reset();
        return state.serializer.read(null, state.input, BigDecimal.class);
    }

    public static void main (String[] args) throws RunnerException {
        final Options opt = new OptionsBuilder()
                .include(".*" + BigDecimalBenchmark.class.getSimpleName() + ".*")
                .timeUnit(MICROSECONDS)
                .warmupIterations(1)
                .warmupTime(seconds(1))
                .measurementIterations(4)
                .measurementTime(seconds(1))
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}
