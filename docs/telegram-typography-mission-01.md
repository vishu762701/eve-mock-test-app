# Telegram Android text/icon migration: scoped source register

**Status:** Partial migration delivered to `main`; final native UI and artifact results must be checked on the exact delivery commit. No app-wide or pixel-identical claim.

Telegram reference archive SHA-256: `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3` (uploaded `Telegram-master (1).zip`). The archive has no .git history, so upstream commit identity is not proven. EVE authoritative starting main SHA: `251475ad7712b4da22339f48bd9e242e9d6b2971`. EVE ZIP was not supplied to this chat.

Source paths below start at `TMessagesProj/src/main/java/org/telegram/`.

| EVE mapped text role | Telegram source:line | Telegram observed properties | EVE style | Fidelity |
|---|---|---|---|---|
| Settings/Notifications/Result heading | `ui/ActionBar/ActionBar.java:522-535,1437-1461` | 20dp portrait one-line title, 18dp when subtitle/landscape, 17dp glass; `AndroidUtilities.bold()`; title role | NavigationTitle 20sp, system medium | Partial: EVE headings are not full Telegram ActionBar, no responsive alternate |
| Settings row label | `ui/Cells/TextDetailSettingsCell.java:40-45` | 16dp regular, `key_windowBackgroundWhiteBlackText` | SettingsLabel 16sp | Mapped |
| Settings row supporting label | `ui/Cells/TextDetailSettingsCell.java:50-55,87-92` | 13dp, gray text, optional multiline | SettingsDetail 13sp | Mapped, wrapping preserved |
| Settings section heading | `ui/Cells/HeaderCell.java:48-53,94-99` | 14dp, `AndroidUtilities.bold()`, default `key_windowBackgroundWhiteBlueHeader` | SettingsSection 14sp, medium; dedicated day/night blue role | Light adapted for contrast; night exact theme value |
| Popup menu action | `ui/ActionBar/ActionBarMenuItem.java:493-504` | 16dp, submenu theme role | MenuItem 16sp | Mapped |
| Dialog title/message | `ui/ActionBar/AlertDialog.java:792-794,851-852` | 20dp bold / 16dp regular | DialogTitle/DialogMessage | Defined, NOT wired |
| Search field | `ui/ActionBar/ActionBarMenuItem.java:1530-1532` | 18dp, search text theme | SearchInput | Defined, NOT wired |

`AndroidUtilities.java:262-270,2392-2412`: Telegram title bold uses bundled `fonts/rmedium.ttf` or Android system-weight 500 depending on SharedConfig. EVE currently uses the Android system medium option; this does **not** guarantee the bundled default's exact glyph metrics. Telegram uses DIP units for many text properties; EVE uses SP to preserve Android fontScale accessibility. Theme color evidence: `ui/ActionBar/ThemeColors.java:14-16,100,107,197-204`, `assets/night.attheme:1-2,88,115,319,362,423`. Night secondary is deliberately opaque for contrast. Light supporting text is adjusted to `#6C6C70` to exceed 4.5:1 on the `#F5F5F5` canvas and `#FFFFFF` surface. The Telegram light `BlueHeader` theme role defaults to `#298ACF`, which is only 3.42:1 against EVE's grouped `#F5F5F5` background. EVE uses an explicitly accessibility-adapted `#176A9E` for Light header contrast (5.36:1) and Telegram's actual night `windowBackgroundWhiteBlueHeader=#6CB6F8` (night.attheme:93). These adaptations are NOT exact Telegram light palette reproduction.

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

**Licensing blocker:** Uploaded root LICENSE:1-3 specifies GNU GPL v2. README.md:9-15 says avoid Telegram branding and publish source. At inspected EVE baseline top-level LICENSE was not present. GPL-version compatibility and whole-app source-distribution obligations are unresolved; **no Telegram-authored code, images or animations were copied; Google's separately licensed Roboto Medium font is documented below**. Only style parameters were independently authored. Do not import/deliver GPL assets until a compatible license/source-publication plan is verified.

**Protection:** Views with `android:tag="eve_telegram_type"` bypass the retired published config renderer in `TypographyApplier`; dynamic old presets cannot override them. This marker must not be reused for unrelated View tags.

**Regression:** `TelegramTypeSourceRolesTest.textRolesMeetNormalTextContrastOnDayAndNightSurfaces` covers primary, secondary, section and dialog roles against both EVE canvas and card surfaces. The previous failed native UI assertion was `StudentUiRenderingTest` Settings `PREFERENCES` (3.36:1); this fix changes app text colors rather than weakening the assertion.

