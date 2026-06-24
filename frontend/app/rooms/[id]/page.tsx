"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Separator } from "@/components/ui/separator"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Navbar } from "@/components/navbar"
import { MainStage } from "@/components/main-stage"
import { ChatPanel } from "@/components/chat-panel"
import { useAuth } from "@/components/auth-provider"
import { roomApi, topicApi } from "@/lib/api/services"
import { ApiError } from "@/lib/api/client"
import { createRoomStompConnection, type RoomStompConnection } from "@/lib/api/stomp"
import type { RoomEvent, RoomParticipant, RoomParticipantEvent, UserSanctionEvent } from "@/lib/api/types"
import {
  ArrowLeft,
  Users,
  MessageSquare,
  Zap,
  LogOut,
} from "lucide-react"

type RoomView = {
  id: string
  title: string
  description: string
  category: string
  status: "OPEN" | "CLOSED"
  tags: string[]
  isLive: boolean
}

function ChatUnavailable() {
  return (
    <div className="flex h-full min-h-0 flex-col items-center justify-center gap-2 px-4 text-center text-muted-foreground">
      <MessageSquare className="size-6" />
      <p className="text-sm font-medium text-foreground">채팅 연결 대기 중</p>
      <p className="text-xs">토론방 입장이 완료되면 실시간 채팅이 연결됩니다.</p>
    </div>
  )
}

