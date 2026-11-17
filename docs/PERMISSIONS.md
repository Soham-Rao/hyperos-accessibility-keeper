# Permission guide

Updated 18 November 2026 · available offline in Keeper

Keeper actively restores selected accessibility services. It does not request an accessibility service of its own. Enabling another app's service can give that app access to screen content and interactions. Choose only apps you trust.

## Secure settings: WRITE_SECURE_SETTINGS

Needed to restore or rebind selected accessibility services and enable the accessibility master setting. The grant can change other secure settings too; Android does not limit it to this purpose. Keeper's recovery writes are limited to the accessibility service list and master switch. Without it, recovery cannot write settings. Grant or revoke through the documented ADB setup.

## Diagnostics: DUMP

Used to read accessibility service status: bound, binding or crashed. This powerful grant allows broader system diagnostics. Keeper reads the accessibility dump into memory and parses status; it does not persist the full dump. Without it, enabled settings can still be read and restored, but runtime status may be unverified. An enabled service is not proof of successful binding.

## Installed apps: QUERY_ALL_PACKAGES

Used for complete accessibility-service discovery, labels, icons and opening a chosen app. Narrow queries omitted some services on the tested phone. The permission exposes broader app inventory; Keeper does not save or upload the entire inventory. Newly discovered services start unprotected.

## Notifications: POST_NOTIFICATIONS

Shows recovery updates, warnings and actions such as Open app. You can refuse or disable notifications in Android settings; this can hide useful recovery warnings. Android may still show foreground-service information through its own controls.

## Background operation

FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE support the visible monitoring service. RECEIVE_BOOT_COMPLETED allows restart after boot, subject to consent, permissions and firmware restrictions. HyperOS may additionally require Background autostart and suitable battery settings. These are not guarantees of continuous operation.

AndroidX WorkManager adds WAKE_LOCK for scheduled work and ACCESS_NETWORK_STATE for scheduling infrastructure. Keeper does not request INTERNET and has no automatic data upload. AndroidX also adds an app-specific signature permission for protected internal broadcasts. These merged-manifest permissions are distinct from accessibility access in other apps.

## Your controls

Turn a protection switch off to stop subsequent recovery for that service. Its current Android setting stays unchanged. An in-flight rebind may finish over a few seconds. Pause suspends subsequent recovery for 30 minutes. Turn protection off and let any rebind finish before intentionally disabling a service in Android Settings. Revoke the write grant to prevent further secure-settings writes. Uninstalling Keeper stops Keeper's work and does not intentionally disable other apps' accessibility services.

Setup and revocation commands: https://github.com/Soham-Rao/hyperos-accessibility-keeper/blob/main/docs/INSTALL.md

Android permission reference: https://developer.android.com/reference/android/Manifest.permission
