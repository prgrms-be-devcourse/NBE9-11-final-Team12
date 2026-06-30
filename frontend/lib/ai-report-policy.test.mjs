import assert from "node:assert/strict"
import test from "node:test"

import {
  CUSTOM_PROMPT_MAX_COUNT,
  aiReportErrorMessage,
} from "./ai-report-policy.js"

test("custom prompt max count matches backend policy", () => {
  assert.equal(CUSTOM_PROMPT_MAX_COUNT, 3)
})

test("shows explicit message when Prompt Guard blocks a custom prompt", () => {
  const message = aiReportErrorMessage(
    { code: "PROMPT_GUARD_BLOCKED", message: "blocked", status: 422 },
    "fallback",
  )

  assert.equal(message, "개인화 요청에 안전하지 않은 지시가 포함되어 있어 생성할 수 없습니다.")
})

test("shows explicit message when Prompt Guard is unavailable", () => {
  const message = aiReportErrorMessage(
    { code: "PROMPT_GUARD_UNAVAILABLE", message: "unavailable", status: 503 },
    "fallback",
  )

  assert.equal(message, "개인화 요청 안전성 검사 서버가 준비되지 않았습니다. 잠시 후 다시 시도해주세요.")
})
