"use client"

import Link from "next/link"
import { FormEvent, type ReactNode, useEffect, useMemo, useState } from "react"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useAuth } from "@/components/auth-provider"
import { ApiError } from "@/lib/api/client"
import type {
  ClassifiedIssueCandidate,
  ClassifiedIssueNews,
  OffTopicAiReview,
  RoomSummary,
  SpeechReportDetail,
  SpeechReportStatus,
  SpeechReportSummary,
  TopicSummary,
  ViolationSeverity,
} from "@/lib/api/types"
import { adminApi, roomApi, topicApi } from "@/lib/api/services"
import {
  AlertTriangle,
  CheckCircle2,
  ExternalLink,
  FileText,
  Loader2,
  Pencil,
  Power,
  RefreshCw,
  Shield,
  Sparkles,
  Trash2,
  Zap,
} from "lucide-react"

type TopicDraft = {
  title: string
  description: string
  category: string
  sourceUrl: string
}

type TopicEditDraft = TopicDraft & {
  topicId: number
}

type RoomDraft = {
  topicId: number
  topicTitle: string
  title: string
  maxParticipants: string
}

const REPORT_STATUS_FILTERS: { value: SpeechReportStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "PENDING", label: "대기" },
  { value: "REVIEWING", label: "검토 중" },
  { value: "RESOLVED", label: "처리됨" },
  { value: "REJECTED", label: "반려됨" },
]

const REPORT_STATUS_LABELS: Record<SpeechReportStatus, string> = {
  PENDING: "대기",
  REVIEWING: "검토 중",
  RESOLVED: "처리됨",
  REJECTED: "반려됨",
}

const REPORT_REASON_LABELS: Record<string, string> = {
  ABUSE_HARASSMENT: "욕설 / 괴롭힘",
  HATE_SPEECH: "혐오 발언",
  SEXUAL_CONTENT: "성적 콘텐츠",
  THREAT_VIOLENCE: "위협 / 폭력",
  SPAM: "광고 / 스팸",
  MISINFORMATION: "허위 정보",
  PRIVACY_VIOLATION: "개인정보 노출",
  OFF_TOPIC: "논점 이탈",
  OTHER: "기타",
}

const SEVERITY_OPTIONS: { value: ViolationSeverity; label: string }[] = [
  { value: "LOW", label: "낮음" },
  { value: "MEDIUM", label: "보통" },
  { value: "HIGH", label: "높음" },
  { value: "CRITICAL", label: "심각" },
]

