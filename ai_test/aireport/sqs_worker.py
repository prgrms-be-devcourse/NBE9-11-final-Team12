import json
import logging
import os
import time
import urllib.error
import urllib.request
from dataclasses import dataclass


logger = logging.getLogger(__name__)

MAX_GENERATION_ATTEMPTS = 2

BASE_REPORT_ALIASES = {
    "핵심 주제": "coreLine",
    "핵심 쟁점": "keyIssues",
    "AI 종합 정리": "aiSummary",
    "공통 의견": "commonGround",
    "AI의 개인적 의견": "aiOpinion"
}


@dataclass(frozen=True)
class AiReportWorkerMessage:
    report_id: int
    room_id: int
    generation_type: str
    idempotency_key: str | None = None

    @staticmethod
    def from_json(message_body):
        payload = json.loads(message_body)
        report_id = payload.get("reportId")
        room_id = payload.get("roomId")
        generation_type = payload.get("generationType")
        if report_id is None or room_id is None or not generation_type:
            raise ValueError("AI report SQS message must contain reportId, roomId, and generationType")
        return AiReportWorkerMessage(
            report_id=int(report_id),
            room_id=int(room_id),
            generation_type=str(generation_type),
            idempotency_key=payload.get("idempotencyKey"),
        )


class AiReportSqsWorker:
    def __init__(
        self,
        sqs_client,
        queue_url,
        backend_client,
        generate_report,
        max_number_of_messages=1,
        wait_time_seconds=10,
        visibility_timeout_seconds=None,
    ):
        self.sqs_client = sqs_client
        self.queue_url = queue_url
        self.backend_client = backend_client
        self.generate_report = generate_report
        self.max_number_of_messages = max_number_of_messages
        self.wait_time_seconds = wait_time_seconds
        self.visibility_timeout_seconds = visibility_timeout_seconds

    def poll_once(self):
        receive_args = {
            "QueueUrl": self.queue_url,
            "MaxNumberOfMessages": self.max_number_of_messages,
            "WaitTimeSeconds": self.wait_time_seconds,
            "AttributeNames": ["ApproximateReceiveCount"],
        }
        if self.visibility_timeout_seconds is not None:
            receive_args["VisibilityTimeout"] = self.visibility_timeout_seconds

        response = self.sqs_client.receive_message(**receive_args)
        messages = response.get("Messages", [])
        processed_count = 0

        for raw_message in messages:
            if self._process_raw_message(raw_message):
                processed_count += 1

        return processed_count

    def run_forever(self, sleep_seconds=1):
        while True:
            self.poll_once()
            if sleep_seconds > 0:
                time.sleep(sleep_seconds)

    def _process_raw_message(self, raw_message):
        receipt_handle = raw_message["ReceiptHandle"]
        try:
            # 1단계: SQS 메시지 파싱
            message = AiReportWorkerMessage.from_json(raw_message.get("Body", ""))
            receive_count = _receive_count(raw_message)
        except (json.JSONDecodeError, TypeError, ValueError):
            logger.exception("Invalid AI report SQS message. Deleting message to avoid poison retry.")
            self._delete_message(receipt_handle)
            return False

        try:
            # 2단계: 백엔드에 일 시작 보고 및 데이터 수령 (/processing)
            processing_response = self.backend_client.start_processing(
                message.report_id,
                message.generation_type,
            )
            generation_request = processing_response["request"]
        except Exception:
            logger.exception(
                "Failed to start AI report processing. reportId=%s roomId=%s",
                message.report_id,
                message.room_id,
            )
            return False

        try:
            # 3단계: 실제 LLM 연산 수행 및 리포트 작성
            report = self.generate_report(generation_request)
        except Exception as exc:
            logger.exception(
                "Failed to generate AI report. reportId=%s roomId=%s",
                message.report_id,
                message.room_id,
            )
            if receive_count < MAX_GENERATION_ATTEMPTS:
                logger.warning(
                    "Keeping AI report message for retry. reportId=%s roomId=%s receiveCount=%s maxAttempts=%s",
                    message.report_id,
                    message.room_id,
                    receive_count,
                    MAX_GENERATION_ATTEMPTS,
                )
                return False
            try:
                self.backend_client.fail(
                    message.report_id,
                    {
                        "errorCode": "AI_WORKER_GENERATION_FAILED",
                        "errorMessage": str(exc),
                    },
                )
            except Exception:
                logger.exception(
                    "Failed to send AI report failure callback. reportId=%s roomId=%s",
                    message.report_id,
                    message.room_id,
                )
                return False
            self._delete_message(receipt_handle)
            return True

        try:
            # 4단계: 백엔드에 결과 전송 (/complete) 및 SQS 메시지 삭제
            self.backend_client.complete(message.report_id, to_complete_payload(message.generation_type, report))
            self._delete_message(receipt_handle)
            return True
        except Exception:
            logger.exception(
                "Failed to send AI report complete callback. reportId=%s roomId=%s",
                message.report_id,
                message.room_id,
            )
            return False

    def _delete_message(self, receipt_handle):
        self.sqs_client.delete_message(
            QueueUrl=self.queue_url,
            ReceiptHandle=receipt_handle,
        )


