#!/bin/sh
# Converts the Kryo jar and AndroidTest to dex with D8, the dexer of the Android build tools, for the minimum Android API level
# that Kryo supports, and runs AndroidTest on the connected device or emulator, with data written by JvmData on this JVM. Fails on any D8 warning, eg bytecode that needs
# a newer API level, which Animal Sniffer doesn't check. Needs the Android SDK (ANDROID_HOME) and the Kryo jar, built with:
# mvn -pl main package -DskipTests
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

rm -rf target/classes target/dex
mkdir -p target/classes target/dex
d8=target/r8-$d8Version.jar
if [ ! -f $d8 ]; then
	curl -sSf -o $d8 https://dl.google.com/dl/android/maven2/com/android/tools/r8/$d8Version/r8-$d8Version.jar
fi

version=$(mvn -q -f ../main/pom.xml help:evaluate -Dexpression=project.version -DforceStdout)
kryo=../target/kryo-$version.jar
"${bin}javac" --release 17 -nowarn -d target/classes -cp "$kryo:$androidJar" src/com/esotericsoftware/kryo/android/*.java
# The output is checked for warnings and printed on errors, so the assignment must not exit with set -e.
if ! output=$("${bin}java" -cp $d8 com.android.tools.r8.D8 --min-api $minApi --lib "$androidJar" --output target/dex $kryo \
	$(find target/classes -name '*.class') 2>&1) || [ -n "$output" ]; then
	echo "$output"
	exit 1
fi
echo "D8 converted kryo-$version.jar for Android API level $minApi without warnings."
if [ "$1" = "--dex-only" ]; then exit; fi

"${bin}java" -cp "target/classes:$kryo" com.esotericsoftware.kryo.android.JvmData target/jvm-data.bin
adb=$(command -v adb || echo "$sdk/platform-tools/adb")
"$adb" push target/dex/classes.dex /data/local/tmp/kryo-android-test.dex > /dev/null
"$adb" push target/jvm-data.bin /data/local/tmp/kryo-jvm-data.bin > /dev/null
# app_process runs a main class with the Android framework, which dalvikvm can't.
"$adb" shell "CLASSPATH=/data/local/tmp/kryo-android-test.dex app_process /system/bin com.esotericsoftware.kryo.android.AndroidTest \
	/data/local/tmp/kryo-jvm-data.bin"
