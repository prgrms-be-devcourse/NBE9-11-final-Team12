import assert from "node:assert/strict"
import test from "node:test"

import { getPaymentSuccessRedirectPath } from "./payment-success-flow.js"

test("custom AI report payment success stays on the success page instead of entering the room", () => {
  const path = getPaymentSuccessRedirectPath({ targetType: "CUSTOM_AI_REPORT" }, "10")

  assert.equal(path, null)
})
