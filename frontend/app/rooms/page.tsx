"use client"

import { useEffect, useMemo, useState } from "react"
import { Navbar } from "@/components/navbar"
import { TopicCard, type Topic } from "@/components/topic-card"
import { ApiError } from "@/lib/api/client"
import { roomApi, topicApi } from "@/lib/api/services"
import type { RoomSummary, TopicSummary } from "@/lib/api/types"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import {
  Layers3,
  MessageSquare,
  Radio,
  Search,
  TrendingUp,
  Users,
} from "lucide-react"

const ALL_CATEGORIES = "전체"

const STATUS_TABS = [
  { label: "전체", value: "all" },
  { label: "진행 중", value: "open" },
  { label: "종료", value: "closed" },
  { label: "참여자순", value: "popular" },
]

function messageOf(error: unknown) {
  return error instanceof ApiError ? error.message : "토론방 목록을 불러오지 못했습니다."
}

function formatRoomClock(value: string | null | undefined) {
  if (!value) return null

  return new Date(value).toLocaleTimeString("ko-KR", {
    hour: "2-digit",
    minute: "2-digit",
  })
}

function roomTimeLabel(room: RoomSummary) {
  const startedAt = formatRoomClock(room.startedAt)
  const endedAt = formatRoomClock(room.endedAt)

  if (startedAt && endedAt) return `${startedAt} - ${endedAt}`
  if (startedAt) return `${startedAt} 시작`
  return room.status === "OPEN" ? "진행 중" : undefined
}

function toTopicCard(
  room: RoomSummary,
  topic: TopicSummary | null,
  participantCount: number,
): Topic {
  const category = topic?.category || "기타"

  return {
    id: String(room.roomId),
    title: room.title,
    description: topic?.title || "승인된 토픽으로 개설된 실시간 토론방입니다.",
    category,
    status: room.status,
    participants: participantCount,
    timeLeft: roomTimeLabel(room),
    tags: [category],
    isLive: room.status === "OPEN",
  }
}

