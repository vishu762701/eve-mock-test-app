# EVE — Complete the unfinished Telegram mission

Mission ID: EVE-TG-COMPLETION-REPAIR-20261024
Repository: https://github.com/vishu762701/eve-mock-test-app
Branch: main only
Audited remote snapshot: 0b9825a6bd8eecaeab011f750f047abe76f5e11d
Audit date: 2026-10-24

## Execution contract

Implement the remaining work yourself in Antigravity. This is a continuation and correction of EVE_TELEGRAM_COMPLETE_MISSION.md, not a new redesign. Preserve every original requirement and valid completed implementation. Do not return another plan, ask for the Telegram ZIP, repeat the multi-part paste protocol, or stop after checking Git/CI. The original root mission file includes the verified source archive. Read its human-readable requirements and decode locally without printing the base64 into chat or Termux.

Original mission file SHA-256: 54103ebca326496bff963c741439bf42f0032a9d313368791434c3382da297bb
Original embedded reference archive SHA-256: e87ac0cbc0a19f5434c15cffba7635726562b0c2831ebaaa4512ef5c55d4f80a

First inspect actual current main, working tree, relevant rules and current CI. The audited SHA is evidence, never a reset target. Preserve concurrent/uncommitted work. Only ordinary fast-forward commits/pushes to main; no force-push, reset --hard, clean, branches or PRs. Preserve authentication, authorization, server scoring, answers/timers, payments, notifications, existing Lottie and the circular reveal.

The latest Telegram visual migration supersedes the old monochrome/locked visual choices for its explicitly affected components. This is already stated in the original mission at line 30 and in the repository instruction hierarchy. Fix conflicting implementation, fixtures and descriptive docs together. Do not keep an obsolete visual rule just to satisfy an obsolete assertion; replace it with meaningful source-backed behavior checks, preserving functional and security safeguards.

## Independently established baseline — preserve what is working

VERIFIED from GitHub API: run 39996537943 has successful backend, build, firebase-functions, both native navigation jobs and APK publication. Artifact 12521126958 is Eve-debug-apk, 16,978,346 bytes, associated with audited SHA. Its API artifact digest is 35c2c97b3ea8fa5ff589883fe405c15cbdf1d41fee8f35737bbb21644314f8d6. Do not silently call this the extracted APK file checksum; obtain that separately from the build evidence/bytes when reporting it.

VERIFIED by independent byte comparison against the supplied Telegram source: the 11 newly reported icon mappings at mdpi/hdpi/xhdpi/xxhdpi (44 files) match the original assets. The five bundled .attheme files also match the original source. Preserve them and their attribution. Top-left btnMenu and four primary destination attachments exist. A functional theme picker/import path and a selection animation exist. These are useful partial deliveries, not proof of the whole mission.

The code audit below is source evidence. Device-level appearance, smoothness and all acceptance behaviors have NOT been independently established by this audit. Reproduce them rather than claiming the audit ran an APK.

## Findings and required repairs, ranked by impact

### P1-1 — The new theme is not the app-wide theme authority

VERIFIED evidence at the audited SHA:
- app/src/main/java/com/eve/app/EveApplication.kt:19 initializes the old ThemeManager.applySavedMode, not TelegramThemeEngine.
- app/src/main/java/com/eve/app/util/ThemeManager.kt:183 reads eve_prefs/key_dark_mode and sets AppCompatDelegate.
- TelegramThemeEngine uses a separate eve_telegram_theme/theme_mode store. Its only external init call is AppearanceActivity.kt:43. resolveCurrentPalette does not initialize persisted selection itself.
- app/src/main/java/com/eve/app/ui/settings/AppearanceActivity.kt:52 applies only the new engine. Its preview renderer at :233 changes six preview views, not the screen/app palette.
- app/src/main/java/com/eve/app/theme/TelegramThemeEngine.kt:334 notifies listeners; the production subscription found is EveNavigationHost.kt:86, updating only the pill.
- app/src/main/java/com/eve/app/util/SystemBarHelper.kt:170-174 still uses configuration uiMode and R.color.bg_app. EveBaseActivity has no new palette binding.
- Most XML backgrounds, styles, selectors and runtime factories remain resource-driven. A repository-wide production search found engine consumers only in AppearanceActivity, the navigation classes and DesignSegmentedTabs; merely importing the engine elsewhere is not integration.

