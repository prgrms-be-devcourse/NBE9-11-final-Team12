import json

from pydantic import BaseModel, ConfigDict, Field, field_validator


class AiReportModel(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)

    core_line: str = Field(alias="핵심 한줄")
    key_issues: list[str] = Field(alias="핵심 쟁점")
    ai_summary: str = Field(alias="AI 종합 정리")
    common_ground: str = Field(alias="공통 의견")
    ai_opinion: str = Field(alias="AI의 개인적 소견")


class CustomReportModel(BaseModel):
    label: str
    content: str

    @field_validator("label", "content")
    @classmethod
    def non_blank_text(cls, value):
        if not isinstance(value, str) or not value.strip():
            raise ValueError("customReports label and content must be non-blank strings")
        return value.strip()


def validate_report(report, require_base_report=True, expected_custom_report_count=None):
    validated = {}
    if require_base_report:
        validated.update(_validate_base_report(report))
    if expected_custom_report_count is not None:
        validated["customReports"] = _validate_custom_reports(
            report.get("customReports"),
            expected_custom_report_count,
        )
    return validated


def validate_report_file(path):
    try:
        report = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ValueError(f"Saved report file is not valid JSON: {path}") from exc

    return validate_report(report)


def _validate_base_report(report):
    try:
        validated = AiReportModel.model_validate(report)
        return validated.model_dump(by_alias=True)
    except Exception as exc:
        raise ValueError(f"Report schema validation failed: base report fields are required: {exc}") from exc


def _validate_custom_reports(custom_reports, expected_count):
    if not isinstance(custom_reports, list):
        raise ValueError(
            "Report schema validation failed: customPrompts were provided, "
            "so model response must include customReports as a list"
        )
    if len(custom_reports) != expected_count:
        raise ValueError(
            f"Report schema validation failed: customReports length must be {expected_count}, got {len(custom_reports)}"
        )

    try:
        return [
            CustomReportModel.model_validate(item).model_dump()
            for item in custom_reports
        ]
    except Exception as exc:
        raise ValueError(
            f"Report schema validation failed: customReports label and content must be non-blank strings: {exc}"
        ) from exc
