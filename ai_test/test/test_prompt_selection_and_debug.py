import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from aireport.prompt_security import NoOpPromptGuard, PromptSecurityService
from aireport.report_generator import (
    PROMPT_MODE_BASE,
    PROMPT_MODE_CUSTOM_WITH_BASE,
    PROMPT_MODE_CUSTOM_WITHOUT_BASE,
    ReportGenerationError,
    ReportGenerator,
)


class PromptSelectionAndDebugTest(unittest.TestCase):
    def setUp(self):
        self.clustering_patch = patch("aireport.report_generator.USE_TEXT_CLUSTERING", False)
        self.clustering_patch.start()

    def tearDown(self):
        self.clustering_patch.stop()

    def test_selects_base_prompt_when_custom_prompts_are_absent(self):
        generator = _generator("{}")

        generator.build_prompt(_payload())

        self.assertEqual(generator.last_prompt_mode, PROMPT_MODE_BASE)
        self.assertIn("BASE TEMPLATE", generator.last_prompt)

    def test_selects_custom_without_base_prompt_when_custom_prompts_exist_without_base_report(self):
        generator = _generator("{}")

        generator.build_prompt(_payload(custom_prompts=["minority view"]))

        self.assertEqual(generator.last_prompt_mode, PROMPT_MODE_CUSTOM_WITHOUT_BASE)
        self.assertIn("CUSTOM WITHOUT BASE TEMPLATE", generator.last_prompt)

    def test_selects_custom_with_base_prompt_when_custom_prompts_and_base_report_exist(self):
        generator = _generator("{}")

        generator.build_prompt(_payload(custom_prompts=["minority view"], base_report=_base_report()))

        self.assertEqual(generator.last_prompt_mode, PROMPT_MODE_CUSTOM_WITH_BASE)
        self.assertIn("CUSTOM WITH BASE TEMPLATE", generator.last_prompt)

    def test_writes_debug_file_when_model_response_has_no_json_object(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            debug_path = Path(tmp_dir) / "debug.json"
            generator = _generator("plain text response", debug_output_path=debug_path)

            with self.assertRaisesRegex(ReportGenerationError, "JSON object"):
                generator.generate(_payload(custom_prompts=["minority view"]))

            debug = json.loads(debug_path.read_text(encoding="utf-8"))
            self.assertEqual(debug["promptMode"], PROMPT_MODE_CUSTOM_WITHOUT_BASE)
            self.assertEqual(debug["rawResponse"], "plain text response")
            self.assertIn("CUSTOM WITHOUT BASE TEMPLATE", debug["prompt"])
            self.assertEqual(debug["validation"]["expectedCustomReportCount"], 1)
            self.assertTrue(debug["error"])


def _generator(response, debug_output_path=None):
    generator = ReportGenerator(
        _FakeModelClient(response),
        prompt_templates={
            PROMPT_MODE_BASE: "BASE TEMPLATE\n{{FEW_SHOT_EXAMPLES}}\n{{DEBATE_JSON}}",
            PROMPT_MODE_CUSTOM_WITHOUT_BASE: "CUSTOM WITHOUT BASE TEMPLATE\n{{FEW_SHOT_EXAMPLES}}\n{{DEBATE_JSON}}",
            PROMPT_MODE_CUSTOM_WITH_BASE: "CUSTOM WITH BASE TEMPLATE\n{{FEW_SHOT_EXAMPLES}}\n{{DEBATE_JSON}}",
        },
        prompt_security=PromptSecurityService(guard=NoOpPromptGuard()),
        debug_output_path=debug_output_path,
    )
    original_build_prompt = generator.build_prompt

    def tracking_build_prompt(debate):
        prompt = original_build_prompt(debate)
        generator.last_prompt = prompt
        return prompt

    generator.build_prompt = tracking_build_prompt
    return generator


class _FakeModelClient:
    def __init__(self, response):
        self.response = response

    def generate(self, prompt):
        return self.response


def _payload(custom_prompts=None, base_report=None):
    payload = {
        "topic": {"title": "topic"},
        "speeches": [
            {"content": "support", "stance": "PRO"},
            {"content": "oppose", "stance": "CON"},
        ],
    }
    if custom_prompts is not None:
        payload["customPrompts"] = [{"prompt": prompt} for prompt in custom_prompts]
    if base_report is not None:
        payload["baseReport"] = base_report
    return payload


def _base_report():
    return {
        "coreLine": "existing core",
        "keyIssues": ["issue 1"],
        "aiSummary": "existing summary",
        "commonGround": "existing common",
        "aiOpinion": "existing opinion",
    }


if __name__ == "__main__":
    unittest.main()
