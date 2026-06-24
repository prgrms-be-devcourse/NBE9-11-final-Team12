import subprocess
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent
REQUIREMENTS_PATH = PROJECT_ROOT / "requirements.txt"

USE_CUDA = True
CUDA_WHEEL = "cu118"
LLAMA_CPP_EXTRA_INDEX_URL = f"https://abetlen.github.io/llama-cpp-python/whl/{CUDA_WHEEL}"


def main():
    print(f"Python executable: {sys.executable}")
    print(f"Installing common dependencies from: {REQUIREMENTS_PATH}")
    subprocess.check_call(
        [
            sys.executable,
            "-m",
            "pip",
            "install",
            "-r",
            str(REQUIREMENTS_PATH),
        ]
    )
    install_llama_cpp_python()
    print("Dependency installation finished.")


def install_llama_cpp_python():
    # 로컬 CUDA 소스 빌드는 CUDA Toolkit과 Visual Studio 버전 충돌이 자주 납니다.
    # 사용자의 CUDA 11.8 환경에 맞춰 공식 prebuilt wheel 인덱스(cu118)를 사용합니다.
    print(f"Installing llama-cpp-python CUDA wheel: {CUDA_WHEEL}")
    subprocess.check_call(
        [
            sys.executable,
            "-m",
            "pip",
            "install",
            "--upgrade",
            "llama-cpp-python>=0.3.0",
            "--extra-index-url",
            LLAMA_CPP_EXTRA_INDEX_URL,
        ]
    )


if __name__ == "__main__":
    main()
