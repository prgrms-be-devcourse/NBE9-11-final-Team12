"use client"

import Link from "next/link"
import { ArrowRight, CalendarClock, MessageSquare, Radio } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"
import type { RoomSummary, TopicSummary } from "@/lib/api/types"
import { cn } from "@/lib/utils"

export type Topic = {
  id: string
  title: string
  description: string
  category: string
  status: "OPEN" | "CLOSED" | "HOT"
  participants: number
  messages: number
  likes: number
  timeLeft?: string
  tags?: string[]
  isLive?: boolean
  isTrending?: boolean
}

type TopicCardProps =
  | {
      kind: "room"
      item: RoomSummary
      topic?: TopicSummary
      participantCount?: number
      className?: string
    }
  | {
      kind: "topic"
      item: TopicSummary
      className?: string
    }

function formatDate(value: string | null | undefined) {
  if (!value) return "일정 미정"
  return new Date(value).toLocaleString("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

export function TopicCard(props: TopicCardProps) {
  const isRoom = props.kind === "room"
  const title = isRoom ? props.item.title : props.item.title
  const category = isRoom ? props.topic?.category ?? "토론" : props.item.category
  const description = isRoom ? props.topic?.sourceUrl ?? "입장해서 발언과 채팅에 참여해 보세요." : props.item.sourceUrl ?? "출처 없음"
  const href = isRoom ? `/rooms/${props.item.roomId}` : "/rooms"

  return (
    <Card
      className={cn(
        "group relative flex flex-col overflow-hidden border-border bg-card shadow-card transition-all duration-200 hover:-translate-y-px hover:border-border/80 hover:shadow-card-hover",
        props.className,
      )}
    >
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between gap-2">
          <div className="flex flex-wrap items-center gap-1.5">
            <Badge variant="outline" className="border-primary/25 text-primary text-[11px]">
              {category}
            </Badge>
            {isRoom && props.item.status === "OPEN" && (
              <span className="flex items-center gap-1 rounded-full border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-400">
                <span className="size-1.5 rounded-full bg-emerald-500" />
                LIVE
              </span>
            )}
          </div>
          <div className="flex shrink-0 items-center gap-1 text-[11px] text-muted-foreground">
            <CalendarClock className="size-3" />
            {formatDate(isRoom ? props.item.startedAt : props.item.approvedAt ?? props.item.createdAt)}
          </div>
        </div>

        <h3 className="mt-2 line-clamp-2 text-sm font-semibold leading-snug text-foreground transition-colors group-hover:text-primary">
          {title}
        </h3>
        <p className="line-clamp-2 text-[12px] leading-relaxed text-muted-foreground">{description}</p>
      </CardHeader>

      <CardContent className="pb-3 pt-0">
        <div className="flex flex-wrap gap-1">
          <span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">#{category}</span>
          {isRoom && <span className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">#{props.item.status}</span>}
        </div>
      </CardContent>

      <CardFooter className="mt-auto flex items-center justify-between border-t border-border pt-3">
        <div className="flex items-center gap-3 text-[12px] text-muted-foreground">
          <span className="flex items-center gap-1">
            {isRoom ? <Radio className="size-3.5" /> : <MessageSquare className="size-3.5" />}
            <span className="font-medium text-foreground">
              {isRoom ? `${props.participantCount ?? 0}명` : "승인 토픽"}
            </span>
          </span>
        </div>
        <Link href={href}>
          <Button size="sm" variant="outline" className="h-7 gap-1 text-[12px] font-semibold">
            {isRoom ? "입장" : "방 보기"}
            <ArrowRight className="size-3" />
          </Button>
        </Link>
      </CardFooter>
    </Card>
  )
}
