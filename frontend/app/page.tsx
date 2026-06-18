"use client"

import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import { ArrowRight, Loader2, MessageSquare, Radio, Sparkles, Users, Zap, type LucideIcon } from "lucide-react"
import { Navbar } from "@/components/navbar"
import { TopicCard } from "@/components/topic-card"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { roomApi, topicApi } from "@/lib/api/services"
import type { RoomSummary, TopicSummary } from "@/lib/api/types"

export default function HomePage() {
  const [rooms, setRooms] = useState<RoomSummary[]>([])
  const [topics, setTopics] = useState<TopicSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    async function load() {
      setLoading(true)
      setError("")
      try {
        const [openRooms, topicPage] = await Promise.all([roomApi.open(), topicApi.list(0, 8)])
        setRooms(openRooms)
        setTopics(topicPage.content)
      } catch {
        setError("서비스 데이터를 불러오지 못했습니다. 백엔드 서버가 실행 중인지 확인해 주세요.")
      } finally {
        setLoading(false)
      }
    }

    void load()
  }, [])

  const topicById = useMemo(() => new Map(topics.map((topic) => [topic.id, topic])), [topics])

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <section className="border-b border-border">
        <div className="mx-auto grid max-w-7xl gap-10 px-4 py-14 md:px-6 lg:grid-cols-[1.2fr_0.8fr] lg:py-20">
          <div className="flex flex-col justify-center gap-5">
            <Badge variant="outline" className="w-fit gap-1.5 border-primary/25 bg-primary/5 text-primary">
              <Sparkles className="size-3.5" />
              실시간 이슈 기반 토론 서비스
            </Badge>
            <div>
              <h1 className="max-w-3xl text-4xl font-bold leading-tight tracking-tight text-foreground md:text-5xl">
                지금 열려 있는 토론방에서 의견을 나누세요
              </h1>
              <p className="mt-4 max-w-2xl text-base leading-relaxed text-muted-foreground">
                승인된 이슈를 바탕으로 토론방에 입장하고, 발언을 등록하며, 실시간 채팅 흐름을 확인할 수 있습니다.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Link href="/rooms">
                <Button size="lg" className="gap-2 font-semibold">
                  <Zap className="size-4" />
                  토론방 보기
                </Button>
              </Link>
              <Link href="/signup">
                <Button variant="outline" size="lg" className="gap-2 font-semibold">
                  회원가입
                  <ArrowRight className="size-4" />
                </Button>
              </Link>
            </div>
          </div>

          <div className="rounded-xl border border-border bg-card p-5 shadow-card">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <p className="text-sm font-semibold text-foreground">서비스 현황</p>
                <p className="text-xs text-muted-foreground">현재 API 기준 데이터</p>
              </div>
              <Radio className="size-5 text-primary" />
            </div>
            <div className="grid gap-3">
              <div className="rounded-lg bg-muted p-4">
                <p className="text-2xl font-bold text-foreground">{rooms.length}</p>
                <p className="text-xs text-muted-foreground">열린 토론방</p>
              </div>
              <div className="rounded-lg bg-muted p-4">
                <p className="text-2xl font-bold text-foreground">{topics.length}</p>
                <p className="text-xs text-muted-foreground">승인 토픽</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-7xl px-4 py-10 md:px-6">
        {loading ? (
          <div className="flex h-48 items-center justify-center">
            <Loader2 className="size-6 animate-spin text-primary" />
          </div>
        ) : error ? (
          <div className="rounded-xl border border-destructive/20 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        ) : (
          <div className="grid gap-10">
            <section>
              <div className="mb-4 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Radio className="size-4 text-primary" />
                  <h2 className="text-base font-bold text-foreground">진행 중인 토론방</h2>
                </div>
                <Link href="/rooms" className="flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-primary">
                  전체 보기 <ArrowRight className="size-3.5" />
                </Link>
              </div>
              {rooms.length > 0 ? (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {rooms.slice(0, 6).map((room) => (
                    <TopicCard key={room.roomId} kind="room" item={room} topic={topicById.get(room.topicId)} />
                  ))}
                </div>
              ) : (
                <EmptyState icon={Radio} message="현재 열린 토론방이 없습니다." />
              )}
            </section>

            <section>
              <div className="mb-4 flex items-center gap-2">
                <MessageSquare className="size-4 text-muted-foreground" />
                <h2 className="text-base font-bold text-foreground">승인된 토픽</h2>
              </div>
              {topics.length > 0 ? (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  {topics.map((topic) => (
                    <TopicCard key={topic.id} kind="topic" item={topic} />
                  ))}
                </div>
              ) : (
                <EmptyState icon={Users} message="아직 승인된 토픽이 없습니다." />
              )}
            </section>
          </div>
        )}
      </main>
    </div>
  )
}

function EmptyState({ icon: Icon, message }: { icon: LucideIcon; message: string }) {
  return (
    <div className="flex h-40 flex-col items-center justify-center gap-2 rounded-xl border border-border bg-card text-center">
      <Icon className="size-6 text-muted-foreground" />
      <p className="text-sm text-muted-foreground">{message}</p>
    </div>
  )
}
