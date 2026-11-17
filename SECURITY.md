# Security and responsible reporting

Experimental beta · updated 18 November 2026

This project has not received an independent security audit. Device coverage is limited; compatibility claims must be tied to recorded tests. Powerful grants and automatically restored third-party accessibility access carry risk. Only protect apps you trust.

## Sensitive reports

Do not publish exploit details, secrets or unredacted logs in an issue. Check the repository's Security tab for a private reporting option. If one is unavailable, open a minimal issue requesting a private contact method without including the vulnerability or personal data. The maintainer must arrange a private channel before you share details. No dedicated security inbox, response SLA or bounty is currently promised.

Project: https://github.com/Soham-Rao/hyperos-accessibility-keeper

## Useful non-sensitive details

Include Keeper version, device model, Android and firmware versions, steps, expected and observed behavior, and whether the issue happens with a single selected service. Share only the minimum evidence needed. Do not post full system dumps, device serial numbers, account information, signing keys or tokens.

## Reduce exposure

Download only artifacts linked by this repository and verify release signatures/checksums when signed releases are available. There is currently no published signed beta release from this fork. Do not treat CI debug artifacts as stable releases.

Turn off protection before disabling a target service. Revoke WRITE_SECURE_SETTINGS to prevent further settings repair, revoke DUMP to remove diagnostic access, or uninstall Keeper. Uninstalling Keeper leaves other apps' accessibility settings to Android and your own choices. Refer to docs/INSTALL.md for commands.

The repository retains upstream license and source history. Security work must preserve those notices. Policies and warnings reduce surprises; they do not constitute a completed security audit or guarantee that software is safe.