Implications from these call paths: a selected theme does not recolor the entire app; Appearance mode and the existing toggle can disagree; after process restart Home may use default in-memory selection until Appearance initializes the engine. These violate the original mission's explicit runtime palette and persistence requirements.

Repair:
1. Establish ONE initialized, persisted selection/mode authority before any themed view is inflated; migrate the old mode preference deterministically. Keep the existing reveal as transition owner with an explicit adapter. Do not run competing theme systems or duplicate recreations.
2. Route Day/Night/System changes, old overflow toggle, startup, configuration changes and system-bar appearance through that authority. Preserve separate selected day/night variants. Synchronize status/navigation icons and palette with the reveal.
3. Inventory and wire all affected student/admin screens: backgrounds, solid cards, text, controls, drawables, selectors, Paint caches, adapters, menus, dialogs/sheets, tabs, buttons and shared factories. Cover XML and Kotlin; two values folders cannot represent arbitrary selected themes.
4. Rebind/reinflate safely without losing active test answers, timer, question position, navigation state, text edits or backend state. Remove listeners on destruction and avoid unnecessary bitmap/asset parsing on every frame or tab measurement.

Required behavior evidence: choose a visibly different theme and inspect Home, History, Settings, Profile, Result, an admin screen and the test flow; switch modes through BOTH entry points; background/foreground, process death, cold startup directly into Home and a deep link, and system-night changes. Prove saved choices apply before opening Appearance.

### P1-2 — Theme fallback/default/accent fidelity is incomplete

VERIFIED: TelegramThemeEngine.kt:466-467 is only colors[key] ?: default, not the upstream fallback chain. :374, :385-387 invent fixed secondary roles. :470 explicitly calls the accent path "not source-exact". The top-level comment at :18 also says it is not an exact port. The previous report's SOURCE-EXACT description overstates this implementation.

Upstream evidence already in the reference pack: Theme.java:3707 maps glass_targetMainTabs to dialogBackground; :3709-3711 maps selected/unselected glass keys; the full default/fallback and accent processing are in Theme.java/ThemeColors.java. EVE's hard-coded glass default ignores a custom dialogBackground when glass_targetMainTabs is absent. Day/9 and Night/0 IDs and accent seeds existing in code are not proof that every resolved UI token matches Telegram.

Repair the actual source resolution order: defaults, per-theme overrides, permitted accent transformation, special cases and fallback semantics. Port suitable Java/helpers rather than rewriting them arbitrarily in Kotlin. Preserve alpha and source rounding/branch behavior. Do not transform every raw key indiscriminately or label an approximation exact. Keep semantic exam colors intact. Ensure all available bundled/source-defined presets and accents are covered or specifically accounted for; a few arbitrarily selected accents are not a complete catalog.

Create independent source-derived expected values for Day/9, Night/0 and representative other accents, with origin references. Test missing direct keys resolved via their upstream fallback (including dialogBackground -> glass target), signed ARGB/alpha, defaults and special accent cases. Comparing values to the implementation's own constants alone is insufficient.

### P1-3 — The bottom pill does not render the requested glass/refraction

VERIFIED: EveBottomPillBar.kt:161-169 changes a paint colour, selecting 0.85 solely by API level; :192-198 draws a fill and two complete rounded strokes. It does not capture/blur a backdrop or run the bundled shader. paintShadow is configured at :158 but never drawn. Both "top" and "bottom" paints stroke the whole outline rather than using the source's edge treatment. A production-code search finds no RuntimeShader or liquid_glass_shader consumer.

Existing toolbar glass is a different old pipeline: DesignGlassLayout.kt:27-30 uses the old hairline; :101-105 and :154 use the old blur/tint resources. Having some old blur elsewhere does not implement the new bottom-pill source rendering or common provider.

