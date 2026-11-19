# Accessibility Keeper

Keep the accessibility apps **you choose** running.

[![Android CI](https://github.com/Soham-Rao/hyperos-accessibility-keeper/actions/workflows/build.yml/badge.svg)](https://github.com/Soham-Rao/hyperos-accessibility-keeper/actions/workflows/build.yml)
[![Experimental beta](https://img.shields.io/badge/status-experimental_beta-e9bc72)](docs/TESTING.md)
[![MIT license](https://img.shields.io/badge/license-MIT-88dfbf)](LICENSE)
[![Android 13 or newer](https://img.shields.io/badge/Android-13%2B-88dfbf)](docs/INSTALL.md)

<picture>
  <source media="(max-width: 640px)" srcset="docs/media/overview-mobile.svg">
  <img src="docs/media/overview.svg" alt="Illustrated Keeper interface with invented apps: choose protection, see recovery, and open an app when needed. Demo data, not a device screenshot.">
</picture>

> **Experimental beta.** Download the [signed APK](https://github.com/Soham-Rao/hyperos-accessibility-keeper/releases/tag/v0.2.0-beta.1). Tested on **POCO F6 · HyperOS 3 · Android 16**.

Keeper finds your installed accessibility services and attempts to restore the ones you protect when they stop. Some apps still need to be opened to finish connecting.

## Start here

| I want to… | Go to |
| --- | --- |
| Install and set up Keeper | [Beginner installation](#install-accessibility-keeper) |
| Understand permissions or stop protection | [Safety and permissions](docs/SAFETY.md) |
| Report a bug or suggest something | [Open an issue](https://github.com/Soham-Rao/hyperos-accessibility-keeper/issues/new/choose) |
| Check compatibility or help test | [Testing and known limitations](docs/TESTING.md) |
| Build, fork or change the app | [Developer guide](docs/DEVELOPMENT.md) · [Contributing](CONTRIBUTING.md) |
| Find signed APKs | [Releases](https://github.com/Soham-Rao/hyperos-accessibility-keeper/releases) |

## Install Accessibility Keeper

You’ll need an Android phone running **Android 13 or newer**, a computer you trust, and a USB cable that can transfer data. You download and install the app on your phone, then use the computer once to give Keeper the permissions it needs. If you only have a phone, you can install the APK now and finish setup when you have access to a computer.

### 1. Download and install on your phone

1. Open the [official beta download page](https://github.com/Soham-Rao/hyperos-accessibility-keeper/releases/tag/v0.2.0-beta.1) in your phone’s browser.
2. Scroll to **Assets** and tap **accessibility-keeper-0.2.0-beta.1.apk**. An APK is the file Android uses to install an app. If you see **Source code (zip)** or **SHA256SUMS**, those are separate files.
3. When the download finishes, tap **Open**. If you missed that button, open your phone’s **Files** app, go to **Downloads**, and tap the APK.
4. If Android says this app can’t install unknown apps, tap **Settings** in that message and turn on **Allow from this source** for the browser or file manager you just used. On some phones it’s called **Install unknown apps**. Go back to the installer and tap **Install**. Check that the prompt says **Accessibility Keeper** and that you downloaded it from the official page above. If Android shows a separate security block, stop and check the message before continuing.
5. Tap **Open**. Read the safety screen, then continue if you agree. You can turn **Allow from this source** back off right away. Search for **Install unknown apps** in Settings and select the same browser or file manager.

If you previously installed a developer build with a different signing key, Android may reject the update. Uninstalling that build lets you install this beta, but removes your Keeper choices and permissions. Make a note of your selected services first so you can set them up again.

### 2. Get the computer ready

1. On the computer, open [Google’s Platform Tools downloads](https://developer.android.com/tools/releases/platform-tools). Choose **Windows**, **Mac**, or **Linux**, then download the ZIP file.
2. Open your computer’s **Downloads** folder and extract the ZIP. On Windows, right-click it and choose **Extract All**. On Mac, double-click it. Open the extracted **platform-tools** folder. It should contain **adb.exe** on Windows or **adb** on Mac and Linux.
3. Open a command window in that folder. On Windows, click the address bar in File Explorer, type **powershell**, and press **Enter**. On Mac or Linux, open **Terminal**, type **cd** followed by a space, drag the **platform-tools** folder into the window, and press **Enter**. This tells the command window where to find the tool.

### 3. Connect and approve your phone

1. On HyperOS, open **Settings → About phone**. Tap **OS version** repeatedly until you see a message that developer options are enabled. The label may be **HyperOS version** or **Build number** on your phone.
2. Search Settings for **Developer options**, or look under **Additional settings**. Open it and turn on **USB debugging**.
3. Plug the phone into the computer and unlock it. When the phone asks **Allow USB debugging?**, tap **Allow** for the computer you just connected.
4. On the computer, copy the line for your system below, paste it into the command window, and press **Enter**.

| Computer | Command |
| --- | --- |
| Windows | `./adb.exe devices` |
| Mac or Linux | `./adb devices` |

Look for one row under **List of devices attached** ending in **device**. The letters and numbers at the start are your phone’s identifier. Keep that private. If the row says **unauthorized**, unlock the phone and check for the approval prompt, then run the command again. If there’s no row, try another cable or USB port. If more than one device appears, disconnect the others before continuing.

### 4. Give Keeper its two permissions

Copy one line at a time and press **Enter** after each line. These commands give Keeper permission to restore your selected services and check whether they’re connected. A successful command usually returns to the next prompt without printing anything.

**Windows**

```powershell
./adb.exe shell pm grant io.github.soham.accessibilitykeeper android.permission.WRITE_SECURE_SETTINGS
./adb.exe shell pm grant io.github.soham.accessibilitykeeper android.permission.DUMP
```

**Mac or Linux**

```sh
./adb shell pm grant io.github.soham.accessibilitykeeper android.permission.WRITE_SECURE_SETTINGS
./adb shell pm grant io.github.soham.accessibilitykeeper android.permission.DUMP
```

If you see **permission denied** or **SecurityException**, check whether your HyperOS Developer options also has **USB debugging (Security settings)**. Some builds need that separate switch for permission grants. If the error persists, see the [setup troubleshooting guide](docs/INSTALL.md#if-something-does-not-work).

### 5. Pick what Keeper should protect

1. Open Keeper on your phone. If it still says **One-time setup needed**, check that both commands in step 4 ran successfully. Allow notifications if you want to see recovery updates.
2. In your phone’s Settings, search for **Background autostart** and allow it for **Accessibility Keeper** if your phone offers the setting. Open Keeper’s **App info → Battery** settings and choose **No restrictions** or **Unrestricted** if available. Menu names vary by phone.
3. In Keeper, turn on the switch beside an accessibility service you already use and trust. Start with one. A protected service may still need you to tap **Open app** to finish connecting.
4. Unplug the cable. You can turn **USB debugging** off afterward. Open Keeper again and check that it still shows protection on.

For checksum verification, setup errors or permission removal, see the [full installation guide](docs/INSTALL.md).

## Everyday use

1. **Choose apps:** turn on their protection switches. New apps start unselected.
2. **Let Keeper help:** check the running status or Recent activity. Tap an app tile for options.
3. **Stay in control:** pause all recovery, or turn off a protection switch before deliberately disabling that service in Android Settings.

**Switch off means stop protection.** It leaves the service's existing Android accessibility setting unchanged. Keeper never starts clicking or opens another app without your action.

## Before granting access

Keeper uses powerful setup permissions to **monitor and restore/rebind your selected services**. Only protect third-party accessibility apps you trust. Read [what each permission does](docs/SAFETY.md) before setup. Recovery is best effort; it is not guaranteed across all apps or HyperOS versions.

## Credits

Maintained by [Soham-Rao](https://github.com/Soham-Rao). Based on [HyperOS Accessibility Guard by haku4130](https://github.com/haku4130/hyperos-accessibility-guard), with the original [MIT license](LICENSE) retained. This fork adds selectable multi-app protection, a redesigned interface, safer list-preserving recovery and an opening-required fallback. No upstream or device-vendor endorsement is implied.

[Fork changes](CHANGES.md) · [Third-party notices](THIRD_PARTY_NOTICES.md) · [Source provenance](docs/provenance/README.md)

[Privacy](PRIVACY.md) · [Terms](TERMS.md) · [Security](SECURITY.md) · [Permission guide](docs/PERMISSIONS.md)

[Audit findings and remaining release checks](docs/AUDIT.md)

[Device compatibility](docs/COMPATIBILITY.md) · [Release preparation](docs/RELEASE.md)

[Watch the animated walkthrough](docs/media/beta/keeper-walkthrough.svg) · [Browse the demo stills](docs/media/beta)
