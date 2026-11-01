"""Real fixture tests for the scoped Telegram acceptance claim guard."""
import json
from pathlib import Path
import tempfile
import unittest
from importlib.util import module_from_spec, spec_from_file_location

SCRIPT = Path(__file__).resolve().parents[1] / 'verify_telegram_acceptance.py'
spec = spec_from_file_location('telegram_acceptance', SCRIPT)
module = module_from_spec(spec)
spec.loader.exec_module(module)


class TelegramAcceptanceGateTest(unittest.TestCase):
    def test_current_manifest_has_explicit_unaccepted_scope(self):
        root = Path(__file__).resolve().parents[3]
        errors, ids = module.verify(root / 'docs/telegram-profile-fidelity.json', root)
        self.assertEqual([], errors)
        self.assertEqual(['personal-qr', 'profile-overflow', 'profile-shell'], ids)
        errors, _ = module.verify(root / 'docs/telegram-profile-fidelity.json', root, ['personal-qr'])
        self.assertTrue(any('not accepted' in e for e in errors))

    def test_claim_cannot_skip_evidence_or_real_paths(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for path in ('app/main.kt', 'test/test.kt', 'docs/provenance.md'):
                p = root / path
                p.parent.mkdir(parents=True, exist_ok=True)
                p.write_text('fixture', encoding='utf-8')
            target = {
                'id': 'personal-qr', 'status': 'ACCEPTED',
                'upstream_paths': ['TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrRender.java'],
                'eve_paths': ['app/main.kt'], 'tests': ['test/test.kt'],
                'source_map': 'docs/provenance.md', 'remaining_gaps': [],
            }
            data = {'schema': 1, 'official_source': 'https://github.com/DrKLO/Telegram',
                    'source_commit': 'a' * 40, 'archive_sha256': 'b' * 64,
                    'targets': [target]}
            m = root / 'manifest.json'
            m.write_text(json.dumps(data), encoding='utf-8')
            errors, _ = module.verify(m, root, ['personal-qr'])
            self.assertTrue(any('requires evidence' in e for e in errors))
            target['status'] = 'OPEN'
            target['remaining_gaps'] = ['Original QR scene and motion are unimplemented.']
            m.write_text(json.dumps(data), encoding='utf-8')
            errors, _ = module.verify(m, root)
            self.assertEqual([], errors)
            errors, _ = module.verify(m, root, ['personal-qr'])
            self.assertTrue(any('not accepted' in e for e in errors))
            target['eve_paths'] = ['../outside.kt']
            m.write_text(json.dumps(data), encoding='utf-8')
            errors, _ = module.verify(m, root)
            self.assertTrue(any('safe relative paths' in e for e in errors))

    def test_forged_or_missing_evidence_cannot_claim_acceptance(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for path in ('app/x.kt', 'test/x.kt', 'docs/map.md'):
                p = root / path
                p.parent.mkdir(parents=True, exist_ok=True)
                p.write_text('fixture', encoding='utf-8')
            bad = {key: {'path': 'docs/map.md', 'sha256': '0' * 64}
                   for key in module.REQUIRED_EVIDENCE}
            data = {'schema': 1, 'official_source': 'https://github.com/DrKLO/Telegram',
                    'source_commit': 'a' * 40, 'archive_sha256': 'b' * 64,
                    'targets': [{'id': 'profile-shell', 'status': 'ACCEPTED',
                        'upstream_paths': ['TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java'],
                        'eve_paths': ['app/x.kt'], 'tests': ['test/x.kt'],
                        'source_map': 'docs/map.md', 'remaining_gaps': [], 'evidence': bad}]}
            m = root / 'manifest.json'
            m.write_text(json.dumps(data), encoding='utf-8')
            errors, _ = module.verify(m, root)
            self.assertTrue(any('checksum mismatch' in e for e in errors))


if __name__ == '__main__':
    unittest.main()
