import { expect, test } from "@playwright/test"
import { loadE2eState, loginByUi } from "../helpers/state"

const adminPassword = process.env.E2E_ADMIN_PASSWORD

test.describe("관리자 신고·제재 플로우", () => {
  test.skip(!adminPassword, "E2E_ADMIN_PASSWORD 환경 변수가 필요합니다.")

  test("의견 신고를 검토 시작하고 위반 확정 후 제재 추천 UI를 확인한다", async ({ page }) => {
    const state = loadE2eState()

    await loginByUi(page, state.admin.email, adminPassword!)
    await page.goto("/admin")
    await page.getByRole("tab", { name: "신고/제재 관리" }).click()
    await page.getByRole("button", { name: "의견 신고" }).click()
    await page.getByRole("button", { name: /검토 대기/ }).click()
    await page.getByRole("button", { name: new RegExp(`#${state.report.reportId}`) }).click()

    await expect(page.getByText(state.report.description)).toBeVisible()
    await expect(page.getByText("대상 사용자의 최근 제재 이력이 없습니다.").or(page.getByText("최근 제재 이력"))).toBeVisible()

    await page.getByRole("button", { name: "검토 시작" }).click()
    await expect(page.getByText("검토 중")).toBeVisible()

    await page.locator("select").first().selectOption("MEDIUM")
    await page.getByPlaceholder("처리 또는 반려 사유").fill("E2E 검증 결과 운영 정책 위반으로 확정합니다.")
    await page.getByRole("button", { name: "위반 확정" }).click()

    await expect(page.getByText("처리 완료")).toBeVisible()
    await page.getByRole("button", { name: "제재 추천안 계산" }).click()
    await expect(page.getByText(/추천 제재 유형/)).toBeVisible()
    await expect(page.getByText("관리자 직접 제재 적용")).toBeVisible()
  })
})