class BackendCallbackClient:
    def __init__(self, base_url, worker_token, timeout_seconds=10):
        self.base_url = base_url.rstrip("/")
        self.worker_token = worker_token
        self.timeout_seconds = timeout_seconds

    def start_processing(self, report_id, generation_type):
        response = self._post_json(
            f"/api/v1/internal/ai-reports/{report_id}/processing",
            {"generationType": generation_type},
        )
        return response["data"]

    def complete(self, report_id, payload):
        self._post_json(
            f"/api/v1/internal/ai-reports/{report_id}/complete",
            payload,
        )

    def fail(self, report_id, payload):
        self._post_json(
            f"/api/v1/internal/ai-reports/{report_id}/fail",
            payload,
        )

    def _post_json(self, path, payload):
        request = urllib.request.Request(
            self.base_url + path,
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "X-AI-Worker-Token": self.worker_token,
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_seconds) as response:
                return json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"Backend callback failed. status={exc.code}, body={body}") from exc


def to_complete_payload(generation_type, report):
    payload = {
        "generationType": generation_type,
        "customReports": report.get("customReports", []),
    }
    for source_key, target_key in BASE_REPORT_ALIASES.items():
        if source_key in report:
            payload[target_key] = report[source_key]
    return payload


def create_boto3_sqs_client(region_name):
    import boto3

    return boto3.client("sqs", region_name=region_name)


def create_worker_from_env(generate_report):
    queue_url = _required_env("AI_REPORT_QUEUE_URL")
    backend_base_url = _required_env("AI_REPORT_BACKEND_BASE_URL")
    worker_token = _required_env("AI_REPORT_WORKER_TOKEN")
    region = os.getenv("AI_REPORT_QUEUE_REGION", "ap-northeast-2")
    return AiReportSqsWorker(
        sqs_client=create_boto3_sqs_client(region),
        queue_url=queue_url,
        backend_client=BackendCallbackClient(
            base_url=backend_base_url,
            worker_token=worker_token,
            timeout_seconds=int(os.getenv("AI_REPORT_BACKEND_TIMEOUT_SECONDS", "10")),
        ),
        generate_report=generate_report,
        max_number_of_messages=int(os.getenv("AI_REPORT_WORKER_BATCH_SIZE", "1")),
        wait_time_seconds=int(os.getenv("AI_REPORT_WORKER_WAIT_TIME_SECONDS", "10")),
        visibility_timeout_seconds=int(os.getenv("AI_REPORT_WORKER_VISIBILITY_TIMEOUT_SECONDS", "300")),
    )


def _required_env(name):
    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"{name} is required.")
    return value


def _receive_count(raw_message):
    raw_receive_count = raw_message.get("Attributes", {}).get("ApproximateReceiveCount", "1")
    try:
        return int(raw_receive_count)
    except (TypeError, ValueError):
        return 1
