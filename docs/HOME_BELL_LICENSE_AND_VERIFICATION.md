# Home notification bell

## Owner decision and Apple evidence

The owner approved an independent permissively licensed bell instead of requiring
Apple-exact artwork after review of the official terms. No Apple asset was copied.

- Official iOS 26 generation: Apple's WWDC25 “What's new in UIKit” at
  https://developer.apple.com/videos/play/wwdc2025/243/ (23:16 onward) identifies
  SF Symbols 7 and its effects; https://developer.apple.com/sf-symbols/ now describes
  a newer generation and is not evidence for a specific iOS 26 bell shape.
- The exact requested Apple symbol variant, weight, scale and animated states were
  **NOT FOUND** in supplied references. No screenshot of a specific Apple bell was
  provided; `bell`, filled or badge variants cannot be equated without evidence.
- Current official Xcode agreement https://www.apple.com/legal/sla/docs/xcode.pdf,
  §2.10, limits system-provided images to apps for the corresponding Apple-branded
  products. Android redistribution permission was **BLOCKED / not established**.
- Apple Design Resources license (20230621), §2A–B,
  https://developer.apple.com/support/downloads/terms/apple-design-resources/Apple-Design-Resources-License-20230621-English.pdf,
  is not an Android embedding license either. No unverified third-party Apple copy,
  extracted glyph or lookalike was imported. These findings are scoped reuse evidence,
  not blanket legal advice or Apple visual parity claims.

## Implemented replacement

`docs/sources/home-bell/provenance.json` pins full upstream Tabler revision,
original SVG hash, MIT license and output hashes. Android use is permitted by MIT
subject to retaining the notice. The source 24-unit grid, 2-unit round stroke and
path geometry are converted to the idle VectorDrawable and all Lottie frames.
SVG arcs use cubic segments no larger than 45 degrees. EVE owns the independent
ring motion: a decaying rotation, fixed 140-frame/60fps composition, same existing
playback/lifecycle/navigation controller and approximately 2333ms duration.
This is **ADAPTED / independent licensed artwork**, never Apple-exact.

Home references only `ic_home_notification` and `home_notification_bell`.
`ic_bell` remains in the inbox item, reminder/settings controls and
`ReminderScheduler`; the historical `notification_bell` remains in the inbox empty
state. Removing those shared files would break remaining callers. Android system
notification small icons were not changed.

Unread acknowledgement remains owned by NotificationsActivity, never Home.
Incoming event handling, first-sync baseline, pending-resume event, generation
cancellation, tap deduplication, delayed inbox navigation, reduced motion,
Day/Night tint and accessible parent tap target are preserved.

## Verification boundary

JVM tests verify retained behavior contracts, independent resource wiring and
source/output hashes. Native tests render the production Lottie, compare idle and
animated endpoints, observe genuinely different frames and verify Day/Night ink.
Their compilation is not execution; record actual FULL results and reviewed
frames in the mission report. A fixture is not proof of live FCM delivery to a
signed-in production account. No claim of Apple shape, timing or animation parity.
