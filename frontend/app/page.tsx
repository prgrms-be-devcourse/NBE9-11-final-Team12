"use client"

import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { Navbar } from "@/components/navbar"
import { TopicCard, type Topic } from "@/components/topic-card"
import { useAuth } from "@/components/auth-provider"
import { ApiError } from "@/lib/api/client"
import { roomApi, topicApi } from "@/lib/api/services"
import type { RoomSummary, TopicDetail } from "@/lib/api/types"
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

function messageOf(error: unknown) {
  return error instanceof ApiError ? error.message : "토의방 정보를 불러오지 못했습니다."
}

function toTopicCard(room: RoomSummary, detail: TopicDetail | null, participantCount: number): Topic {
  const category = detail?.category ?? "토론"
  return {
    id: String(room.roomId),
    title: room.title,
    description: detail?.description ?? "승인된 토픽으로 개설된 실시간 토론방입니다.",
    category,
    status: room.status,
    participants: participantCount,
    timeLeft: room.status === "OPEN" ? "진행 중" : undefined,
    tags: [category],
    isLive: room.status === "OPEN",
  }
}

export default function HomePage() {
  const { user } = useAuth()
  const [topics, setTopics] = useState<Topic[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  useEffect(() => {
    let mounted = true

    setLoading(true)
    setError("")

    async function loadRooms() {
      try {
        const rooms = await roomApi.list()
        const cards = await Promise.all(
          rooms.map(async (room) => {
            const [topicResult, countResult] = await Promise.allSettled([
              topicApi.detail(room.topicId),
              roomApi.participantCount(room.roomId),
            ])
            const detail = topicResult.status === "fulfilled" ? topicResult.value : null
            const count = countResult.status === "fulfilled" ? countResult.value.participantCount : 0
            return toTopicCard(room, detail, count)
          }),
        )
        if (mounted) setTopics(cards)
      } catch (requestError) {
        if (mounted) setError(messageOf(requestError))
      } finally {
        if (mounted) setLoading(false)
      }
    }

    void loadRooms()

    return () => {
      mounted = false
    }
  }, [])

  const openTopics = useMemo(() => topics.filter((topic) => topic.status === "OPEN"), [topics])
  const featuredTopic = openTopics[0] ?? topics[0]
  const visibleTopics = topics.slice(0, 6)
  const totalParticipants = topics.reduce((sum, topic) => sum + topic.participants, 0)
  const categories = Array.from(new Set(topics.map((topic) => topic.category))).slice(0, 8)

  const stats = [
    { label: "전체 토의방", value: topics.length.toLocaleString(), icon: MessageSquare, color: "text-primary" },
    { label: "진행 중", value: openTopics.length.toLocaleString(), icon: Zap, color: "text-emerald-600" },
    { label: "참여자", value: totalParticipants.toLocaleString(), icon: Users, color: "text-accent" },
    { label: "카테고리", value: categories.length.toLocaleString(), icon: TrendingUp, color: "text-rose-500" },
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
                  className="gap-1.5 border-primary/25 bg-primary/5 px-3 py-1 text-xs font-medium text-primary"
                >
                  <Sparkles className="size-3" />
                  승인된 이슈를 실시간 토의방으로 엽니다
                </Badge>
              </div>

              <h1 className="text-balance text-4xl font-bold leading-[1.15] tracking-tight text-foreground md:text-5xl lg:text-[3.5rem]">
                실시간 <span className="text-primary">광장형</span>
                <br />
                토의 아레나
              </h1>

              <p className="max-w-lg text-balance text-base leading-relaxed text-muted-foreground md:text-[17px]">
                승인된 이슈별 토의방에서 채팅, 의견 작성, 발언권 신청으로 다양한 관점을 나눕니다.
              </p>

              <div className="flex flex-wrap justify-center gap-3 lg:justify-start">
                <Link href="/rooms">
                  <Button size="lg" className="gap-2 font-semibold shadow-sm">
                    <Zap className="size-4" />
                    토의 시작하기
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

              <p className="text-xs text-muted-foreground">
                {user
                  ? `현재 ${openTopics.length.toLocaleString()}개의 토의방이 진행 중입니다`
                  : "로그인 후 실제 토의방 목록과 참여 현황을 확인할 수 있습니다"}
              </p>
            </div>

            <div className="w-full max-w-sm shrink-0 lg:max-w-md">
              <div className="rounded-2xl border border-border bg-card p-5 shadow-card">
                {featuredTopic ? (
                  <>
                    <div className="mb-4 flex items-center justify-between">
                      <Badge variant="outline" className="border-primary/30 text-[11px] font-semibold text-primary">
                        {featuredTopic.category}
                      </Badge>
                      <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                        <Users className="size-3.5" />
                        <span className="font-medium text-foreground">{featuredTopic.participants.toLocaleString()}</span>
                        <span>참여 중</span>
                      </div>
                    </div>

                    <h3 className="mb-1 text-balance text-[15px] font-semibold leading-snug text-foreground">
                      {featuredTopic.title}
                    </h3>
                    <p className="mb-4 text-xs leading-relaxed text-muted-foreground">
                      {featuredTopic.description}
                    </p>

                    <Separator className="mb-4" />

                    <div className="flex items-center justify-between">
                      <span className="text-xs text-muted-foreground">
                        {featuredTopic.status === "OPEN" ? "진행 중" : "종료"}
                      </span>
                      <Link
                        href={
                          user
                            ? `/rooms/${featuredTopic.id}`
                            : `/login?redirect=${encodeURIComponent(`/rooms/${featuredTopic.id}`)}`
                        }
                      >
                        <Button size="sm" className="gap-1.5 text-xs font-semibold shadow-sm">
                          입장하기
                          <ArrowRight className="size-3.5" />
                        </Button>
                      </Link>
                    </div>
                  </>
                ) : (
                  <div className="flex min-h-48 flex-col items-center justify-center gap-2 text-center">
                    <MessageSquare className="size-6 text-muted-foreground" />
                    <p className="text-sm font-semibold text-foreground">
                      {loading ? "토의방을 불러오는 중..." : user ? "열린 토의방이 없습니다" : "로그인이 필요합니다"}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {user ? "관리자가 토의방을 개설하면 이곳에 표시됩니다." : "계정으로 로그인하면 DB에 등록된 토의방을 볼 수 있습니다."}
                    </p>
                    {error && <p className="text-xs text-destructive">{error}</p>}
                  </div>
                )}
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
                백엔드에 등록된 토의방을 기준으로 보여줍니다
              </h2>
              <p className="text-sm leading-relaxed text-muted-foreground">
                방 목록, 참여자 수, 토픽 정보는 실제 서버 응답에서 가져옵니다. 아직 API가 없는 예약방과 알림 기능은 표시하지 않습니다.
              </p>
            </div>
            <div className="flex shrink-0 flex-wrap gap-2">
              {[
                { icon: Globe, label: "토픽 조회", color: "text-primary", bg: "bg-primary/8 dark:bg-primary/15" },
                { icon: Shield, label: "인증 기반 입장", color: "text-violet-600 dark:text-violet-400", bg: "bg-violet-50 dark:bg-violet-500/10" },
                { icon: Zap, label: "발언권 시스템", color: "text-amber-600 dark:text-amber-400", bg: "bg-amber-50 dark:bg-amber-500/10" },
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

        {user && categories.length > 0 && (
          <div className="mb-6 flex items-center gap-2 overflow-x-auto pb-1">
            {categories.map((category) => (
              <span
                key={category}
                className="flex-shrink-0 rounded-full bg-muted px-3.5 py-1.5 text-[13px] font-medium text-muted-foreground"
              >
                {category}
              </span>
            ))}
          </div>
        )}

        {user && (
          <section>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-[15px] font-semibold text-foreground">토의방</h2>
              <Link
                href="/rooms"
                className="flex items-center gap-1 text-xs font-medium text-muted-foreground transition-colors hover:text-primary"
              >
                전체 보기 <ArrowRight className="size-3.5" />
              </Link>
            </div>
            {visibleTopics.length > 0 ? (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                {visibleTopics.map((topic) => (
                  <TopicCard key={topic.id} topic={topic} />
                ))}
              </div>
            ) : (
              !loading && (
                <div className="rounded-xl border border-border bg-card px-6 py-12 text-center">
                  <p className="text-sm text-muted-foreground">표시할 토의방이 없습니다.</p>
                </div>
              )
            )}
          </section>
        )}
      </main>

      <footer className="mt-16 border-t border-border">
        <div className="mx-auto max-w-7xl px-4 py-8 md:px-6">
          <div className="flex flex-col items-center justify-between gap-4 md:flex-row">
            <div className="flex items-center gap-2">
              <div className="flex size-6 items-center justify-center rounded-md bg-primary">
                <Zap className="size-3.5 text-primary-foreground" />
              </div>
              <span className="text-sm font-bold text-foreground">시시비비</span>
              <span className="text-xs text-muted-foreground">ARENA TALK</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
