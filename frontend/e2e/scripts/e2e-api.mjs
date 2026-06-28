const DEFAULT_API_BASE_URL = "http://localhost:8080"

export const apiBaseUrl = process.env.E2E_API_BASE_URL ?? DEFAULT_API_BASE_URL

/**
 * @typedef {Object} E2eRequestOptions
 * @property {string} [method]
 * @property {unknown} [body]
 * @property {number[]} [allowStatuses]
 */

export class E2eApiError extends Error {
  constructor(message, status, code, data) {
    super(message)
    this.status = status
    this.code = code
    this.data = data
  }
}

export class E2eApiClient {
  constructor(baseUrl = apiBaseUrl) {
    this.baseUrl = baseUrl
    this.cookies = new Map()
  }

  cookieHeader() {
    return [...this.cookies.entries()]
      .map(([name, value]) => `${name}=${value}`)
      .join("; ")
  }

  hasCookie(name) {
    return this.cookies.has(name)
  }

  deleteCookie(name) {
    this.cookies.delete(name)
  }

  saveCookies(response) {
    const setCookie = response.headers.getSetCookie?.() ?? []
    for (const cookie of setCookie) {
      const [pair] = cookie.split(";")
      const separatorIndex = pair.indexOf("=")
      if (separatorIndex < 0) continue
      const name = pair.slice(0, separatorIndex)
      const value = pair.slice(separatorIndex + 1)
      if (value) {
        this.cookies.set(name, value)
      } else {
        this.cookies.delete(name)
      }
    }
  }

  async csrfToken() {
    const response = await fetch(`${this.baseUrl}/api/v1/csrf`, {
      headers: this.cookieHeader() ? { Cookie: this.cookieHeader() } : undefined,
    })
    this.saveCookies(response)
    const payload = await response.json()
    if (!response.ok || !payload.data) {
      throw new E2eApiError(payload.message ?? "CSRF 토큰 발급 실패", response.status, payload.code, payload.data)
    }
    return payload.data
  }

  /**
   * @param {string} path
   * @param {E2eRequestOptions} [options]
   */
  async requestRaw(path, { method = "GET", body, allowStatuses = [] } = {}) {
    const headers = new Headers()
    if (body !== undefined) headers.set("Content-Type", "application/json")
    if (this.cookieHeader()) headers.set("Cookie", this.cookieHeader())

    if (!["GET", "HEAD", "OPTIONS"].includes(method) && !path.startsWith("/api/v1/auth/")) {
      headers.set("X-XSRF-TOKEN", await this.csrfToken())
      if (this.cookieHeader()) headers.set("Cookie", this.cookieHeader())
    }

    const response = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    this.saveCookies(response)

    const text = await response.text()
    const payload = text ? JSON.parse(text) : {}
    if (!response.ok && !allowStatuses.includes(response.status)) {
      throw new E2eApiError(payload.message ?? "API 요청 실패", response.status, payload.code, payload.data)
    }
    return {
      status: response.status,
      ok: response.ok,
      code: payload.code,
      message: payload.message,
      data: payload.data,
    }
  }

  /**
   * @param {string} path
   * @param {E2eRequestOptions} [options]
   */
  async request(path, options) {
    const response = await this.requestRaw(path, options)
    return response.data
  }

  get(path, options) {
    return this.request(path, { ...options, method: "GET" })
  }

  post(path, body, options) {
    return this.request(path, { ...options, method: "POST", body })
  }

  patch(path, body, options) {
    return this.request(path, { ...options, method: "PATCH", body })
  }

  delete(path, options) {
    return this.request(path, { ...options, method: "DELETE" })
  }
}

export async function login(client, email, password) {
  return client.post("/api/v1/auth/login", { email, password })
}

export async function signupOrLogin(client, { email, password, nickname }) {
  try {
    await client.post("/api/v1/auth/signup", { email, password, nickname })
    return login(client, email, password)
  } catch (error) {
    if (error instanceof E2eApiError && error.status === 409) {
      return login(client, email, password)
    }
    throw error
  }
}
