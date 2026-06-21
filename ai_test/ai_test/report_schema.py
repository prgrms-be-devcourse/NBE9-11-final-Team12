REQUIRED_REPORT_FIELDS = (
    "핵심 한줄",
    "핵심 쟁점",
    "AI 종합 정리",
    "공통 의견",
    "AI의 개인적 소견",
)


def validate_report(report):
    # 리포트 UI와 저장 구조가 기대하는 최소 필드를 먼저 확인합니다.
    # 모델이 필드를 빠뜨리면 품질 문제가 아니라 실행 실패로 명확히 드러나게 합니다.
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

    return report
