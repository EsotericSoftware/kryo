#!/usr/bin/env bash

args="-f 4 -wi 5 -i 3 -t 2 -w 2s -r 2s -rf csv -rff"
jmh="$JAVA_HOME/bin/java -cp ../eclipse/bin;../eclipse/.apt_generated;../lib/*;lib/* com.esotericsoftware.kryo.benchmarks.KryoBenchmarks $args"

set -ex

mkdir -p charts/results
$jmh charts/results/fieldSerializer.csv FieldSerializerBenchmark
$jmh charts/results/array.csv ArrayBenchmark
$jmh charts/results/string.csv StringBenchmark
$jmh charts/results/variableEncoding.csv VariableEncodingBenchmark

cd charts/results
find ../*.r -type f -exec echo "{}:" \; -exec Rscript  {} \;
