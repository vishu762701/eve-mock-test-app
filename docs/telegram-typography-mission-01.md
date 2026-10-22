# Telegram Android text/icon migration: scoped source register

**Status:** Partially implemented, staged for CI. No app-wide or pixel-identical claim.

Telegram reference archive SHA-256: `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3` (uploaded `Telegram-master (1).zip`). The archive has no .git history, so upstream commit identity is not proven. EVE authoritative starting main SHA: `251475ad7712b4da22339f48bd9e242e9d6b2971`. EVE ZIP was not supplied to this chat.

Source paths below start at `TMessagesProj/src/main/java/org/telegram/`.

| EVE mapped text role | Telegram source:line | Telegram observed properties | EVE style | Fidelity |
|---|---|---|---|---|
| Settings/Notifications/Result heading | `ui/ActionBar/ActionBar.java:522-535,1437-1461` | 20dp portrait one-line title, 18dp when subtitle/landscape, 17dp glass; `AndroidUtilities.bold()`; title role | NavigationTitle 20sp, system medium | Partial: EVE headings are not full Telegram ActionBar, no responsive alternate |
| Settings row label | `ui/Cells/TextDetailSettingsCell.java:40-45` | 16dp regular, `key_windowBackgroundWhiteBlackText` | SettingsLabel 16sp | Mapped |
| Settings row supporting label | `ui/Cells/TextDetailSettingsCell.java:50-55,87-92` | 13dp, gray text, optional multiline | SettingsDetail 13sp | Mapped, wrapping preserved |
| Settings section heading | `ui/Cells/HeaderCell.java:94-99` | 14dp, `AndroidUtilities.bold()`, themed color | SettingsSection 14sp, medium | Color adapt |
| Popup menu action | `ui/ActionBar/ActionBarMenuItem.java:493-504` | 16dp, submenu theme role | MenuItem 16sp | Mapped |
| Dialog title/message | `ui/ActionBar/AlertDialog.java:792-794,851-852` | 20dp bold / 16dp regular | DialogTitle/DialogMessage | Defined, NOT wired |
| Search field | `ui/ActionBar/ActionBarMenuItem.java:1530-1532` | 18dp, search text theme | SearchInput | Defined, NOT wired |

`AndroidUtilities.java:262-270,2392-2412`: Telegram title bold uses bundled `fonts/rmedium.ttf` or Android system-weight 500 depending on SharedConfig. EVE currently uses the Android system medium option; this does **not** guarantee the bundled default's exact glyph metrics. Telegram uses DIP units for many text properties; EVE uses SP to preserve Android fontScale accessibility. Theme color evidence: `ui/ActionBar/ThemeColors.java:14-16,100,107,197-204`, `assets/night.attheme:1-2,88,115,319,362,423`. Night secondary is deliberately opaque for contrast.

### Icon source candidates: audited as paths only, NOT imported

| EVE action | Actual ZIP asset | Confidence |
|---|---|---|
| Back | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/ic_ab_back.webp` | ActionBar.java:288-293,453 accepts drawable; individual caller/state unconfirmed |
| Overflow | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/ic_ab_other.webp` | Asset found; call-site/state not verified |
| Search | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/msg_search.webp` | Asset found; call-site/state not verified |
| Delete | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/msg_delete.webp` | Asset found; contextual use unverified |
| Share | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/msg_share.webp` | Asset found; contextual use unverified |
| Bell | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/msg_notifications.webp` | **NO EXACT TELEGRAM COUNTERPART established** for EVE inbox/bell |
| Exam score, accuracy, ranking, attempts | none established | **NO EXACT TELEGRAM COUNTERPART** |

**Licensing blocker:** Uploaded root LICENSE:1-3 specifies GNU GPL v2. README.md:9-15 says avoid Telegram branding and publish source. At inspected EVE baseline top-level LICENSE was not present. GPL-version compatibility and whole-app source-distribution obligations are unresolved; **no Telegram code, fonts, images or animations were copied**. Only style parameters were independently authored. Do not import/deliver GPL assets until a compatible license/source-publication plan is verified.

**Protection:** Views with `android:tag="eve_telegram_type"` bypass the retired published config renderer in `TypographyApplier`; dynamic old presets cannot override them. This marker must not be reused for unrelated View tags.

**Remaining gates:** all other screen roles (questions, timers, scores, button, badge, input, dialog), source-exact icon art/states/motion, full layout/dynamic View inventory, cache/Inter cleanup, custom fonts, license compatibility, light/dark screenshots, RTL/Devanagari/fontScale, native UI, security and publication checks. No obsolete file deleted because reachability is not fully established.
