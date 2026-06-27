"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { ApiError } from "@/lib/api/client"
import { chatApi } from "@/lib/api/services"
import type { RealtimeStatus, RoomStompConnection } from "@/lib/api/stomp"
import type { ChatEvent, ChatMessageEventPayload, ChatMessage as ApiChatMessage, ChatReportReason } from "@/lib/api/types"
import { useAuth } from "@/components/auth-provider"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { cn } from "@/lib/utils"
import { Flag, Loader2, MessageSquare, Send, Trash2 } from "lucide-react"

interface ChatViewMessage {
  id: string
  userId: number
  nickname: string
  content: string
  timestamp: string
}

const avatarColors = [
  "bg-primary/15 text-primary",
  "bg-violet-100 text-violet-700 dark:bg-violet-500/20 dark:text-violet-400",
  "bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-400",
  "bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400",
  "bg-red-100 text-red-600 dark:bg-red-500/20 dark:text-red-400",
  "bg-pink-100 text-pink-700 dark:bg-pink-500/20 dark:text-pink-400",
]

const REPORT_REASONS: { value: ChatReportReason; label: string }[] = [
  { value: "ABUSE_HARASSMENT", label: "욕설 / 괴롭힘" },
  { value: "HATE_SPEECH", label: "혐오 발언" },
  { value: "SEXUAL_CONTENT", label: "성적 콘텐츠" },
  { value: "THREAT_VIOLENCE", label: "위협 / 폭력" },
  { value: "SPAM", label: "광고 / 스팸" },
  { value: "MISINFORMATION", label: "허위 정보" },
  { value: "PRIVACY_VIOLATION", label: "개인정보 노출" },
  { value: "OFF_TOPIC", label: "주제 무관" },
  { value: "OTHER", label: "기타" },
]

function getAvatarColor(userId: number) {
  return avatarColors[userId % avatarColors.length]
}

function messageOf(error: unknown) {
  return error instanceof ApiError ? error.message : "채팅 요청 처리 중 오류가 발생했습니다."
}

function toViewMessage(message: ApiChatMessage): ChatViewMessage {
  return {
    id: String(message.messageId),
    userId: message.userId,
    nickname: message.nicknameSnapshot ? `@${message.nicknameSnapshot}` : `@user_${message.userId}`,
    content: message.content,
    timestamp: new Date(message.createdAt).toLocaleTimeString("ko-KR", {
      hour: "2-digit",
      minute: "2-digit",
    }),
  }
}

function eventToViewMessage(event: ChatMessageEventPayload): ChatViewMessage | null {
  if (!event.content) return null
  return toViewMessage({
    messageId: event.messageId,
    roomId: event.roomId,
    userId: event.userId,
    nicknameSnapshot: event.nicknameSnapshot,
    content: event.content,
    createdAt: event.createdAt,
  })
}

