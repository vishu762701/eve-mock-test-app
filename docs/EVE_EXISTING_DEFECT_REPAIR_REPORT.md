# Existing-defect stabilization acceptance record

Scope: the owner's attached EVE_CODEX_EXISTING_DEFECTS_ONLY_REPAIR.txt; existing behavior only, no production deployment, no changes to permissions/scoring or feature backlogs.

Baseline: `303119f2f2059cc94161240d5fa7cd41faaa6d0e`. Exact baseline FAST: https://github.com/vishu762701/eve-mock-test-app/actions/runs/41182187546 (success). Historical FULL: https://github.com/vishu762701/eve-mock-test-app/actions/runs/41179432158 at `a6e0345bb880ecc4975c5f868acced55c0540b30`; it does not verify this repair.

## Discrepancy ledger

| Finding | Evidence and disposition |
|---|---|
| P2 Category/Bio can confirm another account's readback after account switching | VERIFIED source defect: `ProfileActivity.selectCategory` and `ChangeBioActivity.saveBio` checked response text without initiator identity. Both now use `ProfileSaveConfirmation.matches`, requiring a nonblank unchanged UID, successful readback and matching value. Regression cases cover switched/signed-out/missing identities, failures, missing text and comparison semantics. No API/schema/feature changes. |
| Historical missing-source STOP instruction | VERIFIED in `docs/telegram-parity-sweep.md`; corrected to the live scoped evidence rule while preserving historical tables/hashes. |
| Profile fields / birthday / category persistence and Poll delete label | Existing repairs present on current main; not duplicated. Runtime usability on owner's physical device remains UNKNOWN. |
| Connected Profile renderer / drawer / shared popup parity | SOURCE BLOCKED: no original Telegram archive or matched upstream runtime frames attached in this environment. Existing documented ADAPTED integrations remain; source differences alone are not defects. |
| Bio/presence activation | RELEASE BLOCKED: staging code/migrations exist. Production release remains manual and was not dispatched. |
| Installed device APK identity, cutouts, high refresh motion, TalkBack and physical performance | UNKNOWN: no attached device/adb or trace evidence. Screenshot counts alone cannot establish parity. |
| Earlier broad-audit payment and Firebase leads | Outside this narrower repair batch; no staged payment test, permission edits, new monetization work or deployments included. |

## Verification and exact continuation

Local: documentation verifier and host Python suite pass; `git diff --check` passes. Android SDK/Gradle/device unavailable locally; JVM execution, Android compilation and native acceptance require exact-SHA remote CI. No local Android execution claimed.

Implementation: `78a77e11638bfd6532b063699d5b8af9612efbc7`; forward test integration repair: `396a84c4846864f3b197f1c5bc5ed9afc5f1d6fb`. Initial FAST https://github.com/vishu762701/eve-mock-test-app/actions/runs/41183525890 failed because two source tests expected the replaced comparison expressions (600 tests, 2 failures). Both now require the real shared confirmation guard and initiator UID; behavioral assertions were retained.

Repaired exact-SHA FAST: https://github.com/vishu762701/eve-mock-test-app/actions/runs/41183587590 — SUCCESS (Android build/JVM/test-source compilation, backend, Firebase). Final boundary regression also checks UID case sensitivity independently of category case folding.

Status: scoped source repair, FAST, terminal FULL and APK integrity VERIFIED. Broader Telegram fidelity remains source/device BLOCKED; this is not whole-app parity acceptance. Broader fidelity mission remains INCOMPLETE for the explicitly unavailable external evidence. Native source-parity and physical-device criteria remain individually BLOCKED/UNKNOWN even if CI succeeds.

## Final implementation and artifact evidence

Final executable/test SHA: `fe32756e00a7fb1cfebea27510f73399dce7e01e`. FULL: https://github.com/vishu762701/eve-mock-test-app/actions/runs/41183928351 — all six jobs SUCCESS. Its Android job also passed FAST compilation/JVM/test-source checks.

| Job | ID | Result |
|---|---|---|
| Android build/lint/JVM and test compilation | 124486844826 | SUCCESS |
| backend | 124486844695 | SUCCESS |
| firebase-functions | 124486844927 | SUCCESS |
| Native UI (gesture) | 124487904549 | SUCCESS |
| Native UI (three-button) | 124487904553 | SUCCESS |
| Publish verified debug APK | 124489291139 | SUCCESS |

Both native logs report 172 EVE screenshots, zero missing required theme pairs, zero warnings and ten geometry review candidates. Candidates are not demonstrated bugs. These are emulator results, not physical-device or matched Telegram reference comparisons.

Artifact: [Eve-debug-apk 13036891052](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41183928351/artifacts/13036891052). Downloaded ZIP SHA-256 `6a78b4b6206e65304e8219ecb1d710875d71b50c4086c66166f8a0294b5198be` matches GitHub's artifact digest. Extracted APK: 16,589,207 bytes; SHA-256 `3a4ab5f2a364d8ec84639ddffc77e34209dc030a7a9beee38319046ae462862f`, independently computed and matched to publication's EXPECTED_SHA256. No installed-device identity claim.

Changed production files: `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt:379`, `ChangeBioActivity.kt:52`, new shared `ProfileSaveConfirmation.kt:4`. Tests: `ProfileSaveConfirmationTest.kt`, `ProfileCategorySourceContractTest.kt`, `ProfileBioFeatureGateTest.kt`. Starter and historical parity introduction were corrected; no new product behavior, permission change, schema or production dispatch.

## G7 external-evidence checkpoint

Executable repair is complete at the final SHA above; subsequent acceptance-record commits change documentation only. Remaining P1 fidelity evidence: original Telegram renderer/dependency source and matched frames absent from the supplied files. Physical cutout/high-refresh/installed-device checks UNKNOWN. Bio/presence production activation RELEASE BLOCKED by the explicit no-deployment scope. Next permitted action is verify the genuine source/reference artifacts when available and triage only demonstrated existing integration defects; do not replay completed fixes or deploy staged contracts. Background execution is never promised.
