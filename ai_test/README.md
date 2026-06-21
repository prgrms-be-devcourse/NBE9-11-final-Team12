# AI Report Model Test Harness

`ai_test`는 백엔드와 분리된 AI 리포트 생성 모델 실험 공간입니다.
목표는 요금 청구 없이 로컬에서 실행 가능한 오픈 모델이 토론 데이터를 입력받아 쓸만한 AI 리포트를 생성하는지 확인하는 것입니다.

중재 메시지 생성, 제재 판단, 사용자 처벌 판단은 이 하네스 범위에서 제외합니다.

## 실행 방식

이 프로젝트는 Ollama를 사용하지 않습니다.

```text
Hugging Face GGUF 모델 파일
-> llama-cpp-python
-> run_ai_report.py
-> AI 리포트 JSON 출력
```

기본 모델 파일은 다음 위치를 사용합니다.

```text
models/Qwen2.5-7B-Instruct-Q4_K_M.gguf
```

이 파일은 Git에 올리지 않습니다.

## IDE에서 실행하는 방법

1. IDE에서 `D:\NBE9-11-final-Team12\ai_test` 폴더를 엽니다.
2. Python Interpreter를 사용자가 만든 Python 3.10 가상환경 `nbe911`로 선택합니다.
3. [install_dependencies.py](./install_dependencies.py)를 열고 Run 합니다.
   - `nbe911` 가상환경에 `huggingface_hub`를 설치합니다.
   - `llama-cpp-python`은 CUDA 11.8용 prebuilt wheel(`cu118`)로 설치합니다.
4. [download_model.py](./download_model.py)를 열고 Run 합니다.
   - Hugging Face에서 `Qwen2.5-7B-Instruct-Q4_K_M.gguf`를 `models/` 폴더로 내려받습니다.
   - 약 4.68GB 파일입니다.
5. [run_ai_report.py](./run_ai_report.py)를 열고 Run 합니다.

실행 인자, `PYTHONPATH`, 모듈 이름을 직접 설정하지 않아도 됩니다.

## 기본 실행 파일

[run_ai_report.py](./run_ai_report.py)가 IDE Run 전용 진입점입니다.

기본값:

- 샘플: `samples/anonymous_debate_3h_sample.json`
- 프롬프트: `prompts/report_generation_prompt.md`
- 모델 파일: `models/Qwen2.5-7B-Instruct-Q4_K_M.gguf`
- 실행 모드: 실제 모델 호출
- 결과 저장: `outputs/latest_ai_report.json`

## CUDA GPU 사용

CUDA GPU 사용은 기본으로 켜져 있습니다.

- [src/ai_test/config.py](ai_test/config.py)의 `gpu_layers` 기본값은 `-1`입니다.
- `-1`은 가능한 모든 레이어를 GPU로 올리겠다는 llama.cpp 설정입니다.
- [install_dependencies.py](./install_dependencies.py)는 로컬 소스 빌드 대신 CUDA 11.8용 prebuilt wheel 인덱스(`cu118`)를 사용합니다.

주의할 점:

- NVIDIA GPU만 있다고 바로 되는 것은 아닙니다.
- 첨부한 `STL1002: Unexpected compiler version, expected CUDA 12.4 or newer` 오류는 CUDA 11.8과 최신 Visual Studio 컴파일러 조합으로 소스 빌드가 막힌 경우입니다.
- 지금 구조에서는 소스 빌드를 피하고 `cu118` wheel을 설치하므로 CUDA 12.4로 바로 올릴 필요는 없습니다.
- 나중에 CUDA Toolkit을 12.4 이상으로 바꾸면 [install_dependencies.py](./install_dependencies.py)의 `CUDA_WHEEL` 값을 `cu124`처럼 맞춰 변경할 수 있습니다.
- GPU 메모리가 부족하면 `AI_TEST_GPU_LAYERS` 값을 낮춰 일부 레이어만 GPU에 올릴 수 있습니다.

프롬프트만 확인하고 싶으면 `run_ai_report.py`의 아래 값을 바꾼 뒤 Run 합니다.

```python
RUN_MODE = "preview"
```

다시 실제 모델을 호출하려면 아래처럼 되돌립니다.

```python
RUN_MODE = "model"
```

## 모델 선택

기본은 `Qwen2.5-7B-Instruct-Q4_K_M.gguf`입니다.

이유:

