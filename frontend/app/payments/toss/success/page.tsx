"use client"

import { Suspense, useEffect, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Loader2 } from "lucide-react"
import { ApiError } from "@/lib/api/client"
import { paymentApi } from "@/lib/api/services"
import { getPaymentSuccessRedirectPath } from "@/lib/payment-success-flow"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

function messageOf(error: unknown) {
  if (error instanceof ApiError) {
    return error.message || "결제 승인 처리에 실패했습니다."
  }

  if (error instanceof Error) {
    return error.message
  }

  return "결제 승인 처리에 실패했습니다."
}

function TossPaymentSuccessContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [error, setError] = useState("")
  const [completed, setCompleted] = useState(false)

  const roomId = searchParams.get("roomId")

  useEffect(() => {
    const paymentKey = searchParams.get("paymentKey")
    const orderId = searchParams.get("orderId")
    const amount = Number(searchParams.get("amount"))

    if (!paymentKey || !orderId || !Number.isFinite(amount) || amount <= 0) {
      setError("결제 승인 정보가 올바르지 않습니다.")
      return
    }

    let canceled = false

    async function confirmPayment() {
      try {
        const payment = await paymentApi.confirm({ paymentKey, orderId, amount })
        if (canceled) return

        const redirectPath = getPaymentSuccessRedirectPath(payment, roomId)
        if (redirectPath) {
          router.replace(redirectPath)
          return
        }

        setCompleted(true)
      } catch (confirmError) {
        if (canceled) return
        setError(messageOf(confirmError))
      }
    }

    void confirmPayment()

    return () => {
      canceled = true
    }
  }, [roomId, router, searchParams])

  if (error) {
    return (
      <main className="mx-auto flex min-h-screen max-w-md items-center px-4">
        <Card className="w-full">
          <CardHeader>
            <CardTitle>결제 승인 실패</CardTitle>
            <CardDescription>{error}</CardDescription>
          </CardHeader>
          <CardContent>
            <Button className="w-full" onClick={() => router.replace(roomId ? `/rooms/${roomId}` : "/rooms")}>
              토론방으로 돌아가기
            </Button>
          </CardContent>
        </Card>
      </main>
    )
  }

  if (completed) {
    return (
      <main className="mx-auto flex min-h-screen max-w-md items-center px-4">
        <Card className="w-full">
          <CardHeader>
            <CardTitle>결제가 완료되었습니다</CardTitle>
            <CardDescription>
              커스텀 AI 리포트 생성 요청을 접수했습니다. 리포트가 준비되면 PDF 다운로드를 진행할 수 있습니다.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button className="w-full" onClick={() => router.replace("/rooms")}>
              토론방 목록으로 돌아가기
            </Button>
          </CardContent>
        </Card>
      </main>
    )
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md items-center px-4">
      <Card className="w-full">
        <CardHeader>
          <CardTitle>결제 승인 중</CardTitle>
          <CardDescription>커스텀 AI 리포트 생성 요청을 등록하고 있습니다.</CardDescription>
        </CardHeader>
        <CardContent className="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          잠시만 기다려 주세요.
        </CardContent>
      </Card>
    </main>
  )
}

export default function TossPaymentSuccessPage() {
  return (
    <Suspense>
      <TossPaymentSuccessContent />
    </Suspense>
  )
}