**Remaining gates:** all other screen roles (questions, timers, scores, button, badge, input, dialog), source-exact icon art/states/motion, full layout/dynamic View inventory, cache/Inter cleanup, custom fonts, license compatibility, light/dark screenshots, RTL/Devanagari/fontScale, native UI, security and publication checks. No obsolete file deleted because reachability is not fully established.

## Main-only incremental component coverage (second change)

Source-verified matches from the same uploaded Telegram ZIP:
- `ui/Cells/DialogCell.java:1252-1254` and `ui/ActionBar/Theme.java:7803-7815`: conversation-row name 17dp medium/bold() and preview 16dp regular. Adapted to EVE `item_notification.xml` title/body at **17sp/16sp**, without copying Telegram messaging content or interactions.
- `ui/ActionBar/Theme.java:7897,7926`: dialog-list date text 12dp and `key_chats_date`, mapped to the EVE inbox relative-time label as 12sp with accessible secondary color. These are semantic counterparts, not an assertion that notifications and chats are identical products.
- `ui/ActionBar/ActionBarMenuItem.java:1530-1532`: 18dp ActionBar search text. Wired Home's `etSearch` to a derived `Widget.Eve.Token.Input.TelegramSearch` that **inherits** EVE's input background, focus states, cursor, input type and touch area. Style parameters are adapted to SP.
- `ui/Cells/TextSettingsCell.java:82` / `TextDetailSettingsCell.java:40-45`: 16dp settings-action label applied to About screen's Privacy/Terms/Contact links. About navigation title uses the already mapped ActionBar-derived style.

All migrated views bear the `eve_telegram_type` tag to avoid a stale published Inter/iOS typography override. Existing EVE bell drawing and Lottie, button callbacks and scoring logic are untouched. No Telegram-authored GPL source/art assets were copied; the independently Google-licensed font is documented below.

### Remaining critical blockers
- GPL-2.0-only compatibility and full corresponding-source obligations are unresolved: no Telegram icon art, RLottie, or bundled Telegram fonts may be imported into a production build without resolution.
- Other views, programmatic custom Views, dialog internals, exam-specific roles and Telegram-unique icon states remain unmapped; do not describe the app as Telegram-exact.
- CI Android build/lint/native UI, light/dark captures, font scale 1.0/enlarged, Devanagari/RTL, permission and motion regression require evidence on the new commit.

## Main-only native typography foundation — full shared-token coverage

Mapped all `TextAppearance.Eve.*` and `TextAppearance.Eve.Token.*` typography resources, plus shared Android button/input widget fonts and `Theme.Eve.Foundation` family, away from bundled Inter and onto platform `sans-serif` / `sans-serif-medium` (Telegram `AndroidUtilities.bold()` uses platform 500 on supported devices). Original fonts/assets are preserved because standalone license and dependency reachability are unresolved, **not** because they remain approved default.

The shared Home/nav title and content title now use 20sp/26sp as the ActionBar title analogue. Body 16sp/22sp maps Telegram message/setting regular text. Heading 17sp/22sp maps DialogCell conversation header. The 34sp hero value and 11/12sp auxiliary labels are EVE exam-content-specific compatibility semantics, *not* asserted official Telegram universal typography roles. All base/sw360/sw600 dimension variants were updated together. Semantic success/error colors are retained for exam-result meaning.

The prior published `ios26_balanced` typography configuration/cache and backend contract are retained but no longer allowed to repaint XML text or programmatic `DesignType` components. A future `telegram_native` preset requires separate compatibility/security verification before remote overrides may be re-enabled. No deprecated server API or bundled fonts were removed because active dependencies have not been fully audited. Tested compatibility role constraints are updated to measure **actual new fonts and sizes**, not disabled.

## Source-fidelity correction: the guessed icon pass was reverted

The prior UI-only 2dp uniform-stroke edits and independently improvised share-arrow geometry are **NOT real Telegram source mappings**. They were explicitly rejected as insufficient by the owner's source-exact requirement. The 10 affected vector drawables (`ic_back`, `ic_more_vert`, `ic_search`, `ic_delete`, `ic_bell`, `ic_settings`, `ic_bookmark`, `ic_close`, `ic_send`, `ic_share`) were restored from verified pre-mission EVE commit `251475ad7712b4da22339f48bd9e242e9d6b2971`. Original resource names, touch areas, tint and event handlers remain intact. The retained MIT Tabler baseline is a **temporary working fallback**, not the requested Telegram-exact replacement.

