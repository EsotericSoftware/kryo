#!/bin/sh
# Converts the Kryo jar to dex with D8, the dexer of the Android build tools, for the minimum Android API level that Kryo
# supports. Fails on any D8 warning, eg bytecode that needs a newer API level, which Animal Sniffer doesn't check. Needs the
# Android SDK (ANDROID_HOME, for android.jar) and the Kryo jar, built with: mvn -pl main package -DskipTests
set -e
cd "$(dirname "$0")"
bin=${JAVA_HOME:+$JAVA_HOME/bin/}
d8Version=9.5.23
minApi=26

sdk=${ANDROID_HOME:-$HOME/Library/Android/sdk}
androidJar=$(ls -d "$sdk"/platforms/android-*/android.jar | sort -t- -k2 -n | tail -1)
if [ ! -f "$androidJar" ]; then
	echo "No android.jar found in $sdk/platforms, set ANDROID_HOME."
	exit 1
fi

mkdir -p target/dex
d8=target/r8-$d8Version.jar
if [ ! -f $d8 ]; then
	curl -sSf -o $d8 https://dl.google.com/dl/android/maven2/com/android/tools/r8/$d8Version/r8-$d8Version.jar
fi

version=$(mvn -q -f ../main/pom.xml help:evaluate -Dexpression=project.version -DforceStdout)
output=$("${bin}java" -cp $d8 com.android.tools.r8.D8 --release --min-api $minApi --lib "$androidJar" --output target/dex \
	../target/kryo-$version.jar 2>&1)
if [ -n "$output" ]; then
	echo "$output"
	exit 1
fi
echo "D8 converted kryo-$version.jar for Android API level $minApi without warnings."
