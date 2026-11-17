# Testing and compatibility

This is experimental software focused on accessibility-service recovery, particularly on HyperOS. Please report useful evidence rather than assume it works on every device.

## Current evidence

As of 2026-11-18:

| Environment | Evidence | Limits |
| --- | --- | --- |
| POCO F6 · HyperOS 3 / OS3.0.303.0.WNPINXM · Android 16 | Installed-service discovery; selected-service recovery; preservation of unrelated entries; app-opening actions; menus/settings and theme checks | One maintainer device, limited selected-service sample |
| Local unit suite | 67 passing tests, including update/reinstall identity decisions | Does not simulate real firmware or third-party app behavior |
| POCO F6 · disposable verification build and dummy service | Consent blocks repair; protection off leaves Android setting unchanged; deliberate disable stays off; pause/resume; controlled short pause expiry; revoked write grant blocks repair; revoked diagnostic grant yields unknown status; reinstall default-off clears selection; opted-in same-signer reinstall retains selection even after force-stop; uninstalling Keeper leaves enabled-service list unchanged | One device; dummy target; short expiry test does not represent a 30-minute or multi-day endurance run |
| Reboot / multi-day / other models | Not yet established by a recorded test matrix | Do not infer these from boot receiver code or one successful recovery |

The [Android CI page](https://github.com/Soham-Rao/hyperos-accessibility-keeper/actions/workflows/build.yml) is the source of current remote check results; a local pass is not a claim that every CI run passed.

## Known limitations

- Some apps need opening after recovery before their service becomes usable. Open app actions are provided for this case.
- Android's bound state confirms the system connection, not the correctness of the third-party app's own features.
- Runtime diagnostics may be unavailable or differ by firmware; the UI distinguishes unverified status.
- HyperOS resource/category rules were checked against the maintainer's phone. Other firmware and private OEM filtering policies can differ.
- Force-stopping Keeper prevents recovery until you open it again. Autostart and unrestricted battery settings are helpful but not universal guarantees.
- Reinstall behavior has limited single-device fixture coverage. Signing-identity changes are covered in unit tests, not a physical replacement-signer test. Profiles/dual apps and long-term battery use remain unverified.

## Reproduce the isolated device checks

The custom instrumentation runner refuses to run against the normal package. Build a disposable copy with `./gradlew -PverificationBuild=true assembleDebug assembleDebugAndroidTest`; its application ID ends in `.verification`. The checks in `VerificationInstrumentation` use only a dummy service named `com.example.keeperfixture/com.example.keeperfixture.FixtureService`, never personal apps. Install a minimal trusted accessibility-service fixture with that identity before running them.

Grant the disposable copy the documented write/diagnostic permissions, then run its test runner with `adb shell am instrument -w -e case controls io.github.soham.accessibilitykeeper.verification.test/io.github.haku4130.noscrollguard.VerificationInstrumentation`. Other explicit cases cover externally revoked grants and target reinstallation; review the test source before running them. These are manual device checks, not CI emulator jobs. Uninstall the disposable copy, its test package and fixture afterward, preserving unrelated accessibility entries. Do not substitute your daily Keeper package in destructive commands.

## Help test a beta

Use a trusted app and a noncritical situation. Keep a way to return to Android Settings and disable protection. Do not make essential assistive tools unavailable just to manufacture a test.

Record:

1. Keeper version/source commit and install type (debug or published release).
2. Device model, Android version and HyperOS version.
3. What you did, what you expected and what actually happened.
4. Whether the service was disabled, connecting, running or asking to be opened.
5. Whether unrelated services changed, and whether restarting/unlocking affected the result.
6. Optional sanitized journal excerpt or redacted screenshot, only if useful.

Do not include IMEI, device serials, accounts, tokens or full device dumps. App names are optional if you do not want to disclose them; a service type and reproducible description can still help.

[Report a bug or compatibility result](https://github.com/Soham-Rao/hyperos-accessibility-keeper/issues/new/choose). Reports from other devices will be labeled as community-reported until independently reproduced.

Developers: use the [build/check commands](DEVELOPMENT.md#build-and-check). Add regression tests for actual failure patterns rather than tests that merely repeat implementation details.
