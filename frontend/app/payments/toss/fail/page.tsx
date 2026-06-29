"use client"

import { Suspense } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

function TossPaymentFailContent() {
  const router = useRouter()
  const searchParams = useSearchParams()

  const roomId = searchParams.get("roomId")
  const message = searchParams.get("message") || "결제가 취소되었거나 실패했습니다."
  const code = searchParams.get("code")

  return (
    <main className="mx-auto flex min-h-screen max-w-md items-center px-4">
      <Card className="w-full">
        <CardHeader>
          <CardTitle>결제 실패</CardTitle>
          <CardDescription>{message}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {code && (
            <p className="rounded-md bg-muted px-3 py-2 text-xs text-muted-foreground">
              오류 코드: {code}
            </p>
          )}
          <Button className="w-full" onClick={() => router.replace(roomId ? `/rooms/${roomId}` : "/rooms")}>
            토론방으로 돌아가기
          </Button>
        </CardContent>
      </Card>
    </main>
  )
}

export default function TossPaymentFailPage() {
  return (
    <Suspense>
      <TossPaymentFailContent />
    </Suspense>
  )
}
