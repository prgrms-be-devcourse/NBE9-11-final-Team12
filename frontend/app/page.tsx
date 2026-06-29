"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { Navbar } from "@/components/navbar"
import { useAuth } from "@/components/auth-provider"
import {
  Zap,
  TrendingUp,
  Users,
  MessageSquare,
  ChevronRight,
  Sparkles,
  Globe,
  Shield,
  ArrowRight,
} from "lucide-react"

export default function HomePage() {
  const { user } = useAuth()

  const stats = [
    { label: "승인 이슈", value: "검토 완료", icon: MessageSquare, color: "text-primary" },
    { label: "실시간 토론", value: "참여 가능", icon: Zap, color: "text-emerald-600" },
    { label: "다양한 관점", value: "의견 공유", icon: Users, color: "text-accent" },
    { label: "토론 흐름", value: "발언권 운영", icon: TrendingUp, color: "text-rose-500" },
  ]

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <section className="relative overflow-hidden border-b border-border">
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.03] dark:opacity-[0.06]"
          style={{
            backgroundImage: `linear-gradient(var(--border) 1px, transparent 1px), linear-gradient(90deg, var(--border) 1px, transparent 1px)`,
            backgroundSize: "40px 40px",
          }}
        />
        <div className="pointer-events-none absolute left-1/2 top-0 h-[300px] w-[700px] -translate-x-1/2 rounded-full bg-primary/6 blur-3xl dark:bg-primary/10" />

        <div className="relative mx-auto max-w-7xl px-4 py-16 md:px-6 md:py-24">
          <div className="flex flex-col items-center gap-12 text-center lg:flex-row lg:gap-20 lg:text-left">
            <div className="flex flex-1 flex-col gap-5">
              <div className="flex justify-center lg:justify-start">
                <Badge
                  variant="outline"
                  className="-1.5 borgapder-primary/25 bg-primary/5 px-3 py-1 text-sm font-medium text-primary"
                >
                  <Sparkles className="size-3" />
                  지성인을 위한 오늘의 아고라: 팽팽한 논쟁 속, 당신의 날카로운 시선을 기다립니다.
                </Badge>
              </div>

              <h1 className="text-balance text-4xl font-bold leading-[1.15] tracking-tight text-foreground md:text-5xl lg:text-[3.5rem]">
                실시간 <span className="text-primary">광장형 </span>
                <br />
                토론 플랫폼
              </h1>

              {/* <p className="max-w-lg text-balance text-base leading-relaxed text-muted-foreground md:text-[17px]">
                관심 있는 이슈의 토론방에 참여해 채팅으로 반응하고, 발언권을 받아 자신의 의견을 남길 수 있습니다.
              </p> */}

              <div className="flex flex-wrap justify-center gap-3 lg:justify-start">
                <Link href="/rooms">
                  <Button size="lg" className="gap-2 font-semibold shadow-sm">
                    <Zap className="size-4" />
                    토론 시작하기
                  </Button>
                </Link>
                {!user && (
                  <Link href="/signup">
                    <Button variant="outline" size="lg" className="gap-2 font-semibold">
                      무료로 시작하기
                      <ChevronRight className="size-4" />
                    </Button>
                  </Link>
                )}
              </div>

              <p className="text-sm text-muted-foreground">
                {user
                  ? "토론방 목록에서 참여 가능한 방을 확인해보세요"
                  : "로그인 후 실제 토론방 참여가 가능합니다"}
              </p>
            </div>

            <div className="w-full max-w-sm shrink-0 lg:max-w-md">
              <div className="rounded-2xl border border-border bg-card p-5 shadow-card">
                <div className="mb-4 flex items-center justify-between">
                  <Badge variant="outline" className="border-primary/30 text-[11px] font-semibold text-primary">
                    ISSUE TALK
                  </Badge>
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Users className="size-3.5" />
                    <span>실시간 참여</span>
                  </div>
                </div>

                <h3 className="mb-1 text-balance text-[15px] font-semibold leading-snug text-foreground">
                  지금 열려 있는 토론방을 확인하세요
                </h3>
                <p className="mb-4 text-xs leading-relaxed text-muted-foreground">
                  토론방 목록에서 현재 참여 가능한 방, 참여자 수, 관련 이슈 정보를 확인할 수 있습니다.
                </p>

                <Separator className="mb-4" />

                <div className="flex items-center justify-between">
                  <span className="text-xs text-muted-foreground">목록에서 선택 후 입장</span>
                  <Link href="/rooms">
                    <Button size="sm" className="gap-1.5 text-xs font-semibold shadow-sm">
                      토론방 목록
                      <ArrowRight className="size-3.5" />
                    </Button>
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {user && (
        <section className="border-b border-border bg-card">
          <div className="mx-auto max-w-7xl px-4 py-4 md:px-6">
            <div className="grid grid-cols-2 gap-6 md:grid-cols-4">
              {stats.map((stat) => {
                const Icon = stat.icon
                return (
                  <div key={stat.label} className="flex items-center gap-3">
                    <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted">
                      <Icon className={`size-4 ${stat.color}`} />
                    </div>
                    <div>
                      <p className="text-[15px] font-bold leading-tight text-foreground">{stat.value}</p>
                      <p className="text-[11px] text-muted-foreground">{stat.label}</p>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        </section>
      )}

      <main className="mx-auto max-w-7xl px-4 py-10 md:px-6">
        <div className="mb-8 rounded-xl border border-border bg-card p-5 md:p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex-1">
              <div className="mb-2 flex items-center gap-2">
                <div className="flex size-6 items-center justify-center rounded-md bg-primary/10">
                  <Sparkles className="size-3.5 text-primary" />
                </div>
                <span className="text-xs font-semibold uppercase tracking-wider text-primary">Live Debate</span>
              </div>
              <h2 className="mb-1.5 text-[17px] font-bold text-foreground">
                실제 토론방 목록은 전용 화면에서 확인이 가능합니다.
              </h2>
              <p className="text-sm leading-relaxed text-muted-foreground">
                토론방 목록 화면에서 서버에 등록된 방과 참여자 수, 관련 이슈 정보를 확인하고 입장할 수 있습니다.
              </p>
            </div>
            <div className="flex shrink-0 flex-wrap gap-2">
              {[
                { icon: Globe, label: "이슈 기반 토론", color: "text-primary", bg: "bg-primary/8 dark:bg-primary/15" },
                { icon: Shield, label: "로그인 후 참여", color: "text-violet-600 dark:text-violet-400", bg: "bg-violet-50 dark:bg-violet-500/10" },
                { icon: Zap, label: "발언권 의견 작성", color: "text-amber-600 dark:text-amber-400", bg: "bg-amber-50 dark:bg-amber-500/10" },
              ].map(({ icon: Icon, label, color, bg }) => (
                <div
                  key={label}
                  className={`flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-medium text-foreground ${bg}`}
                >
                  <Icon className={`size-3.5 ${color}`} />
                  {label}
                </div>
              ))}
            </div>
          </div>
        </div>
      </main>

      <footer className="mt-16 border-t border-border">
        <div className="mx-auto max-w-7xl px-4 py-8 md:px-6">
          <div className="flex flex-col items-center justify-between gap-4 md:flex-row">
            <div className="flex items-center gap-2">
              <div className="flex size-6 items-center justify-center rounded-md bg-primary">
                <Zap className="size-3.5 text-primary-foreground" />
              </div>
              <span className="text-sm font-bold text-foreground">이슈톡</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
