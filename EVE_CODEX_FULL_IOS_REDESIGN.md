# CODEX MASTER PROMPT — EVE FULL iOS-STYLE REDESIGN

TASK

Rebuild the EVE Android app so its entire visual language, interaction feel, motion, materials, colors, spacing, controls, navigation chrome, cards, pills, dialogs, sheets, icons, and Login screen feel like a polished current iOS application.

This is NOT a small theme tweak. Treat this as a full visual-system migration from the app's current Android/Material-heavy appearance to a coherent Apple-inspired iOS visual language, while keeping EVE's existing product functionality, authentication, navigation, backend contracts, exam behavior, and data behavior working.

The authoritative design references are Apple's official Human Interface Guidelines, Apple Developer documentation, and official Apple WWDC design sessions listed at the end of this prompt.

IMPORTANT:
- The app remains an Android Kotlin/XML app.
- Do NOT attempt to convert the project to SwiftUI/UIKit.
- Reproduce the Apple visual/interaction principles using stable Android equivalents.
- Do not add fake "iOS" branding or pretend Android is literally running iOS.
- Do not copy proprietary Apple code/assets into the repository.
- Use Apple's official design guidance as the visual source of truth and implement legal/platform-appropriate equivalents.

CURRENT REPO REALITY — INSPECTED BEFORE THIS PROMPT

The latest supplied repo is an Android Kotlin/MVVM/XML project.

Important existing areas include:
- `app/src/main/java/com/eve/app/ui/home/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/java/com/eve/app/ui/login/LoginActivity.kt`
- `app/src/main/res/layout/activity_login.xml`
- `app/src/main/java/com/eve/app/ui/admin/AdminActivity.kt`
- `app/src/main/java/com/eve/app/ui/admin/AppConfigActivity.kt`
- `app/src/main/java/com/eve/app/data/remote/EveApiService.kt`
- `app/src/main/java/com/eve/app/data/repository/AdminRepository.kt`
- `app/src/main/java/com/eve/app/data/repository/HomeBannerRepository.kt`
- `app/src/main/java/com/eve/app/data/model/AppContent.kt`
- `app/src/main/java/com/eve/app/data/model/AppConfig.kt`
- `backend/src/routes/appContent.ts`
- `backend/src/routes/admin.ts`
- existing shared UI/blur/motion helpers under `ui/common` and `util`

The repo already contains `MASTER_TASK_APPLE.md` and existing Apple/Liquid-Glass work. Do NOT blindly trust that file as a complete implementation. Inspect the actual current code and use source code as the implementation truth.

REQUIREMENT 1 — OVERALL DESIGN DIRECTION

The whole app must feel intentionally designed for iOS, not like an Android app with blue colors added.

Use:
- Apple-like hierarchy
- large, confident typography
- clean spacing
- restrained color
- soft depth
- large rounded geometry
- translucent/frosted chrome
- subtle real-time blur where appropriate
- springy, tactile interactions
- quiet separators
- system-like controls
- edge-to-edge content
- smooth, deliberate transitions
- consistent optical alignment

Do NOT:
- use Android Material styling as the visible design language
- make every button/pill blue
- use the old green/red/black-and-white visual vocabulary as the main identity
- use random gradients just to make the UI look "premium"
- overuse glass/transparency until text becomes hard to read
- mix unrelated design systems screen-by-screen

The result must look like ONE product designed from the beginning with a current Apple/iOS visual system.

REQUIREMENT 2 — COLOR SYSTEM

Adopt an Apple system-style color system for both light and dark modes.

Dark mode:
- Background should remain true black / OLED-like where appropriate.
- Primary surface approximately system secondary/elevated dark surfaces.
- Text hierarchy should use white + secondary/tertiary white/gray equivalents.
- Status colors should follow Apple-style semantic roles.

Light mode:
- Use an Apple-style grouped/system background rather than plain Android white everywhere.
- Cards/surfaces should be clean white/elevated surfaces where appropriate.
- Text hierarchy should use primary/secondary/tertiary label tones.

CRITICAL COLOR RULE:
BLUE MUST NOT TAKE OVER THE APP.

Use system-blue-like color only where it is semantically appropriate:
- primary action
- selected state
- active control
- link
- important navigation state

Do NOT make:
- every card blue
- every pill blue
- every icon blue
- every heading blue
- every section title blue
- all borders blue
- all interactive surfaces blue

