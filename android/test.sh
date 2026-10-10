#!/bin/sh
# Converts the Kryo jar and the tests in src to dex with D8, the dexer of the Android build tools, for the minimum Android API
# level that Kryo supports, and runs AndroidTest on the connected device or emulator: with data written by JvmData on this JVM,
# and SerializationCompatTest between the JVM and Android, see AndroidSerializationCompat. Fails on any D8 warning for Kryo, eg
# bytecode that needs a newer API level, which Animal Sniffer doesn't check. Needs the Android SDK (ANDROID_HOME) and the Kryo
# jar, built with: mvn -pl main package -DskipTests
#
# With --dex-only, only converts to dex, without a device.
set -e
cd "$(dirname "$0")"
bin=${JAVA_HOME:+$JAVA_HOME/bin/}
d8Version=9.5.23
minApi=26

sdk=${ANDROID_HOME:-$HOME/Library/Android/sdk}
# The newest platform, sorted by its number so the SDK path doesn't matter.
platform=$(ls "$sdk"/platforms | grep -E '^android-[0-9]+$' | sort -t- -k2 -n | tail -1)
androidJar=$sdk/platforms/$platform/android.jar
if [ ! -f "$androidJar" ]; then
	echo "No android.jar found in $sdk/platforms, set ANDROID_HOME."
	exit 1
fi

rm -rf target/classes target/dex target/compat
mkdir -p target/classes target/dex/kryo target/dex/test target/compat
d8=target/r8-$d8Version.jar
if [ ! -f $d8 ]; then
	curl -sSf -o $d8 https://dl.google.com/dl/android/maven2/com/android/tools/r8/$d8Version/r8-$d8Version.jar
fi

version=$(mvn -q -f ../main/pom.xml help:evaluate -Dexpression=project.version -DforceStdout)
kryo=../target/kryo-$version.jar
# The test data and comparison of SerializationCompatTest, and the libraries they and StdInstantiatorStrategy need on Android.
mvn -q -f ../main/pom.xml dependency:build-classpath -Dmdep.includeScope=test -Dmdep.outputFile="$PWD/target/dependencies.txt"
libraries=$(tr ':' '\n' < target/dependencies.txt \
	| grep -E '/(objenesis|commons-lang3|junit-jupiter-api|junit-platform-commons|opentest4j|apiguardian-api)-[0-9]' | paste -sd: -)
compatTest="../test/com/esotericsoftware/kryo"
"${bin}javac" --release 17 -nowarn -d target/classes -cp "$kryo:$androidJar:$libraries" $(find src -name '*.java') \
	$compatTest/SerializationCompatTestData.java $compatTest/TestDataJava11.java $compatTest/TestDataJava17.java \
	$compatTest/ReflectionAssert.java
# Kryo is checked for D8 warnings, the output is printed on errors, so the assignment must not exit with set -e.
if ! output=$("${bin}java" -cp $d8 com.android.tools.r8.D8 --min-api $minApi --lib "$androidJar" --output target/dex/kryo $kryo \
	2>&1) || [ -n "$output" ]; then
	echo "$output"
	exit 1
fi
echo "D8 converted kryo-$version.jar for Android API level $minApi without warnings."
# The tests and libraries are not checked for D8 warnings.
if ! output=$("${bin}java" -cp $d8 com.android.tools.r8.D8 --min-api $minApi --lib "$androidJar" --lib $kryo --output target/dex/test \
	$(find target/classes -name '*.class') $(echo "$libraries" | tr ':' ' ') 2>&1); then
	echo "$output"
	exit 1
fi
if [ "$1" = "--dex-only" ]; then exit; fi

"${bin}java" -cp "target/classes:$kryo" com.esotericsoftware.kryo.android.JvmData target/jvm-data.bin
"${bin}java" -cp "target/classes:$kryo:$(cat target/dependencies.txt)" com.esotericsoftware.kryo.AndroidSerializationCompat write \
	target/compat/jvm
adb=$(command -v adb || echo "$sdk/platform-tools/adb")
device=/data/local/tmp/kryo-android-test
"$adb" shell "rm -rf /data/local/tmp/kryo-android-test && mkdir -p /data/local/tmp/kryo-android-test"
"$adb" push target/dex/kryo/classes.dex $device/kryo.dex > /dev/null
"$adb" push target/dex/test/classes.dex $device/test.dex > /dev/null
"$adb" push target/jvm-data.bin $device/ > /dev/null
"$adb" push target/compat/jvm $device/ > /dev/null
# app_process runs a main class with the Android framework, which dalvikvm can't.
status=0
"$adb" shell "CLASSPATH=$device/kryo.dex:$device/test.dex app_process /system/bin com.esotericsoftware.kryo.android.AndroidTest \
	$device/jvm-data.bin $device/jvm $device/android" || status=1

# The files written on Android, read on the JVM.
"$adb" pull $device/android target/compat > /dev/null
"${bin}java" -cp "target/classes:$kryo:$(cat target/dependencies.txt)" com.esotericsoftware.kryo.AndroidSerializationCompat read \
	target/compat/android "$("$adb" shell getprop ro.build.version.sdk | tr -d '\r')" || status=1
exit $status
