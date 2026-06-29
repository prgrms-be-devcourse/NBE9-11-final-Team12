"use client"

import Link from "next/link"
import { useState } from "react"
import { authApi } from "@/lib/api/services"
import { ApiError } from "@/lib/api/client"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Progress } from "@/components/ui/progress"
import { Separator } from "@/components/ui/separator"
import { cn } from "@/lib/utils"
import {
  Zap,
  Mail,
  Lock,
  Eye,
  EyeOff,
  User,
  ArrowRight,
  CheckCircle2,
  Circle,
  Shield,
} from "lucide-react"

function getPasswordStrength(password: string): { score: number; label: string; color: string } {
  if (!password) return { score: 0, label: "", color: "" }
  let score = 0
  if (password.length >= 8) score += 25
  if (/[A-Z]/.test(password)) score += 25
  if (/[0-9]/.test(password)) score += 25
  if (/[^A-Za-z0-9]/.test(password)) score += 25

  if (score <= 25) return { score, label: "매우 약함", color: "bg-destructive" }
  if (score <= 50) return { score, label: "약함", color: "bg-amber-500" }
  if (score <= 75) return { score, label: "보통", color: "bg-yellow-400" }
  return { score, label: "강함", color: "bg-emerald-500" }
}

const steps = ["계정 정보", "프로필 설정", "완료"]

