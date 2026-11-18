# Maintainer audit — 18 November 2026

Scope: security, recovery edge cases and usability of the experimental 0.2.0 candidate, starting from `e49e7f7063e097b7c1a915c15751ee16635bdf44`. This is a maintainer review with automated checks, not an independent security assessment or a promise of compatibility.

Status: this maintainer review, regression fixes and the recorded single-device checks are complete. Broader device coverage and the release gates below remain open. Results are also summarized in [Testing](TESTING.md).

Local validation: 86 passing unit tests, debug and unsigned-release builds, device-test APK compilation, lint, workflow validation and canonical packaged-policy/license checks. The standalone fixture builds in original and renamed-component variants. The isolated control and UI suites were also executed successfully on the maintainer's POCO F6, Android 16 / HyperOS OS3.0.303.0.WNPINXM.

## Findings and changes

| Finding | Effect | Change / evidence |
| --- | --- | --- |
| A repair reused the status snapshot captured before an earlier target was repaired | Later decisions could be based on stale status | Refresh the snapshot after repair; unknown or incomplete diagnostics stay unverified |
| A healthy sample cleared the attempt cooldown; unbound-but-not-crashed status was acted on immediately | Flapping or temporary connection changes could cause unnecessary rebinds | Keep the 60-second per-service cooldown across healthy readings; give a configured, non-crashed service 30 seconds to settle. Manual Check now can retry explicitly. Stateful regression coverage |
| Crash fields were read as one line; component-like strings inside labels could be treated as component IDs | Missed crash entries or misleading running status | Parse complete balanced sets, including multiline fields; distinguish label-only output from structured identifiers; use no label-only proof for duplicate or delimiter-bearing labels. Reproduced failures and regression tests |
| Diagnostics chose the device's foreground user rather than explicitly choosing Keeper's own user/profile | A same-named component in a parent profile could supply misleading status | Parse for the process's Android UID user range; missing own-user data stays unverified. Synthetic multi-user tests; physical profile/clone tests remain pending |
| A target installation or protection control could change between planning and writing | A stale planned action could run after a changed choice or restore access to a different installation | Recheck controls before starting and installation identity before restoration. An already-started rebind may finish restoring the original trusted installation; an in-flight repair does not restore a replacement installation. A later check applies the saved reinstall policy. Regression tests |
| Journal messages were bounded only by entry count; carriage returns and I/O failures were not fully handled | Oversized exports, misleading text, lost logs or aborted recovery checks | Cap messages and journal size; normalize line controls; replace the journal through a temporary file; tolerate journal I/O failures without preventing recovery. Bounded-export and failure-path tests |
| Share exported older entries that were not visible in Recent activity | Users could overlook part of the text being shared | Add a full export preview before the share chooser. No automatic upload |
| Custom controls lacked some roles/headings; Settings rebuilt its scroll position; keyboard insets were ignored | Harder navigation with assistive tools, large text or an open keyboard | Add button/heading/selected semantics, preserve scroll, account for the keyboard, and make Done dismiss it. Device UI assertions passed; dark/light and enlarged-text captures inspected |
| Paused/manual-check and notification text could be misleading | A requested check could do nothing while appearing to run | Explain paused state and require an explicit Resume and check; show remaining pause minutes and permission/pause status in the ongoing notification |
| Runtime inventory checks compared coordinates but did not enforce the reviewed artifact hashes | A changed artifact with the same version could go unnoticed | Verify debug/release runtime SHA-256 values against the inventory. A deliberately altered checksum was rejected, then the original inventory was restored and accepted |

The initial focused regression run reproduced seven failures before the corresponding fixes. Additional tests cover cooldown state, changed installation during rebind, stopped controls, profile separation and export size. See the checked-in test source for assertions and [device test instructions](../device-tests/README.md).

## Permission and component review

The settings-writing adapter accepts only `enabled_accessibility_services` strings and setting `accessibility_enabled` to `1`. The broader Android grant still permits much more; this code restriction does not narrow the OS permission itself. No repair deliberately writes the master switch to `0`.

The release merged manifest contains 22 components. Its externally accessible entries are:

| Component | Boundary |
| --- | --- |
| Main activity | Public launcher. Incoming extras do not select services, set permissions or export a log |
| Boot receiver | Handles only the protected system `BOOT_COMPLETED` action; consent still gates recovery |
| AndroidX WorkManager job service | Requires the system `BIND_JOB_SERVICE` permission |
| AndroidX diagnostics receiver | Requires `DUMP` |
| AndroidX profile installer receiver | Requires `DUMP` |

