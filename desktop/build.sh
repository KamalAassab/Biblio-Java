#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  BiblioTech desktop — compile and run (macOS / Linux)
#  Usage: ./desktop/build.sh [run]
# ─────────────────────────────────────────────────────────────
set -euo pipefail

cd "$(dirname "$0")"

JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
CP="lib/postgresql-42.7.4.jar:lib/flatlaf-3.5.4.jar"

mkdir -p out

# Interfaces/ holds the original coursework stubs, superseded by the concrete
# classes in src/ and excluded from the build.
mapfile -t SOURCES < <(find src -name '*.java' -not -path '*/Interfaces/*')

echo "Compiling ${#SOURCES[@]} files..."
"$JAVAC" -encoding UTF-8 -nowarn -cp "$CP" -d out "${SOURCES[@]}"
echo "Build OK  ->  desktop/out"

if [[ "${1:-}" == "run" ]]; then
  exec "$JAVA" -Dfile.encoding=UTF-8 -cp "out:$CP" GUI_Main
fi
