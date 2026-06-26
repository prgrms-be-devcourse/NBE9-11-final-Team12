"use client"

import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardFooter, CardHeader } from "@/components/ui/card"
import { useAuth } from "@/components/auth-provider"
import { cn } from "@/lib/utils"
import {
  ArrowRight,
  Clock,
  Users,
} from "lucide-react"

export interface Topic {
  id: string
  title: string
  description: string
  category: string
  status: "OPEN" | "CLOSED"
  participants: number
  messages?: number
  likes?: number
  timeLeft?: string
  tags?: string[]
  isLive?: boolean
}

interface TopicCardProps {
  topic: Topic
  className?: string
}

const categoryConfig: Record<string, { bg: string; text: string; border: string }> = {
  정치: { bg: "bg-red-50 dark:bg-red-500/10", text: "text-red-700 dark:text-red-400", border: "border-red-200 dark:border-red-500/20" },
  경제: { bg: "bg-emerald-50 dark:bg-emerald-500/10", text: "text-emerald-700 dark:text-emerald-400", border: "border-emerald-200 dark:border-emerald-500/20" },
  사회: { bg: "bg-amber-50 dark:bg-amber-500/10", text: "text-amber-700 dark:text-amber-400", border: "border-amber-200 dark:border-amber-500/20" },
  국제: { bg: "bg-sky-50 dark:bg-sky-500/10", text: "text-sky-700 dark:text-sky-400", border: "border-sky-200 dark:border-sky-500/20" },
  IT: { bg: "bg-violet-50 dark:bg-violet-500/10", text: "text-violet-700 dark:text-violet-400", border: "border-violet-200 dark:border-violet-500/20" },
  과학: { bg: "bg-teal-50 dark:bg-teal-500/10", text: "text-teal-700 dark:text-teal-400", border: "border-teal-200 dark:border-teal-500/20" },
  문화: { bg: "bg-pink-50 dark:bg-pink-500/10", text: "text-pink-700 dark:text-pink-400", border: "border-pink-200 dark:border-pink-500/20" },
  스포츠: { bg: "bg-orange-50 dark:bg-orange-500/10", text: "text-orange-700 dark:text-orange-400", border: "border-orange-200 dark:border-orange-500/20" },
  연예: { bg: "bg-fuchsia-50 dark:bg-fuchsia-500/10", text: "text-fuchsia-700 dark:text-fuchsia-400", border: "border-fuchsia-200 dark:border-fuchsia-500/20" },
  기타: { bg: "bg-muted", text: "text-muted-foreground", border: "border-border" },
}

const defaultCategory = { bg: "bg-muted", text: "text-muted-foreground", border: "border-border" }

export function TopicCard({ topic, className }: TopicCardProps) {
  const category = categoryConfig[topic.category] ?? defaultCategory
  const { user } = useAuth()
  const roomHref = `/rooms/${topic.id}`
  const entryHref = user ? roomHref : `/login?redirect=${encodeURIComponent(roomHref)}`

  return (
    <Card
      className={cn(
        "group relative flex flex-col overflow-hidden border-border bg-card shadow-card transition-all duration-200",
        "hover:-translate-y-px hover:border-border/80 hover:shadow-card-hover",
        className,
      )}
    >
      <CardHeader className="pb-2">
        <div className="flex items-start justify-between gap-2">
          <div className="flex flex-wrap items-center gap-1.5">
            <span
              className={cn(
                "rounded-full border px-2 py-0.5 text-[11px] font-medium",
                category.bg,
                category.text,
                category.border,
              )}
            >
              {topic.category}
            </span>
            <span
              className={cn(
                "rounded-full border px-2 py-0.5 text-[11px] font-semibold",
                topic.status === "OPEN"
                  ? "border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-500/20 dark:bg-emerald-500/10 dark:text-emerald-400"
                  : "border-border bg-muted text-muted-foreground",
              )}
            >
              {topic.status === "OPEN" ? "진행 중" : "종료"}
            </span>
          </div>
          {topic.timeLeft && (
            <div className="flex shrink-0 items-center gap-1 text-[11px] text-muted-foreground">
              <Clock className="size-3" />
              {topic.timeLeft}
            </div>
          )}
        </div>

        <h3 className="mt-2 line-clamp-2 text-sm font-semibold leading-snug text-foreground transition-colors group-hover:text-primary">
          {topic.title}
        </h3>
        <p className="line-clamp-2 text-[12px] leading-relaxed text-muted-foreground">
          {topic.description}
        </p>
      </CardHeader>

      <CardContent className="pb-3 pt-0">
        {topic.tags && topic.tags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {topic.tags.map((tag) => (
              <span
                key={tag}
                className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground"
              >
                #{tag}
              </span>
            ))}
          </div>
        )}
      </CardContent>

      <CardFooter className="mt-auto flex items-center justify-between border-t border-border pt-3">
        <div className="flex items-center gap-3 text-[12px] text-muted-foreground">
          <span className="flex items-center gap-1">
            <Users className="size-3.5" />
            <span className="font-medium text-foreground">{topic.participants.toLocaleString()}</span>
          </span>
        </div>
        <Link href={entryHref}>
          <Button
            size="sm"
            variant="outline"
            className="h-7 gap-1 text-[12px] font-semibold"
          >
            입장
            <ArrowRight className="size-3" />
          </Button>
        </Link>
      </CardFooter>
    </Card>
  )
}
