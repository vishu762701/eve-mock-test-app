# Profile implementation and evidence boundaries

## Verified source, preservation and rights

Official origin: https://telegram.org/apps/android links the Android repository
https://github.com/DrKLO/Telegram. Live Git HEAD at source-work start and GitHub's
immutable commit API agreed on **3de37c8cfb039452be7398909461da6288babf15**.
`docs/sources/profile-mission/source-map.json` records the root tree, fetched source
hashes, Git blob matches and seven exact asset pairs. This is HTTPS-origin/tree
verification, not a claim of a signed commit or complete application equivalence.

Existing valid profile work is retained: the full-window Gooey renderer and cutout
trajectory, API-31+ RenderEffect path and CPU/stack-blur fallback, theme roles,
source action icons, 74/54dp normal action geometry, independent alpha stages,
ButtonBounce, native button accessibility, editors, birthday validation, category
radio selector/readback, local per-account photos, reminders and badges. Historical
reports were used as pointers and their cited official source was checked again.

The upstream Java headers grant GPL v2 or later. The QR glyph is unchanged upstream
asset material under the root GPL v2 license; no asset-specific later-version grant
is invented. Full Telegram/GPL notices and release compatibility gates remain.
ZXing Core 3.5.3 supplies actual QR encoding under Apache 2.0; its full license ships
in APK assets. No Telegram account, messaging, network, credentials or keys enter EVE.
Combined-work production distribution clearance remains a separate existing gate.

## Material source/property map

All upstream paths below are relative to
`TMessagesProj/src/main/java/org/telegram/` at the full revision above.

| Component/property | Official entry/dependency | EVE implementation/disposition | Classification |
| --- | --- | --- | --- |
| Own-profile upper-left QR | `ui/ProfileActivity.java:2499-2510`, `ui/QrShare/QrShareActivity.java` | `ProfileActivity.showPersonalQr`; `UsernameCardActivity`, `ProfileQrPayload`; Telegram account/link resolver excluded | ADAPTED; glyph bytes EXACT |
| Overflow and owner window lifecycle | `ui/ProfileActivity.java:4009`; `ui/ActionBar/ActionBar.java`, `BaseFragment.java` | Existing EVE toolbar + native dialog; Refresh, Edit info, Settings, Copy username, Close only; dismiss/duplicate/lifecycle guards | ADAPTED |
| Set Photo / Edit Info / Settings | `ui/ProfileActivity.java:3909-4004`; `ui/Components/ProfileActionsView.java:247-337,412-460,512-587,1247-1328` | Existing EVE photo/name/settings destinations, action geometry, alpha, bounce and accessibility retained; explicit Day/Night icon ink | ADAPTED; six glyph byte pairs EXACT |
| Mobile / Username / Birthday rows | `ui/ProfileActivity.java:13194,13517-13636`; `ui/Cells/TextDetailCell.java:68-145` | `activity_profile.xml`, existing EVE editors/readbacks; 16sp values/13sp labels, editable EVE data and validation | ADAPTED (upstream uses dp typography) |
| Normal photo/header and collapse | `ui/ProfileActivity.java:5354-5359,6039-6047,6375-6450`; `ui/Components/ProfileGooeyView.java`; `messenger/NotchInfoUtils.java` | Existing `ProfileGooeyView`, `ProfileScrollState`, `ProfileAvatarTrajectory`, `NotchInfoUtils`, `ProfileStackBlur`; EVE full-window host retained | ADAPTED; not full upstream renderer equivalence |
| Pull resistance / expansion threshold | `ui/ProfileActivity.java:4320-4340,8465-8520` | `ProfilePullScrollView`: half resistance before latch, 0.33 threshold, continuous deltas after latch, bounded pull; real NestedScrollView input remains parent | ADAPTED, verified source numeric constants |
| Motion / cancellation / restoration | `ui/ProfileActivity.java:4190-4237,8479-8520`; `ui/Components/CubicBezierInterpolator.java:13-15` | 250ms distance/velocity-scaled settle, 1100–2000dp/s factor and (.42,0,.58,1) curve; cancellation, pointer handling, detach, reduced motion, Parcelable restoration | ADAPTED |
| Expanded single photo / name / status | `ui/ProfileActivity.java:6375-6450,8465-8520`; `ui/Components/ProfileGalleryView.java` | `ProfileHeaderExpansion`: single EVE photo, white text/scrim, source 30/10dp text-end roles with large-font spacing; full Telegram gallery/pager/media services excluded | ADAPTED; EVE scrim/geometry are not asserted upstream-exact |
| Background / card surface endpoints | `ui/ActionBar/ThemeColors.java:79,133`; `assets/night.attheme:61,235` | Existing `design_colors.xml`: Day #F1F1F3/#FFFFFF; Night #000000/#181819; no global theme change | EXACT endpoint values only |
| Card geometry, EVE category and extras | Profile list/cell subsystem above; EVE-specific fields have no Telegram business equivalent | Existing EVE card tokens; intact category selector moved to a separate next card, reminders/badges retained | ADAPTED; EVE business behavior |
| Large-font action height | Source 74dp row / 54dp control and action alpha stages | `ProfileActionAccessibility`, `ProfileActionsCollapse.atHeight`: normal metrics retained; >1.4 font scale reserves actual two-line text height; collapse uses real row size | ADAPTED accessibility accommodation, not invented source metrics |
| Photo persistence | Upstream `BackupImageView` / account ImageReceiver dependencies inspected | Existing UID-scoped `ProfilePhotoManager` and Coil/local file pipeline retained; Telegram media loader/network excluded | ADAPTED; photo remains device-local, not multi-device photo sync |
| Presence | Telegram account presence is excluded | Existing authenticated EVE leases + `ProfilePresenceClient`; resumed private profile polling, sequence/UID checks, unknown on failure/pause | EVE-only / ADAPTED |
| Username identity / compatibility | No Telegram identity infrastructure imported | Existing EVE `username`, immutable Firebase/D1 `id`, legacy `eve_id`; only username is displayed/shared by profile | EVE-only; compatibility fields retained |

