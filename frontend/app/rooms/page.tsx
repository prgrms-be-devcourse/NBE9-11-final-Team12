"use client"

import { useState, useEffect } from "react"
import { Navbar } from "@/components/navbar"
import { TopicCard } from "@/components/topic-card"
import { mockTopics, scheduledRooms, type ScheduledRoom } from "@/lib/mock-data"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Search,
  SlidersHorizontal,
  Users,
  Clock,
  Flame,
  Bell,
  BellRing,
  CalendarClock,
  TrendingUp,
  MessageSquare,
  ChevronRight,
} from "lucide-react"
import { cn } from "@/lib/utils"

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatCountdown(targetDate: Date): string {
  const diff = Math.max(0, targetDate.getTime() - Date.now())
  const totalSecs = Math.floor(diff / 1000)
  const hours = Math.floor(totalSecs / 3600)
  const mins = Math.floor((totalSecs % 3600) / 60)
  const secs = totalSecs % 60

  if (hours >= 24) {
    const days = Math.floor(hours / 24)
    const remHours = hours % 24
    return remHours > 0 ? `${days}일 ${remHours}시간 후` : `${days}일 후`
  }
  if (hours > 0) return `${hours}시간 ${String(mins).padStart(2, "0")}분 후`
  if (mins > 0) return `${mins}분 ${String(secs).padStart(2, "0")}초 후`
  return `${secs}초 후`
}

