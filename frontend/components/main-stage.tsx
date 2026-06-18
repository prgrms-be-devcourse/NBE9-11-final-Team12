"use client"

import { FormEvent, useCallback, useEffect, useState } from "react"
import { Flag, History, Loader2, MessageSquarePlus, Mic, UserRound } from "lucide-react"
import { ApiError } from "@/lib/api/client"
import { speechApi, stageApi } from "@/lib/api/services"
import type { SpeechReportReason, SpeechStance, SpeechSummary, StageCurrentSpeaker, StageQueue, StageRequestStatus } from "@/lib/api/types"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"

const REPORT_REASONS: { value: SpeechReportReason; label: string }[] = [
  { value: "ABUSE_HARASSMENT", label: "욕설 또는 괴롭힘" },
  { value: "HATE_SPEECH", label: "혐오 발언" },
  { value: "SEXUAL_CONTENT", label: "성적 콘텐츠" },
  { value: "THREAT_VIOLENCE", label: "위협 또는 폭력" },
  { value: "SPAM", label: "광고 또는 스팸" },
  { value: "MISINFORMATION", label: "허위 정보" },
  { value: "PRIVACY_VIOLATION", label: "개인정보 노출" },
  { value: "OFF_TOPIC", label: "주제와 무관" },
  { value: "OTHER", label: "기타" },
]

function messageOf(error: unknown) {
  return error instanceof ApiError ? error.message : "요청 처리 중 오류가 발생했습니다."
}

