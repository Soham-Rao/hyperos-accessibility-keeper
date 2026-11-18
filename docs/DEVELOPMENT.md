# Build, fork and change Keeper

## Toolchain

| Item | Project setting |
| --- | --- |
| JDK | 17 |
| Gradle | 8.13, via the checked-in wrapper |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.2.0 |
| Compile / target SDK | 36 |
| Build tools used by the current AGP | 35.0.0 |
| Minimum Android | API 33; compatibility still requires device testing |
| Application ID | `io.github.soham.accessibilitykeeper` |

Install Java 17 and the Android SDK using Android Studio or Android's command-line tools. Ensure `JAVA_HOME` points to Java 17 and configure your SDK path with `ANDROID_HOME` or ignored `local.properties`.

```sh
sdkmanager "platforms;android-36" "build-tools;35.0.0"
```

For example, a Windows SDK at `D:/Android/Sdk` can be configured with `sdk.dir=D\:/Android/Sdk` in `local.properties`. Do not commit that machine-specific file. Keep large SDK/Gradle caches on a drive with sufficient free space.

## Fork and clone

Use GitHub's Fork button for a public fork when the project is public, then clone your repository:

```sh
git clone git@github.com:YOUR_ACCOUNT/hyperos-accessibility-keeper.git
cd hyperos-accessibility-keeper
git remote add upstream git@github.com:Soham-Rao/hyperos-accessibility-keeper.git
```

Keep license/copyright notices and acknowledge the original upstream project. Review [Contributing](../CONTRIBUTING.md) before submitting changes.

## Build and check

Windows PowerShell:

```powershell
./gradlew.bat --build-cache testDebugUnitTest lintDebug assembleDebug
```

macOS / Linux:

```sh
./gradlew --build-cache testDebugUnitTest lintDebug assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. This is a developer build signed with the builder's debug key, not a published release-key-signed APK. Different signing keys can prevent an in-place update.

On Windows, the development installer can install the built APK, grant setup permissions, add a device-idle whitelist entry, and open the app:

```powershell
./install.ps1 -Serial DEVICE_SERIAL -Adb "D:/Android/Sdk/platform-tools/adb.exe"
```

The script's default adb path assumes a sibling `AndroidSdk` directory; pass `-Adb` for another SDK. For other platforms use the [manual setup instructions](INSTALL.md). The inherited `install.sh` is intentionally disabled rather than install/configure the wrong upstream package.

## Where changes belong

Source namespace retained from upstream: `app/src/main/java/io/github/haku4130/noscrollguard/`.

| File / directory | Responsibility |
| --- | --- |
| `Targets.kt`, `DownloadedAppsPolicy.kt` | Installed service discovery and category filtering |
| `repair/` | Preserve unrelated services, rebind a selected target, decide when to wait or ask for help |
| `state/TargetRuntime.kt` | Runtime binding/crash verification with unknown-state handling |
| `RecoveryStatus.kt` | Remember when an app needs opening rather than endless retries |
| `service/` | Foreground monitoring, recovery entry points and notifications |
| `ui/MainActivity.kt` | App list, switches, menus, settings and activity |
| `ui/AboutActivity.kt`, `ui/LegalDocumentActivity.kt`, `ui/InfoUi.kt` | Credits, project links and offline license documents |
| `log/` | Bounded local activity journal and optional service metadata |
| `app/src/test/` | Unit tests for settings preservation, decisions, parsing and logs |

`docs/DESIGN.md` and `docs/BUG-REPORT.md` contain the original upstream investigation. They are historical context about another target/device, not verification of this fork on every phone.

## Keep CI inexpensive

- One Ubuntu 24.04 job; no emulator, OS matrix or scheduled builds.
- Code/build/workflow changes on main and non-draft PRs run unit tests, lint and debug packaging in one Gradle invocation.
- README, docs, media and issue-template-only changes skip Android CI.
- New runs cancel superseded runs for the same ref; the job is capped at 10 minutes.
- Gradle state is cached; only main writes the cache. No second Java cache duplicates it.
- APK upload is off by default. To keep one temporarily, run **Actions → Android CI → Run workflow** with `upload_debug_apk` enabled. It expires after three days. Failure reports also expire after three days.
- The release workflow is separate and not configured for a public release yet. Do not push release tags as a CI test.

If branch protection later requires this workflow, review GitHub's path-filter/required-check behavior: a workflow skipped by path filters may leave a required check pending. Do not enable a rule that blocks documentation-only PRs without addressing that.

## Device testing and diagnostics

CI compiles the optional device-test APK in the same Gradle invocation, but does not run an emulator or device suite. Run the [isolated device checks](../device-tests/README.md) locally; compilation alone does not establish a UI/device pass.

Follow [Testing](TESTING.md). Unit tests do not establish device compatibility, battery behavior or actual function of a third-party app. Do not intentionally disrupt someone's selected services without a controlled, reversible device test.

Keep raw captures, device identifiers, machine settings and signing files out of commits. `misc/` is private working space and ignored entirely.

## Updating dependencies and notices

The root `PRIVACY.md`, `TERMS.md`, `SECURITY.md` and `docs/PERMISSIONS.md` are also bundled as offline assets. Changes to these files correctly trigger Android CI because they affect the APK. Keep them aligned with actual behavior and bump `Consent.REVISION` when users must review a changed safety acknowledgment. Test both fresh installs and an upgrade with existing selections; do not silently clear selected apps during migration.

The build checks that `legal/runtime-dependencies.json` matches the resolved release runtime component list. It also packages the canonical root MIT license and files under `legal/` into APK assets, used by the offline About/license screens.

When changing runtime dependencies:

1. Inspect `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` (use `gradlew.bat` on Windows).
2. Review exact-version Maven license metadata, including parent POMs, and embedded LICENSE/NOTICE/COPYING files in AAR/JAR archives and nested classes.jar files.
3. Update the component inventory, artifact hashes, `THIRD_PARTY_NOTICES.md` and `legal/THIRD_PARTY_NOTICES.txt`. Include any newly required full license or notice text under `legal/`; add an offline reader action if a different license is introduced.
4. Rebuild, inspect the packaged assets and check the offline documents in the app. Keep upstream copyright/permission notices.

The build checks both runtime component lists and the SHA-256 of their resolved artifacts against the reviewed inventory. A negative check with an altered checksum confirmed that the build rejects it. This covers the APK's listed runtime artifacts, not every Gradle plugin, POM or build-tool input; complete build-chain verification remains a release-preparation task. Refresh the notice review when dependencies or assets change. See [third-party notices](../THIRD_PARTY_NOTICES.md) and [provenance](provenance/README.md).
