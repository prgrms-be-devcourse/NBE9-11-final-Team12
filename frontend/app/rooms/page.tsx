"use client"

import { useEffect, useMemo, useState } from "react"
import { Navbar } from "@/components/navbar"
import { TopicCard, type Topic } from "@/components/topic-card"
import { ApiError } from "@/lib/api/client"
import { roomApi, topicApi } from "@/lib/api/services"
import type { RoomSummary, TopicDetail } from "@/lib/api/types"
import { Input } from "@/components/ui/input"
import {
  Search,
  Users,
  Radio,
  MessageSquare,
  TrendingUp,
} from "lucide-react"
import { cn } from "@/lib/utils"

const TABS = [
  { label: "전체", value: "all" },
  { label: "진행 중", value: "open" },
  { label: "종료", value: "closed" },
  { label: "참여자순", value: "popular" },
]

function messageOf(error: unknown) {
  return error instanceof ApiError ? error.message : "토의방 목록을 불러오지 못했습니다."
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

export default function RoomsPage() {
  const [activeTab, setActiveTab] = useState("all")
  const [activeCategory, setActiveCategory] = useState("전체")
  const [search, setSearch] = useState("")
  const [topics, setTopics] = useState<Topic[]>([])
  const [loading, setLoading] = useState(true)
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

  const categories = useMemo(
    () => ["전체", ...Array.from(new Set(topics.map((topic) => topic.category))).sort()],
    [topics],
  )

  const visibleTopics = useMemo(() => {
    const query = search.trim().toLowerCase()
    const filtered = topics.filter((topic) => {
      const matchesCategory = activeCategory === "전체" || topic.category === activeCategory
      const matchesSearch =
        !query ||
        topic.title.toLowerCase().includes(query) ||
        topic.description.toLowerCase().includes(query) ||
        topic.category.toLowerCase().includes(query)
      const matchesTab =
        activeTab === "all" ||
        (activeTab === "open" && topic.status === "OPEN") ||
        (activeTab === "closed" && topic.status === "CLOSED") ||
        activeTab === "popular"
      return matchesCategory && matchesSearch && matchesTab
    })

    if (activeTab === "popular") {
      return [...filtered].sort((a, b) => b.participants - a.participants)
    }
    return filtered
  }, [activeCategory, activeTab, search, topics])

  const totalParticipants = topics.reduce((sum, topic) => sum + topic.participants, 0)
  const openCount = topics.filter((topic) => topic.status === "OPEN").length
  const closedCount = topics.filter((topic) => topic.status === "CLOSED").length

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <main className="mx-auto max-w-7xl px-4 py-8 md:px-6">
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="mb-1 text-2xl font-bold text-foreground">토의방 목록</h1>
            <p className="text-sm text-muted-foreground">
              {topics.length}개 토의방 · 총 {totalParticipants.toLocaleString()}명 참여 중
            </p>
          </div>
          <div className="relative flex-1 sm:max-w-72">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="토의방 검색..."
              className="pl-9 bg-card border-border/50"
            />
          </div>
        </div>

        <div className="mb-8 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[
            { label: "전체 토의방", value: `${topics.length}개`, icon: MessageSquare, color: "text-primary" },
            { label: "진행 중", value: `${openCount}개`, icon: Radio, color: "text-emerald-600 dark:text-emerald-400" },
            { label: "종료", value: `${closedCount}개`, icon: TrendingUp, color: "text-muted-foreground" },
            { label: "총 참여자", value: totalParticipants.toLocaleString(), icon: Users, color: "text-accent" },
          ].map(({ label, value, icon: Icon, color }) => (
            <div
              key={label}
              className="flex items-center gap-3 rounded-xl border border-border/50 bg-card/50 px-4 py-3"
            >
              <Icon className={`size-5 shrink-0 ${color}`} />
              <div>
                <p className="text-sm font-semibold text-foreground">{value}</p>
                <p className="text-[11px] text-muted-foreground">{label}</p>
              </div>
            </div>
          ))}
        </div>

        {error && (
          <div className="mb-6 rounded-xl border border-destructive/20 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        )}

        {loading && (
          <div className="mb-6 rounded-xl border border-border bg-card px-6 py-8 text-center text-sm text-muted-foreground">
            토의방 목록을 불러오는 중...
          </div>
        )}

        <div className="mb-5 flex items-center gap-1 overflow-x-auto border-b border-border/50">
          {TABS.map((tab) => (
            <button
              key={tab.value}
              onClick={() => setActiveTab(tab.value)}
              className={cn(
                "flex shrink-0 items-center gap-1.5 border-b-2 px-4 py-2.5 text-sm font-medium transition-colors",
                activeTab === tab.value
                  ? "border-primary text-primary"
                  : "border-transparent text-muted-foreground hover:text-foreground",
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <div className="mb-6 flex flex-wrap gap-2">
          {categories.map((category) => (
            <button
              key={category}
              onClick={() => setActiveCategory(category)}
              className={cn(
                "rounded-full border px-3 py-1 text-[12px] font-medium transition-colors",
                activeCategory === category
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-card text-muted-foreground hover:border-primary/40 hover:text-foreground",
              )}
            >
              {category}
            </button>
          ))}
        </div>

        {visibleTopics.length > 0 ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {visibleTopics.map((topic) => (
              <TopicCard key={topic.id} topic={topic} />
            ))}
          </div>
        ) : (
          !loading && <EmptyState />
        )}
      </main>
    </div>
  )
}

function EmptyState() {
  return (
    <div className="rounded-xl border border-border bg-card px-6 py-12 text-center">
      <p className="text-sm text-muted-foreground">해당 조건의 토의방이 없습니다.</p>
    </div>
  )
}
