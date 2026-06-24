import json
import unittest
from unittest.mock import patch

from aireport.input_contract import normalize_report_request
from aireport.api import AiReportGenerateResponse
from aireport.report_generator import ReportGenerator
from aireport.report_schema import AiReportModel, validate_report
from aireport.prompt_security import NoOpPromptGuard, PromptSecurityService


class BaseReportContractTest(unittest.TestCase):
    def test_normalizes_base_report_and_omits_unneeded_fields(self):
        payload = {
            "speeches": [{"content": "support", "stance": "PRO"}],
            "baseReport": {
                "coreLine": "existing core",
                "keyIssues": ["issue 1"],
                "aiSummary": "existing summary",
                "commonGround": "existing common",
                "aiOpinion": "existing opinion",
                "user": {"id": 7},
                "linkUrl": "https://example.com",
                "imageUrl": "https://example.com/image.png",
                "updatedAt": "2026-06-23T00:00:00",
            },
        }

        normalized = normalize_report_request(payload)

        self.assertEqual(
            normalized["baseReport"],
            {
                "coreLine": "existing core",
                "keyIssues": ["issue 1"],
                "aiSummary": "existing summary",
                "commonGround": "existing common",
                "aiOpinion": "existing opinion",
            },
        )


class ConditionalReportSchemaTest(unittest.TestCase):
    def test_base_report_is_required_when_request_has_no_base_report(self):
        with self.assertRaisesRegex(ValueError, "base report fields"):
            validate_report(
                {"customReports": [{"label": "Minority view", "content": "summary"}]},
                require_base_report=True,
                expected_custom_report_count=1,
            )

    def test_custom_reports_only_is_valid_when_base_report_exists(self):
        report = validate_report(
            {"customReports": [{"label": "Minority view", "content": "summary"}]},
            require_base_report=False,
            expected_custom_report_count=1,
        )

        self.assertEqual(report, {"customReports": [{"label": "Minority view", "content": "summary"}]})

    def test_custom_report_count_must_match_custom_prompts(self):
        with self.assertRaisesRegex(ValueError, "customReports length"):
            validate_report(
                {"customReports": [{"label": "Only one", "content": "summary"}]},
                require_base_report=False,
                expected_custom_report_count=2,
            )

    def test_custom_report_label_and_content_must_be_non_blank(self):
        with self.assertRaisesRegex(ValueError, "non-blank"):
            validate_report(
                {"customReports": [{"label": " ", "content": "summary"}]},
                require_base_report=False,
                expected_custom_report_count=1,
            )


class AiReportApiResponseContractTest(unittest.TestCase):
    def test_response_model_accepts_custom_reports_only(self):
        response = AiReportGenerateResponse.model_validate({
            "customReports": [{"label": "Minority view", "content": "summary"}],
        })

        self.assertEqual(response.customReports[0]["label"], "Minority view")


class ReportGeneratorCustomReportTest(unittest.TestCase):
    def setUp(self):
        self.clustering_patch = patch("aireport.report_generator.USE_TEXT_CLUSTERING", False)
        self.clustering_patch.start()

    def tearDown(self):
        self.clustering_patch.stop()

    def test_base_report_absent_custom_prompts_generates_base_and_custom_reports(self):
        model_client = _FakeModelClient(_report_json(custom_reports=[{"label": "Minority view", "content": "summary"}]))
        generator = _generator(model_client)

        report = generator.generate(_payload(base_report=None, custom_prompts=["summarize minority view"]))

        self.assertIn(AiReportModel.model_fields["core_line"].alias, report)
        self.assertEqual(len(report["customReports"]), 1)

    def test_base_report_present_custom_prompts_allows_custom_reports_only(self):
        model_client = _FakeModelClient(json.dumps({
            "customReports": [{"label": "Minority view", "content": "summary"}],
        }))
        generator = _generator(model_client)

        report = generator.generate(_payload(base_report=_base_report(), custom_prompts=["summarize minority view"]))

        self.assertEqual(report, {"customReports": [{"label": "Minority view", "content": "summary"}]})
        self.assertIn('"baseReport"', generator.last_prompt)
        self.assertNotIn('"user"', generator.last_prompt)

    def test_base_report_present_custom_prompts_rejects_base_regeneration_only(self):
        model_client = _FakeModelClient(_report_json())
        generator = _generator(model_client)

        with self.assertRaisesRegex(Exception, "customReports"):
            generator.generate(_payload(base_report=_base_report(), custom_prompts=["summarize minority view"]))


class _FakeModelClient:
    def __init__(self, response):
        self.response = response
        self.prompts = []

    def generate(self, prompt):
        self.prompts.append(prompt)
        return self.response


def _generator(model_client):
    generator = ReportGenerator(
        model_client,
        prompt_template="SYSTEM\n{{FEW_SHOT_EXAMPLES}}\nINPUT\n{{DEBATE_JSON}}",
        prompt_security=PromptSecurityService(guard=NoOpPromptGuard()),
        debug_output_path=None,
    )
    original_build_prompt = generator.build_prompt

    def tracking_build_prompt(debate):
        prompt = original_build_prompt(debate)
        generator.last_prompt = prompt
        return prompt

    generator.build_prompt = tracking_build_prompt
    return generator


def _payload(base_report, custom_prompts):
    payload = {
        "topic": {"title": "topic"},
        "speeches": [
            {"content": "support", "stance": "PRO"},
            {"content": "oppose", "stance": "CON"},
        ],
        "customPrompts": [{"prompt": prompt} for prompt in custom_prompts],
    }
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
        "user": {"id": 7},
    }


def _report_json(core_line="core", custom_reports=None):
    fields = AiReportModel.model_fields
    report = {
        fields["core_line"].alias: core_line,
        fields["key_issues"].alias: ["issue"],
        fields["ai_summary"].alias: "summary",
        fields["common_ground"].alias: "common",
        fields["ai_opinion"].alias: "opinion",
    }
    if custom_reports is not None:
        report["customReports"] = custom_reports
    return json.dumps(report, ensure_ascii=False)


if __name__ == "__main__":
    unittest.main()