The complete `ProfileActionsView` Canvas/shader/parentExpanded renderer is not ported.
EVE uses native action containers; the photo expansion does not claim that renderer's
icon/shader transitions. Messaging/media tabs, channels, posts and fake features are
not added. No supported EVE counterparts justify adding them. The two owner reference
images were **NOT FOUND** in this chat/workspace; no screenshot-match claim is made.

## EVE data and privacy integration

- The separate profile EVE ID row and copy action are removed. A username is the
  actual editable, case-insensitively unique server username, not a renamed UID.
  D1/Firebase IDs, `eve_id`, API compatibility fields, account ownership, recovery
  and privileged legacy lookup remain intact. No destructive migration is needed.
- Profile and editors capture the initiating UID. `ProfileRequestOwner` is local
  Retrofit/OkHttp `@Tag` metadata, never an HTTP header/body/query or QR field.
  `AuthInterceptor` checks it before token work, before send and before a refreshed
  retry. A queued Alice request cannot pick up Bob's token. Readback must match
  both owner identity and expected value; auth changes close old profile/editors.
- Missing/unread profile data cannot enable editing fabricated defaults. Read errors
  have a working menu refresh. Unconfirmed category/birthday writes retain the
  previous display and require a refresh before retry. Existing fields and unrelated
  configuration are never replaced by an identity rename or reset.
- The real QR destination is registered as `eve-mock-test://username/<username>`.
  It displays a username card and a working copy action. Only normalized username
  bytes are encoded. No phone, birthday, UID, token or private profile lookup occurs.
  Query, fragment, extra path, userinfo, port, escaped-name and non-EVE payloads fail
  closed. An ordinary scanner can open the registered deep link. A shared editable
  username is explicitly not proof of account ownership; no public web URL is invented.
- Presence is a server Boolean, never connectivity or a hardcoded role. The profile
  clears unknown/stale display on pause and failed fetch, checks request order/UID,
  and refreshes while resumed. Process heartbeats and revocation are bound to their
  initiating UID/session. A different device's lease is not removed on logout.
  The server's existing bounded 90-second lease expiry is the fallback when offline
  revocation cannot reach it; instant global Firebase revocation is not claimed.
- Bio is preserved when an authenticated backend returns the field. On an older
  backend it is hidden with its divider, rather than showing a nonfunctional
  “server update” placeholder. Backend data/editor support is not deleted.

## Verification and limitations

`ProfileQrPayloadTest` executes real ZXing encode/decode and malformed/private-data
rejection. `ProfileRequestOwnerTest` exercises real Retrofit request construction,
local-only tags, four profile/presence API methods and mismatched identities.
`ProfilePullStateTest` dispatches real touch events through the production view,
checks threshold/resistance/cancel/repeated gestures and Parcel round-trip restoration
both before and after measured size becomes available. Existing birthday/name/phone,
username, category, photo scope, theme, Gooey and accessibility tests remain enabled.

`ProfileIdentityAndExpansionNativeTest` inflates production layout and calls the
production renderer in Day/Night, with 320dp/2x-font/RTL cases. It checks header/text
geometry, large-font labels, category ordering, QR destination and restored compact
shape, and exports disposable initial/expanded PNGs. Existing native camera, header,
actions, motion, birthday/category and accessibility fixtures remain in FULL's two
navigation-mode suites. A temporary host native-graphics probe executed the same
render checks locally; host rendering is not a physical device or signed-in backend
acceptance. Record actual final native execution and reviewed frames separately.

These fixtures do not authenticate the owner's production account, receive a real
production FCM, prove physical camera-cutout rendering or compare matched Telegram
frames. Those checks remain UNKNOWN where credentials/reference images/devices are
absent. A build pass and byte-exact glyphs never make the whole screen source-exact.

## Backend rollout status and safe staged plan

No production deploy, remote D1 migration or production write was performed here.
Latest recorded successful GitHub Worker deployment at investigation time is
[41158961055](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41158961055),
commit `bed0e17845ad76e9c3c706093403642490d748df`; its D1, Worker and smoke steps passed.
That source has the established `id/eveId/username` profile contract but no Bio or
presence implementation. Presence/Bio being unavailable live is therefore INFERRED
from recorded deployment history; out-of-band live deployments cannot be excluded.
The actual authenticated live profile response was not available for inspection.

No new schema migration is introduced by this mission. Existing staged
`0025_user_bio.sql` and `0026_user_presence_sessions.sql` are prerequisites for the
newer Worker profile/presence paths; all local handler fixtures apply actual ordered
migrations. The maintenance backend fixes are also staged only. Before a separately
approved production release, inspect pending remote migrations and a recoverable
backup, confirm their actual order/content, then run only the existing manual
owner-approved deployment workflow. Verify public maintenance readback, an authorized
admin toggle/readback/audit, legacy client fields, private profile save/readback and
multi-device/expiry/logout presence using disposable accounts. Do not remove legacy
columns or account data. No approval for those live operations is inferred here.

Related existing risk left visible: `backend/src/routes/auth.ts` retains legacy
read-route `ALTER TABLE` fallback for absent old profile columns. It was not invoked
against production or broadened here; manual schema preparation before rollout is
necessary. The backend's real deployed version/schema and live Firebase revocation
semantics remain UNKNOWN beyond the recorded deployment evidence above.