Actual Telegram static art candidates and call sites remain in the source register. Exact Telegram binaries and procedural animation code are **BLOCKED for import pending compatible GPLv2-or-later licensing and complete corresponding-source publication analysis**. Preserve EVE domain-specific icons where Telegram has no exact counterpart. The app must not ship under a false "Telegram icons migrated" label.
## Native UI regression discovered and corrected at test-contract level

The first global-font migration CI run `39776033769` passed Android build/lint/unit, backend and Firebase, but gesture instrumentation found three stale iOS typography assertions (actual 16sp option text vs former 17sp, modal `fontFamily=sans-serif` returned TYPE_STRING rather than the retired Inter font resource ID). The corresponding tests were updated to assert the **new runtime tokens** while preserving every size, layout, state, gesture, question-answer functional and image evidence check. Production XML/Java/Kotlin UI was NOT changed for this test-only correction. This is a replacement of obsolete iOS expectations, not weakening an unmet Telegram or functional guarantee. Confirm the new commit's CI before claiming green.

Follow-up stacktrace `ApprovedUiRenderingTest.kt:254` specifically identified the question-body 17sp assertion, not only the answer option. The native test now checks both question and answer text at the migrated 16sp size.

### Dialog + ActionBar source correction (new main change)

Telegram source ZIP `ui/ActionBar/ActionBar.java:522-537,1443-1455` binds default title to `Theme.key_actionBarDefaultTitle`, bold() system 500 or bundled rmedium, padding 8dp top/bottom, and no-subtitle title text size 20dp default, 18dp phone landscape, 20dp tablet. Existing EVE navigation title role is 20sp; new qualified `values-land/telegram_typography.xml` uses 18sp on phone landscape, and `values-sw600dp-land/telegram_typography.xml` restores 20sp on tablets. EVE existing toolbar padding is preserved to avoid unapproved geometry change; this is a **documented deviation**, not pixel-exact ActionBar.

Telegram `ui/ActionBar/AlertDialog.java:788-795,849-854` gives title 20dp with `bold()` and themed dialog text, and message 16dp regular with the same dialog text role in the standard case. Shared `Widget.Eve.Token.Dialog.Title` and `Body` now use `TextAppearance.Eve.Telegram.DialogTitle` (20sp) and `DialogMessage` (16sp). Feedback-create and feedback-reply custom modal headers are mapped; existing controls, accessibility tags and form behavior are unchanged. Theme resolution stays day/night through `telegram_type_dialog`, and Dp→Sp remains the accessibility deviation.

## Exact Telegram default Roboto Medium — Google Apache-2.0 asset

The Telegram archive's actual `SharedConfig.java:619` default is `useSystemBoldFont=false`. `AndroidUtilities.java:262-270` therefore uses bundled `rmedium.ttf` for `bold()` unless system font is explicitly selected. The exact Google-owned Roboto Medium 2.138 font file, from matching archive/upstream Git blob `aba251d159c783048b352d17f6581d406f2a6ddb`, now ships under `res/font/telegram_roboto_medium.ttf` and is applied to audited ActionBar/Section/Dialog/Notification title roles. Its Google copyright, full Apache-2.0 license and source SHA-256 are bundled in app assets. No Telegram GPL-owned icon or Java code is copied.

This corrects the earlier inexact system-500 substitute for the named roles only. Global/app-specific legacy types and platform-dependent fallback renderings remain a documented fidelity gap; do not label the whole mission complete. Details and 95-layout inventory: `docs/telegram-full-layout-inventory.md`.

## Exact bundled medium font — shared title and heading mapping

Source default branch: `SharedConfig.java:619` `useSystemBoldFont=false`; `AndroidUtilities.java:262-270` `bold()` returns `fonts/rmedium.ttf`. Both `ActionBar.java:534` and `Theme.java:7812` assign that same Typeface to ActionBar title and conversation list heading. The already licensed byte-for-byte font at `res/font/telegram_roboto_medium.ttf` is now used for EVE shared 20sp `TextAppearance.Eve.Token.Title` and 17sp `TextAppearance.Eve.Token.Heading`, as well as `TextAppearance.Eve.Headline` base variants. `DesignType.apply()` explicitly binds these two semantic counterparts through `FontManager.telegramMediumTypeface()` rather than substituting 600/700 system-bold. The custom method uses a thread-safe cache and falls back to Android native 500 only when the bundled font fails.

