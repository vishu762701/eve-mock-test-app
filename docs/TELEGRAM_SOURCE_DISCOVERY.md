# Telegram Android source discovery

**Recorded:** 2026-11-12
**Status:** archive located, hashed, extracted and inspected. Android build/device
verification for the current EVE work is BLOCKED in this environment (no Java,
Gradle, Android SDK, adb or emulator). This document is discovery evidence, not a
claim of screen-level parity.

## Current owner-supplied authoritative archive

| Property | Verified value |
| --- | --- |
| Uploaded archive | `Telegram-Android-master (1).zip` |
| ZIP SHA-256 | `768df2b468c589bc1b44e19cc023a8f0a4def1f4033d93c800d71016908202e4` |
| Compressed size | `116,850,881` bytes |
| Archive root | `Telegram-Android-master/` |
| File entries | `14,160` |
| Total entries, including directories | `14,886` |
| Uncompressed file payload | `443,628,854` bytes |
| `.git` directory | Absent; archive identity must be checked by Git blob hashes |

This archive is the read-only working snapshot for Telegram screen ports. It is not to be edited in place or committed into EVE.

## Official source revision and app version

| Property | Verified value |
| --- | --- |
| Official repository | `https://github.com/TelegramMessenger/Telegram-Android` |
| Branch | `master` |
| Pinned commit | `21f7d109da6ab3506c9e9577f98c6a38771ab335` |
| Commit message | `update to 13.2.2 (7303)` |
| Commit date | `2026-11-12T02:43:03Z` |
| `APP_VERSION_NAME` | `13.2.2` |
| `APP_VERSION_CODE` | `7303` |
| License file | Root `LICENSE`: GPL-2.0; preserve individual file headers and third-party notices |

The official GitHub `master` head was checked on 2026-11-21 and resolved to the same pinned commit. The GitHub Releases page still has an older tagged release entry; do not mistake the latest release tag for the current source branch head. The source-of-truth pin is the immutable commit above, not a moving branch name or a locally remembered version.

## Pinned Git blob verification (19 files)

For each path below, the Git blob SHA-1 calculated from the uploaded ZIP matches the blob SHA returned by the official repository at the pinned commit.

| Archive-relative path | Official Git blob SHA-1 | Result |
| --- | --- | --- |
| `.gitmodules` | `2a8c3dfa4f71be61f27e66648cddf1df5dd63fa4` | PASS |
| `LICENSE` | `d159169d1050894d3ea3b98e1c965c4058208fe1` | PASS |
| `README.md` | `f42b0063a7986cfa1242e9441fcce7e0e8c0de89` | PASS |
| `gradle.properties` | `17bb1dca2c58cc29eef16dd4ec48c3ec17ac2d19` | PASS |
| `TMessagesProj/build.gradle` | `4450811549815776d5832811e363c33f2638d9f1` | PASS |
| `TMessagesProj/src/main/AndroidManifest.xml` | `99f64adbf76f4501355e463489df4963ce8bd55b` | PASS |
| `TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java` | `ca821813f7de19064ee4510e61ed438d1566b5b4` | PASS |
| `TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java` | `d59b29a66c618169c229d2f16f847cb2d5a2300b` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java` | `e2b917be30918744a2c1c8a09c92cc60758419b9` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java` | `9656ebaf5c40097e8f42de2f224f4897d8ccda9c` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrShareActivity.java` | `34892736506aa6bc5b15f6e9f0175a1330df9da8` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrShareView.java` | `50bc0a5c64da4cb6a471dff35ba1fda1ded8be32` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrCodeLayer.java` | `703de92bb9158a7e936603ee02c6776f379213bd` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrRender.java` | `f9d47be54503024733f7868a6add2d32b936f89a` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java` | `7fc7292d12a05631ecf5af68fb2d00c368c37adc` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java` | `b8c83792e71f4d1acdecbf9f1227d476aa28917b` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenu.java` | `49c4c96c20a07e773f332aa158ada3c0807372fc` | PASS |
| `TMessagesProj/src/main/java/org/telegram/ui/Components/RecyclerListView.java` | `57293d468ded3d1a1bd7780d9431d8825e3ca8bf` | PASS |
| `TMessagesProj/src/main/res/values/strings.xml` | `e4f8cf67bbbc891cd3c893372f4d63e25d0319f2` | PASS |

This is a representative source-identity check across the build metadata, manifest, screen entry points, QR renderer, theme, list framework, and strings. It does not claim that every one of the 14,160 archive files was independently compared.

## ZIP limitations

The archive contains `.gitmodules` entries for 21 submodules, but their working trees are not expanded in this ZIP (for example, the Telegram native/media/widget submodule paths are empty). Therefore this ZIP is a source reference snapshot, **not a fully self-contained reproducible build checkout**. For a complete upstream build, clone the exact commit with recursive submodules as prescribed by the official README. For EVE screen porting, import only the verified dependency closure needed by the target screen.

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
