#!/bin/sh
# Builds the Traccar server (from this fork) and the web app (traccar-web submodule)
# inside Docker containers, then stages everything into deploy/out with the same
# layout as the official release package. Output is platform-independent.
set -e

ROOT=$(cd "$(dirname "$0")/.." && pwd)
OUT="$ROOT/deploy/out"

if [ ! -f "$ROOT/traccar-web/package.json" ]; then
    echo "traccar-web submodule is empty. Run: git submodule update --init traccar-web" >&2
    exit 1
fi

echo "==> Building server"
docker run --rm \
    -v "$ROOT":/src -v traccar-gradle-cache:/root/.gradle -w /src \
    eclipse-temurin:25-jdk ./gradlew --no-daemon assemble

echo "==> Building web app"
docker run --rm \
    -v "$ROOT/traccar-web":/src -v /src/node_modules -v traccar-npm-cache:/root/.npm -w /src \
    node:22 sh -c "npm ci --no-audit --no-fund && npm run build"

echo "==> Staging package in $OUT"
rm -rf "$OUT"
mkdir -p "$OUT/conf" "$OUT/lib" "$OUT/schema" "$OUT/templates/translations" "$OUT/web"
cp "$ROOT/target/tracker-server.jar" "$OUT/"
cp "$ROOT"/target/lib/* "$OUT/lib/"
cp "$ROOT"/schema/* "$OUT/schema/"
cp -r "$ROOT"/templates/* "$OUT/templates/"
cp "$ROOT/setup/traccar.xml" "$OUT/conf/"
cp -r "$ROOT"/traccar-web/build/* "$OUT/web/"
cp -r "$ROOT"/traccar-web/src/resources/l10n/* "$OUT/templates/translations/"

echo "==> Done: $(du -sh "$OUT" | cut -f1)"
