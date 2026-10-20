#!/system/bin/sh
set -u
LAB=/data/user/0/com.deepseek.harness.compat/files/星球卑一号实验室
W="$LAB/.probe"
APP="$LAB/项目/12-PCL每日新闻/pclnews/app/src/main"
OUT="$LAB/项目/12-PCL每日新闻/build"
PKG=com.pclnews
TK="$W/tx"
AJ="$W/lib/x/plat/android-34/android.jar"
R8="$W/lib/r8.jar"
AS="$W/lib/apksig.jar"
KS="$W/qqbq.keystore"

export LD_LIBRARY_PATH="$TK/lib"
export PATH="$TK/bin:$PATH"
export JAVA_HOME="$TK"
export TMPDIR="$LAB/.probe/tmp"
mkdir -p "$TMPDIR" "$OUT"

JAVA="$TK/lib/jvm/java-21-openjdk/bin/java"
JAVAC="$TK/lib/jvm/java-21-openjdk/bin/javac"
KEYTOOL="$TK/lib/jvm/java-21-openjdk/bin/keytool"
AAPT2="$TK/bin/aapt2"

die(){ echo "FAILED: $1"; exit 1; }
for f in "$JAVA" "$JAVAC" "$AAPT2" "$AJ" "$R8" "$AS"; do [ -e "$f" ] || die "missing $f"; done

echo "== 1) aapt2 compile res =="
rm -rf "$OUT/res.zip" "$OUT/base.apk"
"$AAPT2" compile --dir "$APP/res" -o "$OUT/res.zip" 2>&1 | tail -5
[ -f "$OUT/res.zip" ] || die "aapt2 compile"

echo "== 2) aapt2 link (+assets) =="
"$AAPT2" link -o "$OUT/base.apk" -I "$AJ" \
  --manifest "$APP/AndroidManifest.xml" \
  -A "$APP/assets" \
  --min-sdk-version 26 --target-sdk-version 34 \
  --version-code 6 --version-name 3.3 \
  --java "$OUT/gen" \
  "$OUT/res.zip" 2>&1 | tail -8
[ -f "$OUT/base.apk" ] || die "aapt2 link"

echo "== 3) javac =="
rm -rf "$OUT/classes"; mkdir -p "$OUT/classes"
SRC=$(find "$APP/java" -name "*.java" | tr '\n' ' ')
"$JAVAC" -nowarn -source 11 -target 11 -encoding UTF-8 -classpath "$AJ" -d "$OUT/classes" $SRC 2>&1 | grep -v "^Note:" | head -30
[ -f "$OUT/classes/com/pclnews/MainActivity.class" ] || die "javac"

echo "== 4) d8 dex =="
rm -rf "$OUT/dex"; mkdir -p "$OUT/dex"
CLASSES=$(find "$OUT/classes" -name "*.class" | tr '\n' ' ')
"$JAVA" -Xmx1400m -cp "$R8" com.android.tools.r8.D8 --release --min-api 26 --lib "$AJ" \
  --output "$OUT/dex" $CLASSES 2>&1 | tail -12
[ -f "$OUT/dex/classes.dex" ] || die "d8"

echo "== 5) 打包 dex =="
cp "$OUT/base.apk" "$OUT/unsigned.apk"
cd "$OUT/dex" || die "cd dex"
zip -q -X "$OUT/unsigned.apk" classes.dex || die "zip dex"
cd - >/dev/null

echo "== 6) 签名 =="
if [ ! -f "$KS" ]; then
  "$KEYTOOL" -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias qqbq -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=PCLNews,OU=Local,O=Local,L=Local,ST=Local,C=CN" >/dev/null 2>&1 || die "keytool"
fi
if [ ! -f "$OUT/Sign.class" ]; then
  "$JAVAC" -nowarn -encoding UTF-8 -classpath "$AS" -d "$OUT" "$W/sign/Sign.java" 2>&1 | head -5
fi
[ -f "$OUT/Sign.class" ] || die "Sign.java 编译失败"
rm -f "$OUT/pclnews.apk"
"$JAVA" -cp "$AS:$OUT" Sign "$KS" android qqbq "$OUT/unsigned.apk" "$OUT/pclnews.apk" 2>&1 | tail -6
[ -s "$OUT/pclnews.apk" ] || die "sign"
ls -l "$OUT/pclnews.apk"
"$AAPT2" dump badging "$OUT/pclnews.apk" 2>&1 | head -4
echo "APK_DONE"
