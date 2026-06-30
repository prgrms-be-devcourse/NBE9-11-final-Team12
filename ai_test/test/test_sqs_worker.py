import json
import logging
import unittest

from aireport.report_schema import AiReportModel
from aireport.sqs_worker import AiReportSqsWorker, AiReportWorkerMessage, to_complete_payload


def setUpModule():
    logging.disable(logging.CRITICAL)


def tearDownModule():
    logging.disable(logging.NOTSET)


class AiReportWorkerMessageTest(unittest.TestCase):
    def test_parses_backend_queue_message(self):
        message = AiReportWorkerMessage.from_json(json.dumps({
            "reportId": 55,
            "roomId": 10,
            "generationType": "BASE_ONLY",
            "idempotencyKey": "ai-report-55-v1",
        }))

        self.assertEqual(message.report_id, 55)
        self.assertEqual(message.room_id, 10)
        self.assertEqual(message.generation_type, "BASE_ONLY")
        self.assertEqual(message.idempotency_key, "ai-report-55-v1")


class AiReportSqsWorkerTest(unittest.TestCase):
    def test_poll_once_completes_message_and_deletes_it_when_generation_succeeds(self):
        sqs = _FakeSqsClient([
            _sqs_message({"reportId": 55, "roomId": 10, "generationType": "BASE_ONLY"})
        ])
        backend = _FakeBackendClient(processing_response={
            "request": {"speeches": [{"content": "hello"}]},
        })
        worker = AiReportSqsWorker(
            sqs_client=sqs,
            queue_url="queue-url",
            backend_client=backend,
            generate_report=lambda payload: {
                _base_alias("core_line"): "core",
                _base_alias("key_issues"): ["issue"],
                _base_alias("ai_summary"): "summary",
                _base_alias("common_ground"): "common",
                _base_alias("ai_opinion"): "opinion",
                "customReports": [],
            },
        )

        processed_count = worker.poll_once()

        self.assertEqual(processed_count, 1)
        self.assertEqual(backend.started, [(55, "BASE_ONLY")])
        self.assertEqual(backend.completed[0][0], 55)
        self.assertEqual(backend.completed[0][1]["coreLine"], "core")
        self.assertEqual(sqs.deleted_receipts, ["receipt-1"])
        self.assertEqual(sqs.receive_args["AttributeNames"], ["ApproximateReceiveCount"])

    def test_complete_payload_maps_validated_report_schema_aliases_to_backend_fields(self):
        payload = to_complete_payload("BASE_ONLY", {
            _base_alias("core_line"): "core",
            _base_alias("key_issues"): ["issue"],
            _base_alias("ai_summary"): "summary",
            _base_alias("common_ground"): "common",
            _base_alias("ai_opinion"): "opinion",
            "customReports": [],
        })

        self.assertEqual(payload["generationType"], "BASE_ONLY")
        self.assertEqual(payload["coreLine"], "core")
        self.assertEqual(payload["keyIssues"], ["issue"])
        self.assertEqual(payload["aiSummary"], "summary")
        self.assertEqual(payload["commonGround"], "common")
        self.assertEqual(payload["aiOpinion"], "opinion")

    def test_poll_once_keeps_message_when_generation_fails_first_time(self):
        sqs = _FakeSqsClient([
            _sqs_message(
                {"reportId": 55, "roomId": 10, "generationType": "BASE_ONLY"},
                receive_count=1,
            )
        ])
        backend = _FakeBackendClient(processing_response={
            "request": {"speeches": [{"content": "hello"}]},
        })

        def fail_generation(_payload):
            raise RuntimeError("local LLM timed out")

        worker = AiReportSqsWorker(
            sqs_client=sqs,
            queue_url="queue-url",
            backend_client=backend,
            generate_report=fail_generation,
        )

        processed_count = worker.poll_once()

        self.assertEqual(processed_count, 0)
        self.assertEqual(backend.failed, [])
        self.assertEqual(sqs.deleted_receipts, [])

    def test_poll_once_marks_failed_and_deletes_message_when_generation_fails_second_time(self):
        sqs = _FakeSqsClient([
            _sqs_message(
                {"reportId": 55, "roomId": 10, "generationType": "BASE_ONLY"},
                receive_count=2,
            )
        ])
        backend = _FakeBackendClient(processing_response={
            "request": {"speeches": [{"content": "hello"}]},
        })

        def fail_generation(_payload):
            raise RuntimeError("local LLM timed out")

        worker = AiReportSqsWorker(
            sqs_client=sqs,
            queue_url="queue-url",
            backend_client=backend,
            generate_report=fail_generation,
        )

        processed_count = worker.poll_once()

        self.assertEqual(processed_count, 1)
        self.assertEqual(backend.failed[0][0], 55)
        self.assertEqual(backend.failed[0][1]["errorCode"], "AI_WORKER_GENERATION_FAILED")
        self.assertIn("local LLM timed out", backend.failed[0][1]["errorMessage"])
        self.assertEqual(sqs.deleted_receipts, ["receipt-1"])

    def test_poll_once_keeps_message_when_complete_callback_fails(self):
        sqs = _FakeSqsClient([
            _sqs_message({"reportId": 55, "roomId": 10, "generationType": "BASE_ONLY"})
        ])
        backend = _FakeBackendClient(
            processing_response={"request": {"speeches": [{"content": "hello"}]}},
            complete_error=RuntimeError("complete callback failed"),
        )
        worker = AiReportSqsWorker(
            sqs_client=sqs,
            queue_url="queue-url",
            backend_client=backend,
            generate_report=lambda _payload: {
                _base_alias("core_line"): "core",
                _base_alias("key_issues"): ["issue"],
                _base_alias("ai_summary"): "summary",
                _base_alias("common_ground"): "common",
                _base_alias("ai_opinion"): "opinion",
            },
        )

        processed_count = worker.poll_once()

        self.assertEqual(processed_count, 0)
        self.assertEqual(len(backend.completed), 1)
        self.assertEqual(backend.failed, [])
        self.assertEqual(sqs.deleted_receipts, [])

    def test_poll_once_keeps_message_when_failure_callback_fails(self):
        sqs = _FakeSqsClient([
            _sqs_message(
                {"reportId": 55, "roomId": 10, "generationType": "BASE_ONLY"},
                receive_count=2,
            )
        ])
        backend = _FakeBackendClient(
            processing_response={"request": {"speeches": [{"content": "hello"}]}},
            fail_error=RuntimeError("fail callback failed"),
        )

        def fail_generation(_payload):
            raise RuntimeError("local LLM timed out")

        worker = AiReportSqsWorker(
            sqs_client=sqs,
            queue_url="queue-url",
            backend_client=backend,
            generate_report=fail_generation,
        )

        processed_count = worker.poll_once()

        self.assertEqual(processed_count, 0)
        self.assertEqual(len(backend.failed), 1)
        self.assertEqual(sqs.deleted_receipts, [])

    def test_poll_once_keeps_message_when_processing_callback_fails(self):
        sqs = _FakeSqsClient([
            _sqs_message({"reportId": 55, "roomId": 10, "generationType": "BASE_ONLY"})
        ])
        backend = _FakeBackendClient(processing_error=RuntimeError("backend unavailable"))
        worker = AiReportSqsWorker(
            sqs_client=sqs,
            queue_url="queue-url",
            backend_client=backend,
            generate_report=lambda _payload: {},
        )

        processed_count = worker.poll_once()

        self.assertEqual(processed_count, 0)
        self.assertEqual(sqs.deleted_receipts, [])
        self.assertEqual(backend.completed, [])
        self.assertEqual(backend.failed, [])

    def test_poll_once_deletes_invalid_message_without_calling_backend(self):
        sqs = _FakeSqsClient([
            {"Body": "{\"roomId\": 10}", "ReceiptHandle": "receipt-1"}
        ])
        backend = _FakeBackendClient()
        worker = AiReportSqsWorker(
            sqs_client=sqs,
            queue_url="queue-url",
            backend_client=backend,
            generate_report=lambda _payload: {},
        )

        processed_count = worker.poll_once()

        self.assertEqual(processed_count, 0)
        self.assertEqual(sqs.deleted_receipts, ["receipt-1"])
        self.assertEqual(backend.started, [])