export default function SignupPage() {
  const [step, setStep] = useState(0)
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState("")
  const [form, setForm] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    nickname: "",
    agreeTerms: false,
    agreePrivacy: false,
    agreeMarketing: false,
  })

  const passwordStrength = getPasswordStrength(form.password)
  const passwordsMatch = form.password && form.confirmPassword && form.password === form.confirmPassword

  const handleChange = (field: string, value: string | boolean) => {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  const handleNext = async (e: React.FormEvent) => {
    e.preventDefault()
    if (step < steps.length - 2) {
      setStep((s) => s + 1)
    } else {
      setIsLoading(true)
      setError("")
      try {
        await authApi.signup({
          email: form.email,
          password: form.password,
          nickname: form.nickname,
        })
        setIsLoading(false)
        setStep(2)
      } catch (requestError) {
        setError(requestError instanceof ApiError ? requestError.message : "회원가입에 실패했습니다.")
        setIsLoading(false)
      }
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Background decorations */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute -left-60 top-20 size-[500px] rounded-full bg-primary/5 blur-3xl" />
        <div className="absolute -right-60 bottom-20 size-[400px] rounded-full bg-accent/5 blur-3xl" />
      </div>

      {/* Header */}
      <header className="relative z-10 flex h-16 items-center justify-between px-6">
        <Link href="/" className="flex items-center gap-2">
          <div className="flex size-8 items-center justify-center rounded-lg bg-primary">
            <Zap className="size-4 text-primary-foreground" />
          </div>
          <span className="font-bold text-foreground">이슈톡</span>
        </Link>
        <Link href="/login">
          <Button variant="ghost" size="sm" className="gap-1.5 text-muted-foreground text-xs">
            이미 계정이 있으신가요?
            <ArrowRight className="size-3.5" />
          </Button>
        </Link>
      </header>

      {/* Main content */}
      <main className="relative z-10 flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-md">
          {/* Card */}
          <div className="rounded-2xl border border-border/50 bg-card p-8 shadow-2xl">

            {step < 2 ? (
              <>
                {/* Title */}
                <div className="mb-6 text-center">
                  <h1 className="mb-1 text-2xl font-bold text-foreground">회원가입</h1>
                  <p className="text-sm text-muted-foreground">
                    이슈톡에 참여하세요
                  </p>
                </div>

                {/* Step indicators */}
                <div className="mb-8 flex items-center justify-center gap-3">
                  {steps.slice(0, 2).map((s, idx) => (
                    <div key={s} className="flex items-center gap-2">
                      <div
                        className={cn(
                          "flex size-7 items-center justify-center rounded-full text-xs font-bold transition-colors",
                          idx <= step
                            ? "bg-primary text-primary-foreground"
                            : "bg-muted text-muted-foreground"
                        )}
                      >
                        {idx < step ? <CheckCircle2 className="size-4" /> : idx + 1}
                      </div>
                      <span
                        className={cn(
                          "text-xs",
                          idx === step ? "font-medium text-foreground" : "text-muted-foreground"
                        )}
                      >
                        {s}
                      </span>
                      {idx < 1 && (
                        <div
                          className={cn(
                            "h-px w-8 transition-colors",
                            idx < step ? "bg-primary" : "bg-border"
                          )}
                        />
                      )}
                    </div>
                  ))}
                </div>

                <form onSubmit={handleNext} className="flex flex-col gap-4">
                  {step === 0 && (
                    <>
                      {/* Email */}
                      <div className="flex flex-col gap-1.5">
                        <label htmlFor="email" className="text-xs font-medium text-foreground">
                          이메일 <span className="text-destructive">*</span>
                        </label>
                        <div className="relative">
                          <Mail className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                          <Input
                            id="email"
                            type="email"
                            placeholder="name@example.com"
                            value={form.email}
                            onChange={(e) => handleChange("email", e.target.value)}
                            className="h-11 bg-muted border-border/50 pl-10 text-sm placeholder:text-muted-foreground/60"
                            required
                          />
                        </div>
                      </div>

                      {/* Password */}
                      <div className="flex flex-col gap-1.5">
                        <label htmlFor="password" className="text-xs font-medium text-foreground">
                          비밀번호 <span className="text-destructive">*</span>
                        </label>
                        <div className="relative">
                          <Lock className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                          <Input
                            id="password"
                            type={showPassword ? "text" : "password"}
                            placeholder="8자 이상, 영문+숫자+특수문자"
                            value={form.password}
                            onChange={(e) => handleChange("password", e.target.value)}
                            className="h-11 bg-muted border-border/50 pl-10 pr-10 text-sm placeholder:text-muted-foreground/60"
                            required
                          />
                          <button
                            type="button"
                            onClick={() => setShowPassword(!showPassword)}
                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                            aria-label={showPassword ? "비밀번호 숨기기" : "비밀번호 보기"}
                          >
                            {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                          </button>
                        </div>
                        {form.password && (
                          <div className="flex flex-col gap-1">
                            <Progress
                              value={passwordStrength.score}
                              className="h-1"
                            />
                            <span className={cn(
                              "text-[11px]",
                              passwordStrength.score === 100 ? "text-emerald-700 dark:text-emerald-400" :
                              passwordStrength.score >= 75 ? "text-yellow-600 dark:text-yellow-400" :
                              passwordStrength.score >= 50 ? "text-amber-600 dark:text-amber-400" : "text-destructive"
                            )}>
                              비밀번호 강도: {passwordStrength.label}
                            </span>
                          </div>
                        )}
                      </div>

                      {/* Confirm password */}
                      <div className="flex flex-col gap-1.5">
                        <label htmlFor="confirmPassword" className="text-xs font-medium text-foreground">
                          비밀번호 확인 <span className="text-destructive">*</span>
                        </label>
                        <div className="relative">
                          <Lock className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                          <Input
                            id="confirmPassword"
                            type="password"
                            placeholder="비밀번호 재입력"
                            value={form.confirmPassword}
                            onChange={(e) => handleChange("confirmPassword", e.target.value)}
                            className={cn(
                              "h-11 bg-muted border-border/50 pl-10 text-sm placeholder:text-muted-foreground/60",
                              form.confirmPassword && !passwordsMatch && "border-destructive/50",
                              passwordsMatch && "border-emerald-500/50"
                            )}
                            required
                          />
                          {form.confirmPassword && (
                            <div className="absolute right-3 top-1/2 -translate-y-1/2">
                              {passwordsMatch ? (
                                <CheckCircle2 className="size-4 text-emerald-600 dark:text-emerald-400" />
                              ) : (
                                <Circle className="size-4 text-muted-foreground" />
                              )}
                            </div>
                          )}
                        </div>
                        {form.confirmPassword && !passwordsMatch && (
                          <p className="text-[11px] text-destructive">비밀번호가 일치하지 않습니다.</p>
                        )}
                      </div>
                    </>
                  )}

                  {step === 1 && (
                    <>
                      {/* Nickname */}
                      <div className="flex flex-col gap-1.5">
                        <label htmlFor="nickname" className="text-xs font-medium text-foreground">
                          닉네임 <span className="text-destructive">*</span>
                        </label>
                        <div className="relative">
                          <User className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                          <Input
                            id="nickname"
                            type="text"
                            placeholder="@닉네임 (2~20자)"
                            value={form.nickname}
                            onChange={(e) => handleChange("nickname", e.target.value)}
                            className="h-11 bg-muted border-border/50 pl-10 text-sm placeholder:text-muted-foreground/60"
                            minLength={2}
                            maxLength={20}
                            required
                          />
                        </div>
                        <p className="text-[11px] text-muted-foreground">
                          토론방에서 표시될 닉네임입니다. 언제든 변경 가능합니다.
                        </p>
                      </div>

                      {/* Terms */}
                      <div className="rounded-xl border border-border/50 bg-muted/30 p-4">
                        <div className="mb-3 flex items-center gap-2">
                          <Shield className="size-4 text-primary" />
                          <span className="text-xs font-semibold text-foreground">약관 동의</span>
                        </div>
                        <div className="flex flex-col gap-3">
                          {[
                            { key: "agreeTerms", label: "이용약관 동의", required: true },
                            { key: "agreePrivacy", label: "개인정보처리방침 동의", required: true },
                            { key: "agreeMarketing", label: "마케팅 정보 수신 동의 (선택)", required: false },
                          ].map(({ key, label, required }) => (
                            <label key={key} className="flex items-center gap-2.5 cursor-pointer">
                              <input
                                type="checkbox"
                                checked={Boolean(form[key as keyof typeof form])}
                                onChange={(e) => handleChange(key, e.target.checked)}
                                className="size-4 rounded accent-primary"
                                required={required}
                              />
                              <span className="text-xs text-muted-foreground">
                                {label}
                                {required && <span className="ml-0.5 text-destructive">*</span>}
                              </span>
                            </label>
                          ))}
                        </div>
                      </div>
                    </>
                  )}

                  {/* Submit / Next */}
                  <Button
                    type="submit"
                    size="lg"
                    className="mt-2 h-11 w-full gap-2 text-sm font-semibold"
                    disabled={isLoading || (step === 0 && !!form.confirmPassword && !passwordsMatch)}
                  >
                    {isLoading ? (
                      <span className="flex items-center gap-2">
                        <span className="size-4 animate-spin rounded-full border-2 border-primary-foreground/30 border-t-primary-foreground" />
                        처리 중...
                      </span>
                    ) : step === 0 ? (
                      <>
                        다음
                        <ArrowRight className="size-4" />
                      </>
                    ) : (
                      <>
                        가입 완료
                        <CheckCircle2 className="size-4" />
                      </>
                    )}
                  </Button>
                </form>
                {error && <p className="mt-3 text-center text-xs text-destructive">{error}</p>}

                {step === 0 && (
                  <>
                    <Separator className="my-5" />
                    <p className="text-center text-xs text-muted-foreground">
                      이미 계정이 있으신가요?{" "}
                      <Link href="/login" className="font-medium text-primary hover:underline">
                        로그인
                      </Link>
                    </p>
                  </>
                )}
              </>
            ) : (
              /* Success step */
              <div className="flex flex-col items-center gap-5 py-6 text-center">
                <div className="flex size-20 items-center justify-center rounded-full bg-emerald-50 dark:bg-emerald-500/10 ring-2 ring-emerald-200 dark:ring-emerald-500/30">
                  <CheckCircle2 className="size-10 text-emerald-600 dark:text-emerald-400" />
                </div>
                <div>
                  <h2 className="mb-1 text-2xl font-bold text-foreground">가입 완료!</h2>
                  <p className="text-sm text-muted-foreground">
                    이슈톡에 오신 것을 환영합니다.
                  </p>
                </div>
                <div className="w-full rounded-xl border border-primary/20 bg-primary/5 px-4 py-3 text-left">
                  <p className="text-xs font-medium text-primary mb-1">시작하기 전 알아두세요</p>
                  <ul className="flex flex-col gap-1 text-xs text-muted-foreground">
                    <li className="flex items-center gap-1.5">
                      <span className="size-1 rounded-full bg-primary flex-shrink-0" />
                      발언권을 신청해 실시간 토론에 참여하세요
                    </li>
                    <li className="flex items-center gap-1.5">
                      <span className="size-1 rounded-full bg-primary flex-shrink-0" />
                      커뮤니티 규칙을 준수해 주세요
                    </li>
                  </ul>
                </div>
                <Link href="/rooms" className="w-full">
                  <Button size="lg" className="w-full gap-2 text-sm font-semibold">
                    <Zap className="size-4" />
                    토론 시작하기
                  </Button>
                </Link>
                <Link href="/" className="text-xs text-muted-foreground hover:text-foreground transition-colors">
                  홈으로 돌아가기
                </Link>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  )
}
