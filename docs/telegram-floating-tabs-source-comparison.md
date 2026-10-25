# Floating tabs — source comparison and verified scope

Source availability: Telegram source is not stored in this repository; the owner supplies it per session. See `SOURCE_OF_TRUTH.md` for attachment/hash requirements and `AGENTS.md` for the missing-source STOP rule.

Source: original Telegram Android source ZIP SHA-256 `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`, matching `EVE_PROVENANCE.md`. This report distinguishes source numeric parity from Android/EVE adaptations.

| Property | EVE file:line | Telegram file:line | Value | Action |
|---|---|---|---|---|
| Height | `app/src/main/java/com/eve/app/ui/navigation/EveNavigationHost.kt:42` | `ui/DialogsActivity.java:291`, 56dp | **56dp**, fixed on outer `EveBottomPillBar` | Fixed outer height; previously `WRAP_CONTENT` expanded through the `MATCH_PARENT` glass child |
| Outer margins | `app/src/main/java/com/eve/app/ui/navigation/EveBottomPillBar.kt:95` | `ui/DialogsActivity.java:292–293`, 8dp and 72dp total allocation | 8dp bottom + 8dp left/right, plus Android navigation inset | Apply 8dp side margins, keep system bar inset |
| Max width | `app/src/main/java/com/eve/app/ui/navigation/EveBottomPillBar.kt:177` | `ui/MainTabsActivity.java:309–312`, 328 + 8×2 = 344dp; `ui/MainTabsLayout.java:64–66` | 344dp **including the glass background** | Cap outer pill and inner tabs; previously only inner tabs capped |
| Dynamic widths | `app/src/main/java/com/eve/app/ui/navigation/MainTabsLayout.kt:114` | `ui/MainTabsLayout.java:68–151` | Text-measured, weighted widths; minimum available strip 320dp | Replace equal-width division with source's three-pass weighted sizing |
| Text-size passes | `app/src/main/java/com/eve/app/ui/navigation/MainTabsLayout.kt:30` | `ui/MainTabsLayout.java:46–47` | 12 / 12 / 10dp | Preserve |
| Per-tab width paddings | `app/src/main/java/com/eve/app/ui/navigation/MainTabsLayout.kt:31` | `ui/MainTabsLayout.java:46–47,78` | 16 / 8 / 4dp | Preserve |
| Tab label weights | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:116` | `ui/Components/glass/GlassTabView.java:87–96,233–238` | Default Medium 500; selected 800 | Correct prior 700 claim: bold() resolves to bundled Medium; selected system ExtraBold is a documented font substitute |
| Label text position | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:123` | `ui/Components/glass/GlassTabView.java:95–96` | 28.33dp from top | Preserve |
| Main tab icon | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:78` | `ui/Components/glass/GlassTabView.java:400–407` | 24×24dp at top 4dp | Preserve |
| Avatar | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:96` | `ui/Components/glass/GlassTabView.java:414–428` | 22×22dp, radius 11dp, top 5dp | Preserve |
| Selected indicator | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:273` | `ui/Components/glass/GlassTabView.java:150–165` | Capsule scale 0.6 to 1 and 0.09× alpha | Preserve source math |
| Selection motion | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:213` | `ui/Components/glass/GlassTabView.java:65–67` | 320ms decelerated | Preserve |
| Press/reselect | `app/src/main/java/com/eve/app/ui/navigation/MainTabsLayout.kt:97` | `ui/MainTabsActivity.java:334–348` | Click selects; active click invokes scroll-to-top | Preserve four EVE destinations and reselect callback |
| Color roles | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:194` | `ui/Components/glass/GlassTabView.java:407–409` | `glassTabUnselected`, `glassTabSelected`, `glassTabSelectedText` from EVE's resolved palette | Keep semantic theme-dependent mapping in Day/Night; never hardcode Telegram's theme colors |
| Liquid-glass API gate | `app/src/main/java/com/eve/app/ui/navigation/EveBottomPillBar.kt:165` | `ui/Components/blur3/LiquidGlassEffect.java:14` | API 33+; non-shader drawable fallback on earlier Android | Preserve |
| Glass radius | `app/src/main/java/com/eve/app/ui/navigation/EveBottomPillBar.kt:217` | `ui/MainTabsActivity.java:366–369` | 28dp, based on 56dp height | Preserve |
| Vertical padding | `app/src/main/java/com/eve/app/ui/navigation/MainTabsLayout.kt:42` | `ui/MainTabsActivity.java:311` uses 12dp top/bottom around its wrapper layout | 0 inside 56dp tap targets | Intentional Android accessibility adaptation: EVE retains ≥48dp actual touch height; copying 12dp internal padding would clip labels/taps |
| Destination count | `app/src/main/java/com/eve/app/ui/navigation/EveBottomPillBar.kt:45` | `ui/MainTabsActivity.java:314–319` uses five including Contacts/Calls | Four: Home, History, Settings, Profile | Intentional EVE-specific navigation requirement |

Additional source corrections: `MainTabsLayout.java:141` uses `Math.round` for each
weighted tab width; EVE now uses the same operation. `AndroidUtilities.java:2701–2705`
uses ceiling for density conversion, now matched by the tab leaf and sizing helper.
`GlassTabView.java:152,158` uses visual width for the selector; EVE now does too.
Source leaves TextView font padding at its default; EVE explicitly enables it locally to override its global theme
(`themes.xml:110`, `design_widgets.xml:6`). Source `res/values/styles.xml:88–90`
inherits `android:Widget.Holo.Light.TextView` and does not disable font padding.

## Failure and proof

The initial Home screenshot showed the pill at the top with a dark, sharp content layer. The root cause was the outer pill's `WRAP_CONTENT` height combined with its `MATCH_PARENT` glass background. A real native geometry assertion failed in both gesture and three-button modes before the fixed 56dp constraint. The expanded glass child could fill the Home viewport and tint/dim content.

The native regression now checks 56dp outer height, exact background height containment, bottom-of-window gravity and actual bottom coordinates after navigation-inset dispatch, as well as 344dp max width. FULL device runs in both navigation modes are required before calling the fix runtime-verified.

## Preserved behavior

Telegram Day/Night circular reveal, all Lottie assets/playback, server-side scoring, drawer behavior, Firebase authentication, four destinations, API <33 glass fallback, and Android system navigation bar remain unchanged.

The exact Telegram native render-node blur implementation is not claimed to be bit-identical to the EVE RuntimeShader-backed coating; production parity of refraction across hardware is a separate visual acceptance check.

## Explicit remaining deviations

`AndroidUtilities.java:253` maps selected labels to bundled `fonts/rextrabold.ttf`.
EVE retains the existing platform 800 substitute on API 28+, and Bold on API 24–27;
only the unselected Medium font uses the exact bundled source bytes. Selected glyph
parity is not claimed. Settings keeps EVE's existing Lottie asset/playback/triggers;
no upstream RLottie asset was substituted. Badges, Telegram contacts/calls,
long-press account/folder menus and gesture-driven tab override are not EVE features.
Native shader/refraction equivalence remains a visual-review limitation.
