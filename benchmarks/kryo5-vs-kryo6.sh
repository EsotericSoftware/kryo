#!/usr/bin/env bash

# Runs the benchmarks for the Kryo 5 vs Kryo 6 charts, see README.md, and generates the charts: ObjectGraphBenchmark at scale 4
# with references and RecordSerializerBenchmark, with Kryo 6 (VarHandles, Unsafe and code generation) and with Kryo 5. The
# Kryo 5 results come from the same benchmarks compiled against Kryo 5.7.1, without the codeGeneration parameter that Kryo 5
# doesn't have, in a temporary directory. JMH parameters can be given to replace the defaults.

set -e
cd "$(dirname "$0")"

args=${*:-"-f 2 -wi 5 -i 5 -w 1s -r 1s"}
# Only for Unsafe field access and Kryo 5, which uses Unsafe: with this flag, the default field access of Kryo 6 is Unsafe too.
allow=--sun-misc-unsafe-memory-access=allow
graph="ObjectGraphBenchmark -p scale=4 -p chunked=false,true -e copy"
records="-p publicRecord=true"

mvn -q -f ../pom.xml -pl main,benchmarks -DskipTests package dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
jmh="java -cp target/classes:$(cat target/classpath.txt) org.openjdk.jmh.Main $args -rf json -rff"

# Kryo 5: the two benchmarks and their data, compiled against Kryo 5.7.1.
kryo5=target/kryo5
rm -rf $kryo5
mkdir -p $kryo5/src/main/java/com/esotericsoftware/kryo/benchmarks
cp -R src/main/java/com/esotericsoftware/kryo/benchmarks/data $kryo5/src/main/java/com/esotericsoftware/kryo/benchmarks
python3 - $kryo5/src/main/java/com/esotericsoftware/kryo/benchmarks <<'PY'
import re, sys
out = sys.argv[1]
src = "src/main/java/com/esotericsoftware/kryo/benchmarks/"
# Without the codeGeneration parameter, the check for its platform support and the copy benchmark, which Kryo 5 doesn't have.
graph = open(src + "ObjectGraphBenchmark.java").read()
graph = graph.replace("import com.esotericsoftware.kryo.bytecode.Bytecode;\n", "")
graph = re.sub(r"\t\t/\*\* If true, the field serializers use generated code.*?\n\t\t@Param\(\{\"false\"\}\) public boolean codeGeneration;\n\n", "", graph, flags=re.S)
graph = re.sub(r"\t\t\tif \(codeGeneration && .*?\n\t\t\t\tthrow new IllegalStateException\(.*?\n", "", graph)
graph = graph.replace("\t\t\tfactory.getConfig().setCodeGeneration(codeGeneration);\n", "")
graph = re.sub(r"\n\t@Benchmark\n\tpublic Object copy \(FieldSerializerState state\) \{\n.*?\n\t\}\n", "", graph, flags=re.S)
records = open(src + "RecordSerializerBenchmark.java").read()
records = records.replace("\t\t@Param({\"false\", \"true\"}) public boolean codeGeneration;\n\n", "")
records = records.replace("\t\t\tfactory.getConfig().setCodeGeneration(codeGeneration);\n", "")
for name, text in (("ObjectGraphBenchmark", graph), ("RecordSerializerBenchmark", records)):
    assert "codeGeneration" not in text and "Bytecode" not in text, name
    open(out + "/" + name + ".java", "w").write(text)
PY
cat > $kryo5/pom.xml <<'POM'
<project xmlns="http://maven.apache.org/POM/4.0.0">
	<modelVersion>4.0.0</modelVersion>
	<groupId>com.esotericsoftware</groupId>
	<artifactId>kryo5-benchmarks</artifactId>
	<version>1</version>
	<properties>
		<maven.compiler.release>17</maven.compiler.release>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
	</properties>
	<dependencies>
		<dependency><groupId>com.esotericsoftware</groupId><artifactId>kryo</artifactId><version>5.7.1</version></dependency>
		<dependency><groupId>org.openjdk.jmh</groupId><artifactId>jmh-core</artifactId><version>1.37</version></dependency>
	</dependencies>
	<build><plugins><plugin>
		<groupId>org.apache.maven.plugins</groupId>
		<artifactId>maven-compiler-plugin</artifactId>
		<version>3.14.0</version>
		<configuration><annotationProcessorPaths><path>
			<groupId>org.openjdk.jmh</groupId><artifactId>jmh-generator-annprocess</artifactId><version>1.37</version>
		</path></annotationProcessorPaths></configuration>
	</plugin></plugins></build>
</project>
POM
mvn -q -f $kryo5/pom.xml compile dependency:build-classpath -Dmdep.outputFile=classpath.txt
jmh5="java -cp $kryo5/target/classes:$(cat $kryo5/target/classpath.txt) org.openjdk.jmh.Main $args -rf json -rff"

set -x
mkdir -p charts/results
$jmh charts/results/kryo6VarHandle.json $graph -p codeGeneration=false
$jmh charts/results/kryo6Unsafe.json $graph -p codeGeneration=false -jvmArgsAppend "-Dkryo.fieldAccess=UNSAFE $allow"
$jmh charts/results/kryo6CodeGeneration.json $graph -p codeGeneration=true
$jmh5 charts/results/kryo5.json $graph -jvmArgsAppend "$allow"
$jmh charts/results/kryo6Records.json RecordSerializerBenchmark.field $records -p codeGeneration=false,true
$jmh5 charts/results/kryo5Records.json RecordSerializerBenchmark.record $records -jvmArgsAppend "$allow"

python3 charts/charts.py