Neutral surfaces should stay neutral. Let content and small accents carry color.

Preserve semantic status colors where required:
- success = Apple-like green
- destructive/error = Apple-like red
- warning = Apple-like orange
- informational/primary = blue

The visual balance should feel mostly neutral with controlled accent color.

REQUIREMENT 3 — PILLS ARE LOCKED TO APPLE REGULAR MATERIAL

Every pill in EVE must use the selected APPLE REGULAR MATERIAL visual language.

This is a locked decision:
- material = Apple Regular-style material
- not Clear
- not ultra-thin
- not overly transparent
- not a flat opaque rectangle

Pills must feel:
- premium
- soft
- rounded
- frosted
- slightly translucent
- readable
- dimensional without heavy shadows

Use strong rounded geometry with optical proportions.

Pills include, where applicable:
- category/filter chips
- streak/status pills
- timer pills
- compact action pills
- segmented/filter controls
- navigation pills
- compact state controls
- other genuinely pill-shaped controls

Do not force every rectangular component into pill geometry. Pills should remain pills; cards should remain rounded cards.

IMPORTANT:
Apple Regular Material is for the pill treatment specifically in this design direction. Do not blindly apply the exact same material to every surface in the app.

REQUIREMENT 4 — BACKGROUNDS / CHROME / BLUR

The background must feel iOS-like in both themes.

Use:
- true black dark foundation where required
- Apple-style grouped light background
- restrained surface hierarchy
- real-time blur for appropriate chrome

Use blur on:
- translucent top app bar/navigation chrome
- dialogs
- bottom sheets
- popup menus
- drawer/sheet chrome
- appropriate floating chrome
- pill material
- surfaces that intentionally sit above moving/visual content

Do NOT blur:
- text
- Lottie artwork itself
- exam artwork/content itself
- question content
- photos as a content asset
- important UI labels

The effect should be "frosted material over content", not a giant opaque gray rectangle.

The upper portions of the UI should have the refined blurred/frosted feel visible in modern iOS interfaces.

Create/use one consistent shared blur/material mechanism rather than scattered one-off implementations.

Use `RenderEffect` on supported Android versions and the project's appropriate existing fallback for older versions. Inspect current `EveBlurHelper`, `FastBlurHelper`, `GlassmorphismHelper`, and related classes before changing them.

Do not introduce multiple incompatible blur systems.

REQUIREMENT 5 — SHAPE / CORNERS

Current card shapes are not premium enough.

Standardize the geometry:
- small controls: approximately 10–12dp
- normal cards: approximately 20–24dp
- large hero/sheet/dialog surfaces: approximately 28–32dp
- pills: fully/strongly rounded
- circular controls: truly circular

Exact values may be tuned after inspecting the current layout so the result looks optical rather than formulaic.

The Home hero and exam/test cards in particular must have:
- clean rounded corners
- refined edges
- soft frosted/elevated appearance
- no cheap-looking hard rectangular corners

No small card should look like an old MaterialCardView with an arbitrary 8dp radius.

REQUIREMENT 6 — APPLE-STYLE MOTION

The app should feel responsive and physical.

Implement consistent motion for:
- screen entrance/exit
- card appearance
- buttons
- pills
- segmented controls
- dialogs
- bottom sheets
- menus
- tab changes
- expanding/collapsing content
- selection state
- search opening/closing
- loading/skeleton states
- theme transitions where existing behavior must remain

Touch feedback:
- subtle scale-down on press
- tiny opacity adjustment
- smooth spring back
- keep a visible accessibility-friendly feedback cue
- no aggressive bounce
- no cartoon animation

Prefer Apple-like spring/timing feel over generic Android "pop" animations.

Respect reduced-motion/accessibility settings. When system animation is reduced, provide the appropriate less-motion behavior instead of forcing motion.

Do not create gratuitous animation everywhere. Apple-like design uses motion to communicate state and hierarchy, not to decorate every tap.

REQUIREMENT 7 — TYPOGRAPHY

Typography must feel like an Apple-designed app.

Use Android system fonts / legal platform-appropriate fonts that visually approximate the iOS hierarchy.

Do not ship Apple's proprietary font files unless the project has a verified license to do so.

