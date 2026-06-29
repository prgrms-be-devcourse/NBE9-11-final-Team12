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
import { adminApi, roomApi, topicApi } from "@/lib/api/services"
import type {
  ChatReportDetail,
  ClassifiedIssueCandidate,
  ClassifiedIssueNews,
  OffTopicAiReview,
  RoomSummary,
  TopicSummary,
} from "@/lib/api/types"
import type {
  AdminUser,
  AdminUserRole,
  AdminUserStatus,
  SpeechReportDetail,
  SpeechReportReason,
  SpeechReportStatus,
  UserSanction,
  UserSanctionRecommendation,
  UserSanctionType,
  ViolationSeverity,
} from "@/lib/api/types"
import {
  AlertTriangle,
  CheckCircle2,
  ExternalLink,
  FileText,
  Gavel,
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

type ReportKind = "speech" | "chat"
type AdminReportDetail = SpeechReportDetail | ChatReportDetail

const REPORT_REASONS: { value: SpeechReportReason | ""; label: string }[] = [
  { value: "", label: "전체 사유" },
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

const REPORT_STATUSES: { value: SpeechReportStatus | ""; label: string }[] = [
  { value: "", label: "전체 상태" },
  { value: "PENDING", label: "검토 대기" },
  { value: "REVIEWING", label: "검토 중" },
  { value: "RESOLVED", label: "처리 완료" },
  { value: "REJECTED", label: "반려" },
]

const SEVERITIES: { value: ViolationSeverity; label: string }[] = [
  { value: "LOW", label: "낮음" },
  { value: "MEDIUM", label: "보통" },
  { value: "HIGH", label: "높음" },
  { value: "CRITICAL", label: "심각" },
]

const MAX_RESTRICTION_HOURS = 720

const SANCTION_TYPES: {
  value: UserSanctionType
  label: string
  requiresDuration: boolean
  maxDurationHours?: number
}[] = [
  { value: "WARNING", label: "경고", requiresDuration: false },
  { value: "CHAT_RESTRICTION", label: "채팅 제한", requiresDuration: true, maxDurationHours: MAX_RESTRICTION_HOURS },
  { value: "STAGE_RESTRICTION", label: "발언/의견 작성 제한", requiresDuration: true, maxDurationHours: MAX_RESTRICTION_HOURS },
  { value: "ACCOUNT_SUSPENSION", label: "계정 정지", requiresDuration: false },
]

const EMPTY_REPORT_STATUS_COUNTS: Record<SpeechReportStatus | "ALL", number> = {
  ALL: 0,
  PENDING: 0,
  REVIEWING: 0,
  RESOLVED: 0,
  REJECTED: 0,
}

const USER_STATUSES: { value: AdminUserStatus | ""; label: string }[] = [
  { value: "", label: "전체 상태" },
  { value: "ACTIVE", label: "활성" },
  { value: "INACTIVE", label: "비활성" },
  { value: "BANNED", label: "계정 정지" },
]

const USER_ROLES: { value: AdminUserRole | ""; label: string }[] = [
  { value: "", label: "전체 권한" },
  { value: "USER", label: "일반 사용자" },
  { value: "ADMIN", label: "관리자" },
]

function reportStatusLabel(status: SpeechReportStatus) {
  return REPORT_STATUSES.find((item) => item.value === status)?.label ?? status
}

function reportReasonLabel(reason: SpeechReportReason) {
  return REPORT_REASONS.find((item) => item.value === reason)?.label ?? reason
}

function severityLabel(severity: ViolationSeverity | null) {
  if (!severity) return "-"
  return SEVERITIES.find((item) => item.value === severity)?.label ?? severity
}

function sanctionTypeLabel(type: UserSanctionType) {
  if (type === "SPEECH_RESTRICTION" || type === "STAGE_RESTRICTION") {
    return "발언/의견 작성 제한"
  }
  return SANCTION_TYPES.find((item) => item.value === type)?.label ?? type
}

function sanctionStateLabel(state: UserSanction["state"]) {
  switch (state) {
    case "ACTIVE":
      return "적용 중"
    case "EXPIRED":
      return "만료"
    case "REVOKED":
      return "해제"
    default:
      return state
  }
}

function reportKindLabel(kind: ReportKind) {
  return kind === "speech" ? "의견 신고" : "채팅 신고"
}

function reportTargetLabel(report: AdminReportDetail) {
  if ("speechId" in report) {
    return `의견 #${report.speechId}`
  }
  return `채팅 #${report.messageId} · 토론방 #${report.roomId}`
}

function userLabel(userId: number | null, nickname?: string | null) {
  if (!userId) return "-"
  return nickname ? `${nickname} (#${userId})` : `사용자 #${userId}`
}

function relativeTimeLabel(value: string) {
  const elapsedMs = Date.now() - new Date(value).getTime()
  if (!Number.isFinite(elapsedMs) || elapsedMs < 0) return "방금 전"

  const minutes = Math.floor(elapsedMs / 60000)
  if (minutes < 1) return "방금 전"
  if (minutes < 60) return `${minutes}분 전`

  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}시간 전`

  return `${Math.floor(hours / 24)}일 전`
}

function reportQueueGuide(status: SpeechReportStatus) {
  switch (status) {
    case "PENDING":
      return "먼저 원문과 신고 사유를 확인한 뒤 검토를 시작합니다."
    case "REVIEWING":
      return "심각도와 처리 사유를 남기고 위반 확정 또는 반려로 종결합니다."
    case "RESOLVED":
      return "확정된 신고입니다. 필요하면 제재 이력 확인 후 제재를 적용합니다."
    case "REJECTED":
      return "정책 위반이 아니라고 판단되어 종결된 신고입니다."
    default:
      return "신고 상태를 확인합니다."
  }
}

function severityDescription(severity: ViolationSeverity) {
  switch (severity) {
    case "LOW":
      return "경미한 표현 문제나 단발성 위반으로 판단되는 경우에 사용합니다."
    case "MEDIUM":
      return "일반적인 욕설, 비방, 주제 방해처럼 기본 제재 검토가 필요한 경우에 사용합니다."
    case "HIGH":
      return "반복 위반, 명확한 혐오·성적 표현, 토론 방해 정도가 큰 경우에 사용합니다."
    case "CRITICAL":
      return "폭력 위협, 심각한 개인정보 노출, 계정 정지 검토가 필요한 수준에 사용합니다."
    default:
      return "신고 원문과 사용자 이력을 함께 보고 판단합니다."
  }
}

function sanctionPeriodLabel(sanction: UserSanction) {
  const startsAt = new Date(sanction.startsAt).toLocaleString("ko-KR")
  const endsAt = sanction.endsAt ? new Date(sanction.endsAt).toLocaleString("ko-KR") : "무기한"
  return `${startsAt} ~ ${endsAt}`
}

function userStatusLabel(status: AdminUserStatus) {
  return USER_STATUSES.find((item) => item.value === status)?.label ?? status
}

function userRoleLabel(role: AdminUserRole) {
  return USER_ROLES.find((item) => item.value === role)?.label ?? role
}

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
  if (review.confidence !== null && review.confidence < 0.3) {
    return "AI 검토 완료 · 논점 이탈 가능성 있음"
  }
  if (review.confidence !== null && review.confidence < 0.6) {
    return "AI 검토 완료 · 논점 이탈 의심"
  }
  return "AI 검토 완료 · 정상 범위"
}

function aiReviewTextClass(review: OffTopicAiReview | null | undefined) {
  return review?.status === "COMPLETED" && review.confidence !== null && review.confidence < 0.3
    ? "text-destructive"
    : "text-foreground"
}

function relationScoreLabel(value: number | null) {
  const percent = formatPercent(value)
  return percent ? `연관성 점수 ${percent}` : ""
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
  const [manualDraft, setManualDraft] = useState<TopicDraft>(emptyDraft)
  const [editDraft, setEditDraft] = useState<TopicEditDraft | null>(null)
  const [roomDraft, setRoomDraft] = useState<RoomDraft | null>(null)
  const [rooms, setRooms] = useState<RoomSummary[]>([])
  const [roomsLoading, setRoomsLoading] = useState(false)
  const [roomsError, setRoomsError] = useState("")
  const [roomMessage, setRoomMessage] = useState("")
  const [mutatingKey, setMutatingKey] = useState("")
  const [reportKind, setReportKind] = useState<ReportKind>("speech")
  const [reportStatusFilter, setReportStatusFilter] = useState<SpeechReportStatus | "">("")
  const [reportReasonFilter, setReportReasonFilter] = useState<SpeechReportReason | "">("")
  const [reports, setReports] = useState<AdminReportDetail[]>([])
  const [reportStatusCounts, setReportStatusCounts] = useState<Record<SpeechReportStatus | "ALL", number>>(EMPTY_REPORT_STATUS_COUNTS)
  const [selectedReport, setSelectedReport] = useState<AdminReportDetail | null>(null)
  const [reportsLoading, setReportsLoading] = useState(false)
  const [reportsError, setReportsError] = useState("")
  const [reportsMessage, setReportsMessage] = useState("")
  const [resolutionNote, setResolutionNote] = useState("")
  const [severity, setSeverity] = useState<ViolationSeverity>("MEDIUM")
  const [selectedReportTargetUser, setSelectedReportTargetUser] = useState<AdminUser | null>(null)
  const [recommendation, setRecommendation] = useState<UserSanctionRecommendation | null>(null)
  const [sanctions, setSanctions] = useState<UserSanction[]>([])
  const [manualSanctionType, setManualSanctionType] = useState<UserSanctionType>("WARNING")
  const [manualSanctionDurationHours, setManualSanctionDurationHours] = useState("")
  const [sanctionReason, setSanctionReason] = useState("")
  const [adminUsers, setAdminUsers] = useState<AdminUser[]>([])
  const [userKeyword, setUserKeyword] = useState("")
  const [userStatusFilter, setUserStatusFilter] = useState<AdminUserStatus | "">("")
  const [userRoleFilter, setUserRoleFilter] = useState<AdminUserRole | "">("")
  const [selectedAdminUser, setSelectedAdminUser] = useState<AdminUser | null>(null)
  const [accountSanctions, setAccountSanctions] = useState<UserSanction[]>([])
  const [selectedRevokeSanction, setSelectedRevokeSanction] = useState<UserSanction | null>(null)
  const [revokeReason, setRevokeReason] = useState("")
  const [usersLoading, setUsersLoading] = useState(false)
  const [userDetailLoading, setUserDetailLoading] = useState(false)
  const [usersError, setUsersError] = useState("")
  const [usersMessage, setUsersMessage] = useState("")

  const isAdmin = user?.role === "ADMIN"
  const candidateCountLabel = useMemo(() => `${candidates.length.toLocaleString()}개`, [candidates.length])
  const openRoomCount = useMemo(() => rooms.filter((room) => room.status === "OPEN").length, [rooms])
  const selectedSanctionType = SANCTION_TYPES.find((type) => type.value === manualSanctionType)
  const blocksDuplicateRecommendedSanction =
    Boolean(recommendation?.activeSameTypeSanction && manualSanctionType === recommendation.recommendedType)
  const needsReviewCount = reportStatusCounts.PENDING + reportStatusCounts.REVIEWING
  const closedReportCount = reportStatusCounts.RESOLVED + reportStatusCounts.REJECTED

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

  const loadReportStatusCounts = async (
    kind: ReportKind = reportKind,
    reason: SpeechReportReason | "" = reportReasonFilter,
  ) => {
    const listApi = kind === "speech" ? adminApi.reports : adminApi.chatReports
    try {
      const [all, pending, reviewing, resolved, rejected] = await Promise.all([
        listApi({ reason, page: 0, size: 1 }),
        listApi({ status: "PENDING", reason, page: 0, size: 1 }),
        listApi({ status: "REVIEWING", reason, page: 0, size: 1 }),
        listApi({ status: "RESOLVED", reason, page: 0, size: 1 }),
        listApi({ status: "REJECTED", reason, page: 0, size: 1 }),
      ])
      setReportStatusCounts({
        ALL: all.totalElements,
        PENDING: pending.totalElements,
        REVIEWING: reviewing.totalElements,
        RESOLVED: resolved.totalElements,
        REJECTED: rejected.totalElements,
      })
    } catch {
      setReportStatusCounts(EMPTY_REPORT_STATUS_COUNTS)
    }
  }

  const loadReportTargetContext = async (
    report: AdminReportDetail,
    kind: ReportKind,
    includeRecommendation = false,
  ) => {
    setRecommendation(null)
    const [targetUser, sanctionPage] = await Promise.all([
      adminApi.userDetail(report.reportedUserId),
      adminApi.sanctions(report.reportedUserId),
    ])
    setSelectedReportTargetUser(targetUser)
    setSanctions(sanctionPage.content)

    if (!includeRecommendation) return

    if (targetUser.role === "ADMIN") {
      setReportsMessage("관리자 계정은 사용자 제재 대상이 아니므로 제재 적용을 차단했습니다.")
      return
    }

    if (kind === "speech") {
      const nextRecommendation = await adminApi.sanctionRecommendation(report.reportedUserId, report.reportId)
      setRecommendation(nextRecommendation)
      setManualSanctionType(nextRecommendation.recommendedType)
      setManualSanctionDurationHours(nextRecommendation.recommendedDurationHours?.toString() ?? "")
      setSanctionReason(nextRecommendation.recommendationReason)
      return
    }

    setManualSanctionType("CHAT_RESTRICTION")
    setManualSanctionDurationHours("24")
    setSanctionReason("채팅 신고 검토 결과 운영 정책 위반으로 판단되었습니다.")
  }

  const loadReports = async (filters: {
    kind?: ReportKind
    status?: SpeechReportStatus | ""
    reason?: SpeechReportReason | ""
  } = {}) => {
    const kind = filters.kind ?? reportKind
    const status = filters.status ?? reportStatusFilter
    const reason = filters.reason ?? reportReasonFilter
    const listApi = kind === "speech" ? adminApi.reports : adminApi.chatReports
    const detailApi = kind === "speech" ? adminApi.reportDetail : adminApi.chatReportDetail

    setReportsLoading(true)
    setReportsError("")
    setReportsMessage("")
    try {
      const response = await listApi({
        status,
        reason,
        page: 0,
        size: 20,
      })
      const details = await Promise.all(
        response.content.map((report) => detailApi(report.reportId)),
      )
      const shouldPreserveSelection = kind === reportKind
      const nextSelected = shouldPreserveSelection && selectedReport
        ? details.find((report) => report.reportId === selectedReport.reportId) ?? details[0] ?? null
        : details[0] ?? null
      setReports(details)
      setSelectedReport(nextSelected)
      setRecommendation(null)
      setSanctions([])
      setSelectedReportTargetUser(null)
      await loadReportStatusCounts(kind, reason)
      if (nextSelected) {
        await loadReportTargetContext(nextSelected, kind)
      }
    } catch (error) {
      setReportsError(messageOf(error, "신고 목록을 불러오지 못했습니다."))
    } finally {
      setReportsLoading(false)
    }
  }

  const selectReport = async (reportId: number) => {
    const detailApi = reportKind === "speech" ? adminApi.reportDetail : adminApi.chatReportDetail
    setMutatingKey(`report-detail-${reportId}`)
    setReportsError("")
    setReportsMessage("")
    try {
      const detail = await detailApi(reportId)
      setSelectedReport(detail)
      setRecommendation(null)
      setSanctions([])
      setSelectedReportTargetUser(null)
      setResolutionNote("")
      setManualSanctionType("WARNING")
      setManualSanctionDurationHours("")
      setSanctionReason("")
      await loadReportTargetContext(detail, reportKind)
    } catch (error) {
      setReportsError(messageOf(error, "신고 상세를 불러오지 못했습니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const reviewReport = async (action: "START_REVIEW" | "RESOLVE" | "REJECT") => {
    if (!selectedReport) return
    const reviewApi = reportKind === "speech" ? adminApi.reviewReport : adminApi.reviewChatReport
    setMutatingKey(`report-review-${action}`)
    setReportsError("")
    setReportsMessage("")
    try {
      await reviewApi(selectedReport.reportId, {
        action,
        resolutionNote: action === "START_REVIEW" ? undefined : resolutionNote.trim(),
        severity: action === "RESOLVE" ? severity : undefined,
      })
      setReportsMessage(`${reportKindLabel(reportKind)} 검토 상태를 변경했습니다.`)
      setResolutionNote("")
      await loadReports()
    } catch (error) {
      setReportsError(messageOf(error, "신고 처리에 실패했습니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const loadSanctionContext = async () => {
    if (!selectedReport) return
    setMutatingKey("sanction-context")
    setReportsError("")
    setReportsMessage("")
    try {
      await loadReportTargetContext(selectedReport, reportKind, true)
    } catch (error) {
      setReportsError(messageOf(error, "제재 정보를 불러오지 못했습니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const createManualSanction = async () => {
    if (!selectedReport) return
    if (selectedReportTargetUser?.role === "ADMIN") {
      setReportsError("관리자 계정은 사용자 제재 대상으로 지정할 수 없습니다.")
      return
    }
    if (blocksDuplicateRecommendedSanction) {
      setReportsError("동일 유형의 활성 제재가 이미 있습니다. 신규 등록 대신 기존 제재를 연장해주세요.")
      return
    }
    const durationHours = selectedSanctionType?.requiresDuration && manualSanctionDurationHours.trim()
      ? Number(manualSanctionDurationHours)
      : null
    if (selectedSanctionType?.requiresDuration && (!durationHours || durationHours < 1)) {
      setReportsError("제재 기간은 1시간 이상이어야 합니다.")
      return
    }
    if (
      selectedSanctionType?.requiresDuration
      && selectedSanctionType.maxDurationHours
      && durationHours
      && durationHours > selectedSanctionType.maxDurationHours
    ) {
      setReportsError(`제재 기간은 최대 ${selectedSanctionType.maxDurationHours}시간까지 입력할 수 있습니다.`)
      return
    }
    setMutatingKey("sanction-create")
    setReportsError("")
    setReportsMessage("")
    try {
      await adminApi.createSanction(selectedReport.reportedUserId, {
        type: manualSanctionType,
        reason: sanctionReason.trim(),
        durationHours,
        reportId: reportKind === "speech" ? selectedReport.reportId : null,
      })
      setReportsMessage("선택한 제재를 적용했습니다.")
      await loadReportTargetContext(selectedReport, reportKind)
    } catch (error) {
      setReportsError(messageOf(error, "제재 적용에 실패했습니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const extendRecommendedSanction = async () => {
    if (!selectedReport || !recommendation?.activeSameTypeSanctionId) return
    if (!recommendation.recommendedDurationHours) {
      setReportsError("기간이 없는 제재는 연장할 수 없습니다.")
      return
    }
    if (!sanctionReason.trim()) {
      setReportsError("제재 연장 사유를 입력해주세요.")
      return
    }

    setMutatingKey("sanction-extend")
    setReportsError("")
    setReportsMessage("")
    try {
      await adminApi.extendSanction(
        selectedReport.reportedUserId,
        recommendation.activeSameTypeSanctionId,
        recommendation.recommendedDurationHours,
        sanctionReason.trim(),
      )
      setReportsMessage("기존 활성 제재를 추천 기간 기준으로 연장했습니다.")
      await loadReportTargetContext(selectedReport, reportKind, true)
    } catch (error) {
      setReportsError(messageOf(error, "제재 연장에 실패했습니다."))
    } finally {
      setMutatingKey("")
    }
  }

  const loadAdminUsers = async (filters: {
    keyword?: string
    status?: AdminUserStatus | ""
    role?: AdminUserRole | ""
  } = {}) => {
    const keyword = filters.keyword ?? userKeyword
    const status = filters.status ?? userStatusFilter
    const role = filters.role ?? userRoleFilter

    setUsersLoading(true)
    setUsersError("")
    setUsersMessage("")
    try {
      const response = await adminApi.users({
        keyword,
        status,
        role,
        page: 0,
        size: 20,
      })
      setAdminUsers(response.content)
      setSelectedAdminUser(null)
      setAccountSanctions([])
      setSelectedRevokeSanction(null)
      setRevokeReason("")
    } catch (error) {
      setUsersError(messageOf(error, "사용자 목록을 불러오지 못했습니다."))
    } finally {
      setUsersLoading(false)
    }
  }

  const selectAdminUser = async (userId: number) => {
    setUserDetailLoading(true)
    setUsersError("")
    setUsersMessage("")
    try {
      const [userDetail, sanctionPage] = await Promise.all([
        adminApi.userDetail(userId),
        adminApi.sanctions(userId),
      ])
      setSelectedAdminUser(userDetail)
      setAccountSanctions(sanctionPage.content)
      setSelectedRevokeSanction(null)
      setRevokeReason("")
    } catch (error) {
      setUsersError(messageOf(error, "사용자 상세 정보를 불러오지 못했습니다."))
    } finally {
      setUserDetailLoading(false)
    }
  }

  const revokeSelectedSanction = async () => {
    if (!selectedAdminUser || !selectedRevokeSanction || !revokeReason.trim()) return

    setMutatingKey("user-sanction-revoke")
    setUsersError("")
    setUsersMessage("")
    try {
      await adminApi.revokeSanction(
        selectedAdminUser.userId,
        selectedRevokeSanction.sanctionId,
        revokeReason.trim(),
      )
      await loadAdminUsers()
      await selectAdminUser(selectedAdminUser.userId)
      setUsersMessage("선택한 사용자 제재를 해제했습니다.")
    } catch (error) {
      setUsersError(messageOf(error, "사용자 제재 해제에 실패했습니다."))
    } finally {
      setMutatingKey("")
    }
  }

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

  useEffect(() => {
    if (!loading && isAdmin) {
      void loadCandidates()
      void loadTopics()
      void loadRooms()
      void loadReports()
      void loadAdminUsers()
    }
  }, [loading, isAdmin])

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
            <span className="font-bold text-foreground">이슈톡</span>
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
            토픽 운영, 신고 검토, 사용자 제재를 관리합니다.
          </p>
        </div>

        <Tabs defaultValue="topic-management" className="gap-5">
          <TabsList className="w-full justify-start sm:w-fit">
            <TabsTrigger value="topic-management">토픽 관리</TabsTrigger>
            <TabsTrigger value="rooms">토론방 관리</TabsTrigger>
            <TabsTrigger value="reports">신고/제재 관리</TabsTrigger>
            <TabsTrigger value="users">사용자 관리</TabsTrigger>
          </TabsList>

          <TabsContent value="topic-management" className="space-y-5">
            <Tabs defaultValue="candidates" className="gap-5">
              <TabsList className="w-full justify-start sm:w-fit">
                <TabsTrigger value="candidates">이슈 후보</TabsTrigger>
                <TabsTrigger value="topics">승인된 토픽</TabsTrigger>
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
            </Tabs>
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
                      <AlertTriangle className="size-4 text-destructive" />
                      신고 검토
                    </CardTitle>
                    <CardDescription>
                      의견 신고와 채팅 신고를 분리해 확인하고, 위반 확정 후 필요한 제재를 적용합니다.
                    </CardDescription>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <select
                      value={reportStatusFilter}
                      onChange={(event) => {
                        const nextStatus = event.target.value as SpeechReportStatus | ""
                        setReportStatusFilter(nextStatus)
                        void loadReports({ status: nextStatus })
                      }}
                      className="h-9 rounded-md border bg-background px-2 text-xs"
                    >
                      {REPORT_STATUSES.map((status) => (
                        <option key={status.value || "all"} value={status.value}>{status.label}</option>
                      ))}
                    </select>
                    <select
                      value={reportReasonFilter}
                      onChange={(event) => {
                        const nextReason = event.target.value as SpeechReportReason | ""
                        setReportReasonFilter(nextReason)
                        void loadReports({ reason: nextReason })
                      }}
                      className="h-9 rounded-md border bg-background px-2 text-xs"
                    >
                      {REPORT_REASONS.map((reason) => (
                        <option key={reason.value || "all"} value={reason.value}>{reason.label}</option>
                      ))}
                    </select>
                    <Button variant="outline" size="sm" className="gap-1.5 text-xs" onClick={() => void loadReports()} disabled={reportsLoading}>
                      <RefreshCw className="size-3.5" />
                      조회
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                {reportsMessage && <StatusMessage tone="success" message={reportsMessage} />}
                {reportsError && <StatusMessage tone="error" message={reportsError} />}
                <div className="mb-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
                  <div className="rounded-lg border border-border/60 bg-muted/20 p-3">
                    <p className="text-[11px] text-muted-foreground">처리 필요</p>
                    <p className="mt-1 text-lg font-bold text-foreground">{needsReviewCount.toLocaleString()}</p>
                    <p className="mt-1 text-[11px] text-muted-foreground">검토 대기 + 검토 중</p>
                  </div>
                  <div className="rounded-lg border border-border/60 bg-muted/20 p-3">
                    <p className="text-[11px] text-muted-foreground">검토 대기</p>
                    <p className="mt-1 text-lg font-bold text-foreground">{reportStatusCounts.PENDING.toLocaleString()}</p>
                    <p className="mt-1 text-[11px] text-muted-foreground">아직 담당 검토 전</p>
                  </div>
                  <div className="rounded-lg border border-border/60 bg-muted/20 p-3">
                    <p className="text-[11px] text-muted-foreground">검토 중</p>
                    <p className="mt-1 text-lg font-bold text-foreground">{reportStatusCounts.REVIEWING.toLocaleString()}</p>
                    <p className="mt-1 text-[11px] text-muted-foreground">심각도·사유 판단 필요</p>
                  </div>
                  <div className="rounded-lg border border-border/60 bg-muted/20 p-3">
                    <p className="text-[11px] text-muted-foreground">종결</p>
                    <p className="mt-1 text-lg font-bold text-foreground">{closedReportCount.toLocaleString()}</p>
                    <p className="mt-1 text-[11px] text-muted-foreground">처리 완료 + 반려</p>
                  </div>
                </div>
                <div className="mb-4 flex flex-wrap gap-2">
                  {([
                    { value: "speech", label: "의견 신고" },
                    { value: "chat", label: "채팅 신고" },
                  ] as const).map((kind) => (
                    <Button
                      key={kind.value}
                      type="button"
                      variant={reportKind === kind.value ? "default" : "outline"}
                      size="sm"
                      className="h-8 text-xs"
                      onClick={() => {
                        setReportKind(kind.value)
                        setReportStatusFilter("")
                        setReportReasonFilter("")
                        setSelectedReport(null)
                        setRecommendation(null)
                        setSanctions([])
                        void loadReports({ kind: kind.value, status: "", reason: "" })
                      }}
                    >
                      {kind.label}
                    </Button>
                  ))}
                </div>
                <div className="mb-4 flex flex-wrap gap-2">
                  {REPORT_STATUSES.map((status) => {
                    const count = status.value ? reportStatusCounts[status.value] : reportStatusCounts.ALL
                    return (
                      <Button
                        key={status.value || "all"}
                        type="button"
                        variant={reportStatusFilter === status.value ? "default" : "outline"}
                        size="sm"
                        className="h-8 gap-1.5 text-xs"
                        onClick={() => {
                          setReportStatusFilter(status.value)
                          void loadReports({ status: status.value })
                        }}
                      >
                        {status.label}
                        <Badge variant="secondary" className="px-1.5 py-0 text-[10px]">{count}</Badge>
                      </Button>
                    )
                  })}
                </div>
                {reportsLoading ? (
                  <LoadingBox message="신고 목록을 불러오는 중입니다." />
                ) : reports.length === 0 ? (
                  <EmptyBox message="조회 조건에 해당하는 신고가 없습니다." />
                ) : (
                  <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(360px,0.9fr)]">
                    <div className="space-y-2">
                      {reports.map((report) => (
                        <button
                          key={report.reportId}
                          type="button"
                          onClick={() => selectReport(report.reportId)}
                          className={`w-full rounded-lg border p-3 text-left transition-colors ${selectedReport?.reportId === report.reportId ? "border-primary bg-primary/5" : "border-border/60 hover:bg-muted/40"}`}
                        >
                          <div className="mb-2 flex flex-wrap items-center gap-2">
                            <Badge variant="outline">#{report.reportId}</Badge>
                            <Badge variant={report.status === "PENDING" ? "destructive" : "secondary"}>{reportStatusLabel(report.status)}</Badge>
                            <Badge variant="outline">{reportReasonLabel(report.reason)}</Badge>
                          </div>
                          <p className="line-clamp-2 text-sm font-medium">{report.contentSnapshot}</p>
                          <div className="mt-2 grid gap-1 text-[11px] text-muted-foreground sm:grid-cols-2">
                            <span>{reportTargetLabel(report)}</span>
                            <span>신고 일시: {new Date(report.createdAt).toLocaleString("ko-KR")}</span>
                            <span>접수 경과: {relativeTimeLabel(report.createdAt)}</span>
                            <span>대상: {userLabel(report.reportedUserId, report.reportedUserNickname)}</span>
                            <span>신고자: {userLabel(report.reporterUserId, report.reporterUserNickname)}</span>
                          </div>
                          {reportKind === "speech" && "offTopicAiReview" in report && (
                            <div className="mt-2 rounded-lg bg-muted/40 px-3 py-2">
                              <p className={`text-xs font-medium ${aiReviewTextClass(report.offTopicAiReview)}`}>
                                {aiReviewLabel(report.offTopicAiReview)}
                                {relationScoreLabel(report.offTopicAiReview?.confidence ?? null)
                                  ? ` · ${relationScoreLabel(report.offTopicAiReview?.confidence ?? null)}`
                                  : ""}
                              </p>
                              {report.offTopicAiReview && (
                                <p className="mt-1 text-[11px] text-muted-foreground">
                                  신고 {report.offTopicAiReview.reportCount} / 기준 {report.offTopicAiReview.threshold} · 현재 참여자 {report.offTopicAiReview.participantCount}명
                                </p>
                              )}
                            </div>
                          )}
                        </button>
                      ))}
                    </div>

                    <div className="space-y-4">
                      {selectedReport ? (
                        <>
                          <Card className="border-border/60">
                            <CardHeader className="pb-3">
                              <CardTitle className="text-sm">{reportKindLabel(reportKind)} 상세 #{selectedReport.reportId}</CardTitle>
                              <CardDescription>
                                {reportTargetLabel(selectedReport)} · 대상 {userLabel(selectedReport.reportedUserId, selectedReport.reportedUserNickname)}
                              </CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-3">
                              <div className="rounded-lg border bg-muted/30 p-3 text-sm">
                                <p className="whitespace-pre-wrap">{selectedReport.contentSnapshot}</p>
                              </div>
                              {selectedReport.description && (
                                <p className="rounded-lg bg-muted/40 px-3 py-2 text-xs text-muted-foreground">
                                  상세 설명: {selectedReport.description}
                                </p>
                              )}
                              {reportKind === "speech" && "offTopicAiReview" in selectedReport && (
                                <AiReviewBox review={selectedReport.offTopicAiReview} />
                              )}
                              <div className="grid gap-2 text-xs text-muted-foreground sm:grid-cols-2">
                                <span>상태: {reportStatusLabel(selectedReport.status)}</span>
                                <span>신고 사유: {reportReasonLabel(selectedReport.reason)}</span>
                                <span>심각도: {severityLabel(selectedReport.severity)}</span>
                                <span>처리자: {userLabel(selectedReport.reviewedBy, selectedReport.reviewedByNickname)}</span>
                              </div>
                              <div className="grid gap-2 rounded-lg bg-muted/30 p-3 text-xs text-muted-foreground sm:grid-cols-2">
                                <span>신고 대상: {userLabel(selectedReport.reportedUserId, selectedReport.reportedUserNickname)}</span>
                                <span>신고자: {userLabel(selectedReport.reporterUserId, selectedReport.reporterUserNickname)}</span>
                              </div>
                              <div className="rounded-lg border border-primary/20 bg-primary/5 px-3 py-2 text-xs text-primary">
                                다음 작업: {reportQueueGuide(selectedReport.status)}
                              </div>
                              {selectedReport.status === "PENDING" && (
                                <Button
                                  className="w-full"
                                  disabled={mutatingKey === "report-review-START_REVIEW"}
                                  onClick={() => reviewReport("START_REVIEW")}
                                >
                                  {mutatingKey === "report-review-START_REVIEW" && <Loader2 className="mr-2 size-4 animate-spin" />}
                                  검토 시작
                                </Button>
                              )}
                              {selectedReport.status === "REVIEWING" && (
                                <div className="space-y-2">
                                  <select
                                    value={severity}
                                    onChange={(event) => setSeverity(event.target.value as ViolationSeverity)}
                                    className="h-9 w-full rounded-md border bg-background px-2 text-xs"
                                  >
                                    {SEVERITIES.map((item) => (
                                      <option key={item.value} value={item.value}>{item.label}</option>
                                    ))}
                                  </select>
                                  <p className="text-[11px] leading-relaxed text-muted-foreground">
                                    {severityDescription(severity)}
                                  </p>
                                  <p className="text-[11px] leading-relaxed text-muted-foreground">
                                    의견 신고는 확정 처리 후 제재 추천안 계산의 기준으로 사용됩니다.
                                  </p>
                                  <textarea
                                    value={resolutionNote}
                                    onChange={(event) => setResolutionNote(event.target.value)}
                                    maxLength={500}
                                    rows={3}
                                    placeholder="처리 또는 반려 사유"
                                    className="w-full resize-none rounded-lg border bg-background px-3 py-2 text-xs outline-none focus:border-primary"
                                  />
                                  <div className="grid gap-2 sm:grid-cols-2">
                                    <Button
                                      disabled={!resolutionNote.trim() || mutatingKey === "report-review-RESOLVE"}
                                      onClick={() => reviewReport("RESOLVE")}
                                    >
                                      {mutatingKey === "report-review-RESOLVE" && <Loader2 className="mr-2 size-4 animate-spin" />}
                                      위반 확정
                                    </Button>
                                    <Button
                                      variant="outline"
                                      disabled={!resolutionNote.trim() || mutatingKey === "report-review-REJECT"}
                                      onClick={() => reviewReport("REJECT")}
                                    >
                                      위반 아님
                                    </Button>
                                  </div>
                                </div>
                              )}
                            </CardContent>
                          </Card>

                          {selectedReport.status === "RESOLVED" && (
                            <Card className="border-border/60">
                              <CardHeader className="pb-3">
                                <CardTitle className="flex items-center gap-2 text-sm">
                                  <Gavel className="size-4 text-primary" />
                                  제재 적용
                                </CardTitle>
                                <CardDescription>
                                  {reportKind === "speech"
                                    ? "추천안은 보조 판단 자료이며, 관리자가 유형과 기간을 직접 선택해 제재를 적용할 수 있습니다."
                                    : "채팅 신고는 현재 직접 제재로 처리합니다. 채팅 신고 ID는 제재 이력에 연결하지 않습니다."}
                                </CardDescription>
                              </CardHeader>
                              <CardContent className="space-y-3">
                                <Button
                                  variant="outline"
                                  className="w-full gap-1.5"
                                  disabled={mutatingKey === "sanction-context"}
                                  onClick={loadSanctionContext}
                                >
                                  {mutatingKey === "sanction-context" ? <Loader2 className="size-4 animate-spin" /> : <Gavel className="size-4" />}
                                  {reportKind === "speech" ? "제재 추천안 계산" : "채팅 제재 기본값 채우기"}
                                </Button>
                                {recommendation && (
                                  <div className="space-y-2 rounded-lg border bg-muted/30 p-3">
                                    <div className="grid gap-1 text-xs text-muted-foreground">
                                      <span>추천 제재 유형: {sanctionTypeLabel(recommendation.recommendedType)}</span>
                                      <span>추천 제재 기간: {recommendation.recommendedDurationHours ? `${recommendation.recommendedDurationHours}시간` : "기간 없음"}</span>
                                      <span>누적 점수: {recommendation.weightedScore}</span>
                                      <span>동일 활성 제재: {recommendation.activeSameTypeSanction ? `있음 #${recommendation.activeSameTypeSanctionId}` : "없음"}</span>
                                      {recommendation.activeSameTypeEndsAt && (
                                        <span>현재 종료 시각: {new Date(recommendation.activeSameTypeEndsAt).toLocaleString("ko-KR")}</span>
                                      )}
                                    </div>
                                    {recommendation.activeSameTypeSanction && recommendation.recommendedDurationHours && (
                                      <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900">
                                        동일 유형 활성 제재가 있어 신규 등록 대신 기존 제재 연장을 우선합니다.
                                      </div>
                                    )}
                                    {recommendation.accountSuspensionReviewRecommended && (
                                      <div className="rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-xs text-destructive">
                                        반복·심각 위반 기준에 따라 계정 정지 검토가 필요합니다.
                                      </div>
                                    )}
                                  </div>
                                )}
                                {selectedReportTargetUser?.role === "ADMIN" ? (
                                  <div className="rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-xs text-destructive">
                                    관리자 계정은 제재 대상에서 제외됩니다. 검토 결과는 신고 처리 이력으로만 남길 수 있습니다.
                                  </div>
                                ) : (
                                  <div className="space-y-2 rounded-lg border p-3">
                                    <p className="text-xs font-semibold">관리자 직접 제재 적용</p>
                                    <select
                                      value={manualSanctionType}
                                      onChange={(event) => {
                                        const nextType = event.target.value as UserSanctionType
                                        setManualSanctionType(nextType)
                                        if (!SANCTION_TYPES.find((type) => type.value === nextType)?.requiresDuration) {
                                          setManualSanctionDurationHours("")
                                        }
                                      }}
                                      className="h-9 w-full rounded-md border bg-background px-2 text-xs"
                                    >
                                      {SANCTION_TYPES.map((type) => (
                                        <option key={type.value} value={type.value}>{type.label}</option>
                                      ))}
                                    </select>
                                    {selectedSanctionType?.requiresDuration && (
                                      <div className="space-y-1">
                                        <Input
                                          type="number"
                                          min={1}
                                          max={selectedSanctionType.maxDurationHours}
                                          value={manualSanctionDurationHours}
                                          onChange={(event) => setManualSanctionDurationHours(event.target.value)}
                                          placeholder={`제재 기간(시간), 최대 ${selectedSanctionType.maxDurationHours}시간`}
                                          className="h-9 text-xs"
                                        />
                                        <p className="text-[11px] text-muted-foreground">
                                          최대 {selectedSanctionType.maxDurationHours}시간(30일)까지 입력할 수 있습니다.
                                        </p>
                                      </div>
                                    )}
                                    <textarea
                                      value={sanctionReason}
                                      onChange={(event) => setSanctionReason(event.target.value)}
                                      maxLength={500}
                                      rows={3}
                                      placeholder="제재 사유"
                                      className="w-full resize-none rounded-lg border bg-background px-3 py-2 text-xs outline-none focus:border-primary"
                                    />
                                    <Button
                                      className="w-full"
                                      disabled={
                                        !sanctionReason.trim()
                                        || (selectedSanctionType?.requiresDuration && !manualSanctionDurationHours.trim())
                                        || blocksDuplicateRecommendedSanction
                                        || mutatingKey === "sanction-create"
                                      }
                                      onClick={createManualSanction}
                                    >
                                      {mutatingKey === "sanction-create" && <Loader2 className="mr-2 size-4 animate-spin" />}
                                      선택한 제재 적용
                                    </Button>
                                    {recommendation?.activeSameTypeSanction && recommendation.recommendedDurationHours && (
                                      <Button
                                        variant="outline"
                                        className="w-full"
                                        disabled={!sanctionReason.trim() || mutatingKey === "sanction-extend"}
                                        onClick={extendRecommendedSanction}
                                      >
                                        {mutatingKey === "sanction-extend" && <Loader2 className="mr-2 size-4 animate-spin" />}
                                        기존 제재 연장
                                      </Button>
                                    )}
                                    {blocksDuplicateRecommendedSanction && (
                                      <p className="text-[11px] text-muted-foreground">
                                        동일 유형 제재는 중복 등록하지 않고 기존 제재를 연장하는 방식으로 처리합니다.
                                      </p>
                                    )}
                                  </div>
                                )}
                                <div className="space-y-2">
                                  <p className="text-xs font-semibold">최근 제재 이력</p>
                                  {sanctions.length === 0 ? (
                                    <div className="rounded-lg border border-dashed px-3 py-3 text-xs text-muted-foreground">
                                      대상 사용자의 최근 제재 이력이 없습니다.
                                    </div>
                                  ) : (
                                    sanctions.slice(0, 5).map((sanction) => (
                                      <div key={sanction.sanctionId} className="rounded-lg border px-3 py-2 text-xs">
                                        <div className="flex flex-wrap items-center gap-2">
                                          <Badge variant="outline">#{sanction.sanctionId}</Badge>
                                          <Badge variant="secondary">{sanctionTypeLabel(sanction.type)}</Badge>
                                          <Badge variant={sanction.state === "ACTIVE" ? "destructive" : "outline"}>
                                            {sanctionStateLabel(sanction.state)}
                                          </Badge>
                                        </div>
                                        <p className="mt-1 text-muted-foreground">기간: {sanctionPeriodLabel(sanction)}</p>
                                        <p className="mt-1 text-muted-foreground">{sanction.reason}</p>
                                        {sanction.revokedAt && (
                                          <p className="mt-1 text-muted-foreground">
                                            해제: {new Date(sanction.revokedAt).toLocaleString("ko-KR")}
                                          </p>
                                        )}
                                      </div>
                                    ))
                                  )}
                                </div>
                              </CardContent>
                            </Card>
                          )}
                        </>
                      ) : (
                        <EmptyBox message="선택된 신고가 없습니다." />
                      )}
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="users" className="space-y-5">
            <Card>
              <CardHeader>
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2 text-sm">
                      <Shield className="size-4 text-primary" />
                      사용자 계정 관리
                    </CardTitle>
                    <CardDescription>
                      사용자 계정 상태와 제재 이력을 확인하고, 오인 제재는 사유를 남긴 뒤 해제합니다.
                    </CardDescription>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Input
                      value={userKeyword}
                      onChange={(event) => setUserKeyword(event.target.value)}
                      placeholder="이메일 또는 닉네임 검색"
                      className="h-9 w-48 text-xs"
                    />
                    <select
                      value={userStatusFilter}
                      onChange={(event) => {
                        const nextStatus = event.target.value as AdminUserStatus | ""
                        setUserStatusFilter(nextStatus)
                        void loadAdminUsers({ status: nextStatus })
                      }}
                      className="h-9 rounded-md border bg-background px-2 text-xs"
                    >
                      {USER_STATUSES.map((status) => (
                        <option key={status.value || "all"} value={status.value}>{status.label}</option>
                      ))}
                    </select>
                    <select
                      value={userRoleFilter}
                      onChange={(event) => {
                        const nextRole = event.target.value as AdminUserRole | ""
                        setUserRoleFilter(nextRole)
                        void loadAdminUsers({ role: nextRole })
                      }}
                      className="h-9 rounded-md border bg-background px-2 text-xs"
                    >
                      {USER_ROLES.map((role) => (
                        <option key={role.value || "all"} value={role.value}>{role.label}</option>
                      ))}
                    </select>
                    <Button
                      variant="outline"
                      size="sm"
                      className="gap-1.5 text-xs"
                      onClick={() => void loadAdminUsers()}
                      disabled={usersLoading}
                    >
                      <RefreshCw className="size-3.5" />
                      조회
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                {usersMessage && <StatusMessage tone="success" message={usersMessage} />}
                {usersError && <StatusMessage tone="error" message={usersError} />}
                <div className="mb-4 rounded-lg border border-muted bg-muted/20 px-3 py-2 text-xs text-muted-foreground">
                  비밀번호는 보안상 조회하거나 표시하지 않습니다. 계정 접근 차단과 복구는 제재 이력 기준으로 처리합니다.
                </div>
                {usersLoading ? (
                  <LoadingBox message="사용자 목록을 불러오는 중입니다." />
                ) : adminUsers.length === 0 ? (
                  <EmptyBox message="조회 조건에 해당하는 사용자가 없습니다." />
                ) : (
                  <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(380px,0.9fr)]">
                    <div className="space-y-2">
                      {adminUsers.map((adminUser) => (
                        <button
                          key={adminUser.userId}
                          type="button"
                          onClick={() => void selectAdminUser(adminUser.userId)}
                          className={`w-full rounded-lg border p-3 text-left transition-colors ${selectedAdminUser?.userId === adminUser.userId ? "border-primary bg-primary/5" : "border-border/60 hover:bg-muted/40"}`}
                        >
                          <div className="mb-2 flex flex-wrap items-center gap-2">
                            <Badge variant="outline">#{adminUser.userId}</Badge>
                            <Badge variant={adminUser.status === "BANNED" ? "destructive" : "secondary"}>
                              {userStatusLabel(adminUser.status)}
                            </Badge>
                            <Badge variant="outline">{userRoleLabel(adminUser.role)}</Badge>
                          </div>
                          <p className="text-sm font-semibold text-foreground">{adminUser.nickname}</p>
                          <div className="mt-2 grid gap-1 text-[11px] text-muted-foreground sm:grid-cols-2">
                            <span>{adminUser.email}</span>
                            <span>가입: {new Date(adminUser.createdAt).toLocaleString("ko-KR")}</span>
                            <span>수정: {new Date(adminUser.updatedAt).toLocaleString("ko-KR")}</span>
                          </div>
                        </button>
                      ))}
                    </div>

                    <div className="space-y-4">
                      {selectedAdminUser ? (
                        <>
                          <Card className="border-border/60">
                            <CardHeader className="pb-3">
                              <CardTitle className="text-sm">
                                {selectedAdminUser.nickname} 계정 #{selectedAdminUser.userId}
                              </CardTitle>
                              <CardDescription>{selectedAdminUser.email}</CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-3">
                              {userDetailLoading ? (
                                <LoadingBox message="계정 상세 정보를 불러오는 중입니다." />
                              ) : (
                                <>
                                  <div className="grid gap-2 text-xs text-muted-foreground sm:grid-cols-2">
                                    <span>상태: {userStatusLabel(selectedAdminUser.status)}</span>
                                    <span>권한: {userRoleLabel(selectedAdminUser.role)}</span>
                                    <span>가입일: {new Date(selectedAdminUser.createdAt).toLocaleString("ko-KR")}</span>
                                    <span>수정일: {new Date(selectedAdminUser.updatedAt).toLocaleString("ko-KR")}</span>
                                  </div>
                                  {selectedAdminUser.status === "BANNED" && (
                                    <div className="rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-xs text-destructive">
                                      계정 정지 상태입니다. 활성 ACCOUNT_SUSPENSION 제재를 선택해 해제하면 계정이 다시 활성화됩니다.
                                    </div>
                                  )}
                                </>
                              )}
                            </CardContent>
                          </Card>

                          <Card className="border-border/60">
                            <CardHeader className="pb-3">
                              <CardTitle className="text-sm">제재 이력</CardTitle>
                              <CardDescription>
                                제재 해제는 운영 이력 보존을 위해 삭제가 아니라 REVOKED 상태로 기록됩니다.
                              </CardDescription>
                            </CardHeader>
                            <CardContent className="space-y-3">
                              {accountSanctions.length === 0 ? (
                                <EmptyBox message="제재 이력이 없습니다. 사용자를 선택하면 최근 제재 이력을 조회합니다." />
                              ) : (
                                <div className="space-y-2">
                                  {accountSanctions.map((sanction) => (
                                    <button
                                      key={sanction.sanctionId}
                                      type="button"
                                      onClick={() => {
                                        setSelectedRevokeSanction(sanction)
                                        setRevokeReason("")
                                      }}
                                      className={`w-full rounded-lg border px-3 py-2 text-left text-xs transition-colors ${selectedRevokeSanction?.sanctionId === sanction.sanctionId ? "border-primary bg-primary/5" : "border-border/60 hover:bg-muted/40"}`}
                                    >
                                      <div className="flex flex-wrap items-center gap-2">
                                        <Badge variant="outline">#{sanction.sanctionId}</Badge>
                                        <Badge variant="secondary">{sanctionTypeLabel(sanction.type)}</Badge>
                                        <Badge variant={sanction.state === "ACTIVE" ? "destructive" : "outline"}>
                                          {sanctionStateLabel(sanction.state)}
                                        </Badge>
                                      </div>
                                      <p className="mt-1 text-muted-foreground">기간: {sanctionPeriodLabel(sanction)}</p>
                                      <p className="mt-1 text-muted-foreground">{sanction.reason}</p>
                                      {sanction.revokedAt && (
                                        <p className="mt-1 text-muted-foreground">
                                          해제: {new Date(sanction.revokedAt).toLocaleString("ko-KR")} · {sanction.revocationReason}
                                        </p>
                                      )}
                                    </button>
                                  ))}
                                </div>
                              )}

                              {selectedRevokeSanction && selectedRevokeSanction.state === "ACTIVE" && (
                                <div className="space-y-2 rounded-lg border p-3">
                                  <p className="text-xs font-semibold">
                                    선택한 제재 해제 #{selectedRevokeSanction.sanctionId}
                                  </p>
                                  <textarea
                                    value={revokeReason}
                                    onChange={(event) => setRevokeReason(event.target.value)}
                                    maxLength={500}
                                    rows={3}
                                    placeholder="해제 사유를 입력하세요. 예: 오인 제재 확인"
                                    className="w-full resize-none rounded-lg border bg-background px-3 py-2 text-xs outline-none focus:border-primary"
                                  />
                                  <Button
                                    className="w-full"
                                    variant={selectedRevokeSanction.type === "ACCOUNT_SUSPENSION" ? "default" : "outline"}
                                    disabled={!revokeReason.trim() || mutatingKey === "user-sanction-revoke"}
                                    onClick={revokeSelectedSanction}
                                  >
                                    {mutatingKey === "user-sanction-revoke" && <Loader2 className="mr-2 size-4 animate-spin" />}
                                    {selectedRevokeSanction.type === "ACCOUNT_SUSPENSION" ? "계정 정지 해제" : "선택한 제재 해제"}
                                  </Button>
                                </div>
                              )}
                            </CardContent>
                          </Card>
                        </>
                      ) : (
                        <EmptyBox message="선택된 사용자가 없습니다." />
                      )}
                    </div>
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
  const relationScore = relationScoreLabel(review?.confidence ?? null)

  return (
    <div className="rounded-lg border border-border/60 bg-muted/20 p-3">
      <div className="flex flex-wrap items-center gap-2">
        <Badge variant={review?.status === "COMPLETED" ? "secondary" : "outline"} className="text-[10px]">
          AI 검토 보조 결과
        </Badge>
        <p className={`text-xs font-medium ${aiReviewTextClass(review)}`}>
          {aiReviewLabel(review)}
          {relationScore ? ` · ${relationScore}` : ""}
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
