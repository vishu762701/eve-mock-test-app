# EVE — Mock Test Capability Matrix

Date: 2026-10-22. Audit-only matrix; no duplicate timer/palette/submission was added. Results remain **server authoritative**. Status `EXISTING SOURCE` means implementation exists and was inspected; it is **not** a full device integration pass.

| Capability | Source evidence | Classification | Residual validation gap |
|---|---|---|---|
| Start attempt + authoritative metadata | TestViewModel.kt:139,279 | EXISTING SOURCE | Backend startAttempt; live-user eligibility UNKNOWN |
| Exam instructions/negative marking | TestActivity.kt; metadata contract | PARTIALLY INSPECTED | Exact instruction UI not established |
| Timed attempt + resume | TestViewModel.kt:31–63,139–160,352–363 | EXISTING SOURCE | Clock-change/process-death device coverage UNKNOWN |
| Question rendering/options/images | TestActivity.kt + QuestionAdapter.kt | PARTIALLY INSPECTED | Font 2.0x and media clipping UNKNOWN |
| Answer selection/clear/mark | TestActivity.kt:140–168 | EXISTING SOURCE | Rapid-tap truth table not run on device |
| Palette and per-question time | TestActivity.kt + QuestionPaletteAdapter.kt | EXISTING SOURCE | TalkBack state details UNKNOWN |
| Persistence and current index | TestViewModel.kt:131–160,298,543–585 | EXISTING SOURCE | Corrupt cache/process death UNKNOWN |
| Exactly-once clientAttemptId | TestViewModel.kt:63,139,150,292; TestActivity.kt:495 | EXISTING SOURCE | Racing/offline end-to-end live client UNKNOWN |
| Submit and server grade | TestActivity.kt:456–590 | EXISTING SOURCE | Server-authoritative result contract backed by backend regression tests |
| Offline submit pending queue | TestActivity.kt:635–660; SubmitWorker.kt | EXISTING SOURCE | Network transition device test UNKNOWN |
| Result/review/reattempt | ResultActivity.kt, test activities | EXISTING SOURCE | Permissions/last-attempt eligibility live test UNKNOWN |
| History/Saved/Mistakes | HistoryActivity.kt; BookmarksActivity.kt; MistakesActivity.kt | EXISTING SOURCE | Home Drawer now routes to History/Saved; data end-to-end UNKNOWN |
| Accessibility and reduced motion | DesignMotion.kt; Android UI suite | PARTIALLY INSPECTED | TalkBack + 2x font + RTL physical QA UNKNOWN |
| Grading secrecy and score fairness | Backend attempts/routes | EXISTING BACKEND CONTRACT | No grading rules changed; cross-user live tests UNKNOWN |

The backend test suite includes deterministic attempt/race/error scenarios, but these do not establish every Android process restore or real student eligibility path. Never treat mock-server data as verified production totals, permissions or scores. No server-side grading algorithm was changed in this checkpoint.


## Deadline regression fix (2026-10-22)

`TestViewModel.startTimer()` retains a monotonic elapsed-time deadline in `MonotonicAttemptCountdown.kt`; `stopTimer()` stops only UI ticks, and `resumeTimerAfterFailure()` restarts observing the **same deadline**. Network/submission time no longer extends the client countdown. Four pure JVM test cases assert partial-second rounding, expiration, new authorized start and invalid negative input. Server attempt expiration and grading remain authoritative. Android process restoration still requires a real server resume and physical-device evidence, not claimed by these tests.
