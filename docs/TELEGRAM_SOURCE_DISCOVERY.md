# Telegram Android source discovery

**Recorded:** 2026-11-12
**Status:** archive located, hashed, extracted and inspected. Android build/device
verification for the current EVE work is BLOCKED in this environment (no Java,
Gradle, Android SDK, adb or emulator). This document is discovery evidence, not a
claim of screen-level parity.

## Archive identity

| Property | Value |
| --- | --- |
| Device path | `/sdcard/Download/Telegrammaster.zip` |
| Archive name | `Telegrammaster.zip` |
| Size | `115,700,719` bytes (about 110 MiB; 441,045,194 uncompressed bytes) |
| SHA-256 | `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10` |
| Entries | `14,794` files (`unzip -l` total line) |
| Extraction root | `/tmp/eve-telegram-source/Telegram-master` |
| Extraction size | `460 MiB` |
| Java sources | `2,268` `.java` files in the extracted tree |
| Resource files | `5,266` files under `TMessagesProj/src/main/res` |
| Original archive handling | Read-only. The ZIP was listed, hashed and extracted; it was not modified, moved or deleted. The SHA-256 was rechecked after extraction and is unchanged. |

The archive has no `.git` directory. To verify revision identity directly, a
partial Git checkout was fetched into `/tmp/eve-telegram-git` with
`git fetch --depth 1 --filter=blob:none origin
3de37c8cfb039452be7398909461da6288babf15` and `HEAD` was set to that commit.
No working tree was checked out and no EVE repository or archive file was
modified. Sixteen representative source/resource files were hashed locally and
compared with `git show HEAD:<path>`; all matched, including `gradle.properties`,
`ImageUpdater.java`, `BottomSheet.java`, the three profile editors,
`CodepointsLengthInputFilter.java`, `AndroidUtilities.java`,
`BotWebViewVibrationEffect.java`, `ButtonBounce.java`, `ProfileActionsView.java`,
`ProfileGooeyView.java`, `NotchInfoUtils.java`, `ThemeColors.java`,
`msg_photos.webp` and `msg_delete.png`. The recorded 16-asset import manifest
also passes `verify_telegram_imports.py --upstream /tmp/eve-telegram-git`.

## Source version

| Property | Value |
| --- | --- |
| Application | Telegram Android (`TMessagesProj` + variant app modules) |
| Version name | `13.1.1` (`gradle.properties:APP_VERSION_NAME`) |
| Version code | `7237` (`gradle.properties:APP_VERSION_CODE`) |
| Upstream repository | `https://github.com/DrKLO/Telegram` |
| Revision | `3de37c8cfb039452be7398909461da6288babf15` |
| License | GNU GPL v2 or later. Root `LICENSE` is GPLv2; Java file headers state "GNU GPL v. 2 or later" and retain Telegram contributor copyright. |

The revision was independently checked against GitHub's immutable commit objects in
the earlier verified profile mission, which also recorded the root tree
`cc3c6ccf48e4167fdec764dfd04e765043f55093`. This archive's content agrees with that
pin: `gradle.properties` reports 13.1.1/7237 and the inspected component files match
the pinned Git blobs byte-for-byte as described above.
`EVE_MANIFEST.sha256` is **ABSENT**; no archive-wide manifest verification is
claimed.

## Pinned Git Blob SHA-1 Verification (10 Blobs)

The 10 Git blob SHA-1 values computed via `sha1("blob " + length + "\0" + bytes)` from `/storage/emulated/0/Download/Telegrammaster.zip`:

