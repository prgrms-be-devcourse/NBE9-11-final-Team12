"""Standalone AI report generation model test harness."""

from aireport.config import AiReportConfig, DEFAULT_MODEL_PATH
from aireport.input_contract import normalize_report_request
from aireport.llama_cpp_client import LlamaCppClient
from aireport.opinion_clusterer import build_clustered_debate_input
from aireport.report_generator import ReportGenerationError, ReportGenerator

__all__ = [
    "AiReportConfig",
    "DEFAULT_MODEL_PATH",
    "LlamaCppClient",
    "ReportGenerationError",
    "ReportGenerator",
    "build_clustered_debate_input",
    "normalize_report_request",
]
