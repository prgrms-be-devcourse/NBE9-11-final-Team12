import logging
import threading
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from aireport.config import AiReportConfig
from aireport.llama_cpp_client import LlamaCppClient
from aireport.prompt_security import PromptSecurityError
from aireport.report_generator import ReportGenerationError, ReportGenerator


logger = logging.getLogger(__name__)

PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_PROMPT_PATH = PROJECT_ROOT / "prompts" / "report_generation_prompt.md"
DEFAULT_FEW_SHOT_PATH = PROJECT_ROOT / "prompts" / "few_shot_examples.md"


class AiReportApiState:
    def __init__(self):
        self.generator = None
        # GGUF 모델 추론은 무겁고 동시 실행 안정성이 떨어질 수 있어 한 번에 하나씩 처리합니다.
        self.generation_lock = threading.Lock()

    def warm_up(self):
        if self.generator is not None:
            return

        config = AiReportConfig.from_env()
        client = LlamaCppClient(
            model_path=config.model_path,
            context_size=config.context_size,
            max_tokens=config.max_tokens,
            temperature=config.temperature,
            gpu_layers=config.gpu_layers,
        )
        logger.info("AI 리포트 서버 모델 warm-up을 시작합니다.")
        client.warm_up()
        self.generator = ReportGenerator(
            client,
            prompt_template=_read_prompt_template(),
            few_shot_examples=_read_text_if_exists(DEFAULT_FEW_SHOT_PATH),
        )
        logger.info("AI 리포트 서버 모델 warm-up을 완료했습니다.")

    def generate(self, request_payload):
        if self.generator is None:
            raise RuntimeError("AI report generator is not initialized.")

        with self.generation_lock:
            return self.generator.generate(request_payload)


class RoomPayload(BaseModel):
    title: str | None = None
    startedAt: str | None = None
    endedAt: str | None = None


class TopicPayload(BaseModel):
    title: str | None = None
    description: str | None = None


class SpeechPayload(BaseModel):
    speechId: int | None = None
    userId: int | None = None
    stance: str | None = None
    content: str
    startedAt: str | None = None
    endedAt: str | None = None
    createdAt: str | None = None


class CustomPromptPayload(BaseModel):
    label: str | None = None
    prompt: str


class AiReportGenerateRequest(BaseModel):
    room: RoomPayload | None = None
    topic: TopicPayload | None = None
    speeches: list[SpeechPayload] = Field(default_factory=list)
    customPrompts: list[CustomPromptPayload] = Field(default_factory=list)


class AiReportGenerateResponse(BaseModel):
    core_line: str = Field(alias="핵심 한줄")
    key_issues: list[str] = Field(alias="핵심 쟁점")
    ai_summary: str = Field(alias="AI 종합 정리")
    common_ground: str = Field(alias="공통 의견")
    ai_opinion: str = Field(alias="AI의 개인적 소견")


api_state = AiReportApiState()


@asynccontextmanager
async def lifespan(app):
    api_state.warm_up()
    yield


app = FastAPI(
    title="AI Report Server",
    description="백엔드 AiReportClient가 호출하는 AI 리포트 생성 API",
    version="0.1.0",
    lifespan=lifespan,
)


@app.get("/health")
def health():
    return {
        "status": "ok",
        "modelLoaded": api_state.generator is not None,
    }


@app.post("/report", response_model=AiReportGenerateResponse, response_model_by_alias=True)
def generate_report(request: AiReportGenerateRequest):
    if not request.speeches:
        raise HTTPException(status_code=400, detail="리포트를 생성할 발화 데이터가 없습니다.")

    try:
        report = api_state.generate(request.model_dump())
    except PromptSecurityError as exc:
        logger.warning("AI 리포트 Prompt Guard 검사가 요청을 차단했습니다: %s", exc)
        raise HTTPException(status_code=400, detail="Prompt Guard blocked unsafe AI report content.") from exc
    except ReportGenerationError as exc:
        logger.warning("AI 리포트 모델 응답 파싱에 실패했습니다: %s", exc)
        raise HTTPException(status_code=502, detail=f"AI 리포트 응답 형식이 올바르지 않습니다: {exc}") from exc
    except ValueError as exc:
        logger.warning("AI 리포트 스키마 검증에 실패했습니다: %s", exc)
        raise HTTPException(status_code=502, detail=f"AI 리포트 검증에 실패했습니다: {exc}") from exc
    except Exception as exc:
        logger.exception("AI 리포트 생성에 실패했습니다.")
        raise HTTPException(status_code=500, detail=f"AI 리포트 생성에 실패했습니다: {exc}") from exc

    return report


def _read_prompt_template():
    if DEFAULT_PROMPT_PATH.exists():
        return DEFAULT_PROMPT_PATH.read_text(encoding="utf-8")
    return None


def _read_text_if_exists(path):
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")
