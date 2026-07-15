# Code signing policy

Windows release artifacts are built from the public repository and are intended to be signed only after their source revision and build origin have been verified.

## Current status

The Windows v1.0.0 files are currently unsigned. Their SHA-256 hashes are published in `SHA256SUMS-Windows.txt`.

After acceptance into the SignPath Foundation open-source program, the repository and release pages will include the required statement:

> Free code signing provided by SignPath.io, certificate by SignPath Foundation.

## Roles

- Committers and reviewers: repository maintainers with write access.
- Approvers: repository owner(s).
- Submitter: the trusted GitHub Actions release workflow; interactive uploads are not used for normal releases.

## Release signing rules

1. Signing requests must originate from the public repository's trusted release workflow.
2. Release signing is limited to version tags and requires maintainer approval.
3. Product name, version, architecture, and source revision must match the release metadata.
4. Only binaries built from this project's source and build scripts may be signed.
5. All maintainers must enable multi-factor authentication on repository and signing accounts.

See [PRIVACY.md](PRIVACY.md), [SECURITY.md](SECURITY.md), and the [MIT License](LICENSE).
