# Accessibility Keeper — v2

Local fork of [HyperOS Accessibility Guard](https://github.com/haku4130/hyperos-accessibility-guard), retaining its MIT license. This version discovers installed accessibility services directly from Android and lets you choose which ones to protect. Tested on a POCO F6 running HyperOS 3 / Android 16.

## Controls

Enable each app's protection switch to restore its accessibility service automatically when disabled, crashed, or unbound. Turn protection off before intentionally disabling accessibility in Android Settings. Turning protection off leaves the current Android setting unchanged. Pause suspends all recovery for 30 minutes.

The home screen includes app icons, search, All apps / Protected filters, individual switches, and pause/resume. Recent activity is on a separate screen, with a shareable full journal. More options opens a quick menu and a dedicated Settings page for recovery setup, autostart guidance, notifications, appearance, help, and about. Appearance can follow the phone or use light/dark mode, and is saved locally. Help and setup use matching rounded panels. Long-press an app to open it or accessibility settings. Existing v1 selections are retained; newly discovered services default to unprotected. Multiple services in one app remain separate entries. Runtime state requires the ADB-granted DUMP permission and may be unavailable on some devices. The keeper never opens a target app automatically or starts autoclicking.

Recovery removes only the affected service, waits for unbinding, merges it into a freshly read service list, and enables the master switch. Other service entries are preserved. Recovery verifies the settings and attempts to verify binding. A reported settings repair alone does not prove the target app's features are working.

Only the switch changes protection. Tapping an app tile opens a small options panel with Open app. Activity cards that ask you to open an app launch it directly when tapped, as do the corresponding recovery notifications. Keeper gives an in-progress Android bind 30 seconds to settle. If a recovery still cannot bind the service, it records that the app needs to be opened and avoids repeated automatic rebinds while the accessibility settings remain correct. Check now allows an explicit retry. A later healthy check clears this state. This does not claim to fix an unresponsive third-party app silently.

A foreground service observes setting changes, checks on screen/unlock, and runs a health check every 30 seconds while the screen is on. WorkManager adds a 15-minute backstop. Repeated failures are throttled for 60 seconds until a healthy check resets the throttle. Boot startup is included; HyperOS autostart still needs to be allowed manually. Force-stopping the keeper prevents recovery until it is reopened. Long-term persistence requires testing on the actual phone.

## Windows build and install

Use Java 17, Android platform 36, and the included Gradle 8.13 wrapper. Set `JAVA_HOME`, `GRADLE_USER_HOME` (prefer a drive with ample free space), and `sdk.dir` in ignored `local.properties` as appropriate for the machine.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\install.ps1 -Serial YOUR_ADB_DEVICE_SERIAL
```

The installer uses the sibling `AndroidSdk/platform-tools/adb.exe` by default and accepts `-Adb` for another location. It installs the debug APK, grants WRITE_SECURE_SETTINGS, DUMP, and notification access, adds the keeper to the battery whitelist, and opens its controls. Approve the USB installation on the unlocked phone, then select the protection switches you want.

Package: `io.github.soham.accessibilitykeeper`. The upstream namespace is retained internally so this is a separate installation from the original guard. This remains a local version; broader device validation and publication are deferred.

Private planning and device-test artifacts live under ignored `misc/`. Upstream investigation notes remain in `docs/` and describe the original NoScroll app and device, rather than this fork's verified behavior.

## Downloaded apps discovery

The list uses `AccessibilityManager.getInstalledAccessibilityServiceList()`, service labels, icons and component identities. Complete package visibility is required: the narrower service-intent query was verified to omit installed apps on the POCO F6. QUERY_ALL_PACKAGES supports this core discovery feature; it does not enable services by itself.

The phone's Settings resource arrays identify services in built-in categories. On Xiaomi/HyperOS, additional system-service exclusions mirror the POCO F6 Settings APK's AccessibilitySettings / RestrictedPreferenceHelper. These exclude system categories rather than maintain a fixed list of supported user apps. Private OEM cloud policy is not a public Android API; exact matching on future firmware or other manufacturers needs validation.

Android reference: [Settings accessibility service discovery and categorization](https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/main/src/com/android/settings/accessibility/AccessibilitySettings.java).
