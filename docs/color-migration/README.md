# Original-source Light/Night color migration

## Provenance boundary

Owner supplied `EVE_TELEGRAM_ORIGINAL_COLOR_SOURCE_PACK.zip`, independently verified
SHA-256 `4b37c4c6749c5347cafdc16c6a1cbb6371599e09222e40be983135400f4fe469`.
ZIP CRC, all 57 source-file SHA-256/size/CRC32 rows and exact membership passed.
`source-role-contract.json` retains the complete per-file manifest and role definitions
with original paths/lines. Extracted source lives outside the git worktree.
The pack attests derivation from original archive SHA-256
`0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`.
That complete archive was NOT received or independently hashed here; its CRC claim
is the source-pack author's attestation. No upstream revision was substituted and
no original `EVE_MANIFEST.sha256` is claimed.

## Root causes and authority

Old XML resources, Material theme attributes, .attheme assets, built-in accent
transforms/persisted preset IDs, runtime component setters, window backgrounds,
and independently shaded glass all affected the final image. An XML-only change
left old runtime overrides active. `active-color-inventory.tsv` enumerates color
producer/consumer lines including qualified resources, drawables, source paints,
selectors, preferences, backend and Functions. Semantic exceptions are classified;
image/QR/branding/avatar data is not recolored.

Precedence: persisted Light/Night/System -> fixed original-source Light/Night
(or an explicitly selected imported theme) -> direct active key -> ONE active
fallback key -> original default -> documented AA foreground accommodation ->
resource/style and runtime consumers. Static resource defaults are a serialization
of the same role contract and are checked against runtime resolution in both modes.
No built-in accent transform participates. All old noncustom IDs/accents migrate to
day/0 or night/0; explicit mode and valid private imported files/IDs survive.
Preview, Reset, Cancel, Apply and process reload preserve this boundary.

Light is explicitly Telegram's original DEFAULT branch, not the separately
accented Telegram Day preset. The retained EVE `day.attheme` filename is a generated
projection of ThemeColors defaults, not a claim of verbatim upstream Day asset.
Night is the original byte-identical `night.attheme` from the verified pack.

## Source-to-EVE role map

All upstream paths below are relative to `TMessagesProj/src/main/`.
Exact ARGB definitions/hashes are in `source-role-contract.json`.

| Role | Original definition/resolution | EVE consumer | Fidelity |
| --- | --- | --- | --- |
| canvasBackground | ThemeColors.java:133 FFF1F1F3; ProfileActivity.java:4227,15286; night.attheme:61 FF000000 | bg_app, Engine.windowBackground, SystemBarHelper | original role |
| primarySurface | ThemeColors.java:79 FFFFFFFF; night.attheme:235 FF181819 | surface, Material styles/cards, Engine.surface | original role |
| modalSurface | ThemeColors.java:24 FFFFFFFF; night.attheme:137 FF1E1E1E | dialog_surface, DesignSurfaces and dialog/sheet styles | original role |
| inset/grouped canvas | same canvas role | surface_inset | compatibility alias; no invented secondary surface |
| primaryText | ThemeColors.java:16,107 FF1A1D21; night.attheme:88 FFFFFFFF | text_primary, typography and runtime text roles | original role |
| secondaryText | ThemeColors.java:99 FF808384; night.attheme:314 6EFFFFFF | text_secondary / accessibleForeground | EVE AA adjustment: minimally darken Light; minimally raise Night alpha over canvas/surface/modal |
| control accent | ThemeColors.java:14,502; night.attheme:158 FF229AF0 | accent, buttons/loading/controls | source role; on_accent uses contrast-maximizing black, an EVE accessibility pairing |
| tabGlassTarget | ThemeColors.java:874 FFFFFFFF; night.attheme:490 FF232324 | glassTargetMainTabs | target input, NOT an opaque fill/pixel promise |
| tabSelectedIcon/Text | ThemeColors.java:876/877 FF1A91E6/FF0D7FCF; Theme.java:3709–3710,9024–9052 + night.attheme:158 -> FF229AF0 for both | raw palette glass roles; GlassTabView paints | source resolver; foreground-only AA adjustment over coated backing roles, raw overlay color retained |
| tabUnselectedIcon/Text | ThemeColors.java:878 FF1A1D21; Theme.java:3711 + night.attheme:88 -> FFFFFFFF | GlassTabView | original role |
| selectedOverlay | GlassTabView.java:150–164; MainTabsLayout.java:283–320; multAlpha(selected,.09*decelerated factor) | existing tab selection drawing | preserved formula and geometry |
| glass coating | blur3/drawable/color/impl/BlurredBackgroundProviderImpl.java:21–36,304–331 | solveSrcColor(surface,target,.76) in DesignGlassLayout | verified non-liquid input/formula; EVE existing platform BlurView backend |
| systemBarStyle | transparent edge-to-edge bars; source canvas is the backing role | existing SystemBarHelper + ThemeManager target background | native EVE adaptation; API24–25 black nav-button backing retained for readability |
| profile inner tab family | SharedMediaLayout.java:4404,6977–6982; ThemeColors.java:549–552; Theme.java:3644–3647 | DesignSegmentedTabs | exact filled-pill counterpart NOT FOUND. EVE keeps its existing geometry, accent/onAccent behavior; it is not glass/profile-underline fidelity |

