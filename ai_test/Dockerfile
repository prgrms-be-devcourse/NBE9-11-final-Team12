FROM nvidia/cuda:12.4.1-devel-ubuntu22.04

ENV PYTHONUNBUFFERED=1
ENV PIP_NO_CACHE_DIR=1
ENV CMAKE_ARGS="-DGGML_CUDA=on"
ENV FORCE_CMAKE=1

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    ca-certificates \
    cmake \
    curl \
    git \
    ninja-build \
    python3 \
    python3-dev \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

RUN python3 -m pip install --upgrade pip setuptools wheel

COPY requirements.txt .
RUN python3 -m pip install -r requirements.txt
RUN python3 -m pip install --no-binary llama-cpp-python llama-cpp-python

COPY aireport ./aireport
COPY prompts ./prompts
COPY run_ai_worker.py ./run_ai_worker.py

RUN mkdir -p /app/outputs

CMD ["python3", "run_ai_worker.py"]
