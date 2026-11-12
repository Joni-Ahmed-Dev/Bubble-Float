#!/bin/bash
# CI build for Bubble Float: same zero-dependency aapt2/d8 build as build.sh,
# but using the Android SDK installed by setup-android.
set -euo pipefail
cd "$(dirname "$0")/.."

: "${ANDROID_HOME:?ANDROID_HOME is required}"
SDK="$ANDROID_HOME"
BT="$SDK/build-tools/34.0.0"
AJAR="$SDK/platforms/android-34/android.jar"

if [ ! -f "$AJAR" ]; then
  yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
  "$SDK/cmdline-tools/latest/bin/sdkmanager" "platforms;android-34" "build-tools;34.0.0"
fi

rm -rf build
mkdir -p build/gen build/classes build/dex

"$BT/aapt2" compile --dir res -o build/compiled_res.zip
"$BT/aapt2" link -o build/app-unsigned.apk \
    --manifest AndroidManifest.xml \
    -I "$AJAR" \
    --java build/gen \
    --min-sdk-version 26 --target-sdk-version 34 \
    --version-code 1 --version-name 1.0 \
    build/compiled_res.zip

find src build/gen -name "*.java" > build/sources.txt
javac -encoding UTF-8 -source 8 -target 8 -d build/classes -cp "$AJAR" @build/sources.txt 2>&1 | grep -v "bootstrap class path" | tee build/javac.log
if grep -q "error:" build/javac.log; then echo "JAVAC FAILED"; exit 1; fi

"$BT/d8" --lib "$AJAR" --output build/dex $(find build/classes -name "*.class" | tr '\n' ' ')

cp build/app-unsigned.apk build/app-aligned.apk
(cd build/dex && zip -q ../app-aligned.apk classes.dex)
"$BT/zipalign" -f 4 build/app-aligned.apk build/app.apk

KS="$PWD/ci-debug.keystore"
if [ ! -f "$KS" ]; then
    keytool -genkeypair -keystore "$KS" -alias bubblefloat -keyalg RSA -keysize 2048 \
        -validity 10950 -storepass android -keypass android \
        -dname "CN=Bubble Float, OU=Muse, O=Local, C=BD" 2>/dev/null
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --out bubble-float.apk build/app.apk
"$BT/apksigner" verify --print-certs bubble-float.apk > /dev/null && echo "BUILD OK: $(pwd)/bubble-float.apk" && ls -lh bubble-float.apk
