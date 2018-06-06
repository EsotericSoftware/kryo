# Kryo JMH Benchmarks

This module contains JMH benchmarks for Kryo.

## Usage

To run benchmarks execute
```
mvn -f benchmarks/pom.xml compile exec:java
```

JMH options can be set via `-Dexec.args`, e.g.
```
mvn -f benchmarks/pom.xml compile exec:java -Dexec.args="-incl FieldSerializersBenchmark -f 2 -t 2 -wi 10 -i 10"
```
