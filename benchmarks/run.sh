#!/usr/bin/env bash

# Runs the benchmarks for the charts and generates the charts, see README.md. JMH parameters can be given to replace the
# defaults, eg for a short test run: ./run.sh -f 1 -wi 1 -i 2 -w 200ms -r 200ms

set -e
cd "$(dirname "$0")"

args=${*:-"-f 4 -wi 5 -i 3 -w 2s -r 2s"}

mvn -q -f ../pom.xml -pl main,benchmarks -DskipTests package dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
jmh="java -cp target/classes:$(cat target/classpath.txt) org.openjdk.jmh.Main $args -rf json -rff"

set -x
mkdir -p charts/results
$jmh charts/results/fieldSerializer.json FieldSerializerBenchmark -p legacyChunks=false
$jmh charts/results/objectGraph.json ObjectGraphBenchmark -p scale=4,16 -p chunked=false,true
# The unsafe byte buffers need sun.nio.ch.DirectBuffer.
exports="--add-exports=java.base/sun.nio.ch=ALL-UNNAMED"
$jmh charts/results/string.json StringBenchmark -jvmArgsAppend $exports
$jmh charts/results/variableEncoding.json VariableEncodingBenchmark -jvmArgsAppend $exports
$jmh charts/results/array.json ArrayBenchmark -e Doubles -jvmArgsAppend $exports

python3 charts/charts.py
