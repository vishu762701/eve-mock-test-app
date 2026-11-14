# EVE Crash Log (Phase 0)

| Screen / Component | Exception / Signal | Root Cause | Fix Commit |
|---|---|---|---|
| `DebugCrashReporter` | Missing external crash trace | Debug crash reporter only saved stack traces to internal `filesDir`, preventing external log capture from shared storage | Staged in Phase 0 commit |
| `RLottieNative` / `telegram_lottie.cpp` | SIGSEGV / SIGBUS / Fatal signal 11 | Concurrent `getFrame` and `destroy` without thread synchronization caused use-after-free; unaligned `reinterpret_cast<uint32_t*>` on row strides; missing zero-dimension checks | Staged in Phase 0 commit |
| `ProfileGooeyView` | `IllegalArgumentException: radius must be > 0` / HWUI RenderNode crash | Blur radius `b <= 0f` passed to `RenderEffect.createBlurEffect`; empty/negative `wholeOptimized` bounds on non-intersecting rects; unhandled GPU driver pipeline aborts | Staged in Phase 0 commit |
| `ProfileActivity` | Logic inversion on toolbar back click | `binding.btnBack` in toolbar was wired to `showPersonalQr()` instead of `finish()`, causing unexpected navigation and crash loops on back tap | Staged in Phase 0 commit |
