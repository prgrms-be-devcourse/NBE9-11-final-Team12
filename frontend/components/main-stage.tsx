"use client"

import { FormEvent, useCallback, useEffect, useState } from "react"
import { Flag, History, Loader2, MessageSquarePlus, Mic, MicOff, Users } from "lucide-react"
import { ApiError } from "@/lib/api/client"
import { speechApi, stageApi } from "@/lib/api/services"
import type { RoomStompConnection } from "@/lib/api/stomp"
import { useAuth } from "@/components/auth-provider"
import type {
  SpeechReportReason,
  SpeechEvent,
  SpeechReactionEvent,
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

function messageOf(error: unknown) {
  return error instanceof ApiError ? error.message : "요청 처리 중 오류가 발생했습니다."
}

export function MainStage({
  roomId,
  liveEnabled = true,
  stompConnection,
  stompConnected,
}: {
  roomId: number
  liveEnabled?: boolean
  stompConnection: RoomStompConnection | null
  stompConnected: boolean
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
  const [createOpen, setCreateOpen] = useState(false)
  const [content, setContent] = useState("")
  const [stance, setStance] = useState<SpeechStance>("PRO")
  const [submitting, setSubmitting] = useState(false)
  const [reportTarget, setReportTarget] = useState<SpeechSummary | null>(null)
  const [reportReason, setReportReason] = useState<SpeechReportReason | null>(null)
  const [reportDescription, setReportDescription] = useState("")
  const [reportSubmitted, setReportSubmitted] = useState(false)
  const [reportError, setReportError] = useState("")

  const loadSpeeches = useCallback(async () => {
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
      setSpeeches(response.items)
    } catch (requestError) {
      setError(messageOf(requestError))
    } finally {
      setLoading(false)
    }
  }, [liveEnabled, roomId])

  const loadStage = useCallback(async () => {
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

    if (speakerResult.status === "fulfilled") setCurrentSpeaker(speakerResult.value)
    if (queueResult.status === "fulfilled") setQueueSummary(queueResult.value)
    if (statusResult.status === "fulfilled") setRequestStatus(statusResult.value)

    const rejected = [speakerResult, queueResult, statusResult].find(
      (result) => result.status === "rejected",
    )
    if (rejected?.status === "rejected") {
      setStageError(messageOf(rejected.reason))
    }
    setStageLoading(false)
  }, [liveEnabled, roomId])

  useEffect(() => {
    void loadSpeeches()
  }, [loadSpeeches])

  useEffect(() => {
    void loadStage()
  }, [loadStage])

  useEffect(() => {
    if (!liveEnabled || !stompConnection || !stompConnected) return

    const unsubscribe = stompConnection.subscribe<StageEvent>(
      `/topic/rooms/${roomId}/stage/events`,
      () => {
        void loadStage()
      },
      setStageError,
    )

    return unsubscribe
  }, [liveEnabled, loadStage, roomId, stompConnected, stompConnection])

  useEffect(() => {
    if (!liveEnabled || !stompConnection || !stompConnected) return

    const unsubscribe = stompConnection.subscribe<SpeechEvent>(
      `/topic/rooms/${roomId}/speeches/events`,
      () => {
        void loadSpeeches()
      },
      setError,
    )

    return unsubscribe
  }, [liveEnabled, loadSpeeches, roomId, stompConnected, stompConnection])

  useEffect(() => {
    if (!liveEnabled || !stompConnection || !stompConnected) return

    const unsubscribe = stompConnection.subscribe<SpeechReactionEvent>(
      `/topic/rooms/${roomId}/speech-reactions/events`,
      () => {
        void loadSpeeches()
      },
      setError,
    )

    return unsubscribe
  }, [liveEnabled, loadSpeeches, roomId, stompConnected, stompConnection])

  const createSpeech = async (event: FormEvent) => {
    event.preventDefault()
    if (!content.trim()) return
    setSubmitting(true)
    setError("")
    try {
      await speechApi.create(roomId, { content: content.trim(), stance })
      setContent("")
      await loadSpeeches()
      setCreateOpen(false)
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

  const isCurrentUserSpeaking =
    Boolean(user && currentSpeaker?.currentSpeaker?.userId === user.userId)

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
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-4">
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

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader><DialogTitle>메인 의견 작성</DialogTitle><DialogDescription>찬반 입장과 의견을 입력해주세요.</DialogDescription></DialogHeader>
          <form className="flex flex-col gap-4" onSubmit={createSpeech}>
            {error && <p className="rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{error}</p>}
            <div className="grid grid-cols-2 gap-2">
              {(["PRO", "CON"] as SpeechStance[]).map((value) => <Button key={value} type="button" variant={stance === value ? "default" : "outline"} onClick={() => setStance(value)}>{value === "PRO" ? "찬성" : "반대"}</Button>)}
            </div>
            <textarea value={content} onChange={(event) => setContent(event.target.value)} rows={7} maxLength={2000} placeholder="의견을 입력하세요." className="resize-none rounded-lg border bg-background px-3 py-2 text-sm outline-none focus:border-primary" />
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
