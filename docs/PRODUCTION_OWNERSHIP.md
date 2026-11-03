# Production ownership registry

This is a compact map of the **existing** runtime ownership contracts at main `f0c923994cfed1deb2026e0b09d3d64bb7982ed0`, based on inspected Manifest, callers and repository code. It is **not** a declaration of full Telegram/device parity, nor a ban on helper classes. Update the affected row and its regression contract together when an authorized migration deliberately changes an owner.

| Responsibility | Authoritative production owner | Producer to consumer / legitimate collaborators |
| --- | --- | --- |
| App launch | `ui/login/LoginActivity.kt` | Manifest MAIN/LAUNCHER; authenticated routing starts Home |
| Home selected category | `ui/home/HomeViewModel.kt` `_selectedCategory` | `MainActivity.kt` reads live StateFlow; `HomeCategorySelection.kt` resolves values; `HomeCategoryChipGroup.kt` renders touch/selection |
| Home search state | `ui/home/MainActivity.kt` | `HomeSearchHistory.kt` partitions submitted history by Firebase UID; `HomeSearchText.kt` is a helper |
| Home overflow | `ui/home/TelegramMenuPopup.kt` | `MainActivity.kt` constructs seven callbacks; source motion/background/gap classes are renderer collaborators |
| Shared app dialogs | `designsystem/TelegramNativeAlertDialog.kt` | App-owned callers; source-background and scrim atoms are adapters, not rival dialog business owners |
| Drawer navigation | `ui/home/MainActivity.kt` | `HomeDrawerActions.kt` maps EVE destinations; shared theme/layout code renders the drawer |
| Profile identity/state | `ui/profile/ProfileActivity.kt`, `ProfileSession.kt` / `ConfirmedProfileStore.kt` | Profile editor Activities write through authenticated API; cached profile is owner-scoped and server-confirmed |
| Profile avatar rendering | `ui/profile/ProfileGooeyView.kt` | `ProfileActivity.kt` hosts renderer; expansion and trajectory classes are motion collaborators |
| Public personal QR | `ui/profile/UsernameCardActivity.kt` | `ProfileActivity.showPersonalQr` → `ProfileQrPayload` → one `TelegramQrSceneView.kt` → source-derived QR drawing primitives |
| QR scanner | `ui/profile/EveQrLiveScannerActivity.kt` | Viewer launches one non-exported scanner; CameraX session, torch, frame and decode helpers are separate responsibilities |
| QR emoji picker | `ui/profile/UsernameCardActivity.kt` | `QrPickerMorphDialog` → `QrEmojiPickerGrid` → `QrAnimationAssets` and native drawable; scene owns selected six-item pattern |
| Notifications and read state | `util/NotificationStore.kt`; acknowledgment caller `ui/notifications/NotificationsActivity.kt` | Home `MainActivity.kt` observes unread and navigates; only visible inbox acknowledges, including newly fetched posts |
| Admin access | `util/AdminAccessGate.kt` and backend authentication/middleware | One active authentication listener and authorization request per protected root; repeat gates invalidate the previous lease; backend independently authorizes each endpoint |
| Admin user directory | `ui/admin/ManageUsersActivity.kt` and `data/repository/AdminRepository.kt` | Fresh `AdminAccessGate` permits rows only for current UID; repository validates page identities/metadata, matching user-status ack and attempt-list responses; Worker `src/routes/admin.ts` remains authoritative |
| Poll status | `ui/admin/ManagePollsActivity.kt` and `data/repository/PollRepository.kt` | Backend `src/routes/polls.ts` owns authorized, audited mutations; source-derived UI controls are adapters |
| Quiz attempt lifecycle | `ui/test/TestActivity.kt` / `TestViewModel.kt` | Activity host and existing ViewModel check the original authenticated UID before/after suspended server reads and before publishing state; `SubmissionRetryPolicy` is the shared owner matcher, `TestSessionStore` owns local drafts, authenticated Worker `src/routes/attempts.ts` owns scoring/deduplication |
| Maintenance presentation | `ui/home/MainActivity.kt` and `util/AppConfigManager.kt` | Server config drives Home gate; no independent theme-toggle authority to set the maintenance flag |
| Day/Night state | `util/ThemeManager.kt`, `util/ThemeSwitchAnimator.kt`, `theme/TelegramThemeEngine.kt` | Source-derived palette roles feed host Activities/dialogs; Home drawer Night black is an approved override |
| Payment-specific QR | `util/QrCodeGenerator.kt` | Payment checkout's own payload renderer is deliberately **not** a duplicate of public username QR |

## Regression enforcement and consolidation

`.github/scripts/tests/test_production_ownership.py` executes under the pre-existing FAST host Python unit-test discovery. It checks one Manifest registration per Activity, one launcher, one registered EVE public username route, protected scanner export settings, the actual Profile → viewer → scanner call chain, inbox-only unread acknowledgment, and Home's ViewModel-selected category contract.

These tests protect **known concrete failure modes**, not class names generally. They deliberately allow multiple QR render primitives, different QR payload types, source adapters, test fixtures and separate permission/visual controllers. In particular, the existing payment QR generator must not be deleted or redirected into EVE's username identity flow.

Before claiming a duplicate, trace both **production** callers, compare responsibilities and side effects, choose the survivor, add a regression, migrate safely, then remove only dead code. A shared name, similar shape, or dormant compatibility helper alone is insufficient proof. No destructive duplicate deletion is authorized by this registry.

## Unaccepted evidence

This source-level map does **not** establish that every Admin, Home, student or native call path works on hardware. Complete device runs, QR screenshot/physics matches, Telegram upstream revision/cross-dependency parity and legal release approval remain separate requirements. Failure to find a competing owner in this inspected set is **not** proof that there are no duplicates anywhere in the repository.