**Scope limit:** EVE title and heading text are analogous to Telegram ActionBar/dialog-list headings, not complete Telegram `SimpleTextView` layout/emoji mechanics; app-specific hero numbers/timers and buttons retain their distinct semantics. Source uses dp and EVE uses sp for accessibility. All existing GPL-art import blockers remain unchanged.

## Day/Night Telegram primary-foreground propagation for mapped headings

`ThemeColors.java:100,197-204` and shipped `night.attheme` establish Telegram dark/foreground roles. Three source-matched shared 20sp/17sp title/heading styles now use `@color/telegram_type_primary` instead of iOS `@color/text_primary`. Resolved resources: light `#FF1A1D21`, dark `#FFFFFFFF`. This applies to TextAppearance defaults; **explicit runtime setTextColor calls and individual XML android:textColor overrides still take precedence** and require future screen-level audit. Global accent, success/error colors, background surfaces and business logic remain unchanged. This source-foreground pass is not a global palette migration or a claim of identical Telegram themed rendering.

## Source-aligned AlertDialog action typography

Exact source: `ui/ActionBar/AlertDialog.java:1112-1138,1151-1176,1192-1215`. Positive, negative and neutral dialog action TextViews use `TypedValue.COMPLEX_UNIT_DIP,16` and `AndroidUtilities.bold()`; the latter selects bundled `rmedium.ttf` under the verified default settings. EVE `Widget.Eve.Telegram.DialogAction.Primary/Secondary` styles now use the separately Apache-2.0 licensed identical bundled font and 16sp, wired through `Theme.Eve.Dialog.Base`. `DesignDialogs.showDestructive()` reasserts exact medium typeface after its existing compatibility styling, preserving destructive color, click handler and content description. **Deviation:** MaterialAlertDialog button geometry/background, 48dp touch height and EVE primary/secondary/destructive semantic colors remain instead of Telegram's 40dp/12dp-padding and `dialogButtonColorKey`, preserving EVE accessibility and scope; no full Telegram AlertDialog or GPL source copied.

## Complete direct-layout icon source registry

The machine-readable `docs/telegram-icon-source-registry.tsv` inventories the **66 distinct direct UI icon references** in all 95 XML layout files, separating verifiable source-art candidates, semantic mismatches and **NO EXACT TELEGRAM COUNTERPART**. It records EVE's existing touch/state and theme tint as fallback (not Telegram source fidelity) and makes no unverified animation claims. `TelegramIconSourceRegistryTest.kt` prevents omissions and false imported/exact statuses. Additional dynamic icon use sites and notification/launcher icons require a separate pass; no GPL icon assets imported.

## Dynamic icon use-site expansion

Direct XML contains 66 icon IDs. A targeted executable search of 11 EVE source files containing `setImageResource`, `R.drawable.ic_*` or `setSmallIcon` identified **17 dynamically referenced IDs**, including **five not visible in the direct XML set**: `ic_bell_off`, `ic_minus`, `ic_status_circle_unattempted`, `ic_status_circle_wrong`, `ic_theme_moon`. The registry now lists **71 unique icons** and `TelegramIconSourceRegistryTest` cross-checks the 11 Kotlin source files and the full XML inventory. Theme-toggle icons, notification state bell, answer grading circles and +/- quantity controls retain EVE states; **NO EXACT TELEGRAM COUNTERPART** is asserted for the five additions. Java reflection, remote icons and launchers remain out of scope of this completed inventory slice.

## Mapping-confidence correction

The 71-resource source registry now distinguishes **UNMAPPED_PENDING_SOURCE_AUDIT** (Telegram counterpart *unknown*) from **SEMANTIC_MISMATCH** (candidate art exists but call-site purpose differs) and **CANDIDATE_NOT_IMPORTED** (matching candidate asset found but GPL artwork not imported). A missing name search does not prove Telegram has no analogous procedural drawable. Preserve EVE drawables until real asset/call-site/theme/dependency evidence supports replacement. No exact match is fabricated.

## Result Detail paired ActionBar title and subtitle mapping

