# EVE Telegram GPL Distribution Audit — 2026-10-24

**Status: SOURCE-LEVEL REVIEW COMPLETE; PRODUCTION RELEASE NOT CLEARED.**

This is an engineering license/provenance risk assessment, not a qualified legal opinion or permission to release. Never set `EVE_GPL_RELEASE_APPROVED=true` from this report alone. Preserve the release fail-closed gate in `.github/workflows/release.yml`.

## Scope and pinned evidence

- Repository: `vishu762701/eve-mock-test-app`, baseline `6b33306548684d40a0d7841f7e4e01ce2275b718`. Inspect the actual release commit again before distribution.
- Archive verification: [GitHub Actions run 40091599695](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40091599695), 6 ZIP files, UI/core manifest 7,575 records, full manifest 13,976 records, 7 exclusions absent, 32 imported icon identifiers and 254 file hash comparisons, zero observed mismatches.
- Exact SHA-256 for all 6 repacked archives and the earlier complete original archive: `THIRD_PARTY_NOTICES.md`.
- Reviewed repository sources: root `LICENSE`, `README.md`, `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts`, `app/src/main/java/com/eve/app/designsystem/ButtonBounce.kt`, `app/src/main/java/com/eve/app/util/ThemeManager.kt`, `app/src/main/assets/licenses/`, `docs/telegram-icon-source-registry.tsv`, `docs/telegram-release-compliance-gate.md`, `.github/workflows/release.yml`, `backend/package.json`, `functions/package.json`.
- The earlier separately supplied complete Telegram ZIP's root `LICENSE` contains GNU GPL version 2, June 1991; its `TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java` includes a "GNU GPL v. 2 or later" header. The repacked in-repository ZIP verifier independently identifies the extracted GPL-2 license text. The mere presence of license text is **not** proof that every bundled file has the same version grant.
- Source archives are verification-only references; they are **not** registered as EVE Gradle modules and are **not** corresponding source automatically supplied alongside any future released APK/AAB.

## Findings, ranked by impact

### P0 — Distribution combined-work compatibility: NOT CLEARED

`app/build.gradle.kts` declares Firebase Auth, Firestore, Functions, Messaging, Analytics, Crashlytics, plus `com.google.android.gms:play-services-auth`; the Google Services and Crashlytics Gradle plugins are also configured. These may add SDK binaries to the installed application. Exact artifacts, transitive licenses, distribution terms, and any relevant GPL-compatible exceptions have **not** been resolved for a pinned release build. The project cannot assume that naming its own code GPL-3.0-or-later makes a combined distributable lawful.

Required: produce the **resolved releaseRuntimeClasspath** dependency/software-bill-of-materials inventory for the candidate release, obtain the actual redistribution terms for each included binary, determine which are linked/bundled, assess any exception, and obtain appropriate legal/rights-holder review. Do not make a blanket assertion that every Firebase SDK is proprietary or that any particular linking arrangement is automatically prohibited; review the actual binary and terms.

