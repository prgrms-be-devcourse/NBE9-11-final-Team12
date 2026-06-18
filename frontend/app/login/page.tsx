"use client"

import Link from "next/link"
import { useState } from "react"
import { useRouter } from "next/navigation"
import { ArrowRight, Eye, EyeOff, Lock, Mail, Zap } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { ApiError } from "@/lib/api/client"
import { authApi } from "@/lib/api/services"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

export default function LoginPage() {
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const router = useRouter()
  const { refresh } = useAuth()

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    setIsLoading(true)
    setError("")
    try {
      await authApi.login({ email, password })
      await refresh()
      router.replace("/")
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : "로그인에 실패했습니다.")
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="flex h-14 items-center justify-between border-b border-border bg-background/80 px-6 backdrop-blur">
        <Link href="/" className="flex items-center gap-2">
          <div className="flex size-7 items-center justify-center rounded-lg bg-primary">
            <Zap className="size-3.5 text-primary-foreground" />
          </div>
          <span className="text-[15px] font-bold text-foreground">시시비비</span>
          <Badge variant="outline" className="hidden border-primary/30 text-primary text-[10px] font-semibold sm:flex">
            ARENA TALK
          </Badge>
        </Link>
        <Link href="/signup">
          <Button variant="ghost" size="sm" className="gap-1.5 text-xs text-muted-foreground">
            회원가입
            <ArrowRight className="size-3.5" />
          </Button>
        </Link>
      </header>

      <main className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-[400px] rounded-2xl border border-border bg-card p-8 shadow-card">
          <div className="mb-7 text-center">
            <div className="mb-4 flex justify-center">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10 ring-1 ring-primary/20">
                <Zap className="size-6 text-primary" />
              </div>
            </div>
            <h1 className="mb-1 text-[22px] font-bold tracking-tight text-foreground">로그인</h1>
            <p className="text-sm text-muted-foreground">계정으로 토론에 참여하세요.</p>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <label htmlFor="email" className="text-[13px] font-medium text-foreground">
                이메일
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input id="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} className="h-10 pl-9 text-sm" required />
              </div>
            </div>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="password" className="text-[13px] font-medium text-foreground">
                비밀번호
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input id="password" type={showPassword ? "text" : "password"} value={password} onChange={(event) => setPassword(event.target.value)} className="h-10 pl-9 pr-10 text-sm" required />
                <button type="button" onClick={() => setShowPassword((open) => !open)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground" aria-label="비밀번호 보기">
                  {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                </button>
              </div>
            </div>

            <Button type="submit" size="lg" className="mt-1 h-10 w-full gap-2 text-sm font-semibold" disabled={isLoading}>
              {isLoading ? "로그인 중..." : "로그인"}
            </Button>
          </form>

          {error && <p className="mt-3 text-center text-xs text-destructive">{error}</p>}

          <p className="mt-6 text-center text-[13px] text-muted-foreground">
            계정이 없나요?{" "}
            <Link href="/signup" className="font-semibold text-primary hover:underline">
              회원가입
            </Link>
          </p>
        </div>
      </main>
    </div>
  )
}
