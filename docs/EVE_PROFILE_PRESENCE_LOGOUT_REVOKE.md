# Explicit logout revokes online presence before Firebase sign-out — source-fidelity mission, 2026-10-31

**Scope:** Authenticated EVE presence privacy/integrity, not Telegram UI source-identity or unrelated login changes.

## Verified cause
Before this change, `SettingsActivity.executeLogout` and `MainActivity.logout` immediately called `FirebaseAuth.getInstance().signOut()`. The `ProfilePresenceClient` auth listener only attempted its session revoke *after* losing the token; the request could fail, leaving the account's 90-second lease visible as online until expiry. This was an avoidable stale-presence window.

## Behavioral fix
- `ProfilePresenceClient.revokeBeforeLogout()` suspends the heartbeat, snapshots its exact per-foreground UUID, joins the canceled heartbeat, then attempts authenticated `DELETE /api/users/presence` with a hard 1500ms upper bound. It marks logout in progress to prevent a new heartbeat racing with the revoke.
- Both Settings logout confirmation and Home logout call `revokeBeforeLogout()` in their existing activity `lifecycleScope` **before** invoking unchanged local SessionManager, premium cache cleanup, Firebase auth sign-out, Google sign-out and navigation.
- Repeat taps are gated (`rowLogout.isEnabled=false` or `logoutPending=true`). A network failure / legacy Worker is non-blocking, and the server-side 90-second TTL is the fallback.
- Account deletion path is unchanged: backend authenticated DELETE deletes the user record and its presence sessions through D1 foreign-key cascade; token deletion/reauth flow remains server-authoritative.

## Tests and status
`app/src/test/java/com/eve/app/ui/profile/PresenceLogoutContractTest.kt` asserts both explicit paths, call order, 1500ms bound, heartbeat cancellation/join and guard. A native/network auth scenario still requires an Android device with real Worker staging; FAST JVM/source compilation is not a physical runtime guarantee. No backend deployment is performed in this change.

**Remaining mission acceptance:** native foreground/background/logout under Wi-Fi loss, multi-device lease expiry and reconnect; original Telegram Canvas ProfileActionsView exact rendering and image motion; Poll/Category RTL/performance; official Apple SF Symbols Android redistribution rights; final six-job FULL and APK digest. This is a privacy correction, not a claim that original Telegram online/privacy infrastructure is ported.
