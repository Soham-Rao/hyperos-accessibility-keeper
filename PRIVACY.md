# Privacy policy

Effective 18 November 2026 · policy revision 1

Accessibility Keeper is an experimental, local Android utility maintained by Soham Rao. It discovers accessibility services and attempts to restore only the services you choose. It does not operate an account service, advertising system or analytics service.

## Information used on your phone

Keeper queries installed accessibility services, including their labels, icons, package names and service identifiers. Broad package visibility also permits access to installed-app inventory. It reads accessibility secure settings and, when diagnostic access is granted, obtains the accessibility system diagnostic dump in memory to determine whether services are bound, binding or crashed. That dump can contain broader accessibility or window metadata; Keeper parses service status and does not save the full dump.

Keeper saves your selected service identifiers, installation timestamps and signing-certificate fingerprints for selected apps, appearance choice, pause time, policy acknowledgment, notification-prompt status, recovery state and reinstall preference in private app storage. These identify apps, not your signing keys. Certificate fingerprints are public certificate digests.

A local journal retains up to 500 entries containing timestamps, app names, service identifiers, status and recovery actions. It may reveal which apps you use and when. Keeper does not request an accessibility service of its own to read screen content, and does not collect typed text or passwords through such a service.

## Sharing and network access

The packaged application has no INTERNET permission. Keeper includes no first-party telemetry, advertising or automatic log upload. AndroidX libraries provide scheduling and interface functionality; the merged manifest includes network-state and wake-lock permissions for background scheduling. Network state does not grant internet access.

Sharing the journal is an explicit action that opens Android's share chooser. The receiving app handles the shared text under its own policy. Review it before sending. Repository and help links open an external browser, whose network requests and policies are separate. Reports or screenshots you post on GitHub may be public. Do not include secrets, full diagnostic dumps or personal notifications.

## Retention and deletion

Logs rotate locally at 500 entries. Clearing Keeper's Android app storage or uninstalling Keeper removes its local data through Android. Android backup is disabled in the manifest; manufacturer migration tools may behave differently. Deleting local data does not remove copies you previously shared elsewhere.

Keeping choices after reinstalling target apps is off by default. When enabled, records may remain while a target app is absent, allowing the same service and signing identity to retain its choice when it returns. Switch the preference off to discard records for currently absent apps. New service components are never automatically selected. Reinstalling Keeper itself normally resets its data and choices.

## Changes and contact

This version is bundled offline with the app and published in the project repository. A changed acknowledgment revision requires review before automatic recovery resumes. For non-sensitive questions use the project's GitHub issues. For sensitive reports follow [security reporting](SECURITY.md); do not disclose private logs publicly.

[Project repository](https://github.com/Soham-Rao/hyperos-accessibility-keeper)
