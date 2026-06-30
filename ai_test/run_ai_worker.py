import logging
import os
from pathlib import Path

from aireport.api import api_state
from aireport.sqs_worker import create_worker_from_env


_log_level = getattr(logging, os.getenv("LOG_LEVEL", "INFO").upper(), logging.INFO)
logging.basicConfig(level=_log_level, format="%(message)s")


def load_root_env():
    env_path = Path(__file__).resolve().parents[1] / ".env"
    if not env_path.exists():
        return

    for raw_line in env_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")

        if key and key not in os.environ:
            os.environ[key] = value


def main():
    load_root_env()
    api_state.warm_up()
    worker = create_worker_from_env(api_state.generate)
    worker.run_forever()


if __name__ == "__main__":
    main()
