#!/usr/bin/env bash
# One-time setup of the secrets the release workflows need. Run from the repository root,
# logged in to gh as the repository owner. Each part is safe to skip if it's already done.
#
#   tools/setup-release.sh signing-key   # Maven Central: release-signing GPG key -> MAVEN_SIGNING_KEY(_PASSWORD)
#   tools/setup-release.sh php-split     # Packagist: the read-only mirror repo and its deploy key -> PHP_SPLIT_DEPLOY_KEY
#
# The Maven Central user token is set by hand (it's shown once on central.sonatype.com):
#   gh secret set MAVEN_CENTRAL_USERNAME
#   gh secret set MAVEN_CENTRAL_PASSWORD
set -euo pipefail
REPO=RohanG27/thesportsdbLibrary
SPLIT=RohanG27/thesportsdb-client-php
UID_="RohanG27 (thesportsdb-client release signing) <17254313+RohanG27@users.noreply.github.com>"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
umask 077

signing_key() {
  # A dedicated key in a throwaway keyring: it never enters your own keyring, and the private
  # key and passphrase exist only as GitHub secrets once this finishes.
  export GNUPGHOME="$tmp/gnupg"
  mkdir -m 700 "$GNUPGHOME"
  head -c 32 /dev/urandom | base64 | tr -d '=+/\n' > "$tmp/passphrase"
  gpg --batch --quiet --pinentry-mode loopback --passphrase-file "$tmp/passphrase" \
    --quick-gen-key "$UID_" rsa4096 sign 0
  fpr="$(gpg --list-secret-keys --with-colons | awk -F: '/^fpr/ {print $10; exit}')"
  gpg --batch --quiet --pinentry-mode loopback --passphrase-file "$tmp/passphrase" \
    --armor --export-secret-keys "$fpr" > "$tmp/secret.asc"

  # Maven Central checks signatures against public keyservers.
  gpg --keyserver hkps://keyserver.ubuntu.com --send-keys "$fpr"
  gpg --armor --export "$fpr" | curl -fsS -T - https://keys.openpgp.org > /dev/null || \
    echo "note: keys.openpgp.org upload failed; keyserver.ubuntu.com is enough for Maven Central"

  gh secret set MAVEN_SIGNING_KEY -R "$REPO" < "$tmp/secret.asc"
  gh secret set MAVEN_SIGNING_KEY_PASSWORD -R "$REPO" < "$tmp/passphrase"

  # If the key is ever compromised, publish this to revoke it.
  rev="$HOME/thesportsdb-client-signing-key-revocation.asc"
  cp "$GNUPGHOME/openpgp-revocs.d/$fpr.rev" "$rev"
  echo "signing key $fpr: secrets set, public key on keyservers, revocation certificate at $rev"
}

php_split() {
  if ! gh repo view "$SPLIT" > /dev/null 2>&1; then
    gh repo create "$SPLIT" --public \
      --description "Read-only mirror of php/ in $REPO, for Packagist. Issues and pull requests go to the main repository." \
      --homepage "https://github.com/$REPO" --disable-issues --disable-wiki
  fi
  ssh-keygen -q -t ed25519 -N "" -C "php-split deploy key" -f "$tmp/deploy"
  gh repo deploy-key add "$tmp/deploy.pub" -R "$SPLIT" --allow-write --title "php-split workflow in $REPO"
  gh secret set PHP_SPLIT_DEPLOY_KEY -R "$REPO" < "$tmp/deploy"
  echo "mirror $SPLIT ready; PHP_SPLIT_DEPLOY_KEY set. Run the 'PHP split' workflow to fill it:"
  echo "  gh workflow run php-split.yml -R $REPO"
}

case "${1:-}" in
  signing-key) signing_key ;;
  php-split) php_split ;;
  *) sed -n '2,10p' "$0"; exit 1 ;;
esac
