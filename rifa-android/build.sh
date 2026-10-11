#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:-/workspace/android-sdk}"
BT="$SDK/build-tools/35.0.0"
mkdir -p build/generated build/classes build/dex /workspace/artifacts
"$BT/aapt" package -f -m -J build/generated -M AndroidManifest.xml -S res -I "$SDK/platforms/android-35/android.jar" -F build/unsigned.apk
java com.sun.tools.javac.Main -encoding UTF-8 -source 8 -target 8 -bootclasspath "$SDK/platforms/android-35/android.jar:$BT/core-lambda-stubs.jar" -d build/classes src/br/com/minhasrifas/*.java build/generated/br/com/minhasrifas/R.java
"$BT/d8" --lib "$SDK/platforms/android-35/android.jar" --min-api 26 --output build/dex build/classes/br/com/minhasrifas/*.class
cp build/unsigned.apk build/with-dex.apk
zip -j build/with-dex.apk build/dex/classes.dex
"$BT/zipalign" -f 4 build/with-dex.apk build/aligned.apk
if [ ! -f build/local-signing.jks ]; then
  keytool -genkeypair -keystore build/local-signing.jks -storepass android -keypass android -alias local -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Minhas Rifas Local' -noprompt
fi
"$BT/apksigner" sign --ks build/local-signing.jks --ks-pass pass:android --key-pass pass:android --out /workspace/artifacts/Minhas-Rifas.apk build/aligned.apk
"$BT/apksigner" verify --verbose /workspace/artifacts/Minhas-Rifas.apk
