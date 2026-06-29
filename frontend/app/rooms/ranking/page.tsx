"use client"

import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import { Navbar } from "@/components/navbar"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { ApiError } from "@/lib/api/client"
import { roomApi } from "@/lib/api/services"
import type { RoomRanking } from "@/lib/api/types"
import { cn } from "@/lib/utils"
import {
  ArrowRight,
  BarChart3,
  Clock,
  Medal,
  RefreshCw,
  Trophy,
  Users,
} from "lucide-react"

function messageOf(error: unknown) {
  return error instanceof ApiError ? error.message : "토론방 순위를 불러오지 못했습니다."
}

function formatRoomTime(value: string | null | undefined) {
  if (!value) return null

  return new Date(value).toLocaleString("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

function formatScore(score: number | null) {
  if (score == null) return "집계 중"
  return score.toFixed(3)
}

function rankLabel(item: RoomRanking, index: number) {
  return item.rank ?? index + 1
}

export default function RoomRankingPage() {
  const [rankings, setRankings] = useState<RoomRanking[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState("")

  async function loadRankings(isRefresh = false) {
    if (isRefresh) {
      setRefreshing(true)
    } else {
      setLoading(true)
    }
    setError("")

    try {
      const response = await roomApi.ranking()
      setRankings(response)
    } catch (requestError) {
      setError(messageOf(requestError))
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    void loadRankings()
  }, [])

  const openCount = useMemo(
    () => rankings.filter((item) => item.room.status === "OPEN").length,
    [rankings],
  )
  const bestScore = rankings[0]?.score ?? null

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      <main className="mx-auto max-w-6xl px-4 py-8 md:px-6">
        <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <div className="mb-2 flex items-center gap-2">
              <div className="flex size-9 items-center justify-center rounded-lg bg-amber-500/12 text-amber-600 dark:text-amber-300">
                <Trophy className="size-5" />
              </div>
              <span className="text-xs font-semibold uppercase text-primary">Room Ranking</span>
            </div>
            <h1 className="text-2xl font-bold text-foreground">토론방 순위</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              참여자 수, 채팅 메시지 수, 공감 수를 반영한 실시간 토론방 순위입니다.
            </p>
          </div>

          <Button
            variant="outline"
            size="sm"
            className="w-fit gap-1.5"
            onClick={() => void loadRankings(true)}
            disabled={loading || refreshing}
          >
            <RefreshCw className={cn("size-3.5", refreshing && "animate-spin")} />
            새로고침
          </Button>
        </div>

        <div className="mb-6 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <MetricCard icon={Trophy} label="순위 집계" value={`${rankings.length.toLocaleString()}개`} tone="amber" />
          <MetricCard icon={Users} label="진행 중" value={`${openCount.toLocaleString()}개`} tone="emerald" />
          <MetricCard icon={BarChart3} label="최고 점수" value={formatScore(bestScore)} tone="sky" />
        </div>

        {error && (
          <div className="mb-5 rounded-lg border border-destructive/20 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        )}

        {loading ? (
          <div className="rounded-lg border border-border bg-card px-6 py-12 text-center text-sm text-muted-foreground">
            토론방 순위를 불러오는 중입니다.
          </div>
        ) : rankings.length > 0 ? (
          <div className="space-y-3">
            {rankings.map((item, index) => (
              <RankingRow
                key={item.room.roomId}
                item={item}
                rank={rankLabel(item, index)}
              />
            ))}
          </div>
        ) : (
          <div className="rounded-lg border border-border bg-card px-6 py-12 text-center">
            <p className="text-sm font-medium text-foreground">표시할 토론방 순위가 없습니다.</p>
            <p className="mt-1 text-xs text-muted-foreground">
              토론방 활동이 집계되면 이곳에 순위가 표시됩니다.
            </p>
          </div>
        )}
      </main>
    </div>
  )
}

function MetricCard({
  icon: Icon,
  label,
  value,
  tone,
}: {
  icon: typeof Trophy
  label: string
  value: string
  tone: "amber" | "emerald" | "sky"
}) {
  const toneClass = {
    amber: "bg-amber-500/10 text-amber-600 dark:text-amber-300",
    emerald: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-300",
    sky: "bg-sky-500/10 text-sky-600 dark:text-sky-300",
  }[tone]

  return (
    <div className="flex items-center gap-3 rounded-lg border border-border/60 bg-card/60 px-4 py-3">
      <div className={cn("flex size-9 items-center justify-center rounded-md", toneClass)}>
        <Icon className="size-4" />
      </div>
      <div>
        <p className="text-sm font-semibold text-foreground">{value}</p>
        <p className="text-[11px] text-muted-foreground">{label}</p>
      </div>
    </div>
  )
}

function RankingRow({ item, rank }: { item: RoomRanking; rank: number }) {
  const room = item.room
  const startedAt = formatRoomTime(room.startedAt)
  const endedAt = formatRoomTime(room.endedAt)
  const roomHref = `/rooms/${room.roomId}`
  const isPodium = rank <= 3

  return (
    <Card
      className={cn(
        "group border-border bg-card transition-all hover:-translate-y-px hover:border-primary/30 hover:shadow-card-hover",
        isPodium && "border-amber-300/50 dark:border-amber-500/30",
      )}
    >
      <CardContent className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center">
        <div
          className={cn(
            "flex size-12 shrink-0 items-center justify-center rounded-lg border text-sm font-bold",
            isPodium
              ? "border-amber-300/60 bg-amber-500/10 text-amber-700 dark:text-amber-300"
              : "border-border bg-muted text-muted-foreground",
          )}
        >
          {isPodium ? <Medal className="size-5" /> : rank}
        </div>

        <Link href={roomHref} className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <span
              className={cn(
                "rounded-full border px-2 py-0.5 text-[11px] font-semibold",
                room.status === "OPEN"
                  ? "border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-400"
                  : "border-border bg-muted text-muted-foreground",
              )}
            >
              {room.status === "OPEN" ? "진행 중" : "종료"}
            </span>
            <span className="text-[11px] text-muted-foreground">토픽 #{room.topicId}</span>
          </div>
          <h2 className="mt-1 line-clamp-2 text-sm font-semibold text-foreground group-hover:text-primary">
            {room.title}
          </h2>
          <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-[12px] text-muted-foreground">
            {startedAt && (
              <span className="flex items-center gap-1">
                <Clock className="size-3.5" />
                {endedAt ? `${startedAt} - ${endedAt}` : `${startedAt} 시작`}
              </span>
            )}
          </div>
        </Link>

        <div className="flex items-center justify-between gap-3 sm:min-w-44 sm:justify-end">
          <div className="text-right">
            <p className="text-[11px] text-muted-foreground">점수</p>
            <p className="text-sm font-semibold text-foreground">{formatScore(item.score)}</p>
          </div>
          <Link href={roomHref}>
            <Button size="sm" className="h-8 gap-1.5">
              입장
              <ArrowRight className="size-3.5" />
            </Button>
          </Link>
        </div>
      </CardContent>
    </Card>
  )
}