Use:
- large prominent titles
- strong but not oversized section hierarchy
- readable secondary labels
- restrained captions
- consistent line spacing
- Dynamic Type/accessibility-friendly scaling where practical

Do not make everything bold.

Do not make headings blue by default.

REQUIREMENT 8 — ICONOGRAPHY

Redesign the icon language throughout the app to be consistent with the weight and simplicity associated with SF Symbols.

IMPORTANT:
- Use SF Symbols as a visual/reference language only.
- Do not blindly package Apple's proprietary SF Symbols assets into the Android app unless legally licensed/allowed.
- Redraw or use legally available equivalents inside the existing drawable/vector architecture.
- Keep existing filenames/references where practical to avoid unnecessary functional changes.
- Maintain consistent stroke weight, rounded joins/caps, optical balance, and filled/outline state pairing.

Icons should feel:
- simple
- crisp
- restrained
- system-like
- visually consistent

REQUIREMENT 9 — HOME SCREEN

`activity_main.xml` / `MainActivity.kt` must be redesigned as an iOS-style Home screen.

The top region should feel like a modern iOS navigation/header area:
- edge-to-edge
- restrained
- blurred/frosted where appropriate
- correct safe-area/inset handling
- refined icon buttons
- correct typography
- no heavy Android toolbar feeling

HOME HERO / "FIND YOUR NEXT TEST" — NEW FUNCTIONAL REQUIREMENT:

The current hardcoded hero content inside the "Find your next test" box must be removed.

Do NOT keep these as hardcoded fallback content:
- "Find your next test"
- "Choose an exam to begin practicing."
- hardcoded streak copy inside that hero
- hardcoded "Admin Dashboard" content/button inside that hero

The hero container may remain as a premium rounded/frosted surface, but its content must be EMPTY by default until an admin publishes content.

There must be an Admin Dashboard option that lets an authorized admin manage this Home hero content.

ADMIN-MANAGED HOME HERO:
- Use the existing backend app-content architecture as the first-choice source of truth rather than creating a second unrelated config backend.
- Use a dedicated content type such as `home_hero`.
- Inspect the current `/api/app-content/{type}` implementation and extend it cleanly rather than creating duplicate storage systems.
- Keep the Cloudflare Worker/D1 backend as the source of truth for this Home content because that architecture already serves home/banner/app-content features.
- The admin UI should allow the admin to enter/edit the content they want shown.
- Support at minimum a title/headline and supporting text/body.
- Where the existing architecture permits it cleanly, support an optional CTA label/action and an enabled/disabled state.
- Validate and sanitize content.
- Publish/save should be authenticated and admin-only.
- Students should read the published content and render it without hardcoded fallback copy.
- If there is no published content or content is disabled, the hero should remain visually empty/blank rather than inventing fallback text.
- Never make the Admin Dashboard shortcut permanently embedded in the student-facing hero.

Use the existing `AdminActivity` / content-management area appropriately; do not create an isolated admin screen that duplicates existing content management unnecessarily.

Do NOT break:
- existing home banner carousel
- exam loading
- pinned exams
- exam navigation
- feedback
- search
- profile drawer
- theme toggle
- floating airplane behavior
- streak data
- any other Home functionality

EXAM / TEST CARDS:
The current RPSC/test boxes in the supplied screenshot are visually too flat and rectangular.

Redesign them as premium iOS-style rounded cards:
- stronger corner radius
- cleaner spacing
- restrained surface treatment
- subtle frosted/elevated depth
- correct text hierarchy
- better optical icon placement
- no giant blue card fills

Where the current pencil Lottie is used:
- do not alter the Lottie file itself
- do not recolor it
- do not add a background ring/tile around it
- retain its existing behavior
- only improve the surrounding card/surface

The category/filter controls must use the locked Apple Regular-style pill material.

REQUIREMENT 10 — LOGIN SCREEN MUST BE COMPLETELY REBUILT

THIS IS A FULL REPLACEMENT OF THE CURRENT LOGIN VISUAL DESIGN.

Do not merely restyle the current login card.

Completely remove the current Login screen visual structure and build a new iOS-style authentication screen from scratch.

Current files include:
- `LoginActivity.kt`
- `activity_login.xml`
- `AmbientBackgroundView`
- existing blur/card/login-specific styling

The new Login screen should no longer look like the current design.

