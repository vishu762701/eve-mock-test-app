# EVE profile online presence — authenticated foreground lease design

Status: **STAGED; production deployment and native multi-device acceptance NOT performed.** This is an EVE API contract, not a copy of Telegram proprietary server presence.

## Server
- `backend/migrations/0026_user_presence_sessions.sql` creates `user_presence_sessions` keyed by authenticated user UID and random per-process-foreground UUID, with 90-second server-authoritative expiry. The table is automatically deleted with the user record via FK CASCADE.
- Authenticated `POST /api/users/presence` validates a canonical UUID, ensures user row with token identity, prunes expired leases, and upserts the matching session for +90 seconds. The client cannot control UID, email or expiry.
- Authenticated `DELETE /api/users/presence` revokes only one of the caller's sessions. Other devices' sessions cannot be deleted.
- Authenticated `GET /api/users/profile` computes `isOnline` by any non-expired owned lease. `last_active` alone is NOT online evidence; no public presence endpoint is added.
- Server tests in `backend/test/username_and_phone.test.mjs`: multi-device, other-user isolation, malformed UUIDs, expiry and renewal.

## Android
- `app/src/main/java/com/eve/app/util/ProfilePresenceClient.kt`: `ProcessLifecycleOwner` foreground lifecycle, 30s authenticated heartbeats, unique UUID per foreground session, stop/revoke on background/signout (after in-flight heartbeat cancellation), fail-closed when old Worker responds HTTP 404, TTL handling for network loss. Uses `lifecycle-process:2.8.4`.
- `EveApplication.kt` registers lifecycle observer and notifies on Firebase auth state changes. The foreground lifecycle works across all activities, rather than declaring the user offline whenever the Profile screen is closed.
- `EveApiService.kt` adds POST and HTTP-with-body DELETE, and nullable `isOnline` DTO field for old-server compatibility.
- `ProfilePresenceStatus.kt` maps `true` to "online", `false` to "offline", `null` to the preexisting Eve ID/Student status. No fabricated online state.
- `ProfileActivity.kt` uses only authenticated GET to render the status. This is EVE ADAPTED user-visible text; exact Telegram presence/typing/last-seen privacy policy remains NOT PORTED.

## Release gates
Worker and remote D1 deployment remain MANUAL-ONLY since `896f3d995ba9235f6ae71dfa58ca708ea43866a3`. FAST is compile/backend tests; it does not execute native multi-device tests or prove production migration. **Do not manually dispatch Worker production or claim online globally until expressly authorized, tested on two active sessions, plus privacy and rate-limit policy acceptance.** No client network call makes an offline lease permanently online; expired sessions evaluate offline.

Remaining mission components: app-wide Telegram source exactness/native matching; original ProfileActionsView renderer; Poll & Category native acceptance; official Apple iOS animated bell Android distribution rights; final six-job FULL and published APK SHA-256.
