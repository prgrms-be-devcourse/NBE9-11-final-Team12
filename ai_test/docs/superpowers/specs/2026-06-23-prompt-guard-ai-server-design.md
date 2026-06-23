# Prompt Guard AI Server Design

## Goal

Add a second prompt-injection defense layer to the Python AI report server. Spring Boot remains the first gate, while this service validates personalized prompts again, keeps untrusted debate data separated from system instructions, scans the final assembled prompt, and guards LLM output before returning a report.

## Scope

- Accept up to five personalized `customPrompts` in report requests.
- Treat both debate speeches and `customPrompts` as untrusted user-controlled data.
- Block unsafe input or final prompts when Prompt Guard returns `HIGH` or `CRITICAL`.
- Scan and sanitize model output before JSON parsing and schema validation.
- Keep basic reports working when `customPrompts` is absent or empty.
- Document Spring Boot work as a handoff prompt instead of editing Spring code in this repository.

## Architecture

The AI server uses a small guard abstraction around Prompt Guard so tests can inject deterministic fake scan results. Request normalization validates `customPrompts` shape and length before prompt generation. `ReportGenerator` builds a prompt with explicit trusted and untrusted sections, scans the final prompt, calls the model only after approval, then scans/sanitizes the raw LLM output before JSON schema validation.

## Data Flow

1. FastAPI receives room, topic, speeches, and optional `customPrompts`.
2. Pydantic performs request-level shape validation.
3. `normalize_report_request` compacts speeches and normalizes `customPrompts`.
4. The guard scans each custom prompt and the final model prompt.
5. The model generates JSON text.
6. The guard sanitizes and scans output.
7. `validate_report` enforces required fields and returns the report.

## Decisions

- The Python service blocks `HIGH` and `CRITICAL`; `SAFE`, `LOW`, and `MEDIUM` pass.
- Guard dependency is optional at import time. If `prompt_guard` is unavailable, the default local guard allows content so local model tests still run. Production can install Prompt Guard for enforcement.
- Output redaction is preferred when Prompt Guard can sanitize safely. If the sanitized output is still blocked, generation fails.
- `customPrompts` are advisory personalization requests. The prompt template instructs the LLM that they must not override system rules, schema, or safety instructions.

## Testing

- Unit tests cover custom prompt normalization and validation.
- Unit tests cover input guard blocking, final prompt blocking, and output blocking.
- Existing schema validation remains the final structural gate.
