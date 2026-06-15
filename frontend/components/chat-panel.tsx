"use client"

import { useState, useRef, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog"
import { cn } from "@/lib/utils"
import { mockChatMessages } from "@/lib/mock-data"
import { Send, Heart, MessageSquare, Pin, Flag } from "lucide-react"

interface ChatMessage {
  id: string
  userId: string
  nickname: string
  content: string
  timestamp: string
  isHighlighted: boolean
  likeCount?: number
}

const avatarColors = [
  "bg-primary/15 text-primary",
  "bg-violet-100 text-violet-700 dark:bg-violet-500/20 dark:text-violet-400",
  "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-400",
  "bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400",
  "bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-400",
  "bg-pink-100 text-pink-700 dark:bg-pink-500/20 dark:text-pink-400",
]

function getAvatarColor(userId: string) {
  const idx = userId.charCodeAt(1) % avatarColors.length
  return avatarColors[idx]
}

const REPORT_REASONS = [
  "욕설 / 혐오 발언",
  "허위 사실 유포",
  "광고 / 스팸",
  "개인정보 노출",
  "주제 무관 발언",
  "기타",
]

// seed initial like counts
const initialMessages: ChatMessage[] = mockChatMessages.map((m, i) => ({
  ...m,
  likeCount: [3, 12, 1, 5, 0, 9, 2, 4][i % 8],
}))

export function ChatPanel() {
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages)
  const [input, setInput] = useState("")
  const [likedIds, setLikedIds] = useState<Set<string>>(new Set())
  const bottomRef = useRef<HTMLDivElement>(null)

  // Report state
  const [reportTarget, setReportTarget] = useState<ChatMessage | null>(null)
  const [reportReason, setReportReason] = useState<string | null>(null)
  const [reportSubmitted, setReportSubmitted] = useState(false)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  const handleSend = () => {
    if (!input.trim()) return
    const newMsg: ChatMessage = {
      id: `m${Date.now()}`,
      userId: "me",
      nickname: "@나",
      content: input.trim(),
      timestamp: new Date().toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" }),
      isHighlighted: false,
      likeCount: 0,
    }
    setMessages((prev) => [...prev, newMsg])
    setInput("")
  }

  const handleLike = (msgId: string) => {
    const alreadyLiked = likedIds.has(msgId)
    setLikedIds((prev) => {
      const next = new Set(prev)
      alreadyLiked ? next.delete(msgId) : next.add(msgId)
      return next
    })
    setMessages((prev) =>
      prev.map((m) =>
        m.id === msgId
          ? { ...m, likeCount: (m.likeCount ?? 0) + (alreadyLiked ? -1 : 1) }
          : m
      )
    )
  }

  const handleReportClose = () => {
    setReportTarget(null)
    setTimeout(() => {
      setReportReason(null)
      setReportSubmitted(false)
    }, 300)
  }

  const handleReportSubmit = () => {
    if (!reportReason) return
    setReportSubmitted(true)
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      {/* Header */}
      <div className="shrink-0 flex items-center justify-between border-b border-border/50 px-4 py-3">
        <div className="flex items-center gap-2">
          <MessageSquare className="size-4 text-primary" />
          <span className="text-sm font-semibold text-foreground">실시간 채팅</span>
        </div>
        <Badge variant="outline" className="border-primary/30 text-primary text-[10px]">
          {messages.length}개
        </Badge>
      </div>

      {/* Messages — native scroll so new messages push up correctly */}
      <div className="min-h-0 flex-1 overflow-y-auto px-3 py-2">
        <div className="flex flex-col gap-2">
          {messages.map((msg) => {
            const isLiked = likedIds.has(msg.id)
            return (
              <div
                key={msg.id}
                className={cn(
                  "group flex gap-2 rounded-lg px-2 py-1.5 transition-colors hover:bg-muted/40",
                  msg.isHighlighted && "border border-primary/20 bg-primary/5"
                )}
              >
                <Avatar className="size-6 shrink-0 mt-0.5">
                  <AvatarFallback
                    className={cn("text-[9px] font-bold", getAvatarColor(msg.userId))}
                  >
                    {msg.nickname.slice(1, 3).toUpperCase()}
                  </AvatarFallback>
                </Avatar>

                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline gap-1.5">
                    <span
                      className={cn(
                        "text-[11px] font-semibold",
                        msg.userId === "me" ? "text-primary" : "text-muted-foreground"
                      )}
                    >
                      {msg.nickname}
                    </span>
                    <span className="text-[10px] text-muted-foreground/50">{msg.timestamp}</span>
                    {msg.isHighlighted && (
                      <Pin className="size-2.5 text-primary" />
                    )}
                  </div>
                  <p className="break-words text-xs leading-relaxed text-foreground">
                    {msg.content}
                  </p>
                </div>

                {/* Hover actions: like + report */}
                <div className="mt-0.5 flex shrink-0 items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
                  {/* Like */}
                  <button
                    onClick={() => handleLike(msg.id)}
                    className={cn(
                      "flex items-center gap-1 rounded px-1.5 py-0.5 text-[10px] transition-colors",
                      isLiked
                        ? "text-rose-500"
                        : "text-muted-foreground hover:text-rose-400"
                    )}
                    aria-label="좋아요"
                  >
                    <Heart
                      className={cn(
                        "size-3 transition-transform",
                        isLiked ? "fill-rose-500 scale-110" : ""
                      )}
                    />
                    {(msg.likeCount ?? 0) > 0 && (
                      <span className="font-medium">{msg.likeCount}</span>
                    )}
                  </button>

                  {/* Report — only show for others' messages */}
                  {msg.userId !== "me" && (
                    <button
                      onClick={() => setReportTarget(msg)}
                      className="flex size-5 items-center justify-center rounded text-muted-foreground/60 hover:text-destructive hover:bg-muted transition-colors"
                      aria-label="메시지 신고"
                    >
                      <Flag className="size-3" />
                    </button>
                  )}
                </div>
              </div>
            )
          })}
          <div ref={bottomRef} />
        </div>
      </div>

      {/* Input */}
      <div className="shrink-0 border-t border-border/50 p-3">
        <div className="flex items-center gap-2">
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && !e.shiftKey && handleSend()}
            placeholder="메시지 입력..."
            className="h-9 bg-muted border-border/50 text-xs"
          />
          <Button
            size="icon"
            className="size-9 shrink-0"
            onClick={handleSend}
            disabled={!input.trim()}
          >
            <Send className="size-4" />
            <span className="sr-only">전송</span>
          </Button>
        </div>
        <p className="mt-1.5 text-[10px] text-muted-foreground/50 text-center">
          커뮤니티 규칙을 준수하여 예의 바른 토론에 참여해주세요
        </p>
      </div>

      {/* Chat report modal */}
      <Dialog open={!!reportTarget} onOpenChange={handleReportClose}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-sm">
              <Flag className="size-4 text-destructive" />
              채팅 메시지 신고
            </DialogTitle>
            <DialogDescription className="text-xs">
              <span className="font-semibold text-foreground">{reportTarget?.nickname}</span>
              의 메시지를 신고합니다. 메시지 내용과 사용자 ID가 함께 접수됩니다.
            </DialogDescription>
          </DialogHeader>

          {/* Message preview */}
          {reportTarget && !reportSubmitted && (
            <div className="rounded-md border border-border bg-muted/50 px-3 py-2 text-xs text-muted-foreground italic">
              &ldquo;{reportTarget.content}&rdquo;
            </div>
          )}

          {reportSubmitted ? (
            <div className="flex flex-col items-center gap-3 py-4">
              <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
                <Flag className="size-5 text-primary" />
              </div>
              <p className="text-sm font-semibold text-foreground">신고가 접수되었습니다</p>
              <p className="text-xs text-muted-foreground text-center">
                검토 후 커뮤니티 가이드라인에 따라 처리됩니다.
              </p>
              <Button size="sm" className="mt-1 w-full" onClick={handleReportClose}>
                확인
              </Button>
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              <p className="text-xs font-medium text-foreground">신고 사유를 선택하세요</p>
              <div className="flex flex-col gap-1.5">
                {REPORT_REASONS.map((reason) => (
                  <button
                    key={reason}
                    onClick={() => setReportReason(reason)}
                    className={`rounded-lg border px-3 py-2.5 text-left text-xs transition-colors ${
                      reportReason === reason
                        ? "border-primary/50 bg-primary/10 text-primary font-medium"
                        : "border-border text-foreground hover:border-border/80 hover:bg-muted/60"
                    }`}
                  >
                    {reason}
                  </button>
                ))}
              </div>
              <div className="flex gap-2 pt-1">
                <Button
                  variant="outline"
                  size="sm"
                  className="flex-1 text-xs"
                  onClick={handleReportClose}
                >
                  취소
                </Button>
                <Button
                  size="sm"
                  variant="destructive"
                  className="flex-1 text-xs"
                  disabled={!reportReason}
                  onClick={handleReportSubmit}
                >
                  신고하기
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
