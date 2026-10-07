# Focused visual continuation evidence

Final APK: native card/border pixels/reset passed (55.526 s); typography/undo/recreation/reopen passed (90.242 s); glass low/high radius, independent opacity, foreground and reset passed (33.192 s). See their raw `*-device-pass.txt` files and screenshots.

Seven distinct focused scenarios passed across runs. `initial-device-run.txt` contains the five passing scenarios plus the incorrect card fixture and unfinished typography. `isolated-retry.txt` contains a passing pointer scenario, capture failure before native reset, and typography input timeout. `cold-retry.txt` records the cold emulator interruption. These are not complete-suite passes.

`checks.txt` records the final APK/JVM results, backend type check and 85-test summary. `UI_STUDIO_VERIFICATION.md` in the repository root contains the capability matrices, scope, root causes and unverified environments.

Screenshots are actual emulator captures. The native test extracts the existing production Result MaterialCard into the existing Login hierarchy; it does not execute authentication or production business actions. Glass uses a local striped backdrop with sharp native foreground to make radius changes observable. At partial material/item opacity, original sharp backdrop remains visible through the blurred layer. No physical optical refraction is claimed.
