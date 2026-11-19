#!/usr/bin/env bash
# Creates the fixed signing key used by every build (run once).
# Usage: docker compose run --rm apk bash /workspace/docker/create-signing-key.sh
set -euo pipefail
KEYS_DIR=/keys

if [ -f "$KEYS_DIR/keystore.properties" ]; then
  echo "A signing key already exists in the keys folder. Nothing was changed."
  exit 1
fi

mkdir -p "$KEYS_DIR"
PASS="$(head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n')"

keytool -genkeypair \
  -keystore "$KEYS_DIR/retailpilot.jks" -storetype PKCS12 \
  -alias retailpilot -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" \
  -dname "CN=RetailPilot, O=RetailPilot" > /dev/null

cat > "$KEYS_DIR/keystore.properties" <<PROPS
storeFile=retailpilot.jks
storePassword=$PASS
keyAlias=retailpilot
keyPassword=$PASS
PROPS

echo "Signing key created in the keys folder (retailpilot.jks + keystore.properties)."
echo "BACK UP THIS FOLDER. Without it, future builds cannot update the installed app."
