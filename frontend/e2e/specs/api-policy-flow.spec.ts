import { expect, test } from "@playwright/test"
import { E2eApiClient, login, signupOrLogin } from "../scripts/e2e-api.mjs"
import { loadE2eState } from "../helpers/state"

const adminPassword = process.env.E2E_ADMIN_PASSWORD
const testPassword = process.env.E2E_TEST_PASSWORD ?? "E2eTest123!"

function testUser(prefix: string, runId: string) {
  return {
    email: `e2e-${prefix}-${runId}-${Date.now()}@example.com`,
    password: testPassword,
    nickname: `E2E${prefix}${String(Date.now()).slice(-4)}`,
  }
}

async function joinRoom(client: E2eApiClient, roomId: number) {
  await client.post(`/api/v1/rooms/${roomId}/participants`, undefined, {
    allowStatuses: [409],
  })
}

test.describe.serial("API 기반 실제 사용자·관리자 정책 플로우", () => {
  test.skip(!adminPassword, "E2E_ADMIN_PASSWORD 환경 변수가 필요합니다.")

  test("Access Token 만료·삭제 후 Refresh Token 재발급과 로그아웃을 검증한다", async () => {
    const state = loadE2eState()
    const client = new E2eApiClient()

    await login(client, state.users.speaker.email, state.users.speaker.password)
    expect(client.hasCookie("accessToken")).toBeTruthy()
    expect(client.hasCookie("refreshToken")).toBeTruthy()

    const me = await client.get("/api/v1/users/me")
    expect(me.email).toBe(state.users.speaker.email)

    client.deleteCookie("accessToken")
    const unauthorized = await client.requestRaw("/api/v1/users/me", {
      allowStatuses: [401],
    })
    expect(unauthorized.status).toBe(401)

    const reissue = await client.requestRaw("/api/v1/auth/reissue", {
      method: "POST",
    })
    expect(reissue.status).toBe(200)
    expect(client.hasCookie("accessToken")).toBeTruthy()
    expect(client.hasCookie("refreshToken")).toBeTruthy()

    const meAfterReissue = await client.get("/api/v1/users/me")
    expect(meAfterReissue.userId).toBe(state.users.speaker.userId)

    const logout = await client.requestRaw("/api/v1/auth/logout", {
      method: "POST",
    })
    expect(logout.status).toBe(200)

    const afterLogout = await client.requestRaw("/api/v1/users/me", {
      allowStatuses: [401],
    })
    expect(afterLogout.status).toBe(401)
  })

  test("의견 작성·공감·신고의 핵심 예외 정책을 검증한다", async () => {
    const state = loadE2eState()
    const speaker = new E2eApiClient()
    const reporter = new E2eApiClient()

    await login(speaker, state.users.speaker.email, state.users.speaker.password)
    await login(reporter, state.users.reporter.email, state.users.reporter.password)

    const emptySpeech = await speaker.requestRaw(`/api/v1/rooms/${state.room.roomId}/speeches`, {
      method: "POST",
      body: { content: "", stance: "PRO" },
      allowStatuses: [400],
    })
    expect(emptySpeech.status).toBe(400)
    expect(emptySpeech.code).toBe("INVALID_INPUT_VALUE")

    const profanitySpeech = await speaker.requestRaw(`/api/v1/rooms/${state.room.roomId}/speeches`, {
      method: "POST",
      body: { content: "시발", stance: "PRO" },
      allowStatuses: [400],
    })
    expect(profanitySpeech.status).toBe(400)
    expect(profanitySpeech.code).toBe("SPEECH_CONTENT_CONTAINS_PROFANITY")

    const selfReport = await speaker.requestRaw(`/api/v1/speeches/${state.speeches.first.speechId}/reports`, {
      method: "POST",
      body: { reason: "SPAM", description: "본인 신고 차단 검증" },
      allowStatuses: [400],
    })
    expect(selfReport.status).toBe(400)
    expect(selfReport.code).toBe("SPEECH_REPORT_SELF_NOT_ALLOWED")

    const otherWithoutDescription = await reporter.requestRaw(`/api/v1/speeches/${state.speeches.first.speechId}/reports`, {
      method: "POST",
      body: { reason: "OTHER" },
      allowStatuses: [400],
    })
    expect(otherWithoutDescription.status).toBe(400)
    expect(otherWithoutDescription.code).toBe("SPEECH_REPORT_DESCRIPTION_REQUIRED")

    const reportForDuplicateCheck = await reporter.requestRaw(`/api/v1/speeches/${state.speeches.second.speechId}/reports`, {
      method: "POST",
      body: { reason: "SPAM", description: "중복 신고 검증을 위한 최초 신고" },
    })
    expect(reportForDuplicateCheck.status).toBe(201)

    const duplicateReport = await reporter.requestRaw(`/api/v1/speeches/${state.speeches.second.speechId}/reports`, {
      method: "POST",
      body: { reason: "OFF_TOPIC", description: "중복 신고 차단 검증" },
      allowStatuses: [409],
    })
    expect(duplicateReport.status).toBe(409)
    expect(duplicateReport.code).toBe("SPEECH_REPORT_ALREADY_EXISTS")

    const duplicateReaction = await reporter.requestRaw(`/api/v1/speeches/${state.speeches.first.speechId}/reactions`, {
      method: "POST",
      allowStatuses: [409],
    })
    expect(duplicateReaction.status).toBe(409)
    expect(duplicateReaction.code).toBe("SPEECH_REACTION_ALREADY_EXISTS")

    const deleteReaction = await reporter.requestRaw(`/api/v1/speeches/${state.speeches.first.speechId}/reactions`, {
      method: "DELETE",
    })
    expect(deleteReaction.status).toBe(200)

    const missingReaction = await reporter.requestRaw(`/api/v1/speeches/${state.speeches.first.speechId}/reactions`, {
      method: "DELETE",
      allowStatuses: [404],
    })
    expect(missingReaction.status).toBe(404)
    expect(missingReaction.code).toBe("SPEECH_REACTION_NOT_FOUND")

    await reporter.post(`/api/v1/speeches/${state.speeches.first.speechId}/reactions`)
  })

  test("발언권·의견 제한과 계정 정지 세션 무효화를 검증한다", async () => {
    const state = loadE2eState()
    const admin = new E2eApiClient()
    const restricted = new E2eApiClient()
    const suspended = new E2eApiClient()

    await login(admin, state.admin.email, adminPassword!)

    const restrictedUser = await signupOrLogin(restricted, testUser("restricted", state.runId))
    await joinRoom(restricted, state.room.roomId)

    const speechRestriction = await admin.post(`/api/v1/admin/users/${restrictedUser.userId}/sanctions`, {
      type: "SPEECH_RESTRICTION",
      reason: "E2E 의견·발언권 제한 검증",
      durationHours: 1,
    })
    expect(speechRestriction.type).toBe("SPEECH_RESTRICTION")

    const stageDenied = await restricted.requestRaw(`/api/v1/rooms/${state.room.roomId}/stage/requests`, {
      method: "POST",
      body: { stance: null },
      allowStatuses: [403],
    })
    expect(stageDenied.status).toBe(403)
    expect(stageDenied.code).toBe("USER_STAGE_RESTRICTED")

    const speechDenied = await restricted.requestRaw(`/api/v1/rooms/${state.room.roomId}/speeches`, {
      method: "POST",
      body: { content: "제한 상태에서는 의견이 등록되지 않아야 합니다.", stance: "PRO" },
      allowStatuses: [403],
    })
    expect(speechDenied.status).toBe(403)
    expect(speechDenied.code).toBe("USER_SPEECH_RESTRICTED")

    const activeSanctions = await restricted.get("/api/v1/users/me/sanctions/active")
    expect(activeSanctions.map((sanction: { type: string }) => sanction.type)).toContain("SPEECH_RESTRICTION")

    const suspendedAccount = testUser("suspended", state.runId)
    const suspendedUser = await signupOrLogin(suspended, suspendedAccount)
    await joinRoom(suspended, state.room.roomId)

    const suspension = await admin.post(`/api/v1/admin/users/${suspendedUser.userId}/sanctions`, {
      type: "ACCOUNT_SUSPENSION",
      reason: "E2E 계정 정지 검증",
    })
    expect(suspension.type).toBe("ACCOUNT_SUSPENSION")

    const oldAccessDenied = await suspended.requestRaw("/api/v1/users/me", {
      allowStatuses: [403],
    })
    expect(oldAccessDenied.status).toBe(403)
    expect(oldAccessDenied.code).toBe("USER_BANNED")

    const reissueDenied = await suspended.requestRaw("/api/v1/auth/reissue", {
      method: "POST",
      allowStatuses: [401, 403],
    })
    expect([401, 403]).toContain(reissueDenied.status)

    const loginDenied = await new E2eApiClient().requestRaw("/api/v1/auth/login", {
      method: "POST",
      body: { email: suspendedAccount.email, password: suspendedAccount.password },
      allowStatuses: [403],
    })
    expect(loginDenied.status).toBe(403)
    expect(loginDenied.code).toBe("USER_BANNED")
  })

  test("관리자 신고 검토와 제재 추천 조회를 검증한다", async () => {
    const state = loadE2eState()
    const admin = new E2eApiClient()
    const speaker = new E2eApiClient()
    const reporter = new E2eApiClient()

    await login(admin, state.admin.email, adminPassword!)
    await login(speaker, state.users.speaker.email, state.users.speaker.password)
    await login(reporter, state.users.reporter.email, state.users.reporter.password)

    const speech = await speaker.post(`/api/v1/rooms/${state.room.roomId}/speeches`, {
      content: `E2E 관리자 검토용 의견 ${Date.now()}`,
      stance: "PRO",
    })
    const report = await reporter.post(`/api/v1/speeches/${speech.speechId}/reports`, {
      reason: "OFF_TOPIC",
      description: "E2E 관리자 검토용 신고입니다.",
    })

    const list = await admin.get("/api/v1/admin/reports?status=PENDING&page=0&size=20")
    expect(list.content.some((item: { reportId: number }) => item.reportId === report.reportId)).toBeTruthy()

    const detail = await admin.get(`/api/v1/admin/reports/${report.reportId}`)
    expect(detail.contentSnapshot).toBe(speech.content)
    expect(detail.reportedUserId).toBe(state.users.speaker.userId)

    const review = await admin.patch(`/api/v1/admin/reports/${report.reportId}`, {
      action: "START_REVIEW",
    })
    expect(review.status).toBe("REVIEWING")

    const resolveWithoutSeverity = await admin.requestRaw(`/api/v1/admin/reports/${report.reportId}`, {
      method: "PATCH",
      body: {
        action: "RESOLVE",
        resolutionNote: "심각도 누락 검증",
      },
      allowStatuses: [400],
    })
    expect(resolveWithoutSeverity.status).toBe(400)
    expect(resolveWithoutSeverity.code).toBe("SPEECH_REPORT_SEVERITY_REQUIRED")

    const resolved = await admin.patch(`/api/v1/admin/reports/${report.reportId}`, {
      action: "RESOLVE",
      resolutionNote: "E2E 검증 결과 위반으로 확정합니다.",
      severity: "MEDIUM",
    })
    expect(resolved.status).toBe("RESOLVED")
    expect(resolved.severity).toBe("MEDIUM")

    const recommendation = await admin.get(
      `/api/v1/admin/users/${state.users.speaker.userId}/sanctions/recommendation?reportId=${report.reportId}`,
    )
    expect(recommendation.reportId).toBe(report.reportId)
    expect(recommendation.userId).toBe(state.users.speaker.userId)
    expect(recommendation.recommendedType).toBeTruthy()
  })
})
