import json
import logging
import os
import time
from dataclasses import dataclass, field
from typing import Any
from urllib import request as urllib_request


BLOCKING_SEVERITIES = {"MEDIUM", "HIGH", "CRITICAL"}
SEVERITY_ORDER = {"SAFE": 0, "LOW": 1, "MEDIUM": 2, "HIGH": 3, "CRITICAL": 4}
DEFAULT_HTTP_TIMEOUT_SECONDS = 30

logger = logging.getLogger(__name__)


class PromptSecurityError(ValueError):
    pass


@dataclass(frozen=True)
class PromptGuardResult:
    severity: str = "SAFE"
    action: str = "allow"
    blocked: bool = False
    reasons: list[str] = field(default_factory=list)

    @staticmethod
    def from_raw(raw_result):
        severity = _enum_or_text(getattr(raw_result, "severity", None) or _mapping_value(raw_result, "severity") or "SAFE")
        action = _enum_or_text(getattr(raw_result, "action", None) or _mapping_value(raw_result, "action") or "allow")
        blocked = bool(getattr(raw_result, "blocked", False) or _mapping_value(raw_result, "blocked") or action == "block")
        reasons = getattr(raw_result, "reasons", None) or _mapping_value(raw_result, "reasons") or []
        return PromptGuardResult(
            severity=severity.upper(),
            action=action.lower(),
            blocked=blocked,
            reasons=[str(reason) for reason in reasons],
        )


class NoOpPromptGuard:
    def analyze(self, content):
        return PromptGuardResult(severity="SAFE")

    def sanitize_output(self, content):
        return {
            "blocked": False,
            "sanitized_text": content,
        }


class LocalPromptGuard:
    def __init__(self, config=None):
        try:
            from prompt_guard import PromptGuard
        except ImportError as exc:
            raise RuntimeError("prompt_guard package is not installed") from exc
        self._guard = PromptGuard(config or {})

    def analyze(self, content):
        return PromptGuardResult.from_raw(self._guard.analyze(content))

    def sanitize_output(self, content):
        return self._guard.sanitize_output(content)


class HttpPromptGuard:
    def __init__(self, base_url, timeout=DEFAULT_HTTP_TIMEOUT_SECONDS, urlopen=None):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.urlopen = urlopen or urllib_request.urlopen

    def analyze(self, content):
        payload = self._post_scan(content, "analyze")
        return _scan_response_to_result(payload)

    def sanitize_output(self, content):
        return self._post_scan(content, "sanitize")

    def _post_scan(self, content, scan_type):
        body = json.dumps({"content": content, "type": scan_type}).encode("utf-8")
        request = urllib_request.Request(
            f"{self.base_url}/scan",
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        started_at = time.perf_counter()
        try:
            with self.urlopen(request, timeout=self.timeout) as response:
                payload = json.loads(response.read().decode("utf-8"))
                elapsed = time.perf_counter() - started_at
                logger.info("Prompt Guard HTTP %s completed in %.2fs", scan_type, elapsed)
                return payload
        except Exception as exc:
            elapsed = time.perf_counter() - started_at
            logger.warning("Prompt Guard HTTP %s failed after %.2fs", scan_type, elapsed)
            raise PromptSecurityError(f"Prompt Guard HTTP {scan_type} failed") from exc


class PromptSecurityService:
    def __init__(self, guard=None, blocking_severities=None):
        self.guard = guard or _default_guard()
        self.blocking_severities = set(blocking_severities or BLOCKING_SEVERITIES)

    def check_input(self, content, label="input"):
        self._check_content(content, label)

    def check_final_prompt(self, content):
        self._check_content(content, "final prompt")

    def guard_output(self, content):
        # Only sanitize (strip injection patterns from output).
        # Full analyze is skipped for model output because legitimate Korean summaries
        # can trigger false positives (repetition_detected, text_defragmented).
        sanitized = _normalize_sanitize_result(self.guard.sanitize_output(content), content)
        if sanitized["blocked"]:
            raise PromptSecurityError("model output was blocked by Prompt Guard")
        return sanitized["sanitized_text"]

    def _check_content(self, content, label):
        result = PromptGuardResult.from_raw(self.guard.analyze(content))
        if self._is_blocked(result):
            reasons = ", ".join(result.reasons) if result.reasons else "no reason provided"
            raise PromptSecurityError(f"{label} was blocked by Prompt Guard: {result.severity} ({reasons})")

    def _is_blocked(self, result):
        return result.blocked or result.severity in self.blocking_severities


def _default_guard():
    base_url = os.getenv("PROMPT_GUARD_BASE_URL") or os.getenv("AI_REPORT_PROMPT_GUARD_BASE_URL")
    if base_url:
        logger.info("Prompt Guard HTTP client enabled. base_url=%s", base_url)
        return HttpPromptGuard(base_url, timeout=_prompt_guard_timeout_seconds())
    try:
        logger.info("Prompt Guard local package enabled.")
        return LocalPromptGuard()
    except RuntimeError:
        logger.warning("Prompt Guard is unavailable. Falling back to NoOpPromptGuard.")
        return NoOpPromptGuard()


def _prompt_guard_timeout_seconds():
    raw_value = os.getenv("PROMPT_GUARD_TIMEOUT_SECONDS") or os.getenv("AI_REPORT_PROMPT_GUARD_TIMEOUT_SECONDS")
    if not raw_value:
        return DEFAULT_HTTP_TIMEOUT_SECONDS
    try:
        return float(raw_value)
    except ValueError as exc:
        raise RuntimeError("Prompt Guard timeout must be a number of seconds.") from exc


def _scan_response_to_result(payload):
    matches = payload.get("matches") or []
    severities = [
        str(match.get("severity", "SAFE")).upper()
        for match in matches
        if isinstance(match, dict)
    ]
    severity = max(severities or ["SAFE"], key=lambda item: SEVERITY_ORDER.get(item, 0))
    reasons = [
        str(match.get("type") or match.get("pattern") or match.get("reason"))
        for match in matches
        if isinstance(match, dict) and (match.get("type") or match.get("pattern") or match.get("reason"))
    ]
    return PromptGuardResult(
        severity=severity,
        action=str(payload.get("action") or "allow").lower(),
        blocked=bool(payload.get("blocked", False)),
        reasons=reasons,
    )


def _enum_or_text(value):
    raw_value = getattr(value, "value", value)
    name = getattr(raw_value, "name", None)
    if name:
        return str(name)
    return str(raw_value)


def _mapping_value(value, key):
    if isinstance(value, dict):
        return value.get(key)
    return None


def _normalize_sanitize_result(raw_result: Any, fallback_text):
    blocked = bool(getattr(raw_result, "blocked", False) or _mapping_value(raw_result, "blocked"))
    sanitized_text = (
        getattr(raw_result, "sanitized_text", None)
        or _mapping_value(raw_result, "sanitized_text")
        or fallback_text
    )
    return {
        "blocked": blocked,
        "sanitized_text": sanitized_text,
    }
