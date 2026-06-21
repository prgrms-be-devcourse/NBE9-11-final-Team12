import json
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from aireport import AiReportConfig, DEFAULT_MODEL_PATH as CONFIG_DEFAULT_MODEL_PATH
from aireport import LlamaCppClient
from aireport import ReportGenerator
from aireport.report_schema import validate_report_file

DEFAULT_MODEL_PATH = CONFIG_DEFAULT_MODEL_PATH
DEFAULT_SAMPLE_PATH = PROJECT_ROOT / "samples" / "anonymous_debate_3h_sample.json"
DEFAULT_PROMPT_PATH = PROJECT_ROOT / "prompts" / "report_generation_prompt.md"
DEFAULT_FEW_SHOT_PATH = PROJECT_ROOT / "prompts" / "few_shot_examples.md"
DEFAULT_OUTPUT_PATH = PROJECT_ROOT / "outputs" / "latest_ai_report.json"
DEFAULT_MODEL_INPUT_OUTPUT_PATH = PROJECT_ROOT / "outputs" / "latest_model_input.json"

# IDE에서 프롬프트만 확인하고 싶으면 "model"을 "preview"로 바꾸면 됩니다.
RUN_MODE = "model"


def main():
    # IDE Run 버튼을 눌렀을 때 실행되는 진입점입니다.
    # RUN_MODE만 바꾸면 모델 호출 없이 프롬프트 확인도 가능합니다.

    _print_cuda_device_status()

    print("AI 리포트 생성 실행을 시작합니다.", flush=True)
    if RUN_MODE == "preview":
        print("preview 모드: 모델 호출 없이 프롬프트만 출력합니다.", flush=True)
        preview_prompt()
        return

    try:
        report, model_input = run_report()
    except Exception as exc:
        print("AI 리포트 생성 실행에 실패했습니다.")
        print(f"원인: {exc}")
        print("")
        print("확인할 것:")
        print("- IDE Python Interpreter가 nbe911 가상환경을 보고 있는지 확인")
        print("- install_dependencies.py를 Run 해서 CUDA용 llama-cpp-python을 설치했는지 확인")
        print("- download_model.py를 Run 해서 GGUF 모델 파일을 받아두었는지 확인")
        return

    print(json.dumps(report, ensure_ascii=False, indent=2))
    save_json(model_input, DEFAULT_MODEL_INPUT_OUTPUT_PATH, "모델 입력 데이터")
    save_report(report, DEFAULT_OUTPUT_PATH)
    validate_report_file(DEFAULT_OUTPUT_PATH)
    print("저장된 AI 리포트 JSON 스키마 검증 완료", flush=True)
    print(f"모델 입력 데이터 저장 위치: {DEFAULT_MODEL_INPUT_OUTPUT_PATH}")
    print(f"AI 리포트 결과 저장 위치: {DEFAULT_OUTPUT_PATH}")


def run_report():
    # 설정, 샘플, 프롬프트, 모델 클라이언트를 조립하는 실제 리포트 생성 흐름입니다.
    # 백엔드와 연결하지 않고 로컬 파일만 사용하므로 실험 결과를 안전하게 반복할 수 있습니다.
    print("1/5 설정을 읽는 중...", flush=True)
    config = AiReportConfig.from_env()
    print(f"2/5 샘플 데이터를 읽는 중: {DEFAULT_SAMPLE_PATH}", flush=True)
    sample = _read_json(DEFAULT_SAMPLE_PATH)
    print(f"3/5 프롬프트 템플릿과 few-shot 예시를 읽는 중: {DEFAULT_PROMPT_PATH}", flush=True)
    prompt_template = DEFAULT_PROMPT_PATH.read_text(encoding="utf-8")
    few_shot_examples = _read_text_if_exists(DEFAULT_FEW_SHOT_PATH)
    print(f"4/5 모델 클라이언트를 준비하는 중: {config.model_path}", flush=True)
    client = LlamaCppClient(
        model_path=config.model_path,
        context_size=config.context_size,
        max_tokens=config.max_tokens,
        temperature=config.temperature,
        gpu_layers=config.gpu_layers,
    )
    generator = ReportGenerator(
        client,
        prompt_template=prompt_template,
        few_shot_examples=few_shot_examples,
    )
    print("5/5 모델로 AI 리포트를 생성하는 중입니다. 모델 로딩과 추론 때문에 시간이 걸릴 수 있습니다.", flush=True)
    report = generator.generate(sample)
    return report, generator.last_model_input


def preview_prompt():
    # 모델을 로딩하지 않고 최종 프롬프트만 출력합니다.
    # 모델 응답 품질이 이상할 때 입력 프롬프트부터 확인하기 위한 모드입니다.
    sample = _read_json(DEFAULT_SAMPLE_PATH)
    prompt_template = DEFAULT_PROMPT_PATH.read_text(encoding="utf-8")
    few_shot_examples = _read_text_if_exists(DEFAULT_FEW_SHOT_PATH)
    generator = ReportGenerator(
        _PreviewClient(),
        prompt_template=prompt_template,
        few_shot_examples=few_shot_examples,
    )
    print(generator.build_prompt(sample))


def _read_json(path):
    # 샘플 토론 데이터는 한글을 포함하므로 UTF-8로 고정해서 읽습니다.
    return json.loads(path.read_text(encoding="utf-8"))


def _read_text_if_exists(path):
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def save_report(report, output_path=DEFAULT_OUTPUT_PATH):
    # IDE 콘솔 출력은 길면 확인하기 어렵기 때문에 결과 JSON도 파일로 남깁니다.
    save_json(report, output_path, "결과 JSON")


def save_json(data, output_path, label):
    print(f"{label} 파일을 저장하는 중...", flush=True)
    output_path.parent.mkdir(exist_ok=True)
    output_path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


class _PreviewClient:
    def generate(self, prompt):
        return "{}"


def _print_cuda_device_status():
    try:
        import torch
    except ImportError:
        print("torch 미설치: CUDA 장치 출력은 건너뜁니다.", flush=True)
        return

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"torch 기준 실행 장치: {device}", flush=True)


if __name__ == "__main__":
    main()
