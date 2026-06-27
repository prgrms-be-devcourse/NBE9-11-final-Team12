import { expect, test } from "@playwright/test"
import { loadE2eState, loginByUi } from "../helpers/state"

test.describe("핵심 사용자 플로우", () => {
  test("로그인 후 토론방에서 신뢰도, 발언 시간, 의견 묶음 표시를 확인한다", async ({ page }) => {
    const state = loadE2eState()

    await loginByUi(page, state.users.speaker.email, state.users.speaker.password)
    await page.goto(`/rooms/${state.room.roomId}`)

    await expect(page.getByRole("heading", { name: state.room.title }).first()).toBeVisible()
    await expect(page.getByText("내 신뢰도")).toBeVisible()
    await expect(page.getByText(/새싹 참여자|활동 참여자|꾸준한 기여자|토론 리더/)).toBeVisible()
    await expect(page.getByText("기본 발언 시간 3분").last()).toBeVisible()
    await expect(page.getByRole("article").filter({ hasText: state.speeches.first.content })).toBeVisible()
    await expect(page.getByRole("article").filter({ hasText: state.speeches.second.content })).toBeVisible()
    await expect(page.getByRole("article").getByText(/[23]개 묶음/)).toBeVisible()
    await expect(page.getByRole("article").getByText(/발언 중|발언 완료/)).toBeVisible()
  })
})
