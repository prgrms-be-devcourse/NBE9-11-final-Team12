"use client"

import { type ReactNode, useCallback, useEffect, useRef, useState } from "react"
import { BarChart3, Loader2, Plus, RefreshCw, Sparkles, Trash2 } from "lucide-react"
import { ApiError } from "@/lib/api/client"
import { aiReportApi } from "@/lib/api/services"
import type { AiReport, AiReportGenerateRequest } from "@/lib/api/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"

type RoomStatus = "OPEN" | "CLOSED"
type CustomPromptDraft = {
  id: number
  prompt: string
}

function messageOf(error: unknown, fallback = "AI 리포트를 불러오지 못했습니다.") {
  if (!(error instanceof ApiError)) return fallback
  if (error.code === "AI_REPORT_ROOM_NOT_CLOSED") {
    return "종료된 토론방만 AI 리포트를 생성할 수 있습니다."
  }
  if (error.code === "AI_REPORT_CUSTOM_PROMPT_REQUIRED") {
    return "커스텀 리포트에 사용할 프롬프트를 입력해주세요."
  }
  if (error.code === "AI_REPORT_NOT_FOUND") {
    return "기본 AI 요약 리포트가 아직 준비되지 않았습니다."
  }
  if (error.code === "UNAUTHORIZED" || error.status === 401) {
    return "로그인이 필요한 기능입니다."
  }
  return error.message || fallback
}

function isNotFound(error: unknown) {
  return error instanceof ApiError
    && (error.status === 404 || error.code === "AI_REPORT_NOT_FOUND")
}

function hasText(value: string | null | undefined) {
  return Boolean(value?.trim())
}

function isReportInProgress(status: AiReport["status"] | undefined) {
  return status === "REQUESTED"
    || status === "QUEUED"
    || status === "PROCESSING"
    || status === "PENDING"
}

function isReportFailed(status: AiReport["status"] | undefined) {
  return status === "PUBLISH_FAILED"
    || status === "GENERATION_FAILED"
    || status === "BLOCKED"
    || status === "FAILED"
}

function statusLabel(status: AiReport["status"]) {
  if (status === "COMPLETED") return "완료"
  if (isReportInProgress(status)) return "생성 중"
  return "실패"
}

function ReportSection({
  title,
  children,
}: {
  title: string
  children: ReactNode
}) {
  return (
    <section className="rounded-lg border border-border/60 bg-background/70 p-3">
      <h4 className="mb-2 text-xs font-semibold text-foreground">{title}</h4>
      {children}
    </section>
  )
}