function messageOf(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

function formatPercent(value: number | null) {
  if (value === null) return null
  const normalized = value <= 1 ? value * 100 : value
  return `${Math.round(normalized)}%`
}

function aiReviewLabel(review: OffTopicAiReview | null | undefined) {
  if (!review) return "AI 검토 없음"
  if (review.status === "PENDING") return "AI 검토 중"
  if (review.status === "FAILED") return "AI 검토 실패"
  return review.offTopic ? "AI 검토 완료 · 논점 이탈 가능성 있음" : "AI 검토 완료 · 논점 이탈 가능성 낮음"
}

function categoryOf(candidate: ClassifiedIssueCandidate) {
  return candidate.news.find((item) => item.category)?.category ?? "기타"
}

function sourceUrlOf(candidate: ClassifiedIssueCandidate) {
  return candidate.news[0]?.news.originallink || candidate.news[0]?.news.link || ""
}

function descriptionOf(candidate: ClassifiedIssueCandidate) {
  return candidate.news
    .slice(0, 2)
    .map((item) => item.news.description || item.news.title)
    .filter(Boolean)
    .join("\n\n")
}

function draftFromCandidate(candidate: ClassifiedIssueCandidate): TopicDraft {
  return {
    title: candidate.keyword,
    description: descriptionOf(candidate),
    category: categoryOf(candidate),
    sourceUrl: sourceUrlOf(candidate),
  }
}

function draftFromNews(item: ClassifiedIssueNews): TopicDraft {
  return {
    title: item.news.title,
    description: item.news.description,
    category: item.category || "기타",
    sourceUrl: item.news.originallink || item.news.link || "",
  }
}

function emptyDraft(): TopicDraft {
  return {
    title: "",
    description: "",
    category: "",
    sourceUrl: "",
  }
}

export default function AdminDashboardPage() {
  const { user, loading } = useAuth()
  const [candidates, setCandidates] = useState<ClassifiedIssueCandidate[]>([])
  const [candidatesLoading, setCandidatesLoading] = useState(false)
  const [candidatesError, setCandidatesError] = useState("")
  const [candidateMessage, setCandidateMessage] = useState("")
  const [topics, setTopics] = useState<TopicSummary[]>([])
  const [topicsLoading, setTopicsLoading] = useState(false)
  const [topicsError, setTopicsError] = useState("")
  const [topicMessage, setTopicMessage] = useState("")
  const [rooms, setRooms] = useState<RoomSummary[]>([])
  const [roomsLoading, setRoomsLoading] = useState(false)
  const [roomsError, setRoomsError] = useState("")
  const [roomMessage, setRoomMessage] = useState("")
  const [manualDraft, setManualDraft] = useState<TopicDraft>(emptyDraft)
  const [editDraft, setEditDraft] = useState<TopicEditDraft | null>(null)
  const [roomDraft, setRoomDraft] = useState<RoomDraft | null>(null)
  const [reports, setReports] = useState<SpeechReportSummary[]>([])
  const [reportsLoading, setReportsLoading] = useState(false)
  const [reportsError, setReportsError] = useState("")
  const [reportsMessage, setReportsMessage] = useState("")
  const [reportStatusFilter, setReportStatusFilter] = useState<SpeechReportStatus | "ALL">("ALL")
  const [selectedReportId, setSelectedReportId] = useState<number | null>(null)
  const [selectedReport, setSelectedReport] = useState<SpeechReportDetail | null>(null)
  const [reportDetailLoading, setReportDetailLoading] = useState(false)
  const [reportDetailError, setReportDetailError] = useState("")
  const [resolutionNote, setResolutionNote] = useState("")
  const [reviewSeverity, setReviewSeverity] = useState<ViolationSeverity>("MEDIUM")
  const [mutatingKey, setMutatingKey] = useState("")

  const isAdmin = user?.role === "ADMIN"
  const candidateCountLabel = useMemo(() => `${candidates.length.toLocaleString()}개`, [candidates.length])
  const openRoomCount = useMemo(() => rooms.filter((room) => room.status === "OPEN").length, [rooms])

  const loadCandidates = async () => {
    setCandidatesLoading(true)
    setCandidatesError("")
    setCandidateMessage("")
    try {
      setCandidates(await adminApi.classifiedCandidates())
    } catch (error) {
      setCandidatesError(messageOf(error, "토픽 후보를 불러오지 못했습니다."))
    } finally {
      setCandidatesLoading(false)
    }
  }

  const refreshCandidates = async () => {
    setCandidatesLoading(true)
    setCandidatesError("")
    setCandidateMessage("")
    try {
      const refreshed = await adminApi.refreshClassifiedCandidates()
      setCandidates(refreshed)
      setCandidateMessage("최신 이슈 후보를 새로 가져왔습니다.")
    } catch (error) {
      setCandidatesError(messageOf(error, "토픽 후보 새로고침에 실패했습니다."))
    } finally {
      setCandidatesLoading(false)
    }
  }

  const loadTopics = async () => {
    setTopicsLoading(true)
    setTopicsError("")
    setTopicMessage("")
    try {
      const response = await topicApi.list(0, 100)
      setTopics(response.content)
    } catch (error) {
      setTopicsError(messageOf(error, "승인된 토픽 목록을 불러오지 못했습니다."))
    } finally {
      setTopicsLoading(false)
    }
  }

  const loadRooms = async () => {
    setRoomsLoading(true)
    setRoomsError("")
    setRoomMessage("")
    try {
      setRooms(await roomApi.list())
    } catch (error) {
      setRoomsError(messageOf(error, "토론방 목록을 불러오지 못했습니다."))
    } finally {
      setRoomsLoading(false)
    }
  }

  const loadReports = async () => {
    setReportsLoading(true)
    setReportsError("")
    try {
      const response = await adminApi.reports({
        status: reportStatusFilter === "ALL" ? undefined : reportStatusFilter,
        page: 0,
        size: 30,
      })
      setReports(response.content)
    } catch (error) {
      setReportsError(messageOf(error, "신고 목록을 불러오지 못했습니다."))
    } finally {
      setReportsLoading(false)
    }
  }

  const loadReportDetail = async (reportId: number) => {
    setSelectedReportId(reportId)
    setReportDetailLoading(true)
    setReportDetailError("")
    setReportsMessage("")
    try {
      const detail = await adminApi.reportDetail(reportId)
      setSelectedReport(detail)
      setResolutionNote(detail.resolutionNote ?? "")
      setReviewSeverity(detail.severity ?? "MEDIUM")
    } catch (error) {
      setSelectedReport(null)
      setReportDetailError(messageOf(error, "신고 상세를 불러오지 못했습니다."))
    } finally {
      setReportDetailLoading(false)
    }
  }

  useEffect(() => {
    if (!loading && isAdmin) {
      void loadCandidates()
      void loadTopics()
      void loadRooms()
    }
  }, [loading, isAdmin])

  useEffect(() => {
    if (!loading && isAdmin) {
      void loadReports()
    }
  }, [loading, isAdmin, reportStatusFilter])

  const createTopic = async (draft: TopicDraft, key: string) => {
    if (!draft.title.trim() || !draft.category.trim()) return false

    setMutatingKey(key)
    setCandidatesError("")
    setTopicsError("")
    setCandidateMessage("")
    setTopicMessage("")
    try {
      const created = await adminApi.createTopic({
        title: draft.title.trim(),
        description: draft.description.trim() || undefined,
        category: draft.category.trim(),
        sourceUrl: draft.sourceUrl.trim() || undefined,
      })
      const message = `토픽 #${created.topicId}이 승인되었습니다.`
      setCandidateMessage(message)
      setTopicMessage(message)
      await loadTopics()
      return true
    } catch (error) {
      setCandidatesError(messageOf(error, "토픽 승인에 실패했습니다."))
      return false
    } finally {
      setMutatingKey("")
    }
  }

  const createManualTopic = async (event: FormEvent) => {
    event.preventDefault()
    const created = await createTopic(manualDraft, "manual-topic")
    if (created) {
      setManualDraft(emptyDraft())
    }
  }

  const updateTopic = async (event: FormEvent) => {
    event.preventDefault()
    if (!editDraft) return

    setMutatingKey(`topic-update-${editDraft.topicId}`)
    setTopicsError("")
    setTopicMessage("")
    try {
      await adminApi.updateTopic(editDraft.topicId, {
        title: editDraft.title.trim(),
        description: editDraft.description.trim() || undefined,
        category: editDraft.category.trim(),
        sourceUrl: editDraft.sourceUrl.trim() || undefined,
      })
      setEditDraft(null)
      setTopicMessage("토픽을 수정했습니다.")
      await loadTopics()
    } catch (error) {
      setTopicsError(messageOf(error, "토픽 수정에 실패했습니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const deleteTopic = async (topicId: number) => {
    setMutatingKey(`topic-delete-${topicId}`)
    setTopicsError("")
    setTopicMessage("")
    try {
      await adminApi.deleteTopic(topicId)
      setTopicMessage("토픽을 삭제했습니다.")
      await loadTopics()
    } catch (error) {
      setTopicsError(messageOf(error, "토픽 삭제에 실패했습니다. 연결된 토론방이 있으면 먼저 토론방을 정리해야 합니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const previewRoomTitle = async (topic: TopicSummary) => {
    setMutatingKey(`room-preview-${topic.id}`)
    setTopicsError("")
    setTopicMessage("")
    try {
      const preview = await adminApi.previewRoomTitle(topic.id)
      setRoomDraft({
        topicId: topic.id,
        topicTitle: topic.title,
        title: preview.title,
        maxParticipants: "",
      })
      setTopicMessage("AI가 만든 토론방 제목을 확인한 뒤 수정하거나 최종 생성할 수 있습니다.")
    } catch (error) {
      setTopicsError(messageOf(error, "토론방 제목 미리보기에 실패했습니다. 이미 토론방이 있는 토픽일 수 있습니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const createRoom = async (event: FormEvent) => {
    event.preventDefault()
    if (!roomDraft || !roomDraft.title.trim()) return

    setMutatingKey(`room-create-${roomDraft.topicId}`)
    setTopicsError("")
    setTopicMessage("")
    try {
      const maxParticipants = roomDraft.maxParticipants.trim()
        ? Number(roomDraft.maxParticipants)
        : undefined
      const room = await adminApi.createRoom({
        topicId: roomDraft.topicId,
        title: roomDraft.title.trim(),
        maxParticipants,
      })
      setRoomDraft(null)
      setTopicMessage(`토론방 #${room.roomId}이 생성되었습니다.`)
      await loadRooms()
    } catch (error) {
      setTopicsError(messageOf(error, "토론방 생성에 실패했습니다. 제목이나 정원 값을 확인해주세요."))
    } finally {
      setMutatingKey("")
    }
  }

  const closeRoom = async (room: RoomSummary) => {
    if (room.status === "CLOSED") return
    const confirmed = window.confirm(`'${room.title}' 토론방을 강제 종료할까요?`)
    if (!confirmed) return

    setMutatingKey(`room-close-${room.roomId}`)
    setRoomsError("")
    setRoomMessage("")
    try {
      await adminApi.deleteRoom(room.roomId)
      setRoomMessage(`토론방 #${room.roomId}을 강제 종료했습니다.`)
      await loadRooms()
    } catch (error) {
      setRoomsError(messageOf(error, "토론방 강제 종료에 실패했습니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const startReportReview = async () => {
    if (!selectedReport || selectedReport.status !== "PENDING") return

    setMutatingKey(`report-start-${selectedReport.reportId}`)
    setReportDetailError("")
    setReportsMessage("")
    try {
      await adminApi.reviewReport(selectedReport.reportId, { action: "START_REVIEW" })
      setReportsMessage(`신고 #${selectedReport.reportId} 검토를 시작했습니다.`)
      await Promise.all([loadReports(), loadReportDetail(selectedReport.reportId)])
    } catch (error) {
      setReportDetailError(messageOf(error, "신고 검토 시작에 실패했습니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const completeReportReview = async (action: "RESOLVE" | "REJECT") => {
    if (!selectedReport || selectedReport.status !== "REVIEWING") return
    const note = resolutionNote.trim()
    if (!note) return

    setMutatingKey(`report-${action.toLowerCase()}-${selectedReport.reportId}`)
    setReportDetailError("")
    setReportsMessage("")
    try {
      await adminApi.reviewReport(selectedReport.reportId, {
        action,
        resolutionNote: note,
        severity: action === "RESOLVE" ? reviewSeverity : null,
      })
      setReportsMessage(
        action === "RESOLVE"
          ? `신고 #${selectedReport.reportId}을 처리했습니다.`
          : `신고 #${selectedReport.reportId}을 반려했습니다.`,
      )
      await Promise.all([loadReports(), loadReportDetail(selectedReport.reportId)])
    } catch (error) {
      setReportDetailError(messageOf(error, "신고 처리에 실패했습니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const startEdit = async (topic: TopicSummary) => {
    setTopicsError("")
    try {
      const detail = await topicApi.detail(topic.id)
      setEditDraft({
        topicId: detail.id,
        title: detail.title,
        description: detail.description ?? "",
        category: detail.category,
        sourceUrl: detail.sourceUrl ?? "",
      })
    } catch (error) {
      setTopicsError(messageOf(error, "토픽 상세 정보를 불러오지 못했습니다."))
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center gap-2 text-sm text-muted-foreground">
        <Loader2 className="size-4 animate-spin" />
        인증 상태를 확인하는 중입니다.
      </div>
    )
  }

  if (!isAdmin) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-background px-4 text-center">
        <Shield className="size-8 text-destructive" />
        <p className="font-semibold text-foreground">관리자만 접근할 수 있습니다.</p>
        <Link href="/">
          <Button>홈으로</Button>
        </Link>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-50 border-b border-border/50 bg-background/95 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-screen-xl items-center justify-between px-4 md:px-6">
          <Link href="/" className="flex items-center gap-2">
            <div className="flex size-8 items-center justify-center rounded-lg bg-primary">
              <Zap className="size-4 text-primary-foreground" />
            </div>
            <span className="font-bold text-foreground">시시비비</span>
            <Badge className="border-accent/30 bg-accent/20 text-[10px] text-accent">
              관리자
            </Badge>
          </Link>
          <Link href="/rooms">
            <Button variant="outline" size="sm" className="text-xs">
              토론방 보기
            </Button>
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-screen-xl px-4 py-6 md:px-6">
        <div className="mb-6">
          <h1 className="text-xl font-bold text-foreground">관리자 대시보드</h1>
          <p className="text-sm text-muted-foreground">
            이슈 후보를 승인된 토픽으로 등록하고, 토론방 생성과 강제 종료를 관리합니다.
          </p>
        </div>

        <Tabs defaultValue="candidates" className="gap-5">
          <TabsList className="w-full justify-start sm:w-fit">
            <TabsTrigger value="candidates">이슈 후보</TabsTrigger>
            <TabsTrigger value="topics">승인된 토픽</TabsTrigger>
            <TabsTrigger value="reports">신고 관리</TabsTrigger>
            <TabsTrigger value="rooms">토론방 관리</TabsTrigger>
          </TabsList>

          <TabsContent value="candidates" className="space-y-5">
            <Card>
              <CardHeader>
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2 text-sm">
                      <Sparkles className="size-4 text-primary" />
                      이슈 후보
                    </CardTitle>
                    <CardDescription>
                      3시간마다 자동 갱신되는 후보를 보거나, 즉시 새로 가져올 수 있습니다.
                    </CardDescription>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Badge variant="outline">{candidateCountLabel}</Badge>
                    <Button variant="outline" size="sm" className="gap-1.5 text-xs" onClick={loadCandidates} disabled={candidatesLoading}>
                      <RefreshCw className="size-3.5" />
                      캐시 조회
                    </Button>
                    <Button size="sm" className="gap-1.5 text-xs" onClick={refreshCandidates} disabled={candidatesLoading}>
                      {candidatesLoading ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
                      지금 새로고침
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                {candidateMessage && <StatusMessage tone="success" message={candidateMessage} />}
                {candidatesError && <StatusMessage tone="error" message={candidatesError} />}
                {candidatesLoading ? (
                  <LoadingBox message="토픽 후보를 불러오는 중입니다." />
                ) : candidates.length === 0 ? (
                  <EmptyBox message="표시할 토픽 후보가 없습니다. 지금 새로고침을 눌러 후보를 가져와 보세요." />
                ) : (
                  <div className="grid gap-4 lg:grid-cols-2">
                    {candidates.map((candidate, index) => {
                      const candidateDraft = draftFromCandidate(candidate)
                      const key = `${candidate.keyword}-${index}`
                      return (
                        <Card key={key} className="border-border/60">
                          <CardHeader className="pb-3">
                            <div className="flex items-start justify-between gap-3">
                              <div className="min-w-0">
                                <CardTitle className="line-clamp-2 text-sm">{candidate.keyword}</CardTitle>
                                <CardDescription className="mt-1 flex flex-wrap gap-2 text-xs">
                                  <span>검색량 {candidate.searchVolume?.toLocaleString() ?? "-"}</span>
                                  <span>증가율 {candidate.increasePercentage ?? "-"}%</span>
                                </CardDescription>
                              </div>
                              <Badge variant="outline" className="shrink-0">
                                {candidateDraft.category}
                              </Badge>
                            </div>
                          </CardHeader>
                          <CardContent className="space-y-4">
                            <div className="space-y-2">
                              {candidate.news.slice(0, 3).map((item, newsIndex) => {
                                const newsDraft = draftFromNews(item)
                                const newsKey = `news-${key}-${newsIndex}`

                                return (
                                  <div
                                    key={`${key}-news-${newsIndex}`}
                                    className="rounded-lg border border-border/50 p-3 transition-colors hover:border-primary/40 hover:bg-muted/40"
                                  >
                                    <div className="mb-1 flex items-center justify-between gap-2">
                                      <Badge variant="outline" className="text-[10px]">
                                        {item.category}
                                      </Badge>
                                      {(item.news.originallink || item.news.link) && (
                                        <a
                                          href={item.news.originallink || item.news.link}
                                          target="_blank"
                                          rel="noreferrer"
                                          className="inline-flex items-center gap-1 text-[11px] text-muted-foreground hover:text-primary"
                                        >
                                          원문
                                          <ExternalLink className="size-3" />
                                        </a>
                                      )}
                                    </div>
                                    <p className="line-clamp-2 text-xs font-medium text-foreground">{item.news.title}</p>
                                    <p className="mt-1 line-clamp-2 text-[11px] leading-relaxed text-muted-foreground">
                                      {item.news.description}
                                    </p>
                                    <Button
                                      className="mt-3 w-full gap-1.5 text-xs"
                                      size="sm"
                                      disabled={mutatingKey === newsKey}
                                      onClick={() => createTopic(newsDraft, newsKey)}
                                    >
                                      {mutatingKey === newsKey ? <Loader2 className="size-3.5 animate-spin" /> : <CheckCircle2 className="size-3.5" />}
                                      이 뉴스로 토픽 승인
                                    </Button>
                                  </div>
                                )
                              })}
                            </div>
                            <div className="flex flex-wrap gap-1">
                              {Array.from(new Set(candidate.news.flatMap((item) => item.keywords))).slice(0, 8).map((keyword) => (
                                <span key={keyword} className="rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">
                                  #{keyword}
                                </span>
                              ))}
                            </div>
                          </CardContent>
                        </Card>
                      )
                    })}
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-sm">직접 토픽 등록</CardTitle>
                <CardDescription>후보에 없는 주제를 바로 승인된 토픽으로 등록할 수 있습니다.</CardDescription>
              </CardHeader>
              <CardContent>
                <TopicForm
                  draft={manualDraft}
                  onChange={setManualDraft}
                  onSubmit={createManualTopic}
                  submitting={mutatingKey === "manual-topic"}
                  submitLabel="토픽 등록"
                />
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="topics" className="space-y-5">
            <Card>
              <CardHeader>
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2 text-sm">
                      <FileText className="size-4 text-primary" />
                      승인된 토픽
                    </CardTitle>
                    <CardDescription>
                      승인된 토픽에서 AI 제목을 먼저 확인하고, 수정한 뒤 최종 토론방을 생성합니다.
                    </CardDescription>
                  </div>
                  <Button variant="outline" size="sm" className="gap-1.5 text-xs" onClick={loadTopics} disabled={topicsLoading}>
                    <RefreshCw className="size-3.5" />
                    새로고침
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                {topicMessage && <StatusMessage tone="success" message={topicMessage} />}
                {topicsError && <StatusMessage tone="error" message={topicsError} />}
                {topicsLoading ? (
                  <LoadingBox message="승인된 토픽을 불러오는 중입니다." />
                ) : topics.length === 0 ? (
                  <EmptyBox message="등록된 승인 토픽이 없습니다." />
                ) : (
                  <div className="grid gap-3">
                    {topics.map((topic) => (
                      <div key={topic.id} className="rounded-lg border border-border/60 bg-card p-4">
                        <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                          <div className="min-w-0">
                            <div className="mb-2 flex flex-wrap items-center gap-2">
                              <Badge variant="outline">{topic.category}</Badge>
                              <span className="text-[11px] text-muted-foreground">#{topic.id}</span>
                            </div>
                            <p className="line-clamp-2 text-sm font-semibold text-foreground">{topic.title}</p>
                            {topic.sourceUrl && (
                              <a
                                href={topic.sourceUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="mt-1 inline-flex items-center gap-1 text-xs text-muted-foreground hover:text-primary"
                              >
                                출처 보기
                                <ExternalLink className="size-3" />
                              </a>
                            )}
                          </div>
                          <div className="flex shrink-0 flex-wrap gap-2">
                            <Button variant="outline" size="sm" className="gap-1.5 text-xs" onClick={() => startEdit(topic)}>
                              <Pencil className="size-3.5" />
                              수정
                            </Button>
                            <Button
                              size="sm"
                              className="gap-1.5 text-xs"
                              disabled={mutatingKey === `room-preview-${topic.id}`}
                              onClick={() => previewRoomTitle(topic)}
                            >
                              {mutatingKey === `room-preview-${topic.id}` ? <Loader2 className="size-3.5 animate-spin" /> : <Sparkles className="size-3.5" />}
                              AI 제목 확인
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="gap-1.5 text-xs text-destructive hover:text-destructive"
                              disabled={mutatingKey === `topic-delete-${topic.id}`}
                              onClick={() => deleteTopic(topic.id)}
                            >
                              <Trash2 className="size-3.5" />
                              삭제
                            </Button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>

            {roomDraft && (
              <Card className="border-primary/30">
                <CardHeader>
                  <CardTitle className="text-sm">토론방 최종 생성</CardTitle>
                  <CardDescription>
                    AI가 만든 제목을 확인하고 필요하면 수정한 뒤 토론방을 생성합니다.
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <RoomForm
                    draft={roomDraft}
                    onChange={setRoomDraft}
                    onSubmit={createRoom}
                    submitting={mutatingKey === `room-create-${roomDraft.topicId}`}
                    secondaryAction={
                      <Button type="button" variant="outline" onClick={() => setRoomDraft(null)}>
                        취소
                      </Button>
                    }
                  />
                </CardContent>
              </Card>
            )}

            {editDraft && (
              <Card className="border-primary/30">
                <CardHeader>
                  <CardTitle className="text-sm">토픽 수정</CardTitle>
                  <CardDescription>선택한 승인 토픽의 표시 정보를 수정합니다.</CardDescription>
                </CardHeader>
                <CardContent>
                  <TopicForm
                    draft={editDraft}
                    onChange={(next) => setEditDraft({ ...next, topicId: editDraft.topicId })}
                    onSubmit={updateTopic}
                    submitting={mutatingKey === `topic-update-${editDraft.topicId}`}
                    submitLabel="수정 저장"
                    secondaryAction={
                      <Button type="button" variant="outline" onClick={() => setEditDraft(null)}>
                        취소
                      </Button>
                    }
                  />
                </CardContent>
              </Card>
            )}
          </TabsContent>

          <TabsContent value="rooms" className="space-y-5">
            <Card>
              <CardHeader>
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2 text-sm">
                      <Power className="size-4 text-primary" />
                      토론방 관리
                    </CardTitle>
                    <CardDescription>
                      진행 중인 토론방을 관리자 권한으로 강제 종료할 수 있습니다.
                    </CardDescription>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Badge variant="outline">진행 중 {openRoomCount.toLocaleString()}개</Badge>
                    <Button variant="outline" size="sm" className="gap-1.5 text-xs" onClick={loadRooms} disabled={roomsLoading}>
                      <RefreshCw className="size-3.5" />
                      새로고침
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                {roomMessage && <StatusMessage tone="success" message={roomMessage} />}
                {roomsError && <StatusMessage tone="error" message={roomsError} />}
                {roomsLoading ? (
                  <LoadingBox message="토론방 목록을 불러오는 중입니다." />
                ) : rooms.length === 0 ? (
                  <EmptyBox message="생성된 토론방이 없습니다." />
                ) : (
                  <div className="grid gap-3">
                    {rooms.map((room) => (
                      <div key={room.roomId} className="rounded-lg border border-border/60 bg-card p-4">
                        <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                          <div className="min-w-0">
                            <div className="mb-2 flex flex-wrap items-center gap-2">
                              <Badge
                                variant={room.status === "OPEN" ? "default" : "outline"}
                                className="text-[10px]"
                              >
                                {room.status === "OPEN" ? "진행 중" : "종료"}
                              </Badge>
                              <span className="text-[11px] text-muted-foreground">방 #{room.roomId}</span>
                              <span className="text-[11px] text-muted-foreground">토픽 #{room.topicId}</span>
                            </div>
                            <p className="line-clamp-2 text-sm font-semibold text-foreground">{room.title}</p>
                            <p className="mt-1 text-[11px] text-muted-foreground">
                              시작: {room.startedAt ? new Date(room.startedAt).toLocaleString("ko-KR") : "-"}
                            </p>
                          </div>
                          <div className="flex shrink-0 flex-wrap gap-2">
                            <Link href={`/rooms/${room.roomId}`}>
                              <Button variant="outline" size="sm" className="gap-1.5 text-xs">
                                보기
                              </Button>
                            </Link>
                            <Button
                              variant={room.status === "OPEN" ? "destructive" : "outline"}
                              size="sm"
                              className="gap-1.5 text-xs"
                              disabled={room.status === "CLOSED" || mutatingKey === `room-close-${room.roomId}`}
                              onClick={() => closeRoom(room)}
                            >
                              {mutatingKey === `room-close-${room.roomId}` ? <Loader2 className="size-3.5 animate-spin" /> : <Power className="size-3.5" />}
                              강제 종료
                            </Button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="reports" className="space-y-5">
            <Card>
              <CardHeader>
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2 text-sm">
                      <AlertTriangle className="size-4 text-primary" />
                      신고 관리
                    </CardTitle>
                    <CardDescription>
                      AI 검토 결과는 관리자 판단을 돕는 보조 정보이며, 최종 처리는 관리자가 결정합니다.
                    </CardDescription>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {REPORT_STATUS_FILTERS.map((filter) => (
                      <Button
                        key={filter.value}
                        type="button"
                        variant={reportStatusFilter === filter.value ? "default" : "outline"}
                        size="sm"
                        className="text-xs"
                        onClick={() => setReportStatusFilter(filter.value)}
                      >
                        {filter.label}
                      </Button>
                    ))}
                    <Button variant="outline" size="sm" className="gap-1.5 text-xs" onClick={loadReports} disabled={reportsLoading}>
                      <RefreshCw className="size-3.5" />
                      새로고침
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                {reportsMessage && <StatusMessage tone="success" message={reportsMessage} />}
                {reportsError && <StatusMessage tone="error" message={reportsError} />}
                {reportsLoading ? (
                  <LoadingBox message="신고 목록을 불러오는 중입니다." />
                ) : reports.length === 0 ? (
                  <EmptyBox message="표시할 신고가 없습니다." />
                ) : (
                  <div className="grid gap-3 lg:grid-cols-2">
                    {reports.map((report) => {
                      const confidence = formatPercent(report.offTopicAiReview?.confidence ?? null)
                      const selected = selectedReportId === report.reportId

                      return (
                        <button
                          key={report.reportId}
                          type="button"
                          className={
                            selected
                              ? "rounded-lg border border-primary/60 bg-primary/5 p-4 text-left transition-colors"
                              : "rounded-lg border border-border/60 bg-card p-4 text-left transition-colors hover:border-primary/40 hover:bg-muted/30"
                          }
                          onClick={() => loadReportDetail(report.reportId)}
                        >
                          <div className="flex flex-wrap items-center gap-2">
                            <Badge variant="outline">{REPORT_REASON_LABELS[report.reason] ?? report.reason}</Badge>
                            <Badge variant="secondary">{REPORT_STATUS_LABELS[report.status]}</Badge>
                            <span className="text-[11px] text-muted-foreground">신고 #{report.reportId}</span>
                          </div>
                          <p className="mt-2 text-sm font-semibold text-foreground">의견 #{report.speechId}</p>
                          <p className="mt-1 text-xs text-muted-foreground">
                            신고자 #{report.reporterUserId} · 대상 사용자 #{report.reportedUserId} · {new Date(report.createdAt).toLocaleString("ko-KR")}
                          </p>
                          <div className="mt-3 rounded-lg bg-muted/40 px-3 py-2">
                            <p className="text-xs font-medium text-foreground">
                              {aiReviewLabel(report.offTopicAiReview)}
                              {confidence ? ` · 신뢰도 ${confidence}` : ""}
                            </p>
                            {report.offTopicAiReview && (
                              <p className="mt-1 text-[11px] text-muted-foreground">
                                신고 {report.offTopicAiReview.reportCount} / 기준 {report.offTopicAiReview.threshold} · 현재 참여자 {report.offTopicAiReview.participantCount}명
                              </p>
                            )}
                          </div>
                        </button>
                      )
                    })}
                  </div>
                )}
              </CardContent>
            </Card>

            <Card className="border-border/70">
              <CardHeader>
                <CardTitle className="text-sm">신고 상세</CardTitle>
                <CardDescription>신고 내용과 AI 검토 보조 근거를 함께 확인합니다.</CardDescription>
              </CardHeader>
              <CardContent>
                {reportDetailError && <StatusMessage tone="error" message={reportDetailError} />}
                {reportDetailLoading ? (
                  <LoadingBox message="신고 상세를 불러오는 중입니다." />
                ) : !selectedReport ? (
                  <EmptyBox message="목록에서 신고를 선택하면 상세 정보가 표시됩니다." />
                ) : (
                  <div className="space-y-4">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant="outline">{REPORT_REASON_LABELS[selectedReport.reason] ?? selectedReport.reason}</Badge>
                      <Badge variant="secondary">{REPORT_STATUS_LABELS[selectedReport.status]}</Badge>
                      <span className="text-xs text-muted-foreground">신고 #{selectedReport.reportId}</span>
                    </div>

                    <div className="grid gap-3 md:grid-cols-3">
                      <InfoBox label="의견" value={`#${selectedReport.speechId}`} />
                      <InfoBox label="신고자" value={`#${selectedReport.reporterUserId}`} />
                      <InfoBox label="대상 사용자" value={`#${selectedReport.reportedUserId}`} />
                    </div>

                    <div className="rounded-lg border border-border/60 bg-muted/20 p-3">
                      <p className="text-[11px] font-medium text-muted-foreground">신고 당시 의견 내용</p>
                      <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-foreground">{selectedReport.contentSnapshot}</p>
                    </div>

                    {selectedReport.description && (
                      <div className="rounded-lg border border-border/60 p-3">
                        <p className="text-[11px] font-medium text-muted-foreground">신고 설명</p>
                        <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-foreground">{selectedReport.description}</p>
                      </div>
                    )}

                    <AiReviewBox review={selectedReport.offTopicAiReview} />

                    {selectedReport.resolutionNote && (
                      <div className="rounded-lg border border-border/60 p-3">
                        <p className="text-[11px] font-medium text-muted-foreground">관리자 처리 메모</p>
                        <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-foreground">{selectedReport.resolutionNote}</p>
                      </div>
                    )}

                    {selectedReport.status === "PENDING" ? (
                      <div className="flex justify-end">
                        <Button
                          size="sm"
                          className="text-xs"
                          disabled={mutatingKey === `report-start-${selectedReport.reportId}`}
                          onClick={startReportReview}
                        >
                          {mutatingKey === `report-start-${selectedReport.reportId}` && <Loader2 className="mr-2 size-4 animate-spin" />}
                          검토 시작
                        </Button>
                      </div>
                    ) : selectedReport.status === "REVIEWING" ? (
                      <div className="space-y-3 rounded-lg border border-border/60 p-3">
                        <div>
                          <p className="mb-2 text-xs font-medium text-foreground">심각도</p>
                          <div className="flex flex-wrap gap-2">
                            {SEVERITY_OPTIONS.map((option) => (
                              <Button
                                key={option.value}
                                type="button"
                                variant={reviewSeverity === option.value ? "default" : "outline"}
                                size="sm"
                                className="text-xs"
                                onClick={() => setReviewSeverity(option.value)}
                              >
                                {option.label}
                              </Button>
                            ))}
                          </div>
                        </div>
                        <textarea
                          value={resolutionNote}
                          onChange={(event) => setResolutionNote(event.target.value)}
                          placeholder="관리자 처리 사유를 입력하세요."
                          rows={4}
                          maxLength={500}
                          className="w-full resize-none rounded-lg border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
                        />
                        <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
                          <Button
                            type="button"
                            variant="outline"
                            disabled={!resolutionNote.trim() || mutatingKey === `report-reject-${selectedReport.reportId}`}
                            onClick={() => completeReportReview("REJECT")}
                          >
                            {mutatingKey === `report-reject-${selectedReport.reportId}` && <Loader2 className="mr-2 size-4 animate-spin" />}
                            반려
                          </Button>
                          <Button
                            type="button"
                            variant="destructive"
                            disabled={!resolutionNote.trim() || mutatingKey === `report-resolve-${selectedReport.reportId}`}
                            onClick={() => completeReportReview("RESOLVE")}
                          >
                            {mutatingKey === `report-resolve-${selectedReport.reportId}` && <Loader2 className="mr-2 size-4 animate-spin" />}
                            처리
                          </Button>
                        </div>
                      </div>
                    ) : null}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </main>
    </div>
  )
}

function TopicForm({
  draft,
  onChange,
  onSubmit,
  submitting,
  submitLabel,
  secondaryAction,
}: {
  draft: TopicDraft
  onChange: (draft: TopicDraft) => void
  onSubmit: (event: FormEvent) => void
  submitting: boolean
  submitLabel: string
  secondaryAction?: ReactNode
}) {
  return (
    <form className="grid gap-3 md:grid-cols-2" onSubmit={onSubmit}>
      <Input
        value={draft.title}
        onChange={(event) => onChange({ ...draft, title: event.target.value })}
        placeholder="토픽 제목"
        required
      />
      <Input
        value={draft.category}
        onChange={(event) => onChange({ ...draft, category: event.target.value })}
        placeholder="카테고리"
        required
      />
      <Input
        value={draft.sourceUrl}
        onChange={(event) => onChange({ ...draft, sourceUrl: event.target.value })}
        placeholder="출처 URL"
        className="md:col-span-2"
      />
      <textarea
        value={draft.description}
        onChange={(event) => onChange({ ...draft, description: event.target.value })}
        placeholder="토픽 설명"
        rows={4}
        className="resize-none rounded-lg border border-input bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 md:col-span-2"
      />
      <div className="flex flex-col gap-2 md:col-span-2 sm:flex-row sm:justify-end">
        {secondaryAction}
        <Button type="submit" disabled={submitting || !draft.title.trim() || !draft.category.trim()}>
          {submitting && <Loader2 className="mr-2 size-4 animate-spin" />}
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}

function RoomForm({
  draft,
  onChange,
  onSubmit,
  submitting,
  secondaryAction,
}: {
  draft: RoomDraft
  onChange: (draft: RoomDraft) => void
  onSubmit: (event: FormEvent) => void
  submitting: boolean
  secondaryAction?: ReactNode
}) {
  return (
    <form className="grid gap-3" onSubmit={onSubmit}>
      <div className="rounded-lg border border-border/60 bg-muted/30 px-3 py-2">
        <p className="text-[11px] text-muted-foreground">원본 토픽</p>
        <p className="mt-1 line-clamp-2 text-sm font-medium text-foreground">{draft.topicTitle}</p>
      </div>
      <Input
        value={draft.title}
        onChange={(event) => onChange({ ...draft, title: event.target.value })}
        placeholder="토론방 제목"
        maxLength={100}
        required
      />
      <Input
        value={draft.maxParticipants}
        onChange={(event) => onChange({ ...draft, maxParticipants: event.target.value })}
        placeholder="최대 참여자 수, 비워두면 기본값"
        type="number"
        min={1}
      />
      <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
        {secondaryAction}
        <Button type="submit" disabled={submitting || !draft.title.trim()}>
          {submitting && <Loader2 className="mr-2 size-4 animate-spin" />}
          최종 토론방 생성
        </Button>
      </div>
    </form>
  )
}

function InfoBox({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border/60 bg-muted/20 px-3 py-2">
      <p className="text-[11px] text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm font-medium text-foreground">{value}</p>
    </div>
  )
}

function AiReviewBox({ review }: { review: OffTopicAiReview | null | undefined }) {
  const confidence = formatPercent(review?.confidence ?? null)

  return (
    <div className="rounded-lg border border-border/60 bg-muted/20 p-3">
      <div className="flex flex-wrap items-center gap-2">
        <Badge variant={review?.status === "COMPLETED" ? "secondary" : "outline"} className="text-[10px]">
          AI 검토 보조 결과
        </Badge>
        <p className="text-xs font-medium text-foreground">
          {aiReviewLabel(review)}
          {confidence ? ` · 신뢰도 ${confidence}` : ""}
        </p>
      </div>
      {review ? (
        <>
          <p className="mt-2 text-[11px] text-muted-foreground">
            신고 {review.reportCount} / 기준 {review.threshold} · 현재 참여자 {review.participantCount}명
            {review.completedAt ? ` · 완료 ${new Date(review.completedAt).toLocaleString("ko-KR")}` : ""}
          </p>
          {review.reason && (
            <p className="mt-3 whitespace-pre-wrap text-sm leading-relaxed text-foreground">{review.reason}</p>
          )}
        </>
      ) : (
        <p className="mt-2 text-xs text-muted-foreground">이 신고에는 AI 검토 보조 결과가 없습니다.</p>
      )}
    </div>
  )
}

function StatusMessage({ tone, message }: { tone: "success" | "error"; message: string }) {
  return (
    <p
      className={
        tone === "success"
          ? "mb-3 rounded-lg bg-emerald-500/10 px-3 py-2 text-xs text-emerald-700 dark:text-emerald-300"
          : "mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive"
      }
    >
      {message}
    </p>
  )
}

function LoadingBox({ message }: { message: string }) {
  return (
    <div className="flex items-center justify-center gap-2 rounded-lg border border-border px-4 py-10 text-sm text-muted-foreground">
      <Loader2 className="size-4 animate-spin" />
      {message}
    </div>
  )
}

function EmptyBox({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-border px-4 py-10 text-center text-sm text-muted-foreground">
      {message}
    </div>
  )
}
