import os
from dataclasses import dataclass
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MODEL_PATH = PROJECT_ROOT / "models" / "Qwen2.5-7B-Instruct-Q4_K_M.gguf"


@dataclass(frozen=True)
class AiTestConfig:
    model_path: Path = DEFAULT_MODEL_PATH
    temperature: float = 0.2
    # 3시간 토론 샘플은 입력이 길기 때문에 기본 컨텍스트를 넉넉하게 잡습니다.
    context_size: int = 32768
    max_tokens: int = 2048
    # -1은 가능한 모든 레이어를 GPU로 올리겠다는 llama.cpp 관례입니다.
    # CUDA 빌드가 제대로 설치되어 있으면 GPU를 사용하고, 아니면 설치 단계에서 실패합니다.
    gpu_layers: int = -1

    @staticmethod
    def from_env():
        # IDE Run을 기본으로 하되, 나중에 모델 경로나 실험 파라미터만 바꾸고 싶을 때
        # 환경변수로 덮어쓸 수 있게 한 곳에서 설정을 모읍니다.
        return AiTestConfig(
            model_path=Path(os.getenv("AI_TEST_MODEL_PATH", str(DEFAULT_MODEL_PATH))),
            temperature=float(os.getenv("AI_TEST_TEMPERATURE", "0.5")),
            context_size=int(os.getenv("AI_TEST_CONTEXT_SIZE", "32768")),
            max_tokens=int(os.getenv("AI_TEST_MAX_TOKENS", "1024")),
            gpu_layers=int(os.getenv("AI_TEST_GPU_LAYERS", "-1")),
        )
