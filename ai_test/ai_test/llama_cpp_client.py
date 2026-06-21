import time
from pathlib import Path


class LlamaCppClient:
    def __init__(
        self,
        model_path,
        context_size=8192,
        max_tokens=1024,
        temperature=0.5,
        gpu_layers=-1,
        n_batch=512,
        n_threads=8,
        llama_factory=None,
    ):
        self.model_path = Path(model_path)
        self.context_size = context_size
        self.max_tokens = max_tokens
        self.temperature = temperature
        self.gpu_layers = gpu_layers
        self.n_batch = n_batch
        self.n_threads = n_threads
        self._llama_factory = llama_factory
        self._llm = None

    def generate(self, prompt):
        # ReportGenerator는 문자열 프롬프트만 넘기고, 이 클래스가 llama.cpp 호출 세부사항을 책임집니다.
        llm = self._load_model()
        prompt_char_count = len(prompt)
        print("성능 측정 시작", flush=True)
        print(f"- 프롬프트 글자 수: {prompt_char_count}", flush=True)
        print(f"- 설정: n_ctx={self.context_size}, max_tokens={self.max_tokens}, temperature={self.temperature}", flush=True)
        print(f"- 실행: n_gpu_layers={self.gpu_layers}, n_batch={self.n_batch}, n_threads={self.n_threads}", flush=True)
        print("모델 답변 생성을 시작했습니다. 긴 토론 샘플은 몇 분 걸릴 수 있습니다.", flush=True)
        started_at = time.perf_counter()
        response = llm.create_chat_completion(
            messages=[
                {
                    "role": "user",
                    "content": prompt,
                }
            ],
            temperature=self.temperature,
            max_tokens=self.max_tokens,
        )
        elapsed_seconds = time.perf_counter() - started_at
        content = response["choices"][0]["message"]["content"]
        print("모델 답변 생성을 완료했습니다.", flush=True)
        _print_generation_metrics(
            response=response,
            llm=llm,
            prompt=prompt,
            completion=content,
            elapsed_seconds=elapsed_seconds,
            context_size=self.context_size,
            max_tokens=self.max_tokens,
        )
        return content

    def _load_model(self):
        # GGUF 모델은 크기 때문에 매 요청마다 다시 로딩하지 않고 한 번 로딩한 인스턴스를 재사용합니다.
        if self._llm is not None:
            return self._llm

        if not self.model_path.exists() and self._llama_factory is None:
            raise RuntimeError(
                "GGUF model file was not found. Run download_model.py in the IDE "
                f"or place the model at: {self.model_path}"
            )

        print(f"CUDA GPU 사용 설정: n_gpu_layers={self.gpu_layers}", flush=True)
        print("GGUF 모델 파일을 메모리에 로딩하는 중입니다.", flush=True)
        factory = self._llama_factory or _import_llama
        started_at = time.perf_counter()
        self._llm = factory(
            model_path=str(self.model_path),
            n_ctx=self.context_size,
            n_gpu_layers=self.gpu_layers,
            n_batch=self.n_batch,
            n_threads=self.n_threads,
            verbose=False,
        )
        elapsed_seconds = time.perf_counter() - started_at
        print(f"GGUF 모델 로딩을 완료했습니다. 로딩 시간: {elapsed_seconds:.2f}초", flush=True)
        return self._llm


def _print_generation_metrics(
    response,
    llm,
    prompt,
    completion,
    elapsed_seconds,
    context_size,
    max_tokens,
):
    usage = response.get("usage") or {}
    prompt_tokens = usage.get("prompt_tokens")
    completion_tokens = usage.get("completion_tokens")
    total_tokens = usage.get("total_tokens")
    token_source = "llama.cpp usage"

    if prompt_tokens is None:
        prompt_tokens = _safe_count_tokens(llm, prompt)
        token_source = "tokenize 추정"
    if completion_tokens is None:
        completion_tokens = _safe_count_tokens(llm, completion)
        token_source = "tokenize 추정"
    if total_tokens is None and prompt_tokens is not None and completion_tokens is not None:
        total_tokens = prompt_tokens + completion_tokens

    print("성능 측정 결과", flush=True)
    print(f"- 입력 토큰 수: {_format_metric(prompt_tokens)}", flush=True)
    print(f"- 출력 토큰 수: {_format_metric(completion_tokens)}", flush=True)
    print(f"- 전체 토큰 수: {_format_metric(total_tokens)}", flush=True)
    print(f"- 토큰 산정 방식: {token_source}", flush=True)
    print(f"- 생성 소요 시간: {elapsed_seconds:.2f}초", flush=True)
    print(f"- 출력 글자 수: {len(completion)}", flush=True)
    print(f"- 요청 최대 출력 토큰: {max_tokens}", flush=True)

    if completion_tokens and elapsed_seconds > 0:
        print(f"- 출력 속도: {completion_tokens / elapsed_seconds:.2f} tokens/sec", flush=True)
    if total_tokens and elapsed_seconds > 0:
        print(f"- 전체 처리 속도: {total_tokens / elapsed_seconds:.2f} tokens/sec", flush=True)
    if prompt_tokens:
        context_usage = prompt_tokens / context_size * 100
        remaining_context = max(context_size - prompt_tokens, 0)
        print(f"- 컨텍스트 사용률: {context_usage:.2f}% ({prompt_tokens}/{context_size})", flush=True)
        print(f"- 남은 입력 컨텍스트 여유: {remaining_context} tokens", flush=True)


def _safe_count_tokens(llm, text):
    tokenizer = getattr(llm, "tokenize", None)
    if not callable(tokenizer):
        return None

    try:
        tokens = tokenizer(text.encode("utf-8"), add_bos=False)
    except TypeError:
        try:
            tokens = tokenizer(text.encode("utf-8"))
        except Exception:
            return None
    except Exception:
        return None

    try:
        return len(tokens)
    except TypeError:
        return None


def _format_metric(value):
    if value is None:
        return "알 수 없음"
    return str(value)


def _import_llama(**kwargs):
    # import를 지연시켜 preview 모드나 단위 테스트에서는 무거운 llama_cpp 의존성이 없어도 동작하게 합니다.
    try:
        from llama_cpp import Llama
    except ImportError as exc:
        raise RuntimeError(
            "llama-cpp-python is not installed in the selected Python environment. "
            "Install the project dependencies in the nbe911 environment."
        ) from exc

    return Llama(**kwargs)
