import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


class PromptGuardContainerContractTest(unittest.TestCase):

    def test_dockerfile_uses_official_prompt_guard_repository(self):
        dockerfile = (ROOT / "Dockerfile").read_text(encoding="utf-8")

        self.assertIn("https://github.com/seojoonkim/prompt-guard.git", dockerfile)
        self.assertIn("pip install --no-cache-dir -e", dockerfile)
        self.assertIn("uvicorn", dockerfile)
        self.assertIn("app:app", dockerfile)
        self.assertNotIn("prompt_guard_app.app:app", dockerfile)

    def test_config_blocks_high_and_critical_without_external_reporting(self):
        config = (ROOT / "config.yaml").read_text(encoding="utf-8")

        self.assertIn("HIGH: block", config)
        self.assertIn("CRITICAL: block", config)
        self.assertIn("enabled: false", config)
        self.assertIn("reporting: false", config)


if __name__ == "__main__":
    unittest.main()
