# Prompt Guard AI Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Prompt Guard defense-in-depth to the Python AI report server, including personalized custom prompt support.

**Architecture:** Add a focused guard module with injectable behavior, extend request normalization for `customPrompts`, and wire the guard through `ReportGenerator`. Keep FastAPI DTOs aligned with the normalized request contract.

**Tech Stack:** Python 3.10+, FastAPI, Pydantic v2, unittest, optional `prompt_guard`.

---

### Task 1: Custom Prompt Contract

**Files:**
- Modify: `aireport/input_contract.py`
- Test: `test/test_prompt_security.py`

- [ ] Write failing tests for accepting one to five custom prompts, rejecting six prompts, and rejecting blank prompt text.
- [ ] Run `python -m unittest test.test_prompt_security -v` and verify the new tests fail.
- [ ] Implement normalization helpers that return `customPrompts` as `{label, prompt}` dictionaries.
- [ ] Run the focused tests and verify they pass.

### Task 2: Prompt Guard Abstraction

**Files:**
- Create: `aireport/prompt_security.py`
- Test: `test/test_prompt_security.py`

- [ ] Write failing tests for blocking `HIGH` and `CRITICAL` severities.
- [ ] Run the focused tests and verify failure.
- [ ] Implement `PromptSecurityError`, `PromptGuardResult`, `NoOpPromptGuard`, and `PromptSecurityService`.
- [ ] Run focused tests and verify they pass.

### Task 3: Report Generator Integration

**Files:**
- Modify: `aireport/report_generator.py`
- Test: `test/test_prompt_security.py`

- [ ] Write failing tests showing unsafe custom prompts, final assembled prompts, and unsafe output stop generation.
- [ ] Run the focused tests and verify failure.
- [ ] Inject `prompt_security` into `ReportGenerator`, scan custom prompts before model calls, scan final prompts, and guard raw output before parsing.
- [ ] Run focused tests and verify they pass.

### Task 4: API Contract

**Files:**
- Modify: `aireport/api.py`
- Test: `test/test_prompt_security.py`

- [ ] Write failing tests for FastAPI request model accepting `customPrompts`.
- [ ] Run focused tests and verify failure.
- [ ] Add `CustomPromptPayload` and include it in `AiReportGenerateRequest`.
- [ ] Run focused tests and verify they pass.

### Task 5: Full Verification

**Files:**
- Run tests only.

- [ ] Run `python -m unittest discover -s test -v`.
- [ ] Read output and fix any regressions.
- [ ] Summarize changed files and any remaining operational setup needed for Spring Boot or Prompt Guard deployment.
