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

    def test_dockerfile_applies_korean_instruction_override_patch(self):
        dockerfile = (ROOT / "Dockerfile").read_text(encoding="utf-8")
        patch = (ROOT / "patch_patterns.py").read_text(encoding="utf-8")

        self.assertIn("patch_patterns.py", dockerfile)
        self.assertIn("PATTERNS_KO", patch)
        self.assertIn("이전", patch)
        self.assertIn("지시", patch)
        self.assertIn("무시", patch)

    def test_dockerfile_adds_korean_critical_yaml_patterns(self):
        patch = (ROOT / "patch_patterns.py").read_text(encoding="utf-8")

        self.assertIn("CRITICAL_YAML_FILE", patch)
        self.assertIn("ko_secret_exfiltration", patch)
        self.assertIn("ko_prompt_extraction", patch)
        self.assertIn("ko_cognitive_rootkit", patch)
        self.assertIn("ko_covert_exfiltration", patch)

    def test_dockerfile_adds_korean_high_yaml_patterns(self):
        patch = (ROOT / "patch_patterns.py").read_text(encoding="utf-8")

        self.assertIn("HIGH_YAML_FILE", patch)
        self.assertIn("ko_instruction_override", patch)
        self.assertIn("ko_jailbreak", patch)
        self.assertIn("ko_indirect_injection", patch)
        self.assertIn("ko_memory_poisoning", patch)
        self.assertIn("ko_action_gate_bypass", patch)

    def test_dockerfile_adds_korean_medium_yaml_patterns(self):
        patch = (ROOT / "patch_patterns.py").read_text(encoding="utf-8")

        self.assertIn("MEDIUM_YAML_FILE", patch)
        self.assertIn("ko_role_manipulation", patch)
        self.assertIn("ko_context_hijacking", patch)
        self.assertIn("ko_output_manipulation", patch)
        self.assertIn("ko_bypass_coaching", patch)
        self.assertIn("ko_recursive_delegation", patch)


if __name__ == "__main__":
    unittest.main()
