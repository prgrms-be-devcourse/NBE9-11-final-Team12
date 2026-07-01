export const CUSTOM_PROMPT_MAX_COUNT = 3

export function aiReportErrorMessage(error, fallback = "AI 리포트를 불러오지 못했습니다.") {
  if (!error || typeof error !== "object") return fallback

  if (error.code === "AI_REPORT_ROOM_NOT_CLOSED") {
    return "종료된 토론방만 AI 리포트를 생성할 수 있습니다."
  }
  if (error.code === "AI_REPORT_CUSTOM_PROMPT_REQUIRED") {
    return "커스텀 리포트에 사용할 프롬프트를 입력해주세요."
  }
  if (error.code === "AI_REPORT_NOT_FOUND") {
    return "기본 AI 요약 리포트가 아직 준비되지 않았습니다."
  }
  if (error.code === "PROMPT_GUARD_BLOCKED") {
    return "개인화 요청에 안전하지 않은 지시가 포함되어 있어 생성할 수 없습니다."
  }
  if (error.code === "PROMPT_GUARD_UNAVAILABLE") {
    return "개인화 요청 안전성 검사 서버가 준비되지 않았습니다. 잠시 후 다시 시도해주세요."
  }
  if (error.code === "UNAUTHORIZED" || error.status === 401) {
    return "로그인이 필요한 기능입니다."
  }

  return error.message || fallback
}
