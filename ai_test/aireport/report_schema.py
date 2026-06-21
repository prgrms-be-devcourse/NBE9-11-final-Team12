import json


REQUIRED_REPORT_FIELDS = (
    "핵심 한줄",
    "핵심 쟁점",
    "AI 종합 정리",
    "공통 의견",
    "AI의 개인적 소견",
)


try:
    from pydantic import BaseModel, Field

    class AiReportModel(BaseModel):
        core_line: str = Field(alias="핵심 한줄")
        key_issues: list[str] = Field(alias="핵심 쟁점")
        ai_summary: str = Field(alias="AI 종합 정리")
        common_ground: str = Field(alias="공통 의견")
        ai_opinion: str = Field(alias="AI의 개인적 소견")

        class Config:
            extra = "allow"
            allow_population_by_field_name = True

    PYDANTIC_AVAILABLE = True
except ImportError:
    AiReportModel = None
    PYDANTIC_AVAILABLE = False


def validate_report(report):
    # 리포트 UI와 저장 구조가 기대하는 최소 필드를 먼저 확인합니다.
    # 모델이 필드를 빠뜨리면 품질 문제가 아니라 실행 실패로 명확히 드러나게 합니다.
    if PYDANTIC_AVAILABLE:
        return _validate_report_with_pydantic(report)

    if not isinstance(report, dict):
        raise ValueError("Report must be a JSON object")

    missing = [field for field in REQUIRED_REPORT_FIELDS if field not in report]
    if missing:
        raise ValueError(f"Missing required report fields: {', '.join(missing)}")

    list_fields = ("핵심 쟁점",)
    # 목록으로 보여줄 영역은 반드시 배열이어야 후속 UI/백엔드 매핑이 단순해집니다.
    invalid_list_fields = [
        field for field in list_fields if not isinstance(report.get(field), list)
    ]
    if invalid_list_fields:
        raise ValueError(
            "Report fields must be lists: " + ", ".join(invalid_list_fields)
        )

    invalid_string_fields = [
        field
        for field in REQUIRED_REPORT_FIELDS
        if field not in list_fields and not isinstance(report.get(field), str)
    ]
    if invalid_string_fields:
        raise ValueError(
            "Report fields must be strings: " + ", ".join(invalid_string_fields)
        )

    return report


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
