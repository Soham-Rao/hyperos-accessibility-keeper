# Changes from HyperOS Accessibility Guard

Accessibility Keeper is maintained by [Soham-Rao](https://github.com/Soham-Rao/hyperos-accessibility-keeper) and is based on [HyperOS Accessibility Guard by haku4130](https://github.com/haku4130/hyperos-accessibility-guard).

Starting upstream revision: [`959ce7cd924c35847426d0f8e730bf90bf25ba1b`](https://github.com/haku4130/hyperos-accessibility-guard/commit/959ce7cd924c35847426d0f8e730bf90bf25ba1b). Original copyright and MIT terms are preserved in [LICENSE](LICENSE); new modifications are also offered under MIT. Third-party libraries retain their own licenses and notices.

## Material changes in this fork

- Require a versioned safety acknowledgment before recovery starts, with offline permission, privacy, terms and security documents.
- Default target-app reinstall retention to off. When opted in, preserve only existing service choices with the same signing identity; detect missed removals using installation identity.

- Discover installed accessibility services rather than hardcode one target application. Keep separate services in one app independently selectable.
- Store explicit per-service protection choices; newly discovered services start unselected.
- Rebind only an affected selected service, preserve unrelated service entries, and reread the list before restoring the target. Do not deliberately turn the master switch off during a repair.
- Distinguish enabled settings, pending binding, bound state, crashed state and unavailable diagnostics.
- Allow an in-progress bind to settle. After an unsuccessful rebind, provide an Open app fallback rather than endlessly cycling the service. A later verified healthy check clears that state; an explicit manual check can retry.
- Replace the original UI with a searchable service list, protected filter, switch-only selection, tile options, activity, a quick menu, settings and appearance choices.
- Make relevant activity cards and notifications open the affected app directly after the user's action. Do not launch another app silently.
- Add local component metadata to recovery events while retaining compatibility with older journal entries.
- Provide Windows setup tooling, current build instructions and user/contributor documentation. Disable the upstream installer rather than configure the wrong package.
- Adapt CI to a cached, bounded, single Linux job with code-path filtering and opt-in temporary APK artifacts.
- Add fork attribution, offline license documents, a dependency inventory and a repository provenance record.

The package identity is distinct from the upstream app. Some internal source namespaces and historical investigation documents remain from upstream; retaining them is not a claim that all upstream device observations apply to this fork.

## Scope of claims

The initial device evidence is from one POCO F6 on HyperOS 3 / Android 16. Some apps still require opening to finish connecting. This document describes changes, not guaranteed compatibility or a completed security/legal audit. See [testing and limitations](docs/TESTING.md), [safety](docs/SAFETY.md), [third-party notices](THIRD_PARTY_NOTICES.md) and [provenance](docs/provenance/README.md).
