import logging

from aireport.api import api_state
from aireport.sqs_worker import create_worker_from_env


logging.basicConfig(level=logging.INFO, format="%(message)s")


def main():
    api_state.warm_up()
    worker = create_worker_from_env(api_state.generate)
    worker.run_forever()


if __name__ == "__main__":
    main()
