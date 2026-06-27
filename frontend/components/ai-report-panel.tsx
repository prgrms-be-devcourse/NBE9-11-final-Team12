"use client"

import { type ReactNode, useCallback, useEffect, useRef, useState } from "react"
import { BarChart3, Loader2, RefreshCw, Sparkles } from "lucide-react"
import { ApiError } from "@/lib/api/client"
import { aiReportApi } from "@/lib/api/services"
import type { AiReport } from "@/lib/api/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

type RoomStatus = "OPEN" | "CLOSED"

function messageOf(error: unknown, fallback = "AI 리포트를 불러오지 못했습니다.") {
  if (!(error instanceof ApiError)) return fallback
  if (error.code === "AI_REPORT_ROOM_NOT_CLOSED") {
    return "종료된 토론방만 AI 리포트를 생성할 수 있습니다."
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
}: {
  roomId: number
  roomStatus: RoomStatus
}) {
  const [report, setReport] = useState<AiReport | null>(null)
  const [loading, setLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState("")
  const requestSeqRef = useRef(0)
  const mountedRef = useRef(false)

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
    if (roomStatus !== "CLOSED" || report?.status !== "PENDING") return

    const intervalId = window.setInterval(() => {
      void loadReport(false)
    }, 5000)

    return () => window.clearInterval(intervalId)
  }, [loadReport, report?.status, roomStatus])

  const generateReport = async () => {
    if (roomStatus !== "CLOSED" || submitting) return

    setSubmitting(true)
    setError("")
    try {
      const response = await aiReportApi.generate(roomId)
      if (!mountedRef.current) return
      setReport(response)
    } catch (requestError) {
      if (!mountedRef.current) return
      setError(messageOf(requestError, "AI 리포트 생성에 실패했습니다."))
    } finally {
      if (mountedRef.current) setSubmitting(false)
    }
  }

  if (roomStatus !== "CLOSED") {
    return (
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
    )
  }

  return (
    <Card size="sm">
      <CardHeader>
        <div className="flex items-start justify-between gap-3">
          <div>
            <CardTitle className="flex items-center gap-2 text-sm">
              <BarChart3 className="size-4 text-primary" />
              AI 요약 리포트
            </CardTitle>
            <CardDescription className="text-xs">
              종료된 토론 내용을 바탕으로 별도 리포트를 생성합니다.
            </CardDescription>
          </div>
          {report?.status && (
            <Badge variant={report.status === "COMPLETED" ? "secondary" : "outline"} className="text-[10px]">
              {report.status === "COMPLETED" ? "완료" : report.status === "PENDING" ? "생성 중" : "실패"}
            </Badge>
          )}
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
        ) : !report ? (
          <div className="space-y-3">
            <p className="text-xs leading-relaxed text-muted-foreground">
              아직 생성된 AI 요약 리포트가 없습니다.
            </p>
            <Button size="sm" className="w-full gap-1.5 text-xs" disabled={submitting} onClick={generateReport}>
              {submitting ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
              AI 요약 리포트 생성
            </Button>
          </div>
        ) : report.status === "PENDING" ? (
          <div className="space-y-3 rounded-lg border border-border/60 bg-muted/30 p-3">
            <div className="flex items-center gap-2 text-xs font-medium text-foreground">
              <Loader2 className="size-4 animate-spin text-primary" />
              AI 리포트를 생성 중입니다.
            </div>
            <p className="text-xs leading-relaxed text-muted-foreground">
              약 2분 정도 걸릴 수 있습니다. 완료되면 자동으로 내용이 표시됩니다.
            </p>
          </div>
        ) : report.status === "FAILED" ? (
          <div className="space-y-3">
            <p className="rounded-lg bg-destructive/10 px-3 py-2 text-xs leading-relaxed text-destructive">
              {report.errorMessage || "AI 리포트 생성에 실패했습니다."}
            </p>
            <Button size="sm" variant="outline" className="w-full gap-1.5 text-xs" disabled={submitting} onClick={generateReport}>
              {submitting ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}
              다시 생성
            </Button>
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
              <ReportSection title="AI의 개인적 의견">
                <p className="whitespace-pre-wrap text-xs leading-relaxed text-foreground">{report.aiOpinion}</p>
              </ReportSection>
            )}

            {report.customReports.length > 0 && (
              <ReportSection title="개인화 리포트">
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

            {report.completedAt && (
              <p className="text-[11px] text-muted-foreground">
                생성 완료: {new Date(report.completedAt).toLocaleString("ko-KR")}
              </p>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}