class _FakeSqsClient:
    def __init__(self, messages):
        self.messages = messages
        self.deleted_receipts = []

    def receive_message(self, **kwargs):
        self.receive_args = kwargs
        return {"Messages": list(self.messages)}

    def delete_message(self, QueueUrl, ReceiptHandle):
        self.deleted_receipts.append(ReceiptHandle)


class _FakeBackendClient:
    def __init__(
        self,
        processing_response=None,
        processing_error=None,
        complete_error=None,
        fail_error=None,
    ):
        self.processing_response = processing_response or {"request": {"speeches": []}}
        self.processing_error = processing_error
        self.complete_error = complete_error
        self.fail_error = fail_error
        self.started = []
        self.completed = []
        self.failed = []

    def start_processing(self, report_id, generation_type):
        if self.processing_error is not None:
            raise self.processing_error
        self.started.append((report_id, generation_type))
        return self.processing_response

    def complete(self, report_id, payload):
        self.completed.append((report_id, payload))
        if self.complete_error is not None:
            raise self.complete_error

    def fail(self, report_id, payload):
        self.failed.append((report_id, payload))
        if self.fail_error is not None:
            raise self.fail_error


def _sqs_message(body, receive_count=1):
    return {
        "Body": json.dumps(body),
        "ReceiptHandle": "receipt-1",
        "Attributes": {
            "ApproximateReceiveCount": str(receive_count),
        },
    }


def _base_alias(field_name):
    return AiReportModel.model_fields[field_name].alias


if __name__ == "__main__":
    unittest.main()
