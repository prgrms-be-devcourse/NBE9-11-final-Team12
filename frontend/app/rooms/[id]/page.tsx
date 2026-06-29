"use client"

import Link from "next/link"
import { useCallback, useEffect, useRef, useState } from "react"
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
import { roomApi, sanctionApi, topicApi, trustApi } from "@/lib/api/services"
import { ApiError } from "@/lib/api/client"
import { createRoomStompConnection, type RealtimeStatus, type RoomStompConnection } from "@/lib/api/stomp"
import type {
  ActiveUserSanction,
  RoomEvent,
  RoomParticipant,
  RoomParticipantEvent,
  RoomSyncState,
  UserSanctionEvent,
  UserTrustDetail,
  UserSanctionType,
} from "@/lib/api/types"
import {
  ArrowLeft,
  Users,
  MessageSquare,
  Zap,
  LogOut,
  ShieldCheck,
  Clock,
  ExternalLink,
} from "lucide-react"

type RoomView = {
  id: string
  title: string
  topicTitle: string
  sourceUrl: string | null
  category: string
  status: "OPEN" | "CLOSED"
  startedAt: string | null
  endedAt: string | null
  timeLabel: string
  tags: string[]
  isLive: boolean
}

function ChatUnavailable({ closed = false }: { closed?: boolean }) {
  return (
    <div className="flex h-full min-h-0 flex-col items-center justify-center gap-2 px-4 text-center text-muted-foreground">
      <MessageSquare className="size-6" />
      <p className="text-sm font-medium text-foreground">
        {closed ? "종료된 토론방입니다" : "채팅을 준비 중입니다"}
      </p>
      <p className="text-xs">
        {closed
          ? "종료된 토론방의 채팅 내역은 표시하지 않습니다."
          : "토론방 입장 후 실시간 채팅이 연결됩니다."}
      </p>
    </div>
  )
}

function trustLevelLabel(level: UserTrustDetail["trustLevel"]) {
  switch (level) {
    case "CAUTION":
      return "주의 필요"
    case "NORMAL":
      return "일반"
    case "RELIABLE":
      return "신뢰 높음"
    case "TRUSTED":
      return "매우 신뢰"
    default:
      return level
  }
}

function activityLevelLabel(level: UserTrustDetail["activityLevel"]) {
  switch (level) {
    case "NEW":
      return "새싹 참여자"
    case "ACTIVE":
      return "활동 참여자"
    case "CONTRIBUTOR":
      return "꾸준한 기여자"
    case "LEADER":
      return "토론 리더"
    default:
      return level
  }
}

function sanctionTypeLabel(type: UserSanctionType) {
  switch (type) {
    case "WARNING":
      return "경고"
    case "CHAT_RESTRICTION":
      return "채팅 제한"
    case "SPEECH_RESTRICTION":
    case "STAGE_RESTRICTION":
      return "발언/의견 작성 제한"
    case "ACCOUNT_SUSPENSION":
      return "계정 정지"
    default:
      return type
  }
}

