# Mock-test radio indicator provenance and compatibility

Official repository: https://github.com/DrKLO/Telegram
Verified revision: `0aae959806c57be8ad457980f2a5c6277ff18635`.
Fetched original bytes from that revision's raw GitHub URLs; both match the
owner's original reference hashes. No reference ZIP is present in this workspace;
no whole-ZIP or internal-manifest verification is claimed.

| Official path under TMessagesProj/src/main/java/org/telegram/ui/ | SHA-256 |
| --- | --- |
| Components/RadioButton.java | `2422b0fecc33129021ec0654f66f0ec3dd5be78c1191890a5ff25b0758abb61b` |
| Cells/RadioButtonCell.java | `7745a9d5ac7a9e6f52e5089ab8b1c221f16db06c5edac5225eb6f772a610c0c2` |

Both original headers grant GNU GPL version 2 or later and credit Nikolai
Kudashov, 2013–2018. Bundled `assets/licenses/Telegram-GPL2.txt` preserves the
upstream license text; the derived Kotlin source is supplied in this repository.
No changes to combined-binary licensing clearance or production release guards.

## Before-state matrix

| Existing EVE behavior | Verified source behavior | Relevant existing location | Necessary delta |
| --- | --- | --- | --- |
| Static check/circle selector, native tint | Progress-driven ring/fill/erase | TelegramRadioButton.kt (former lines 36–40); RadioButton.java:159–188 | Dedicated Drawable, no native tint |
| No indicator checked animator | 200 ms progress, default ObjectAnimator easing | RadioButton.java:122–125 | Same timing/easing with ValueAnimator |
| Listener detached during bind, jump during reset | Bind checked without animation | QuestionAdapter.kt:89–142; RadioButtonCell.java:84–92 | Preserve adapter and honor jump |
| Generic runtime radio tint overwrites state colors | Two resolved indicator colors | TelegramThemeApplier.kt radio case; RadioButton.java:100–113 | Specific answer-radio palette handler |

## Source mapping

| EVE location | Telegram location | Original behavior/value | Fidelity or adaptation |
| --- | --- | --- | --- |
| TelegramRadioIndicator.kt paints | RadioButton.java:33–56 | AA paints, dp(2) stroke, CLEAR eraser | Same configuration; per-instance paints replace static shared paints |
| TelegramRadioIndicator.kt size | RadioButtonCell.java:39–47 | dp(20) indicator in 22dp cell slot | Same indicator size; retain EVE 24dp icon slot and existing insets to preserve text/layout |
| TelegramRadioIndicator.kt setColors | RadioButton.java:100–113 | independent unchecked/checked colors | EVE textSecondary/onAccent tokens and runtime palette replace Telegram Theme keys |
| TelegramRadioIndicator.kt setChecked | RadioButton.java:116–150 | 200 ms, default easing, immediate when unattached/nonanimated | ValueAnimator replaces reflected ObjectAnimator property; cancel previous transition before reversing; disabled animation settles immediately |
| TelegramRadioIndicator.kt draw | RadioButton.java:159–188 | progress / .5 then 2 - progress / .5, integer RGB deltas, radius and inner fill/erase formulas | Same formulas, dp ceil and integer size division; bound-centered Drawable replaces View canvas; modern isolated saveLayer replaces deprecated full-alpha overload |
| TelegramRadioButton.kt lifecycle/jump | RadioButton.java:128–150; RadioButtonCell.java:84–92 | attached animation, immediate binding | Native checked state drives indicator; jump also cancels, detach settles, attach synchronizes |
| TelegramRadioButton.kt native superclass | RadioButtonCell.java:102–108 | RadioButton class, checkable, checked accessibility | Native AppCompatRadioButton supplies semantics and RadioGroup integration; no extra accessibility node |

Only no-icon drawing is ported. Telegram icon mode and application dependencies
are unused and excluded. This is a source-derived compatibility adaptation,
not a byte-identical Telegram View or a transplant of RadioButtonCell layout.
Tile typography, radius, padding, XML hierarchy, scrolling and press animator
remain unchanged. Static selector resources remain referenced by the generic
radio style and other screens; no broad resource deletion is justified.

## Verification contracts

JVM tests exercise progress, duration, cancellation, reverse/interruption,
immediate binding, independent colors/progress, raster ring/dot and layer safety,
runtime palette, system animator scale and attach/detach. Native tests extend the
existing harness with all four options, repeated/rapid taps, exact callback list,
real TestViewModel truth, Clear and recycled/recreated holders. Existing functional,
small-screen/font-scale, Day/Night rendering and both-navigation gates remain.
Actual command results and exact-SHA CI outcomes belong in the mission report.
