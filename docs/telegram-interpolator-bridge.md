# Canonical Telegram CubicBezierInterpolator bridge

- Original source: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`
- Upstream file: `TMessagesProj/src/main/java/org/telegram/ui/Components/CubicBezierInterpolator.java`
- EVE canonical file: `app/src/main/java/org/telegram/ui/Components/CubicBezierInterpolator.java`
- Verified Git blob SHA-1 (upstream and EVE): `3a9cae100e17a03abacf2f4df71db2b82757b968`
- Canonical source classification: **EXACT**, byte-for-byte unchanged within this class only.
- EVE package facades: `app/src/main/java/com/eve/app/telegram/components/CubicBezierInterpolator.java` and `app/src/main/java/com/eve/app/ui/profile/popup/CubicBezierInterpolator.java`.
- Facade classification: **ADAPTED** (package-compatibility subclass and typed static constants).
- Original protected fields and `getInterpolation`, `getXForTime` and coordinate math are inherited instead of duplicated. Public constructors and static typed EVE fields are retained for existing callers. Inherited `Emphasized`, `EmphasizedDecelerate`, `EmphasizedAccelerate` and `StandardDecelerate` refer to the original class.
- Verification: `.github/scripts/tests/test_telegram_interpolator_bridge.py` checks the pinned Git blob and bridge invariants; Java compilation is the existing exact-SHA FAST Android CI gate. Neither is an end-to-end animation parity proof.
- Unrelated originals `org.telegram.messenger.AndroidUtilities` and `org.telegram.ui.Components.LayoutHelper` are **NOT FOUND** in current EVE source. Do not invent them or call existing EVE layout helpers source-exact. Implement them only after mapping their own upstream dependencies and supported EVE call sites.
- License: original source is part of the Telegram Android distribution and subject to applicable Telegram GPL notices. Bridge creation does not change release-approval obligations. See `THIRD_PARTY_NOTICES.md` and `docs/telegram-release-compliance-gate.md`.
