FROM python:3.12-slim

ARG PROMPT_GUARD_REPO=https://github.com/seojoonkim/prompt-guard.git
ARG PROMPT_GUARD_REF=main

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV CONFIG_PATH=/app/config.yaml
ENV PG_API_ENABLED=false

WORKDIR /opt

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates git \
    && git clone --depth 1 --branch "${PROMPT_GUARD_REF}" "${PROMPT_GUARD_REPO}" /opt/prompt-guard

COPY patch_patterns.py /tmp/patch_patterns.py

RUN python /tmp/patch_patterns.py \
    && pip install --no-cache-dir -e "/opt/prompt-guard[dev]" \
    && apt-get purge -y --auto-remove git \
    && rm -rf /var/lib/apt/lists/*

COPY config.yaml /app/config.yaml

WORKDIR /opt/prompt-guard

EXPOSE 8080

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8080"]
