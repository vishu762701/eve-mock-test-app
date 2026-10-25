# Floating tabs — source comparison and verified scope

Source: original Telegram Android source ZIP SHA-256 `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`, matching `EVE_PROVENANCE.md`. This report distinguishes source numeric parity from Android/EVE adaptations.

| Property | Current EVE | Telegram (relative to `TMessagesProj/src/main/java/org/telegram/`) | Action |
|---|---|---|---|
| Height | **56dp**, fixed on outer `EveBottomPillBar` | `ui/DialogsActivity.java:291`, 56dp | Fixed outer height; previously `WRAP_CONTENT` expanded through the `MATCH_PARENT` glass child |
| Outer margins | 8dp bottom + 8dp left/right, plus Android navigation inset | `ui/DialogsActivity.java:292–293`, 8dp and 72dp total allocation | Apply 8dp side margins, keep system bar inset |
| Max width | 344dp **including the glass background** | `ui/MainTabsActivity.java:309–312`, 328 + 8×2 = 344dp; `ui/MainTabsLayout.java:64–66` | Cap outer pill and inner tabs; previously only inner tabs capped |
| Dynamic widths | Text-measured, weighted widths; minimum available strip 320dp | `ui/MainTabsLayout.java:68–151` | Replace equal-width division with source's three-pass weighted sizing |
| Text-size passes | 12 / 12 / 10dp | `ui/MainTabsLayout.java:46–47` | Preserve |
| Per-tab width paddings | 16 / 8 / 4dp | `ui/MainTabsLayout.java:46–47,78` | Preserve |
| Tab label weights | Default Medium 500; selected 800 | `ui/Components/glass/GlassTabView.java:87–96,233–238` | Correct prior 700 claim: bold() resolves to bundled Medium; selected system ExtraBold is a documented font substitute |
| Label text position | 28.33dp from top | `ui/Components/glass/GlassTabView.java:95–96` | Preserve |
| Main tab icon | 24×24dp at top 4dp | `ui/Components/glass/GlassTabView.java:400–407` | Preserve |
| Avatar | 22×22dp, radius 11dp, top 5dp | `ui/Components/glass/GlassTabView.java:414–428` | Preserve |
| Selected indicator | Capsule scale 0.6 to 1 and 0.09× alpha | `ui/Components/glass/GlassTabView.java:150–165` | Preserve source math |
| Selection motion | 320ms decelerated | `ui/Components/glass/GlassTabView.java:65–67` | Preserve |
| Press/reselect | Click selects; active click invokes scroll-to-top | `ui/MainTabsActivity.java:334–348` | Preserve four EVE destinations and reselect callback |
| Color roles | `glassTabUnselected`, `glassTabSelected`, `glassTabSelectedText` from EVE's resolved palette | `ui/Components/glass/GlassTabView.java:407–409` | Keep semantic theme-dependent mapping in Day/Night; never hardcode Telegram's theme colors |
| Liquid-glass API gate | API 33+; non-shader drawable fallback on earlier Android | `ui/Components/blur3/LiquidGlassEffect.java:14` | Preserve |
| Glass radius | 28dp, based on 56dp height | `ui/MainTabsActivity.java:366–369` | Preserve |
| Vertical padding | 0 inside 56dp tap targets | `ui/MainTabsActivity.java:311` uses 12dp top/bottom around its wrapper layout | Intentional Android accessibility adaptation: EVE retains ≥48dp actual touch height; copying 12dp internal padding would clip labels/taps |
| Destination count | Four: Home, History, Settings, Profile | `ui/MainTabsActivity.java:314–319` uses five including Contacts/Calls | Intentional EVE-specific navigation requirement |

Additional source corrections: `MainTabsLayout.java:141` uses `Math.round` for each
weighted tab width; EVE now uses the same operation. `AndroidUtilities.java:2701–2705`
uses ceiling for density conversion, now matched by the tab leaf and sizing helper.
`GlassTabView.java:152,158` uses visual width for the selector; EVE now does too.
Source leaves TextView font padding at its default; EVE no longer disables it.

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
