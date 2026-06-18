"use client"

import Link from "next/link"
import { useState } from "react"
import { useRouter } from "next/navigation"
import { ArrowRight, CheckCircle2, Eye, EyeOff, Lock, Mail, User, Zap, type LucideIcon } from "lucide-react"
import { ApiError } from "@/lib/api/client"
import { authApi } from "@/lib/api/services"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

export default function SignupPage() {
  const router = useRouter()
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState("")
  const [form, setForm] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    nickname: "",
  })

  const passwordsMatch = form.password.length > 0 && form.password === form.confirmPassword

  const handleChange = (field: keyof typeof form, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!passwordsMatch) {
      setError("비밀번호가 일치하지 않습니다.")
      return
    }

    setIsLoading(true)
    setError("")
    try {
      await authApi.signup({
        email: form.email,
        password: form.password,
        nickname: form.nickname,
      })
      router.replace("/login")
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : "회원가입에 실패했습니다.")
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
        <Link href="/login">
          <Button variant="ghost" size="sm" className="gap-1.5 text-xs text-muted-foreground">
            로그인
            <ArrowRight className="size-3.5" />
          </Button>
        </Link>
      </header>

      <main className="flex flex-1 items-center justify-center px-4 py-12">
        <div className="w-full max-w-md rounded-2xl border border-border bg-card p-8 shadow-card">
          <div className="mb-7 text-center">
            <div className="mb-4 flex justify-center">
              <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10 ring-1 ring-primary/20">
                <CheckCircle2 className="size-6 text-primary" />
              </div>
            </div>
            <h1 className="mb-1 text-2xl font-bold text-foreground">회원가입</h1>
            <p className="text-sm text-muted-foreground">토론에 사용할 계정을 만듭니다.</p>
          </div>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <Field label="이메일" icon={Mail}>
              <Input type="email" value={form.email} onChange={(event) => handleChange("email", event.target.value)} className="h-10 pl-9 text-sm" required />
            </Field>

            <div className="flex flex-col gap-1.5">
              <label htmlFor="password" className="text-[13px] font-medium text-foreground">
                비밀번호
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input id="password" type={showPassword ? "text" : "password"} value={form.password} onChange={(event) => handleChange("password", event.target.value)} className="h-10 pl-9 pr-10 text-sm" minLength={8} required />
                <button type="button" onClick={() => setShowPassword((open) => !open)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground" aria-label="비밀번호 보기">
                  {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                </button>
              </div>
            </div>

            <Field label="비밀번호 확인" icon={Lock}>
              <Input type="password" value={form.confirmPassword} onChange={(event) => handleChange("confirmPassword", event.target.value)} className="h-10 pl-9 text-sm" required />
            </Field>

            <Field label="닉네임" icon={User}>
              <Input value={form.nickname} onChange={(event) => handleChange("nickname", event.target.value)} className="h-10 pl-9 text-sm" minLength={2} maxLength={20} required />
            </Field>

            {form.confirmPassword && !passwordsMatch && <p className="text-xs text-destructive">비밀번호가 일치하지 않습니다.</p>}

            <Button type="submit" size="lg" className="mt-1 h-10 w-full gap-2 text-sm font-semibold" disabled={isLoading || !passwordsMatch}>
              {isLoading ? "가입 중..." : "회원가입"}
            </Button>
          </form>

          {error && <p className="mt-3 text-center text-xs text-destructive">{error}</p>}
        </div>
      </main>
    </div>
  )
}

function Field({ label, icon: Icon, children }: { label: string; icon: LucideIcon; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-[13px] font-medium text-foreground">{label}</label>
      <div className="relative">
        <Icon className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        {children}
      </div>
    </div>
  )
}
