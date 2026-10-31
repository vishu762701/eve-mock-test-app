#!/usr/bin/env python3
"""Compare recorded verbatim imports with files in a pinned Telegram Git checkout.

A successful byte comparison is not proof of dependency completeness, runtime
parity, source origin, or licensing. Authenticate the checkout independently.
"""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]


def verify(manifest, upstream, root=ROOT):
    data = json.loads(Path(manifest).read_text(encoding='utf-8'))
    revision = data['revision']
    if not re.fullmatch(r'[0-9a-f]{40}', revision):
        raise ValueError('revision must be a full lowercase Git commit SHA')
    head = subprocess.check_output(['git', '-C', str(upstream), 'rev-parse', 'HEAD^{commit}'], text=True).strip()
    if head != revision:
        raise ValueError(f'upstream HEAD mismatch: {head} != {revision}')
    entries = data['imports']
    if not entries:
        raise ValueError('empty import list')
    seen = set()
    for entry in entries:
        source, target = entry['upstream'], entry['eve']
        for path in (source, target):
            p = PurePosixPath(path)
            if p.is_absolute() or '..' in p.parts or not p.parts:
                raise ValueError(f'unsafe path: {path}')
        if target in seen:
            raise ValueError(f'duplicate target: {target}')
        seen.add(target)
        path = (root / target).resolve()
        if not path.is_relative_to(root.resolve()):
            raise ValueError(f'target escapes repository: {target}')
        original = subprocess.check_output(['git', '-C', str(upstream), 'show', f'{revision}:{source}'])
        actual = path.read_bytes()
        expected = entry['sha256']
        if not re.fullmatch(r'[0-9a-f]{64}', expected):
            raise ValueError(f'invalid SHA-256: {target}')
        if hashlib.sha256(original).hexdigest() != expected or hashlib.sha256(actual).hexdigest() != expected:
            raise ValueError(f'byte mismatch: {source} -> {target}')
    return len(entries)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--upstream', required=True, help='independently authenticated pinned Telegram Git checkout')
    parser.add_argument('--manifest', default=str(ROOT / 'docs/telegram-verbatim-imports.json'))
    args = parser.parse_args()
    try:
        count = verify(args.manifest, Path(args.upstream))
    except (OSError, KeyError, ValueError, subprocess.CalledProcessError) as exc:
        print(f'FAIL: {exc}', file=sys.stderr)
        return 1
    print(f'PASS: {count} recorded byte-identical imports; no runtime parity or license conclusion')
    return 0


if __name__ == '__main__':
    sys.exit(main())