export default function RoomDetailPage() {
  const params = useParams<{ id: string }>()
  const roomId = Number(params.id)
  const router = useRouter()
  const { user, loading: authLoading } = useAuth()
  const [joinError, setJoinError] = useState("")
  const [joined, setJoined] = useState(false)
  const [roomView, setRoomView] = useState<RoomView | null>(null)
  const [participantCount, setParticipantCount] = useState(0)
  const [participants, setParticipants] = useState<RoomParticipant[]>([])
  const [leaving, setLeaving] = useState(false)
  const [stompConnection, setStompConnection] = useState<RoomStompConnection | null>(null)
  const [stompConnected, setStompConnected] = useState(false)

  useEffect(() => {
    if (!Number.isSafeInteger(roomId) || roomId <= 0) {
      router.replace("/rooms")
      return
    }
    if (authLoading) return
    if (!user) {
      setJoined(false)
      router.replace(`/login?redirect=${encodeURIComponent(`/rooms/${roomId}`)}`)
      return
    }

    async function loadRoom() {
      setJoinError("")
      try {
        const room = await roomApi.detail(roomId)
        const topicDetail = await topicApi.detail(room.topicId)
        setRoomView({
          id: String(room.roomId),
          title: room.title,
          description: topicDetail.description ?? "승인된 토픽으로 개설된 실시간 토론방입니다.",
          category: topicDetail.category,
          status: room.status,
          tags: [topicDetail.category],
          isLive: room.status === "OPEN",
        })
      } catch (error) {
        setJoinError(error instanceof ApiError ? error.message : "토론방 정보를 불러오지 못했습니다.")
      }
    }

    async function joinRoom() {
      try {
        await roomApi.join(roomId)
        setJoined(true)
      } catch (error) {
        if (error instanceof ApiError && error.code === "ROOM_ALREADY_PARTICIPATED") {
          setJoined(true)
          return
        }
        setJoined(false)
        setJoinError(error instanceof ApiError ? error.message : "토론방 입장에 실패했습니다.")
      }
    }

    async function loadParticipantCount() {
      try {
        const [countResponse, participantResponse] = await Promise.all([
          roomApi.participantCount(roomId),
          roomApi.participants(roomId),
        ])
        setParticipantCount(countResponse.participantCount)
        setParticipants(participantResponse)
      } catch {
      }
    }

    void loadRoom()
    void joinRoom().then(loadParticipantCount)
  }, [authLoading, roomId, router, user])

  useEffect(() => {
    if (!joined) return

    const connection = createRoomStompConnection(roomId, {
      onStatus: setStompConnected,
      onError: (message) => setJoinError(message),
    })
    setStompConnection(connection)
    connection.connect()

    return () => {
      setStompConnection(null)
      setStompConnected(false)
      connection.disconnect()
    }
  }, [joined, roomId])

  useEffect(() => {
    if (!stompConnection || !stompConnected) return

    const unsubscribeParticipants = stompConnection.subscribe<RoomParticipantEvent | RoomEvent>(
      `/topic/rooms/${roomId}/participants/events`,
      (event) => {
        if (event.eventType === "PARTICIPANT_JOINED" || event.eventType === "PARTICIPANT_LEFT") {
          setParticipantCount(event.data.participantCount)
        }
      },
      setJoinError,
    )
    const unsubscribeRoom = stompConnection.subscribe<RoomParticipantEvent | RoomEvent>(
      `/topic/rooms/${roomId}/room/events`,
      (event) => {
        if (event.eventType !== "ROOM_CLOSED") return
        setRoomView((prev) => prev && ({
          ...prev,
          status: "CLOSED",
          isLive: false,
        }))
      },
      setJoinError,
    )
    const unsubscribeSanctions = user
      ? stompConnection.subscribe<UserSanctionEvent>(
        `/topic/users/${user.userId}/sanctions/events`,
        (event) => {
          const action = event.eventType === "SANCTION_REVOKED" ? "해제" : "변경"
          setJoinError(`사용자 제재 상태가 ${action}되었습니다. 필요한 경우 요청을 다시 시도해주세요.`)
        },
        setJoinError,
      )
      : () => {}

    return () => {
      unsubscribeParticipants()
      unsubscribeRoom()
      unsubscribeSanctions()
    }
  }, [roomId, stompConnected, stompConnection, user])

  const leaveRoom = async () => {
    setLeaving(true)
    setJoinError("")
    try {
      await roomApi.leave(roomId)
      router.push("/rooms")
    } catch (error) {
      setJoinError(error instanceof ApiError ? error.message : "토론방 나가기에 실패했습니다.")
    } finally {
      setLeaving(false)
    }
  }

  return (
    <div className="flex flex-col bg-background" style={{ height: "100dvh" }}>
      <Navbar />

      {/* Room top bar */}
      <div className="shrink-0 border-b border-border/50 bg-background/95 backdrop-blur-md">
        <div className="mx-auto flex max-w-7xl items-center gap-3 px-4 py-3 md:px-6">
          <Link href="/rooms">
            <Button variant="ghost" size="icon" className="size-8 shrink-0">
              <ArrowLeft className="size-4" />
              <span className="sr-only">뒤로</span>
            </Button>
          </Link>

          <div className="flex min-w-0 flex-1 items-center gap-2">
            <Badge className={`gap-1.5 shrink-0 text-[11px] ${roomView?.status === "OPEN"
              ? "bg-primary/20 text-primary border-primary/30"
              : "bg-muted text-muted-foreground border-border"
              }`}>
              <span className={`size-1.5 rounded-full ${roomView?.status === "OPEN" ? "bg-primary animate-live-pulse" : "bg-muted-foreground"}`} />
              {roomView?.status === "OPEN" ? "LIVE" : "CLOSED"}
            </Badge>
            <h1 className="truncate text-sm font-semibold text-foreground">
              {roomView?.title ?? "토론방"}
            </h1>
          </div>

          <div className="flex shrink-0 items-center gap-3 text-xs text-muted-foreground">
            <span className="hidden items-center gap-1 sm:flex">
              <Users className="size-3.5" />
              {participantCount.toLocaleString()}
            </span>
            <Button variant="outline" size="sm" className="h-8 gap-1.5 text-xs" disabled={!joined || leaving} onClick={leaveRoom}>
              <LogOut className="size-3.5" />
              나가기
            </Button>
          </div>
        </div>
      </div>

      {/* Body — fills remaining space, scrollable on mobile, fixed on desktop */}
      <div className="min-h-0 flex-1 overflow-y-auto lg:overflow-hidden">
        <div className="mx-auto flex h-full min-h-0 w-full max-w-7xl flex-col px-4 py-4 md:px-6 lg:py-4">
          {joinError && (user || !roomView) && (
            <p className="mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{joinError}</p>
          )}

          {/* 3-column layout on desktop */}
          <div className="flex min-h-0 flex-col gap-6 lg:flex-1 lg:flex-row">
            {/* LEFT: Topic info panel */}
            <aside className="w-full shrink-0 lg:w-64 xl:w-72 lg:overflow-y-auto">
              <div className="flex flex-col gap-4">
                {/* Topic card */}
                <div className="rounded-xl border border-border/50 bg-card p-4">
                  <Badge
                    variant="outline"
                    className="mb-2 border-primary/30 text-primary text-[10px]"
                  >
                    {roomView?.category ?? "토론"}
                  </Badge>
                  <h2 className="mb-2 text-sm font-semibold leading-snug text-foreground">
                    {roomView?.title ?? "토론방 정보를 불러오는 중..."}
                  </h2>
                  <p className="mb-3 text-xs leading-relaxed text-muted-foreground">
                    {roomView?.description ?? "잠시만 기다려 주세요."}
                  </p>

                  <Separator className="mb-3" />

                  <div className="flex items-center justify-center gap-2 text-center">
                    <Users className="size-3.5 text-muted-foreground" />
                    <span className="text-sm font-bold text-foreground">{participantCount.toLocaleString()}</span>
                    <span className="text-[10px] text-muted-foreground">참여자</span>
                  </div>

                  {roomView?.tags && roomView.tags.length > 0 && (
                    <>
                      <Separator className="my-3" />
                      <div className="flex flex-wrap gap-1">
                        {roomView.tags.map((tag) => (
                          <span
                            key={tag}
                            className="rounded-full bg-muted px-2 py-0.5 text-[10px] text-muted-foreground"
                          >
                            #{tag}
                          </span>
                        ))}
                      </div>
                    </>
                  )}
                </div>

                {/* Participants preview */}
                <div className="rounded-xl border border-border/50 bg-card p-4">
                  <div className="mb-3 flex items-center justify-between">
                    <span className="text-xs font-semibold text-foreground">참여자</span>
                    <span className="text-[11px] text-muted-foreground">
                      {participantCount.toLocaleString()}명
                    </span>
                  </div>
                  <div className="flex items-center gap-1">
                    {participants.slice(0, 5).map((participant, idx) => (
                      <Avatar
                        key={participant.roomParticipantId}
                        className="size-7 border-2 border-background"
                        style={{ marginLeft: idx > 0 ? "-8px" : "0" }}
                      >
                        <AvatarFallback className="bg-muted text-[9px] font-bold text-muted-foreground">
                          U{participant.userId}
                        </AvatarFallback>
                      </Avatar>
                    ))}
                    {participantCount > 5 && (
                      <span className="ml-2 text-[11px] text-muted-foreground">
                        +{(participantCount - 5).toLocaleString()}명
                      </span>
                    )}
                  </div>
                </div>
              </div>
            </aside>

            {/* CENTER: Main Stage */}
            <div className="min-h-0 flex-1 lg:flex lg:flex-col">
              <div className="min-h-0 rounded-xl border border-border/50 bg-card overflow-hidden lg:flex-1 lg:flex lg:flex-col">                {/* Mobile tab view */}
                <div className="block lg:hidden">
                  <Tabs defaultValue="stage">
                    <TabsList className="w-full rounded-none border-b border-border/50 bg-transparent p-0 h-auto">
                      <TabsTrigger
                        value="stage"
                        className="flex-1 rounded-none border-b-2 border-transparent py-3 text-xs data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary"
                      >
                        <Zap className="size-3.5 mr-1.5" />
                        Main Stage
                      </TabsTrigger>
                      <TabsTrigger
                        value="chat"
                        className="flex-1 rounded-none border-b-2 border-transparent py-3 text-xs data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary"
                      >
                        <MessageSquare className="size-3.5 mr-1.5" />
                        채팅
                      </TabsTrigger>
                    </TabsList>
                    <TabsContent value="stage" className="m-0 h-[70vh] min-h-[500px]">
                      <MainStage
                        roomId={roomId}
                        liveEnabled={joined}
                        stompConnection={stompConnection}
                        stompConnected={stompConnected}
                      />
                    </TabsContent>
                    <TabsContent value="chat" className="m-0 h-[70vh] min-h-[500px]">
                      {joined ? (
                        <ChatPanel
                          roomId={roomId}
                          stompConnection={stompConnection}
                          stompConnected={stompConnected}
                        />
                      ) : <ChatUnavailable />}
                    </TabsContent>
                  </Tabs>
                </div>

                {/* Desktop: Side by side, fills remaining height */}
                <div className="hidden min-h-0 lg:flex lg:flex-1">
                  <div className="min-h-0 flex-1 border-r border-border/50">
                    <MainStage
                      roomId={roomId}
                      liveEnabled={joined}
                      stompConnection={stompConnection}
                      stompConnected={stompConnected}
                    />
                  </div>
                  <div className="min-h-0 w-80 xl:w-96">
                    {joined ? (
                      <ChatPanel
                        roomId={roomId}
                        stompConnection={stompConnection}
                        stompConnected={stompConnected}
                      />
                    ) : <ChatUnavailable />}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>{/* max-w-7xl inner */}
      </div>{/* body scroll wrapper */}
    </div>
  )
}
