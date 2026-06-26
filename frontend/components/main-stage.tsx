"use client"

import { FormEvent, useCallback, useEffect, useMemo, useRef, useState } from "react"
import {
  Crown,
  Flag,
  History,
  ImageIcon,
  Loader2,
  MessageSquarePlus,
  Mic,
  MicOff,
  ThumbsUp,
  Users,
  X,
} from "lucide-react"
import { ApiError } from "@/lib/api/client"
import { speechApi, stageApi } from "@/lib/api/services"
import type { RoomStompConnection } from "@/lib/api/stomp"
import { useAuth } from "@/components/auth-provider"
import type {
  BestSpeech,
  SpeechEvent,
  SpeechReactionEvent,
  SpeechReportReason,
  SpeechStance,
  SpeechSummary,
  StageCurrentSpeaker,
  StageEvent,
  StageQueue,
  StageRequestStatus,
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

type SpeechGroup = {
  key: string
  userId: number
  stance: SpeechStance | null
  status: string
  createdAt: string
  speeches: SpeechSummary[]
}

function messageOf(error: unknown) {
  return error instanceof ApiError ? error.message : "요청 처리 중 오류가 발생했습니다."
}

function formatRemainingTime(totalSeconds: number | null) {
  if (totalSeconds === null) return null
  const safeSeconds = Math.max(0, totalSeconds)
  const minutes = Math.floor(safeSeconds / 60)
  const seconds = safeSeconds % 60
  return `${minutes}:${seconds.toString().padStart(2, "0")}`
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
  const [bestSpeech, setBestSpeech] = useState<BestSpeech | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [stageLoading, setStageLoading] = useState(true)
  const [stageError, setStageError] = useState("")
  const [currentSpeaker, setCurrentSpeaker] = useState<StageCurrentSpeaker | null>(null)
  const [queueSummary, setQueueSummary] = useState<StageQueue | null>(null)
  const [requestStatus, setRequestStatus] = useState<StageRequestStatus | null>(null)
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
  const [nowTimestamp, setNowTimestamp] = useState(() => Date.now())
  const speechesRequestSeqRef = useRef(0)
  const stageRequestSeqRef = useRef(0)
  const imageInputRef = useRef<HTMLInputElement | null>(null)
  const handledEventIdsRef = useRef<string[]>([])
  const speechesRecoveryTimerRef = useRef<number | null>(null)
  const stageRecoveryTimerRef = useRef<number | null>(null)

  const isCurrentUserSpeaking =
    Boolean(user && currentSpeaker?.currentSpeaker?.userId === user.userId)

  const remainingSeconds = useMemo(() => {
    const expiresAt = currentSpeaker?.currentSpeaker?.expiresAt
    if (!expiresAt) return null
    return Math.ceil((new Date(expiresAt).getTime() - nowTimestamp) / 1000)
  }, [currentSpeaker, nowTimestamp])

  const speechGroups = useMemo(() => {
    return speeches.reduce<SpeechGroup[]>((groups, speech) => {
      const previous = groups[groups.length - 1]
      if (
        previous
        && previous.userId === speech.userId
        && previous.stance === speech.stance
        && previous.status === speech.status
      ) {
        previous.speeches.push(speech)
        return groups
      }

      groups.push({
        key: String(speech.speechId),
        userId: speech.userId,
        stance: speech.stance,
        status: speech.status,
        createdAt: speech.createdAt,
        speeches: [speech],
      })
      return groups
    }, [])
  }, [speeches])

  const rememberEvent = useCallback((eventId: string) => {
    if (handledEventIdsRef.current.includes(eventId)) return false
    handledEventIdsRef.current = [...handledEventIdsRef.current.slice(-199), eventId]
    return true
  }, [])

  const loadSpeeches = useCallback(async () => {
    const requestSeq = ++speechesRequestSeqRef.current
    if (!liveEnabled) {
      setSpeeches([])
      setBestSpeech(null)
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
      void speechApi.best(roomId)
        .then(setBestSpeech)
        .catch(() => setBestSpeech(null))
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

  const refreshSpeeches = useCallback(async () => {
    await loadSpeeches()
  }, [loadSpeeches])

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
    void loadSpeeches()
  }, [loadSpeeches])

  useEffect(() => {
    void loadStage()
  }, [loadStage])

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
  }, [loadSpeeches, loadStage, recoveryKey])

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
    if (!currentSpeaker?.currentSpeaker?.expiresAt) return
    setNowTimestamp(Date.now())
    const intervalId = window.setInterval(() => setNowTimestamp(Date.now()), 1000)
    return () => window.clearInterval(intervalId)
  }, [currentSpeaker?.currentSpeaker?.expiresAt])

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

  const createSpeech = async (event: FormEvent) => {
    event.preventDefault()
    if (!content.trim()) return
    if (!isCurrentUserSpeaking) {
      setError("발언권을 받은 사용자만 의견을 작성할 수 있습니다.")
      setCreateOpen(false)
      return
    }

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
      await refreshSpeeches()
      setCreateOpen(false)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : messageOf(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  const toggleReaction = async (speech: SpeechSummary) => {
    if (speech.userId === user?.userId) return
    setSubmitting(true)
    setError("")
    try {
      if (speech.reactedByMe) {
        await speechApi.deleteReaction(speech.speechId)
      } else {
        await speechApi.createReaction(speech.speechId)
      }
      await refreshSpeeches()
    } catch (requestError) {
      setError(messageOf(requestError))
    } finally {
      setSubmitting(false)
    }
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

  const openCreateDialog = () => {
    if (!isCurrentUserSpeaking) {
      setStageError("발언권을 받은 사용자만 의견을 작성할 수 있습니다.")
      return
    }
    setCreateOpen(true)
  }

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
          disabled={!liveEnabled || !isCurrentUserSpeaking}
          onClick={openCreateDialog}
        >
          <MessageSquarePlus className="size-3.5" /> 의견 작성
        </Button>
      </div>

      <div className="border-b border-border/50 bg-muted/20 px-4 py-3">
        {stageError && <p className="mb-2 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{stageError}</p>}
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
              {currentSpeaker?.hasCurrentSpeaker && (
                <Badge variant="outline" className="text-[10px]">
                  남은 시간 {formatRemainingTime(remainingSeconds)}
                </Badge>
              )}
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
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-4">
        {error && <p className="mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{error}</p>}
        {loading ? (
          <div className="flex h-40 items-center justify-center"><Loader2 className="size-5 animate-spin text-primary" /></div>
        ) : speeches.length === 0 ? (
          <div className="flex h-40 flex-col items-center justify-center gap-2 text-center text-muted-foreground">
            <History className="size-6" />
            <p className="text-sm">아직 등록된 의견이 없습니다.</p>
            <p className="text-xs">발언권을 받은 사용자가 첫 의견을 작성할 수 있습니다.</p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {bestSpeech && (
              <div className="rounded-xl border border-amber-200 bg-amber-50/70 p-4 text-amber-950">
                <div className="mb-2 flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2 text-xs font-semibold">
                    <Crown className="size-4 text-amber-600" />
                    베스트 의견
                  </div>
                  <Badge variant="outline" className="border-amber-300 bg-white/70 text-[10px] text-amber-700">
                    공감 {bestSpeech.reactionCount.toLocaleString()}
                  </Badge>
                </div>
                <p className="line-clamp-3 whitespace-pre-wrap text-sm leading-relaxed">{bestSpeech.content}</p>
              </div>
            )}
            {speechGroups.map((group) => (
              <article key={group.key} className="rounded-xl border border-border/50 bg-card p-4">
                <div className="mb-3 flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <Avatar className="size-7"><AvatarFallback className="text-[10px]">U{group.userId}</AvatarFallback></Avatar>
                    <div>
                      <p className="text-xs font-semibold">사용자 #{group.userId}</p>
                      <p className="text-[10px] text-muted-foreground">{new Date(group.createdAt).toLocaleString("ko-KR")}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {group.speeches.length > 1 && <Badge variant="outline" className="text-[10px]">{group.speeches.length}개 묶음</Badge>}
                    {group.stance && <Badge variant="outline" className="text-[10px]">{group.stance === "PRO" ? "찬성" : "반대"}</Badge>}
                    <Badge variant="secondary" className="text-[10px]">{group.status}</Badge>
                  </div>
                </div>
                <div className="flex flex-col gap-3">
                  {group.speeches.map((speech, index) => (
                    <div key={speech.speechId} className={index > 0 ? "border-t border-border/50 pt-3" : ""}>
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
                      <div className="mt-3 flex items-center justify-between gap-2">
                        <Button
                          variant={speech.reactedByMe ? "default" : "outline"}
                          size="sm"
                          className="gap-1.5 text-xs"
                          disabled={submitting || speech.userId === user?.userId}
                          onClick={() => toggleReaction(speech)}
                        >
                          <ThumbsUp className="size-3.5" />
                          {speech.reactionCount.toLocaleString()}
                          <span className="hidden sm:inline">{speech.reactedByMe ? "공감 취소" : "공감"}</span>
                        </Button>
                        <Button variant="ghost" size="sm" className="gap-1 text-xs text-muted-foreground hover:text-destructive" onClick={() => openReport(speech)}>
                          <Flag className="size-3" /> 신고
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </article>
            ))}
          </div>
        )}
      </div>

      <Dialog open={createOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader><DialogTitle>메인 의견 작성</DialogTitle><DialogDescription>발언권 시간 내에 찬반 입장과 의견을 입력해주세요.</DialogDescription></DialogHeader>
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
            <Button type="submit" disabled={submitting || !content.trim() || !isCurrentUserSpeaking}>{submitting && <Loader2 className="mr-2 size-4 animate-spin" />}등록</Button>
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