Telegram ActionBar.java:469-477 and 1445-1452 specify title/subtitle paired metrics of 18dp/14dp for phones and 20dp/16dp for tablets. ResultDetailActivity now changes title style only when the exam-name subtitle is present; otherwise the 20sp NavigationTitle is kept. The mapped XML text styles use corresponding sp sizes for Android font scaling. The Telegram source light subtitle #FF79817E and night #73F2F2F2 are mapped to the more legible EVE telegram_type_secondary Day/Night role, as a documented contrast deviation. Existing labels, multi-line content, screen geometry and functions remain unchanged. No Telegram GPL asset was copied.

## Verified source icon geometry and warning mismatch

See `docs/telegram-icon-binary-geometry.md` for 10 actual source assets across mdpi/hdpi/xhdpi/xxhdpi. Nine candidates have conventional 24dp (24/36/48/72px) square image bounds. Telegram's `msg_warning` is only 2x9px at mdpi, so it has been **reclassified as SEMANTIC_MISMATCH**, not an EVE warning icon. The measurements are archive-derived, not source-exact integration; all EVE icon drawables remain unchanged until legal and call-site requirements are met.

## Additional exact source call-site corrections

The ZIP's actual Java usages prove `msg_calendar` is a chat search date icon (ChatActivity.java:10664), `msg_pin` is a chat pin action (DialogsActivity.java:6739), and `msg_warning` is a message-error indicator (Theme.java:8228). The icon registry now records those semantics and blocks careless substitutions. Zero Telegram icon bitmaps have been added to EVE's packaged resources; an unresolved whole-app redistribution/license plan remains the constraint.

## Source-derived ActionBar icon tint — limited real UI change

Telegram source `ui/ActionBar/ThemeColors.java:193` defines `key_actionBarDefaultIcon=DEFAULT_BLACK_TEXT` (Telegram day `#FF1A1D21`); bundled `night.attheme` defines the corresponding action icon color white. Eight EVE activity XML layouts now tint **back and overflow actions only** using the already paired `@color/telegram_type_primary` resource (Day `#FF1A1D21`, Night `#FFFFFFFF`): Home, Settings, Notifications, Result, Profile, Result Detail, About and Admin. Original icon art is untouched because Telegram image copyright/GPL compatibility remains unresolved. Existing content descriptions, 48dp touch targets, selectors, callbacks and app navigation are intact. This is verified **color role adaptation only**, emphatically **not exact official icon glyph integration**. Source on-screen per-Activity code may still override icon tint and requires runtime visual review.

## Complete activity ActionBar tint source role

All 39 activity layouts with a navigational back icon or Home overflow are mapped to source-traced `Theme.key_actionBarDefaultIcon` Day/Night ink. The last batch includes `MaterialToolbar.app:navigationIconTint` in Syllabus and an explicit ImageButton tint in System Monitor; both differed structurally from ordinary EVE ImageButton call sites. `TelegramActionIconTintTest.everyActivityNavigationActionUsesTelegramSourceIconColors` audits every activity XML and fails for unmapped color roles. No icon bitmap/vector path has been changed or copied from Telegram; this is only the official ActionBar color-role part of Mission 01.

## Source-verified Telegram list roles on exam, history, feedback rows

Official ZIP `ui/Cells/DialogCell.java:1252-1254` names 17dp, previews 16dp; `ui/ActionBar/Theme.java:7812,7897` applies `AndroidUtilities.bold()` to list names and 12dp to time. EVE ListTitle (17sp original bundled Roboto Medium), ListPreview (16sp regular), and ListTime (12sp regular) now style the primary rows in `item_exam`, `item_history`, `item_feedback_post`, `item_existing_exam_admin` and Kotlin ExamAdapter rebindings. Day/Night roles use EVE accessible primary/secondary adaptation; the explicit non-source deviation is `sp` font scaling and the existing attempted green-status text meaning. Exam title wrapping expanded to two lines for long exam names; no navigation, marking or server logic changed. GPL Telegram icon artwork still remains unimported pending license compliance.

## Official Telegram Tab Label Typography

Telegram `ui/Components/ScrollSlidingTextTabStrip.java:547-554` uses 15dp, single line, and `AndroidUtilities.bold()` for selected AND unselected labels, with 16dp internal padding. EVE `DesignSegmentedTabs` and `DesignType.Role.TAB` now use the existing exact bundled Roboto Medium for both states, and shared 15sp sizing applies in base/360dp/600dp qualifiers. Only font/size changed; EVE existing accessible active/idle colors, track, indicator, touch bounds and `ResultTabs` large-font scroll fallback are deliberately retained. This is a source-aligned typography subset, not a Telegram pixel-identical tab track.

