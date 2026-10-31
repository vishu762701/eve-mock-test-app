"""No backend change may deploy a production Worker or D1 migration without consent."""
from pathlib import Path
import re
import unittest


class ProductionDeployApprovalGateTest(unittest.TestCase):
    def test_worker_requires_explicit_manual_production_approval(self):
        root = Path(__file__).resolve().parents[3]
        yaml = (root / ".github/workflows/deploy-worker.yml").read_text()
        self.assertRegex(yaml, r"(?m)^on:\s*\n  workflow_dispatch:")
        self.assertNotRegex(yaml, r"(?m)^  push:")
        self.assertNotRegex(yaml, r"(?m)^  schedule:")
        self.assertRegex(yaml, r"(?m)^      confirm_production:")
        self.assertRegex(yaml, r"(?m)^        type: boolean$")
        self.assertRegex(yaml, r"(?m)^        default: false$")
        self.assertIn("if: ${{ inputs.confirm_production == true }}", yaml)

    def test_only_approved_job_can_run_remote_migrations_or_worker_deploy(self):
        root = Path(__file__).resolve().parents[3]
        yaml = (root / ".github/workflows/deploy-worker.yml").read_text()
        self.assertEqual(len(re.findall(r"(?m)^jobs:$", yaml)), 1)
        self.assertEqual(len(re.findall(r"(?m)^  deploy:$", yaml)), 1)
        self.assertIn("wrangler d1 migrations apply eve-database --remote", yaml)
        self.assertIn("uses: cloudflare/wrangler-action@v3", yaml)
        self.assertLess(yaml.index("if: ${{ inputs.confirm_production == true }}"),
                        yaml.index("wrangler d1 migrations apply eve-database --remote"))

    def test_preflight_is_default_and_all_live_write_steps_are_guarded(self):
        root = Path(__file__).resolve().parents[3]
        yaml = (root / ".github/workflows/deploy-worker.yml").read_text()
        self.assertRegex(yaml, r"preflight_only:\n(?:.*\n){3}        default: true")
        self.assertIn("group: eve-production-worker", yaml)
        self.assertIn("cancel-in-progress: false", yaml)
        for name in ("Apply D1 Migrations", "Verify migrated remote schema before Worker deployment",
                     "Configure Worker Secrets (if present)", "Deploy Cloudflare Worker",
                     "Run Live Production Smoke Tests"):
            step = yaml.split("- name: " + name + "\n", 1)[1].split("\n      - name:", 1)[0]
            self.assertIn("if: ${{ inputs.preflight_only == false }}", step)
        self.assertLess(yaml.index("run: node scripts/production-preflight.mjs\n"),
                        yaml.index("wrangler d1 migrations apply"))
        self.assertLess(yaml.index("run: node scripts/production-preflight.mjs --post-migration"),
                        yaml.index("uses: cloudflare/wrangler-action@v3"))


if __name__ == "__main__":
    unittest.main()