- Qwen2.5-7B-Instruct는 한국어를 포함한 다국어를 지원합니다.
- 리포트 생성에 필요한 긴 텍스트 생성과 JSON 구조화 출력 성능을 기대할 수 있습니다.
- `Q4_K_M`은 4비트 양자화 중 품질과 용량 균형이 무난한 선택입니다.
- 로컬 실행이므로 토큰 요금이 없습니다.

출처:

- 원본 모델: `Qwen/Qwen2.5-7B-Instruct`
- GGUF 양자화: `bartowski/Qwen2.5-7B-Instruct-GGUF`

## 출력 스키마

모델 응답은 JSON 객체 하나여야 하며, 필수 필드는 다음과 같습니다.

- `topic`
- `oneLineSummary`
- `overallSummary`
- `pros`
- `cons`
- `keyIssues`
- `evidence`
- `balancedConclusion`
- `additionalPerspective`

## 문제가 생겼을 때

`run_ai_report.py` 실행 시 실패하면 IDE 콘솔의 안내를 확인합니다.

주로 확인할 부분:

- IDE Python Interpreter가 `nbe911`인지
- [install_dependencies.py](./install_dependencies.py)를 Run 해서 `llama-cpp-python`, `huggingface_hub`가 `nbe911`에 설치되어 있는지
- 설치 콘솔에 `Installing llama-cpp-python CUDA wheel: cu118`이 출력되는지
- [download_model.py](./download_model.py)를 Run 해서 GGUF 모델 파일을 받아두었는지
- `models/Qwen2.5-7B-Instruct-Q4_K_M.gguf` 파일이 실제로 있는지

## 파일별 역할

- [run_ai_report.py](./run_ai_report.py): IDE Run 전용 진입점입니다. 샘플, 프롬프트, GGUF 모델 경로를 자동으로 잡고 AI 리포트를 생성합니다.
- [install_dependencies.py](./install_dependencies.py): `nbe911` 가상환경에 필요한 패키지를 설치합니다. CUDA 11.8용 `llama-cpp-python` prebuilt wheel 설치도 여기서 처리합니다.
- [download_model.py](./download_model.py): Hugging Face에서 GGUF 모델 파일을 `models/` 폴더로 내려받습니다.
- [src/ai_test/config.py](ai_test/config.py): 모델 경로, 온도, 컨텍스트 크기, 최대 생성 토큰 수, GPU 레이어 수를 관리합니다.
- [src/ai_test/llama_cpp_client.py](ai_test/llama_cpp_client.py): GGUF 모델을 로딩하고 `llama-cpp-python`으로 로컬 추론을 실행합니다.
- [src/ai_test/report_generator.py](ai_test/report_generator.py): 토론 샘플을 프롬프트로 만들고, 모델 응답 JSON을 파싱합니다.
- [src/ai_test/report_schema.py](ai_test/report_schema.py): AI 리포트가 필수 필드와 배열 필드를 지키는지 검증합니다.
- [prompts/report_generation_prompt.md](./prompts/report_generation_prompt.md): 모델에게 전달할 AI 리포트 생성 프롬프트입니다.
- [samples/anonymous_debate_3h_sample.json](./samples/anonymous_debate_3h_sample.json): 기본 입력으로 사용하는 3시간 익명 토론 샘플 데이터입니다.
- [outputs/.gitkeep](./outputs/.gitkeep): 리포트 결과 저장 폴더를 Git에 남기기 위한 파일입니다. 실제 결과 JSON은 Git에 올리지 않습니다.
- [models/.gitkeep](./models/.gitkeep): 빈 `models/` 폴더를 Git에 남기기 위한 파일입니다. 실제 `.gguf` 모델 파일은 Git에 올리지 않습니다.
- [requirements.txt](./requirements.txt): 일반 Python 의존성을 적습니다. CUDA wheel 인덱스가 필요한 `llama-cpp-python`은 설치 스크립트에서 별도로 설치합니다.
- [tests](./tests): 설정, 실행 진입점, 모델 클라이언트, 리포트 파싱을 검증하는 단위 테스트입니다.

제거한 파일:

- `src/ai_test/cli.py`: IDE Run 전용 구조에서는 터미널 CLI 진입점이 필요 없어 제거했습니다.
- `src/ai_test/model_client.py`: 현재는 `LlamaCppClient` 하나만 사용하므로 추상 인터페이스를 제거했습니다.

## 테스트

IDE 테스트 실행 기능으로 `tests` 폴더를 실행하면 됩니다.
터미널을 사용할 경우의 전체 테스트 명령은 다음과 같습니다.

```powershell
python -m unittest discover -s tests -v
```
