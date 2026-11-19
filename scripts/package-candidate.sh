#!/usr/bin/env bash
set -euo pipefail

tools=${1:?Android build-tools directory required}
out=${2:?Output directory required}
expected=${RELEASE_CERT_SHA256:?Expected signer SHA-256 required}
expected=${expected//:/}
[[ "$expected" =~ ^[[:xdigit:]]{64}$ ]]
apk=app/build/outputs/apk/release/app-release.apk
signature=$("$tools/apksigner" verify --print-certs "$apk")
mapfile -t signers < <(sed -nE 's/^Signer #[0-9]+ certificate SHA-256 digest: ([a-fA-F0-9]+)$/\1/p' <<< "$signature")
[[ ${#signers[@]} == 1 && "${signers[0],,}" == "${expected,,}" ]] || {
  echo 'APK signer does not match the expected release certificate' >&2
  exit 1
}
badging=$("$tools/aapt" dump badging "$apk")
package=$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<< "$badging")
version=$(sed -n "s/^package: .*versionName='\([^']*\)'.*/\1/p" <<< "$badging")
code=$(sed -n "s/^package: .*versionCode='\([^']*\)'.*/\1/p" <<< "$badging")
[[ "$package" == io.github.soham.accessibilitykeeper && "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?$ && "$code" =~ ^[0-9]+$ ]]
grep -qx "sdkVersion:'33'" <<< "$badging"
grep -qx "targetSdkVersion:'36'" <<< "$badging"
if grep -q "android.permission.INTERNET" <<< "$badging"; then
  echo 'Unexpected INTERNET permission' >&2
  exit 1
fi
for notice in MIT.txt THIRD_PARTY_NOTICES.txt APACHE-2.0.txt PRIVACY.md TERMS.md SECURITY.md PERMISSIONS.md; do
  unzip -tqq "$apk" "assets/legal/$notice"
done
mkdir "$out"
name="accessibility-keeper-$version.apk"
cp "$apk" "$out/$name"
(
  cd "$out"
  sha256sum "$name" > SHA256SUMS
  sha256sum --check SHA256SUMS
  printf 'Source: %s\nPackage: %s\nVersion: %s (%s)\nSigning certificate SHA-256: %s\n' \
    "$(git rev-parse HEAD)" "$package" "$version" "$code" "${signers[0]}" > BUILD-INFO.txt
)
