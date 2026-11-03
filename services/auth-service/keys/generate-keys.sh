#!/usr/bin/env bash
# Generates the RSA key pair used by auth-service to sign/verify JWTs.
# Output (in this directory): private.pem (PKCS#8) and public.pem.
# Usage: ./generate-keys.sh [--force]
set -euo pipefail

# Resolve the keys directory regardless of where the script is invoked from
KEYS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v openssl >/dev/null 2>&1; then
  echo "error: openssl not found on PATH" >&2
  exit 1
fi

if [[ ( -f "$KEYS_DIR/private.pem" || -f "$KEYS_DIR/public.pem" ) && "${1:-}" != "--force" ]]; then
  echo "Keys already exist in $KEYS_DIR. Re-run with --force to overwrite." >&2
  exit 1
fi

TMP_PKCS1="$KEYS_DIR/private-pkcs1.pem"
trap 'rm -f "$TMP_PKCS1"' EXIT

openssl genrsa -out "$TMP_PKCS1" 2048
openssl pkcs8 -topk8 -inform PEM -in "$TMP_PKCS1" -out "$KEYS_DIR/private.pem" -nocrypt
openssl rsa -in "$TMP_PKCS1" -pubout -out "$KEYS_DIR/public.pem"

echo "Generated $KEYS_DIR/private.pem and $KEYS_DIR/public.pem"
