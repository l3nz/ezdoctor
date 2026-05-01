#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/build/libs/ezdoctor-all.jar"

echo "==> Building uberjar..."
cd "$SCRIPT_DIR"
./gradlew fatJar

echo ""
echo "==> Cleaning examples..."
rm -f "$SCRIPT_DIR/examples/"*.pdf "$SCRIPT_DIR/examples/"*.html

echo ""
echo "==> Running examples..."
for adoc in "$SCRIPT_DIR/examples/"*.adoc; do
    name="$(basename "$adoc" .adoc)"
    out="$SCRIPT_DIR/examples/${name}.pdf"
    echo "  Converting: $adoc -> $out"
    java \
      --add-opens=java.base/java.lang=ALL-UNNAMED \
      --add-opens=java.base/java.io=ALL-UNNAMED \
      --add-opens=java.base/java.nio=ALL-UNNAMED \
      --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
      --add-opens=java.base/java.util=ALL-UNNAMED \
      --enable-native-access=ALL-UNNAMED \
      --sun-misc-unsafe-memory-access=allow \
      -Xmx512m \
      -jar "$JAR" pdf "$adoc" "$out"
done

echo ""
echo "Done. PDFs written to examples/"