Replace:
- current login card composition
- current ambient-art presentation if it conflicts with the new design
- current visual hierarchy
- current old segmented/tab appearance
- old input styling
- old button styling
- old decorative effects that make it look like the current app

Keep the underlying AUTH FUNCTIONALITY intact.

Preserve and verify:
- Firebase email/password sign-in
- sign-up flow
- name handling if currently supported
- Google sign-in
- Google token handling
- auth errors
- validation
- password visibility behavior
- loading state
- existing session bypass
- crash-debug flow where applicable
- navigation to Home after successful authentication
- back behavior
- analytics/crash reporting already wired to auth, unless a change is required purely because of the new UI

New Login visual direction:
- clean iOS-style background
- large Apple-like title hierarchy
- calm spacing
- rounded input fields
- quiet secondary labels
- a single strong primary action
- secondary auth action
- subtle sign-in/sign-up switch
- tasteful spring entrance
- minimal decorative noise
- premium but not flashy
- no "Android Material form" appearance

Input fields should feel like iOS text fields:
- rounded
- comfortable height
- filled/frosted surface
- subtle border/separator only when needed
- clear focus state
- no thick Material outline boxes
- correct keyboard/input behavior
- excellent error presentation

Primary button:
- strong but restrained
- Apple-style geometry
- system blue only as an intentional primary action
- no giant glowing effect

Google sign-in:
- keep its actual behavior
- restyle only
- preserve branding requirements and legibility
- do not fabricate an Apple sign-in action unless the app's authentication backend actually supports it

Sign In / Sign Up switching should feel like a native iOS segmented/control transition rather than an Android tab bar.

No auth bug may be introduced by the visual rewrite.

REQUIREMENT 11 — ALL OTHER SCREENS

Apply the same coherent design system to:
- Test screen
- Result screen
- Result detail/review
- Profile
- Settings
- History
- Bookmarks
- Mistakes
- Practice
- PYQ
- Syllabus
- Leaderboard
- Notifications
- Feedback
- Premium
- Payment screens
- About/content screens
- all Admin screens
- all dialogs
- bottom sheets
- popups
- empty/error/loading states
- shared controls

Do this systematically.

Do not only redesign Home and Login while leaving the rest of the app looking like a different product.

TEST SCREEN:
Preserve all current behavior exactly:
- question flow
- timer
- bookmark
- report
- answer selection
- submit
- navigation
- no premature correctness reveal
- attempt state

Only rebuild the visual and interaction language.

RESULT:
Preserve:
- score
- rank
- percentile
- review
- palette-isolated question behavior
- reattempt flow
- leaderboard behavior
- cutoff logic

Only rebuild visual treatment.

ADMIN:
Apply the Apple-style system consistently to:
- Dashboard
- content management
- exam management
- question management
- feedback
- broadcasts
- analytics
- users
- premium
- polls
- syllabus
- app config
- dialogs and confirmation sheets

Destructive actions should use semantic red, not random colors.

REQUIREMENT 12 — EXISTING LOTTIE / ANIMATION GUARDRAIL

Inspect the repo for existing Lottie files and their playback logic.

Do NOT alter animation JSON content unless the current task specifically requires replacing an animation.

Do NOT:
- recolor Lottie artwork
- change loop mode
- change timing
- change trigger behavior
- put a fake circle/ring/tile around the existing Home pencil animation
- put a fake circle/ring/tile around the floating airplane animation

You MAY redesign the surrounding container/surface when necessary for the new iOS visual language.

REQUIREMENT 13 — EXISTING THEME TOGGLE

The existing circular-reveal theme transition has already been implemented and must not be casually broken.

Inspect:
- `ThemeSwitchAnimator.kt`
- `ThemeManager.kt`
- `TelegramMenuPopup.kt`

Keep the existing theme-toggle behavior unless the new global iOS visual system explicitly requires a minimal compatibility adjustment.

Do not replace its interaction with an unrelated toggle.

REQUIREMENT 14 — DESIGN SOURCE OF TRUTH

Use these Apple sources as design authority:

1. Apple Human Interface Guidelines — Materials
2. Apple Liquid Glass documentation
3. Apple "Adopting Liquid Glass"
4. Apple WWDC25 "Meet Liquid Glass"
5. Apple HIG — Branding / Color / Typography / Layout / Buttons / Menus / Motion
6. Apple official design videos/resources for current iOS design

