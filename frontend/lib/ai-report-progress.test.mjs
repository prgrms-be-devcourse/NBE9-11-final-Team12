import assert from "node:assert/strict"
import test from "node:test"

import { getAiReportProgressActionState } from "./ai-report-progress.js"

test("does not show auto report generation progress before user requests it", () => {
  const state = getAiReportProgressActionState({
    status: "PROCESSING",
    pdf: { pdfStatus: "NOT_STARTED", downloadAvailable: false },
  })

  assert.equal(state, null)
})

test("shows base report generation progress after user requests it", () => {
  const state = getAiReportProgressActionState({
    status: "PROCESSING",
    pdf: { pdfExportId: 99, pdfStatus: "NOT_STARTED", downloadAvailable: false },
  }, "base")

  assert.deepEqual(state, {
    label: "기본 AI 요약 리포트가 생성 중입니다.",
    busy: true,
    clickable: false,
    tone: "pending",
  })
})

test("shows custom report generation progress after user requests it", () => {
  const state = getAiReportProgressActionState({
    status: "QUEUED",
    pdf: { pdfExportId: 99, pdfStatus: "NOT_STARTED", downloadAvailable: false },
  }, "custom")

  assert.deepEqual(state, {
    label: "커스텀 리포트가 생성 중입니다.",
    busy: true,
    clickable: false,
    tone: "pending",
  })
})

test("shows pdf preparation after report completes before download is ready", () => {
  const state = getAiReportProgressActionState({
    status: "COMPLETED",
    pdf: { pdfExportId: 99, pdfStatus: "GENERATING", downloadAvailable: false },
  })

  assert.deepEqual(state, {
    label: "PDF 준비 중",
    busy: true,
    clickable: false,
    tone: "pending",
  })
})

test("allows download when pdf is ready", () => {
  const state = getAiReportProgressActionState({
    status: "COMPLETED",
    pdf: { pdfExportId: 99, pdfStatus: "READY", downloadAvailable: true },
  })

  assert.deepEqual(state, {
    label: "PDF 다운로드",
    busy: false,
    clickable: true,
    tone: "ready",
  })
})

test("shows failed state when report generation fails", () => {
  const state = getAiReportProgressActionState({
    status: "GENERATION_FAILED",
    pdf: { pdfStatus: "NOT_STARTED", downloadAvailable: false },
  })

  assert.deepEqual(state, {
    label: "생성 실패",
    busy: false,
    clickable: false,
    tone: "failed",
  })
})

test("does not show pdf progress when completed report has no viewer pdf export", () => {
  const state = getAiReportProgressActionState({
    status: "COMPLETED",
    pdf: { pdfExportId: null, pdfStatus: "NOT_STARTED", downloadAvailable: false },
  })

  assert.equal(state, null)
})
