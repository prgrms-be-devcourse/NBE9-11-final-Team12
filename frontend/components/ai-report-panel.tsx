"use client"

import { type ReactNode, useCallback, useEffect, useRef, useState } from "react"
import { BarChart3, Download, FileText, Loader2, Plus, RefreshCw, Sparkles, Trash2 } from "lucide-react"
import { ApiError } from "@/lib/api/client"
import { aiReportApi, paymentApi } from "@/lib/api/services"
import { getAiReportProgressActionState } from "@/lib/ai-report-progress"
import { requestTossCardPayment } from "@/lib/toss-payments"
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
type GenerationKind = "base" | "custom"

const CUSTOM_AI_REPORT_PRICE = 3000

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

function AiReportProgressAction({
  report,
  generationKind,
  downloading,
  onDownloadClick,
}: {
  report: AiReport | null
  generationKind: GenerationKind | null
  downloading: boolean
  onDownloadClick: () => void
}) {
  const state = getAiReportProgressActionState(report, generationKind)
  if (!state) return null

  const icon = state.clickable
    ? <Download className="size-3.5" />
    : state.busy
      ? <Loader2 className="size-3.5 animate-spin" />
      : <FileText className="size-3.5" />

  if (state.clickable) {
    return (
      <Button
        type="button"
        size="sm"
        variant="secondary"
        className="w-full gap-1.5 text-xs"
        disabled={downloading}
        onClick={onDownloadClick}
      >
        {downloading ? <Loader2 className="size-3.5 animate-spin" /> : icon}
        {state.label}
      </Button>
    )
  }

  return (
    <div
      className="flex h-9 w-full items-center justify-center gap-1.5 rounded-md border border-border bg-muted/50 px-3 text-xs font-medium text-muted-foreground"
      aria-live="polite"
    >
      {icon}
      {state.label}
    </div>
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
  const [choiceOpen, setChoiceOpen] = useState(false)
  const [baseConfirmOpen, setBaseConfirmOpen] = useState(false)
  const [customOpen, setCustomOpen] = useState(false)
  const [customChoiceOpen, setCustomChoiceOpen] = useState(false)
  const [downloadConfirmOpen, setDownloadConfirmOpen] = useState(false)
  const [downloadingPdf, setDownloadingPdf] = useState(false)
  const [generationKind, setGenerationKind] = useState<GenerationKind | null>(null)
  const [customPrompts, setCustomPrompts] = useState<CustomPromptDraft[]>([
    { id: 1, prompt: "" },
  ])
  const requestSeqRef = useRef(0)
  const mountedRef = useRef(false)
  const nextPromptIdRef = useRef(2)
  const generationStorageKey = `ai-report-generation-kind:${roomId}`

  const rememberGenerationKind = useCallback((kind: GenerationKind) => {
    setGenerationKind(kind)
    window.localStorage.setItem(generationStorageKey, kind)
  }, [generationStorageKey])

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
    const storedKind = window.localStorage.getItem(generationStorageKey)
    if (storedKind === "base" || storedKind === "custom") {
      setGenerationKind(storedKind)
    }
    void loadReport()

    return () => {
      mountedRef.current = false
      requestSeqRef.current += 1
    }
  }, [generationStorageKey, loadReport])

  useEffect(() => {
    const pdfInProgress = report?.pdf?.pdfExportId
      && !report.pdf.downloadAvailable
      && report.pdf.pdfStatus !== "FAILED"
    if (roomStatus !== "CLOSED" || (report && !isReportInProgress(report.status) && !pdfInProgress)) return

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
    setCustomOpen(false)
    resetGenerateDialog()
  }

  const openChoiceDialog = () => {
    if (roomStatus !== "CLOSED" || submitting) return
    setError("")
    setChoiceOpen(true)
  }

  const openCustomPromptDialog = () => {
    if (submitting) return
    setError("")
    resetGenerateDialog()
    setCustomChoiceOpen(false)
    setChoiceOpen(false)
    setCustomOpen(true)
  }

  const generateReport = async (body: AiReportGenerateRequest) => {
    if (roomStatus !== "CLOSED" || report?.status !== "COMPLETED" || submitting) return

    setSubmitting(true)
    setError("")

    try {
      rememberGenerationKind("custom")
      const payment = await paymentApi.createCustomAiReportPayment({
        roomId,
        amount: CUSTOM_AI_REPORT_PRICE,
        customPrompts: body.customPrompts,
      })
      if (!mountedRef.current) return

      const origin = window.location.origin
      const successUrl = `${origin}/payments/toss/success?roomId=${roomId}`
      const failUrl = `${origin}/payments/toss/fail?roomId=${roomId}`

      await requestTossCardPayment({
        amount: payment.amount,
        orderId: payment.orderId,
        orderName: payment.orderName,
        successUrl,
        failUrl,
      })
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

  const requestBaseReport = async () => {
    if (roomStatus !== "CLOSED" || submitting) return

    setSubmitting(true)
    setError("")

    try {
      rememberGenerationKind("base")
      const response = await aiReportApi.generate(roomId, { customPrompts: [] })
      if (!mountedRef.current) return
      setReport(response)
      void loadReport(false)
      setChoiceOpen(false)
      setBaseConfirmOpen(false)
    } catch (requestError) {
      if (!mountedRef.current) return
      setError(messageOf(requestError, "기본 AI 요약 리포트 생성 요청에 실패했습니다."))
    } finally {
      if (mountedRef.current) setSubmitting(false)
    }
  }

  const requestExistingReportPdf = async (kind: GenerationKind) => {
    if (roomStatus !== "CLOSED" || submitting) return
    if (report?.pdf?.downloadAvailable) {
      setChoiceOpen(false)
      setCustomChoiceOpen(false)
      setDownloadConfirmOpen(true)
      return
    }

    setSubmitting(true)
    setError("")

    try {
      rememberGenerationKind(kind)
      const response = await aiReportApi.generate(roomId, { customPrompts: [] })
      if (!mountedRef.current) return
      setReport(response)
      void loadReport(false)
      setChoiceOpen(false)
      setCustomChoiceOpen(false)
    } catch (requestError) {
      if (!mountedRef.current) return
      setError(messageOf(requestError, "AI 요약 리포트 PDF 준비 요청에 실패했습니다."))
    } finally {
      if (mountedRef.current) setSubmitting(false)
    }
  }

  const openDownloadConfirm = () => {
    if (!report?.pdf?.downloadAvailable || downloadingPdf) return
    setError("")
    setDownloadConfirmOpen(true)
  }

  const downloadPdf = async () => {
    if (!report?.pdf?.downloadAvailable || downloadingPdf) return

    setDownloadingPdf(true)
    setError("")

    try {
      const response = await aiReportApi.downloadUrl(roomId)
      if (!mountedRef.current) return

      const link = document.createElement("a")
      link.href = response.downloadUrl
      link.rel = "noreferrer"
      link.click()
      setDownloadConfirmOpen(false)
    } catch (requestError) {
      if (!mountedRef.current) return
      setError(messageOf(requestError, "AI 요약 리포트 PDF 다운로드 URL을 발급하지 못했습니다."))
    } finally {
      if (mountedRef.current) setDownloadingPdf(false)
    }
  }

  const baseReportCompleted = report?.status === "COMPLETED"
  const customReportCompleted = baseReportCompleted && report.customReports.length > 0
  const progressState = getAiReportProgressActionState(report, generationKind)

  const choiceDialog = (
    <Dialog open={choiceOpen} onOpenChange={setChoiceOpen}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>AI 요약 리포트 생성</DialogTitle>
          <DialogDescription>
            생성할 리포트 종류를 선택하세요.
          </DialogDescription>
        </DialogHeader>

        <div className="grid gap-3 sm:grid-cols-2">
          <button
            type="button"
            disabled={submitting}
            onClick={() => {
              if (baseReportCompleted) {
                void requestExistingReportPdf("base")
                return
              }
              setChoiceOpen(false)
              setBaseConfirmOpen(true)
            }}
            className="rounded-lg border border-border bg-background p-4 text-left transition-colors hover:border-primary disabled:opacity-60"
          >
            <span className="mb-2 flex items-center gap-2 text-sm font-semibold text-foreground">
              <FileText className="size-4 text-primary" />
              {baseReportCompleted ? "기본 리포트 다운로드" : "기본 리포트 생성"}
            </span>
            <span className="block text-xs leading-relaxed text-muted-foreground">
              토론방에서 나눈 의견을 요약하여 핵심적인 정보를 추출한 리포트를 생성합니다. 생성에는 시간이 소요되며 생성 완료 시 이메일로 알림이 발송됩니다.
            </span>
          </button>

          <button
            type="button"
            disabled={submitting || !baseReportCompleted}
            onClick={() => {
              setChoiceOpen(false)
              if (customReportCompleted) {
                setCustomChoiceOpen(true)
                return
              }
              openCustomPromptDialog()
            }}
            className="rounded-lg border border-border bg-background p-4 text-left transition-colors hover:border-primary disabled:opacity-60"
          >
            <span className="mb-2 flex items-center gap-2 text-sm font-semibold text-foreground">
              <Sparkles className="size-4 text-primary" />
              {customReportCompleted ? "커스텀 리포트 다운로드" : "커스텀 리포트 생성"}
            </span>
            <span className="block text-xs leading-relaxed text-muted-foreground">
              기본 리포트와 더불어 원하시는 내용으로 정리된 커스텀 리포트가 생성됩니다. 프롬프트를 입력하여 나만의 요약 리포트를 만들어보세요
            </span>
          </button>
        </div>
      </DialogContent>
    </Dialog>
  )

  const baseConfirmDialog = (
    <Dialog open={baseConfirmOpen} onOpenChange={(open) => {
      if (submitting) return
      setBaseConfirmOpen(open)
    }}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>기본 리포트 생성</DialogTitle>
          <DialogDescription>
            기본 리포트 생성 하시겠습니까?
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
          <Button type="button" variant="outline" disabled={submitting} onClick={() => setBaseConfirmOpen(false)}>
            닫기
          </Button>
          <Button type="button" disabled={submitting} onClick={() => void requestBaseReport()}>
            {submitting && <Loader2 className="mr-2 size-4 animate-spin" />}
            확인
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )

  const customChoiceDialog = (
    <Dialog open={customChoiceOpen} onOpenChange={setCustomChoiceOpen}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>커스텀 리포트</DialogTitle>
          <DialogDescription>
            기존 리포트를 다운로드하거나 새 커스텀 리포트를 생성할 수 있습니다.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-2">
          <Button
            type="button"
            variant="secondary"
            className="w-full justify-start gap-2"
            disabled={submitting}
            onClick={() => void requestExistingReportPdf("custom")}
          >
            <Download className="size-4" />
            기존 커스텀 다운로드
          </Button>
          <Button
            type="button"
            variant="outline"
            className="w-full justify-start gap-2"
            disabled={submitting}
            onClick={openCustomPromptDialog}
          >
            <Plus className="size-4" />
            새 커스텀 리포트 생성하기
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )

  const generateDialog = (
    <Dialog open={customOpen} onOpenChange={(open) => {
      if (open) {
        setCustomOpen(true)
        return
      }
      closeGenerateDialog()
    }}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>커스텀 리포트 생성</DialogTitle>
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
              확인
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )

  const downloadDialog = (
    <Dialog open={downloadConfirmOpen} onOpenChange={(open) => {
      if (downloadingPdf) return
      setDownloadConfirmOpen(open)
    }}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>AI 요약 리포트 PDF 다운로드</DialogTitle>
          <DialogDescription>
            요약 리포트 PDF를 다운로드 받으시겠습니까?
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="outline"
            disabled={downloadingPdf}
            onClick={() => setDownloadConfirmOpen(false)}
          >
            아니오
          </Button>
          <Button type="button" disabled={downloadingPdf} onClick={() => void downloadPdf()}>
            {downloadingPdf && <Loader2 className="mr-2 size-4 animate-spin" />}
            네
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  )

  if (roomStatus !== "CLOSED") {
    return (
      <>
        <Card size="sm">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-sm">
              <BarChart3 className="size-4 text-muted-foreground" />
              AI 요약 리포트
            </CardTitle>
            <CardDescription className="text-xs">
              토론 종료 후 AI 요약 리포트를 생성할 수 있습니다.
            </CardDescription>
          </CardHeader>
        </Card>
        {downloadDialog}
      </>
    )
  }

  const showStatusBadge = report?.status && (!isReportInProgress(report.status) || Boolean(progressState))
  const statusBadge = showStatusBadge ? (
    <Badge variant={report.status === "COMPLETED" ? "secondary" : "outline"} className="text-[10px]">
      {statusLabel(report.status)}
    </Badge>
  ) : null

  const waitingMessage = (
    <div className="space-y-2">
      {progressState ? (
        <>
          <AiReportProgressAction
            report={report}
            generationKind={generationKind}
            downloading={downloadingPdf}
            onDownloadClick={openDownloadConfirm}
          />
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
        </>
      ) : (
        <Button
          type="button"
          size="sm"
          variant="outline"
          className="w-full gap-1.5 text-xs"
          disabled={submitting}
          onClick={openChoiceDialog}
        >
          {submitting ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
          AI 요약 리포트 생성
        </Button>
      )}
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
      <AiReportProgressAction
        report={report}
        generationKind={generationKind}
        downloading={downloadingPdf}
        onDownloadClick={openDownloadConfirm}
      />
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
              <div className="grid gap-2 sm:grid-cols-2">
                <Button size="sm" variant="outline" className="w-full gap-1.5 text-xs" disabled={submitting} onClick={openChoiceDialog}>
                  {submitting ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
                  AI 요약 리포트 생성
                </Button>
                {progressState && (
                  <AiReportProgressAction
                    report={report}
                    generationKind={generationKind}
                    downloading={downloadingPdf}
                    onDownloadClick={openDownloadConfirm}
                  />
                )}
              </div>
            </div>
          )}
        </div>
        {choiceDialog}
        {baseConfirmDialog}
        {customChoiceDialog}
        {generateDialog}
        {downloadDialog}
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
              종료된 토론 내용을 바탕으로 AI 요약 리포트를 생성합니다.
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

              <div className="grid gap-2 sm:grid-cols-2">
                <Button size="sm" variant="outline" className="w-full gap-1.5 text-xs" disabled={submitting} onClick={openChoiceDialog}>
                  {submitting ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
                  AI 요약 리포트 생성
                </Button>
                {progressState && (
                  <AiReportProgressAction
                    report={report}
                    generationKind={generationKind}
                    downloading={downloadingPdf}
                    onDownloadClick={openDownloadConfirm}
                  />
                )}
              </div>

              {report.completedAt && (
                <p className="text-[11px] text-muted-foreground">
                  생성 완료: {new Date(report.completedAt).toLocaleString("ko-KR")}
                </p>
              )}
            </div>
          )}
        </CardContent>
      </Card>
      {choiceDialog}
      {baseConfirmDialog}
      {customChoiceDialog}
      {generateDialog}
      {downloadDialog}
    </>
  )
}
