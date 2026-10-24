#!/bin/bash
# Upload the release/publishing secrets used by .github/workflows/release.yml
# to this repository's GitHub Actions secrets.
#
# Secrets set:
#   ANDROID_KEYSTORE_B64           base64 of the release keystore (.jks)
#   ANDROID_KEYSTORE_PASSWORD      keystore password
#   ANDROID_KEY_ALIAS              key alias
#   ANDROID_KEY_PASSWORD           key password
#   APP_SECRETS_PROPERTIES_B64     base64 of secrets.properties (OAuth secrets)
#   PLAY_SERVICE_ACCOUNT_JSON      Google Play service-account JSON (AAB upload)
#
# Sources (override with env vars):
#   KEYSTORE_DIR                   default /home/philg/tank/Android/Keystores/PEVPlugShare
#   PLAY_SERVICE_ACCOUNT_JSON      default ./play-service-account.json
#
# Values are passed to `gh secret set` over stdin and never printed.

set -euo pipefail

REPO="${GITHUB_REPOSITORY:-githuba42r/PEVPlugShare}"
KEYSTORE_DIR="${KEYSTORE_DIR:-/home/philg/tank/Android/Keystores/PEVPlugShare}"
PLAY_JSON="${PLAY_SERVICE_ACCOUNT_JSON:-play-service-account.json}"
APP_SECRETS="secrets.properties"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'

prop() {
  grep -E "^$1=" "$KEYSTORE_DIR/keystore.properties" | tail -1 | cut -d= -f2-
}

fail() { echo -e "${RED}ERROR: $1${NC}" >&2; exit 1; }

command -v gh >/dev/null 2>&1 || fail "gh CLI not found"
gh auth status >/dev/null 2>&1 || fail "gh is not authenticated"
[ -d "$KEYSTORE_DIR" ] || fail "keystore dir not found: $KEYSTORE_DIR"
[ -f "$KEYSTORE_DIR/keystore.properties" ] || fail "missing $KEYSTORE_DIR/keystore.properties"
[ -f "$KEYSTORE_DIR/pevplugshare-release.jks" ] || fail "missing $KEYSTORE_DIR/pevplugshare-release.jks"

STORE_PASS="$(prop storePassword)"
KEY_ALIAS="$(prop keyAlias)"
KEY_PASS="$(prop keyPassword)"
[ -n "$STORE_PASS" ] || fail "storePassword empty in keystore.properties"
[ -n "$KEY_ALIAS" ] || fail "keyAlias empty in keystore.properties"
[ -n "$KEY_PASS" ] || fail "keyPassword empty in keystore.properties"

echo "Verifying keystore credentials..."
keytool -list -keystore "$KEYSTORE_DIR/pevplugshare-release.jks" \
  -storepass "$STORE_PASS" -alias "$KEY_ALIAS" >/dev/null 2>&1 \
  || fail "keystore/alias/password rejected — refusing to upload bad secrets"
echo -e "${GREEN}✓ keystore + alias verified${NC}"

echo "Setting secrets on $REPO..."
printf '%s' "$(base64 -w0 "$KEYSTORE_DIR/pevplugshare-release.jks")" \
  | gh secret set ANDROID_KEYSTORE_B64 --repo "$REPO"
printf '%s' "$STORE_PASS" | gh secret set ANDROID_KEYSTORE_PASSWORD --repo "$REPO"
printf '%s' "$KEY_ALIAS"  | gh secret set ANDROID_KEY_ALIAS --repo "$REPO"
printf '%s' "$KEY_PASS"   | gh secret set ANDROID_KEY_PASSWORD --repo "$REPO"
echo -e "${GREEN}✓ signing secrets set${NC}"

if [ -f "$APP_SECRETS" ]; then
  gh secret set APP_SECRETS_PROPERTIES_B64 --repo "$REPO" < <(base64 -w0 "$APP_SECRETS")
  echo -e "${GREEN}✓ APP_SECRETS_PROPERTIES_B64 set${NC}"
else
  echo -e "${YELLOW}! $APP_SECRETS not found — APP_SECRETS_PROPERTIES_B64 left unchanged${NC}"
fi

if [ -f "$PLAY_JSON" ]; then
  gh secret set PLAY_SERVICE_ACCOUNT_JSON --repo "$REPO" < "$PLAY_JSON"
  echo -e "${GREEN}✓ PLAY_SERVICE_ACCOUNT_JSON set${NC}"
else
  echo -e "${YELLOW}! $PLAY_JSON not found — Play upload will be skipped by the workflow${NC}"
fi

echo
echo "Current secrets:"
gh secret list --repo "$REPO"
