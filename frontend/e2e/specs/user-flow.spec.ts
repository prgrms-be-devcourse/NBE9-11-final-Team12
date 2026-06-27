import { expect, test } from "@playwright/test"
import { loadE2eState, loginByUi } from "../helpers/state"

test.describe("핵심 사용자 플로우", () => {
  test("로그인 후 토론방에서 신뢰도, 발언 시간, 의견 묶음 표시를 확인한다", async ({ page }) => {
    const state = loadE2eState()

    await loginByUi(page, state.users.speaker.email, state.users.speaker.password)
    await page.goto(`/rooms/${state.room.roomId}`)

    await expect(page.getByText(state.room.title)).toBeVisible()
    await expect(page.getByText("내 신뢰도")).toBeVisible()
    await expect(page.getByText(/새싹 참여자|활동 참여자|꾸준한 기여자|토론 리더/)).toBeVisible()
    await expect(page.getByText("기본 발언 시간 3분")).toBeVisible()
    await expect(page.getByText(/남은 시간 \d+:\d{2}/)).toBeVisible()
    await expect(page.getByText(state.speeches.first.content)).toBeVisible()
    await expect(page.getByText(state.speeches.second.content)).toBeVisible()
    await expect(page.getByText("2개 묶음")).toBeVisible()
    await expect(page.getByText("발언 중").first()).toBeVisible()
  })
})
