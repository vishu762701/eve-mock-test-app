# Home search owner restoration repair: owner exception approved

Base main c57dfa64693047e3e0b76530d4e930e4cd0baba1 has FAST41575420756 and
Docs41575420725 SUCCESS. No production API, permission, source renderer or numerical
motion value changes. The whole mission remains NOT MET and QR acceptance OPEN.

| ID | Requirement | Existing path / repair | Status / evidence |
|---|---|---|---|
| R1 | No foreign query/manual search restore | Existing MainActivity launch UID, manual Bundle producer/consumer and live UID predicate | MET scoped host; real authenticated Activity save/recreation and account-switch regression |
| R2 | No foreign automatic hierarchy replay | Existing onRestoreInstanceState passes an owned tree unchanged; foreign/untagged trees are copied without android:viewHierarchyState | MET scoped host; Android's actual EditText restoration separately reproduced the leak even with manual search flags cleared |
| R3 | Preserve current controls and same-owner state | Same toolbar, search ViewModel, filter/list contract and original 150ms renderer; original owned hierarchy keeps cursor selection | MET scoped host query/cursor, repeated real open/Back/close, Hindi/RTL/Night/200% font roundtrip; physical IME/visual/source comparison still OPEN |
| R4 | Commit-budget governance | Two existing successful search delivery commits; one additional coherent confidentiality repair | MET authorization: owner explicitly approved on 2026-11-03; carrying exact-SHA FAST pending; category exception not reused |

Live GitHub reverified both earlier search implementation commits:
285a95288e7ee08eb9ef22d744965f8e7421627e /
[40995030992](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40995030992)
SUCCESS, and 460e093d30f55631c46e16a20c937da444d01c7b /
[41005412909](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41005412909)
SUCCESS. This patch repairs the same Home search feature rather than inventing a
new feature name to avoid its limit. Section2 says: "If an unforeseen failure makes
the limit unsafe to respect, document the reason and obtain owner approval for an
exception." The approved exception is one additional repair commit plus mandatory
exact-SHA FAST, not a FULL/licensing/deployment exception.

Two actual production-path regressions failed before: a recreated second account
saw the first account's query; clearing manual search flags still allowed native
view hierarchy to refill the field. Same-account recreation control passed. Early
SDK fixture constructor/shadow/photo/push errors were corrected and are not product
failure evidence. The test fixture isolates application startup background services,
Firebase auth/push and HTTP transport; MainActivity, HomeViewModel, registry, widgets,
callbacks, Bundle and Android hierarchy restoration are real host implementations.
All HTTP calls are intercepted; only synthetic exam/config reads return data.
No real Firebase app/token, user data, push registration, Worker request or write.

| Upstream path:lines@SHA | EVE path:lines | Classification / reason |
|---|---|---|
| Existing ActionBarMenuItem.java:935-1041@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/ui/home/MainActivity.kt search renderer | ADAPTED existing icon/search fade; unchanged by this repair |
| EVE Firebase/SavedState contract; no Telegram authentication imported | MainActivity.kt:124,291,622,643,1733 | EVE-authored UID metadata/restore checks; no EXACT claim or new state owner |

One immutable origin UID tags manual state; saving after identity replacement cannot
stamp a new account onto old text. Foreign/untagged state fails closed. Same-account
native cursor/hierarchy restoration stays enabled; the repair does not globally
disable EditText saving or replace Android restoration. Existing old theme/search/
seam and categories remain. No speculative offsets, overlays, delays or duplicate
search surface. Normal restart/recreation still uses the existing owner and widgets.

Eight native Activity tests PASS in the scoped run. Full mandatory local validation
is recorded after it actually completes. Owner approved ONE additional Home repair commit on 2026-11-03 with: "Haan, ek additional repair commit authorize hai".
Carrying commit and exact-SHA FAST are still pending; no success is invented. Production
rollout, human GPL release clearance, device/reference matrix and protected FULL
remain separate controls.

Local complete validation PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (2m52s), 956 tests / 203 suites, zero failures/errors/skips;
lint 0 errors / 1629 warnings. Native Home8 cases PASS; helper59, docs81, acceptance
structure, four byte-identical imports and git diff --check PASS. No new backend,
production service, permission or Telegram source import. This result is local-only;
no additional Home search commit or exact-SHA FAST until owner exception arrives.

Owner exception receipt: reviewed patch SHA-256 c996573268a71ac25bfa30ba041e5b2b0a0fffe3e33b6f74ad7c395c37873a77; all eight reviewed file hashes reverified before application against unchanged live main. Only documentation changed afterward to record this approval. No expensive unchanged local suite rerun is needed; its exact source/test bytes match the recorded956-test PASS. Mandatory exact-SHA FAST remains required.
