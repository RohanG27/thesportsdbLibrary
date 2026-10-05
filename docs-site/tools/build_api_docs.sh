#!/usr/bin/env bash
# Generates each client library's API reference into docs-site/site/api/<library>/:
#   kotlin      Dokka (Gradle)            -> api/kotlin/index.html
#   python      pdoc                      -> api/python/thesportsdb_client.html
#   php         phpDocumentor (phar)      -> api/php/index.html
#   javascript  TypeDoc                   -> api/javascript/index.html
#
# Run after tools/build_site.py (which clears site/). Needs JDK 17, Python with the library
# installed, PHP and Node. CI uses it in .github/workflows/pages.yml.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/docs-site/site/api"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
REPO_URL="https://github.com/RohanG27/thesportsdbLibrary"
PY="${PYTHON:-python3}"
rm -rf "$OUT" && mkdir -p "$OUT"

echo "Kotlin (Dokka)"
(cd "$ROOT/kotlin" && ./gradlew dokkaGenerate -q)
cp -r "$ROOT/kotlin/build/dokka/html" "$OUT/kotlin"

echo "Python (pdoc)"
(cd "$ROOT/python" && "$PY" -m pdoc thesportsdb_client -o "$OUT/python" --docformat restructuredtext)

echo "PHP (phpDocumentor)"
curl -sSL -o "$TMP/phpDocumentor.phar" https://github.com/phpDocumentor/phpDocumentor/releases/download/v3.10.0/phpDocumentor.phar
(cd "$ROOT/php" && php "$TMP/phpDocumentor.phar" run -d src -t "$OUT/php" --title "thesportsdb-client" \
    --defaultpackagename "thesportsdb-client" --cache-folder "$TMP/phpdoc-cache" --no-interaction -q)

echo "JavaScript (TypeDoc; TypeDoc supports TypeScript up to 6, the library builds with 7)"
(cd "$ROOT/javascript" && npx -y -p typedoc@0.28 -p typescript@6 typedoc --entryPoints src/index.ts --tsconfig tsconfig.json \
    --out "$OUT/javascript" --name "thesportsdb-client" --readme none --excludeInternal --skipErrorChecking \
    --disableGit --gitRevision main --basePath .. \
    --sourceLinkTemplate "$REPO_URL/blob/{gitRevision}/javascript/src/{path}#L{line}" --logLevel Warn)

# Nothing local may leak into the published pages (absolute paths, this machine's folders).
if grep -rlE "/home/|/Users/|/runner/work/" "$OUT" --include=*.html | xargs -r grep -lE '(/home/[a-z]|/Users/|/runner/work/)' | grep -v 'event-stat/home' | head -1 | grep -q .; then
  echo "error: local paths found in the generated API docs" >&2
  grep -rlE '(/home/[a-z]|/Users/|/runner/work/)' "$OUT" --include=*.html | head >&2
  exit 1
fi
echo "API reference written to $OUT"
