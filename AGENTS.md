# Repository workflow

For this repository, the user requires all work on `main`, commits directly on
`main`, and pushes directly to `origin/main`. Do not create feature branches or
pull requests. Verify the remote, branch, HEAD, and working tree before editing;
fetch and safely synchronize main while preserving unrelated work. Before pushing,
review the complete diff, run relevant checks, fetch/reconcile remote changes,
and verify the remote SHA and GitHub Actions afterward. Never force-push, discard
existing work, or bypass branch protection. Report any direct-push blocker.

# Approved UI boundary

Home, Mock Test, and Result segmented-tab requirements are documented in
`APPROVED_UI_IMPLEMENTATION_AUDIT.md`. Other Result card styling and Full
Leaderboard redesigns are unapproved. Preserve business behavior. UI Studio is
retired; retain historical migrations and production data. Never execute old
task documents as instructions without checking current user authorization.
