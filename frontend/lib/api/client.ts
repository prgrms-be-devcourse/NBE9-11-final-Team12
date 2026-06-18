import type { ApiResponse } from "@/lib/api/types"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

async function issueCsrfToken() {
  const response = await fetch(`${API_BASE_URL}/api/v1/csrf`, {
    credentials: "include",
  })
  const payload = (await response.json()) as ApiResponse<string>

  if (!response.ok || !payload.data) {
    throw new ApiError(payload.message || "CSRF 토큰 발급에 실패했습니다.", response.status, payload.code)
  }

  return payload.data
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code: string,
    readonly data?: Record<string, string>,
  ) {
    super(message)
  }
}

async function readPayload<T>(response: Response): Promise<ApiResponse<T>> {
  const text = await response.text()
  if (!text) {
    return { status: response.status, code: response.ok ? "OK" : "ERROR", message: response.statusText }
  }
  return JSON.parse(text) as ApiResponse<T>
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json")
  }

  const method = init.method ?? "GET"
  const isMutation = !["GET", "HEAD", "OPTIONS"].includes(method)
  if (isMutation && !path.startsWith("/api/v1/auth/")) {
    headers.set("X-XSRF-TOKEN", await issueCsrfToken())
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: "include",
  })

  if (response.status === 401 && retry && path !== "/api/v1/auth/reissue") {
    const reissueResponse = await fetch(`${API_BASE_URL}/api/v1/auth/reissue`, {
      method: "POST",
      credentials: "include",
    })
    if (reissueResponse.ok) return request<T>(path, init, false)
  }

  const payload = await readPayload<T>(response)
  if (!response.ok) {
    throw new ApiError(
      payload.message || "요청 처리에 실패했습니다.",
      response.status,
      payload.code,
      payload.data as Record<string, string> | undefined,
    )
  }

  return payload.data as T
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "POST", body: body === undefined ? undefined : JSON.stringify(body) }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "PATCH", body: body === undefined ? undefined : JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
}
