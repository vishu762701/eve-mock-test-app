Always read and follow ENGINEERING_STANDARD.md before doing anything. Then read SOURCE_OF_TRUTH.md.

## Repository workflow

- Commit and push directly to `origin/main`; do not open pull requests. Verify the remote, branch, HEAD, and working tree before editing. Fetch and safely synchronize before editing and pushing, preserving unrelated work. Never bypass branch protection; report any direct-push blocker. Verify the remote SHA after pushing.

## Approved UI boundary

- Before UI changes, read `APPROVED_UI_IMPLEMENTATION_AUDIT.md` together with the current UI design authority in `SOURCE_OF_TRUTH.md`. Their current authority sections supersede historical numeric requirements and older approval boundaries.
- Redesigns outside that approval, including Full Leaderboard redesigns, require explicit user authorization.
- App-wide iOS 26 colour and typography migration (ADR-008), and full-screen geometry migration (ADR-009: Full-screen iOS 26 geometry) are authorized by the user. Radii, padding, row heights and backgrounds may change; screen regions, behavior, protected test colours, Lottie assets/triggers/sizes, icons and the theme reveal stay protected.
