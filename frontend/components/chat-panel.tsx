"use client"

import { useEffect, useRef, useState } from "react"
import { ApiError } from "@/lib/api/client"
import { chatApi } from "@/lib/api/services"
import { subscribeRoomChat } from "@/lib/api/stomp"
import type { ChatEvent, ChatMessage as ApiChatMessage } from "@/lib/api/types"
import { useAuth } from "@/components/auth-provider"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import { MessageSquare, Send, Trash2 } from "lucide-react"

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

function eventToViewMessage(event: ChatEvent): ChatViewMessage | null {
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

export function ChatPanel({ roomId }: { roomId: number }) {
  const { user } = useAuth()
  const [messages, setMessages] = useState<ChatViewMessage[]>([])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(true)
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState("")
  const [deletingId, setDeletingId] = useState("")
  const bottomRef = useRef<HTMLDivElement>(null)
  const chatRef = useRef<ReturnType<typeof subscribeRoomChat> | null>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  useEffect(() => {
    let mounted = true
    setLoading(true)
    setError("")

    chatApi.messages(roomId)
      .then((response) => {
        if (!mounted) return
        setMessages(response.items.map(toViewMessage))
      })
      .catch((requestError) => {
        if (!mounted) return
        setError(messageOf(requestError))
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })

    const subscription = subscribeRoomChat(roomId, {
      onStatus: (nextConnected) => {
        if (mounted) setConnected(nextConnected)
      },
      onError: (message) => {
        if (mounted) setError(message)
      },
      onEvent: (event) => {
        if (!mounted) return
        if (event.type === "MESSAGE_DELETED") {
          setMessages((prev) => prev.filter((message) => message.id !== String(event.messageId)))
          return
        }

        const nextMessage = eventToViewMessage(event)
        if (!nextMessage) return
        setMessages((prev) => {
          if (prev.some((message) => message.id === nextMessage.id)) return prev
          return [...prev, nextMessage]
        })
      },
    })

    chatRef.current = subscription

    return () => {
      mounted = false
      subscription.disconnect()
      if (chatRef.current === subscription) chatRef.current = null
    }
  }, [roomId])

  const handleSend = () => {
    if (!input.trim()) return
    const sent = chatRef.current?.send(input.trim())
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
      setMessages((prev) => prev.filter((message) => message.id !== messageId))
    } catch (requestError) {
      setError(messageOf(requestError))
    } finally {
      setDeletingId("")
    }
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="shrink-0 flex items-center justify-between border-b border-border/50 px-4 py-3">
        <div className="flex items-center gap-2">
          <MessageSquare className="size-4 text-primary" />
          <span className="text-sm font-semibold text-foreground">실시간 채팅</span>
        </div>
        <Badge variant="outline" className={cn("text-[10px]", connected ? "border-primary/30 text-primary" : "border-border text-muted-foreground")}>
          {connected ? `${messages.length}개` : "연결 중"}
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

                {isMine && (
                  <button
                    onClick={() => deleteMessage(message.id)}
                    disabled={deletingId === message.id}
                    className="mt-0.5 flex size-6 shrink-0 items-center justify-center rounded text-muted-foreground/60 opacity-0 transition-opacity hover:bg-muted hover:text-destructive group-hover:opacity-100 disabled:opacity-50"
                    aria-label="메시지 삭제"
                  >
                    <Trash2 className="size-3" />
                  </button>
                )}
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
            disabled={!input.trim() || !connected}
          >
            <Send className="size-4" />
            <span className="sr-only">전송</span>
          </Button>
        </div>
        <p className="mt-1.5 text-[10px] text-muted-foreground/50 text-center">
          커뮤니티 규칙을 준수하여 예의 바른 토론에 참여해주세요
        </p>
      </div>
    </div>
  )
}