Do not rely on random YouTube tutorials, Behance mockups, Pinterest screenshots, or unofficial "iOS clone" libraries as the primary source.

When Apple's guidance evolves, prefer the current Apple documentation.

REQUIREMENT 15 — NO PROPRIETARY APPLE ASSET MISUSE

Do not blindly download and commit:
- Apple proprietary source code
- Apple internal assets
- unlicensed SF Pro font files
- unlicensed SF Symbols package content
- Apple's private UI frameworks

Use official Apple documentation and publicly documented design guidance, and implement Android equivalents.

REQUIREMENT 16 — ARCHITECTURE / SOURCE OF TRUTH

Do not create duplicate data stores for existing features.

Before any backend change:
1. inspect which backend currently owns that feature
2. preserve that backend as the source of truth
3. update API model/repository/UI together
4. verify authenticated admin access
5. verify student read path
6. verify empty/error/offline behavior

For the Home hero specifically, prefer the existing Cloudflare Worker/D1 app-content system (`api/app-content/{type}`) instead of inventing a separate Firestore config unless actual code inspection proves that impossible.

REQUIREMENT 17 — STRICT NO-BREAK RULE

Do not break:
- Firebase Auth
- Firestore integration
- Cloudflare Worker API
- D1 storage
- exam loading
- question loading
- answer submission
- results
- leaderboard
- admin authorization
- notifications
- broadcasts
- premium
- profile
- search
- theme switch
- Lottie playback
- back navigation
- deep links/intents
- data persistence

If a visual change requires a supporting refactor, keep it isolated and backward-compatible.

Do not change business logic merely because the code could be cleaner.

LOCATION

Primary UI:
- `app/src/main/res/layout/**`
- `app/src/main/res/drawable/**`
- `app/src/main/res/color/**`
- `app/src/main/res/values/**`
- `app/src/main/res/values-night/**`

Primary Kotlin UI:
- `app/src/main/java/com/eve/app/ui/**`
- `app/src/main/java/com/eve/app/util/**`

Home:
- `ui/home/MainActivity.kt`
- `res/layout/activity_main.xml`
- exam/home adapters and relevant shared components

Login:
- `ui/login/LoginActivity.kt`
- `res/layout/activity_login.xml`
- related login-specific views/resources/helpers

Admin:
- `ui/admin/AdminActivity.kt`
- relevant content-management models/repositories
- `EveApiService.kt`
- `AdminRepository.kt`

Home hero backend:
- `backend/src/routes/appContent.ts`
- backend types/migrations only if genuinely required
- corresponding Android model/repository/API methods

DO NOT CHANGE

Do not change:
- exam scoring rules
- timer rules
- question correctness logic
- attempt logic
- result calculations
- leaderboard calculations
- auth provider behavior
- Firestore business logic
- Cloudflare Worker business logic unrelated to the new Home hero content
- Lottie JSON files and playback behavior
- production data
- secrets
- Firebase configuration
- admin authorization rules unless required to safely expose the new admin content feature
- unrelated completed features

Do not force-push.

Do not delete uncommitted user work.

Do not claim runtime verification that you did not actually perform.

BEHAVIOR

The finished app should feel like one coherent current Apple/iOS-inspired product from Login through Home, Test, Result, Profile, Settings, and Admin.

The user should notice:
1. cleaner hierarchy
2. softer rounded geometry
3. better material depth
4. more restrained color
5. less blue
6. more refined motion
7. better touch response
8. consistent iOS-like spacing
9. better dialogs/sheets
10. a completely new Login screen
11. an empty-by-default Home hero controlled by Admin
12. premium exam cards and pills

The app should NOT feel like:
- old Android Material
- a generic glassmorphism template
- a blue-themed Android app
- a collection of unrelated screen redesigns

ACCEPTANCE CRITERIA

A. Global
- Light and dark mode both work.
- No obvious old theme remnants remain.
- No screen looks visually disconnected from the new design system.
- Blue is restrained and semantically used.
- All important cards/dialogs/sheets have the new corner system.
- Pills use the locked Apple Regular-style material.
- Blur is actually visible where intended and does not make content unreadable.

B. Home
- The Home hero no longer hardcodes the previous "Find your next test" copy.
- The hero is empty by default when no admin content exists.
- Admin can create/update/disable the hero content.
- Student reads published hero content correctly.
- Admin authorization is enforced.
- Existing banner carousel remains functional.
- Exam cards have premium rounded iOS-style surfaces.
- Category controls use the locked pill material.
- Existing Lottie behavior is unchanged.

