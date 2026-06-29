FROM nvidia/cuda:11.8.0-devel-ubuntu22.04

ENV PYTHONUNBUFFERED=1
ENV PIP_NO_CACHE_DIR=1

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    ca-certificates \
    curl \
    git \
    python3 \
    python3-dev \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

RUN python3 -m pip install --upgrade pip setuptools wheel

COPY requirements.txt .
RUN python3 -m pip install -r requirements.txt

RUN python3 -m pip install "llama-cpp-python>=0.3.0" \
    --extra-index-url https://abetlen.github.io/llama-cpp-python/whl/cu118

COPY aireport ./aireport
COPY prompts ./prompts
COPY run_ai_worker.py ./run_ai_worker.py

RUN mkdir -p /app/outputs

CMD ["python3", "run_ai_worker.py"]
