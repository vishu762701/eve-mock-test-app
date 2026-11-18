"""Ensure Telegram's verified original cubic Bezier math stays canonical."""
from pathlib import Path
from hashlib import sha1
import unittest

ROOT = Path(__file__).resolve().parents[3]
BASE = ROOT / "app/src/main/java"

class TelegramInterpolatorBridgeTest(unittest.TestCase):
    def test_unchanged_original_telegram_blob(self):
        path = BASE / "org/telegram/ui/Components/CubicBezierInterpolator.java"
        payload = path.read_bytes()
        identity = sha1(b"blob " + str(len(payload)).encode("ascii") + b"\0" + payload).hexdigest()
        self.assertEqual("3a9cae100e17a03abacf2f4df71db2b82757b968", identity)

    def test_duplicate_interpolator_copies_removed_in_favor_of_canonical(self):
        for relative in (
            "com/eve/app/telegram/components/CubicBezierInterpolator.java",
            "com/eve/app/ui/profile/popup/CubicBezierInterpolator.java",
        ):
            with self.subTest(relative=relative):
                self.assertFalse((BASE / relative).exists(), f"{relative} must be deleted in favor of org/telegram copy")

if __name__ == "__main__":
    unittest.main()