C. Login
- Existing login UI is visually replaced, not merely recolored.
- Sign in works.
- Sign up works.
- Google login works.
- Validation works.
- Error/loading states work.
- Existing session bypass works.
- No auth regressions.
- New login visually matches the global iOS system.

D. Other screens
- Test/result/admin/profile/settings/etc. all use the same visual language.
- Existing product behavior remains unchanged.

E. Motion
- Tappable elements have subtle spring/tactile feedback.
- Screen/sheet/dialog transitions are polished.
- Reduced-motion behavior is respected.
- No excessive animation is introduced.

VERIFICATION — MANDATORY

Do not stop at a green compile.

Run as much real verification as the environment supports.

At minimum:
1. `./gradlew assembleDebug`
2. `git diff --check`
3. resource/XML validation through the Android build
4. Kotlin compilation
5. backend tests/build/type checks if backend was changed
6. API-level verification for the new Home hero read/write path if backend was changed
7. verify auth flow code paths were not accidentally removed
8. grep for stale hardcoded Home hero strings and remove unintended duplicates
9. grep for stale old login view/resource references
10. verify no renamed/deleted resource references are broken
11. verify no Lottie playback contract changed
12. verify both light/dark resource variants
13. inspect diffs for accidental unrelated changes

If emulator/device UI verification is available:
- actually launch the app
- inspect Login
- inspect Home
- inspect at least one exam card
- inspect a pill
- inspect Test
- inspect Result
- inspect Admin
- inspect one dialog/sheet
- test light/dark
- test Home hero with no content
- test Home hero with admin content
- test login success/failure and Google flow as far as available

If emulator/device verification is NOT available:
- explicitly say so.
- Do NOT claim "UI verified" merely because Gradle passed.
- Report exactly what was statically/locally verified and what remains unverified.

FINAL REPORT

Return:
1. WHAT CHANGED
2. HOME HERO IMPLEMENTATION
3. LOGIN REBUILD
4. GLOBAL iOS DESIGN SYSTEM
5. BACKEND/API CHANGES
6. VERIFICATION PERFORMED
7. NOT VERIFIED / LIMITATIONS
8. FILES CHANGED
9. COMMIT HASH
10. PUSH RESULT

Do not claim success without evidence.

Before editing:
- inspect the actual current code
- inspect existing helpers/dependencies
- inspect the current implementation of each affected feature
- identify source-of-truth/backend ownership

During editing:
- proceed autonomously for routine implementation
- fix build/resource issues you encounter
- keep the scope focused on this prompt

After editing:
- verify
- review diff
- commit once the task is complete
- push to the current main branch
- leave a clean working tree if possible

OFFICIAL APPLE REFERENCES

Apple Human Interface Guidelines — Materials:
https://developer.apple.com/design/human-interface-guidelines/materials

Apple Liquid Glass:
https://developer.apple.com/documentation/technologyoverviews/liquid-glass

Apple — Adopting Liquid Glass:
https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass

Apple WWDC25 — Meet Liquid Glass:
https://developer.apple.com/videos/play/wwdc2025/219/

Apple HIG — Branding:
https://developer.apple.com/design/human-interface-guidelines/branding

Apple HIG — Layout:
https://developer.apple.com/design/human-interface-guidelines/layout

Apple HIG — Labels / typography guidance:
https://developer.apple.com/design/human-interface-guidelines/labels

Apple HIG — Menus and actions:
https://developer.apple.com/design/human-interface-guidelines/menus-and-actions

Apple Developer — Design videos/resources:
https://developer.apple.com/videos/design/

FINAL INSTRUCTION

This is a production app.

Do not give me a superficial "theme pass".

Actually inspect the repo, implement the full visual system, rebuild Login completely, implement the Admin-controlled Home hero correctly end-to-end, and apply the same iOS design language throughout the app.

Use Apple's official design guidance as the source of truth, Android-compatible implementation as the execution method, and the actual repository as the source of truth for existing functionality.

Do not ask for routine permission.

Do not ask me to choose between trivial implementation alternatives.

Make the implementation decisions yourself when they are not product-critical.

Do not silently skip difficult parts.

If something cannot be verified, state it plainly.
