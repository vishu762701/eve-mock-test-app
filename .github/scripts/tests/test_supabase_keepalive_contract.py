"""Offline behavioral contracts for the existing Supabase GitHub keepalive script."""

import os
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
WORKFLOW = ROOT / ".github/workflows/supabase-keepalive.yml"


def keepalive_script():
    lines = WORKFLOW.read_text(encoding="utf-8").splitlines()
    index = lines.index("        run: |") + 1
    body = []
    for line in lines[index:]:
        if line and not line.startswith("          "):
            break
        body.append(line[10:] if line else "")
    if not body:
        raise AssertionError("Keepalive script block is empty")
    return "\n".join(body) + "\n"


class SupabaseKeepaliveContractTest(unittest.TestCase):
    def run_case(self, *, url="", key="", db_url="", simulated_http="200"):
        script = keepalive_script()
        with tempfile.TemporaryDirectory() as tmp:
            curl = Path(tmp) / "curl"
            curl.write_text(
                "#!/usr/bin/env bash\n"
                "printf 'HTTP_STATUS:" + simulated_http + "\\n'\n",
                encoding="utf-8",
            )
            curl.chmod(0o755)
            env = {
                **os.environ,
                "PATH": tmp + os.pathsep + os.environ.get("PATH", ""),
                "SUPABASE_PROJECT_URL": url,
                "SUPABASE_KEY": key,
                "SUPABASE_DB_URL": db_url,
            }
            return subprocess.run(
                ["bash", "-c", script],
                text=True,
                env=env,
                capture_output=True,
                timeout=15,
                check=False,
            )

    def test_no_embedded_or_logged_unverified_project_url(self):
        script = keepalive_script()
        self.assertNotIn(":-https://", script)
        self.assertNotIn("Target URL: $TARGET_URL", script)
        self.assertIn('TARGET_URL="${SUPABASE_PROJECT_URL:-}"', script)
        self.assertIn("No SUPABASE_PROJECT_URL secret configured", script)

    def test_missing_project_identity_fails_without_attempting_network(self):
        result = self.run_case()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("No SUPABASE_PROJECT_URL", result.stdout)
        self.assertNotIn("Executing lightweight database query", result.stdout)

    def test_insecure_or_credential_bearing_project_url_is_rejected(self):
        for url in ["http://example.test", "https://user:password@example.test",
                    "https://example.test/rest/v1", "https://example.test?token=abc"]:
            with self.subTest(url=url):
                result = self.run_case(url=url, key="test-key")
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("verified HTTPS project origin", result.stdout)
                self.assertNotIn("Executing lightweight database query", result.stdout)

    def test_missing_key_does_not_attempt_postgrest(self):
        result = self.run_case(url="https://verified.example.test")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("SUPABASE_SERVICE_ROLE_KEY or SUPABASE_ANON_KEY", result.stdout)
        self.assertNotIn("Executing lightweight database query", result.stdout)

    def test_configured_url_with_mocked_postgrest_query_succeeds(self):
        result = self.run_case(url="https://verified.example.test/",
                               key="dummy-test-only", simulated_http="200")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertIn("HTTP 200 OK", result.stdout)
        self.assertNotIn("verified.example.test", result.stdout)

    def test_dns_failure_stays_failure_and_never_fabricates_activity(self):
        result = self.run_case(url="https://verified.example.test",
                               key="dummy-test-only", simulated_http="000")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("HTTP 000 / 000", result.stdout)
        self.assertNotIn("Legitimate database activity registered", result.stdout)


if __name__ == "__main__":
    unittest.main()
