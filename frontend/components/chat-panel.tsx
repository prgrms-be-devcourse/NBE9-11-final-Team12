"use client"

import { useEffect, useRef, useState } from "react"
import { Flag, Loader2, MessageSquare, RefreshCw, Send } from "lucide-react"
import { chatApi } from "@/lib/api/services"
import type { ChatMessage } from "@/lib/api/types"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"

function initials(nickname: string) {
  return nickname.replace("@", "").slice(0, 2).toUpperCase() || "U"
}

function toWebSocketUrl(baseUrl: string) {
  const url = new URL("/api/v1/ws", baseUrl)
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:"
  return url.toString()
}

function stompFrame(command: string, headers: Record<string, string> = {}, body = "") {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`)
  return `${command}\n${headerLines.join("\n")}\n\n${body}\0`
}

function parseStompMessages(raw: string) {
  return raw
    .split("\0")
    .map((frame) => frame.trim())
    .filter(Boolean)
    .map((frame) => {
      const [head, body = ""] = frame.split("\n\n")
      const [command, ...headerLines] = head.split("\n")
      const headers = Object.fromEntries(
        headerLines
          .map((line) => line.split(":"))
          .filter(([key, value]) => key && value !== undefined)
          .map(([key, ...rest]) => [key, rest.join(":")]),
      )
      return { command, headers, body }
    })
}

export function ChatPanel({ roomId }: { roomId: number }) {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState("")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [connected, setConnected] = useState(false)
  const socketRef = useRef<WebSocket | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  const loadMessages = async () => {
    setLoading(true)
    setError("")
    try {
      const response = await chatApi.list(roomId)
      setMessages(response.items)
    } catch {
      setError("채팅 메시지를 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadMessages()
  }, [roomId])

  useEffect(() => {
    const socket = new WebSocket(toWebSocketUrl(API_BASE_URL))
    socketRef.current = socket

    socket.onopen = () => {
      socket.send(stompFrame("CONNECT", { "accept-version": "1.2", "heart-beat": "10000,10000" }))
    }

    socket.onmessage = (event) => {
      for (const frame of parseStompMessages(String(event.data))) {
        if (frame.command === "CONNECTED") {
          setConnected(true)
          socket.send(
            stompFrame("SUBSCRIBE", {
              id: `chat-${roomId}`,
              destination: `/topic/rooms/${roomId}/chat/messages`,
              ack: "auto",
            }),
          )
        }

        if (frame.command === "MESSAGE" && frame.body) {
          try {
            const eventPayload = JSON.parse(frame.body) as ChatMessage & { type?: string }
            if (eventPayload.type === "MESSAGE_DELETED") {
              setMessages((prev) => prev.filter((message) => message.messageId !== eventPayload.messageId))
              continue
            }
            if (eventPayload.messageId && eventPayload.content) {
              setMessages((prev) => {
                if (prev.some((message) => message.messageId === eventPayload.messageId)) return prev
                return [...prev, eventPayload]
              })
            }
          } catch {
            // Ignore malformed frames and keep the existing chat stream.
          }
        }
      }
    }

    socket.onerror = () => {
      setConnected(false)
    }

    socket.onclose = () => {
      setConnected(false)
    }

    return () => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(stompFrame("DISCONNECT"))
      }
      socket.close()
      socketRef.current = null
    }
  }, [roomId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [messages])

  const sendMessage = () => {
    const content = input.trim()
    const socket = socketRef.current
    if (!content || !socket || socket.readyState !== WebSocket.OPEN || !connected) return

    socket.send(
      stompFrame(
        "SEND",
        {
          destination: `/app/rooms/${roomId}/chat/messages`,
          "content-type": "application/json",
        },
        JSON.stringify({ content }),
      ),
    )
    setInput("")
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="flex shrink-0 items-center justify-between border-b border-border/50 px-4 py-3">
        <div className="flex items-center gap-2">
          <MessageSquare className="size-4 text-primary" />
          <span className="text-sm font-semibold text-foreground">채팅</span>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant="outline" className="border-primary/30 text-primary text-[10px]">
            {messages.length}개
          </Badge>
          <span className={cn("size-2 rounded-full", connected ? "bg-emerald-500" : "bg-muted-foreground/40")} title={connected ? "연결됨" : "연결 대기"} />
          <Button variant="ghost" size="icon-sm" onClick={() => void loadMessages()} aria-label="채팅 새로고침">
            <RefreshCw className="size-3.5" />
          </Button>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto px-3 py-2">
        {loading ? (
          <div className="flex h-40 items-center justify-center">
            <Loader2 className="size-5 animate-spin text-primary" />
          </div>
        ) : error ? (
          <p className="rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{error}</p>
        ) : messages.length === 0 ? (
          <div className="flex h-40 items-center justify-center text-center text-xs text-muted-foreground">
            아직 채팅 메시지가 없습니다.
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            {messages.map((message) => (
              <div key={message.messageId} className="group flex gap-2 rounded-lg px-2 py-1.5 transition-colors hover:bg-muted/40">
                <Avatar className="mt-0.5 size-6 shrink-0">
                  <AvatarFallback className="bg-primary/10 text-[9px] font-bold text-primary">
                    {initials(message.nicknameSnapshot)}
                  </AvatarFallback>
                </Avatar>

                <div className="min-w-0 flex-1">
                  <div className="flex items-baseline gap-1.5">
                    <span className="text-[11px] font-semibold text-muted-foreground">{message.nicknameSnapshot}</span>
                    <span className="text-[10px] text-muted-foreground/50">
                      {new Date(message.createdAt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
                    </span>
                  </div>
                  <p className="break-words text-xs leading-relaxed text-foreground">{message.content}</p>
                </div>

                <button
                  className={cn(
                    "mt-0.5 flex size-5 shrink-0 items-center justify-center rounded text-muted-foreground/60 opacity-0 transition-opacity hover:bg-muted hover:text-destructive group-hover:opacity-100",
                  )}
                  aria-label="메시지 신고"
                >
                  <Flag className="size-3" />
                </button>
              </div>
            ))}
            <div ref={bottomRef} />
          </div>
        )}
      </div>

      <div className="shrink-0 border-t border-border/50 p-3">
        <div className="flex items-center gap-2">
          <Input
            value={input}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) sendMessage()
            }}
            placeholder={connected ? "메시지 입력" : "채팅 연결 중"}
            className="h-9 bg-muted text-xs"
            disabled={!connected}
          />
          <Button size="icon" className="size-9 shrink-0" onClick={sendMessage} disabled={!connected || !input.trim()}>
            <Send className="size-4" />
            <span className="sr-only">전송</span>
          </Button>
        </div>
      </div>
    </div>
  )
}
