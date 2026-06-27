import { expect, test } from "@playwright/test"
import { loadE2eState, loginByUi } from "../helpers/state"

test.describe("예외·제한 정책과 시각 표시", () => {
  test("비로그인 사용자는 토론방 접근 시 로그인 화면으로 이동한다", async ({ page }) => {
    const state = loadE2eState()

    await page.goto(`/rooms/${state.room.roomId}`)
    await expect(page).toHaveURL(/\/login/)
  })

  test("잘못된 로그인 정보는 에러 메시지를 표시한다", async ({ page }) => {
    await page.goto("/login")
    await page.locator("#email").fill("not-found@example.com")
    await page.locator("#password").fill("wrong-password")
    await page.getByRole("button", { name: /^로그인$/ }).click()

    await expect(page.getByText(/로그인에 실패|인증|일치/)).toBeVisible()
  })

  test("토론방 화면에 원시 enum 라벨을 노출하지 않는다", async ({ page }) => {
    const state = loadE2eState()

    await loginByUi(page, state.users.speaker.email, state.users.speaker.password)
    await page.goto(`/rooms/${state.room.roomId}`)

    await expect(page.getByText(/\b(NORMAL|NEW|SPEAKING|READY|COMPLETED)\b/)).toHaveCount(0)
  })
})
