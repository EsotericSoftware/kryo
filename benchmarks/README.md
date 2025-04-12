# Kryo Benchmarks

This subproject contains [JMH](http://openjdk.java.net/projects/code-tools/jmh/) benchmarks for Kryo. The [R/ggplot2 files](https://github.com/EsotericSoftware/kryo/tree/master/benchmarks/charts) are used to generate charts from the benchmark results.

## Usage

### With Maven

To run benchmarks execute:
```
mvn -f benchmarks/pom.xml compile exec:java -Dexec.args="[parameters]"
```

Where `[parameters]` should be replaced with JMH parameters.

### Without Maven

This assumes your IDE has compiled Kryo to the `bin` directory, including the benchmarks source and processing of the JMH annotations.
```
java -cp "bin;lib/*;benchmarks/lib/*" com.esotericsoftware.kryo.benchmarks.KryoBenchmarks [parameters]
```
Where `[parameters]` should be replaced with JMH parameters.

### Parameters

If no JMH parameters are given, the benchmarks are run with settings only suitable for development (fork 0, short runs). A full list of JMH parameters can be found by running:
```
java -cp "benchmarks/lib/*" org.openjdk.jmh.Main -h
```
Or by digging through the [JMH source](http://hg.openjdk.java.net/code-tools/jmh/file/3769055ad883/jmh-core/src/main/java/org/openjdk/jmh/runner/options/CommandLineOptions.java).

Running without parameters is equivalent to:
```
-f 0 -wi 1 -i 1 -t 1 -w 1s -r 1s
```

The standard parameters to obtain reasonable results are:
```
-f 4 -wi 5 -i 3 -t 2 -w 2s -r 2s
```

To run only specific benchmarks, specify the benchmark class name(s):
```
-f 4 -wi 5 -i 3 -t 2 -w 2s -r 2s FieldSerializerBenchmark
```

To run only a subset of a benchmark, specify the benchmark class name and the methods:
```
-f 4 -wi 5 -i 3 -t 2 -w 2s -r 2s FieldSerializerBenchmark.field FieldSerializerBenchmark.tagged
```
