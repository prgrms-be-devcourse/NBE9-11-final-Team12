import { readFileSync } from "node:fs"
import { resolve } from "node:path"
import type { Page } from "@playwright/test"

export type E2eState = {
  runId: string
  apiBaseUrl: string
  frontendBaseUrl: string
  admin: {
    email: string
  }
  users: {
    speaker: {
      email: string
      password: string
      nickname: string
      userId: number
    }
    reporter: {
      email: string
      password: string
      nickname: string
      userId: number
    }
  }
  room: {
    roomId: number
    title: string
  }
  speeches: {
    first: {
      speechId: number
      content: string
    }
    second: {
      speechId: number
      content: string
    }
  }
  report: {
    reportId: number
    speechId: number
    speechContent: string
    description: string
  }
}

export function loadE2eState(): E2eState {
  const statePath = resolve("e2e/.e2e-state.json")
  return JSON.parse(readFileSync(statePath, "utf8")) as E2eState
}

export async function loginByUi(page: Page, email: string, password: string) {
  await page.goto("/login")
  await page.locator("#email").fill(email)
  await page.locator("#password").fill(password)
  await page.getByRole("button", { name: /^로그인$/ }).click()
  await page.getByText("로그아웃").waitFor()
}