| Archive-relative path | Expected official Git blob SHA-1 | Verified local blob SHA-1 | Match |
|---|---|---|---|
| `README.md` | `f42b0063a7986cfa1242e9441fcce7e0e8c0de89` | `f42b0063a7986cfa1242e9441fcce7e0e8c0de89` | PASS |
| `gradle.properties` | `4a5efa191b482ab30bf34dbd85df2b201d6757a5` | `4a5efa191b482ab30bf34dbd85df2b201d6757a5` | PASS |
| `.gitmodules` | `88430c54b62eb74c2e5266a8ae2bf8d8453ea450` | `88430c54b62eb74c2e5266a8ae2bf8d8453ea450` | PASS |
| `LICENSE` | `d159169d1050894d3ea3b98e1c965c4058208fe1` | `d159169d1050894d3ea3b98e1c965c4058208fe1` | PASS |
| `TMessagesProj/build.gradle` | `f9a3639a17b128527a65a6a15a6bc17b4bd9620d` | `f9a3639a17b128527a65a6a15a6bc17b4bd9620d` | PASS |
| `TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java` | `4ed7e6491cefa3bc19611e1f7db2cd41d67e8f04` | `4ed7e6491cefa3bc19611e1f7db2cd41d67e8f04` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java` | `7805f9d6d3a216972a18f9e367ad7199f2624555` | `7805f9d6d3a216972a18f9e367ad7199f2624555` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/Components/RecyclerListView.java` | `e53a830224256333be38baa47dcb462b53b92e44` | `e53a830224256333be38baa47dcb462b53b92e44` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java` | `8d3b92d6a680224b9cc7b80f620b656f1184ee3a` | `8d3b92d6a680224b9cc7b80f620b656f1184ee3a` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenu.java` | `49c4c96c20a07e773f332aa158ada3c0807372fc` | `49c4c96c20a07e773f332aa158ada3c0807372fc` | PASS |

All 10 Git blobs match the official Git tree at commit `3de37c8cfb039452be7398909461da6288babf15`.

## GitHub Governance Verification

- **Branch Protection / Ruleset:** Activated Ruleset `id: 27115073` on `~DEFAULT_BRANCH` (`main`).
- **Enforced Rules:**
  - `pull_request`: required, `required_approving_review_count`: 0 (solo-owner unblocked), `required_review_thread_resolution`: true.
  - `required_status_checks`: context `"build"`, strict: true.
  - `non_fast_forward`: active (force pushes blocked).
  - `deletion`: active (branch deletion blocked).
  - `required_linear_history`: active.
  - `bypass_actors`: RepositoryRole 5 (Admin) with `bypass_mode: "always"` for emergency owner bypass.
- **Workflow Triggers:** `.github/workflows/build.yml` configured for `push`, `pull_request` on `main`, and manual `workflow_dispatch`.
- **API Readback:** Verified via `gh api repos/vishu762701/eve-mock-test-app/rulesets/27115073`.


## Relevant source roots

| Root | Contents used here |
| --- | --- |
| `TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java` | self-profile shell, photo/action wiring, overflow, collapse and avatar behavior |
| `TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileActionsView.java` | three-action renderer, ButtonBounce wiring, accessibility nodes |
| `TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileGooeyView.java` | full-window avatar notch/Gooey renderer |
| `TMessagesProj/src/main/java/org/telegram/ui/Components/ImageUpdater.java` | profile photo menu, system camera intent, gallery selection, temp file/path lifecycle |
| `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BottomSheet.java` | item sheet title/cell geometry, selector, destructive item tinting |
| `TMessagesProj/src/main/java/org/telegram/ui/ChangeUsernameActivity.java` | username rules, IME Done, unchanged-handle close, shake/haptic rejection |
| `TMessagesProj/src/main/java/org/telegram/ui/ChangeNameActivity.java` | display-name editor focus/keyboard and unchanged-value behavior |
| `TMessagesProj/src/main/java/org/telegram/ui/ChangeBioActivity.java` | bio IME Done, trailing-newline Done, codepoint filter, unchanged-value close |
| `TMessagesProj/src/main/java/org/telegram/ui/Components/CodepointsLengthInputFilter.java` | Unicode code-point length filter |
| `TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java` | `shakeViewSpring` spring constants |
| `TMessagesProj/src/main/java/org/telegram/messenger/BotWebViewVibrationEffect.java` | `APP_ERROR` vibration timing/amplitude pattern |
| `TMessagesProj/src/main/java/org/telegram/ui/QrShare/` | personal QR scene, picker, scanner controls and motion |
| `TMessagesProj/src/main/res/drawable-*dpi/` | source raster icon payloads used byte-for-byte where available |

## Integrity and limitations

- Extraction was verified by reading representative files and comparing source
  payloads with EVE copies by SHA-256 (see
  `docs/telegram-verbatim-imports.json`).
- The archive is a source snapshot, not a signed release. GPL/combined-work
  distribution obligations remain an open release gate; no production distribution
  clearance is implied.
- The current environment has no Java runtime, Gradle distribution, Android SDK,
  adb, emulator or KVM. Android compilation and runtime evidence for the changes in
  this session are therefore unavailable here; see
  `docs/TELEGRAM_EVE_IMPLEMENTATION_LOG.md`.
- Only profile/QR-related Telegram components are in scope. Telegram accounts,
  MTProto, chats, media services, telemetry and remote APIs are not used.
