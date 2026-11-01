#!/usr/bin/env python3
"""Fail closed on unsupported Telegram UI acceptance claims.

This verifies evidence provenance and completeness, NOT visual equivalence.
Screenshots/motion still require a human comparison to the same official build.
"""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import struct
import sys

ROOT = Path(__file__).resolve().parents[2]
REQUIRED_EVIDENCE = (
    'telegram_reference', 'eve_capture', 'motion_trace', 'native_test_report',
    'comparison_report', 'license_disposition',
)


def _relative_path(raw):
    if not isinstance(raw, str) or not raw or '\\' in raw:
        return None
    p = PurePosixPath(raw)
    if p.is_absolute() or '..' in p.parts or '.' in p.parts or ':' in raw:
        return None
    return p


def _file(root, raw):
    p = _relative_path(raw)
    if p is None:
        return None
    candidate = (root / str(p)).resolve()
    if not candidate.is_relative_to(root.resolve()) or not candidate.is_file():
        return None
    return candidate


def verify(manifest, root=ROOT, required=()):
    errors = []
    try:
        data = json.loads(Path(manifest).read_text(encoding='utf-8'))
    except (OSError, UnicodeError, ValueError) as exc:
        return [f'manifest cannot be read: {exc}'], []
    if data.get('schema') != 1:
        errors.append('schema must be 1')
    if data.get('official_source') != 'https://github.com/DrKLO/Telegram':
        errors.append('official_source must be DrKLO/Telegram')
    if not re.fullmatch(r'[0-9a-f]{40}', str(data.get('source_commit', ''))):
        errors.append('source_commit must be a full lowercase Git SHA')
    if not re.fullmatch(r'[0-9a-f]{64}', str(data.get('archive_sha256', ''))):
        errors.append('archive_sha256 must be full SHA-256')
    items = data.get('targets')
    if not isinstance(items, list) or not items:
        return errors + ['targets must be a nonempty list'], []
    ids = set()
    for t in items:
        if not isinstance(t, dict):
            errors.append('target must be an object')
            continue
        ident = t.get('id')
        if not isinstance(ident, str) or not re.fullmatch(r'[a-z][a-z0-9-]*', ident) or ident in ids:
            errors.append(f'invalid or duplicate target id: {ident}')
            continue
        ids.add(ident)
        state = t.get('status')
        if state not in ('OPEN', 'ACCEPTED'):
            errors.append(f'{ident}: invalid status {state}')
        for label in ('upstream_paths', 'eve_paths', 'tests'):
            paths = t.get(label)
            if not isinstance(paths, list) or not paths or any(_relative_path(p) is None for p in paths):
                errors.append(f'{ident}: {label} must contain safe relative paths')
                continue
            if label == 'upstream_paths':
                if any(not p.startswith('TMessagesProj/src/main/') for p in paths):
                    errors.append(f'{ident}: upstream paths must be within TMessagesProj/src/main')
            else:
                for path in paths:
                    if _file(root, path) is None:
                        errors.append(f'{ident}: missing {label} file: {path}')
        if _file(root, t.get('source_map')) is None:
            errors.append(f'{ident}: source_map must name an existing repository file')
        gaps = t.get('remaining_gaps')
        if not isinstance(gaps, list) or any(not isinstance(g, str) or len(g.strip()) < 10 for g in gaps):
            errors.append(f'{ident}: remaining_gaps must be a list of concrete issues')
            gaps = []
        if state == 'OPEN' and not gaps:
            errors.append(f'{ident}: OPEN must preserve remaining gaps')
        if state == 'ACCEPTED':
            if gaps:
                errors.append(f'{ident}: ACCEPTED cannot retain open gaps')
            evidence = t.get('evidence')
            if not isinstance(evidence, dict):
                errors.append(f'{ident}: ACCEPTED requires evidence object')
                continue
            checked = {}
            for kind in REQUIRED_EVIDENCE:
                entry = evidence.get(kind)
                if not isinstance(entry, dict):
                    errors.append(f'{ident}: missing {kind} evidence')
                    continue
                path = _file(root, entry.get('path'))
                digest = entry.get('sha256')
                if path is None or not re.fullmatch(r'[0-9a-f]{64}', str(digest or '')):
                    errors.append(f'{ident}: invalid {kind} path/digest')
                    continue
                if hashlib.sha256(path.read_bytes()).hexdigest() != digest:
                    errors.append(f'{ident}: {kind} checksum mismatch')
                    continue
                checked[kind] = path
            for kind in ('telegram_reference', 'eve_capture'):
                p = checked.get(kind)
                if p:
                    b = p.read_bytes()[:24]
                    if p.suffix.lower() != '.png' or len(b) < 24 or b[:8] != b'\x89PNG\r\n\x1a\n' or b[12:16] != b'IHDR':
                        errors.append(f'{ident}: {kind} must be an actual PNG header')
            if all(k in checked for k in ('telegram_reference', 'eve_capture')):
                def dimensions(p):
                    return struct.unpack('>II', p.read_bytes()[16:24])
                if dimensions(checked['telegram_reference']) != dimensions(checked['eve_capture']):
                    errors.append(f'{ident}: unmatched screenshot sizes; use same device/viewport')
            report = checked.get('comparison_report')
            if report:
                try:
                    verdict = json.loads(report.read_text(encoding='utf-8'))
                    if verdict.get('result') != 'HUMAN_REVIEWED_PASS' or not verdict.get('reviewer') or not verdict.get('motion_checked'):
                        errors.append(f'{ident}: comparison report lacks human-reviewed visual/motion sign-off')
                    if verdict.get('source_commit') != data.get('source_commit'):
                        errors.append(f'{ident}: comparison report source commit differs')
                except (OSError, UnicodeError, ValueError):
                    errors.append(f'{ident}: comparison report is not valid JSON')
    for ident in required:
        if ident not in ids:
            errors.append(f'required target absent: {ident}')
        elif next((t.get('status') for t in items if isinstance(t, dict) and t.get('id') == ident), None) != 'ACCEPTED':
            errors.append(f'{ident}: not accepted; do not claim Telegram parity')
    return errors, sorted(ids)


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--manifest', type=Path, default=ROOT / 'docs/telegram-profile-fidelity.json')
    p.add_argument('--root', type=Path, default=ROOT)
    p.add_argument('--require-accepted', action='append', default=[], metavar='TARGET')
    args = p.parse_args()
    errors, ids = verify(args.manifest, args.root, args.require_accepted)
    for problem in errors:
        print('FAIL:', problem, file=sys.stderr)
    if errors:
        return 1
    print('PASS: acceptance claims structurally checked; targets:', ', '.join(ids))
    print('NOTE: this gate cannot establish pixel, motion, source or licensing equivalence')
    return 0


if __name__ == '__main__':
    sys.exit(main())
