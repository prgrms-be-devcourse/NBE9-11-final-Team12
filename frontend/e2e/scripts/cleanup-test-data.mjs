import { readFile, rm } from "node:fs/promises"
import { resolve } from "node:path"
import { E2eApiClient, login } from "./e2e-api.mjs"

const statePath = resolve("e2e/.e2e-state.json")
const adminEmail = process.env.E2E_ADMIN_EMAIL
const adminPassword = process.env.E2E_ADMIN_PASSWORD

if (!adminEmail || !adminPassword) {
  throw new Error("E2E_ADMIN_EMAIL, E2E_ADMIN_PASSWORD 환경 변수가 필요합니다.")
}

async function ignore(description, action) {
  try {
    await action()
  } catch (error) {
    console.warn(`[cleanup skipped] ${description}: ${error.message}`)
  }
}

const state = JSON.parse(await readFile(statePath, "utf8"))
const roomId = state.room.roomId
const topicId = state.topic.topicId

for (const user of [state.users.speaker, state.users.reporter]) {
  const client = new E2eApiClient()
  await ignore(`${user.email} login`, () => login(client, user.email, user.password))
  await ignore(`${user.email} leave room`, () => client.post(`/api/v1/rooms/${roomId}/participants/out`))
}

const admin = new E2eApiClient()
await login(admin, adminEmail, adminPassword)
await ignore("delete room", () => admin.delete(`/api/v1/admin/rooms/${roomId}`))
await ignore("delete topic", () => admin.delete(`/api/v1/admin/topics/${topicId}`))
await ignore("remove e2e state", () => rm(statePath, { force: true }))

console.log("E2E test data cleanup completed.")
