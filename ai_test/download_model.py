from pathlib import Path

from aireport.opinion_clusterer import DEFAULT_EMBEDDING_MODEL_NAME


PROJECT_ROOT = Path(__file__).resolve().parent
MODEL_REPO_ID = "bartowski/Qwen2.5-7B-Instruct-GGUF"
MODEL_FILENAME = "Qwen2.5-7B-Instruct-Q4_K_M.gguf"
MODEL_DIR = PROJECT_ROOT / "models"


def main():
    # IDE Run 버튼으로 모델 파일을 한 번만 내려받기 위한 스크립트입니다.
    # 이미 파일이 있으면 다시 받지 않아 실험 시간을 줄입니다.
    MODEL_DIR.mkdir(exist_ok=True)
    download_gguf_model()
    download_embedding_model()


def download_gguf_model():
    target = MODEL_DIR / MODEL_FILENAME

    if target.exists():
        print(f"이미 모델 파일이 있습니다: {target}")
        return

    try:
        from huggingface_hub import hf_hub_download
    except ImportError:
        print("huggingface_hub가 설치되어 있지 않습니다.")
        print("IDE Python Interpreter가 nbe911인지 확인한 뒤 dependencies를 설치하세요.")
        return

    print("Hugging Face에서 GGUF 모델 파일을 내려받습니다.")
    print(f"저장 위치: {target}")
    downloaded_path = hf_hub_download(
        repo_id=MODEL_REPO_ID,
        filename=MODEL_FILENAME,
        local_dir=MODEL_DIR,
    )
    print(f"다운로드 완료: {downloaded_path}")


def download_embedding_model():
    # 클러스터링용 Sentence-BERT 모델을 Hugging Face 캐시에 받아둡니다.
    # 한 번 받아두면 run_ai_report.py 실행 시 네트워크 없이 의미 기반 클러스터링을 사용할 수 있습니다.
    try:
        from sentence_transformers import SentenceTransformer
    except ImportError:
        print("sentence-transformers가 설치되어 있지 않아 임베딩 모델 다운로드를 건너뜁니다.")
        return

    print(f"Sentence-BERT 임베딩 모델을 준비합니다: {DEFAULT_EMBEDDING_MODEL_NAME}")
    SentenceTransformer(DEFAULT_EMBEDDING_MODEL_NAME)
    print("Sentence-BERT 임베딩 모델 준비 완료")


if __name__ == "__main__":
    main()
