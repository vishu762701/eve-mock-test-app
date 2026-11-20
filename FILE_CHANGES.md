# FILE_CHANGES.md

- **Starting Git Commit SHA**: `258cfb3b10a666601e74fc21fa14c7429b6beec4` (`eve-mock-test-app-main.zip` baseline)
- **Code Commit SHA (prior to handover docs)**: `6beafaab55c51a0a6d5c6e48a59529e833e87135`
- **Generated (UTC)**: `2026-11-20T03:19:50Z`
- **Summary**: 1 added (+ handover files), 22 modified, 0 deleted

## Added Files

| File | Lines (+) | SHA-256 (prefix) |
|---|---:|---|
| `TELEGRAM_PARITY_LEDGER.md` | +81 | `a44c34c1aa22` |
| `FILE_CHANGES.md` | handover | recorded in commit |
| `PATCH_MANIFEST.json` | handover | recorded in commit |
| `BUILD_REPORT.md` | handover | recorded in commit |

## Modified Files

| File | Diff | SHA-256 (prefix) |
|---|---|---|
| `app/src/main/java/com/eve/app/ui/admin/AdminsAndRightsActivity.kt` | +167 / -26 | `b2db45646a17` |
| `app/src/main/java/com/eve/app/ui/admin/ApiUsageActivity.kt` | +180 / -53 | `3c2b9c9e1dd3` |
| `app/src/main/java/com/eve/app/ui/admin/FeedbackMessagesActivity.kt` | +153 / -64 | `37e22262f296` |
| `app/src/main/java/com/eve/app/ui/admin/GeneratedTestsActivity.kt` | +157 / -78 | `b384b2a5feb0` |
| `app/src/main/java/com/eve/app/ui/admin/ManageExistingExamsActivity.kt` | +165 / -71 | `58e8c0408a47` |
| `app/src/main/java/com/eve/app/ui/admin/ManagePollsActivity.kt` | +184 / -64 | `c19d9865d1ca` |
| `app/src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt` | +206 / -84 | `a55bcc4954b1` |
| `app/src/main/java/com/eve/app/ui/admin/emoji/EmojiGridAdapter.kt` | +28 / -35 | `9e7c1d9c8bbb` |
| `app/src/main/java/com/eve/app/ui/admin/emoji/EmojiRenderer.kt` | +172 / -38 | `65d5f2de8a59` |
| `app/src/main/java/com/eve/app/ui/admin/emoji/EmojiStudioActivity.kt` | +356 / -153 | `9f9f24566fac` |
| `app/src/main/java/com/eve/app/ui/home/ExamTestsActivity.kt` | +277 / -90 | `ce4b2383d96b` |
| `app/src/main/java/com/eve/app/ui/mistakes/MistakesActivity.kt` | +196 / -68 | `4318ed4c18bc` |
| `app/src/main/java/com/eve/app/ui/notifications/NotificationsActivity.kt` | +213 / -58 | `f027f8381846` |
| `app/src/main/java/com/eve/app/ui/practice/PracticeActivity.kt` | +252 / -76 | `60c7c50df0f7` |
| `app/src/main/java/com/eve/app/ui/premium/PaymentCheckoutActivity.kt` | +260 / -73 | `1f480b797dff` |
| `app/src/main/java/com/eve/app/ui/premium/PremiumActivity.kt` | +204 / -61 | `e1ba9ba320b5` |
| `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt` | +4 / -4 | `6b5e09858fc2` |
| `app/src/main/java/com/eve/app/ui/pyq/PyqActivity.kt` | +285 / -134 | `1a1528db6775` |
| `app/src/main/java/com/eve/app/ui/result/ResultDetailActivity.kt` | +212 / -92 | `cfbd5b05d229` |
| `app/src/main/java/com/eve/app/ui/syllabus/SyllabusActivity.kt` | +272 / -87 | `6d1e35c5a06a` |
| `app/src/main/java/org/telegram/ui/ActionBar/Theme.java` | +1 / -4 | `5635ff10ac95` |
| `docs/port-manifest.json` | +4 / -4 | `bc1759999af3` |

## Deleted Files

None (`0` files deleted). Legacy `activity_*.xml` layouts for migrated screens are still directly referenced by existing test suites (`LockedLayoutInflationTest`, `StudentUiRenderingTest`, `UiBehaviorRegressionTest`, `Ios26DesignFidelityTest`, `AdminRefreshCompatibilityTest`, etc.) and were intentionally retained to avoid breaking CI test compilation without a local Gradle run.

## Integration addendum
- Actual GitHub main base used for this integration: 22667c99394ad2163f58ad1dbc0e442c2b3bf47a
- All 22 replacement file blobs were verified byte-for-byte against that main base before applying.
- No files were deleted by this patch.
- The source-reported commit identifiers in the original handover were not resolvable as GitHub commits. See PATCH_MANIFEST.json for the corrected provenance fields.
