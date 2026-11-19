#!/usr/bin/env bash
# Builds the debug APK inside the container and copies it to ./out/
set -euo pipefail
cd /workspace

if [ -f local.properties ] && grep -q '^sdk.dir' local.properties; then
  echo "Note: ignoring sdk.dir in local.properties (using the container SDK)."
  cp local.properties /tmp/local.properties.bak
  grep -v '^sdk.dir' /tmp/local.properties.bak > local.properties || true
fi

# Works even if Windows checked gradlew out with CRLF line endings
tr -d '\r' < gradlew > .gradlew-lf
trap 'rm -f /workspace/.gradlew-lf; [ -f /tmp/local.properties.bak ] && cp /tmp/local.properties.bak /workspace/local.properties || true' EXIT

bash .gradlew-lf --no-daemon assembleDebug "$@"

mkdir -p out
cp app/build/outputs/apk/debug/app-debug.apk out/RetailPilot-debug.apk
echo
echo "Done: out/RetailPilot-debug.apk  (drag it onto the emulator window to install)"
