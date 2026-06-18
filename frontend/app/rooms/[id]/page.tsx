"use client"

import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { ArrowLeft, CalendarClock, Loader2, Radio, Users, type LucideIcon } from "lucide-react"
import { ChatPanel } from "@/components/chat-panel"
import { MainStage } from "@/components/main-stage"
import { Navbar } from "@/components/navbar"
import { useAuth } from "@/components/auth-provider"
import { ApiError } from "@/lib/api/client"
import { roomApi, topicApi } from "@/lib/api/services"
import type { RoomDetail, RoomParticipant, TopicDetail } from "@/lib/api/types"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"

export default function RoomDetailPage() {
  const params = useParams<{ id: string }>()
  const roomId = Number(params.id)
  const router = useRouter()
  const { user, loading: authLoading } = useAuth()
  const [room, setRoom] = useState<RoomDetail | null>(null)
  const [topic, setTopic] = useState<TopicDetail | null>(null)
  const [participants, setParticipants] = useState<RoomParticipant[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [joinError, setJoinError] = useState("")

  useEffect(() => {
    if (!Number.isSafeInteger(roomId) || roomId <= 0) {
      router.replace("/rooms")
    }
  }, [roomId, router])

  useEffect(() => {
    async function load() {
      setLoading(true)
      setError("")
      try {
        const roomDetail = await roomApi.detail(roomId)
        setRoom(roomDetail)
        const [topicDetail, participantList] = await Promise.all([
          topicApi.detail(roomDetail.topicId).catch(() => null),
          roomApi.participants(roomId).catch(() => []),
        ])
        setTopic(topicDetail)
        setParticipants(participantList)
      } catch {
        setError("토론방 정보를 불러오지 못했습니다.")
      } finally {
        setLoading(false)
      }
    }

    if (Number.isSafeInteger(roomId) && roomId > 0) void load()
  }, [roomId])

  useEffect(() => {
    if (authLoading || !user || !room || room.status !== "OPEN") return

    roomApi.join(roomId).catch((requestError) => {
      if (requestError instanceof ApiError && requestError.code === "ROOM_ALREADY_PARTICIPATED") return
      setJoinError(requestError instanceof ApiError ? requestError.message : "토론방 입장에 실패했습니다.")
    })
  }, [authLoading, room, roomId, user])

  const activeParticipants = useMemo(
    () => participants.filter((participant) => participant.status === "ACTIVE"),
    [participants],
  )

  return (
    <div className="flex flex-col bg-background" style={{ height: "100dvh" }}>
      <Navbar />

      <div className="shrink-0 border-b border-border/50 bg-background/95 backdrop-blur-md">
        <div className="mx-auto flex max-w-7xl items-center gap-3 px-4 py-3 md:px-6">
          <Link href="/rooms">
            <Button variant="ghost" size="icon" className="size-8 shrink-0">
              <ArrowLeft className="size-4" />
              <span className="sr-only">뒤로</span>
            </Button>
          </Link>
          <div className="flex min-w-0 flex-1 items-center gap-2">
            <Badge className="shrink-0 border-primary/30 bg-primary/20 text-primary text-[11px]">
              {room?.status ?? "ROOM"}
            </Badge>
            <h1 className="truncate text-sm font-semibold text-foreground">{room?.title ?? "토론방"}</h1>
          </div>
          <div className="hidden shrink-0 items-center gap-1 text-xs text-muted-foreground sm:flex">
            <Users className="size-3.5" />
            {activeParticipants.length.toLocaleString()}명
          </div>
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto lg:overflow-hidden">
        <div className="mx-auto flex h-full min-h-0 w-full max-w-7xl flex-col px-4 py-4 md:px-6">
          {loading ? (
            <div className="flex flex-1 items-center justify-center">
              <Loader2 className="size-6 animate-spin text-primary" />
            </div>
          ) : error || !room ? (
            <div className="flex flex-1 flex-col items-center justify-center gap-3 text-center">
              <p className="text-sm text-destructive">{error || "토론방을 찾을 수 없습니다."}</p>
              <Link href="/rooms">
                <Button variant="outline">목록으로</Button>
              </Link>
            </div>
          ) : (
            <>
              {joinError && <p className="mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{joinError}</p>}

              <div className="flex min-h-0 flex-col gap-6 lg:flex-1 lg:flex-row">
                <aside className="w-full shrink-0 lg:w-72 lg:overflow-y-auto">
                  <div className="rounded-xl border border-border/50 bg-card p-4">
                    <Badge variant="outline" className="mb-2 border-primary/30 text-primary text-[10px]">
                      {topic?.category ?? "토론"}
                    </Badge>
                    <h2 className="mb-2 text-sm font-semibold leading-snug text-foreground">{topic?.title ?? room.title}</h2>
                    <p className="mb-3 text-xs leading-relaxed text-muted-foreground">
                      {topic?.description ?? "상세 설명이 없습니다."}
                    </p>

                    <Separator className="mb-3" />

                    <div className="grid grid-cols-2 gap-2 text-center">
                      <Metric icon={Users} label="참여자" value={`${activeParticipants.length}명`} />
                      <Metric icon={CalendarClock} label="시작" value={new Date(room.startedAt).toLocaleDateString("ko-KR")} />
                    </div>

                    <Separator className="my-3" />

                    <div>
                      <div className="mb-2 flex items-center justify-between">
                        <span className="text-xs font-semibold text-foreground">참여자</span>
                        <span className="text-[11px] text-muted-foreground">{activeParticipants.length}명</span>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {activeParticipants.slice(0, 12).map((participant) => (
                          <Avatar key={participant.roomParticipantId} className="size-7">
                            <AvatarFallback className="text-[9px]">U{participant.userId}</AvatarFallback>
                          </Avatar>
                        ))}
                      </div>
                    </div>
                  </div>
                </aside>

                <div className="min-h-0 flex-1 lg:flex lg:flex-col">
                  <div className="min-h-0 overflow-hidden rounded-xl border border-border/50 bg-card lg:flex lg:flex-1 lg:flex-col">
                    <div className="block lg:hidden">
                      <Tabs defaultValue="stage">
                        <TabsList className="h-auto w-full rounded-none border-b border-border/50 bg-transparent p-0">
                          <TabsTrigger value="stage" className="flex-1 rounded-none border-b-2 border-transparent py-3 text-xs data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary">
                            <Radio className="mr-1.5 size-3.5" />
                            스테이지
                          </TabsTrigger>
                          <TabsTrigger value="chat" className="flex-1 rounded-none border-b-2 border-transparent py-3 text-xs data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary">
                            채팅
                          </TabsTrigger>
                        </TabsList>
                        <TabsContent value="stage" className="m-0 h-[70vh] min-h-[500px]">
                          <MainStage roomId={roomId} />
                        </TabsContent>
                        <TabsContent value="chat" className="m-0 h-[70vh] min-h-[500px]">
                          <ChatPanel roomId={roomId} />
                        </TabsContent>
                      </Tabs>
                    </div>

                    <div className="hidden min-h-0 lg:flex lg:flex-1">
                      <div className="min-h-0 flex-1 border-r border-border/50">
                        <MainStage roomId={roomId} />
                      </div>
                      <div className="min-h-0 w-80 xl:w-96">
                        <ChatPanel roomId={roomId} />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

function Metric({ icon: Icon, label, value }: { icon: LucideIcon; label: string; value: string }) {
  return (
    <div className="flex flex-col items-center gap-0.5 rounded-lg bg-muted p-3">
      <Icon className="size-3.5 text-muted-foreground" />
      <span className="text-sm font-bold text-foreground">{value}</span>
      <span className="text-[10px] text-muted-foreground">{label}</span>
    </div>
  )
}
