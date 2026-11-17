# Contributing

Thanks for helping make selected accessibility services more reliable. The project is preparing an experimental public beta; compatibility claims should stay tied to evidence.

## Bugs and compatibility

[Open a bug report](https://github.com/Soham-Rao/hyperos-accessibility-keeper/issues/new/choose) with model, Android/HyperOS version, Keeper version, steps and expected/actual behavior. Logs and app names are optional. See [Testing](docs/TESTING.md) for safe testing and redaction.

## Suggestions

Use the suggestion form and describe the problem first, then your idea. Mention who would benefit and any permission, battery or usability tradeoff. There is no need to write code to suggest an improvement.

## Code and documentation

1. Read the [developer guide](docs/DEVELOPMENT.md) and existing issues.
2. Keep changes focused. Discuss changes to permission scope, automatic enabling, release signing or compatibility promises before implementing them.
3. Preserve upstream MIT/copyright notices and credit. Do not add assets with unclear redistribution rights.
4. Run unit tests, lint and a debug build for code changes. Add meaningful regression coverage where possible, and state what was tested locally versus on a real device.
5. Explain the problem, resulting behavior, validation and limitations in your PR. Documentation and accessibility improvements are welcome too.

Do not commit keys, credentials, personal captures, raw system dumps, APKs, build outputs or machine-specific settings. `misc/` is ignored private working space.

## Safety and security reports

Do not publish credentials, exploit details or sensitive logs in a public issue. A dedicated private vulnerability-reporting route is part of beta preparation; until it is configured, request a private contact route without disclosing the vulnerability itself.

Be respectful in discussions. This is a small project with limited devices and CI resources; a report is useful even when the maintainer cannot reproduce it immediately. No guaranteed response time is promised.