export function ChatPanel({
  roomId,
  stompConnection,
  stompConnected,
  realtimeStatus,
  recoveryKey,
}: {
  roomId: number
  stompConnection: RoomStompConnection | null
  stompConnected: boolean
  realtimeStatus: RealtimeStatus
  recoveryKey: number
}) {
  const { user } = useAuth()
  const [messages, setMessages] = useState<ChatViewMessage[]>([])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [deletingId, setDeletingId] = useState("")
  const [reportTarget, setReportTarget] = useState<ChatViewMessage | null>(null)
  const [reportReason, setReportReason] = useState<ChatReportReason | null>(null)
  const [reportDescription, setReportDescription] = useState("")
  const [reportSubmitting, setReportSubmitting] = useState(false)
  const [reportSubmitted, setReportSubmitted] = useState(false)
  const [reportError, setReportError] = useState("")
  const bottomRef = useRef<HTMLDivElement>(null)
  const handledEventIdsRef = useRef<string[]>([])
  const deletedMessageIdsRef = useRef<Set<string>>(new Set())
  const messageRequestSeqRef = useRef(0)

  const rememberEvent = (eventId: string) => {
    if (handledEventIdsRef.current.includes(eventId)) return false
    handledEventIdsRef.current = [...handledEventIdsRef.current.slice(-199), eventId]
    return true
  }

  const mergeMessages = useCallback((incoming: ChatViewMessage[]) => {
    setMessages((prev) => {
      const byId = new Map<string, ChatViewMessage>()
      for (const message of prev) {
        if (!deletedMessageIdsRef.current.has(message.id)) byId.set(message.id, message)
      }
      for (const message of incoming) {
        if (!deletedMessageIdsRef.current.has(message.id)) byId.set(message.id, message)
      }
      return Array.from(byId.values()).sort((left, right) => Number(left.id) - Number(right.id))
    })
  }, [])

  const loadLatestMessages = useCallback(async (showLoading: boolean) => {
    const requestSeq = ++messageRequestSeqRef.current
    if (showLoading) setLoading(true)
    setError("")
    try {
      const response = await chatApi.messages(roomId)
      if (requestSeq !== messageRequestSeqRef.current) return
      mergeMessages(response.items.map(toViewMessage))
    } catch (requestError) {
      if (requestSeq !== messageRequestSeqRef.current) return
      setError(messageOf(requestError))
    } finally {
      if (showLoading && requestSeq === messageRequestSeqRef.current) setLoading(false)
    }
  }, [mergeMessages, roomId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  useEffect(() => {
    setMessages([])
    handledEventIdsRef.current = []
    deletedMessageIdsRef.current.clear()
    messageRequestSeqRef.current = 0
    void loadLatestMessages(true)
  }, [loadLatestMessages, roomId])

  useEffect(() => {
    if (recoveryKey === 0) return
    void loadLatestMessages(false)
  }, [loadLatestMessages, recoveryKey])

  useEffect(() => {
    if (!stompConnection) return

    const unsubscribe = stompConnection.subscribe<ChatEvent>(
      `/topic/rooms/${roomId}/chat/events`,
      (event) => {
        if (!rememberEvent(event.eventId)) return
        if (event.eventType === "MESSAGE_DELETED") {
          deletedMessageIdsRef.current.add(String(event.data.messageId))
          setMessages((prev) => prev.filter((message) => message.id !== String(event.data.messageId)))
          return
        }

        const nextMessage = eventToViewMessage(event.data)
        if (!nextMessage) return
        mergeMessages([nextMessage])
      },
      setError,
    )

    return unsubscribe
  }, [mergeMessages, roomId, stompConnection])

  const handleSend = () => {
    if (!input.trim()) return
    const sent = stompConnection?.sendChat(input.trim())
    if (!sent) {
      setError("채팅 연결이 완료된 뒤 다시 전송해주세요.")
      return
    }
    setInput("")
    setError("")
  }

  const deleteMessage = async (messageId: string) => {
    setDeletingId(messageId)
    setError("")
    try {
      await chatApi.delete(roomId, Number(messageId))
      deletedMessageIdsRef.current.add(messageId)
      setMessages((prev) => prev.filter((message) => message.id !== messageId))
    } catch (requestError) {
      setError(messageOf(requestError))
    } finally {
      setDeletingId("")
    }
  }

  const openReport = (message: ChatViewMessage) => {
    setReportTarget(message)
    setReportReason(null)
    setReportDescription("")
    setReportSubmitted(false)
    setReportError("")
  }

  const closeReport = () => {
    setReportTarget(null)
    setReportReason(null)
    setReportDescription("")
    setReportSubmitted(false)
    setReportError("")
  }

  const submitReport = async () => {
    if (!reportTarget || !reportReason) return

    setReportSubmitting(true)
    setReportError("")
    try {
      await chatApi.report(roomId, Number(reportTarget.id), reportReason, reportDescription)
      setReportSubmitted(true)
    } catch (requestError) {
      setReportError(messageOf(requestError))
    } finally {
      setReportSubmitting(false)
    }
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="shrink-0 flex items-center justify-between border-b border-border/50 px-4 py-3">
        <div className="flex items-center gap-2">
          <MessageSquare className="size-4 text-primary" />
          <span className="text-sm font-semibold text-foreground">실시간 채팅</span>
        </div>
        <Badge variant="outline" className={cn("text-[10px]", stompConnected ? "border-primary/30 text-primary" : "border-border text-muted-foreground")}>
          {stompConnected ? `${messages.length}개` : realtimeStatus === "offline" ? "오프라인" : "연결 중"}
        </Badge>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto px-3 py-2">
        {error && <p className="mb-2 rounded-lg bg-destructive/10 px-3 py-2 text-[11px] text-destructive">{error}</p>}
        <div className="flex flex-col gap-2">
          {loading ? (
            <p className="px-2 py-6 text-center text-xs text-muted-foreground">채팅 내역을 불러오는 중...</p>
          ) : messages.length === 0 ? (
            <p className="px-2 py-6 text-center text-xs text-muted-foreground">아직 채팅 메시지가 없습니다.</p>
          ) : messages.map((message) => {
            const isMine = user?.userId === message.userId
            return (
              <div
                key={message.id}
                className="group flex gap-2 rounded-lg px-2 py-1.5 transition-colors hover:bg-muted/40"
              >
                <Avatar className="size-6 shrink-0 mt-0.5">
                  <AvatarFallback
                    className={cn("text-[9px] font-bold", getAvatarColor(message.userId))}
                  >
                    {message.nickname.slice(1, 3).toUpperCase()}
                  </AvatarFallback>
                </Avatar>

                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline gap-1.5">
                    <span className={cn("text-[11px] font-semibold", isMine ? "text-primary" : "text-muted-foreground")}>
                      {message.nickname}
                    </span>
                    <span className="text-[10px] text-muted-foreground/50">{message.timestamp}</span>
                  </div>
                  <p className="break-words text-xs leading-relaxed text-foreground">
                    {message.content}
                  </p>
                </div>

                <div className="mt-0.5 flex shrink-0 items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100">
                {isMine ? (
                  <button
                    onClick={() => deleteMessage(message.id)}
                    disabled={deletingId === message.id}
                    className="flex size-6 items-center justify-center rounded text-muted-foreground/60 hover:bg-muted hover:text-destructive disabled:opacity-50"
                    aria-label="메시지 삭제"
                  >
                    <Trash2 className="size-3" />
                  </button>
                ) : (
                  <button
                    onClick={() => openReport(message)}
                    className="flex size-6 items-center justify-center rounded text-muted-foreground/60 hover:bg-muted hover:text-destructive"
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

      <div className="shrink-0 border-t border-border/50 p-3">
        <div className="flex items-center gap-2">
          <Input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && !e.shiftKey && handleSend()}
            placeholder="메시지 입력..."
            maxLength={300}
            className="h-9 bg-muted border-border/50 text-xs"
          />
          <Button
            size="icon"
            className="size-9 shrink-0"
            onClick={handleSend}
            disabled={!input.trim() || !stompConnected}
          >
            <Send className="size-4" />
            <span className="sr-only">전송</span>
          </Button>
        </div>
        <p className="mt-1.5 text-[10px] text-muted-foreground/50 text-center">
          커뮤니티 규칙을 준수하여 예의 바른 토론에 참여해주세요
        </p>
      </div>

      <Dialog open={!!reportTarget} onOpenChange={(open) => !open && closeReport()}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Flag className="size-4 text-destructive" />
              채팅 메시지 신고
            </DialogTitle>
            <DialogDescription>신고 내용은 메시지 원문과 함께 운영팀에 전달됩니다.</DialogDescription>
          </DialogHeader>
          {reportSubmitted ? (
            <div className="flex flex-col gap-3 py-3 text-center">
              <p className="font-semibold">신고가 접수되었습니다.</p>
              <Button onClick={closeReport}>확인</Button>
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              {reportTarget && (
                <div className="rounded-lg border bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
                  <p className="font-medium text-foreground">{reportTarget.nickname}</p>
                  <p className="mt-1 line-clamp-3 whitespace-pre-wrap">{reportTarget.content}</p>
                </div>
              )}
              {reportError && <p className="rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{reportError}</p>}
              <div className="grid grid-cols-1 gap-1.5">
                {REPORT_REASONS.map((reason) => (
                  <button
                    key={reason.value}
                    onClick={() => setReportReason(reason.value)}
                    className={`rounded-lg border px-3 py-2 text-left text-xs ${reportReason === reason.value ? "border-primary bg-primary/10 text-primary" : "border-border"}`}
                  >
                    {reason.label}
                  </button>
                ))}
              </div>
              <textarea
                value={reportDescription}
                onChange={(event) => setReportDescription(event.target.value)}
                maxLength={500}
                rows={3}
                placeholder={reportReason === "OTHER" ? "기타 사유는 상세 설명이 필수입니다." : "상세 설명 (선택)"}
                className="resize-none rounded-lg border bg-background px-3 py-2 text-xs outline-none focus:border-primary"
              />
              <Button
                variant="destructive"
                disabled={reportSubmitting || !reportReason || (reportReason === "OTHER" && !reportDescription.trim())}
                onClick={submitReport}
              >
                {reportSubmitting && <Loader2 className="mr-2 size-4 animate-spin" />}
                신고하기
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
