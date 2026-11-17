# Safety and permissions

**Keeper monitors and restores/rebinds accessibility services you select. It is not a monitoring-only app.** It does not decide which third-party apps deserve access; that choice stays with you.

Only protect accessibility apps you trust. Depending on their capabilities, third-party accessibility services can inspect screen content and interact with apps. Restoring their accessibility access can restore those capabilities too.

## Why permissions are requested

| Permission | Keeper's intended use | Broader capability to understand |
| --- | --- | --- |
| `WRITE_SECURE_SETTINGS` | Restore/rebind selected accessibility services and the accessibility master setting | Can change other secure system settings; it is a powerful grant |
| `DUMP` | Check whether accessibility services are bound, binding or crashed | Can access broader system diagnostics; it is not limited to Keeper's status screen |
| `QUERY_ALL_PACKAGES` | Discover installed accessibility services that narrower package queries missed on the tested device | Exposes installed-app inventory, not only selected apps |
| `POST_NOTIFICATIONS` | Recovery updates and actions needing your attention | You control notifications in Android settings |
| Foreground-service permissions | Run the visible monitoring service | Background work has battery/platform implications |
| `RECEIVE_BOOT_COMPLETED` | Resume monitoring after restart | Firmware autostart restrictions and force-stop behavior still matter |

The merged manifest additionally includes AndroidX scheduling permissions (`WAKE_LOCK`, `ACCESS_NETWORK_STATE`) and an app-specific signature permission. It has no `INTERNET` permission. Read the complete [permission guide](PERMISSIONS.md), [privacy policy](../PRIVACY.md), [terms](../TERMS.md) and [security reporting guidance](../SECURITY.md), also bundled offline in the app. A broader independent security audit has not been performed.

## Your controls

- Newly discovered services default to unprotected.
- First use and the policy-upgrade screen require acknowledgment before recovery starts. Existing installed selections are preserved while recovery waits.
- Keeping choices after reinstalling target apps defaults off. Opting in permits only the same selected component and signing identity to retain its choice. Keeper's own uninstall clears its local data through Android.
- Only a switch changes protection. Tapping a tile opens app options.
- Turning protection off stops restoration but leaves the service's existing Android setting unchanged.
- Turn protection off before deliberately disabling that service in Android Settings.
- Pause suspends recovery for 30 minutes. Force-stop is different: reopen Keeper to resume it.
- Some apps still need to be opened after settings are restored. Keeper provides an explicit Open app action rather than silently opening them.
- See [installation and permission revocation](INSTALL.md#stop-protection-or-remove-keeper).

## Logs and reports

Keeper stores local selections and a bounded activity journal. Journal entries can identify apps and reveal when protection/recovery happened. Sharing a journal is your explicit action; inspect it first and remove anything you do not want public. Avoid posting full system dumps, device serials, account information or other apps' notifications.

## Limits

Recovery is best effort. Correct settings and a bound service do not prove that every feature inside another app works. Device firmware can stop background work, and unsupported diagnostics cannot be treated as verified success.

The project retains the [MIT license and its warranty/liability disclaimer](../LICENSE). No claim is made that a disclaimer eliminates every legal risk. Broader device testing and a packaged-app security and usability audit remain part of public beta preparation.

References: [Android secure-settings permission](https://developer.android.com/reference/android/Manifest.permission#WRITE_SECURE_SETTINGS), [accessibility service capabilities](https://developer.android.com/guide/topics/ui/accessibility/service).
