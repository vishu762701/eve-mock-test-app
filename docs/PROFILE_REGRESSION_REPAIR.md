# Profile regression repair — open mission checkpoint

This records implemented repairs and the remaining source-integration work. It is
not acceptance of the complete owner mission or a claim of Telegram screen parity.

## Baseline and authentication

- EVE baseline: `60cb2c36c084405a5868df1952ecb3c5222919a5`, live `main` fetched
  before edits. Existing drawer, availability and notification work is retained.
  Baseline exact-SHA runs: [41262868361](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41262868361)
  and [41262868296](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41262868296), both successful.
- Official origin rechecked at `https://telegram.org/apps`, linking DrKLO/Telegram.
  `git ls-remote ... HEAD` and immutable GitHub objects identify
  **3de37c8cfb039452be7398909461da6288babf15**, 13.1.1 (7237).
  This agrees with the previous profile source pin; no revisions were mixed. The
  attached screenshots contain no build-version metadata: their precise app revision
  remains UNKNOWN; correspondence with the selected self-profile/QR mode is INFERRED.
  `regression-source-map.json` compares downloaded source bytes to authenticated
  Git-tree blob IDs and records SHA-256. This is HTTPS/Git-object provenance,
  not signature verification or a rendering-parity certificate.
- All five owner attachments were inspected. 24371/24373 show EVE defects;
  24377/24379/24383 are the Telegram profile/menu/QR references. Their personal
  data/photos are not copied into this public repository.
- Upstream root LICENSE is GPL v2. Java per-file later-version grants are recorded
  where present; missing grants are not invented. THIRD_PARTY_NOTICES and the
  existing fail-closed production release gate remain applicable.

## Implemented paths and causes

### Confirmed state and revalidation (EVE adapter)

`ProfileActivity.onResume()` previously called `loadProfile()` every time the
screen returned. That load marked all fields unconfirmed, disabled editing and
hid Bio before the same data arrived. A separate presence loop issued another
profile GET immediately on resume. Cancelling/replacing loads was not a shared
identity-scoped repository, and closing/reopening discarded the confirmed data.

`ConfirmedProfileStore` now owns one active identity's confirmed immutable response,
request generation, single in-flight request, 30-second monotonic freshness and
scroll/expansion position. `ProfileSession` binds it to Firebase account changes
and the existing owner-tagged authenticated EVE endpoint. It never persists private
profile fields to disk. Opening renders available confirmed fields immediately;
revalidation keeps those fields mounted. Presence and profile share a request.
Successful editors invalidate freshness, while birthday/category write/readbacks
supersede earlier revalidation. A cancelled screen waiter does not cancel another
waiter's request. Failed reads never manufacture confirmed fields. Account changes
clear both the data and position; server ownership checks remain required.

The existing Android hierarchy restores scroll/expansion after recreation. In-process
reopen also restores the remembered position without animation. After actual process
death the in-memory response is intentionally absent: normal saved View state and a
new authenticated fetch apply. No claim of persistent offline profile storage is made.
Admin authority remains separately revalidated; no cached profile grants admin rights.

### Actual normal popup renderer with EVE destinations

The rejected centered `Profile options` dialog and its invented Retry/Close rows
are removed from the profile entry point. The normal official popup renderer,
including its `PopupSwipeBackLayout` parent, background growth, per-row animation,
clipping, scroll measurement, outside/back dismissal and exit animator is integrated.

The corresponding self-profile source entries are at
`ProfileActivity.java:12156-12174`. EVE exposes Change username and Copy link to
profile. Copy uses the established registered `eve-mock-test://username/<username>`
contract, not a Telegram URL or a new verification/private-profile service.
Change profile color is **NOT FOUND in EVE's profile persistence contract** and is
not shown as a dead action. Telegram-only peer color, messaging/account services,
paid usernames, account notification locks, animated emoji/downloaded image menu
variants, and slider-only behavior are excluded. The normal source swipe-back
parent is retained even with no foreground submenu; it affects measurement.