Repair:
- Integrate the actual source BlurredBackgroundProviderImpl mainTabs/topPanel implementation and its relevant drawables/shader/math, with explicit EVE lifecycle/background adapters.
- Wire actual background sampling/blur/refraction on supported hardware. Isolate API 33+ classes. Capability/reduced-effects state, not API number alone, controls the liquid branch. Maintain correct lower-API/software/reduced-motion/power-save fallback.
- Match directional stroke, shadow, target-colour composition and offsets. Exclude the bar and its children from capture; keep text/icons sharp. Avoid recursive blur, old border/elevation overlays, redundant buffers and leaked listeners.
- Apply requested source-backed chrome to relevant menus/top panels; retain solid content cards. Do not import messaging/account dependencies or fake stubs to compile.

Prove rendering over a scrolling non-uniform backdrop, supported hardware and fallback paths, both modes, rotation/detach, and no self-capture. A source file existing in res/raw or a successful build is not execution evidence.

### P1-4 — Local tabs, chips, buttons and motion retain superseded UI

VERIFIED:
- EveDesignSystem.kt:56-63 deliberately retains "approved adaptive monochrome" category chips and calls new accents "opt-in". This contradicts original mission line 30 and the app-wide outcome.
- DesignSegmentedTabs.kt:72-76 keeps selected text from R.color.on_accent because its indicator remains the old accent fill. It is still Material TabLayout with existing dimension/style machinery, not the required role-selected source component.
- values/design_colors.xml:9-10 and values-night/design_colors.xml:9-10 remain black/white accent pairs. Night surface is #1C1C1E at :22 instead of the selected theme's surface role.
- DesignPress.kt:12 loads the old animator; design_motion.xml:3-6 keeps 100/140ms and 0.96 scale. No production implementation of the supplied ButtonBounce/ButtonWithCounterView/HorizontalRoundTabsLayout/FilledTabsView was found by symbol search. Renamed equivalent code must be demonstrated by actual behavior/source mapping, not inferred from a class name.

Finish the requested role-by-role source migration for category/filter pills, Result/admin local tabs, primary/secondary/destructive/loading buttons and press/selection behavior. Choose the correct upstream counterpart for each role and preserve its callbacks/data semantics. Maintain Result tab order, admin actions, disabled/error/loading behavior, one request per action, long/Hindi labels and accessibility. Never attach old and new press animators/haptics simultaneously.

Update stale visual tests/contracts with the new intended behavior. In particular .github/scripts/tests/test_design_system.py:28 forces text_primary == accent, and :30-34 pins the old fixture. SOURCE_OF_TRUTH.md still says Home has no floating navigation. Reconcile those directly conflicting statements without deleting functional/security assertions or unrelated historical records.

### P1-5 — App-wide icon mapping is unfinished

VERIFIED: docs/telegram-icon-source-registry.tsv has 71 rows: 27 OFFICIAL_TELEGRAM_GPL_ART_IMPORTED, 35 UNMAPPED_PENDING_SOURCE_AUDIT, 4 EVE_ORIGINAL_REDRAW_NOT_SOURCE_EXACT and 5 SEMANTIC_MISMATCH. Its header explicitly says launcher/dynamic indirection still needs audit. The new msg_* assets and ic_menu also require accounting rather than claiming that the older 71-row slice is automatically exhaustive.

Finish the inventory across XML, menus, compound drawables, runtime setters, notification small icons, selectors, badges and Lottie. Import real counterparts where semantically correct, preserving source density bytes and call-site behavior. Preserve intentionally coloured art and branding. Do not force Telegram messaging art onto exam-only meanings. An unresolved mapping cannot be relabelled "no counterpart" without source inspection; document legitimate EVE-specific adaptations with reasons. Preserve the successful msg_report mapping and verify the report handler and distinct mistake-notebook flag behavior.

Produce an actual final coverage matrix with source asset/caller, EVE use sites, all states/modes, exact import versus documented adaptation and remaining blocker. No unexplained PENDING rows in a claimed completed inventory.

### P2-1 — Navigation preservation, motion and accessibility need repair

