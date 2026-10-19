# EVE design reset evidence

The owner's current locked specification supersedes historical UI authority.
Only the Telegram day/night toggle animation and its behavior/resources remain
protected. Business logic, data, navigation flow and features remain invariant.

## Phase 0

- Source baseline: `40ecfd366ccd3df94cbeed8f2526355f940dcec5`, clean `main`,
  synchronized with `origin/main` before editing.
- UI: XML Views, Material 3, ViewBinding; no Compose dependency or build feature.
- AGP 8.5.2; Kotlin 1.9.24; compile/target SDK 36; minimum SDK 24.
- Counts and full layout/source inventory: `baseline.json`; reproducible with
  `python3 scripts/design_inventory.py`. Counts include production comments and
  token definitions, all resource qualifiers, XML font-family resources and JSON.
- `baseline-sha256.json` records every production file's SHA-256 for subsequent
  protected-code/resource comparisons. Nothing has been deleted.
- `python3 -B -m unittest discover -s .github/scripts/tests -v`: 15 tests passed.
  These verify the existing design/runner contracts, not the requested new UI.
- `./gradlew assembleDebug`: exit 127; Gradle is not installed and the repository
  launcher falls back to a nonexistent Termux Gradle path. No Android build result
  is claimed. Build, lint and device verification need local tool setup or Actions.

## Implementation choice and failure analysis

Use native shared resource tokens and theme/widget styles, followed by explicit
screen migration. A runtime tree rewrite was considered and rejected because it
could overwrite feature-specific states and the protected reveal. Principal risks:
qualified resources overriding defaults, clipping ancestors hiding card shadows,
and runtime styling overriding migrated XML. Inventory all qualifiers and callers,
fix clipping in shared hosts, and verify runtime color/font bindings per screen.

## Pending

Phases 1–6, light/dark device verification, final shadow mapping, font/icon license
report, unmapped hue audit, small colored text contrast listing, deleted-resource
proof and `scripts/design_check.sh` PASS remain pending. No screen is marked migrated.

## Phase boundary and requirement conflict

**BLOCKED before Phase 1; no production files changed.** Settings exposes a working
font-choice feature (`SettingsActivity.showFontPickerBottomSheet`,
`bottom_sheet_font_picker.xml`, `FontManager.typeface/mediumTypeface`). Device mode
uses the platform typeface; default mode uses bundled fonts. The locked request
both mandates Poppins with no substitution/only one protected exception and requires
every feature to keep working. Retiring this preference changes a feature; retaining
it introduces another font exception. The product owner must choose precedence.
A clarification has been requested; no preference has been silently removed,
disabled or made ineffective.

| Phase | Status |
| --- | --- |
| 0 — baseline | DONE-VERIFIED: inventory, original-HEAD tag, static checks, direct-main push |
| 1 — root | BLOCKED on font-preference conflict; font download preflight succeeded |
| 2 — components | Pending |
| 3 — navigation/icons/transitions | Pending |
| 4 — every screen | Pending; all sources/layouts listed in `screens.md` |
| 5 — remove old | Pending; no files deleted |
| 6 — enforce | Pending; no design-check PASS claimed |

| Production metric | Before | At Phase 0 boundary |
| --- | ---: | ---: |
| Hex color occurrences | 496 | 496 |
| Literal dp occurrences | 3007 | 3007 |
| Literal sp occurrences | 181 | 181 |
| Style definitions including qualifiers | 279 | 279 |
| Drawables including qualifiers | 133 | 133 |
| Font resources including XML families | 8 | 8 |
| Layouts | 95 | 95 |
| Kotlin sources | 237 | 237 |

### Font preflight

Regular 400, Medium 500 and Bold 700 TrueType files downloaded successfully from
Google Fonts' official `google/fonts` repository, `ofl/poppins/Poppins-Regular.ttf`,
`Poppins-Medium.ttf`, `Poppins-Bold.ttf`. `file` identifies all three as Poppins
TrueType fonts. These files are in workspace scratch only, not bundled into the app
before the root phase. The official `ofl/poppins/OFL.txt` was fetched and read:

