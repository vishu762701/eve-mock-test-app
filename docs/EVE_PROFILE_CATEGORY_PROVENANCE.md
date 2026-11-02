# EVE profile category selector — source mapping (2026-10-31)
- Official revision: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15` (version 13.1.1), repository Git tree `cc3c6ccf48e4167fdec764dfd04e765043f55093`.
- Source archive `Telegrammaster(5).zip` SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`. This particular ZIP has **no** `EVE_MANIFEST.sha256`; `THIRD_PARTY_NOTICES.md` records an independent canonical Git-tree identity check. It must not be relabeled as an older manifested archive.
- Upstream `TMessagesProj/src/main/java/org/telegram/ui/Cells/RadioCell.java:55-76`: 16sp label, 20dp indicator, end-anchored radio and palette-driven checked color. Upstream `RadioCell.java:83-108`: 50dp row and radio selection; `RadioCell.java:132-137`: accessibility radio semantics. Upstream file blob SHA-1 `11dfa01a0734c271cbf0701faceebb73234f8683`.
- Existing EVE `TelegramRadioIndicator.kt` is GPL-noticed as Telegram RadioButton-derived. New `TelegramProfileRadioCell.kt` combines it with the original RadioCell sizing and placement pattern. Classification: **ADAPTED / TELEGRAM-DERIVED**, not an upstream byte-for-byte `RadioCell` port. Deviations: EVE palette accent instead of `Theme.key_radioBackgroundChecked`, EVE text labels, Android ripple, no optional divider, two-line accessible text, 50dp default minimum with content-height growth under large font scales, 20dp inset. This variable-height behavior deliberately differs from the fixed upstream 50dp measurement.
- EVE `ProfileActivity.kt` now keeps old category visible until authenticated `GET /profile` readback equals submitted choice. Error/failed readback retains prior state; touches are serialized while mutation is in flight. The backend's authenticated category persistence and privacy remain authoritative.
- Verification: JVM structural test `ProfileCategorySourceContractTest`, native inflation `ProfileCategoryRadioNativeTest` (device execution needs FULL CI). Real matched Telegram/EVE device screenshots and interrupted animation evidence remain **UNKNOWN**, not claimed as exact.

## Live-palette repair (owner exception authorized; carrying CI required)

Base main `372b108a0b1b1c957566c53eeb7fd69b139e7a3e` exact-SHA FAST
[41475078791](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41475078791)
SUCCESS. Actual canonical theme traversal updates label ink but never calls the
existing category indicator's `setColors` after attach. A native Skia test of the
actual row/drawable reproduces stale unselected-ring pixels after Night palette;
selected-dot preservation control already passes. Proposed minimal repair exposes
existing palette application as updatePalette and routes the existing theme
traverser into it. No new renderer, selection state, style, dimensions, backend or
animation algorithm. Existing EVE accent/secondary adaptation remains intentional;
this repair does not pretend those are upstream radioBackground* role values.

Upstream theme mutation path: `ActionBar/ThemeDescription.java:649-655` updates and
invalidates an existing RadioButton's background/checked colors. Combined with
`Cells/RadioCell.java:68-77`, this maps to EVE's source-derived indicator inside
`TelegramProfileRadioCell.kt:updatePalette`, reached from
`TelegramThemeApplier.kt:applyToViewTree`. ADAPTED callback integration; unchanged
source algorithm is not newly transplanted. Official revision remains full SHA
above. Source SHA-256: RadioCell.java
`9a76e0956868f187b22fda5eaf971998d353f1d35e184c316428d96b096cfd94`,
ThemeDescription.java
`b369eef1af112be82eb682da53c7c8f6eb77c64a563aa82870340d76167b51cf`.

Existing category feature already has TWO verified successful implementation
commits: feb9c8887564506fb7b953bfc5c0d2d2b46096ba / FAST 41163967661 and
14a629726be44bac3381c107324778c9ab919fc7 / FAST 41165900930. Both live results
rechecked SUCCESS. ENGINEERING_STANDARD.md section 2 requires an owner exception
before another successful implementation commit for this feature. The proposed
repair is prepared/tested separately for review; integration remains NOT MET until
that decision and its own exact-SHA FAST. Continue independent mission work.

Proposed repair final local checks: serial `./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64` PASS; 811 tests / 188 suites, zero failures/errors/skips.
Two native row/indicator tests PASS (before repair: stale unselected ring fails,
selected-dot preservation control passes). Python CI helpers 59/docs/claims/diff
PASS. This is LOCAL-ONLY review evidence, not main/CI acceptance. Device/reference
matrix remains OPEN. No owner exception or third implementation commit assumed.

Owner explicitly authorized ONE additional repair commit: "Haan, ek additional
repair commit authorize hai". Reviewed patch SHA-256
236f8f9716ba51d0b14bbf7dec20a688cd63328ebac0227bfa03a6d11699e757 applied cleanly
after successor question-response main 381a76a610a3f1847f59a4c48939d7f2e4e6aea8
FAST 41479914039 SUCCESS. Source/test bytes match the reviewed repair; documentation
records this new authorization. This is the single approved third category repair
commit, not an exception for further iterations. Own exact-SHA FAST remains required.