VERIFIED:
- EveNavigationHost.kt:92-95 calls selectTab(currentTab,false) on every resume.
- MainTabsLayout.kt:62-74 dispatches callbacks even for programmatic selection; Home/MainActivity.kt:317-318 and HistoryActivity.kt:48-49 wire them to scroll-to-top. Therefore a lifecycle restore can trigger the user-reselect behavior and lose scroll position.
- MainTabsLayout.kt only wires click selection; the source gesture/long-press spring mechanics are not implemented there despite its comment claiming gesture coordination.
- GlassTabView.kt:235-243 tints the static ImageView and text only, not its visible Settings Lottie view. :201 updates a private selected flag without establishing View/accessibility selected state. :112/:267 uses DIP text sizing without an explicit accessibility scaling strategy.

Separate programmatic restoration from user selection/reselection. Preserve per-tab state and correct Android Back/deep-link behavior under repeated switching and process recreation. Restore source gesture/press/selection behavior with justified EVE/accessibility adaptations. Update Lottie using a proven compatible recolouring/loading/fallback path and cancel animations appropriately on detach. Establish selected roles/labels/focus and minimum touch targets, with readable enlarged text and Hindi/RTL support. Preserve font-scale handling rather than quietly shrinking text to fake fit.

Verify bottom placement, IME behavior, drawer overlays, gesture/three-button insets exactly once, and content clearance on all four destinations. Do not assume an overlap from code alone: existing Scroll styles already reserve 76dp; measure actual bounds and avoid adding duplicate clearance. Keep the global pill absent from active tests, payments/login and subordinate flows, while preserving local Result tabs.

### P2-2 — Preview, reset and imported-theme recovery have correctness gaps

VERIFIED:
- AppearanceActivity.kt:65 invokes resetToDefaults, which commits immediately via TelegramThemeEngine.kt:317-323. Back/Cancel can no longer restore the prior committed selection. The mission requires preview commit only on Apply.
- AppearanceActivity.kt:43-44 starts a fresh preview on every onCreate; there is no saved preview transaction restoration in that activity. Rotation and external picker recreation need a defined transaction lifecycle.
- TelegramThemeEngine.kt:248-249 falls back to Day for any unknown theme ID, including a missing Night selection.
- registerCustomTheme at :226 accepts any non-empty parsed map, and :229 guesses darkness from only the red channel. parseAtthemeStream at :437-449 accepts arbitrary keys; :459 narrows any Long to Int, wrapping out-of-range values. A file such as foo=1 can pass the non-empty check without recognized theme colours.
- AppearanceActivity.kt:115-120 hard-codes mode-button accents and white selected text. The engine's onAccent contrast test does not cover these controls. Accent swatches are 40dp at :210 without an explicit expanded touch target.

Make Reset a preview operation; only Apply commits. Restore or safely cancel the owned preview transaction across configuration change, process loss and picker return, without leaking a global preview after the owner is gone. Avoid losing user-selected committed themes. Repair missing/corrupt custom-file and invalid accent IDs with mode-appropriate Home defaults and persist the repaired selection/schema deliberately.

Preserve bounded import reads. Validate recognized usable theme data and allowed ARGB representations; reject malformed/out-of-range input rather than silently wrapping. Handle source-supported wallpaper markers safely or record unsupported optional content without executing or unboundedly downloading anything. Use source metadata/explicit supported classification or a justified luminance strategy rather than red-channel-only darkness. Close custom-file streams. Preserve imported files and stable IDs across restart; don't remove unrelated user data. Apply selected palette and actual foreground/background contrast to the whole picker, with >=48dp touch targets and accessible names/states.

### P1/P2 external dependency — Premium catalog remains unfulfilled

The previous agent itself reports BLOCKED_EXTERNAL_ASSET. Neither the five client-bundled assets nor a file import button supplies the missing Premium catalog. Original mission line 176 expressly forbids claiming full completion or running mission-final FULL solely to close this blocker.

Audit the actual available catalog and source-defined presets. Retrieve accessible official reusable exports/assets and their dependencies where possible; record exact theme IDs/names, source/export URL or supplied origin, hashes, Day/Night coverage, wallpaper/animation dependencies, proven upstream access category (or UNKNOWN), availability and redistribution basis. Premium subscription, unique-gift ownership and ordinary themes are different categories. Do not pretend every remote theme is Premium or available locally.

