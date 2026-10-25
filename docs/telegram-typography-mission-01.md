# Telegram Android typography and icon source register

Source availability: Telegram source is not stored in this repository; the owner supplies it per session. See `SOURCE_OF_TRUTH.md` and `AGENTS.md` for source-exact archive/hash verification; absent archives do not block unrelated work.

## Telegram reuse policy

Reuse and distribution follow `ENGINEERING_STANDARD.md` and `THIRD_PARTY_NOTICES.md`; the single fidelity contract is in `AGENTS.md`.

## Official source

Source paths below are relative to the owner-supplied archive extracted outside
git. Telegram source is not stored in this repository. See `SOURCE_OF_TRUTH.md`
for the source-exact per-session archive/hash verification rule. Missing archives
block only source-exact transplants and provenance checks, not unrelated tasks.
Historical split-archive manifest results remain provenance evidence;
they do not make source available in a new session.

| Role | Official source and lines | Source values |
|---|---|---|
| ActionBar title and subtitle | `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java:1443–1461` | Title 20dp, phone landscape 18dp; paired phone title/subtitle 18/14dp, tablet 20/16dp; glass title 17dp |
| Settings label and detail | `TMessagesProj/src/main/java/org/telegram/ui/Cells/TextDetailSettingsCell.java:41–51` | 16dp label, 13dp detail |
| Section header | `TMessagesProj/src/main/java/org/telegram/ui/Cells/HeaderCell.java:94–95` | 14dp, `AndroidUtilities.bold()` |
| Tab label | `TMessagesProj/src/main/java/org/telegram/ui/Components/ScrollSlidingTextTabStrip.java:549–553` | 15dp, `AndroidUtilities.bold()` |
| Medium typeface | `TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java:252–270`; `TMessagesProj/src/main/java/org/telegram/messenger/SharedConfig.java:619` | `fonts/rmedium.ttf` by default; system bold is opt-in |

EVE uses sp for Android font scaling and retains Hindi/Devanagari fallback.
Existing EVE adaptations include accessible Day/Night foregrounds, native touch
bounds and domain-specific status colors. These adaptations do not establish
complete pixel parity with Telegram components.

Icon mappings are recorded in `docs/telegram-icon-source-registry.tsv`.
Per-density SHA-256 comparisons against the pinned source are recorded in
`docs/telegram-source-verification.tsv`. Candidate and semantic-mismatch rows
are distinct from imported artwork. Static source images do not imply animation.
The layout reference inventory remains in `docs/telegram-full-layout-inventory.md`.

Preserve Telegram circular reveal, Lottie assets/playback/triggers, bookmark
states, navigation, accessibility, server-side scoring and answer visibility.
Required build and runtime verification follows `ENGINEERING_STANDARD.md`;
release compliance review follows `docs/telegram-release-compliance-gate.md`.
