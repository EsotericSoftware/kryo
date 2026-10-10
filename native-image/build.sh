#!/bin/sh
# Builds and runs NativeImageTest as a GraalVM native image. Needs GraalVM's native-image on the PATH or in JAVA_HOME, and the
# Kryo jar, built with: mvn -pl main package -DskipTests
#
# To regenerate the reachability metadata for the test classes after changing them, run with --agent.
set -e
cd "$(dirname "$0")"
bin=${JAVA_HOME:+$JAVA_HOME/bin/}

version=$(mvn -q -f ../main/pom.xml help:evaluate -Dexpression=project.version -DforceStdout)
kryo=../target/kryo-$version.jar
mvn -q -f ../main/pom.xml dependency:build-classpath -Dmdep.outputFile="$PWD/target/dependencies.txt" -Dmdep.includeScope=runtime
classpath="target/classes:$kryo:$(cat target/dependencies.txt)"

mkdir -p target/classes
"${bin}javac" --release 17 -d target/classes -cp "$classpath" src/com/esotericsoftware/kryo/nativeimage/*.java

if [ "$1" = "--agent" ]; then
	# The property makes Kryo take the same code paths as in a native image.
	"${bin}java" -Dorg.graalvm.nativeimage.imagecode=agent \
		-agentlib:native-image-agent=config-output-dir=target/agent -cp "$classpath" com.esotericsoftware.kryo.nativeimage.NativeImageTest
	echo "Copy the entries for com.esotericsoftware.kryo.nativeimage from target/agent to META-INF/native-image."
	exit
fi

cp -R META-INF target/classes/
"${bin}native-image" --no-fallback -cp "$classpath" com.esotericsoftware.kryo.nativeimage.NativeImageTest -o target/native-image-test
target/native-image-test
