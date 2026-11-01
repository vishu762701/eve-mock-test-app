#!/usr/bin/env bash
# Authenticated, commit-pinned source comparison; no full Telegram checkout or account stack.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REVISION="$(python3 - "$ROOT/docs/telegram-verbatim-imports.json" <<'PY'
import json, re, sys
with open(sys.argv[1], encoding='utf-8') as f:
    sha = json.load(f)['revision']
if not re.fullmatch(r'[0-9a-f]{40}', sha):
    raise SystemExit('Invalid pinned Telegram commit SHA')
print(sha)
PY
)"
DEST="$(mktemp -d "${RUNNER_TEMP:-/tmp}/eve-telegram-pinned.XXXXXXXX")"
trap 'rm -rf -- "$DEST"' EXIT
git -C "$DEST" init -q
git -C "$DEST" remote add origin https://github.com/DrKLO/Telegram.git
test "$(git -C "$DEST" remote get-url origin)" = "https://github.com/DrKLO/Telegram.git"
# Partial blobless fetch downloads the pinned commit/tree; git show lazily fetches only manifest files.
git -C "$DEST" -c protocol.version=2 fetch --depth=1 --filter=blob:none --no-tags origin "$REVISION"
git -C "$DEST" update-ref refs/heads/source "$REVISION"
git -C "$DEST" symbolic-ref HEAD refs/heads/source
test "$(git -C "$DEST" rev-parse 'HEAD^{commit}')" = "$REVISION"
python3 "$ROOT/.github/scripts/verify_telegram_imports.py" --upstream "$DEST" --manifest "$ROOT/docs/telegram-verbatim-imports.json"
