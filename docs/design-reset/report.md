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