export function MainStage({ roomId }: { roomId: number }) {
  const [speeches, setSpeeches] = useState<SpeechSummary[]>([])
  const [current, setCurrent] = useState<StageCurrentSpeaker | null>(null)
  const [queue, setQueue] = useState<StageQueue | null>(null)
  const [myRequest, setMyRequest] = useState<StageRequestStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [createOpen, setCreateOpen] = useState(false)
  const [content, setContent] = useState("")
  const [stance, setStance] = useState<SpeechStance>("PRO")
  const [submitting, setSubmitting] = useState(false)
  const [reportTarget, setReportTarget] = useState<SpeechSummary | null>(null)
  const [reportReason, setReportReason] = useState<SpeechReportReason | null>(null)
  const [reportDescription, setReportDescription] = useState("")
  const [reportSubmitted, setReportSubmitted] = useState(false)
  const [reportError, setReportError] = useState("")

  const loadStage = useCallback(async () => {
    setLoading(true)
    setError("")
    try {
      const [speechPage, currentSpeaker, waitingQueue, requestStatus] = await Promise.all([
        speechApi.list(roomId),
        stageApi.current(roomId),
        stageApi.queue(roomId),
        stageApi.myRequest(roomId).catch(() => null),
      ])
      setSpeeches(speechPage.items)
      setCurrent(currentSpeaker)
      setQueue(waitingQueue)
      setMyRequest(requestStatus)
    } catch (requestError) {
      setError(messageOf(requestError))
    } finally {
      setLoading(false)
    }
  }, [roomId])

  useEffect(() => {
    void loadStage()
  }, [loadStage])

  const requestTurn = async () => {
    setSubmitting(true)
    setError("")
    try {
      await stageApi.request(roomId)
      await loadStage()
    } catch (requestError) {
      setError(messageOf(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  const cancelTurn = async () => {
    setSubmitting(true)
    setError("")
    try {
      await stageApi.cancelRequest(roomId)
      await loadStage()
    } catch (requestError) {
      setError(messageOf(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  const createSpeech = async (event: FormEvent) => {
    event.preventDefault()
    if (!content.trim()) return
    setSubmitting(true)
    setError("")
    try {
      await speechApi.create(roomId, { content: content.trim(), stance })
      setContent("")
      setCreateOpen(false)
      await loadStage()
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

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="flex items-center justify-between border-b border-border/50 px-4 py-3">
        <div className="flex items-center gap-2">
          <Badge className="border-rose-200 bg-rose-50 text-rose-600 text-[11px]">LIVE</Badge>
          <span className="text-sm font-semibold">메인 스테이지</span>
        </div>
        <Button size="sm" className="gap-1.5 text-xs" onClick={() => setCreateOpen(true)}>
          <MessageSquarePlus className="size-3.5" /> 의견 작성
        </Button>
      </div>

      <div className="grid gap-3 border-b border-border/50 p-4 md:grid-cols-3">
        <div className="rounded-lg bg-muted p-3 md:col-span-2">
          <div className="flex items-center gap-2 text-xs font-semibold text-muted-foreground">
            <Mic className="size-3.5" />
            현재 발언자
          </div>
          <p className="mt-2 text-sm font-semibold text-foreground">
            {current?.hasCurrentSpeaker && current.currentSpeaker ? current.currentSpeaker.nickname : "현재 발언자가 없습니다."}
          </p>
        </div>
        <div className="rounded-lg bg-muted p-3">
          <p className="text-xs font-semibold text-muted-foreground">대기 인원</p>
          <p className="mt-2 text-sm font-semibold text-foreground">{queue?.totalWaitingCount ?? 0}명</p>
        </div>
      </div>

      <div className="flex items-center justify-between border-b border-border/50 px-4 py-3">
        <div className="text-xs text-muted-foreground">
          {myRequest?.hasRequest ? `내 대기 순서: ${myRequest.currentRank ?? myRequest.queueOrder ?? "-"}번` : "발언권을 신청해 토론에 참여하세요."}
        </div>
        {myRequest?.cancelable ? (
          <Button variant="outline" size="sm" disabled={submitting} onClick={cancelTurn}>
            신청 취소
          </Button>
        ) : (
          <Button variant="outline" size="sm" disabled={submitting} onClick={requestTurn}>
            발언권 신청
          </Button>
        )}
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto p-4">
        {error && <p className="mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{error}</p>}
        {loading ? (
          <div className="flex h-40 items-center justify-center">
            <Loader2 className="size-5 animate-spin text-primary" />
          </div>
        ) : speeches.length === 0 ? (
          <div className="flex h-40 flex-col items-center justify-center gap-2 text-center text-muted-foreground">
            <History className="size-6" />
            <p className="text-sm">아직 등록된 의견이 없습니다.</p>
            <p className="text-xs">첫 의견을 작성해 토론을 시작해 보세요.</p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {speeches.map((speech) => (
              <article key={speech.speechId} className="rounded-xl border border-border/50 bg-card p-4">
                <div className="mb-3 flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <Avatar className="size-7">
                      <AvatarFallback className="text-[10px]">
                        <UserRound className="size-3.5" />
                      </AvatarFallback>
                    </Avatar>
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
                  <Button variant="ghost" size="sm" className="gap-1 text-xs text-muted-foreground hover:text-destructive" onClick={() => setReportTarget(speech)}>
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
          <DialogHeader>
            <DialogTitle>메인 의견 작성</DialogTitle>
            <DialogDescription>찬반 입장과 의견을 입력해 주세요.</DialogDescription>
          </DialogHeader>
          <form className="flex flex-col gap-4" onSubmit={createSpeech}>
            <div className="grid grid-cols-2 gap-2">
              {(["PRO", "CON"] as SpeechStance[]).map((value) => (
                <Button key={value} type="button" variant={stance === value ? "default" : "outline"} onClick={() => setStance(value)}>
                  {value === "PRO" ? "찬성" : "반대"}
                </Button>
              ))}
            </div>
            <textarea value={content} onChange={(event) => setContent(event.target.value)} rows={7} maxLength={2000} placeholder="의견을 입력하세요." className="resize-none rounded-lg border bg-background px-3 py-2 text-sm outline-none focus:border-primary" />
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>욕설이 포함된 의견은 등록되지 않습니다.</span>
              <span>{content.length}/2000</span>
            </div>
            <Button type="submit" disabled={submitting || !content.trim()}>
              {submitting && <Loader2 className="mr-2 size-4 animate-spin" />}
              등록
            </Button>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={!!reportTarget} onOpenChange={(open) => !open && closeReport()}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Flag className="size-4 text-destructive" />
              의견 신고
            </DialogTitle>
            <DialogDescription>신고 내용은 운영 검토 자료로 전달됩니다.</DialogDescription>
          </DialogHeader>
          {reportSubmitted ? (
            <div className="flex flex-col gap-3 py-3 text-center">
              <p className="font-semibold">신고가 접수되었습니다.</p>
              <Button onClick={closeReport}>확인</Button>
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              {reportError && <p className="rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{reportError}</p>}
              <div className="grid grid-cols-1 gap-1.5">
                {REPORT_REASONS.map((reason) => (
                  <button key={reason.value} onClick={() => setReportReason(reason.value)} className={`rounded-lg border px-3 py-2 text-left text-xs ${reportReason === reason.value ? "border-primary bg-primary/10 text-primary" : "border-border"}`}>
                    {reason.label}
                  </button>
                ))}
              </div>
              <textarea value={reportDescription} onChange={(event) => setReportDescription(event.target.value)} maxLength={500} rows={3} placeholder={reportReason === "OTHER" ? "기타 사유는 상세 설명이 필수입니다." : "상세 설명 (선택)"} className="resize-none rounded-lg border bg-background px-3 py-2 text-xs outline-none focus:border-primary" />
              <Button variant="destructive" disabled={submitting || !reportReason || (reportReason === "OTHER" && !reportDescription.trim())} onClick={submitReport}>
                {submitting && <Loader2 className="mr-2 size-4 animate-spin" />}
                신고하기
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
