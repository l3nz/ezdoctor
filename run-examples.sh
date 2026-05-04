#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# This is to be expanded for the right version
JAR="$SCRIPT_DIR/build/libs/ezdoctor-*-all.jar"
OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Xmx512m -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Djruby.compile.mode=OFF"

echo "==> Building uberjar..."
cd "$SCRIPT_DIR"
./gradlew fatJar

echo ""
echo "==> Cleaning examples..."
rm -f "$SCRIPT_DIR/examples/*.pdf" \
  "$SCRIPT_DIR/examples/*.html" \
  "$SCRIPT_DIR/examples/*.svg" \
  "$SCRIPT_DIR/examples/*.png" \
  "$SCRIPT_DIR/examples/*.epub"

echo ""
echo "==> Running examples..."
for adoc in "$SCRIPT_DIR/examples/"*.adoc; do
    name="$(basename "$adoc" .adoc)"
    echo -n "  $name: pdf... "
    java $OPTS -jar $JAR pdf "$adoc" "$SCRIPT_DIR/examples/${name}.pdf"
    echo -n "  $name: html... "
    java $OPTS -jar $JAR html "$adoc" "$SCRIPT_DIR/examples/${name}.html"
    echo -n "  $name: epub... "
    java $OPTS -jar $JAR epub "$adoc" "$SCRIPT_DIR/examples/${name}.epub"
done

echo "==> Computing revision..."
for adoc in "$SCRIPT_DIR/examples/plain-with-revision.adoc"; do
    name="$(basename "$adoc" .adoc)"
    echo -n "  $name: pdf... "
    java $OPTS -jar $JAR pdf --rev "$adoc" 
    echo -n "  $name: html... "
    java $OPTS -jar $JAR html --rev "$adoc" 
    echo -n "  $name: epub... "
    java $OPTS -jar $JAR epub --rev "$adoc" 
done

echo "==> adding themes..."
for adoc in "$SCRIPT_DIR/examples/plain.adoc"; do
    name="$(basename "$adoc" .adoc)"
    echo -n "  $name: pdf... "
    java $OPTS -jar $JAR pdf --theme ./themes/contract.yml "$adoc"  $SCRIPT_DIR/examples/plain-with-theme.pdf
    echo -n "  $name: html... "
    java $OPTS -jar $JAR html --theme ./themes/all-red-theme.css "$adoc" $SCRIPT_DIR/examples/plain-with-theme-red.html
done


echo ""
echo "Done. Output written to examples/"