## Seven actual icon artwork changes — EVE-original licensed fallback

The following 24dp Android XML vectors have been **redrawn from independently authored generic UI geometry**: `ic_back`, `ic_more_vert`, `ic_search`, `ic_delete`, `ic_share`, `ic_close`, and `ic_bookmark`. The prior Tabler MIT icon paths are replaced for these seven only; Tabler remains present elsewhere and its license notice must be retained. These common symbols are source-informed about Telegram's corresponding action, but their art was **not copied from Telegram**, and pixel-exact source fidelity is explicitly **NOT VERIFIED**. Scope: original resource IDs remain, original action handlers/pressed states/accessibility labels and 24dp intrinsic bounds remain. Other notification, settings, scoring and brand icons remain untouched. No Telegram-owned GPL asset or RLottie class has been imported. True original Telegram icon raster integration is still blocked by unresolved whole-app GPL compatibility and source distribution obligations.

## Icon register after first artwork migration

Seven EVE-original 24dp vector glyphs replace the prior Tabler-derived paths for `ic_back`, `ic_more_vert`, `ic_search`, `ic_delete`, `ic_share`, `ic_close`, `ic_bookmark`. `docs/telegram-icon-source-registry.tsv` now marks these as **EVE_ORIGINAL_REDRAW_NOT_SOURCE_EXACT** rather than stale candidate/unmapped status; unit coverage verifies the exact count. Their full Android UI resource IDs and view bindings are unchanged. No trademark or Telegram proprietary bitmap reproduced.

## Independently authored alert and utility icon states

Six additional 24dp vectors were independently authored without Telegram GPL art: bell, notification inbox, bell-off, settings, edit and pin. `ic_bell` and `ic_notification` share a consistent generic bell silhouette; `ic_bell_off` adds a clear slash so on/off semantics remain distinguishable. Existing Home notification Lottie, Android notification small-icon resource ID, reminder state toggle, pin state and edit action callbacks are preserved. These are functional **EVE-owned replacements**, not the exact Telegram WebP assets. Total source-informed EVE-owned redraws now 13. The rest retain their previous EVE icons pending source mapping and license clearance.

## Native CI typography regression repair

Native UI gesture/three-button `ManualUiSpecificationTest.responsiveGeometryAndThemeReapplication` originally asserted tab text 14sp and failed after Telegram `ScrollSlidingTextTabStrip.java:549` migrated it to 15sp: expected textPx 36.75, actual 39.375 at fontScale 1.5 (15/14 exactly). Updated the **positive source-derived assertion to 15sp** while keeping its 0.51px tolerance, all fontScale/width/theme loops, ellipsis checks and native runner gates unchanged. The test continues to detect real label-size deviations; this is not a disabled check.

## Factory profile and legacy-cache compatibility repair

Factory initialization previously still selected `ios26_balanced` / Inter. `TypographyRepository.createDefaultConfig`, `TypographyModels` schema defaults, `TypographyPreset.find` and the resolver's safe fallback now choose a first-class `telegram_native` 17-role mapping based on EVE's existing Android semantic type dimensions. Legacy iOS/Inter presets and cached versions **remain readable without altering user files, remote configurations or migrations**. An unknown font-family id now falls back to Android Roboto instead of Inter. Crucially, `TypographyApplier.applyToView` no longer replaces an XML-styled Roboto Medium with generic Roboto Regular when no explicit published override exists; real explicit overrides remain supported. The platform's Devanagari fallback remains intact. This corrects a real code-path inconsistency while retaining the protected test-session freeze.

## Home and floating-menu icon role parity

Home search/clear and idle notification glyphs use the same light/dark `telegram_type_primary` as the verified ActionBar back and overflow icon roles, eliminating ink inconsistencies between sibling toolbar actions. The seven independent popup menu icons now use that role as well. The Mistake Notebook popup action previously displayed an X/close icon, which semantically signalled dismissal; it now uses the existing flag icon, already present in EVE, without changing its click destination or adding GPL artwork. Lottie notification animation, its alpha guards and ThemeSwitch circular reveal remain untouched.

## Shared body and secondary foreground semantic migration