The imported Java rendering/layout/motion code is retained where applicable.
Package, resource and theme/density/platform bindings are isolated in the popup
package. The static-icon row uses native ImageView/TextView in place of Telegram
Lottie/emoji classes. EVE font scaling and TalkBack labels are explicit adaptations.
Theme roles use the existing source-mapped submenu palette, not generic surface
roles. Lifecycle/theme teardown dismisses immediately. Overall classification:
**ADAPTED**, pending device/reference acceptance; eight icon files are EXACT bytes.
A material remaining difference is the profile-specific backdrop: upstream
`ProfileActivity.java:4073-4090,16959-16985` captures the scene and configures
`setBlurredBackgroundFactory(scrimBlur3Factory, ...messageMenuBackground(...))`.
This batch integrates the actual normal nine-patch renderer/fallback, but does not
include that complete blur3 scene-capture/provider/drawable pipeline. It must not
be called the complete official profile overflow or verified screenshot parity.
Profile color persistence, Hindi strings and the full RTL/font/reference matrix
also remain outside the demonstrated acceptance of this batch.

### Shared grouped rows and affected surfaces

The old shared Material elevated-card style added global content padding,
elevation/shadow and corner geometry to the profile. `TelegramProfileSection`
uses `RecyclerListView.setSections()` / `drawBackgroundRect()` instead: 12dp
outside margin, 16dp radius, no global card content padding/elevation/press-scale,
and the source optional shadow paints (default off per SharedConfig.java:684).
Children and ripple drawing clip to the section. Live theme changes invalidate it.

Category, its expandable options, separator and Daily Practice Reminder are now
children of ONE shared section below the personal-information section. There is
no separate reminder outer card or intervening card margin. Existing authenticated
category save/readback and ReminderScheduler persistence/work remain connected.
Business functions and fixed LinearLayout section host are **ADAPTED**; there is
no Telegram exam-category business equivalent. Other app card styles are unchanged.

## Material source and dependency map

All upstream references use the pinned SHA above and the prefix
`TMessagesProj/src/main/java/org/telegram/`.

