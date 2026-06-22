import json

from pydantic import BaseModel, ConfigDict, Field


REQUIRED_REPORT_FIELDS = (
    "핵심 한줄",
    "핵심 쟁점",
    "AI 종합 정리",
    "공통 의견",
    "AI의 개인적 소견",
)


class AiReportModel(BaseModel):
    model_config = ConfigDict(extra="allow", populate_by_name=True)

    core_line: str = Field(alias="핵심 한줄")
    key_issues: list[str] = Field(alias="핵심 쟁점")
    ai_summary: str = Field(alias="AI 종합 정리")
    common_ground: str = Field(alias="공통 의견")
    ai_opinion: str = Field(alias="AI의 개인적 소견")


def validate_report(report):
    # 리포트 UI와 저장 구조가 기대하는 최소 필드를 먼저 확인합니다.
    # 모델이 필드를 빠뜨리면 품질 문제가 아니라 실행 실패로 명확히 드러나게 합니다.
    return _validate_report_with_pydantic(report)


def validate_report_file(path):
    # 실제 서비스에서도 저장 직후 파일/응답 JSON을 다시 읽어 스키마가 깨지지 않았는지 확인합니다.
    try:
        report = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ValueError(f"Saved report file is not valid JSON: {path}") from exc

    return validate_report(report)


def _validate_report_with_pydantic(report):
    try:
        if hasattr(AiReportModel, "model_validate"):
            validated = AiReportModel.model_validate(report)
            return validated.model_dump(by_alias=True)

        validated = AiReportModel.parse_obj(report)
        return validated.dict(by_alias=True)
    except Exception as exc:
        raise ValueError(f"Report schema validation failed: {exc}") from exc
