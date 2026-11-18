# Isolated device checks

These are manual developer checks, separate from CI. They need Android 13 or later and an authorized ADB connection. They install a disposable Keeper package ending in `.verification` and a dummy service, leaving the normal Keeper package installed.

The fixture has no network permission, no screen-content retrieval and no gesture capability. Its accessibility callbacks do nothing. A long service label helps expose layout problems. This source uses the repository's MIT license.

## Build

Configure your Android SDK through `ANDROID_HOME` or ignored `local.properties` files in the root and fixture project. Use the existing wrapper and Java 17:

```sh
./gradlew -PverificationBuild=true assembleDebug assembleDebugAndroidTest
./gradlew -p device-tests/fixture assembleDebug
```

On Windows use `gradlew.bat`. The outputs are `app/build/outputs/apk/debug/app-debug.apk`, `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`, and `device-tests/fixture/build/outputs/apk/debug/keeper-fixture-debug.apk`. The first is a verification APK when the property above is enabled; build again without it for your normal APK.

## Install and run

Replace `DEVICE_SERIAL` with your chosen test device. Keep it unlocked for installation and UI checks.

```sh
adb -s DEVICE_SERIAL install -r device-tests/fixture/build/outputs/apk/debug/keeper-fixture-debug.apk
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s DEVICE_SERIAL install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s DEVICE_SERIAL shell pm grant io.github.soham.accessibilitykeeper.verification android.permission.WRITE_SECURE_SETTINGS
adb -s DEVICE_SERIAL shell pm grant io.github.soham.accessibilitykeeper.verification android.permission.DUMP
adb -s DEVICE_SERIAL shell pm grant io.github.soham.accessibilitykeeper.verification android.permission.POST_NOTIFICATIONS
adb -s DEVICE_SERIAL shell am instrument -w -e case controls io.github.soham.accessibilitykeeper.verification.test/io.github.haku4130.noscrollguard.VerificationInstrumentation
adb -s DEVICE_SERIAL shell am instrument -w -e case ui-audit io.github.soham.accessibilitykeeper.verification.test/io.github.haku4130.noscrollguard.VerificationInstrumentation
```

The runner refuses the normal application ID or a verification copy containing selected non-fixture services. `controls` changes only the fixture's accessibility entry, preserving unrelated entries; it covers consent, protection off, intentional disable, pause/resume and a short controlled expiry. `ui-audit` checks selection behavior, search, roles, keyboard layout, policy links, log preview, theme changes and large text. External links/shares are intercepted, so no log is sent or browser request made by this suite. Font/theme changes are confined to the disposable app process/preferences.

UI automation must use `UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES`; the default can suppress other services and contaminate recovery results. The suite sets that flag. Do not use a generic hierarchy dump as independent proof of a service stopping during an accessibility test.

Additional cases in `VerificationInstrumentation` cover externally revoked grants and reinstall expectations. Read the code and preserve the current service list before destructive checks. Do not substitute your everyday Keeper package in uninstall commands. Controlled fixture tests do not establish third-party app compatibility or human TalkBack usability.

For a renamed-component fixture, build it with `./gradlew -p device-tests/fixture -PrenameFixture=true assembleDebug`, update an installed original fixture signed by the same debug key, and use `expect-new-component-off`. Save the original APK before the variant build overwrites the output. Replacement-signer checks require uninstalling the fixture and signing its APK with a separate disposable test key; never use your release key.

Some HyperOS versions require a fresh Install via USB confirmation after every uninstall. The optional `approve-fixture-install` case can handle only the known fixture name in a supported system installer, immediately after you initiate installation of your locally built fixture. It does not select Remember my choice. Do not use this helper with an unknown APK. The helper may restart the verification process after the uninstall, so record that detail in missed-event results.

## Clean up

Pull any private captures you need before removal, then uninstall the three disposable packages:

```sh
adb -s DEVICE_SERIAL uninstall io.github.soham.accessibilitykeeper.verification.test
adb -s DEVICE_SERIAL uninstall io.github.soham.accessibilitykeeper.verification
adb -s DEVICE_SERIAL uninstall com.example.keeperfixture
```

Check that the fixture entry is absent from `enabled_accessibility_services`; remove only that exact entry if the firmware left it behind. Never overwrite the entire list from an old snapshot, as the user or another app may have changed it meanwhile.
