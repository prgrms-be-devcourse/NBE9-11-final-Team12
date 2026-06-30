"use client"

import Link from "next/link"
import { Suspense, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { authApi } from "@/lib/api/services"
import { ApiError } from "@/lib/api/client"
import { useAuth } from "@/components/auth-provider"
import { Zap, Mail, Lock, Eye, EyeOff, ArrowRight, Radio } from "lucide-react"

function LoginContent() {
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading,    setIsLoading]    = useState(false)
  const [email,        setEmail]        = useState("")
  const [password,     setPassword]     = useState("")
  const [error, setError] = useState("")
  const router = useRouter()
  const searchParams = useSearchParams()
  const { refresh } = useAuth()
  const redirect = searchParams.get("redirect")
  const redirectPath = redirect?.startsWith("/") && !redirect.startsWith("//") ? redirect : "/"

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    setError("")
    try {
      await authApi.login({ email, password })
      await refresh()
      router.replace(redirectPath)
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : "로그인에 실패했습니다.")
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Subtle background */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute left-1/2 top-0 -translate-x-1/2 h-[400px] w-[800px] rounded-full bg-primary/5 dark:bg-primary/8 blur-3xl" />
      </div>

      {/* Header */}
      <header className="relative z-10 flex h-14 items-center justify-between border-b border-border bg-background/80 backdrop-blur px-6">
        <Link href="/" className="flex items-center gap-2">
          <div className="flex size-7 items-center justify-center rounded-lg bg-primary">
            <Zap className="size-3.5 text-primary-foreground" />
          </div>
          <span className="text-[15px] font-bold text-foreground">이슈톡</span>
        </Link>
        <Link href="/signup">
          <Button variant="ghost" size="sm" className="gap-1.5 text-xs text-muted-foreground">
            아직 계정이 없으신가요?
            <ArrowRight className="size-3.5" />
          </Button>
        </Link>
      </header>

      {/* Main */}
      <main className="relative z-10 flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-[400px]">

          {/* Card */}
          <div className="rounded-2xl border border-border bg-card p-8 shadow-card">
            {/* Icon + title */}
            <div className="mb-7 text-center">
              <div className="mb-4 flex justify-center">
                <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/8 dark:bg-primary/15 ring-1 ring-primary/20">
                  <Zap className="size-6 text-primary" />
                </div>
              </div>
              <h1 className="mb-1 text-[22px] font-bold tracking-tight text-foreground">다시 만나요</h1>
              <p className="text-sm text-muted-foreground">이슈톡 계정으로 로그인하세요</p>
            </div>

            {/* Live indicator */}
            <div className="mb-6 flex items-center justify-center gap-2 rounded-xl border border-border bg-muted/50 px-4 py-2.5">
              <span className="size-1.5 rounded-full bg-rose-500 animate-live-pulse" />
              <span className="text-xs text-muted-foreground">
                로그인 후 실제 토론방에 참여할 수 있어요
              </span>
              <Radio className="size-3.5 text-muted-foreground" />
            </div>

            {/* Form */}
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              {/* Email */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="email" className="text-[13px] font-medium text-foreground">
                  이메일
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="email"
                    type="email"
                    placeholder="name@example.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="h-10 pl-9 text-sm"
                    required
                  />
                </div>
              </div>

              {/* Password */}
              <div className="flex flex-col gap-1.5">
                <div className="flex items-center justify-between">
                  <label htmlFor="password" className="text-[13px] font-medium text-foreground">
                    비밀번호
                  </label>
                  <Link href="/forgot-password" className="text-[12px] text-muted-foreground hover:text-primary transition-colors">
                    비밀번호 찾기
                  </Link>
                </div>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="h-10 pl-9 pr-10 text-sm"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                    aria-label={showPassword ? "비밀번호 숨기기" : "비밀번호 보기"}
                  >
                    {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                  </button>
                </div>
              </div>

              {/* Submit */}
              <Button
                type="submit"
                size="lg"
                className="mt-1 h-10 w-full gap-2 text-sm font-semibold shadow-sm"
                disabled={isLoading}
              >
                {isLoading ? (
                  <span className="flex items-center gap-2">
                    <span className="size-4 animate-spin rounded-full border-2 border-primary-foreground/30 border-t-primary-foreground" />
                    로그인 중...
                  </span>
                ) : (
                  <>
                    로그인
                    <ArrowRight className="size-4" />
                  </>
                )}
              </Button>
            </form>
            {error && <p className="mt-3 text-center text-xs text-destructive">{error}</p>}

            {/* Sign up link */}
            <p className="mt-6 text-center text-[13px] text-muted-foreground">
              계정이 없으신가요?{" "}
              <Link href="/signup" className="font-semibold text-primary hover:underline">
                회원가입
              </Link>
            </p>
          </div>

          {/* Terms */}
          <p className="mt-4 text-center text-[11px] text-muted-foreground">
            로그인 시{" "}
            <Link href="#" className="underline underline-offset-2 hover:text-foreground">이용약관</Link>
            {" "}및{" "}
            <Link href="#" className="underline underline-offset-2 hover:text-foreground">개인정보처리방침</Link>
            에 동의하게 됩니다.
          </p>
        </div>
      </main>
    </div>
  )
}

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginContent />
    </Suspense>
  )
}
