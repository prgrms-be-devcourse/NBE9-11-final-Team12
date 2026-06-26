"use client"

import { FormEvent, useCallback, useEffect, useRef, useState } from "react"
import { FileText, Flag, History, ImageIcon, Lightbulb, Loader2, MessageSquarePlus, Mic, MicOff, RefreshCw, Users, X } from "lucide-react"
import { ApiError } from "@/lib/api/client"
import { aiCounterIssueApi, speechApi, stageApi, stageSummaryApi } from "@/lib/api/services"
import type { RoomStompConnection } from "@/lib/api/stomp"
import { useAuth } from "@/components/auth-provider"
import type {
  AiCounterIssue,
  AiCounterIssueEvent,
  SpeechReportReason,
  SpeechEvent,
  SpeechReactionEvent,
  SpeechStance,
  SpeechSummary,
  StageCurrentSpeaker,
  StageEvent,
  StageQueue,
  StageRequestStatus,
  StageSummary,
  StageSummaryEvent,
} from "@/lib/api/types"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"

const REPORT_REASONS: { value: SpeechReportReason; label: string }[] = [
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

const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"]
const MAX_IMAGE_SIZE = 5 * 1024 * 1024

function messageOf(error: unknown) {
  return error instanceof ApiError ? error.message : "요청 처리 중 오류가 발생했습니다."
}

export function MainStage({
  roomId,
  liveEnabled = true,
  stompConnection,
  stompConnected,
  recoveryKey,
}: {
  roomId: number
  liveEnabled?: boolean
  stompConnection: RoomStompConnection | null
  stompConnected: boolean
  recoveryKey: number
}) {
  const { user } = useAuth()
  const [speeches, setSpeeches] = useState<SpeechSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [stageLoading, setStageLoading] = useState(true)
  const [stageError, setStageError] = useState("")
  const [currentSpeaker, setCurrentSpeaker] = useState<StageCurrentSpeaker | null>(null)
  const [queueSummary, setQueueSummary] = useState<StageQueue | null>(null)
  const [requestStatus, setRequestStatus] = useState<StageRequestStatus | null>(null)
  const [stageSummary, setStageSummary] = useState<StageSummary | null>(null)
  const [summaryLoading, setSummaryLoading] = useState(false)
  const [summaryError, setSummaryError] = useState("")
  const [counterIssues, setCounterIssues] = useState<AiCounterIssue[]>([])
  const [counterIssueError, setCounterIssueError] = useState("")
  const [createOpen, setCreateOpen] = useState(false)
  const [content, setContent] = useState("")
  const [stance, setStance] = useState<SpeechStance>("PRO")
  const [selectedImage, setSelectedImage] = useState<File | null>(null)
  const [imagePreviewUrl, setImagePreviewUrl] = useState("")
  const [imageError, setImageError] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [reportTarget, setReportTarget] = useState<SpeechSummary | null>(null)
  const [reportReason, setReportReason] = useState<SpeechReportReason | null>(null)
  const [reportDescription, setReportDescription] = useState("")
  const [reportSubmitted, setReportSubmitted] = useState(false)
  const [reportError, setReportError] = useState("")
  const speechesRequestSeqRef = useRef(0)
  const stageRequestSeqRef = useRef(0)
  const summaryRequestSeqRef = useRef(0)
  const counterIssueRequestSeqRef = useRef(0)
  const summaryInFlightRoomIdRef = useRef<number | null>(null)
  const counterIssueInFlightRoomIdRef = useRef<number | null>(null)
  const counterIssueReloadPendingRef = useRef(false)
  const mountedRef = useRef(true)
  const imageInputRef = useRef<HTMLInputElement | null>(null)
  const handledEventIdsRef = useRef<string[]>([])
  const speechesRecoveryTimerRef = useRef<number | null>(null)
  const stageRecoveryTimerRef = useRef<number | null>(null)

  const rememberEvent = useCallback((eventId: string) => {
    if (handledEventIdsRef.current.includes(eventId)) return false
    handledEventIdsRef.current = [...handledEventIdsRef.current.slice(-199), eventId]
    return true
  }, [])

  const loadSpeeches = useCallback(async () => {
    const requestSeq = ++speechesRequestSeqRef.current
    if (!liveEnabled) {
      setSpeeches([])
      setLoading(false)
      setError("")
      return
    }

    setLoading(true)
    setError("")
    try {
      const response = await speechApi.list(roomId)
      if (requestSeq !== speechesRequestSeqRef.current) return
      setSpeeches(response.items)
    } catch (requestError) {
      if (requestSeq !== speechesRequestSeqRef.current) return
      setError(messageOf(requestError))
    } finally {
      if (requestSeq === speechesRequestSeqRef.current) setLoading(false)
    }
  }, [liveEnabled, roomId])

  const loadStage = useCallback(async () => {
    const requestSeq = ++stageRequestSeqRef.current
    if (!liveEnabled) {
      setStageLoading(false)
      setStageError("")
      return
    }

    setStageLoading(true)
    setStageError("")
    const [speakerResult, queueResult, statusResult] = await Promise.allSettled([
      stageApi.current(roomId),
      stageApi.queueSummary(roomId),
      stageApi.myRequestStatus(roomId),
    ])

    if (requestSeq !== stageRequestSeqRef.current) return

    if (speakerResult.status === "fulfilled") setCurrentSpeaker(speakerResult.value)
    if (queueResult.status === "fulfilled") setQueueSummary(queueResult.value)
    if (statusResult.status === "fulfilled") setRequestStatus(statusResult.value)

    const rejected = [speakerResult, queueResult, statusResult].find(
      (result) => result.status === "rejected",
    )
    if (rejected?.status === "rejected") {
      setStageError(messageOf(rejected.reason))
    }
    if (requestSeq === stageRequestSeqRef.current) setStageLoading(false)
  }, [liveEnabled, roomId])

  const loadStageSummary = useCallback(async (showLoading = true) => {
    if (!liveEnabled) {
      setStageSummary(null)
      setSummaryLoading(false)
      setSummaryError("")
      return
    }

    if (summaryInFlightRoomIdRef.current === roomId) return
    summaryInFlightRoomIdRef.current = roomId
    const requestSeq = ++summaryRequestSeqRef.current

    if (showLoading) setSummaryLoading(true)
    setSummaryError("")
    try {
      const response = await stageSummaryApi.get(roomId)
      if (!mountedRef.current || requestSeq !== summaryRequestSeqRef.current) return
      setStageSummary(response)
    } catch (requestError) {
      if (!mountedRef.current || requestSeq !== summaryRequestSeqRef.current) return
      if (requestError instanceof ApiError && requestError.code === "STAGE_SUMMARY_NOT_FOUND") {
        setStageSummary(null)
        return
      }
      setSummaryError(messageOf(requestError))
    } finally {
      if (summaryInFlightRoomIdRef.current === roomId) {
        summaryInFlightRoomIdRef.current = null
      }
      if (mountedRef.current && requestSeq === summaryRequestSeqRef.current) {
        setSummaryLoading(false)
      }
    }
  }, [liveEnabled, roomId])

  const loadCounterIssues = useCallback(async () => {
    if (!liveEnabled) {
      setCounterIssues([])
      setCounterIssueError("")
      return
    }

    if (counterIssueInFlightRoomIdRef.current === roomId) {
      counterIssueReloadPendingRef.current = true
      return
    }
    counterIssueInFlightRoomIdRef.current = roomId
    counterIssueReloadPendingRef.current = false
    const requestSeq = ++counterIssueRequestSeqRef.current

    setCounterIssueError("")
    try {
      const response = await aiCounterIssueApi.recent(roomId)
      if (!mountedRef.current || requestSeq !== counterIssueRequestSeqRef.current) return
      setCounterIssues(response.filter((issue) => issue.content.trim()))
    } catch (requestError) {
      if (!mountedRef.current || requestSeq !== counterIssueRequestSeqRef.current) return
      if (requestError instanceof ApiError && requestError.status === 404) {
        setCounterIssues([])
        return
      }
      setCounterIssueError(messageOf(requestError))
    } finally {
      if (counterIssueInFlightRoomIdRef.current === roomId) {
        counterIssueInFlightRoomIdRef.current = null
      }
      if (mountedRef.current && counterIssueReloadPendingRef.current) {
        counterIssueReloadPendingRef.current = false
        void loadCounterIssues()
      }
    }
  }, [liveEnabled, roomId])

  const scheduleSpeechesRecovery = useCallback(() => {
    if (speechesRecoveryTimerRef.current !== null) {
      window.clearTimeout(speechesRecoveryTimerRef.current)
    }
    speechesRecoveryTimerRef.current = window.setTimeout(() => {
      speechesRecoveryTimerRef.current = null
      void loadSpeeches()
    }, 250)
  }, [loadSpeeches])

  const scheduleStageRecovery = useCallback(() => {
    if (stageRecoveryTimerRef.current !== null) {
      window.clearTimeout(stageRecoveryTimerRef.current)
    }
    stageRecoveryTimerRef.current = window.setTimeout(() => {
      stageRecoveryTimerRef.current = null
      void loadStage()
    }, 250)
  }, [loadStage])

  useEffect(() => {
    mountedRef.current = true
    return () => {
      mountedRef.current = false
      summaryRequestSeqRef.current += 1
      counterIssueRequestSeqRef.current += 1
      summaryInFlightRoomIdRef.current = null
      counterIssueInFlightRoomIdRef.current = null
      counterIssueReloadPendingRef.current = false
    }
  }, [])

  useEffect(() => {
    void loadSpeeches()
  }, [loadSpeeches])

  useEffect(() => {
    void loadStage()
  }, [loadStage])

  useEffect(() => {
    void loadStageSummary()
  }, [loadStageSummary])

  useEffect(() => {
    void loadCounterIssues()
  }, [loadCounterIssues])

  useEffect(() => {
    handledEventIdsRef.current = []
    return () => {
      if (speechesRecoveryTimerRef.current !== null) {
        window.clearTimeout(speechesRecoveryTimerRef.current)
      }
      if (stageRecoveryTimerRef.current !== null) {
        window.clearTimeout(stageRecoveryTimerRef.current)
      }
    }
  }, [roomId])

  useEffect(() => {
    if (recoveryKey === 0) return
    void loadSpeeches()
    void loadStage()
    void loadStageSummary(false)
    void loadCounterIssues()
  }, [loadCounterIssues, loadSpeeches, loadStage, loadStageSummary, recoveryKey])

  useEffect(() => {
    if (!selectedImage) {
      setImagePreviewUrl("")
      return
    }

    const previewUrl = URL.createObjectURL(selectedImage)
    setImagePreviewUrl(previewUrl)

    return () => URL.revokeObjectURL(previewUrl)
  }, [selectedImage])

  useEffect(() => {
    if (!liveEnabled || stompConnected) return
    if (document.visibilityState !== "visible") return
    const hasActiveStageInterest = Boolean(
      currentSpeaker?.hasCurrentSpeaker || requestStatus?.hasRequest || (queueSummary?.totalWaitingCount ?? 0) > 0,
    )
    if (!hasActiveStageInterest) return

    const intervalId = window.setInterval(() => {
      if (document.visibilityState === "visible") void loadStage()
    }, 5000)

    return () => window.clearInterval(intervalId)
  }, [currentSpeaker, liveEnabled, loadStage, queueSummary, requestStatus, stompConnected])

  useEffect(() => {
    if (!liveEnabled || !stompConnection) return

    const unsubscribe = stompConnection.subscribe<StageEvent>(
      `/topic/rooms/${roomId}/stage/events`,
      (event) => {
        if (!rememberEvent(event.eventId)) return
        scheduleStageRecovery()
      },
      setStageError,
    )

    return unsubscribe
  }, [liveEnabled, rememberEvent, roomId, scheduleStageRecovery, stompConnection])

  useEffect(() => {
    if (!liveEnabled || !stompConnection) return

    const unsubscribe = stompConnection.subscribe<SpeechEvent>(
      `/topic/rooms/${roomId}/speeches/events`,
      (event) => {
        if (!rememberEvent(event.eventId)) return
        scheduleSpeechesRecovery()
      },
      setError,
    )

    return unsubscribe
  }, [liveEnabled, rememberEvent, roomId, scheduleSpeechesRecovery, stompConnection])

  useEffect(() => {
    if (!liveEnabled || !stompConnection) return

    const unsubscribe = stompConnection.subscribe<StageSummaryEvent>(
      `/topic/rooms/${roomId}/stage-summary/events`,
      (event) => {
        if (!rememberEvent(event.eventId)) return
        void loadStageSummary(false)
      },
      setSummaryError,
    )

    return unsubscribe
  }, [liveEnabled, loadStageSummary, rememberEvent, roomId, stompConnection])

  useEffect(() => {
    if (!liveEnabled || !stompConnection) return

    const unsubscribe = stompConnection.subscribe<AiCounterIssueEvent>(
      `/topic/rooms/${roomId}/ai-counter-issues/events`,
      (event) => {
        if (!rememberEvent(event.eventId)) return
        void loadCounterIssues()
      },
      setCounterIssueError,
    )

    return unsubscribe
  }, [liveEnabled, loadCounterIssues, rememberEvent, roomId, stompConnection])

  useEffect(() => {
    if (!liveEnabled || !stompConnection) return

    const unsubscribe = stompConnection.subscribe<SpeechReactionEvent>(
      `/topic/rooms/${roomId}/speech-reactions/events`,
      (event) => {
        if (!rememberEvent(event.eventId)) return
        scheduleSpeechesRecovery()
      },
      setError,
    )

    return unsubscribe
  }, [liveEnabled, rememberEvent, roomId, scheduleSpeechesRecovery, stompConnection])

  const createSpeech = async (event: FormEvent) => {
    event.preventDefault()
    if (!content.trim()) return
    setSubmitting(true)
    setError("")
    setImageError("")
    try {
      const speech = await speechApi.create(roomId, { content: content.trim(), stance })

      if (selectedImage) {
        const upload = await speechApi.createImageUploadUrl(speech.speechId, {
          contentType: selectedImage.type,
          fileSize: selectedImage.size,
        })
        const uploadResponse = await fetch(upload.uploadUrl, {
          method: "PUT",
          headers: {
            "Content-Type": selectedImage.type,
          },
          body: selectedImage,
        })

        if (!uploadResponse.ok) {
          throw new Error("이미지 업로드에 실패했습니다.")
        }

        await speechApi.confirmImage(speech.speechId, upload.imageKey)
      }

      setContent("")
      setSelectedImage(null)
      if (imageInputRef.current) imageInputRef.current.value = ""
      await loadSpeeches()
      setCreateOpen(false)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : messageOf(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  const selectImage = (file: File | null) => {
    setImageError("")

    if (!file) {
      setSelectedImage(null)
      return
    }

    if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
      setSelectedImage(null)
      setImageError("jpg, png, webp 이미지만 첨부할 수 있습니다.")
      if (imageInputRef.current) imageInputRef.current.value = ""
      return
    }

    if (file.size > MAX_IMAGE_SIZE) {
      setSelectedImage(null)
      setImageError("이미지는 5MB 이하만 첨부할 수 있습니다.")
      if (imageInputRef.current) imageInputRef.current.value = ""
      return
    }

    setSelectedImage(file)
  }

  const removeImage = () => {
    setSelectedImage(null)
    setImageError("")
    if (imageInputRef.current) imageInputRef.current.value = ""
  }

  const setCreateDialogOpen = (open: boolean) => {
    setCreateOpen(open)
    if (open) return
    setImageError("")
    setSelectedImage(null)
    if (imageInputRef.current) imageInputRef.current.value = ""
  }

  const submitReport = async () => {
    if (!reportTarget || !reportReason) return
    if (reportReason === "OTHER" && !reportDescription.trim()) return
    setSubmitting(true)
    setReportError("")
    try {
      await speechApi.report(reportTarget.speechId, reportReason, reportDescription.trim())
      setReportSubmitted(true)
    } catch (requestError) {
      setReportError(messageOf(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  const closeReport = () => {
    setReportTarget(null)
    setReportReason(null)
    setReportDescription("")
    setReportSubmitted(false)
    setReportError("")
  }

  const openReport = (speech: SpeechSummary) => {
    setReportError("")
    setReportTarget(speech)
  }

  const requestTurn = async () => {
    setSubmitting(true)
    setStageError("")
    try {
      await stageApi.requestTurn(roomId)
      await loadStage()
    } catch (requestError) {
      setStageError(messageOf(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  const cancelRequest = async () => {
    setSubmitting(true)
    setStageError("")
    try {
      await stageApi.cancelMyRequest(roomId)
      await loadStage()
    } catch (requestError) {
      setStageError(messageOf(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  const completeTurn = async () => {
    setSubmitting(true)
    setStageError("")
    try {
      await stageApi.completeTurn(roomId)
      await loadStage()
    } catch (requestError) {
      setStageError(messageOf(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  const isCurrentUserSpeaking =
    Boolean(user && currentSpeaker?.currentSpeaker?.userId === user.userId)

  const summaryKeyPoints = stageSummary?.keyPoints.filter((point) => point.trim()) ?? []
  const hasCompletedSummary = Boolean(
    stageSummary?.status === "COMPLETED" && (stageSummary.moderatorSummary?.trim() || summaryKeyPoints.length > 0),
  )
  const latestCounterIssue = counterIssues[0] ?? null

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="flex items-center justify-between border-b border-border/50 px-4 py-3">
        <div className="flex items-center gap-2">
          <Badge className="bg-rose-50 text-rose-600 border-rose-200 text-[11px]">LIVE</Badge>
          <span className="text-sm font-semibold">MAIN STAGE</span>
        </div>
        <Button
          size="sm"
          className="gap-1.5 text-xs"
          disabled={!liveEnabled}
          onClick={() => setCreateOpen(true)}
        >
          <MessageSquarePlus className="size-3.5" /> 의견 작성
        </Button>
      </div>

      <div className="border-b border-border/50 bg-muted/20 px-4 py-3">
        {stageError && <p className="mb-2 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{stageError}</p>}
        {counterIssueError && <p className="mb-2 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{counterIssueError}</p>}
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0 flex flex-col gap-1">
            <div className="flex items-center gap-2">
              <Mic className="size-4 text-primary" />
              <span className="text-xs font-semibold text-foreground">
                {!liveEnabled
                  ? "입장 완료 후 발언권을 신청할 수 있습니다"
                  : stageLoading
                  ? "발언권 상태 확인 중..."
                  : currentSpeaker?.hasCurrentSpeaker && currentSpeaker.currentSpeaker
                    ? `${currentSpeaker.currentSpeaker.nickname}님 발언 중`
                    : "현재 발언자가 없습니다"}
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-2 text-[11px] text-muted-foreground">
              <span className="flex items-center gap-1">
                <Users className="size-3" />
                대기 {queueSummary?.totalWaitingCount ?? 0}명
              </span>
              {requestStatus?.hasRequest && (
                <span>
                  내 상태: {requestStatus.status === "WAITING" ? `대기 ${requestStatus.currentRank ?? "-"}순위` : requestStatus.status}
                </span>
              )}
            </div>
          </div>
          <div className="flex shrink-0 gap-2">
            {isCurrentUserSpeaking ? (
              <Button size="sm" variant="outline" className="gap-1.5 text-xs" disabled={submitting} onClick={completeTurn}>
                <MicOff className="size-3.5" />
                발언 종료
              </Button>
            ) : requestStatus?.hasRequest ? (
              <Button
                size="sm"
                variant="outline"
                className="gap-1.5 text-xs"
                disabled={submitting || !requestStatus.cancelable}
                onClick={cancelRequest}
              >
                신청 취소
              </Button>
            ) : (
              <Button size="sm" variant="outline" className="gap-1.5 text-xs" disabled={submitting || !liveEnabled} onClick={requestTurn}>
                <Mic className="size-3.5" />
                발언권 신청
              </Button>
            )}
          </div>
        </div>
        {latestCounterIssue && (
          <div className="mt-3 rounded-lg border border-border/60 bg-background/70 px-3 py-2">
            <div className="mb-1.5 flex flex-wrap items-center gap-2">
              <span className="flex items-center gap-1.5 text-[11px] font-semibold text-foreground">
                <Lightbulb className="size-3.5 text-primary" />
                AI가 제안한 반대 쟁점
              </span>
              <Badge variant="outline" className="text-[10px]">
                {latestCounterIssue.targetStance === "PRO" ? "찬성 입장 대상" : "반대 입장 대상"}
              </Badge>
            </div>
            <p className="whitespace-pre-wrap break-words text-xs leading-relaxed text-muted-foreground">
              {latestCounterIssue.content}
            </p>
          </div>
        )}
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-4">
        <section className="mb-4 rounded-xl border border-border/50 bg-card p-4">
          <div className="mb-3 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex min-w-0 items-center gap-2">
              <FileText className="size-4 shrink-0 text-primary" />
              <div className="min-w-0">
                <h3 className="text-sm font-semibold text-foreground">의견 요약</h3>
                <p className="text-[11px] text-muted-foreground">
                  {stageSummary?.completedAt
                    ? `${new Date(stageSummary.completedAt).toLocaleString("ko-KR")} 기준`
                    : "토론 흐름을 정리한 중간 요약입니다."}
                </p>
              </div>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              {stageSummary?.status && (
                <Badge variant="outline" className="text-[10px]">
                  {stageSummary.status === "COMPLETED" ? "완료" : stageSummary.status === "PENDING" ? "준비 중" : "대기"}
                </Badge>
              )}
              <Button
                size="sm"
                variant="outline"
                className="gap-1.5 text-xs"
                disabled={!liveEnabled || summaryLoading}
                onClick={() => void loadStageSummary()}
              >
                {summaryLoading ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}
                요약 확인
              </Button>
            </div>
          </div>

          {summaryError && (
            <p className="mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{summaryError}</p>
          )}

          {summaryLoading && !stageSummary ? (
            <div className="flex h-24 items-center justify-center">
              <Loader2 className="size-5 animate-spin text-primary" />
            </div>
          ) : hasCompletedSummary ? (
            <div className="space-y-3">
              {stageSummary?.moderatorSummary?.trim() && (
                <p className="whitespace-pre-wrap text-sm leading-relaxed text-foreground">
                  {stageSummary.moderatorSummary}
                </p>
              )}
              {summaryKeyPoints.length > 0 && (
                <ul className="space-y-2">
                  {summaryKeyPoints.map((point) => (
                    <li key={point} className="flex gap-2 text-sm leading-relaxed text-foreground">
                      <span className="mt-2 size-1.5 shrink-0 rounded-full bg-primary" />
                      <span className="min-w-0 break-words">{point}</span>
                    </li>
                  ))}
                </ul>
              )}
              <p className="text-[11px] text-muted-foreground">
                의견 {stageSummary?.speechCount ?? 0}개 · 완료 발언자 {stageSummary?.completedSpeakerCount ?? 0}명 기준
              </p>
            </div>
          ) : (
            <div className="flex min-h-24 flex-col items-center justify-center gap-2 text-center text-muted-foreground">
              <FileText className="size-6" />
              <p className="text-sm">
                {stageSummary?.status === "PENDING"
                  ? "의견 요약을 준비 중입니다."
                  : stageSummary?.status === "FAILED"
                  ? "의견 요약을 준비하지 못했습니다."
                  : "아직 생성된 의견 요약이 없습니다."}
              </p>
              <p className="text-xs">
                {stageSummary?.status === "FAILED" ? "잠시 후 다시 확인해주세요." : "발언이 충분히 모이면 자동으로 생성됩니다."}
              </p>
            </div>
          )}
        </section>

        {error && <p className="mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{error}</p>}
        {loading ? (
          <div className="flex h-40 items-center justify-center"><Loader2 className="size-5 animate-spin text-primary" /></div>
        ) : speeches.length === 0 ? (
          <div className="flex h-40 flex-col items-center justify-center gap-2 text-center text-muted-foreground">
            <History className="size-6" />
            <p className="text-sm">아직 등록된 의견이 없습니다.</p>
            <p className="text-xs">첫 의견을 작성해 토론을 시작해보세요.</p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {speeches.map((speech) => (
              <article key={speech.speechId} className="rounded-xl border border-border/50 bg-card p-4">
                <div className="mb-3 flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <Avatar className="size-7"><AvatarFallback className="text-[10px]">U{speech.userId}</AvatarFallback></Avatar>
                    <div>
                      <p className="text-xs font-semibold">사용자 #{speech.userId}</p>
                      <p className="text-[10px] text-muted-foreground">{new Date(speech.createdAt).toLocaleString("ko-KR")}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {speech.stance && <Badge variant="outline" className="text-[10px]">{speech.stance === "PRO" ? "찬성" : "반대"}</Badge>}
                    <Badge variant="secondary" className="text-[10px]">{speech.status}</Badge>
                  </div>
                </div>
                <p className="whitespace-pre-wrap text-sm leading-relaxed">{speech.content}</p>
                {speech.imageUrl && (
                  <a
                    href={speech.imageUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-3 block overflow-hidden rounded-lg border border-border/60"
                  >
                    <img src={speech.imageUrl} alt="첨부 이미지" className="max-h-80 w-full object-cover" />
                  </a>
                )}
                <div className="mt-3 flex justify-end">
                  <Button variant="ghost" size="sm" className="gap-1 text-xs text-muted-foreground hover:text-destructive" onClick={() => openReport(speech)}>
                    <Flag className="size-3" /> 신고
                  </Button>
                </div>
              </article>
            ))}
          </div>
        )}
      </div>

      <Dialog open={createOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader><DialogTitle>메인 의견 작성</DialogTitle><DialogDescription>찬반 입장과 의견을 입력해주세요.</DialogDescription></DialogHeader>
          <form className="flex flex-col gap-4" onSubmit={createSpeech}>
            {error && <p className="rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{error}</p>}
            {imageError && <p className="rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{imageError}</p>}
            <div className="grid grid-cols-2 gap-2">
              {(["PRO", "CON"] as SpeechStance[]).map((value) => <Button key={value} type="button" variant={stance === value ? "default" : "outline"} onClick={() => setStance(value)}>{value === "PRO" ? "찬성" : "반대"}</Button>)}
            </div>
            <textarea value={content} onChange={(event) => setContent(event.target.value)} rows={7} maxLength={2000} placeholder="의견을 입력하세요." className="resize-none rounded-lg border bg-background px-3 py-2 text-sm outline-none focus:border-primary" />
            <div className="space-y-2">
              <input
                ref={imageInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
                onChange={(event) => selectImage(event.target.files?.[0] ?? null)}
              />
              {selectedImage && imagePreviewUrl ? (
                <div className="overflow-hidden rounded-lg border border-border/70">
                  <div className="flex items-center justify-between border-b border-border/60 px-3 py-2 text-xs">
                    <span className="truncate text-muted-foreground">{selectedImage.name}</span>
                    <Button type="button" variant="ghost" size="sm" className="h-7 gap-1 px-2 text-xs" onClick={removeImage}>
                      <X className="size-3" />
                      삭제
                    </Button>
                  </div>
                  <img src={imagePreviewUrl} alt="첨부 이미지 미리보기" className="max-h-52 w-full object-cover" />
                </div>
              ) : (
                <Button
                  type="button"
                  variant="outline"
                  className="w-full gap-2 text-xs"
                  onClick={() => imageInputRef.current?.click()}
                >
                  <ImageIcon className="size-4" />
                  이미지 첨부
                </Button>
              )}
              <p className="text-[11px] text-muted-foreground">jpg, png, webp 형식의 5MB 이하 이미지를 첨부할 수 있습니다.</p>
            </div>
            <div className="flex items-center justify-between text-xs text-muted-foreground"><span>욕설이 포함된 의견은 등록되지 않습니다.</span><span>{content.length}/2000</span></div>
            <Button type="submit" disabled={submitting || !content.trim()}>{submitting && <Loader2 className="mr-2 size-4 animate-spin" />}등록</Button>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={!!reportTarget} onOpenChange={(open) => !open && closeReport()}>
        <DialogContent className="max-w-sm">
          <DialogHeader><DialogTitle className="flex items-center gap-2"><Flag className="size-4 text-destructive" />의견 신고</DialogTitle><DialogDescription>신고 내용은 운영팀 검토 자료로 전달됩니다.</DialogDescription></DialogHeader>
          {reportSubmitted ? (
            <div className="flex flex-col gap-3 py-3 text-center"><p className="font-semibold">신고가 접수되었습니다.</p><Button onClick={closeReport}>확인</Button></div>
          ) : (
            <div className="flex flex-col gap-3">
              {reportError && <p className="rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{reportError}</p>}
              <div className="grid grid-cols-1 gap-1.5">{REPORT_REASONS.map((reason) => <button key={reason.value} onClick={() => setReportReason(reason.value)} className={`rounded-lg border px-3 py-2 text-left text-xs ${reportReason === reason.value ? "border-primary bg-primary/10 text-primary" : "border-border"}`}>{reason.label}</button>)}</div>
              <textarea value={reportDescription} onChange={(event) => setReportDescription(event.target.value)} maxLength={500} rows={3} placeholder={reportReason === "OTHER" ? "기타 사유는 상세 설명이 필수입니다." : "상세 설명 (선택)"} className="resize-none rounded-lg border bg-background px-3 py-2 text-xs outline-none focus:border-primary" />
              <Button variant="destructive" disabled={submitting || !reportReason || (reportReason === "OTHER" && !reportDescription.trim())} onClick={submitReport}>{submitting && <Loader2 className="mr-2 size-4 animate-spin" />}신고하기</Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
