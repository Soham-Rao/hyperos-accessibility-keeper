# Preparing a beta release

The first candidate uses package `io.github.soham.accessibilitykeeper`, Android 13 or later, and the version declared in the app build. Use a distinct beta version and increment the version code before producing a public candidate.

1. Choose the permanent signing key and make an encrypted backup in independent storage. Verify that the backup opens and yields the same certificate fingerprint. Keep the password separately. The first public signature determines which future APKs Android can install as updates.
2. Configure the private repository Actions secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`. Set `RELEASE_CERT_SHA256` to the independently checked public certificate fingerprint. Keep the keystore and password out of Git.
3. Run **Package signed beta candidate** manually on main. Download the resulting candidate artifact and review its APK, SHA256SUMS and BUILD-INFO.txt. The workflow builds once and has read access to the repository. Record the candidate's actual commit, APK hash and certificate fingerprint in the release notes.
4. Verify the downloaded file with `sha256sum -c SHA256SUMS` or compare its PowerShell `Get-FileHash -Algorithm SHA256` result. Run Android SDK `apksigner verify --print-certs` and compare the certificate SHA-256 to the independently recorded fingerprint.
5. Test clean install, grants, reboot, idle, recovery and the signed upgrade path. Use a disposable installation for debug-to-release migration testing. Android requires matching signing certificates for an in-place upgrade. Moving from a differently signed debug build requires uninstalling it, reinstalling, granting permissions again and reselecting services.
6. Review the release notes and media before preparing the first prerelease. After the visibility decision, enable GitHub private vulnerability reporting and verify the report form from an account without maintainer access. Follow the fallback in the [security policy](../SECURITY.md) until the dedicated route works.

See [GitHub's private reporting configuration](https://docs.github.com/en/code-security/how-tos/report-and-fix-vulnerabilities/configure-vulnerability-reporting/configure-for-a-repository).
