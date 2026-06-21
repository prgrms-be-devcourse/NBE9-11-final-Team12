import json
import re
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from aireport.report_schema import validate_report_file


GENERATED_REPORT_PATH = PROJECT_ROOT / "outputs" / "latest_ai_report.json"
REFERENCE_REPORT_PATH = PROJECT_ROOT / "test" / "reference_report.json"
SCU_PATH = PROJECT_ROOT / "test" / "scu.json"
EVALUATION_OUTPUT_PATH = PROJECT_ROOT / "outputs" / "latest_evaluation_report.json"


def main():
    print("AI 리포트 성능 평가를 시작합니다.", flush=True)

    generated_report = _load_valid_generated_report()
    generated_text = _report_to_text(generated_report)
    result = {
        "jsonSchemaValidation": {
            "passed": True,
            "target": str(GENERATED_REPORT_PATH),
        },
        "rouge": None,
        "bertScore": None,
        "sourceCoverage": None,
        "humanEvaluation": _human_evaluation_guide(),
    }

    if REFERENCE_REPORT_PATH.exists():
        reference_report = validate_report_file(REFERENCE_REPORT_PATH)
        reference_text = _report_to_text(reference_report)
        result["rouge"] = calculate_rouge_scores(reference_text, generated_text)
        result["bertScore"] = calculate_optional_bert_score(reference_text, generated_text)
    else:
        print(f"정답 리포트가 없어 ROUGE/BERTScore를 건너뜁니다: {REFERENCE_REPORT_PATH}", flush=True)

    if SCU_PATH.exists():
        result["sourceCoverage"] = calculate_scu_coverage(generated_text, _read_json(SCU_PATH))
    else:
        print(f"SCU 파일이 없어 쟁점 커버리지 평가를 건너뜁니다: {SCU_PATH}", flush=True)

    _save_json(result, EVALUATION_OUTPUT_PATH)
    print(json.dumps(result, ensure_ascii=False, indent=2), flush=True)
    print(f"평가 결과 저장 위치: {EVALUATION_OUTPUT_PATH}", flush=True)


def _load_valid_generated_report():
    if not GENERATED_REPORT_PATH.exists():
        raise FileNotFoundError(
            f"생성 리포트 파일이 없습니다. 먼저 run_ai_report.py를 실행하세요: {GENERATED_REPORT_PATH}"
        )

    report = validate_report_file(GENERATED_REPORT_PATH)
    print("생성 리포트 JSON 파싱 및 스키마 검증 완료", flush=True)
    return report


def calculate_rouge_scores(reference_text, generated_text):
    reference_tokens = _tokenize(reference_text)
    generated_tokens = _tokenize(generated_text)
    return {
        "rouge1": _f1_overlap(_ngrams(reference_tokens, 1), _ngrams(generated_tokens, 1)),
        "rouge2": _f1_overlap(_ngrams(reference_tokens, 2), _ngrams(generated_tokens, 2)),
        "rougeL": _rouge_l(reference_tokens, generated_tokens),
    }


def calculate_optional_bert_score(reference_text, generated_text):
    try:
        from bert_score import score
    except ImportError:
        return {
            "skipped": True,
            "reason": "bert-score 패키지가 설치되어 있지 않습니다.",
        }

    precision, recall, f1 = score(
        [generated_text],
        [reference_text],
        lang="ko",
        device=_bert_score_device(),
        verbose=False,
    )
    return {
        "precision": round(float(precision[0]), 4),
        "recall": round(float(recall[0]), 4),
        "f1": round(float(f1[0]), 4),
    }


def _bert_score_device():
    try:
        import torch
    except ImportError:
        return "cpu"

    return "cuda" if torch.cuda.is_available() else "cpu"


def calculate_scu_coverage(generated_text, scu_items):
    covered_items = []
    missed_items = []

    for item in scu_items:
        keywords = item.get("keywords") or []
        matched_keywords = [
            keyword
            for keyword in keywords
            if keyword and keyword in generated_text
        ]
        evaluation_item = {
            "id": item.get("id"),
            "text": item.get("text", ""),
            "matchedKeywords": matched_keywords,
        }
        if matched_keywords:
            covered_items.append(evaluation_item)
        else:
            missed_items.append(evaluation_item)

    total_count = len(scu_items)
    covered_count = len(covered_items)
    coverage = covered_count / total_count if total_count else 0
    return {
        "totalCount": total_count,
        "coveredCount": covered_count,
        "coverage": round(coverage, 4),
        "coveredItems": covered_items,
        "missedItems": missed_items,
    }


def _report_to_text(report):
    parts = []
    for value in report.values():
        if isinstance(value, list):
            parts.extend(str(item) for item in value)
        else:
            parts.append(str(value))
    return "\n".join(parts)


def _tokenize(text):
    return re.findall(r"[0-9A-Za-z가-힣]+", text.lower())


def _ngrams(tokens, size):
    if len(tokens) < size:
        return []
    return [tuple(tokens[index : index + size]) for index in range(len(tokens) - size + 1)]


def _f1_overlap(reference_units, generated_units):
    if not reference_units or not generated_units:
        return _score(0, 0, 0)

    reference_counts = _count_units(reference_units)
    generated_counts = _count_units(generated_units)
    overlap_count = 0

    for unit, reference_count in reference_counts.items():
        overlap_count += min(reference_count, generated_counts.get(unit, 0))

    precision = overlap_count / len(generated_units)
    recall = overlap_count / len(reference_units)
    f1 = _f1(precision, recall)
    return _score(precision, recall, f1)


def _rouge_l(reference_tokens, generated_tokens):
    if not reference_tokens or not generated_tokens:
        return _score(0, 0, 0)

    lcs_length = _longest_common_subsequence_length(reference_tokens, generated_tokens)
    precision = lcs_length / len(generated_tokens)
    recall = lcs_length / len(reference_tokens)
    f1 = _f1(precision, recall)
    return _score(precision, recall, f1)


def _longest_common_subsequence_length(left_tokens, right_tokens):
    previous_row = [0] * (len(right_tokens) + 1)
    for left_token in left_tokens:
        current_row = [0]
        for index, right_token in enumerate(right_tokens, start=1):
            if left_token == right_token:
                current_row.append(previous_row[index - 1] + 1)
            else:
                current_row.append(max(previous_row[index], current_row[-1]))
        previous_row = current_row
    return previous_row[-1]


def _count_units(units):
    counts = {}
    for unit in units:
        counts[unit] = counts.get(unit, 0) + 1
    return counts


def _f1(precision, recall):
    if precision + recall == 0:
        return 0
    return 2 * precision * recall / (precision + recall)


def _score(precision, recall, f1):
    return {
        "precision": round(precision, 4),
        "recall": round(recall, 4),
        "f1": round(f1, 4),
    }


def _human_evaluation_guide():
    return {
        "status": "manual_required",
        "criteria": [
            "핵심 쟁점이 빠지지 않았는가",
            "원문에 없는 주장을 만들지 않았는가",
            "찬성/반대/중립을 균형 있게 반영했는가",
            "서비스에 노출 가능한 자연스러운 한국어인가",
        ],
    }


def _read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def _save_json(data, path):
    path.parent.mkdir(exist_ok=True)
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
