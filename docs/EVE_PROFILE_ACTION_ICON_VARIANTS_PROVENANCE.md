# Telegram Profile action icon variants — exact upstream assets and EVE palette adaptation

**Scope:** The three Profile action buttons already rendered in EVE (Set Photo, Edit Info, Settings). FID-04 **PARTIAL**, not complete original Telegram Canvas renderer.

Official source: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, locally authenticated ZIP `Telegrammaster(5).zip` SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`; absent `EVE_MANIFEST.sha256` expressly not verified. All assets use unchanged Telegram vectors, with byte-exact Git blob SHA proven by `ProfileActionIconSourceTest`. Upstream source mapping: `TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileActionsView.java:407-460,1215-1234` and `org/telegram/messenger/AndroidUtilities.java:4925-4926`.

| Property | Official source | EVE mapping | Classification |
|---|---|---|---|
| Filled Set Photo glyph | `drawable/filled_profile_photo.xml`, Git blob `eeb444043a1604a691a408d6d4161f62d09ad76a` | Existing `drawable/telegram_profile_photo.xml` | **EXACT bytes** |
| Filled Edit Info glyph | `drawable/filled_profile_edit_24.xml`, blob `4419e6284f9fd2d67250165904636d5bdc459369` | Existing `drawable/telegram_profile_edit.xml` | **EXACT bytes** |
| Filled Settings glyph | `drawable/filled_profile_settings.xml`, blob `67dbfcfac15413f0308c65e0403a34ec76b2f87f` | Existing `drawable/telegram_profile_settings.xml` | **EXACT bytes** |
| Outline Set Photo glyph | `drawable/outline_profile_photo.xml`, blob `11e2373136ba8ee9dba936df41a89cc74dcd7bea` | New `drawable/telegram_profile_outline_photo.xml` | **EXACT bytes** |
| Outline Edit Info glyph | `drawable/outline_profile_edit_24.xml`, blob `6b30b4aec7ebcd8dabfa6ec48786777c8f326276` | New `drawable/telegram_profile_outline_edit.xml` | **EXACT bytes** |
| Outline Settings glyph | `drawable/outline_profile_settings.xml`, blob `eea35faa4a59b617603d8a9fad67a8b3c4b7ceef` | New `drawable/telegram_profile_outline_settings.xml` | **EXACT bytes** |
| Button color threshold and icon selection | `ProfileActionsView.java:185-187,412-419,447-459`: light button uses outline black unless parentExpanded crosses .75 on API31+, dark button uses filled white | `ProfileActionIcons.resources` / `ProfileActivity.onApplyThemePalette`: light Day background #E5E5EA -> outlines; dark Night #1C1C1E -> filled | **ADAPTED** stable endpoint; EVE has no Telegram parentExpanded viewer overlay |
| Glyph tint and animation crossfade | Original `PorterDuffColorFilter` black/white + `parentExpanded` alpha interpolation | Existing EVE `@color/telegram_type_primary` Day/Night tint; palette callback swaps vectors | **ADAPTED**, not EXACT parentExpanded crossfade |
| Icons 24dp geometry | Source vector definitions and `ProfileActionsView.java:390-401` | Existing Profile XML `ImageView` layout dimensions | **EXACT dimensions** |
| Tap/collapse/role semantics | `ProfileActionsView.java:520-587,1247+` | Existing `ProfileActionBounce`, `ProfileActionsCollapse`, three EVE clickable cells | **PARTIAL** integration; native virtual accessibility nodes not ported |

Unit proof: `app/src/test/java/com/eve/app/ui/profile/ProfileActionIconSourceTest.kt` hashes **all six** committed SVG vector XML files against the official Git blob SHA-1 and checks theme binding. `ProfileActionIconNativeTest.kt` checks Day/Night vector drawable inflation and retained accessible controls on actual emulator execution. FAST compiles this AndroidTest; it does not execute it.

**Pending**: the original Telegram exact action color gradient, source-defined rounded-rect Canvas shader and parentExpanded image crossfade, full component gesture/render path and native matched frames; plus Bio/presence backend contract, Apple official bell rights, app-wide source-fidelity inventory, six-job terminal FULL and APK checksum. No production backend or account data changes.