> Copyright 2020 The Poppins Project Authors (https://github.com/itfoundry/Poppins)
>
> This Font Software is licensed under the SIL Open Font License, Version 1.1.
>
> The OFL allows the licensed fonts to be used, studied, modified and
> redistributed freely as long as they are not sold by themselves.

The full copyright notice and license must be distributed with the bundled fonts
when Phase 1 resumes. No font substitution was made.

### Checks and limits

- `git diff --check`: PASS.
- `git diff pre-design-reset -- app/src/main`: empty, including all protected
  animation files and resources. Their behavior has not been modified.
- `git ls-remote origin refs/heads/main refs/tags/pre-design-reset` verified the
  Phase 0 commit on remote main and original HEAD as the remote tag.
- Actions run https://github.com/vishu762701/eve-mock-test-app/actions/runs/39186602486
  was checked after push: in progress. Backend and Firebase Functions jobs passed;
  Android build/lint/native gesture and three-button jobs were still running.
  This is baseline CI, not evidence of a migrated UI.
- Light/dark rendering, lint delta, final `assembleDebug`, new design enforcement,
  zero-old-token grep, shadow values and contrast audit have **not** been verified.
- Unmapped hues and every small system-colored text use on white remain unaudited;
  no color migration has occurred. No resource-deletion evidence is claimed.
- No new library added, no CI changes made, and no backend or feature changes made.

### Test on phone

1. Open Settings, tap the font preference, and choose Device font.
2. Confirm that the app currently switches typography after recreation.
3. Decide whether this feature should be retired for mandatory Poppins or retained
   as a second explicit exception. No redesigned APK is available at this boundary.

## Phase 1 — root implementation (supersedes the font blocker above)

The owner explicitly chose **remove Device font; Poppins everywhere**. The old
Settings font row and picker entry point are removed. The font lifecycle override
and font-version recreation loop are removed; all other settings remain intact.
Font resource cleanup stays in Phase 5 with the required deletion proof.

- Existing canonical day/night palette verified in `colors.xml`; new inset, text,
  hairline, track, Mono, shadow and glass tokens added in `design_colors.xml` with exact owner values
  (fractional alpha rounded to the nearest 8-bit channel).
- Bundled Regular 400 / Medium 500 / Bold 700 Poppins binaries and XML family;
  complete SIL OFL 1.1 included in APK assets and quoted in third-party notices.
- Locked sizes, type/line heights, shapes, elevation, motion durations and press
  scale defined; identical size resources supplied for sw360dp and sw600dp.
- Root Material 3/2, AppCompat/framework typography, palette, background, control,
  link, selection/cursor, ripple, shape and transparent system-bar defaults bound.
  `DesignTokens` resolves theme colors for custom Views without caching themes.
- Default button, card, chip, input, switch/control tint and ring styles installed.
  Shared runtime behavior and exact switch geometry remain Phase 2.
- Default modal themes are solid, 26dp, with API 31+ modal blur disabled.
- Depth mapping: elevation 9dp; intended y=6dp/blur=20dp. API 28+ ambient/spot
  shadow colors black 16% light / 60% dark, multipliers 1.0. API 24–27 platform
  black shadows use ambient/spot alpha .16 light / .60 dark. Platform shadow
  geometry approximates the requested blur; clipping remediation is Phase 2.
- Old per-screen styling is deliberately still pending; this phase does not
  certify every screen or remove any old resources.

Checks run before this phase's commit: all resource XML parsed; all three size
copies compared equal; TrueType OS/2 weights checked as 400/500/700; 15 Python
contract tests passed; `git diff --check` passed; protected toggle code/resources
have an empty diff. A native light/dark default-widget test was added. JVM UI
assertions changed only for the retired font setting, new theme token bindings
and explicitly disabled modal blur; all other checks remain enabled.

Local Gradle 8.7 and Android SDK tools/platform 36 were installed outside the
repository. `./gradlew assembleDebug` is running with the local tool PATH and
session HTTP proxy; the existing AGP 8.5.2 compileSdk 36 compatibility warning is
recorded, not suppressed. Build/lint/native results are pending at commit time.

Root resource verification caught and fixed an AppCompat `track` attribute name
collision (custom color attributes now use the `design` prefix), and verified the
framework's public `ambientShadowAlpha` / `spotShadowAlpha` attribute names using
the installed SDK 36's resources. Resource merge/link and Kotlin compilation now
pass locally. The next local Java step found missing `jlink` in the environment's
Java installation; a complete JDK is being installed outside the repository.
This does not establish a successful APK build yet. The first root CI run failed
on the fixed resource collision; a new run is required for the correction commit.

### Phase 1 local verification passed

A complete Temurin JDK 21 was installed outside the repository. With Gradle 8.7,
Android SDK 36 and the session proxy/trust store, the following completed with
`BUILD SUCCESSFUL`:

```
./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

- 281 JVM tests, 0 failures/errors/skipped.
- Lint: 0 errors/fatal issues; warning inventory remains for later cleanup.
- Debug APK and instrumentation APK assembled successfully.
- The native Settings fixture no longer references the owner-retired font field.
- Existing exact system hue/background tokens remain in `colors.xml`, since those
  values already matched the owner's spec. This preserves existing color audits;
  new roles are in `design_colors.xml`. No color assertion was weakened.
- Font identity checks parse both legacy family and typographic family names from
  the actual TrueType `name` table; Medium correctly has legacy family
  `Poppins Medium` and typographic family `Poppins`.
- Material elevation tinting is disabled so a 9dp card retains exact `surface`.
  The native test also checks its rendered center pixel in both themes.
- Root implementation is committed; hosted light/dark native checks remain pending
  for the correction commit. Full screen migration and design_check PASS are not
  claimed. No resources have been deleted.

Phase 2 diagnosis (before changes): 89 CardView declarations inspected; 9 have
ancestors retaining default child/padding clipping (Result's four summary cards,
four Premium cards and the palette-circle host). ViewPager2's internal RecyclerView
also requires runtime host handling. Existing explicit elevations, custom surface
helpers and zero-margin rows must be migrated through shared components, not
border/alpha workarounds.

## Phase 2 — shared components

Implementation and local checks complete; hosted native verification is pending.

- Native shared cards and inflater release ancestor clipping for existing XML cards
  and runtime scroll/pager/recycler hosts. Existing bindings remain compatible.
- Segmented strips are 40dp with 3dp padding / 34dp selected segment, Mono fill and
  14/20 Poppins regular/bold labels. Overlay-host helper reserves 40dp and brings
  the strip above content without introducing a tab shadow.
- Native switches retain checked/drag/click behavior with exact 51x31dp tracks,
  27dp white thumbs and a minimum 48dp target. Initialization refreshes checked
  drawable state, including switches declared checked in XML.
- Shared inputs keep password/dropdown/error behavior and use 44dp field height,
  17/24 Poppins, 16dp horizontal padding, capsule / 20dp multiline geometry,
  1.5dp focus/error rings and 14/20 red helper text.
- Solid dialog/sheet helpers preserve caller listeners. Dialog type roles and
  bottom-sheet overlays retain the root font/palette. Cards remain solid surface.
- Medium glass uses existing Apache-2.0 BlurView 2.0.6 / RenderEffect on API 31+
  with foreground children excluded. Radius is 14dp at pixel scale 1; tint is 55%,
  fallback 94%, hairline 0.5dp. Hardware/device visual proof waits for chrome/screen
  integration; below-31 and software paths are explicit in the component.
- Primary/secondary/destructive/disabled buttons, pills, icon-only buttons, inner
  tiles, rows, multiline/dropdown inputs, toasts and badges have shared token styles.
  Chip text padding is zero inside the explicit 14dp chip padding; icons are 24dp.
- One native state-list animator defines .96 press in 100ms / release in 140ms,
  ease-out, and honors system animation scaling. No extra alpha/elevation effects.
- Shared parent TouchDelegates expand small interactive regions to 48dp, expose
  accessibility regions on API 29+, keep existing listeners and clean up on detach.
- New native tests exercise switch pixels/geometry/callbacks, field validation,
  tab selection/height, modal inheritance, clipping, inflater binding compatibility,
  expanded hit targets and delegate cleanup in addition to the root light/dark test.
- The shared verification fixture now uses the new production components/styles.

Local `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` succeeds;
281 JVM tests pass, lint has 0 errors/fatal issues, and both APKs assemble. Static
Python contracts and `git diff --check` are run before commit. Existing native CI
run 39189722872 built/linted/tested successfully but failed four appearance checks:
integer sp rounding, old 1dp borders, an old switch contrast threshold and red error
helper contrast. Those contracts are now aligned to the owner's exact tokens;
mandatory colors are never darkened to satisfy old contrast thresholds. The error
helper "Please enter your message" measured 3.2742:1 on bg_app (not white).
Full system-colored small-text-on-white inventory is still pending screen migration.

No production screen is yet certified fully migrated. Phases 3–6 and all old-resource
cleanup remain pending; no old resource files have been deleted and no final
`design_check.sh` PASS is claimed. Protected toggle core files/resources still have
an empty diff. Shared components do not alter backend/scoring/data/navigation logic.

### Pending decision before navigation

Android 7/API 24–25 cannot switch navigation-button icons to dark. Core 1.13.1
explicitly documents that the appearance API has no effect below 26. Transparent
light bg_app and readable white OS buttons cannot both be satisfied there.
The owner has been asked to permit an Android-7-only dark navigation region or
raise minimum support to API 26. No support/SDK change or extra exception has been
implemented while the answer is pending. The font question is already resolved.

### Current production counts (same baseline scope)

| Metric | Before | After Phase 2 |
| --- | ---: | ---: |
| hex_color_occurrences | 496 | 520 |
| literal_dp_occurrences | 3007 | 3106 |
| literal_sp_occurrences | 181 | 223 |
| style_definitions_including_qualifiers | 279 | 327 |
| drawable_files_including_qualifiers | 133 | 138 |
| font_files_including_xml_families | 8 | 11 |
| layout_files_including_qualifiers | 95 | 95 |
| kotlin_files | 237 | 251 |

Added required tokens/qualifier copies are included; these are not claims of old-resource cleanup.

### Phase 2 hosted verification follow-up

Both hosted native modes exposed three issues: a programmatic test passed generic
LayoutParams where Material TextInputLayout requires LinearLayout.LayoutParams;
an inherited outline assertion still expected the retired control-outline color
instead of the root hairline token; and the premium-active card's legacy yellow
fill reduced locked secondary-text contrast to 4.3556. The fixture now uses the
native parameter type, the assertion follows the root token, and the premium card
uses the required solid surface. No callbacks, premium state, or data changed.
Local and hosted re-verification follows this correction.

### Android 7 navigation decision resolved

The owner explicitly allows a black navigation-area fallback only on Android 7
(API 24–25) in light mode. Keep minSdk 24. All other supported versions retain
the requested transparent navigation bar and appropriate icon contrast. This
resolves the question above; implementation belongs to Phase 3.

### Phase 2 second hosted follow-up

Run 39195476165 passed build/lint/JVM but exposed two later native assertions.
Material 1.12.0 deliberately assigns its boxBackground object to the EditText
(`updateEditTextBoxBackgroundIfNeeded` / `getEditTextBoxBackground` in official
TextInputLayout.java). The native contract now asserts object identity, proving
there is one shared field box rather than incorrectly demanding a null drawable.
The legacy primary-button selector still paired dark Mono's white fill with
white legacy text. Its enabled text now resolves on_accent and its disabled fill
and text resolve track/text_secondary; callbacks and enabled-state logic stay
unchanged. This fixes the home-feedback Reply label's 1:1 dark contrast.

## Phase 2 final evidence

Run [39196346487](https://github.com/vishu762701/eve-mock-test-app/actions/runs/39196346487)
completed successfully for build/lint/JVM, native gesture, native three-button,
backend, functions and verified APK publishing at
`bf97f7f2f5999bb700ae8436c18ca062afc0d2df`. Actual checkout/build/upload logs and
artifact metadata agree on this SHA. Downloaded `Eve-debug-apk` ZIP SHA-256 matches
GitHub `935a704d65a9cd9045ecb86bd5ab7069e668c1f83457a626de0d7f20c6701223`;
its `app-debug.apk` passes ZIP integrity and APK v2 signature verification. No
additional delivery commit or rerun was needed.

## Phase 3 implementation

- Transparent status/navigation bars over bg_app; owner-approved black navigation
  fallback on API 24–25 light mode only. Existing reveal icon timing is unchanged.
- Normal push/pop animations: horizontal slide + fade, 320ms, curve
  (0.2,0.8,0.2,1). Intentional auth redirect and protected theme no-animation
  handoffs remain unchanged.
- 72 icon resource IDs now use MIT Tabler v3.49.0 outline paths, 24-unit grid,
  2.75 stroke, round caps/joins. Source mapping and full bundled license provided.
  Runtime artwork/Lottie screen migration and unused launcher PNG deletion belong
  to the following screen/cleanup phases; they are not falsely marked removed.
- Shared toolbar observes scrolling without replacing feature listeners; it uses
  transparent chrome at top and Medium glass while content is scrolled. Existing
  large headings move below the back/action row and use the locked title role.
- Home bottom bar uses the shared live glass capsule, 54dp height, 12dp side
  margins, 10dp bottom margin, 24dp icons and existing inset/action dispatch.
- Image buttons share the icon-only 48dp target with 12dp internal padding (24dp
  drawing), and the shared press animator.
- Telegram menu container uses shared backdrop glass. setupThemeCard and the
  entire setupListeners function compare byte-identical to the previous commit;
  ThemeManager, ThemeSwitchAnimator, sun/moon vectors and reveal Lottie have no diff.
- Added native PixelCopy verification of 55% real backdrop blur, a softened source
  edge, untinted/sharp foreground children and live source updates in both themes.
  Compilation is verified locally; compositor execution awaits hosted native CI.

Local assembleDebug/testDebugUnitTest/lintDebug/assembleDebugAndroidTest and Python
contracts are checked before the phase commit. Hosted checks remain pending until
that commit is pushed. Full screen migration and cleanup/design_check are pending.

Header Save/Create/Reattempt actions retain MaterialButton APIs and IDs but use
icon-only drawing. Original/dynamic labels remain available as accessibility
labels and long-press tooltips. Added plus/save outline sources from the same
Tabler release. No business callback, eligibility check, or click wiring changed.

Phase 3 caller audit found EveNavigationHelper's source-bound scale-up options
overriding the theme window animation. Its animation options now use the same
shared slide/fade resources; intents, requestCode dispatch and native back-stack
finish calls are unchanged. This necessary follow-up is verified before push.

Phase 3 hosted run 39214474736 passed build/lint/JVM and the new real compositor
blur test, but found two obsolete native fixture assumptions: icon-only null
backgrounds were cast to ColorDrawable, and the popup handoff test waited for a
retired whole-window blur animator event. Bare-action checks now accept no paint
or transparent paint. Handoff coverage now taps a real partially-entered BlurView
frame and checks its actual renderer is disabled at cancellation. All original
reveal event ordering, single callback, measured layers, frame fences, ownership,
scroll restoration and cleanup assertions remain. Detach explicitly disables the
shared blur renderer. No protected production animation/listener code changed.
