#!/usr/bin/env bash
# Builds the debug APK inside the container and copies it to ./out/
set -euo pipefail
cd /workspace

# Fixed signing key (see docker/create-signing-key.sh)
KEYS_DIR=/keys
if [ -f "$KEYS_DIR/keystore.properties" ]; then
  prop() { grep -E "^$1=" "$KEYS_DIR/keystore.properties" | head -n 1 | cut -d= -f2- | tr -d '\r'; }
  export RETAILPILOT_KEYSTORE="$KEYS_DIR/$(prop storeFile)"
  export RETAILPILOT_STORE_PASSWORD="$(prop storePassword)"
  export RETAILPILOT_KEY_ALIAS="$(prop keyAlias)"
  export RETAILPILOT_KEY_PASSWORD="$(prop keyPassword)"
  echo "Signing with your fixed key."
else
  echo "WARNING: no signing key in the keys folder - using a temporary debug key."
  echo "         This build may not install over an existing one without uninstalling (data loss)."
  echo "         Create a key once with: docker compose run --rm apk bash /workspace/docker/create-signing-key.sh"
fi

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
