"use client"

import Link from "next/link"
import { Suspense, useState } from "react"
import { useRouter, useSearchParams } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
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
          <span className="text-[15px] font-bold text-foreground">시시비비</span>
          <Badge variant="outline" className="hidden border-primary/30 text-primary text-[10px] font-semibold sm:flex">
            ARENA TALK
          </Badge>
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
              <p className="text-sm text-muted-foreground">시시비비 계정으로 로그인하세요</p>
            </div>

            {/* Live indicator */}
            <div className="mb-6 flex items-center justify-center gap-2 rounded-xl border border-border bg-muted/50 px-4 py-2.5">
              <span className="size-1.5 rounded-full bg-rose-500 animate-live-pulse" />
              <span className="text-xs text-muted-foreground">
                로그인 후 실제 토의방 현황을 확인할 수 있어요
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

            <div className="my-6 flex items-center gap-3">
              <Separator className="flex-1" />
              <span className="text-[11px] text-muted-foreground">또는</span>
              <Separator className="flex-1" />
            </div>

            {/* Social login */}
            <div className="flex flex-col gap-2.5">
              {/* Kakao */}
              <button
                type="button"
                className="flex h-10 w-full items-center justify-center gap-2 rounded-md border border-[#FEE500] bg-[#FEE500] text-[13px] font-medium text-[#3C1E1E] transition-colors hover:bg-[#FDD835] hover:border-[#FDD835] dark:border-[#FEE500]/80 dark:bg-[#FEE500]/90 dark:text-[#3C1E1E] dark:hover:bg-[#FEE500]"
              >
                {/* Kakao bubble icon */}
                <svg width="18" height="18" viewBox="0 0 18 18" fill="none" aria-hidden="true">
                  <path
                    fillRule="evenodd"
                    clipRule="evenodd"
                    d="M9 1.5C4.858 1.5 1.5 4.187 1.5 7.5c0 2.108 1.282 3.963 3.213 5.085l-.82 3.02a.281.281 0 0 0 .432.305l3.517-2.32A9.9 9.9 0 0 0 9 13.5c4.142 0 7.5-2.687 7.5-6s-3.358-6-7.5-6Z"
                    fill="#3C1E1E"
                  />
                </svg>
                카카오로 계속하기
              </button>

              {/* Google */}
              <button
                type="button"
                className="flex h-10 w-full items-center justify-center gap-2 rounded-md border border-border bg-card text-[13px] font-medium text-foreground transition-colors hover:bg-muted"
              >
                {/* Google "G" icon */}
                <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
                  <path d="M17.64 9.205c0-.639-.057-1.252-.164-1.841H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615Z" fill="#4285F4"/>
                  <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18Z" fill="#34A853"/>
                  <path d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332Z" fill="#FBBC05"/>
                  <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58Z" fill="#EA4335"/>
                </svg>
                구글로 계속하기
              </button>
            </div>

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
