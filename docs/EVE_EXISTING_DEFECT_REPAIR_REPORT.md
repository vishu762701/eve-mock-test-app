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

Status: source repair and FAST VERIFIED; terminal FULL pending. Next action: inspect final implementation FULL run and APK checksum. Broader fidelity mission remains INCOMPLETE for the explicitly unavailable external evidence. Native source-parity and physical-device criteria remain individually BLOCKED/UNKNOWN even if CI succeeds.

Final report must record actual implementation SHA, FAST/FULL URLs, six job outcomes and downloaded APK SHA-256. Background execution is never promised.
