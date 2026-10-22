# Current UI implementation authority

The owner's locked root design-reset specification supersedes historical iOS 26
numeric requirements, former approval boundaries, old icons, fonts and effects.
Use the root day/night tokens, bundled Inter 400/500/600/700 with Hindi fallback, solid 26dp surfaces,
capsule controls and Medium glass only on top bars, bottom bars and menus.
The device-font choice is explicitly retired. API 24–25 retains the approved
black navigation-area fallback in light mode.

UI changes must retain data, business logic, navigation destinations and feature
callbacks. The Telegram day/night reveal, toggle listeners, sun/moon resources
and transition handoff remain protected. Existing Lottie motion, timing, assets
and triggers are retained under the owner's later preservation instruction.

Implementation inventory, exact checks, limitations and verified release evidence
belong in [the current report](docs/design-reset/report.md) and
[screen inventory](docs/design-reset/screens.md). Native fixture verification is
separate from authenticated production end-to-end verification. Do not reuse
superseded task documents or historical numeric design decisions as current UI
requirements.
