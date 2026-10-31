# EVE personal Bio Android editor — source/staging map, 2026-10-31

**STATUS: ANDROID IMPLEMENTED, BACKEND STAGED, PRODUCTION NOT DEPLOYED.**

EVE user requirement: edit a personal Bio in Telegram-style Profile while keeping the unrelated Profile **About navigation** permanently removed.

## Implementation
- `app/src/main/res/layout/activity_profile.xml`: `cellBio`, `tvBioValue`, `tvBioLabel` inserted after Username in the Personal Information card. The cell is **initially disabled in XML**.
- `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt`: only authenticated `GET /api/users/profile` containing `bio` (including empty string) enables the Bio editor. Missing `bio` from an older Worker keeps the row disabled and clearly states `Bio unavailable until server update`. The existing About navigation row is not re-added.
- `app/src/main/java/com/eve/app/ui/profile/ChangeBioActivity.kt`: existing EVE Profile editor hierarchy, optional multiline Unicode Bio, 160 UTF-16-unit limit, Save/Back. PUT success alone is insufficient: authenticated GET must equal the canonical trimmed value before `RESULT_OK` and page finish.
- `app/src/main/res/layout/activity_change_bio.xml`: theme-appropriate typography roles, minimum 48dp toolbar targets, multiline editor and count, no literal textSize.
- `app/src/main/java/com/eve/app/data/remote/EveApiService.kt`: nullable `bio` deserialization makes old API backward compatible; missing is not mistaken for empty/fully supported.
- `app/src/main/AndroidManifest.xml`: non-exported `ChangeBioActivity`.

## Verification
- JVM `ProfileBioFeatureGateTest`: contract gate, readback, validation, removed About, no hardcoded text sizes.
- AndroidTest `ProfileBioNativeTest`: Day/Night inflation, disabled until server support and 160-character EditText cap. FAST compiles but **does not execute** the native test.
- Backend migration and authenticated tests are in commit `eba39574e3ce1ed839d4bdabfbdd378dc8ad4006`, staged behind manual-only worker deploy gate `896f3d995ba9235f6ae71dfa58ca708ea43866a3`.

**Original Telegram exactness:** The editor follows existing source-derived Telegram-style EVE page hierarchy but is **ADAPTED**, not a verbatim dependency-complete Telegram EditBio component. Personal Bio APIs are EVE-specific; no claim of original Telegram profile server behavior. No auth, identity, exam-scoring or other navigation changes.

**Remaining:** Production manual Worker approval and D1 migration, authenticated cross-device GET/PUT, real online presence contract, exact ProfileActionsView renderer/physical motion, licensing of requested official Apple bell, app-wide source exactness and terminal six-job FULL with APK digest. Do not mark mission accepted or claim Bio deployed until these gates pass.