Reference: [GNU GPL FAQ: GPL-incompatible libraries and system libraries](https://www.gnu.org/licenses/gpl-faq.en.html) and [F-Droid inclusion rules](https://f-droid.org/en/docs/Inclusion_Policy/) (different policy from legal redistribution rules).

### P0 — Per-file Telegram grants and corresponding source: NOT CLEARED

EVE carries original Telegram WebP artworks (`docs/telegram-icon-source-registry.tsv`; checksums checked), a modified Telegram-inspired Kotlin `ButtonBounce` port, and a Telegram-derived circular reveal implementation described in `THIRD_PARTY_NOTICES.md`. The archive license is GPL version 2; original Java files such as `BuildVars.java` explicitly state "version 2 or later", but **do not infer the option for all assets or every imported/derived file** from that one header. The `GPL-3.0-or-later` declaration in `README.md` and the GPL v3 root license do not override third-party terms.

Required: enumerate every distributed Telegram-derived item and its copyright holder, original exact path/commit/hash, item-specific license notice/grant, modified-file/change notices, and GPLv3 compatibility; get clarification or substitute properly licensed art only if a grant cannot be established. Independently archive the exact corresponding source, build/install scripts, legal notices and delivery method for every distributed version.

Reference: [GNU GPL FAQ: GPLv2-only versus GPLv2-or-later / GPLv3](https://www.gnu.org/licenses/gpl-faq.en.html).

### P1 — Actual binary and source-delivery package: NOT TESTED

The repository contains root GPL v3 text, a bundled Telegram GPL v2 text, `THIRD_PARTY_NOTICES.md`, plus OFL/Apache/MIT license files in `app/src/main/assets/licenses/`. Availability in a Git repository alone does not demonstrate that **recipients of the exact APK/AAB** receive sufficient notices/source-installation information or the right deliverable for the exact binary revision. No exact signed release candidate, merged manifest, dependency list, APK contents, reproducible build instructions/source archive, or recipient-facing source offer was audited.

Required: build candidate with real release signing, inspect the output APK/AAB, tie binary digest to commit/dependency tree, verify notices displayed/distributed, prepare corresponding-source bundle and recipient access, and log maintainer signoff. Keep release distribution disabled until evidence exists.

### P1 — Checked-in debug signing identity: MITIGATION NEEDED

`app/eve-debug.keystore` is tracked and `app/build.gradle.kts` explicitly configures a fixed debug signer. This is **not** the production release signing key; `release` explicitly requires separate environment credentials. Nevertheless, the checked-in debug private key enables others to sign binaries with the same debug identity. Existing Google Sign-In and update behavior may depend on its SHA-1. Do not remove or replace this file silently.

Required: make an intentional debug signer/Google OAuth migration plan, revise CI/test fixtures and authorized debug signing expectations, then remove any unnecessary public private-key material. Never commit production release keys.

### P1 — Unresolved transitive and asset provenance

Bundled static fonts include Inter, Noto Sans, Poppins and Telegram Roboto Medium. License notices found: `Inter-OFL.txt`, `NotoSans-OFL.txt`, `Poppins-OFL.txt`, `Roboto-APACHE-2.0.txt`, `Roboto-Medium-NOTICE.txt`. These notices are positive evidence, **not** an independently verified binary-to-upstream license match for every density/weight. App dependencies also include AndroidX, Material, Retrofit/OkHttp, Gson, Coil, Lottie and BlurView, for which a resolved release artifact-and-license list remains outstanding. Gradle build plugins, backend and Firebase Functions have separate licensing/deployment considerations; server dependencies do not automatically ship in an Android APK.

Required: collect release artifact dependency metadata/SBOM and license evidence for all included runtime components and assets; verify attribution texts against exact revisions, and record any exclusions and independent server deployments.

## Controls reviewed and decision

| Control | Observed evidence | Status |
|---|---|---|
| ZIP/manifest integrity and imported icon hashes | Run 40091599695 | PASS — integrity only |
| EVE root license | `LICENSE` GNU GPL v3 text; `README.md` GPL-3.0-or-later statement | FOUND; per-file grants still required |
| Telegram license texts | Source ZIP and bundled `app/src/main/assets/licenses/Telegram-GPL2.txt` | FOUND; individual grants unresolved |
| Release approval | `.github/workflows/release.yml` rejects release unless `EVE_GPL_RELEASE_APPROVED` is `true` | GUARD PRESENT; variable's configured value UNKNOWN |
| Release signing | `app/build.gradle.kts` requires non-debug release credentials | GUARD PRESENT |
| Release runtime SBOM + combined-work terms | Not collected for exact release artifact | NOT FOUND |
| Rights-holder/legal clearance | No attributable approval evidence | NOT FOUND |
| Source package for recipient matching release digest | Not verified | NOT FOUND |

**Decision:** Continue normal development and source-backed Telegram UI engineering, preserving upstream notices and doing per-component license checks. Do **not** mark a production APK/AAB as license-cleared or bypass the existing release gate. This audit documents blockers; it does not assert that use of Telegram code in the development repository is prohibited.

## Pre-release closure checklist

- [ ] Rights-holder grant/attribution reviewed for each redistributed Telegram file and derivative.
- [ ] Actual release dependency graph + license inventory retained with binary digest.
- [ ] Third-party proprietary SDK redistribution/GPL compatibility disposition recorded.
- [ ] Source/installation scripts, notices and recipient delivery route prepared and tested.
- [ ] Debug signer policy reviewed separately; production key remains private.
- [ ] Qualified human/legal signoff documents exact release, then and only then update release gate approval variable.
