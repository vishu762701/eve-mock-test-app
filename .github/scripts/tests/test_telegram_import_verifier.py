"""Exercise import verification with real Git objects and changed bytes."""
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'verify_telegram_imports.py'
spec = importlib.util.spec_from_file_location('telegram_imports', SCRIPT)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ImportVerifierTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.upstream = self.root / 'upstream'
        self.upstream.mkdir()
        self.eve = self.root / 'eve'
        self.eve.mkdir()
        def git(*args):
            return subprocess.check_output(['git', '-C', str(self.upstream), *args], stderr=subprocess.DEVNULL, text=True).strip()
        git('init', '-q')
        git('config', 'user.email', 'test@example.invalid')
        git('config', 'user.name', 'Test')
        (self.upstream / 'asset').write_bytes(b'original')
        git('add', 'asset')
        git('commit', '-qm', 'fixture')
        self.revision = git('rev-parse', 'HEAD')
        (self.eve / 'copy').write_bytes(b'original')
        self.manifest = self.root / 'manifest.json'
        self.data = {'revision': self.revision, 'imports': [{'upstream': 'asset', 'eve': 'copy', 'sha256': hashlib.sha256(b'original').hexdigest()}]}
        self.save()

    def save(self):
        self.manifest.write_text(json.dumps(self.data))

    def test_matches_git_object_not_dirty_worktree(self):
        (self.upstream / 'asset').write_bytes(b'dirty')
        self.assertEqual(1, module.verify(self.manifest, self.upstream, self.eve))

    def test_changed_eve_asset_fails(self):
        (self.eve / 'copy').write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError, 'byte mismatch'):
            module.verify(self.manifest, self.upstream, self.eve)

    def test_wrong_revision_and_digest_fail(self):
        self.data['revision'] = 'a' * 40
        self.save()
        with self.assertRaisesRegex(ValueError, 'HEAD mismatch'):
            module.verify(self.manifest, self.upstream, self.eve)
        self.data['revision'] = self.revision
        self.data['imports'][0]['sha256'] = '0' * 64
        self.save()
        with self.assertRaisesRegex(ValueError, 'byte mismatch'):
            module.verify(self.manifest, self.upstream, self.eve)

    def test_unsafe_or_duplicate_paths_fail(self):
        self.data['imports'][0]['eve'] = '../escape'
        self.save()
        with self.assertRaisesRegex(ValueError, 'unsafe path'):
            module.verify(self.manifest, self.upstream, self.eve)
        self.data['imports'][0]['eve'] = 'copy'
        self.data['imports'].append(dict(self.data['imports'][0]))
        self.save()
        with self.assertRaisesRegex(ValueError, 'duplicate target'):
            module.verify(self.manifest, self.upstream, self.eve)
