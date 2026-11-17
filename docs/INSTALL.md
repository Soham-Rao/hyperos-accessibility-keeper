# Install and set up Keeper

**Signed public APKs are not published yet.** This guide describes the setup path; until a release exists, developers can [build from source](DEVELOPMENT.md). Download availability and supported versions will be stated on each [release](https://github.com/Soham-Rao/hyperos-accessibility-keeper/releases).

## Before you start

- Android 13 or newer is the build minimum. Actual device evidence is currently limited to **POCO F6, HyperOS 3, Android 16**.
- You need a computer and USB cable for one-time permission setup. No root or Shizuku is used by this version.
- Read [Safety and permissions](SAFETY.md). Keeper restores accessibility access for the services you select; select only apps you trust.
- A future signed release may require uninstalling an existing developer build if its signing key differs. That removes Keeper's settings and grants. Follow the release's migration instructions before replacing an installation.

## 1. Get the app and computer tools

When a reviewed release is available, use its APK and published SHA-256 checksum. Do not confuse GitHub's source-code ZIP with an installable APK.

Install Google's [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) on your computer. Open a terminal in the extracted folder. On Windows PowerShell, use `./adb.exe` wherever the examples say `adb`, unless adb is already on your PATH.

<details>
<summary>How to compare an APK checksum</summary>

Use the actual filename you downloaded and compare the full result with the release's SHA256SUMS entry.

Windows PowerShell:

```powershell
Get-FileHash -Algorithm SHA256 ./accessibility-keeper.apk
```

macOS:

```sh
shasum -a 256 accessibility-keeper.apk
```

Linux:

```sh
sha256sum accessibility-keeper.apk
```

A matching checksum confirms the file matches the published checksum. It does not by itself prove an app is safe.

</details>

## 2. Connect your phone

Enable Developer options and USB debugging, connect the phone, and approve the computer's authorization prompt on the unlocked phone. HyperOS may also require **USB debugging (Security settings)** and **Install via USB**; names and availability vary by firmware.

```sh
adb devices
```

The phone should be listed as `device`. If it says `unauthorized`, approve the prompt on the phone. Use a trusted personal computer. Note the device serial; replace `DEVICE_SERIAL` below with that value. Do not post it in an issue.

## 3. Install and grant the setup permissions

Install the APK on the phone, or use the computer:

```sh
adb -s DEVICE_SERIAL install -r accessibility-keeper.apk
```

Keep the phone unlocked and approve any installation prompt. Open Keeper, then grant its two recovery/verification permissions:

```sh
adb -s DEVICE_SERIAL shell pm grant io.github.soham.accessibilitykeeper android.permission.WRITE_SECURE_SETTINGS
adb -s DEVICE_SERIAL shell pm grant io.github.soham.accessibilitykeeper android.permission.DUMP
```

Allow notifications when Keeper asks. If a grant is denied, stop and check the phone's debugging/security settings; don't repeatedly paste commands or disable unrelated security protections.

## 4. Let Keeper run in the background

Allow **Background autostart** for Accessibility Keeper and set its battery mode to unrestricted in your phone's settings. The exact menus vary. These settings help, but do not guarantee that every firmware will keep the app running.

Once setup is complete, you can disconnect the computer. Review and turn off debugging access when you no longer need it; verify Keeper still has its setup grants on your firmware.

## 5. Choose the apps to protect

Use the switch next to a service you already trust. Newly discovered services start unselected.

| Control | What happens |
| --- | --- |
| Protection switch on | Keeper attempts to restore that service when it stops |
| Protection switch off | Keeper stops restoring it; its existing Android accessibility setting is unchanged |
| App tile | Opens app options; it does not toggle protection |
| Pause | Stops recovery for all selected apps for 30 minutes |
| Open app in an activity card | Opens that app directly when recovery needs your help |

## If something does not work

- **Setup needed:** check Settings → Recovery setup and the permission commands above.
- **Open app to finish connecting:** open the affected app and check its own settings. Some apps cannot be fully revived by restoring accessibility settings alone.
- **Keeper stopped:** reopen it and check autostart/battery settings. Force-stopping Keeper prevents it recovering services until it is reopened.
- **App missing from the list:** return to Keeper after installing/updating it. If it appears in your phone's Downloaded apps list but not Keeper, [report the discrepancy](https://github.com/Soham-Rao/hyperos-accessibility-keeper/issues/new/choose).

Include model, Android/HyperOS version, Keeper version and steps. Logs are optional: review them before sharing.

## Stop protection or remove Keeper

On first launch, read the safety summary and policies and acknowledge before recovery can start. Existing choices are preserved during an update, but recovery waits for acknowledgment of the new policy revision. Settings and About provide the same documents offline.

Settings → Keep choices after reinstalling apps defaults off. With it off, removal or a detected new installation clears that service's protection choice. With it on, only the same service identifier and signing certificate can retain a previous choice. New service components start off. This applies to target apps; reinstalling Keeper normally resets Keeper's own choices. The first upgrade to this feature records identities for currently installed selections; it cannot reconstruct an installation history from before that baseline existed.

Turn off the relevant protection switch before deliberately disabling that service in Android Settings. Allow a few seconds for a rebind already in progress to finish; subsequent attempts stop. Pause suspends subsequent recovery for 30 minutes. Uninstalling Keeper removes Keeper; it does not intentionally disable other apps' existing accessibility services. Manage those separately in Android Settings.

To revoke the computer-granted permissions while keeping Keeper installed:

```sh
adb -s DEVICE_SERIAL shell pm revoke io.github.soham.accessibilitykeeper android.permission.WRITE_SECURE_SETTINGS
adb -s DEVICE_SERIAL shell pm revoke io.github.soham.accessibilitykeeper android.permission.DUMP
```

Without the write grant, automatic settings repair cannot work. See [known limitations](TESTING.md).
