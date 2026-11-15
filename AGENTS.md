Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.

# EVE agent entrypoint
- The owner's latest explicit message defines scope and is the supreme repository authority. Do exactly that, completely, in the running app. No substitution, no simplification, no "inspired by".
- When the owner says "Telegram" or "exact": the ONLY source is the official Telegram Android repo (github.com/DrKLO/Telegram), pinned to a full commit SHA. Port its real code and constants (ENGINEERING_STANDARD.md section 2), including 1:1 Telegram Physics (springs, ButtonBounce, pull damping, overshoot), Matrix (2D transformations, crop geometry, QR emoji fields), and Settings (Telegram Theme Engine, cache/storage, passcode, language).
- Useless rules, obsolete process friction, and artificial UI locks (such as brittle single-line XML tests) are permanently eliminated. Tests must assert real behavior and contracts without locking UI evolution.
- Full build-failure elimination: maintain zero build failures across FAST and FULL CI pipelines. Repair defective or brittle tests forward.
- One request = one finished batch: implement, self-review, build (exact-SHA FAST), report. Do not invent a never-ending mission and do not stop at a plan, audit or checkpoint.
- Old prompts, reports, checkpoints and most of `docs/` are history and evidence, not instructions. Open a doc only when your task needs it.
- Do not introduce independent rules. This file adds no policy beyond ENGINEERING_STANDARD.md.