## Glass dependency and renderer limits

Traced GlassTabsView/MainTabsLayout -> GlassTabView colors; provider builder ->
themed color provider -> BlurredBackgroundDrawable -> captured color/bitmap/render
node source, shadow/stroke and optional LiquidGlassEffect/GlassEngine. The prior
EVE RuntimeShader sampled its OWN coated view and used an unexplained .55 alpha;
that was not a verified Telegram liquid implementation. The bar now uses EVE's
existing backdrop-only BlurView (API31+ hardware) with the source non-liquid .76
coating ONCE, plus existing source edge colors. It preserves destinations, hit
geometry, selection callbacks, Lottie and avatar data. API24–30/software fallback
is explicitly a translucent coating without blur. Telegram liquid refraction,
renderer-specific shadows/noise and identical live backdrop optics are NOT claimed.

Normal tab text is adjusted only as far as needed for 4.5:1 across expected coated
canvas/surface/target backdrops; icons use 3:1. Arbitrary photo backdrops cannot
provide a universal contrast/pixel-parity guarantee. The raw source roles and .09
overlay remain inspectable. Generic chrome uses the top-panel target independently.

## Retained exceptions and cleanup

Semantic success/error/warning/score/billing/category hues remain EVE-specific;
`system_*`, `retained_hue_*`, and semantic `design_role_*` are not claimed as Telegram.
Transparent masks, QR black/white pixels, scrims/shadows, contrast endpoints and
API24–25 navigation backing remain functional primitives. Night pressed/focus
track uses source actionBarDefaultSelector (16FFFFFF) rather than invisible black.
Filled segmented pills retain accessible EVE accent/onAccent pairing because the
original profile tab is an underline, not a filled segment.

Deleted only retired app-private built-in preset assets arctic/darkblue/bluebubbles
and the now-unreferenced liquid_glass_shader.agsl. Evidence: engine catalog and UI
selection normalization expose only day/night/custom; searches of XML/Kotlin/Java,
assets, tests, build/workflow code found no remaining runtime loader for those four
paths. Public resource names remain as migrated role aliases; none retain their
old default behavior. Imported custom files are app-private `custom_*.attheme`
and are never deleted by the migration. Historical source/provenance is retained.
`resource-changes.tsv` records old/new values for every changed default resource.

## Evidence boundary

Source values differ from JPG estimates (Light F1F1F3 versus sampled F0F0F0;
Night surface 181819 versus compressed 181818). No screenshot-derived replacement
hex or same-build/device pixel parity claim is made. Actual commands, exact-SHA
FAST/FULL outcomes, native captures and remaining limitations are reported at delivery.

The old semantic base hues remain fill/icon data. Normal-text consumers now use
separate `system_*_text` shades, computed by the same minimum AA transformation
against canvas, primary/modal surfaces and their semantic containers. The native
student rendering matrix no longer exempts low-contrast semantic labels. Icons
using the general accent receive a separate minimum-3:1 `accent_icon` foreground;
the source control fill and raw palette accent remain unchanged. Main-tab foreground
AA considers both black and white backdrop extremes after source coating/overlay,
not only a stable source canvas. The source .09 selector remains raw and unchanged.
Primary content panels and modal panels use separate drawables with identical
shape geometry; values-v31 dialog overrides are migrated with the base theme.

Destructive labels now pair the retained semantic red fill with black, the higher-contrast endpoint. Numeric sizes, control geometry and callbacks are unchanged. The native shared-controls matrix enforces 4.5:1 without old exact-hue exceptions.
