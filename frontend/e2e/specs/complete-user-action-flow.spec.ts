import { expect, test, type Browser, type Page } from "@playwright/test"
import { loadE2eState, loginByUi } from "../helpers/state"

async function loginRoomPage(browser: Browser, email: string, password: string, roomId: number): Promise<Page> {
  const context = await browser.newContext({ baseURL: process.env.E2E_FRONTEND_BASE_URL ?? "http://localhost:3000" })
  const page = await context.newPage()
  await loginByUi(page, email, password)
  await page.goto(`/rooms/${roomId}`)
  await expect(page.getByRole("heading").first()).toBeVisible()
  await expect(page.getByText("실시간 채팅")).toBeVisible()
  return page
}

test.describe("완전한 사용자 행동 플로우", () => {
  test("의견 작성, 공감, 의견 신고, 채팅 전송, 채팅 신고를 UI에서 검증한다", async ({ browser }) => {
    test.setTimeout(60_000)
    const state = loadE2eState()
    const uniqueSuffix = Date.now()
    const speechContent = `E2E UI 의견 작성 검증 ${uniqueSuffix}`
    const speechReportDescription = `E2E UI 의견 신고 상세 ${uniqueSuffix}`
    const chatContent = `E2E UI 채팅 전송 검증 ${uniqueSuffix}`
    const chatReportDescription = `E2E UI 채팅 신고 상세 ${uniqueSuffix}`

    const speakerPage = await loginRoomPage(browser, state.users.speaker.email, state.users.speaker.password, state.room.roomId)
    const reporterPage = await loginRoomPage(browser, state.users.reporter.email, state.users.reporter.password, state.room.roomId)

    try {
      await test.step("발언자가 UI에서 발언권을 확보하고 메인 의견을 작성한다", async () => {
        await expect(speakerPage.getByText("기본 발언 시간 3분").last()).toBeVisible()

        const requestTurnButton = speakerPage.getByRole("button", { name: "발언권 신청" })
        if (await requestTurnButton.isVisible().catch(() => false)) {
          const requestTurnResponse = speakerPage.waitForResponse((response) =>
            response.url().includes(`/api/v1/rooms/${state.room.roomId}/stage/requests`) &&
            response.request().method() === "POST" &&
            response.status() === 201,
          )
          await requestTurnButton.click()
          await requestTurnResponse
          await expect(speakerPage.getByText(new RegExp(`${state.users.speaker.nickname}님 발언 중`))).toBeVisible()
        }

        await speakerPage.getByRole("button", { name: /의견 작성/ }).click()
        const dialog = speakerPage.getByRole("dialog", { name: "메인 의견 작성" })
        await expect(dialog).toBeVisible()
        await dialog.getByRole("button", { name: "찬성" }).click()
        await dialog.getByPlaceholder("의견을 입력하세요.").fill(speechContent)

        const createSpeechResponse = speakerPage.waitForResponse((response) =>
          response.url().includes(`/api/v1/rooms/${state.room.roomId}/speeches`) &&
          response.request().method() === "POST" &&
          response.status() === 201,
        )
        await dialog.getByRole("button", { name: "등록" }).click()
        await createSpeechResponse

        await expect(speakerPage.getByText(speechContent)).toBeVisible()
      })

      await test.step("다른 사용자가 UI에서 의견에 공감하고 신고한다", async () => {
        await reporterPage.reload()
        const speechCard = reporterPage.getByRole("article").filter({ hasText: speechContent }).first()
        await expect(speechCard).toBeVisible()
        const reactionResponse = reporterPage.waitForResponse((response) =>
          response.url().includes("/api/v1/speeches/") &&
          response.url().includes("/reactions") &&
          response.request().method() === "POST" &&
          response.status() === 201,
        )
        await speechCard.getByRole("button", { name: /공감/ }).click()
        await reactionResponse
        await expect(speechCard.getByRole("button", { name: /공감 취소/ })).toBeVisible()

        await speechCard.getByRole("button", { name: "신고" }).click()
        const reportDialog = reporterPage.getByRole("dialog", { name: /의견 신고/ })
        await expect(reportDialog).toBeVisible()
        await reportDialog.getByRole("button", { name: "기타" }).click()
        await reportDialog.getByPlaceholder("기타 사유는 상세 설명이 필수입니다.").fill(speechReportDescription)

        const reportResponse = reporterPage.waitForResponse((response) =>
          response.url().includes("/api/v1/speeches/") &&
          response.url().includes("/reports") &&
          response.request().method() === "POST" &&
          response.status() === 201,
        )
        await reportDialog.getByRole("button", { name: "신고하기" }).click()
        await reportResponse
        await expect(reportDialog.getByText("신고가 접수되었습니다.")).toBeVisible()
        await reportDialog.getByRole("button", { name: "확인" }).click()
      })

      await test.step("채팅 메시지를 UI에서 전송하고 수신자 화면에서 재조회한다", async () => {
        await expect(speakerPage.getByText("실시간 채팅")).toBeVisible()
        await expect(reporterPage.getByText("실시간 채팅")).toBeVisible()

        await speakerPage.getByPlaceholder("메시지 입력...").fill(chatContent)
        await speakerPage.getByRole("button", { name: "전송" }).click()

        await expect(speakerPage.getByText(chatContent).first()).toBeVisible()
        await reporterPage.reload()
        await expect(reporterPage.getByText(chatContent).first()).toBeVisible({ timeout: 10_000 })
      })

      await test.step("다른 사용자가 UI에서 채팅 메시지를 신고한다", async () => {
        const chatMessageText = reporterPage.getByText(chatContent, { exact: true }).first()
        const chatMessage = chatMessageText.locator("xpath=ancestor::div[contains(@class, 'group')][1]")
        await chatMessage.hover()
        await chatMessage.getByRole("button", { name: "메시지 신고" }).click()

        const reportDialog = reporterPage.getByRole("dialog", { name: /채팅 메시지 신고/ })
        await expect(reportDialog).toBeVisible()
        await expect(reportDialog.getByText(chatContent)).toBeVisible()
        await reportDialog.getByRole("button", { name: "주제 무관" }).click()
        await reportDialog.getByPlaceholder("상세 설명 (선택)").fill(chatReportDescription)

        const reportResponse = reporterPage.waitForResponse((response) =>
          response.url().includes(`/api/v1/rooms/${state.room.roomId}/chat/messages/`) &&
          response.url().includes("/reports") &&
          response.request().method() === "POST" &&
          response.status() === 201,
        )
        await reportDialog.getByRole("button", { name: "신고하기" }).click()
        await reportResponse
        await expect(reportDialog.getByText("신고가 접수되었습니다.")).toBeVisible()
      })
    } finally {
      await speakerPage.context().close()
      await reporterPage.context().close()
    }
  })
})
