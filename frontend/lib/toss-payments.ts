type TossPaymentMethod = "카드" | "가상계좌" | "계좌이체" | "휴대폰" | "문화상품권" | "도서문화상품권" | "게임문화상품권"

type TossPaymentRequest = {
  amount: number
  orderId: string
  orderName: string
  successUrl: string
  failUrl: string
}

type TossPaymentsInstance = {
  requestPayment: (method: TossPaymentMethod, request: TossPaymentRequest) => Promise<void>
}

declare global {
  interface Window {
    TossPayments?: (clientKey: string) => TossPaymentsInstance
  }
}

const TOSS_SCRIPT_ID = "toss-payments-sdk"
const TOSS_SCRIPT_SRC = "https://js.tosspayments.com/v1/payment"

function loadTossPaymentsScript() {
  if (typeof window === "undefined") {
    return Promise.reject(new Error("Toss Payments can only run in the browser."))
  }

  if (window.TossPayments) {
    return Promise.resolve()
  }

  const existingScript = document.getElementById(TOSS_SCRIPT_ID) as HTMLScriptElement | null
  if (existingScript) {
    return new Promise<void>((resolve, reject) => {
      existingScript.addEventListener("load", () => resolve(), { once: true })
      existingScript.addEventListener("error", () => reject(new Error("Failed to load Toss Payments SDK.")), { once: true })
    })
  }

  return new Promise<void>((resolve, reject) => {
    const script = document.createElement("script")
    script.id = TOSS_SCRIPT_ID
    script.src = TOSS_SCRIPT_SRC
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error("Failed to load Toss Payments SDK."))
    document.head.appendChild(script)
  })
}

export async function requestTossCardPayment(request: TossPaymentRequest) {
  const clientKey = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY
  if (!clientKey) {
    throw new Error("NEXT_PUBLIC_TOSS_CLIENT_KEY is missing.")
  }

  await loadTossPaymentsScript()

  if (!window.TossPayments) {
    throw new Error("Toss Payments SDK is unavailable.")
  }

  await window.TossPayments(clientKey).requestPayment("카드", request)
}
