#!/usr/bin/env bash
set -euo pipefail

SDK=/home/adreno/android-sdk
BT=$SDK/build-tools/34.0.0
PLAT=$SDK/platforms/android-34/android.jar
PRJ=/home/adreno/trucker-money
JAVAC="/home/adreno/Загрузки/gigaide/GigaIDE/jbr/bin/javac"
KEYTOOL=/usr/lib/jvm/java-23-openjdk-amd64/bin/keytool
cd "$PRJ"

rm -rf build/obj build/dex build/stage build/app.unsigned.apk build/app.aligned.apk build/Таптар.apk
mkdir -p build/obj build/dex build/stage

echo "== javac =="
"$JAVAC" --release 8 -classpath "$PLAT" -d build/obj $(find src -name '*.java')
echo "ok"

echo "== d8 =="
/usr/lib/jvm/java-23-openjdk-amd64/bin/java -cp /tmp/opencode/r8.jar com.android.tools.r8.D8 \
    --release --min-api 21 --lib "$PLAT" --output build/dex $(find build/obj -name '*.class') 2>/dev/null
cp build/dex/classes.dex build/stage/classes.dex
echo "ok"

echo "== aapt package =="
"$BT/aapt" package -f -M AndroidManifest.xml -S res -A assets -I "$PLAT" -F build/app.unsigned.apk build/stage
echo "ok"

echo "== zipalign =="
"$BT/zipalign" -f 4 build/app.unsigned.apk build/app.aligned.apk

echo "== keystore =="
if [ ! -f key.jks ]; then
  "$KEYTOOL" -genkeypair -keystore key.jks -alias trucker -keyalg RSA -keysize 2048 \
      -validity 10000 -storepass trucker123 -keypass trucker123 \
      -dname "CN=IP Trucker, OU=Apps, O=Personal, L=RU, C=RU" 2>/dev/null
fi

echo "== apksigner =="
"$BT/apksigner" sign --ks key.jks --ks-key-alias trucker \
    --ks-pass pass:trucker123 --key-pass pass:trucker123 \
    --out build/Таптар.apk build/app.aligned.apk

"$BT/apksigner" verify build/Таптар.apk
ls -la build/Таптар.apk
echo "DONE"
