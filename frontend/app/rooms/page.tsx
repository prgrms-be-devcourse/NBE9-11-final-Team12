"use client"

import { useEffect, useMemo, useState } from "react"
import { Loader2, MessageSquare, Radio, Search, type LucideIcon } from "lucide-react"
import { Navbar } from "@/components/navbar"
import { TopicCard } from "@/components/topic-card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { roomApi, topicApi } from "@/lib/api/services"
import type { RoomSummary, TopicSummary } from "@/lib/api/types"

export default function RoomsPage() {
  const [rooms, setRooms] = useState<RoomSummary[]>([])
  const [topics, setTopics] = useState<TopicSummary[]>([])
  const [query, setQuery] = useState("")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    async function load() {
      setLoading(true)
      setError("")
      try {
        const [roomList, topicPage] = await Promise.all([roomApi.list(), topicApi.list(0, 100)])
        setRooms(roomList)
        setTopics(topicPage.content)
      } catch {
        setError("토론방 목록을 불러오지 못했습니다.")
      } finally {
        setLoading(false)
      }
    }

    void load()
  }, [])

  const topicById = useMemo(() => new Map(topics.map((topic) => [topic.id, topic])), [topics])
  const filteredRooms = rooms.filter((room) => {
    const topic = topicById.get(room.topicId)
    const text = `${room.title} ${topic?.title ?? ""} ${topic?.category ?? ""}`.toLowerCase()
    return text.includes(query.trim().toLowerCase())
  })

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <main className="mx-auto max-w-7xl px-4 py-8 md:px-6">
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="mb-1 text-2xl font-bold text-foreground">토론방 목록</h1>
            <p className="text-sm text-muted-foreground">{rooms.length}개 토론방을 확인할 수 있습니다.</p>
          </div>
          <div className="relative sm:w-72">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="토론방 검색" className="pl-9" />
          </div>
        </div>

        <div className="mb-8 grid gap-3 sm:grid-cols-3">
          <StatCard label="전체 토론방" value={rooms.length.toLocaleString()} icon={MessageSquare} />
          <StatCard label="열린 토론방" value={rooms.filter((room) => room.status === "OPEN").length.toLocaleString()} icon={Radio} />
          <StatCard label="승인 토픽" value={topics.length.toLocaleString()} icon={MessageSquare} />
        </div>

        {loading ? (
          <div className="flex h-52 items-center justify-center">
            <Loader2 className="size-6 animate-spin text-primary" />
          </div>
        ) : error ? (
          <div className="rounded-xl border border-destructive/20 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        ) : filteredRooms.length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {filteredRooms.map((room) => (
              <TopicCard key={room.roomId} kind="room" item={room} topic={topicById.get(room.topicId)} />
            ))}
          </div>
        ) : (
          <div className="flex h-52 flex-col items-center justify-center gap-3 rounded-xl border border-border bg-card text-center">
            <Radio className="size-7 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">조건에 맞는 토론방이 없습니다.</p>
            {query && (
              <Button variant="outline" size="sm" onClick={() => setQuery("")}>
                검색 초기화
              </Button>
            )}
          </div>
        )}
      </main>
    </div>
  )
}

function StatCard({ label, value, icon: Icon }: { label: string; value: string; icon: LucideIcon }) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-card px-4 py-3">
      <Icon className="size-5 text-primary" />
      <div>
        <p className="text-sm font-semibold text-foreground">{value}</p>
        <p className="text-[11px] text-muted-foreground">{label}</p>
      </div>
    </div>
  )
}
