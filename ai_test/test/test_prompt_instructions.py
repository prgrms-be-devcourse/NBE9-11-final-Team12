import unittest
from pathlib import Path

from aireport.report_generator import (
    DEFAULT_CUSTOM_WITH_BASE_PROMPT_TEMPLATE,
    DEFAULT_CUSTOM_WITHOUT_BASE_PROMPT_TEMPLATE,
    DEFAULT_PROMPT_TEMPLATE,
)


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class PromptInstructionTest(unittest.TestCase):
    def test_prompt_templates_are_split_by_request_shape(self):
        base_prompt = (PROJECT_ROOT / "prompts" / "report_base_prompt.md").read_text(encoding="utf-8")
        custom_without_base_prompt = (
            PROJECT_ROOT / "prompts" / "report_custom_without_base_prompt.md"
        ).read_text(encoding="utf-8")
        custom_with_base_prompt = (
            PROJECT_ROOT / "prompts" / "report_custom_with_base_prompt.md"
        ).read_text(encoding="utf-8")

        self.assertNotIn('"customReports":', base_prompt)
        self.assertIn("customReports", custom_without_base_prompt)
        self.assertIn("customReports", custom_with_base_prompt)
        self.assertIn('"핵심 한줄"', custom_without_base_prompt)
        self.assertNotIn('"핵심 한줄":', custom_with_base_prompt)

        for text in (
            base_prompt,
            custom_without_base_prompt,
            custom_with_base_prompt,
            DEFAULT_PROMPT_TEMPLATE,
            DEFAULT_CUSTOM_WITHOUT_BASE_PROMPT_TEMPLATE,
            DEFAULT_CUSTOM_WITH_BASE_PROMPT_TEMPLATE,
        ):
            self.assertIn("{{DEBATE_JSON}}", text)
            self.assertNotIn("CUSTOM_WITHOUT_BASE", text)
            self.assertNotIn("CUSTOM_WITH_BASE", text)

    def test_few_shot_examples_include_custom_reports_output(self):
        few_shot_text = (PROJECT_ROOT / "prompts" / "few_shot_examples.md").read_text(encoding="utf-8")

        self.assertIn("customPrompts", few_shot_text)
        self.assertIn('"customReports"', few_shot_text)
        self.assertIn('"label"', few_shot_text)
        self.assertIn('"content"', few_shot_text)


if __name__ == "__main__":
    unittest.main()