export function AiReportPanel({
  roomId,
  roomStatus,
  compact = false,
}: {
  roomId: number
  roomStatus: RoomStatus
  compact?: boolean
}) {
  const [report, setReport] = useState<AiReport | null>(null)
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState("")
  const [generateOpen, setGenerateOpen] = useState(false)
  const [customPrompts, setCustomPrompts] = useState<CustomPromptDraft[]>([
    { id: 1, prompt: "" },
  ])
  const requestSeqRef = useRef(0)
  const mountedRef = useRef(false)
  const nextPromptIdRef = useRef(2)

  const loadReport = useCallback(async (showLoading = true) => {
    if (roomStatus !== "CLOSED") {
      setReport(null)
      setError("")
      setLoading(false)
      return
    }

    const requestSeq = ++requestSeqRef.current
    if (showLoading) setLoading(true)
    setError("")

    try {
      const response = await aiReportApi.get(roomId)
      if (!mountedRef.current || requestSeq !== requestSeqRef.current) return
      setReport(response)
    } catch (requestError) {
      if (!mountedRef.current || requestSeq !== requestSeqRef.current) return
      if (isNotFound(requestError)) {
        setReport(null)
        return
      }
      setError(messageOf(requestError))
    } finally {
      if (mountedRef.current && requestSeq === requestSeqRef.current) {
        setLoading(false)
      }
    }
  }, [roomId, roomStatus])

  useEffect(() => {
    mountedRef.current = true
    void loadReport()

    return () => {
      mountedRef.current = false
      requestSeqRef.current += 1
    }
  }, [loadReport])

  useEffect(() => {
    if (roomStatus !== "CLOSED" || (report && !isReportInProgress(report.status))) return

    const intervalId = window.setInterval(() => {
      void loadReport(false)
    }, 5000)

    return () => window.clearInterval(intervalId)
  }, [loadReport, report, roomStatus])

  const resetGenerateDialog = () => {
    setCustomPrompts([{ id: 1, prompt: "" }])
    nextPromptIdRef.current = 2
  }

  const closeGenerateDialog = () => {
    if (submitting) return
    setGenerateOpen(false)
    resetGenerateDialog()
  }

  const openGenerateDialog = () => {
    if (roomStatus !== "CLOSED" || report?.status !== "COMPLETED" || submitting) return
    setError("")
    resetGenerateDialog()
    setGenerateOpen(true)
  }

  const generateReport = async (body: AiReportGenerateRequest) => {
    if (roomStatus !== "CLOSED" || report?.status !== "COMPLETED" || submitting) return

    setSubmitting(true)
    setError("")

    try {
      const response = await aiReportApi.generate(roomId, body)
      if (!mountedRef.current) return
      setReport(response)
      setGenerateOpen(false)
      resetGenerateDialog()
    } catch (requestError) {
      if (!mountedRef.current) return
      setError(messageOf(requestError, "커스텀 AI 리포트 생성 요청에 실패했습니다."))
    } finally {
      if (mountedRef.current) setSubmitting(false)
    }
  }

  const addCustomPrompt = () => {
    if (customPrompts.length >= 3) return
    const id = nextPromptIdRef.current
    nextPromptIdRef.current += 1
    setCustomPrompts((prompts) => [...prompts, { id, prompt: "" }])
  }

  const removeCustomPrompt = (id: number) => {
    setCustomPrompts((prompts) => prompts.length === 1
      ? prompts
      : prompts.filter((prompt) => prompt.id !== id))
  }

  const updateCustomPrompt = (id: number, prompt: string) => {
    setCustomPrompts((prompts) => prompts.map((item) => (
      item.id === id ? { ...item, prompt } : item
    )))
  }

  const submitCustomReport = () => {
    const customPromptBody = customPrompts
      .map((item, index) => ({
        label: `커스텀 리포트 ${index + 1}`,
        prompt: item.prompt.trim(),
      }))
      .filter((item) => item.prompt.length > 0)

    if (customPromptBody.length === 0) return
    void generateReport({ customPrompts: customPromptBody })
  }

  const generateDialog = (
    <Dialog open={generateOpen} onOpenChange={(open) => {
      if (open) {
        setGenerateOpen(true)
        return
      }
      closeGenerateDialog()
    }}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>커스텀 AI 리포트 생성</DialogTitle>
          <DialogDescription>
            기본 AI 요약본을 바탕으로 추가로 궁금한 내용을 요청합니다.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-3">
            {customPrompts.map((item, index) => (
              <div key={item.id} className="space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <label className="text-xs font-medium text-foreground">
                    프롬프트 {index + 1}
                  </label>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="size-7"
                    disabled={submitting || customPrompts.length === 1}
                    onClick={() => removeCustomPrompt(item.id)}
                  >
                    <Trash2 className="size-3.5" />
                  </Button>
                </div>
                <textarea
                  value={item.prompt}
                  onChange={(event) => updateCustomPrompt(item.id, event.target.value)}
                  rows={4}
                  maxLength={1000}
                  placeholder="추가로 분석하고 싶은 내용을 입력하세요."
                  className="w-full resize-none rounded-lg border border-input bg-background px-3 py-2 text-sm outline-none focus:border-primary"
                />
              </div>
            ))}
          </div>

          <Button
            type="button"
            variant="outline"
            size="sm"
            className="w-full gap-1.5 text-xs"
            disabled={submitting || customPrompts.length >= 3}
            onClick={addCustomPrompt}
          >
            <Plus className="size-3.5" />
            프롬프트 추가
          </Button>

          <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" disabled={submitting} onClick={closeGenerateDialog}>
              닫기
            </Button>
            <Button
              type="button"
              disabled={submitting || customPrompts.every((item) => !item.prompt.trim())}
              onClick={submitCustomReport}
            >
              {submitting && <Loader2 className="mr-2 size-4 animate-spin" />}
              생성 요청
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )

  if (roomStatus !== "CLOSED") {
    return (
      <Card size="sm">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-sm">
            <BarChart3 className="size-4 text-muted-foreground" />
            AI 요약 리포트
          </CardTitle>
          <CardDescription className="text-xs">
            토론 종료 후 AI 요약 리포트가 자동으로 생성됩니다.
          </CardDescription>
        </CardHeader>
      </Card>
    )
  }

  const statusBadge = report?.status ? (
    <Badge variant={report.status === "COMPLETED" ? "secondary" : "outline"} className="text-[10px]">
      {statusLabel(report.status)}
    </Badge>
  ) : null

  const waitingMessage = (
    <div className="space-y-2">
      <p className="flex items-center gap-2 text-xs leading-relaxed text-muted-foreground">
        <Loader2 className="size-3.5 animate-spin text-primary" />
        기본 AI 요약 리포트가 자동 생성 중입니다.
      </p>
      <Button
        type="button"
        size="sm"
        variant="outline"
        className="w-full gap-1.5 text-xs"
        disabled={loading}
        onClick={() => loadReport()}
      >
        {loading ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}
        상태 새로고침
      </Button>
    </div>
  )

  const failedMessage = report && isReportFailed(report.status) ? (
    <div className="space-y-2">
      <p className="text-xs leading-relaxed text-destructive">
        {report.errorMessage || "AI 요약 리포트 생성에 실패했습니다."}
      </p>
      <Button
        type="button"
        size="sm"
        variant="outline"
        className="w-full gap-1.5 text-xs"
        disabled={loading}
        onClick={() => loadReport()}
      >
        {loading ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}
        상태 새로고침
      </Button>
    </div>
  ) : null

  if (compact) {
    return (
      <>
        <div className="rounded-lg border border-border/60 bg-muted/20 p-3">
          <div className="mb-2 flex items-center justify-between gap-2">
            <span className="flex items-center gap-1.5 text-xs font-semibold text-foreground">
              <BarChart3 className="size-3.5 text-primary" />
              AI 요약 리포트
            </span>
            {statusBadge}
          </div>

          {error && (
            <p className="mb-2 rounded-md bg-destructive/10 px-2 py-1.5 text-[11px] leading-relaxed text-destructive">{error}</p>
          )}

          {loading ? (
            <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
              <Loader2 className="size-3.5 animate-spin" />
              리포트 상태 확인 중
            </div>
          ) : !report || isReportInProgress(report.status) ? (
            waitingMessage
          ) : failedMessage ? (
            failedMessage
          ) : (
            <div className="space-y-2">
              <p className="text-[11px] leading-relaxed text-muted-foreground">
                리포트 생성 완료
                {report.completedAt ? ` · ${new Date(report.completedAt).toLocaleString("ko-KR")}` : ""}
              </p>
              <Button size="sm" variant="outline" className="w-full gap-1.5 text-xs" disabled={submitting} onClick={openGenerateDialog}>
                {submitting ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
                커스텀 리포트 추가
              </Button>
            </div>
          )}
        </div>
        {generateDialog}
      </>
    )
  }

  return (
    <>
      <Card size="sm">
        <CardHeader>
          <div className="flex items-start justify-between gap-3">
            <div>
              <CardTitle className="flex items-center gap-2 text-sm">
                <BarChart3 className="size-4 text-primary" />
                AI 요약 리포트
              </CardTitle>
              <CardDescription className="text-xs">
                종료된 토론 내용을 바탕으로 기본 요약 리포트를 자동 생성합니다.
              </CardDescription>
            </div>
            {statusBadge}
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          {error && (
            <p className="rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{error}</p>
          )}

          {loading ? (
            <div className="flex items-center justify-center gap-2 rounded-lg border border-border/60 px-3 py-8 text-xs text-muted-foreground">
              <Loader2 className="size-4 animate-spin" />
              AI 리포트를 불러오는 중입니다.
            </div>
          ) : !report || isReportInProgress(report.status) ? (
            <div className="rounded-lg border border-border/60 bg-muted/30 p-3">
              {waitingMessage}
            </div>
          ) : failedMessage ? (
            <div className="rounded-lg bg-destructive/10 p-3">
              {failedMessage}
            </div>
          ) : (
            <div className="space-y-3">
              {hasText(report.coreLine) && (
                <ReportSection title="핵심 주제">
                  <p className="whitespace-pre-wrap text-xs leading-relaxed text-foreground">{report.coreLine}</p>
                </ReportSection>
              )}

              {report.keyIssues.length > 0 && (
                <ReportSection title="핵심 쟁점">
                  <ul className="space-y-1.5">
                    {report.keyIssues.map((issue) => (
                      <li key={issue} className="flex gap-2 text-xs leading-relaxed text-foreground">
                        <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-primary" />
                        <span className="min-w-0 break-words">{issue}</span>
                      </li>
                    ))}
                  </ul>
                </ReportSection>
              )}

              {hasText(report.aiSummary) && (
                <ReportSection title="AI 종합 정리">
                  <p className="whitespace-pre-wrap text-xs leading-relaxed text-foreground">{report.aiSummary}</p>
                </ReportSection>
              )}

              {hasText(report.commonGround) && (
                <ReportSection title="공통 의견">
                  <p className="whitespace-pre-wrap text-xs leading-relaxed text-foreground">{report.commonGround}</p>
                </ReportSection>
              )}

              {hasText(report.aiOpinion) && (
                <ReportSection title="AI 의견">
                  <p className="whitespace-pre-wrap text-xs leading-relaxed text-foreground">{report.aiOpinion}</p>
                </ReportSection>
              )}

              {report.customReports.length > 0 && (
                <ReportSection title="커스텀 리포트">
                  <div className="space-y-3">
                    {report.customReports.map((customReport, index) => (
                      <div key={`${customReport.requestLabel}-${index}`} className="rounded-md bg-muted/40 p-2">
                        <div className="mb-1 flex flex-wrap items-center gap-1.5">
                          <Badge variant="outline" className="text-[10px]">
                            {customReport.requestLabel || customReport.label}
                          </Badge>
                          {customReport.label && customReport.label !== customReport.requestLabel && (
                            <span className="text-[10px] text-muted-foreground">{customReport.label}</span>
                          )}
                        </div>
                        <p className="mb-2 text-[11px] leading-relaxed text-muted-foreground">{customReport.prompt}</p>
                        <p className="whitespace-pre-wrap text-xs leading-relaxed text-foreground">{customReport.content}</p>
                      </div>
                    ))}
                  </div>
                </ReportSection>
              )}

              <Button size="sm" variant="outline" className="w-full gap-1.5 text-xs" disabled={submitting} onClick={openGenerateDialog}>
                {submitting ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
                커스텀 리포트 추가
              </Button>

              {report.completedAt && (
                <p className="text-[11px] text-muted-foreground">
                  생성 완료: {new Date(report.completedAt).toLocaleString("ko-KR")}
                </p>
              )}
            </div>
          )}
        </CardContent>
      </Card>
      {generateDialog}
    </>
  )
}