export default function RoomsPage() {
  const [activeStatus, setActiveStatus] = useState("all")
  const [activeCategory, setActiveCategory] = useState(ALL_CATEGORIES)
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
        const [rooms, approvedTopicsPage] = await Promise.all([
          roomApi.list(),
          topicApi.list(0, 200),
        ])
        const approvedTopicById = new Map(
          approvedTopicsPage.content.map((topic) => [topic.id, topic]),
        )
        const cards = await Promise.all(
          rooms.map(async (room) => {
            const countResult = await Promise.allSettled([
              roomApi.participantCount(room.roomId),
            ])
            const count =
              countResult[0].status === "fulfilled"
                ? countResult[0].value.participantCount
                : 0
            return toTopicCard(room, approvedTopicById.get(room.topicId) ?? null, count)
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

  const categoryCounts = useMemo(() => {
    const counts = new Map<string, number>()
    topics.forEach((topic) => {
      counts.set(topic.category, (counts.get(topic.category) ?? 0) + 1)
    })
    return counts
  }, [topics])

  const categories = useMemo(
    () => [
      ALL_CATEGORIES,
      ...Array.from(categoryCounts.keys()).sort((left, right) => left.localeCompare(right, "ko-KR")),
    ],
    [categoryCounts],
  )

  const visibleTopics = useMemo(() => {
    const query = search.trim().toLowerCase()
    const filtered = topics.filter((topic) => {
      const matchesCategory = activeCategory === ALL_CATEGORIES || topic.category === activeCategory
      const matchesSearch =
        !query ||
        topic.title.toLowerCase().includes(query) ||
        topic.description.toLowerCase().includes(query) ||
        topic.category.toLowerCase().includes(query)
      const matchesStatus =
        activeStatus === "all" ||
        (activeStatus === "open" && topic.status === "OPEN") ||
        (activeStatus === "closed" && topic.status === "CLOSED") ||
        activeStatus === "popular"

      return matchesCategory && matchesSearch && matchesStatus
    })

    if (activeStatus === "popular") {
      return [...filtered].sort((a, b) => b.participants - a.participants)
    }

    return filtered
  }, [activeCategory, activeStatus, search, topics])

  const totalParticipants = topics.reduce((sum, topic) => sum + topic.participants, 0)
  const openCount = topics.filter((topic) => topic.status === "OPEN").length
  const closedCount = topics.filter((topic) => topic.status === "CLOSED").length
  const activeCategoryCount =
    activeCategory === ALL_CATEGORIES ? topics.length : categoryCounts.get(activeCategory) ?? 0

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <main className="mx-auto max-w-7xl px-4 py-8 md:px-6">
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="mb-1 text-2xl font-bold text-foreground">토론방 목록</h1>
            <p className="text-sm text-muted-foreground">
              {topics.length.toLocaleString()}개 토론방 · 총 {totalParticipants.toLocaleString()}명 참여 중
            </p>
          </div>
          <div className="relative flex-1 sm:max-w-72">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="제목, 설명, 카테고리 검색"
              className="border-border/50 bg-card pl-9"
            />
          </div>
        </div>

        <div className="mb-8 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[
            { label: "전체 토론방", value: `${topics.length.toLocaleString()}개`, icon: MessageSquare, color: "text-primary" },
            { label: "진행 중", value: `${openCount.toLocaleString()}개`, icon: Radio, color: "text-emerald-600 dark:text-emerald-400" },
            { label: "종료", value: `${closedCount.toLocaleString()}개`, icon: TrendingUp, color: "text-muted-foreground" },
            { label: "카테고리", value: `${categoryCounts.size.toLocaleString()}개`, icon: Layers3, color: "text-accent" },
          ].map(({ label, value, icon: Icon, color }) => (
            <div
              key={label}
              className="flex items-center gap-3 rounded-lg border border-border/50 bg-card/50 px-4 py-3"
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
          <div className="mb-6 rounded-lg border border-destructive/20 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        )}

        {loading && (
          <div className="mb-6 rounded-lg border border-border bg-card px-6 py-8 text-center text-sm text-muted-foreground">
            토론방 목록을 불러오는 중입니다.
          </div>
        )}

        <section className="mb-6">
          <div className="mb-3 flex items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-semibold text-foreground">카테고리별 보기</h2>
              <p className="text-xs text-muted-foreground">
                {activeCategory === ALL_CATEGORIES
                  ? "모든 카테고리의 토론방을 보고 있습니다."
                  : `${activeCategory} 카테고리 토론방 ${activeCategoryCount.toLocaleString()}개`}
              </p>
            </div>
            <div className="hidden items-center gap-1 text-xs text-muted-foreground sm:flex">
              <Users className="size-3.5" />
              {visibleTopics.length.toLocaleString()}개 표시
            </div>
          </div>

          <div className="flex gap-2 overflow-x-auto pb-1">
            {categories.map((category) => {
              const count = category === ALL_CATEGORIES ? topics.length : categoryCounts.get(category) ?? 0
              return (
                <button
                  key={category}
                  onClick={() => setActiveCategory(category)}
                  className={cn(
                    "flex shrink-0 items-center gap-1.5 rounded-full border px-3 py-1.5 text-[12px] font-medium transition-colors",
                    activeCategory === category
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-border bg-card text-muted-foreground hover:border-primary/40 hover:text-foreground",
                  )}
                >
                  <span>{category}</span>
                  <span
                    className={cn(
                      "rounded-full px-1.5 py-0.5 text-[10px]",
                      activeCategory === category ? "bg-primary-foreground/20" : "bg-muted",
                    )}
                  >
                    {count.toLocaleString()}
                  </span>
                </button>
              )
            })}
          </div>
        </section>

        <div className="mb-5 flex items-center gap-1 overflow-x-auto border-b border-border/50">
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.value}
              onClick={() => setActiveStatus(tab.value)}
              className={cn(
                "flex shrink-0 items-center gap-1.5 border-b-2 px-4 py-2.5 text-sm font-medium transition-colors",
                activeStatus === tab.value
                  ? "border-primary text-primary"
                  : "border-transparent text-muted-foreground hover:text-foreground",
              )}
            >
              {tab.label}
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
    <div className="rounded-lg border border-border bg-card px-6 py-12 text-center">
      <p className="text-sm text-muted-foreground">해당 조건에 맞는 토론방이 없습니다.</p>
    </div>
  )
}
