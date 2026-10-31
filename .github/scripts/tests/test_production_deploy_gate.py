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


if __name__ == "__main__":
    unittest.main()