No fake Premium checks, credentials, impersonation or server entitlement bypass. Make all installed reusable choices free in EVE without altering paid-exam entitlements. If exact assets remain genuinely unavailable after investigation, report the SPECIFIC item/dependency and acquisition evidence, not only a generic schema name. Complete every independent local requirement first. Preserve the original mission's blocker/completion policy; don't secretly redefine "all Premium themes" as "supports import". Report PARTIAL / BLOCKED_EXTERNAL_ASSET until that mandatory gap is resolved or the owner explicitly changes scope.

## Why the green report did not establish completion

VERIFIED: the code retains stale monochrome contracts and isolated palette integration; the existing test suite passes those contracts. TelegramThemeEngineTest.kt:82-93 checks accent constants rather than complete resolved source palettes. MainTabsLayoutSizingTest.kt:21 uses four plain View instances, not actual icon/text/Lottie tab behavior. The mission's new Appearance/theme/navigation behaviors have no corresponding direct references in the inspected androidTest tree. This is a coverage gap, not proof those tests are worthless or permission to remove them.

INFERRED from that evidence: build repairs prioritized getting the existing contracts green while the original app-wide migration remained incomplete. The author's private reasoning is UNKNOWN. Do not repeat the inference as an observed motivation.

## Acceptance and verification plan — meaningful behavior, full original scope

Create a concise requirement-to-implementation-to-test matrix for every original mission section, including state, source reference, exact/adapted classification, both modes and evidence. This audit is a starting defect list, not permission to omit unlisted original requirements.

Use focused JVM/host tests to catch:
- Source-derived palette expectations/fallback/ARGB/defaults across multiple themes.
- Single mode authority, startup restore and missing imported-file recovery.
- Preview -> Reset -> Cancel retains the original committed choice; Apply persists; rotation/picker/process events have defined behavior.
- Programmatic tab restore does not emit user-reselect or reset scroll; actual tabs retain usable geometry/selected state.
- Valid/invalid import keys/ranges/size and deterministic failure, source asset hashes and complete inventory.

Implement device acceptance tests for the complete original matrix, including real Appearance selections applied to other activities, cold start, separate Day/Night choices, old toggle/system-mode interaction, active test state preservation, four-destination reselect/Back/deep links, live avatar, IME/insets, drawer access, Lottie/tints, real glass hardware and fallback, reduced effects, 320/360dp/tablet, enlarged fonts/Hindi/RTL and TalkBack. Extend test/evidence registration to actually execute and capture the new screens/flows; XML inflation alone does not run Activity behavior. Measure visible pixels/actual renderer state where appropriate; don't use strings/comments as proof of a working shader.

Preserve all pre-existing regression/security tests and required CI gates. Source-backed visual expectation changes are permitted where the owner superseded old aesthetics. Do not remove tests, weaken contrast, assert only constants copied from implementation, skip failures, introduce continue-on-error or add arbitrary timeouts to get green.

## Delivery and build policy

All intermediate implementation/repair pushes use FAST. No emulator/FULL per component or elapsed time. Run the available host checks and relevant compilation/JVM tests; if local Android tooling is unavailable use exact-commit Actions and inspect the first actionable error. Do not spend effort setting up Android Studio on the owner's phone.

Only after the entire mission acceptance matrix is satisfied, subject to its explicit external-asset blocker policy, use [eve-full] on the actual final implementation commit. Verify the new SHA's FULL run, lint, both native modes, Day/Night evidence, backend, Functions, signing safeguards, and gated APK publication. Reuse the historical green run only as baseline evidence. Diagnose failed checks and fix the root cause with FAST commits; no blind reruns or manual FULL dispatch without owner instruction.

Report concise Hinglish: starting/final SHA, each original requirement's state, component mappings, actual installed theme count and proven Premium provenance, remaining exact blockers, tests and runtime evidence, final CI/artifact only if applicable. Separate VERIFIED file:line/command evidence, INFERRED consequences and UNKNOWN device/asset facts. Report unrelated problems ranked by impact without silently fixing them. Do not claim "all complete" while any mandatory requirement remains unresolved.

END_OF_EVE_TELEGRAM_COMPLETION_REPAIR_20261024
