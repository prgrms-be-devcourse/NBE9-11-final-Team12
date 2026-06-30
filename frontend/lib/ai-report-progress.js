export function getAiReportProgressActionState(report, generationKind) {
  if (!report) {
    return null
  }

  if (isReportFailed(report.status)) {
    return {
      label: "생성 실패",
      busy: false,
      clickable: false,
      tone: "failed",
    }
  }

  if (isReportInProgress(report.status)) {
    if (!generationKind && !report.pdf?.pdfExportId) {
      return null
    }

    const isCustomGeneration = generationKind === "custom"
    const customLabel = isBaseReportCompleted(report)
      ? "커스텀 리포트가 생성 중입니다."
      : "기본 리포트 완료 후 커스텀 리포트가 생성됩니다."

    return {
      label: isCustomGeneration
        ? customLabel
        : "기본 AI 요약 리포트가 생성 중입니다.",
      busy: true,
      clickable: false,
      tone: "pending",
    }
  }

  if (report.status !== "COMPLETED") {
    return null
  }

  const pdf = report.pdf
  if (!pdf?.pdfExportId) {
    return null
  }

  if (pdf?.downloadAvailable || pdf?.pdfStatus === "READY") {
    return {
      label: "PDF 다운로드",
      busy: false,
      clickable: true,
      tone: "ready",
    }
  }

  if (pdf?.pdfStatus === "FAILED") {
    return {
      label: "PDF 생성 실패",
      busy: false,
      clickable: false,
      tone: "failed",
    }
  }

  return {
    label: "PDF 준비 중",
    busy: true,
    clickable: false,
    tone: "pending",
  }
}

function isReportInProgress(status) {
  return status === "REQUESTED"
    || status === "QUEUED"
    || status === "PROCESSING"
    || status === "PENDING"
}

function isReportFailed(status) {
  return status === "PUBLISH_FAILED"
    || status === "GENERATION_FAILED"
    || status === "BLOCKED"
    || status === "FAILED"
}

function isBaseReportCompleted(report) {
  return Boolean(report.completedAt || report.coreLine || report.aiSummary)
}