| Property | Source | EVE mapping / disposition | Classification |
|---|---|---|---|
| Self-profile entries | `ui/ProfileActivity.java:12156-12174` | `ProfileOverflowPopup`, `ProfileActivity.showProfileOptions`; EVE username/link callbacks | ADAPTED; profile color NOT FOUND in EVE |
| Menu factory/host | `ui/ActionBar/ActionBarMenuItem.java:412-418,837-883,1887-1949,2466-2482` | `ProfileOverflowPopup`; real flags=USE_SWIPEBACK, source default row/min-width/anchor relationships | ADAPTED Activity host |
| Popup drawing, measure, touch, motion | `ui/ActionBar/ActionBarPopupWindow.java:160-668,733-1139` | `ui/profile/popup/ActionBarPopupWindow.java`; original normal renderer, package/resource adapters, account notification locking excluded | ADAPTED; native comparison pending |
| Swipe-back hierarchy and transforms | `ui/Components/PopupSwipeBackLayout.java` | Same-name popup class; density/theme adapters; Telegram slider type and account notification locks excluded | ADAPTED |
| Static icon rows | `ui/ActionBar/ActionBarMenuSubItem.java:70-97,125-130,200-219,397-426` | Same-name popup class; static self-profile subset, native text/image widgets, scaled font accommodation | ADAPTED |
| Mask/ripple rendering | `ui/ActionBar/Theme.java:5296-5359,5387-5395`; `ui/Cells/BaseCell.java:150-167` | `popup/Theme.java`; original mask and safe-draw bodies with platform logging/resource/theme bindings | ADAPTED enclosing host |
| Density/interpolation/frame params | `messenger/AndroidUtilities.java:2701-2706,5146-5155,5225-5230`; `ui/Components/LayoutHelper.java:30-32,89-95` | `PopupPlatform`, `LayoutHelper`; UI-only extracted functions, context density adapter | ADAPTED bindings |
| Source interpolation | `ui/Components/CubicBezierInterpolator.java`, `Easings.java` | Same-name popup files, only package changed | EXACT implementation bodies; not whole-screen parity |
| Menu glyphs | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/menu_username_change.webp`, `msg_link2.webp` | `profile_` filename prefix in same qualifiers; all eight Git blobs/SHA-256 checked in JSON | EXACT bytes |
| Grouped surface, clipping | `ui/Components/RecyclerListView.java:3285-3292,3587-3615`; `messenger/SharedConfig.java:684` | `TelegramProfileSection`, two profile XML groups; fixed EVE rows instead of section item decoration | ADAPTED |
| Revalidation and account isolation | Telegram account infrastructure deliberately excluded | `ConfirmedProfileStore`, `ProfileSession`, owner-tagged existing API | EVE-only / ADAPTED |
| Full header/actions/gallery | `ProfileActivity`, `ProfileActionsView`, `ProfileGooeyView`, gallery/blur/cutout dependencies | Existing EVE adapter retained; not replaced by this batch | ADAPTED existing; complete requested integration OPEN |
| Full personal QR | `QrShareActivity` → `QrShareView`, `QrCodeLayer`, QrGlass/Render/backdrops/emoji/scanner/server | Existing `UsernameCardActivity` remains custom; full source renderer not integrated | Requested integration OPEN, not accepted |

`regression-source-map.json` is a provenance manifest, not proof that every inspected
file was imported. The report explicitly differentiates inspected QR/header sources
from popup sources used in this batch.

## Verification and remaining acceptance

New production-behavior tests:

- `ConfirmedProfileStoreTest`: coalesced simultaneous reads, warm reopen, TTL/offline
  recovery without cleared Bio, cancelled-screen sharing, account purge including
  position, non-cooperative late response after an edit, mismatched server identity.
- `ProfileOverflowNativeTest`: actual popup window and original hierarchy, settled
  geometry, callbacks in two scroll positions, disabled missing-link action, Back,
  outside tap and repeated opening. This uses a disposable debug fixture Activity.
- `ProfileIdentityAndExpansionNativeTest` additionally checks the actual section
  parents, absence of Material elevation/padding, and reminder displacement within
  the same group when category options open/close in its existing theme/font matrix.

Local Android SDK/Gradle/JDK were bootstrapped outside the repository. Initial
attempts exposed missing JVM proxy configuration, a JRE without jlink, then missing
proxy CA trust in the downloaded JDK; those were environment setup errors, not
weakened app checks. An omitted Easings dependency and an unavailable UiAutomator
import were repaired locally; native input uses Android Instrumentation directly.
Local result: `:app:assembleDebug :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin` passed with Gradle 8.7 / JDK 21 / Platform 36.
JUnit XML contains 139 suites, **649 tests, zero failed/error/skipped**, including
all six new store tests. Repository host checks passed **51 tests**. Backend
`npm install --no-package-lock`, `npm run build`, `npm test` passed **287 tests**.
These run against production repository code; no Worker deployment is performed.
Remote exact-SHA gate links and APK integrity belong in the delivery checkpoint.
Compilation and host tests do not establish native or reference parity.

Complete mission acceptance remains OPEN. In particular:

1. The current header still uses EVE's NestedScrollView/state/expansion adapters,
   not the complete official parent/layout/actions/gallery subsystem. The source
   has 152dp header + 74dp actions relationships, top-left avatar pivots, a source
   `calculateHeaderExtraDiff()` and fully-collapsed Gooey GONE endpoint which differ
   materially from the current independently transformed EVE header. No independent
   offsets/spacers/fake camera overlay are introduced to claim that this is fixed.
2. The QR screenshot's full official subsystem has not been replaced. Importing only
   QrCodeLayer or drawing a rounded bitmap would not satisfy the request. Its actual
   scene renderer, glass, palette, emoji, scan and sharing integrations remain work.
   Telegram gift purchases, server backdrops and fee-free link modes have no existing
   EVE service contract; source dependencies must be isolated rather than simulated.
3. Matched official app/device motion capture, physical cutout/no-cutout behavior,
   landscape/older API/reduced-motion/RTL/Hindi/large-font full subsystem comparisons
   and signed-in editor/category/reminder/backend end-to-end acceptance remain open.
4. Passing tests for this batch must not be described as mission acceptance or
   100% Telegram equivalence. Maintain the first actionable failing gate and repair
   forward if CI fails. No production deployment or remote migration is authorized
   or performed by this batch.

## Backend and related risks

No Worker, D1, Firebase rules, auth interceptor, migration or production configuration
is changed. The latest recorded production deployment is now
[41254141424](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41254141424)
at `e3de3ae29c1a88d8450f4215cbd4c94132cc65e5`: migration, post-migration schema check,
Worker deploy and live smoke steps all succeeded. Earlier run 41253037991 was
preflight-only, with write/deploy steps skipped. Bio/presence being merely staged
is therefore an outdated historical assumption: their deployment is INFERRED from
the deployed source and successful workflow, while actual authenticated responses,
real-account behavior and any out-of-band subsequent changes remain UNKNOWN.
No new deployment/migration is performed here. EVE's username link intentionally
opens a username-only card and does not assert account ownership.

Related items retained outside this repair batch: (high) combined-binary GPL/license
release clearance; (high) authenticated live Bio/presence and account-isolation acceptance after
the recorded rollout; (medium) reminder OS notification permission availability and
real delivery; (medium) local-only profile-photo synchronization limits. Existing
maintenance/drawer/bell checkpoints and changes are preserved.

## Forward repair of the first FULL failure: target API 36

The first FULL run [41267282568](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41267282568)
failed `lintDebug` on the imported upstream private
`PopupWindow.mOnScrollChangedListener` reflection. `SoonBlockedPrivateApi` correctly
identifies this as blocked for EVE's target API 36. No lint suppression, baseline,
target downgrade or skipped test is introduced.

The legacy listener interception/re-registration workaround is removed. EVE's
minimum API 24 already provides automatic anchor attachment, scroll tracking and
detachment through public `showAsDropDown` / `update` / `dismiss`. Source evidence:
AOSP `core/java/android/widget/PopupWindow.java`, official mirror
[`android-7.0.0_r1:2191-2224`](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-7.0.0_r1/core/java/android/widget/PopupWindow.java#L2191)
and
[`android-16.0.0_r1:2460-2504`](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-16.0.0_r1/core/java/android/widget/PopupWindow.java#L2460).
These were inspected, not imported. The unused private layout-in-screen reflection
is also replaced with public `setIsLaidOutInScreen` on API 29+, with the normal
anchored platform behavior on older supported releases. EVE's profile menu does
not request layout-in-screen mode.

This is an explicit **ADAPTED platform compatibility boundary**. Original popup
drawing, measurement, row cascade, interpolation and exit animation remain sourced.
`ProfileOverflowNativeTest.publicPlatformAnchorTrackingFollowsScrollAndDetachesOnDismiss`
uses an actual scrolling anchor/window, verifies its screen displacement, then
dismisses, scrolls and reopens. Both native navigation modes must execute it in
FULL. The failed run remains failure evidence; a green FAST does not replace FULL.

Local forward-repair validation passed: `lintDebug` (zero errors),
`testDebugUnitTest` (649 tests, zero failures), `compileDebugAndroidTestKotlin`
and `assembleDebug`. The additional real scrolling-anchor native test compiles;
its actual device execution still belongs to the repaired SHA's FULL gate.
A temporary, uncommitted Robolectric native-graphics probe also rendered the actual
Day/Night section and popup paint paths. Its first attempt hit a Robolectric
keyguard-shadow limitation in the debug Activity; the probe used a plain themed
AppCompat host thereafter. This is host graphics evidence only, not native device
or reference parity. Probe code/images are outside the repository.

## Confirmed-cache malformed-response follow-up

A real Gson decode fixture reproduced an additional cache validation defect: Gson
can construct a UserProfileResponse with null core fields even though Kotlin marks
them non-null. The initial store accepted such a response after checking only its
identity; a warm open could then render it outside the load error handler.
`malformedJsonCannotReplaceTheLastConfirmedFieldsOrSeedAWarmOpen` failed against
that implementation before the repair. It removes each of displayName/dob/category
from actual JSON, checks revalidation and write/readback acceptance, and checks a
cold cache. This is not a string/name/constant assertion.

The store now validates core field presence before caching either a GET response
or a confirmed readback. Rejection preserves the last confirmed fields; a cold
malformed response does not seed the cache. Legitimate empty birthdays remain
valid. The UI also preserves a confirmed empty category as “Select category”
instead of displaying a fabricated saved General selection. No server data is
rewritten, no new endpoint is introduced, and optional Bio/presence capability
semantics remain unchanged.

Local validation passed `testDebugUnitTest` (**650 tests**, including seven store
cases), `lintDebug` (zero errors), `compileDebugAndroidTestKotlin` and `assembleDebug`.
This follow-up changes confirmed-state handling; popup rendering is unchanged.
For the per-feature commit record, the original repair batch and this follow-up
are the two confirmed-cache implementation batches; the original batch and the
API-36 forward repair are the two normal-popup implementation batches. The shared
section surface/card is in the original batch only. Record the actual FAST/FULL
outcomes separately; none of this asserts complete mission or visual acceptance.
