# Native QR renderer test-boundary repair

Scope: repair the recorded native test failure and enforce the optimized CPU/C API
suite in the existing Android FAST/FULL build job. This is not QR screen acceptance.

## Source and reproduction

Official Telegram HTTPS Git HEAD and GitHub commit API agree on
`3de37c8cfb039452be7398909461da6288babf15`, root tree
`cc3c6ccf48e4167fdec764dfd04e765043f55093`. `git ls-tree` selects tlottie
`31f1b542f88e7b4be9a01e749920d857535fc715`. An HTTPS checkout of
`https://github.com/dkaraush/tlottie` fetched that immutable commit; all **69**
recorded tlottie files matched its Git objects before edits. This is TLS/Git
object verification, not a signature-verification claim. The MIT grant permits
this test adaptation; original copyright/license remain in source and APK assets.

Using Rust **1.99.1**, unchanged source:

```
cargo test --locked --release --manifest-path app/src/main/cpp/vendor/tlottie/Cargo.toml -- --test-threads=1
```

Result: **241 passed, 1 failed**, zero ignored. The failing
`dos::renderer_rejects_generated_work_after_successful_parse` panics at original
`src/tests/dos.rs:2674` with `LimitExceeded(ParseMemory)` before its RGBA/Alpha8
renderer assertions. Runtime was 2.14s after compilation. The two historical
wall-clock failures did not reproduce in this optimized serial configuration.

## Cause and repair

The dense-edge fixture authors **700,000** vertices plus two equally long tangent
vectors. `composition/json.rs:90-100` enforces cumulative parser allocation even
under the existing test-only authored-data `with_limits_check(false)` flag.
The parser's **13,031,466-byte** default cap is correctly smaller than the model
required by this renderer-only test. Raising it would weaken production protection.

The test now parses the same two-vertex scene normally, then constructs the
original 700,000-vertex alternating-edge model in test memory. Original group,
fill, closed contour, edge positions and zero tangents are retained. Every
original renderer case and both error assertions remain. Other fixtures still
require successful parsing; improved diagnostics identify any failing case.
A new separate test checks both normal authored-input rejection and the specific
`ParseMemory` rejection with the original authored-data test bypass. The original
end-to-end dense JSON time/memory tests are also retained.

**No production parser, renderer, C API, JNI, default limit, test timeout, memory
budget or expected renderer error was changed.** The test module is behind
`cfg(test)` in `src/lib.rs:92-94`. Android runtime source remains byte-identical.
No dependency revision is upgraded or silently mixed.

The existing protected build job now executes:

```
cargo +1.99.1 test --locked --release --lib --features c-api --manifest-path app/src/main/cpp/vendor/tlottie/Cargo.toml -- --test-threads=1
```

This uses the optimized CPU/C API feature set selected by CMake. Serial test
scheduling avoids competing test workers consuming a DoS test's wall-clock budget;
all original 1.5s/frame and 32MiB checks remain active. It does not certify Android
JNI behavior or hardware performance, which still require native device tests.
There is no continue-on-error, ignored test, result filtering or replacement job.
The existing six-job FULL dependency and publication gates remain intact.

## Source fidelity map

All upstream tlottie references below are at
`31f1b542f88e7b4be9a01e749920d857535fc715` (Telegram pin above).

| Telegram upstream file:lines@SHA | EVE file:lines | Classification | Tests | Runtime evidence |
|---|---|---|---|---|
| gitlink `TMessagesProj/jni/tlottie` → `src/tests/dos.rs:652-667,2657-2687` | `app/src/main/cpp/vendor/tlottie/src/tests/dos.rs:652-671,2662-2730` | ADAPTED test construction and added parser-boundary coverage | optimized native suite | host CPU only; no UI claim |
| gitlink → `src/composition/json.rs:90-100`, `src/composition/limits.rs:127-128` | same vendor paths/lines | EXACT bytes; limits untouched | parser regressions + new memory-boundary assertion | host CPU only |
| `TMessagesProj/jni/lottie.cpp` @ `3de37c8cfb039452be7398909461da6288babf15` | `app/src/main/cpp/telegram_lottie.cpp` | EXACT bytes; unchanged by this repair | existing JNI instrumentation test remains | device evidence pending |

Per-file upstream and EVE digests are in `qr-native-upstream-manifest.json`;
only the test module changes classification. Remaining native work: Android
all-ABI packaging/16KiB/current JNI device verification, frame pacing/lifecycle,
and mission-final six-job FULL. GPL combined-distribution review stays OPEN.

## Local verification

- Unchanged pinned source, debug serial: **241 passed / 1 failed**, same parser
  memory failure, 21.70s. Historical timeout failures did not reproduce here;
  they are not relabeled as passes on the earlier machine.
- Repaired optimized CPU suite: **243 passed / 0 failed / 0 ignored**, 3.21s;
  all seven renderer cases reach both original renderer assertions.
- CI helper suite: **59 passed**. Documentation verifier: zero errors/warnings.
  Scoped Telegram acceptance validator passes structurally with all targets OPEN.
- `git diff --check` passes. Production Rust/JNI source files are unchanged.

C API-enabled optimized suite: **243 passed / 0 failed / 0 ignored**, 2.14s.
Exact-SHA FAST is recorded in the mission checkpoint once observed. No local Android SDK/device execution is claimed by this batch.
