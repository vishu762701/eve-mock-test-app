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

    def test_legacy_packages_inherit_original_with_compatible_fields(self):
        for relative in (
            "com/eve/app/telegram/components/CubicBezierInterpolator.java",
            "com/eve/app/ui/profile/popup/CubicBezierInterpolator.java",
        ):
            with self.subTest(relative=relative):
                code = (BASE / relative).read_text(encoding="utf-8")
                self.assertIn("extends org.telegram.ui.Components.CubicBezierInterpolator", code)
                self.assertIn("super(start, end)", code)
                self.assertIn("super(startX, startY, endX, endY)", code)
                for field in ("DEFAULT", "EASE_OUT", "EASE_OUT_QUINT", "EASE_IN", "EASE_BOTH", "EASE_OUT_BACK"):
                    self.assertIn("static final CubicBezierInterpolator " + field, code)
                self.assertNotIn("getInterpolation(", code)
                self.assertNotIn("getXForTime(", code)
                self.assertNotIn("getBezierCoordinateX(", code)

if __name__ == "__main__":
    unittest.main()