The 95-layout base token cascade now maps `TextAppearance.Eve.Token.Body` and `TextAppearance.Eve.Token.Secondary` to Telegram's verified `telegram_type_primary` and the contrast-adapted `telegram_type_secondary` day/night color roles. This reaches legacy reusable body/secondary style references while leaving the original 16sp/14sp size and Android Roboto regular-family settings intact. All explicit green/red/orange/blue answer, destructive and status overrides remain higher priority. EVE's `text_primary`/secondary underlying platform, graph, and exam state tokens have **not** been globally replaced, so code-level setTextColor overrides remain distinct. Telegram's exact message-cell bitmap/padding behaviors are not claimed.

## Native question bookmark icon state repaired

Prior `DesignIconStates.bookmark` always installed `ic_star_outline` for both checked and unchecked question bookmarks and only varied tint; the actual selected/saved silhouette was indistinguishable. It now selects EVE's preexisting `ic_star_filled` for checked and `ic_star_outline` for unchecked, keeps its ColorStateList, accessibility content description, `view.isSelected`, haptics, click callbacks and persistence call sites in QuestionAdapter and BookmarkAdapter. Neither vector is alleged to be Telegram's exact copyrighted artwork; this is a functional state-fidelity repair.

## Exact official Telegram drawer icon tint roles

The source ZIP's `ui/ActionBar/ThemeColors.java:268` establishes `chats_menuItemIcon=0xff889198` (Day). `TMessagesProj/src/main/assets/night.attheme:292` establishes `chats_menuItemIcon=-8224126` (Night, `#FF828282`). EVE's eight Home navigation-drawer icon call sites now use `telegram_drawer_icon` Day/Night rather than mixing black/white toolbar text ink; popover ActionBar submenu icons still follow Telegram's distinct `key_actionBarDefaultSubmenuItemIcon` primary role, so the two legitimate sources are not conflated. Drawer labels and destinations are unchanged. Derived contrast ratios: #889198 on #FFFFFF ≈3.21:1 and #828282 on #000000 ≈5.46:1, meeting ≥3:1 non-text icon contrast.

## Factory profile runtime traversal guard

The Telegram default is an XML style baseline, **not a request to re-apply each TextView on Activity creation**. When the active `telegram_native` configuration has no `screenOverrides`, `elementOverrides` or `componentOverrides`, `TypographyApplier.applyToActivity` now exits immediately. This eliminates potentially expensive full Activity traversal and source text-paint disruption, particularly near theme reveal/native UI focus transitions. Published explicit overrides retain the existing hierarchy traversal path. This reduces a potential risk identified after native gesture CI timing/focus failures; it is not falsely claimed to prove that all seven unrelated gesture assertions shared one root cause.

## Native UI root-text contract update after ink migration

`LockedFoundationTest.defaultWidgetsResolveTheLockedRootInBothThemes` previously asserted the default TextView is always `text_primary` (pure black in Day). After shared base TextView `TextAppearance.Eve.Token.Body` color migrated to Telegram default `#FF1A1D21`, that is no longer the intended color. Its native Day/Night expected foreground now explicitly resolves `R.color.telegram_type_primary` while existing `DesignTokens.primary`, disabled buttons, backgrounds and all geometry checks remain untouched. This is a source-driven update of one obsolete assertion, not a relaxation or disabling of a CI test.

## Result history paired title/subtitle typography source mapping

ResultActivity now follows the verified Telegram ActionBar.java:1445-1452 paired title/subtitle behavior: historical attempts showing an "Attempted on ..." subheading use `NavigationTitle.WithSubtitle` alongside `NavigationSubtitle`; normal result entries keep `NavigationTitle` and hide the subtitle. The existing phone/tablet 18/14 and 20/16sp resource qualifiers automatically apply. No result tabs, scores, review data or navigation handling was changed.

## Shared IconButton default Day/Night color

`Widget.Eve.Token.IconButton` now uses Telegram ActionBar's source-mapped `telegram_type_primary` as its default tint, instead of the retired generic `text_primary`; explicitly tinted answer-status, destructive, category, panel and premium icons retain their local override. The default icon touch sizes, press-scale animator, and `@null` background are unchanged, so this does not invent Telegram ripple timings. No binary Telegram artwork is included.

## Mission 01 V3 provenance and component matrix

See `docs/telegram-mission01-v3-component-matrix.md` for ZIP-verified paths, additional seven real icon source candidates, licensing constraints, Day/Night coverage and CI-process readiness. The registry candidates preserve original EVE art: no GPL2 bitmap has been added and no copyright-bound asset has been falsely declared source-exact.
