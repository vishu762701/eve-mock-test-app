# EVE Telegram GPL production-release compliance gate

Reference-source availability: Telegram source is supplied for explicit source-exact comparisons/copying; its absence never blocks independently authored Eve UI or unrelated development. See `THIRD_PARTY_NOTICES.md` for archive identity. Actual distribution permission remains a separate requirement.

This is a production-distribution safeguard, not a restriction on development-stage redesign, testing or independently authored replacements. When a particular imported asset cannot be cleared, replace it with appropriately licensed original work, update notices and reevaluate the exact release dependency graph.

The app currently includes original static Telegram Android artwork from a reference archive whose root `LICENSE` contains GPL version 2. Whether each imported file has an "or later" grant compatible with EVE's intended GPLv3 distribution remains unverified. The repository's GPL-3 licensing intent and bundled notices are **not** by themselves proof that a production APK/AAB may be distributed lawfully.

`.github/workflows/release.yml` therefore fails closed on both version-tag pushes and manual release dispatch unless the repository-level Actions variable `EVE_GPL_RELEASE_APPROVED` is exactly `true`. Do not set this variable as a workaround for a failing build. This variable records an affirmative human review decision, **not** an automated legal certification.

Before an authorized maintainer sets the variable, review and record evidence for all of the following:

1. Exact provenance and per-file redistribution grants for every imported Telegram icon and any derived Telegram source code.
2. Combined-work compatibility of the chosen GPL license with all bundled SDKs, libraries, proprietary binaries, fonts and artwork; obtain qualified legal review where needed.
3. Complete corresponding source for the exact released build, reproducible build instructions, applicable notices/license texts and source-delivery method offered to recipients.
4. Exclusion of private signing keys, tokens, credentials and personal data from public corresponding-source deliverables.
5. Required brand/trademark separation, release version attribution, and final human approval of the distribution package.

Keep the variable unset or set to anything other than `true` until these checks are complete. Even after it is set, all existing signing, unit-test, lint and release-build gates remain mandatory. This protection does not gate internal FAST debug compilation or the mission-final native UI validation.

## Current engineering audit

See [Telegram GPL distribution audit (2026-10-24)](telegram-gpl-audit-2026-10-24.md) for the reviewed source paths, open release risks, and exact closure checklist. It confirms source integrity but **does not** grant production redistribution clearance. The release approval variable must remain unapproved until all P0 findings are resolved and the recipient/source-delivery package is independently reviewed.
