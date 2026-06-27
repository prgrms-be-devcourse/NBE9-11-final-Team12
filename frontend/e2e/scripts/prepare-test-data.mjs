import { mkdir, writeFile } from "node:fs/promises"
import { dirname, resolve } from "node:path"
import { E2eApiClient, apiBaseUrl, login, signupOrLogin } from "./e2e-api.mjs"

const statePath = resolve("e2e/.e2e-state.json")
const frontendBaseUrl = process.env.E2E_FRONTEND_BASE_URL ?? "http://localhost:3000"
const adminEmail = process.env.E2E_ADMIN_EMAIL
const adminPassword = process.env.E2E_ADMIN_PASSWORD
const testPassword = process.env.E2E_TEST_PASSWORD ?? "E2eTest123!"
const runId = process.env.E2E_RUN_ID ?? new Date().toISOString().replace(/[-:.TZ]/g, "").slice(0, 14)

if (!adminEmail || !adminPassword) {
  throw new Error("E2E_ADMIN_EMAIL, E2E_ADMIN_PASSWORD 환경 변수가 필요합니다.")
}

function idOf(data, ...keys) {
  for (const key of keys) {
    if (data?.[key] !== undefined && data[key] !== null) return data[key]
  }
  throw new Error(`응답에서 ID를 찾지 못했습니다. keys=${keys.join(",")}`)
}

const admin = new E2eApiClient()
await login(admin, adminEmail, adminPassword)

const speaker = {
  email: `e2e-speaker-${runId}@example.com`,
  password: testPassword,
  nickname: `E2E발언자${runId.slice(-4)}`,
}
const reporter = {
  email: `e2e-reporter-${runId}@example.com`,
  password: testPassword,
  nickname: `E2E신고자${runId.slice(-4)}`,
}

const speakerClient = new E2eApiClient()
const speakerUser = await signupOrLogin(speakerClient, speaker)
speaker.userId = speakerUser.userId

const reporterClient = new E2eApiClient()
const reporterUser = await signupOrLogin(reporterClient, reporter)
reporter.userId = reporterUser.userId

const topic = await admin.post("/api/v1/admin/topics", {
  title: `[E2E] 실시간 토론 검증 ${runId}`,
  description: "Playwright E2E 테스트용 토픽입니다.",
  category: "E2E",
  sourceUrl: "https://example.com/e2e",
})
const topicId = idOf(topic, "topicId", "id")

const room = await admin.post("/api/v1/admin/rooms", {
  topicId,
  title: `[E2E] API 연동 검증방 ${runId}`,
  maxParticipants: 20,
})
const roomId = idOf(room, "roomId", "id")

await speakerClient.post(`/api/v1/rooms/${roomId}/participants`)
await speakerClient.post(`/api/v1/rooms/${roomId}/stage/requests`, { stance: "PRO" })

const firstSpeechContent = `E2E 첫 의견 ${runId}`
const firstSpeech = await speakerClient.post(`/api/v1/rooms/${roomId}/speeches`, {
  content: firstSpeechContent,
  stance: "PRO",
})
const speechId = idOf(firstSpeech, "speechId", "id")

const secondSpeechContent = `E2E 이어지는 의견 ${runId}`
const secondSpeech = await speakerClient.post(`/api/v1/rooms/${roomId}/speeches`, {
  content: secondSpeechContent,
  stance: "PRO",
})
const secondSpeechId = idOf(secondSpeech, "speechId", "id")

await reporterClient.post(`/api/v1/rooms/${roomId}/participants`)
await reporterClient.post(`/api/v1/speeches/${speechId}/reactions`)
const report = await reporterClient.post(`/api/v1/speeches/${speechId}/reports`, {
  reason: "OFF_TOPIC",
  description: "E2E 신고 검증용 상세 설명입니다.",
})
const reportId = idOf(report, "reportId", "id")

const state = {
  runId,
  apiBaseUrl,
  frontendBaseUrl,
  admin: {
    email: adminEmail,
  },
  users: {
    speaker,
    reporter,
  },
  topic: {
    topicId,
    title: `[E2E] 실시간 토론 검증 ${runId}`,
  },
  room: {
    roomId,
    title: `[E2E] API 연동 검증방 ${runId}`,
  },
  speeches: {
    first: {
      speechId,
      content: firstSpeechContent,
    },
    second: {
      speechId: secondSpeechId,
      content: secondSpeechContent,
    },
  },
  report: {
    reportId,
    description: "E2E 신고 검증용 상세 설명입니다.",
  },
}

await mkdir(dirname(statePath), { recursive: true })
await writeFile(statePath, `${JSON.stringify(state, null, 2)}\n`)

console.log(`E2E test data prepared: ${statePath}`)
console.log(`roomId=${roomId}, speechId=${speechId}, reportId=${reportId}`)
