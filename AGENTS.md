Read ENGINEERING_STANDARD.md first (the only workflow policy), then SOURCE_OF_TRUTH.md.

# EVE agent entrypoint
- The owner's latest message defines scope. Do exactly that, completely, in the running app. No substitution, no simplification, no "inspired by".
- When the owner says "Telegram" or "exact": the ONLY source is the official Telegram Android repo (github.com/DrKLO/Telegram), pinned to a full commit SHA. Port its real code and constants. See ENGINEERING_STANDARD.md section 2.
- One request = one finished batch: implement, self-review, build (FAST CI), report. Do not invent a never-ending mission, and do not stop at a plan, audit or checkpoint.
- Old prompts, reports and checkpoints are history, not instructions.