Keeper's recovery service, package-removal receiver, policy/About/log-preview activities and AndroidX initialization provider are not exported. Notification PendingIntents are immutable. Opening an app uses the installed service's package identity, not its display label. Notification details use private lock-screen visibility, subject to Android/user settings.

The packaged app has no `INTERNET` permission, telemetry SDK or first-party automatic upload path. Its only subprocess command is the fixed accessibility diagnostic command; app labels are not interpolated into shell commands. Diagnostics are size/time bounded, kept in memory and treated as unknown when incomplete. Private preferences and journal remain in app storage; Android backup is disabled. External browser/sharing actions are user initiated. See [permissions](PERMISSIONS.md) and [privacy](../PRIVACY.md).

## Dependencies, repository and build inputs

- Queried OSV's Maven batch API for the 54 exact runtime component versions: no advisories returned and no further result pages indicated. Database coverage is incomplete; absence of results does not establish safety. Query date: 18 November 2026.
- Reviewed the recorded runtime licenses/notices and packaged inventory. Both debug and unsigned-release builds retain the canonical offline policy and license assets. No new runtime dependency was introduced by these fixes.
- Scanned 18 commits and 183 unique blobs at the starting history for selected high-confidence private-key and provider-token patterns: no matches. Also found no tracked `misc/`, APKs, keystores or `local.properties`. This is a limited pattern scan, not proof that no sensitive information exists.
- Runtime artifact checksums are now enforced. Gradle plugin, POM and full toolchain integrity are not comprehensively locked by this check.
- The repository remains private. No release, tag or public publication was performed as part of this audit.

## Usability checks and limits

Eight primary text/background pairs in the current light/dark palettes meet a calculated 4.5:1 contrast ratio; the lowest measured pair is 5.40:1. This is a color-token calculation, not a complete pixel/accessibility certification. Inline links, disabled states, device overlays and every possible text arrangement are not covered by that number.

The executed isolated UI suite checked switch-only selection, search/empty state, button/heading semantics, keyboard layout, Settings return position, policy/license navigation, external-link intents, reviewed-log sharing, a light theme and a process-local 2× font configuration. Browser/share intents were intercepted, so no log was sent. Dark permission-guide, light Settings and enlarged-text Settings captures were inspected: text remained readable, controls did not overlap, and long sections remained scrollable. This was a process-local font test, not a change to the phone's system font setting. No human TalkBack or Switch Access session has been completed.

Additional physical fixture checks passed: ordinary update with retention off preserves the existing choice; default-off uninstall/reinstall clears it; opted-in same-signer reinstall retains it; a replacement signer does not inherit it; and a renamed service component starts off without the checker writing a new accessibility entry. The uninstall in the missed-event scenario occurred while the verification copy was force-stopped; a test helper restarted it afterward to handle HyperOS's fixture-install prompt. Signing tests used a disposable fixture key, not a production release key.

Revoked diagnostic and write-grant checks passed. External attempts to open the private policy activity or start the private recovery service were denied. Uninstalling the verification copy left the enabled-service list unchanged. The verification copy, its test APK and dummy service were then removed, with only any exact fixture entry eligible for cleanup. These controlled cases do not prove another app's internal features or every firmware's behavior.

Testing must avoid contaminating accessibility state: Android UI automation suppresses other services by default unless `FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES` is used. The new suite sets it explicitly. Connection changes observed during older generic hierarchy dumps are not independent proof of an app defect.

## Before a public beta

1. Extend testing through human assistive-tool use and community device reports; retain the distinction between recorded fixture checks and third-party app compatibility.
2. Establish reboot, longer idle/screen-off and multi-day behavior. Profiles/dual apps and other device/firmware versions remain unverified.
3. Arrange a usable private vulnerability-reporting route. The private-reporting endpoint was unavailable for the current private repository; no dedicated security inbox is promised.
4. Replace the inherited release workflow before use: it still has old branding/toolchain choices, unpinned action tags and tag-triggered publication logic. Do not treat it as a prepared release pipeline or push release tags during testing.
5. Complete release signing/key backup, checksum publication and artifact verification, then prepare sanitized media and beta release notes. No signed public release exists yet.

A process killed midway through a rebind cannot be guaranteed to execute cleanup. Firmware may block background work, diagnostics can change, and a bound service does not prove its features work. Preserve these limits in user-facing documentation.

Sources: [Android exported-component guidance](https://developer.android.com/privacy-and-security/risks/android-exported), [UI automation suppression flag](https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES), [AOSP user UID ranges](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/os/UserHandle.java), [OSV batch API](https://google.github.io/osv.dev/post-v1-querybatch/), [Android accessibility principles](https://developer.android.com/guide/topics/ui/accessibility/principles).