function formatOpenTime(date: Date): string {
  return date.toLocaleString("ko-KR", {
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

// ── Scheduled room card ───────────────────────────────────────────────────────

const categoryConfig: Record<string, { bg: string; text: string; border: string }> = {
  "AI·기술":  { bg: "bg-violet-50 dark:bg-violet-500/10", text: "text-violet-700 dark:text-violet-400", border: "border-violet-200 dark:border-violet-500/20" },
  "경제·금융": { bg: "bg-emerald-50 dark:bg-emerald-500/10", text: "text-emerald-700 dark:text-emerald-400", border: "border-emerald-200 dark:border-emerald-500/20" },
  "사회·복지": { bg: "bg-amber-50 dark:bg-amber-500/10", text: "text-amber-700 dark:text-amber-400", border: "border-amber-200 dark:border-amber-500/20" },
  "정치·외교": { bg: "bg-red-50 dark:bg-red-500/10", text: "text-red-700 dark:text-red-400", border: "border-red-200 dark:border-red-500/20" },
  "문화·연예": { bg: "bg-pink-50 dark:bg-pink-500/10", text: "text-pink-700 dark:text-pink-400", border: "border-pink-200 dark:border-pink-500/20" },
  "스포츠":   { bg: "bg-orange-50 dark:bg-orange-500/10", text: "text-orange-700 dark:text-orange-400", border: "border-orange-200 dark:border-orange-500/20" },
  "환경·과학": { bg: "bg-teal-50 dark:bg-teal-500/10", text: "text-teal-700 dark:text-teal-400", border: "border-teal-200 dark:border-teal-500/20" },
}
const defaultCat = { bg: "bg-muted", text: "text-muted-foreground", border: "border-border" }

function ScheduledRoomCard({ room }: { room: ScheduledRoom }) {
  const [countdown, setCountdown] = useState(() => formatCountdown(room.scheduledAt))
  const [notified, setNotified] = useState(false)
  const cat = categoryConfig[room.category] ?? defaultCat

  useEffect(() => {
    const id = setInterval(() => {
      setCountdown(formatCountdown(room.scheduledAt))
    }, 1000)
    return () => clearInterval(id)
  }, [room.scheduledAt])

  return (
    <div className="group flex flex-col gap-3 rounded-xl border border-border bg-card p-4 transition-all duration-200 hover:border-primary/30 hover:shadow-sm">
      {/* Header row */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex flex-wrap items-center gap-1.5">
          <span className={cn("rounded-full border px-2 py-0.5 text-[11px] font-medium", cat.bg, cat.text, cat.border)}>
            {room.category}
          </span>
          <span className="flex items-center gap-1 rounded-full border border-primary/25 bg-primary/8 px-2 py-0.5 text-[11px] font-semibold text-primary">
            <CalendarClock className="size-2.5" />
            예정
          </span>
        </div>
        {/* Live countdown */}
        <div className="flex shrink-0 items-center gap-1 rounded-md border border-border bg-muted px-2 py-1 text-[11px] font-mono font-semibold text-foreground">
          <Clock className="size-3 text-muted-foreground" />
          {countdown}
        </div>
      </div>

      {/* Title & desc */}
      <div>
        <h3 className="mb-1 line-clamp-2 text-sm font-semibold leading-snug text-foreground">
          {room.title}
        </h3>
        <p className="line-clamp-2 text-[12px] leading-relaxed text-muted-foreground">
          {room.description}
        </p>
      </div>

      {/* Tags */}
      {room.tags && room.tags.length > 0 && (
        <div className="flex flex-wrap gap-1">
          {room.tags.map((tag) => (
            <span key={tag} className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">
              #{tag}
            </span>
          ))}
        </div>
      )}

      {/* Footer */}
      <div className="flex items-center justify-between border-t border-border pt-3">
        <div className="flex items-center gap-3 text-[11px] text-muted-foreground">
          <span className="flex items-center gap-1">
            <Users className="size-3.5" />
            예상 {(room.estimatedParticipants ?? 0).toLocaleString()}명
          </span>
          <span className="flex items-center gap-1">
            <BellRing className="size-3.5" />
            {(room.notifyCount ?? 0).toLocaleString()}명 신청
          </span>
        </div>
        <Button
          size="sm"
          variant="outline"
          className={cn(
            "h-7 gap-1.5 text-[11px] font-semibold transition-all",
            notified && "border-primary/40 bg-primary/10 text-primary hover:bg-primary/15"
          )}
          onClick={() => setNotified((v) => !v)}
        >
          <Bell className={cn("size-3", notified && "fill-primary")} />
          {notified ? "알림 신청됨" : "알림 신청"}
        </Button>
      </div>

      <p className="text-[11px] text-muted-foreground/60">
        오픈 예정: {formatOpenTime(room.scheduledAt)}
      </p>
    </div>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────

const TABS = [
  { label: "전체", value: "all" },
  { label: "핫 토픽", value: "hot" },
  { label: "최신순", value: "latest" },
  { label: "인기순", value: "popular" },
]

const CATEGORIES = [
  "전체", "AI·기술", "경제·금융", "사회·복지",
  "정치·외교", "문화·연예", "스포츠", "환경·과학",
]

const totalParticipants = mockTopics.reduce((sum, t) => sum + t.participants, 0)

export default function RoomsPage() {
  const [activeTab, setActiveTab] = useState("all")
  const [activeCategory, setActiveCategory] = useState("전체")

  const filtered = (topics: typeof mockTopics) =>
    activeCategory === "전체" ? topics : topics.filter((t) => t.category === activeCategory)

  const hotTopics = filtered(mockTopics.filter((t) => t.isTrending))
  const regularTopics = filtered(mockTopics.filter((t) => !t.isTrending))

  const tabTopics = (() => {
    if (activeTab === "hot") return filtered(mockTopics.filter((t) => t.isTrending))
    if (activeTab === "latest") return filtered([...mockTopics].sort((a, b) => b.messages - a.messages))
    if (activeTab === "popular") return filtered([...mockTopics].sort((a, b) => b.participants - a.participants))
    return []
  })()

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <main className="mx-auto max-w-7xl px-4 py-8 md:px-6">

        {/* ── Page header ───────────────────────────────────── */}
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="mb-1 text-2xl font-bold text-foreground">토의방 목록</h1>
            <p className="text-sm text-muted-foreground">
              {mockTopics.length}개 토의방 · 총 {totalParticipants.toLocaleString()}명 참여 중
            </p>
          </div>
          <div className="flex items-center gap-2">
            <div className="relative flex-1 sm:w-64">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input placeholder="토의방 검색..." className="pl-9 bg-card border-border/50" />
            </div>
            <Button variant="outline" size="icon" className="shrink-0">
              <SlidersHorizontal className="size-4" />
              <span className="sr-only">필터</span>
            </Button>
          </div>
        </div>

        {/* ── Quick stats ───────────────────────────────────── */}
        <div className="mb-8 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[
            { label: "참여 중인 토의방", value: `${mockTopics.length}개`, icon: MessageSquare, color: "text-primary" },
            { label: "총 참여자", value: totalParticipants.toLocaleString(), icon: Users, color: "text-accent" },
            { label: "오늘 핫 토픽", value: `${mockTopics.filter((t) => t.isTrending).length}개`, icon: Flame, color: "text-destructive" },
            { label: "평균 토의 시간", value: "47분", icon: Clock, color: "text-amber-600 dark:text-amber-400" },
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

        {/* ── Upcoming / Scheduled rooms ────────────────────── */}
        {scheduledRooms.length > 0 && (
          <section className="mb-10">
            <div className="mb-4 flex items-center gap-2">
              <CalendarClock className="size-4 text-primary" />
              <h2 className="text-base font-bold text-foreground">곧 열리는 토론방</h2>
              <Badge variant="outline" className="border-primary/30 text-primary text-[10px]">
                {scheduledRooms.length}개 예정
              </Badge>
              <div className="ml-auto">
                <Button variant="ghost" size="sm" className="h-7 gap-1 text-[12px] text-muted-foreground hover:text-foreground">
                  전체 일정 보기
                  <ChevronRight className="size-3.5" />
                </Button>
              </div>
            </div>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {scheduledRooms.map((room) => (
                <ScheduledRoomCard key={room.id} room={room} />
              ))}
            </div>
          </section>
        )}

        {/* ── Tabs ─────────────────────────────────────────── */}
        <div className="mb-5 flex items-center gap-1 overflow-x-auto border-b border-border/50">
          {TABS.map((tab) => (
            <button
              key={tab.value}
              onClick={() => setActiveTab(tab.value)}
              className={cn(
                "flex shrink-0 items-center gap-1.5 border-b-2 px-4 py-2.5 text-sm font-medium transition-colors",
                activeTab === tab.value
                  ? "border-primary text-primary"
                  : "border-transparent text-muted-foreground hover:text-foreground"
              )}
            >
              {tab.value === "hot" && <Flame className="size-3.5" />}
              {tab.value === "popular" && <TrendingUp className="size-3.5" />}
              {tab.label}
            </button>
          ))}
        </div>

        {/* ── Category chips ────────────────────────────────── */}
        <div className="mb-6 flex flex-wrap gap-2">
          {CATEGORIES.map((cat) => (
            <button
              key={cat}
              onClick={() => setActiveCategory(cat)}
              className={cn(
                "rounded-full border px-3 py-1 text-[12px] font-medium transition-colors",
                activeCategory === cat
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-card text-muted-foreground hover:border-primary/40 hover:text-foreground"
              )}
            >
              {cat}
            </button>
          ))}
        </div>

        {/* ── Content: "전체" tab has 2-section layout ─────── */}
        {activeTab === "all" ? (
          <>
            {hotTopics.length > 0 && (
              <section className="mb-10">
                <div className="mb-4 flex items-center gap-2">
                  <Flame className="size-4 text-destructive" />
                  <h2 className="text-base font-bold text-foreground">뜨거운 토픽</h2>
                  <Badge variant="outline" className="border-destructive/30 text-destructive text-[10px]">
                    HOT
                  </Badge>
                </div>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {hotTopics.map((topic) => (
                    <TopicCard key={topic.id} topic={topic} />
                  ))}
                </div>
              </section>
            )}

            <section>
              <div className="mb-4 flex items-center gap-2">
                <MessageSquare className="size-4 text-muted-foreground" />
                <h2 className="text-base font-bold text-foreground">전체 토의방</h2>
                <span className="text-sm text-muted-foreground">({regularTopics.length}개)</span>
              </div>
              {regularTopics.length > 0 ? (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {regularTopics.map((topic) => (
                    <TopicCard key={topic.id} topic={topic} />
                  ))}
                </div>
              ) : (
                <EmptyState />
              )}
            </section>
          </>
        ) : (
          <section>
            {tabTopics.length > 0 ? (
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {tabTopics.map((topic) => (
                  <TopicCard key={topic.id} topic={topic} />
                ))}
              </div>
            ) : (
              <EmptyState />
            )}
          </section>
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