function formatRoomDateTime(value: string | null | undefined) {
  if (!value) return null

  return new Date(value).toLocaleString("ko-KR", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

function roomTimeLabel(startedAt: string | null | undefined, endedAt: string | null | undefined) {
  const startLabel = formatRoomDateTime(startedAt)
  const endLabel = formatRoomDateTime(endedAt)

  if (startLabel && endLabel) return `${startLabel} - ${endLabel}`
  if (startLabel) return `${startLabel} 시작`
  return "시간 정보 없음"
}

function formatRemainingRoomTime(endedAt: string | null | undefined, now: number) {
  if (!endedAt) return null

  const endedAtTime = new Date(endedAt).getTime()
  if (Number.isNaN(endedAtTime)) return null

  const remainingSeconds = Math.max(0, Math.floor((endedAtTime - now) / 1000))
  const hours = Math.floor(remainingSeconds / 3600)
  const minutes = Math.floor((remainingSeconds % 3600) / 60)
  const seconds = remainingSeconds % 60
  const paddedMinutes = String(minutes).padStart(2, "0")
  const paddedSeconds = String(seconds).padStart(2, "0")

  if (hours > 0) return `남은 시간 ${hours}:${paddedMinutes}:${paddedSeconds}`
  return `남은 시간 ${minutes}:${paddedSeconds}`
}

export default function RoomDetailPage() {
  const params = useParams<{ id: string }>()
  const roomId = Number(params.id)
  const router = useRouter()
  const { user, loading: authLoading } = useAuth()
  const [joinError, setJoinError] = useState("")
  const [participationError, setParticipationError] = useState("")
  const [realtimeError, setRealtimeError] = useState("")
  const [joined, setJoined] = useState(false)
  const [roomView, setRoomView] = useState<RoomView | null>(null)
  const [syncState, setSyncState] = useState<RoomSyncState | null>(null)
  const [participantCount, setParticipantCount] = useState(0)
  const [participants, setParticipants] = useState<RoomParticipant[]>([])
  const [myTrust, setMyTrust] = useState<UserTrustDetail | null>(null)
  const [activeSanctions, setActiveSanctions] = useState<ActiveUserSanction[]>([])
  const [leaving, setLeaving] = useState(false)
  const [stompConnection, setStompConnection] = useState<RoomStompConnection | null>(null)
  const [stompConnected, setStompConnected] = useState(false)
  const [realtimeStatus, setRealtimeStatus] = useState<RealtimeStatus>("disconnected")
  const [recoveryKey, setRecoveryKey] = useState(0)
  const [roomClosedMessage, setRoomClosedMessage] = useState("")
  const connectedOnceRef = useRef(false)
  const roomRequestSeqRef = useRef(0)
  const participantRequestSeqRef = useRef(0)
  const handledEventIdsRef = useRef<string[]>([])
  const roomRecoveryTimerRef = useRef<number | null>(null)
  const disconnectGraceTimerRef = useRef<number | null>(null)
  const snapshotRecoveryTimerRef = useRef<number | null>(null)
  const [disconnectGraceExceeded, setDisconnectGraceExceeded] = useState(false)
  const [roomTimerNow, setRoomTimerNow] = useState(() => Date.now())
  const liveRoomActive = joined && roomView?.status === "OPEN"

  const rememberEvent = useCallback((eventId: string) => {
    if (handledEventIdsRef.current.includes(eventId)) return false
    handledEventIdsRef.current = [...handledEventIdsRef.current.slice(-199), eventId]
    return true
  }, [])

  const loadRoom = useCallback(async () => {
    const requestSeq = ++roomRequestSeqRef.current
    setJoinError("")
    try {
      const room = await roomApi.detail(roomId)
      const topicDetail = await topicApi.detail(room.topicId)
      if (requestSeq !== roomRequestSeqRef.current) return
      setRoomView({
        id: String(room.roomId),
        title: room.title,
        topicTitle: topicDetail.title,
        sourceUrl: topicDetail.sourceUrl,
        category: topicDetail.category,
        status: room.status,
        startedAt: room.startedAt,
        endedAt: room.endedAt,
        timeLabel: roomTimeLabel(room.startedAt, room.endedAt),
        tags: [topicDetail.category],
        isLive: room.status === "OPEN",
      })
    } catch (error) {
      if (requestSeq !== roomRequestSeqRef.current) return
      setJoinError(error instanceof ApiError ? error.message : "토론방 정보를 불러오지 못했습니다.")
    }
  }, [roomId])

  const loadParticipantSnapshot = useCallback(async () => {
    const requestSeq = ++participantRequestSeqRef.current
    try {
      const [countResponse, participantResponse] = await Promise.all([
        roomApi.participantCount(roomId),
        roomApi.participants(roomId),
      ])
      if (requestSeq !== participantRequestSeqRef.current) return
      setParticipantCount(countResponse.participantCount)
      setParticipants(participantResponse)
    } catch {
    }
  }, [roomId])

  const applySyncState = useCallback((state: RoomSyncState) => {
    setSyncState(state)
    setJoined(state.myParticipantStatus === "JOINED")
    setParticipantCount(state.participantCount)
    setRoomView((current) => current
      ? {
          ...current,
          status: state.roomStatus,
          isLive: state.roomStatus === "OPEN",
        }
      : current)

    if (state.canSubscribe) {
      setParticipationError("")
      return
    }

    if (state.roomStatus === "CLOSED") {
      setParticipationError("?좊줎諛⑹씠 醫낅즺?섏뿀?듬땲??")
      return
    }

    if (state.myParticipantStatus !== "JOINED") {
      setParticipationError("?좊줎諛⑹뿉 李몄뿬 以묒씤 ?곹깭媛 ?꾨떃?덈떎.")
    }
  }, [])

  const loadRoomSyncState = useCallback(async () => {
    try {
      const state = await roomApi.syncState(roomId)
      applySyncState(state)
      return state
    } catch (error) {
      setParticipationError(error instanceof ApiError ? error.message : "?좊줎諛?李몄뿬 ?곹깭瑜?遺덈윭?ㅼ? 紐삵뻽?듬땲??")
      return null
    }
  }, [applySyncState, roomId])

  const loadUserModerationSnapshot = useCallback(async () => {
    if (!user) {
      setMyTrust(null)
      setActiveSanctions([])
      return
    }

    const [trustResult, sanctionsResult] = await Promise.allSettled([
      trustApi.me(),
      sanctionApi.active(),
    ])

    if (trustResult.status === "fulfilled") setMyTrust(trustResult.value)
    if (sanctionsResult.status === "fulfilled") setActiveSanctions(sanctionsResult.value)
  }, [user])

  const scheduleRoomRecovery = useCallback(() => {
    if (roomRecoveryTimerRef.current !== null) {
      window.clearTimeout(roomRecoveryTimerRef.current)
    }
    roomRecoveryTimerRef.current = window.setTimeout(() => {
      roomRecoveryTimerRef.current = null
      void loadRoom()
      void loadParticipantSnapshot()
    }, 250)
  }, [loadParticipantSnapshot, loadRoom])

  const scheduleSnapshotRecovery = useCallback(() => {
    if (snapshotRecoveryTimerRef.current !== null) {
      window.clearTimeout(snapshotRecoveryTimerRef.current)
    }
    snapshotRecoveryTimerRef.current = window.setTimeout(() => {
      snapshotRecoveryTimerRef.current = null
      void loadRoom()
      void loadRoomSyncState()
      void loadParticipantSnapshot()
      setRecoveryKey((value) => value + 1)
    }, 100)
  }, [loadParticipantSnapshot, loadRoom, loadRoomSyncState])

  const ensureJoined = useCallback(async (clearJoinedOnFailure = true) => {
    setParticipationError("")
    try {
      await roomApi.join(roomId)
      const state = await loadRoomSyncState()
      setJoined(state ? state.myParticipantStatus === "JOINED" : true)
      return true
    } catch (error) {
      if (error instanceof ApiError && error.code === "ROOM_ALREADY_PARTICIPATED") {
        const state = await loadRoomSyncState()
        setJoined(state ? state.myParticipantStatus === "JOINED" : true)
        return true
      }

      if (clearJoinedOnFailure) setJoined(false)
      setParticipationError(error instanceof ApiError ? error.message : "토론방 입장에 실패했습니다.")
      return false
    }
  }, [loadRoomSyncState, roomId])

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

    void loadRoom()
    void ensureJoined().then((joinedRoom) => {
      if (joinedRoom) void loadParticipantSnapshot()
    })
    void loadUserModerationSnapshot()
  }, [authLoading, ensureJoined, loadParticipantSnapshot, loadRoom, loadUserModerationSnapshot, roomId, router, user])

  useEffect(() => {
    handledEventIdsRef.current = []
    return () => {
      if (roomRecoveryTimerRef.current !== null) {
        window.clearTimeout(roomRecoveryTimerRef.current)
      }
      if (snapshotRecoveryTimerRef.current !== null) {
        window.clearTimeout(snapshotRecoveryTimerRef.current)
      }
    }
  }, [roomId])

  useEffect(() => {
    if (!liveRoomActive) return

    const connection = createRoomStompConnection(roomId, {
      onStatus: (connected) => {
        setStompConnected(connected)
        if (!connected) return
        connectedOnceRef.current = true
      },
      onRealtimeStatus: setRealtimeStatus,
      onError: (message) => setRealtimeError(message),
      onBeforeResubscribe: () => ensureJoined(false),
      onSubscriptionsReady: scheduleSnapshotRecovery,
    })
    setStompConnection(connection)
    connection.connect()

    return () => {
      setStompConnection(null)
      setStompConnected(false)
      setRealtimeStatus("disconnected")
      connectedOnceRef.current = false
      connection.disconnect()
    }
  }, [ensureJoined, liveRoomActive, roomId, scheduleSnapshotRecovery])

  useEffect(() => {
    if (realtimeStatus === "connected" || realtimeStatus === "disconnected") {
      setDisconnectGraceExceeded(false)
      if (disconnectGraceTimerRef.current !== null) {
        window.clearTimeout(disconnectGraceTimerRef.current)
        disconnectGraceTimerRef.current = null
      }
      return
    }

    if (disconnectGraceTimerRef.current !== null) return
    disconnectGraceTimerRef.current = window.setTimeout(() => {
      disconnectGraceTimerRef.current = null
      setDisconnectGraceExceeded(true)
    }, 60000)

    return () => {
      if (disconnectGraceTimerRef.current !== null) {
        window.clearTimeout(disconnectGraceTimerRef.current)
        disconnectGraceTimerRef.current = null
      }
    }
  }, [realtimeStatus])

  useEffect(() => {
    if (!stompConnection) return

    const unsubscribeParticipants = stompConnection.subscribe<RoomParticipantEvent | RoomEvent>(
      `/topic/rooms/${roomId}/participants/events`,
      (event) => {
        if (!rememberEvent(event.eventId)) return
        if (event.eventType === "PARTICIPANT_JOINED" || event.eventType === "PARTICIPANT_LEFT") {
          setParticipantCount(event.data.participantCount)
          scheduleRoomRecovery()
        }
      },
      setRealtimeError,
    )
    const unsubscribeRoom = stompConnection.subscribe<RoomParticipantEvent | RoomEvent>(
      `/topic/rooms/${roomId}/room/events`,
      (event) => {
        if (!rememberEvent(event.eventId)) return
        if (event.eventType !== "ROOM_CLOSED") return
        setRoomClosedMessage(event.data.message || "토론방이 종료되었습니다.")
        setRoomView((current) => current ? { ...current, status: "CLOSED", isLive: false } : current)
        scheduleRoomRecovery()
      },
      setRealtimeError,
    )
    const unsubscribeSanctions = user
      ? stompConnection.subscribe<UserSanctionEvent>(
        `/topic/users/${user.userId}/sanctions/events`,
        (event) => {
          if (!rememberEvent(event.eventId)) return
          const action = event.eventType === "SANCTION_REVOKED" ? "해제" : "변경"
          setJoinError(`사용자 제재 상태가 ${action}되었습니다. 필요한 경우 요청을 다시 시도해주세요.`)
          void loadUserModerationSnapshot()
        },
        setRealtimeError,
      )
      : () => {}

    return () => {
      unsubscribeParticipants()
      unsubscribeRoom()
      unsubscribeSanctions()
    }
  }, [loadUserModerationSnapshot, rememberEvent, roomId, scheduleRoomRecovery, stompConnection, user])

  useEffect(() => {
    if (recoveryKey === 0) return
    void loadRoom()
    void loadRoomSyncState()
    void loadParticipantSnapshot()
  }, [loadParticipantSnapshot, loadRoom, loadRoomSyncState, recoveryKey])

  useEffect(() => {
    if (roomView?.status !== "OPEN" || !roomView.endedAt) return

    setRoomTimerNow(Date.now())
    const timerId = window.setInterval(() => {
      setRoomTimerNow(Date.now())
    }, 1000)

    return () => {
      window.clearInterval(timerId)
    }
  }, [roomView?.endedAt, roomView?.status])

  useEffect(() => {
    const recoverVisibleSnapshot = () => {
      if (document.visibilityState !== "visible") return
      scheduleSnapshotRecovery()
    }

    window.addEventListener("focus", recoverVisibleSnapshot)
    document.addEventListener("visibilitychange", recoverVisibleSnapshot)

    return () => {
      window.removeEventListener("focus", recoverVisibleSnapshot)
      document.removeEventListener("visibilitychange", recoverVisibleSnapshot)
      if (roomRecoveryTimerRef.current !== null) {
        window.clearTimeout(roomRecoveryTimerRef.current)
      }
    }
  }, [scheduleSnapshotRecovery])

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

  const realtimeMessage =
    realtimeStatus === "offline"
      ? disconnectGraceExceeded
        ? "인터넷 연결이 끊겨 자동 퇴장 처리되었을 수 있습니다. 연결 복구 후 다시 입장합니다."
        : "인터넷 연결이 끊겼습니다. 60초 안에 복구되면 참여 상태를 유지합니다."
      : realtimeStatus === "reconnecting"
        ? disconnectGraceExceeded
          ? "실시간 연결 복구가 지연되어 자동 퇴장 처리되었을 수 있습니다. 복구 후 다시 입장합니다."
          : "실시간 연결을 복구 중입니다. 60초 안에 복구되면 참여 상태를 유지합니다."
        : realtimeStatus === "connecting"
          ? "실시간 연결 중입니다."
          : ""

  const roomRemainingTimeLabel = roomView?.status === "OPEN"
    ? formatRemainingRoomTime(roomView.endedAt, roomTimerNow)
    : roomView
      ? "토론 종료"
      : null

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
          {participationError && (user || !roomView) && (
            <p className="mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{participationError}</p>
          )}
          {realtimeError && liveRoomActive && (
            <p className="mb-3 rounded-lg bg-muted px-3 py-2 text-xs text-muted-foreground">{realtimeError}</p>
          )}
          {realtimeMessage && (
            <p className="mb-3 rounded-lg bg-muted px-3 py-2 text-xs text-muted-foreground">{realtimeMessage}</p>
          )}
          {roomClosedMessage && (
            <p className="mb-3 rounded-lg border border-primary/30 bg-primary/10 px-3 py-2 text-xs font-medium text-primary">
              {roomClosedMessage}
            </p>
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
                  <div className="mb-3 space-y-2">
                    <p className="text-xs leading-relaxed text-muted-foreground">
                      {roomView?.topicTitle ?? "토픽 정보를 불러오는 중입니다."}
                    </p>
                    {roomView?.sourceUrl && (
                      <a
                        href={roomView.sourceUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex max-w-full items-center gap-1 text-xs font-medium text-primary hover:underline"
                      >
                        <span className="truncate">원문 보기</span>
                        <ExternalLink className="size-3 shrink-0" />
                      </a>
                    )}
                  </div>

                  <Separator className="mb-3" />

                  <div className="mb-3 flex items-start gap-2 rounded-lg bg-muted/50 px-3 py-2 text-xs text-muted-foreground">
                    <Clock className="mt-0.5 size-3.5 shrink-0" />
                    <div className="min-w-0">
                      <p className="font-medium text-foreground">토론 시간</p>
                      <p className="mt-0.5 leading-relaxed">{roomView?.timeLabel ?? "시간 정보 없음"}</p>
                      {roomRemainingTimeLabel && (
                        <p className="mt-1 text-sm font-semibold text-foreground">{roomRemainingTimeLabel}</p>
                      )}
                    </div>
                  </div>

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

                <div className="rounded-xl border border-border/50 bg-card p-4">
                  <div className="mb-3 flex items-center gap-2">
                    <ShieldCheck className="size-4 text-primary" />
                    <span className="text-xs font-semibold text-foreground">내 신뢰도</span>
                  </div>
                  {myTrust ? (
                    <div className="flex flex-col gap-2 text-xs">
                      <div className="flex items-center justify-between">
                        <span className="text-muted-foreground">신뢰 점수</span>
                        <span className="font-semibold">{myTrust.score}점 · {trustLevelLabel(myTrust.trustLevel)}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-muted-foreground">활동 등급</span>
                        <span className="font-semibold">{activityLevelLabel(myTrust.activityLevel)}</span>
                      </div>
                    </div>
                  ) : (
                    <p className="text-xs text-muted-foreground">신뢰도 정보를 불러오는 중입니다.</p>
                  )}
                  {activeSanctions.length > 0 && (
                    <>
                      <Separator className="my-3" />
                      <div className="flex flex-col gap-1.5">
                        <p className="text-[11px] font-semibold text-destructive">현재 적용 중인 제한</p>
                        {activeSanctions.map((sanction) => (
                          <div key={sanction.sanctionId} className="rounded-lg bg-destructive/10 px-2 py-1.5 text-[11px] text-destructive">
                            {sanctionTypeLabel(sanction.type)} · {sanction.endsAt ? `${new Date(sanction.endsAt).toLocaleString("ko-KR")}까지` : "해제 전까지"}
                          </div>
                        ))}
                      </div>
                    </>
                  )}
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
                        liveEnabled={liveRoomActive}
                        stompConnection={stompConnection}
                        stompConnected={stompConnected}
                        recoveryKey={recoveryKey}
                      />
                    </TabsContent>
                    <TabsContent value="chat" className="m-0 h-[70vh] min-h-[500px]">
                      {liveRoomActive ? (
                        <ChatPanel
                          roomId={roomId}
                          stompConnection={stompConnection}
                          stompConnected={stompConnected}
                          realtimeStatus={realtimeStatus}
                          recoveryKey={recoveryKey}
                        />
                      ) : <ChatUnavailable closed={roomView?.status === "CLOSED"} />}
                    </TabsContent>
                  </Tabs>
                </div>

                {/* Desktop: Side by side, fills remaining height */}
                <div className="hidden min-h-0 lg:flex lg:flex-1">
                  <div className="min-h-0 flex-1 border-r border-border/50">
                    <MainStage
                      roomId={roomId}
                      liveEnabled={liveRoomActive}
                      stompConnection={stompConnection}
                      stompConnected={stompConnected}
                      recoveryKey={recoveryKey}
                    />
                  </div>
                  <div className="min-h-0 w-80 xl:w-96">
                    {liveRoomActive ? (
                      <ChatPanel
                        roomId={roomId}
                        stompConnection={stompConnection}
                        stompConnected={stompConnected}
                        realtimeStatus={realtimeStatus}
                        recoveryKey={recoveryKey}
                      />
                    ) : <ChatUnavailable closed={roomView?.status === "CLOSED"} />}
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
