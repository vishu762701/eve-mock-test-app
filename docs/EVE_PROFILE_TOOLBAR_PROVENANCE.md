# EVE Profile toolbar shadow provenance — 2026-10-31

- Official source: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, from `Telegrammaster(5).zip` SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`. The revision root tree is `cc3c6ccf48e4167fdec764dfd04e765043f55093` per `THIRD_PARTY_NOTICES.md`.
- Source property: `TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java:2489` sets transparent action-bar background and `:2493` disables cast shadows. Source Git blob `7805f9d6d3a216972a18f9e367ad7199f2624555`.
- EVE affected surface: `app/src/main/res/layout/activity_profile.xml:11-17` used `4dp` elevation **and** `4dp` translation Z, causing a non-source extra rectangular top shadow; both are explicitly zero now.
- Shared EVE `EveGlassToolbar.kt` already disables hairline and scroll backdrop by default; left untouched to avoid changing unrelated screens.
- Classification: upstream no-shadow **EXACT property**, EVE native toolbar layout **ADAPTED**. This does not prove native Telegram header animation geometry, camera cutout, all typography, or matched frames. The profile screen as a whole is NOT EXACT.
- Proof: `ProfileToolbarShadowContractTest` JVM source contract; `ProfileToolbarNoShadowNativeTest` Day/Night native assertion (runtime execution pending six-job FULL CI). Real device screenshots/motion evidence still **UNKNOWN**.
