import logging

import uvicorn


logging.basicConfig(level=logging.INFO, format="%(message)s")

HOST = "127.0.0.1"
PORT = 8000


def main():
    # IDE Run 버튼으로 백엔드가 호출할 AI 서버를 실행하는 진입점입니다.
    # 백엔드 app.ai-report.base-url은 기본적으로 http://127.0.0.1:8000 으로 맞추면 됩니다.
    uvicorn.run(
        "aireport.api:app",
        host=HOST,
        port=PORT,
        reload=False,
    )


if __name__ == "__main__":
    main()
