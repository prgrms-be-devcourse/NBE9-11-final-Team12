"use client"

import Link from "next/link"
import { FormEvent, useState } from "react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Progress } from "@/components/ui/progress"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Separator } from "@/components/ui/separator"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { ScrollArea } from "@/components/ui/scroll-area"
import { cn } from "@/lib/utils"
import { mockTopics } from "@/lib/mock-data"
import { useAuth } from "@/components/auth-provider"
import { adminApi } from "@/lib/api/services"
import { ApiError } from "@/lib/api/client"
import {
  Zap,
  Users,
  Radio,
  MessageSquare,
  TrendingUp,
  Shield,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Clock,
  Activity,
  BarChart3,
  Settings,
  Eye,
  Ban,
  Flag,
  ChevronRight,
  ArrowUpRight,
  ArrowDownRight,
  Server,
  Wifi,
  Database,
} from "lucide-react"

/* ─── mock admin data ─── */
const pendingTopics = [
  {
    id: "pt1",
    title: "AI 뉴스 신뢰 시스템 재설계 토의",
    category: "AI·기술",
    requestedBy: "@ai_watcher",
    createdAt: "5분 전",
    status: "pending",
  },
  {
    id: "pt2",
    title: "주 4일제 도입 기업 성과 분석",
    category: "경제·금융",
    requestedBy: "@work_balance",
    createdAt: "12분 전",
    status: "pending",
  },
  {
    id: "pt3",
    title: "청소년 미디어 중독 사회적 비용",
    category: "사회·복지",
    requestedBy: "@social_study",
    createdAt: "28분 전",
    status: "pending",
  },
]

const communityLogs = [
  { id: "l1", type: "BLOCK", user: "218.xxx.xxx", reason: "스팸 발언 반복", time: "14:23", severity: "high" },
  { id: "l2", type: "WARN", user: "@troll_99", reason: "부적절한 언어 사용", time: "14:19", severity: "medium" },
  { id: "l3", type: "BLOCK", user: "112.xxx.xxx", reason: "광고성 메시지", time: "14:11", severity: "high" },
  { id: "l4", type: "WARN", user: "@anon_42k", reason: "허위 정보 유포", time: "14:05", severity: "medium" },
  { id: "l5", type: "MUTE", user: "@off_topic", reason: "주제 이탈 반복", time: "13:58", severity: "low" },
  { id: "l6", type: "BLOCK", user: "175.xxx.xxx", reason: "개인정보 유출 시도", time: "13:50", severity: "high" },
]

const trafficData = [
  { label: "WebSocket 연결", value: 24681, unit: "개", trend: "up", delta: "+3.2%" },
  { label: "Redis PubSub", value: 5.2, unit: "k/s", trend: "up", delta: "+1.1%" },
  { label: "Live Opinion Stream", value: 317, unit: "msg/s", trend: "down", delta: "-2.4%" },
]

const roomStatuses = mockTopics.slice(0, 5).map((t, idx) => ({
  ...t,
  adminStatus: idx < 2 ? "ACTIVE" : idx === 2 ? "WARNING" : "NORMAL",
  moderatorCount: Math.floor(Math.random() * 3) + 1,
}))

const overviewStats = [
  { label: "총 활성 사용자", value: "48,291", delta: "+12.4%", trend: "up", icon: Users, color: "text-primary" },
  { label: "진행 중 토의방", value: "127", delta: "+8개", trend: "up", icon: Radio, color: "text-accent" },
  { label: "오늘 총 메시지", value: "312,847", delta: "+18.7%", trend: "up", icon: MessageSquare, color: "text-emerald-600 dark:text-emerald-400" },
  { label: "차단 조치", value: "23", delta: "-5개", trend: "down", icon: Ban, color: "text-destructive" },
]

function StatusBadge({ status }: { status: string }) {
  return (
    <Badge
      className={cn(
        "text-[10px] font-medium",
        status === "ACTIVE" && "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-500/15 dark:text-emerald-400 dark:border-emerald-500/30",
        status === "WARNING" && "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-500/15 dark:text-amber-400 dark:border-amber-500/30",
        status === "NORMAL" && "bg-muted text-muted-foreground border-border"
      )}
    >
      {status}
    </Badge>
  )
}

function SeverityDot({ severity }: { severity: string }) {
  return (
    <span
      className={cn(
        "inline-block size-2 rounded-full flex-shrink-0",
        severity === "high" && "bg-destructive",
        severity === "medium" && "bg-amber-400",
        severity === "low" && "bg-muted-foreground"
      )}
    />
  )
}

export default function AdminDashboardPage() {
  const { user, loading } = useAuth()
  const [approving, setApproving] = useState<string | null>(null)
  const [title, setTitle] = useState("")
  const [description, setDescription] = useState("")
  const [category, setCategory] = useState("")
  const [sourceUrl, setSourceUrl] = useState("")
  const [createdRoomId, setCreatedRoomId] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState("")

  const handleApprove = (id: string) => {
    setApproving(id)
    // TODO: 백엔드 연동 — PATCH /api/admin/topics/:id/approve
    setTimeout(() => setApproving(null), 800)
  }

  const createTopicAndRoom = async (event: FormEvent) => {
    event.preventDefault()
    setCreating(true)
    setCreateError("")
    setCreatedRoomId(null)
    try {
      const topic = await adminApi.createTopic({
        title: title.trim(),
        description: description.trim() || undefined,
        category: category.trim(),
        sourceUrl: sourceUrl.trim() || undefined,
      })
      const room = await adminApi.createRoom(topic.topicId)
      setCreatedRoomId(room.roomId)
    } catch (error) {
      setCreateError(error instanceof ApiError ? error.message : "토픽과 토론방 생성에 실패했습니다.")
    } finally {
      setCreating(false)
    }
  }

  if (loading) return <div className="flex min-h-screen items-center justify-center">인증 확인 중...</div>
  if (!user || user.role !== "ADMIN") {
    return <div className="flex min-h-screen flex-col items-center justify-center gap-3"><Shield className="size-8 text-destructive" /><p className="font-semibold">관리자만 접근할 수 있습니다.</p><Link href="/"><Button>홈으로</Button></Link></div>
  }

  return (
    <div className="min-h-screen bg-background">
      {/* Admin top bar */}
      <header className="sticky top-0 z-50 border-b border-border/50 bg-background/95 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-screen-xl items-center justify-between px-4 md:px-6">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2">
            <div className="flex size-8 items-center justify-center rounded-lg bg-primary">
              <Zap className="size-4 text-primary-foreground" />
            </div>
            <span className="font-bold text-foreground">시시비비</span>
            <Badge className="bg-accent/20 text-accent border-accent/30 text-[10px]">
              관리자
            </Badge>
          </Link>

          {/* Nav */}
          <nav className="hidden items-center gap-1 md:flex">
            {[
              { label: "대시보드", icon: BarChart3, active: true },
              { label: "토의방 관리", icon: Radio, active: false },
              { label: "사용자 관리", icon: Users, active: false },
              { label: "설정", icon: Settings, active: false },
            ].map(({ label, icon: Icon, active }) => (
              <button
                key={label}
                className={cn(
                  "flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  active
                    ? "bg-primary/10 text-primary"
                    : "text-muted-foreground hover:bg-muted hover:text-foreground"
                )}
              >
                <Icon className="size-4" />
                {label}
              </button>
            ))}
          </nav>

          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1.5 text-xs font-medium text-emerald-700 dark:text-emerald-400">
              <span className="size-2 rounded-full bg-emerald-500 animate-live-pulse" />
              시스템 정상
            </div>
            <Avatar className="size-8">
              <AvatarFallback className="bg-primary/20 text-primary text-xs font-bold">
                AD
              </AvatarFallback>
            </Avatar>
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-screen-xl px-4 py-6 md:px-6">
        {/* Page title */}
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold text-foreground">관리자 대시보드</h1>
            <p className="text-sm text-muted-foreground">
              마지막 업데이트: 방금 전 · 자동 새로고침 30초
            </p>
          </div>
          <Button variant="outline" size="sm" className="gap-2 text-xs">
            <Activity className="size-3.5 text-primary" />
            실시간 모니터링
          </Button>
        </div>

        <Card className="mb-6 border-primary/20">
          <CardHeader>
            <CardTitle className="text-sm">승인 토픽 및 토론방 생성</CardTitle>
            <CardDescription>현재 백엔드 계약에 따라 승인된 토픽을 생성한 뒤 토론방을 연속 생성합니다.</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="grid gap-3 md:grid-cols-2" onSubmit={createTopicAndRoom}>
              <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="토픽 제목" required className="rounded-lg border bg-background px-3 py-2 text-sm" />
              <input value={category} onChange={(event) => setCategory(event.target.value)} placeholder="카테고리" required className="rounded-lg border bg-background px-3 py-2 text-sm" />
              <input value={sourceUrl} onChange={(event) => setSourceUrl(event.target.value)} placeholder="출처 URL (선택)" className="rounded-lg border bg-background px-3 py-2 text-sm md:col-span-2" />
              <textarea value={description} onChange={(event) => setDescription(event.target.value)} placeholder="토픽 설명 (선택)" rows={3} className="resize-none rounded-lg border bg-background px-3 py-2 text-sm md:col-span-2" />
              {createError && <p className="text-xs text-destructive md:col-span-2">{createError}</p>}
              {createdRoomId && <p className="text-xs text-emerald-600 md:col-span-2">토론방 #{createdRoomId} 생성 완료 · <Link className="underline" href={`/rooms/${createdRoomId}`}>입장하기</Link></p>}
              <Button type="submit" disabled={creating || !title.trim() || !category.trim()} className="md:col-span-2">{creating ? "생성 중..." : "토픽 승인 및 토론방 생성"}</Button>
            </form>
          </CardContent>
        </Card>

        {/* Overview stats */}
        <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {overviewStats.map(({ label, value, delta, trend, icon: Icon, color }) => (
            <Card key={label} className="border-border/50 bg-card">
              <CardContent className="flex items-center gap-4 pt-5 pb-5">
                <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-muted">
                  <Icon className={cn("size-5", color)} />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xl font-bold text-foreground">{value}</p>
                  <p className="text-xs text-muted-foreground">{label}</p>
                  <div className="mt-0.5 flex items-center gap-1">
                    {trend === "up" ? (
                      <ArrowUpRight className="size-3 text-emerald-600 dark:text-emerald-400" />
                    ) : (
                      <ArrowDownRight className="size-3 text-destructive" />
                    )}
                    <span
                      className={cn(
                        "text-[11px] font-medium",
                        trend === "up" ? "text-emerald-600 dark:text-emerald-400" : "text-destructive"
                      )}
                    >
                      {delta}
                    </span>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* Main grid: 3 columns */}
        <div className="grid gap-6 lg:grid-cols-3">

          {/* AI 토픽 승인 대기 */}
          <Card className="border-border/50 bg-card lg:col-span-1">
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2 text-sm font-semibold">
                  <Zap className="size-4 text-primary" />
                  AI 토픽 승인 대기
                </CardTitle>
                <Badge className="bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-500/15 dark:text-amber-400 dark:border-amber-500/30 text-[10px]">
                  {pendingTopics.length}건
                </Badge>
              </div>
              <CardDescription className="text-[11px]">
                AI가 제안한 토픽을 검토하고 승인하세요
              </CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-3 pt-0">
              {pendingTopics.map((topic) => (
                <div
                  key={topic.id}
                  className="rounded-xl border border-border/50 bg-muted/30 p-3"
                >
                  <div className="mb-1.5 flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <Badge
                        variant="outline"
                        className="mb-1 border-border/50 text-[10px] text-muted-foreground"
                      >
                        {topic.category}
                      </Badge>
                      <p className="line-clamp-2 text-xs font-medium text-foreground leading-snug">
                        {topic.title}
                      </p>
                    </div>
                  </div>
                  <div className="mb-2 flex items-center gap-1.5 text-[11px] text-muted-foreground">
                    <span>{topic.requestedBy}</span>
                    <span>·</span>
                    <Clock className="size-3" />
                    <span>{topic.createdAt}</span>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      className="h-7 flex-1 gap-1 text-[11px] bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 dark:bg-emerald-500/15 dark:text-emerald-400 dark:border-emerald-500/30 dark:hover:bg-emerald-500/25"
                      variant="ghost"
                      onClick={() => handleApprove(topic.id)}
                      disabled={approving === topic.id}
                    >
                      <CheckCircle2 className="size-3" />
                      승인
                    </Button>
                    <Button
                      size="sm"
                      className="h-7 flex-1 gap-1 text-[11px] bg-red-50 text-red-600 border border-red-200 hover:bg-red-100 dark:bg-destructive/10 dark:text-destructive dark:border-destructive/30 dark:hover:bg-destructive/20"
                      variant="ghost"
                    >
                      <XCircle className="size-3" />
                      거절
                    </Button>
                  </div>
                </div>
              ))}
              <Button
                variant="ghost"
                size="sm"
                className="w-full gap-1 text-xs text-muted-foreground"
              >
                전체 보기
                <ChevronRight className="size-3.5" />
              </Button>
            </CardContent>
          </Card>

          {/* 커뮤니티 보호 로그 */}
          <Card className="border-border/50 bg-card lg:col-span-1">
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="flex items-center gap-2 text-sm font-semibold">
                  <Shield className="size-4 text-accent" />
                  커뮤니티 보호 로그
                </CardTitle>
                <Badge className="bg-destructive/15 text-destructive border-destructive/30 text-[10px]">
                  오늘 23건
                </Badge>
              </div>
              <CardDescription className="text-[11px]">
                자동 필터링 및 관리자 조치 내역
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-0">
              <ScrollArea className="h-[300px] pr-2">
                <div className="flex flex-col gap-2">
                  {communityLogs.map((log) => (
                    <div
                      key={log.id}
                      className="flex items-start gap-2.5 rounded-lg border border-border/50 bg-muted/20 px-3 py-2.5"
                    >
                      <SeverityDot severity={log.severity} />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <Badge
                            className={cn(
                              "text-[9px] px-1 py-0",
                              log.type === "BLOCK" && "bg-destructive/20 text-destructive border-destructive/30",
                              log.type === "WARN" && "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-500/15 dark:text-amber-400 dark:border-amber-500/30",
                              log.type === "MUTE" && "bg-muted text-muted-foreground border-border/50"
                            )}
                          >
                            {log.type}
                          </Badge>
                          <span className="truncate text-[11px] font-medium text-foreground">
                            {log.user}
                          </span>
                        </div>
                        <p className="mt-0.5 text-[11px] text-muted-foreground">{log.reason}</p>
                      </div>
                      <span className="shrink-0 text-[10px] text-muted-foreground">{log.time}</span>
                    </div>
                  ))}
                </div>
              </ScrollArea>
            </CardContent>
          </Card>

          {/* 실시간 트래픽 + 토의방 상태 */}
          <div className="flex flex-col gap-6 lg:col-span-1">
            {/* 실시간 트래픽 */}
            <Card className="border-border/50 bg-card">
              <CardHeader className="pb-3">
                <CardTitle className="flex items-center gap-2 text-sm font-semibold">
                  <Server className="size-4 text-emerald-600 dark:text-emerald-400" />
                  실시간 트래픽
                </CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col gap-3 pt-0">
                {trafficData.map(({ label, value, unit, trend, delta }) => (
                  <div key={label} className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Wifi className={cn("size-3.5", trend === "up" ? "text-emerald-600 dark:text-emerald-400" : "text-muted-foreground")} />
                      <span className="text-xs text-muted-foreground">{label}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-sm font-semibold text-foreground">
                        {value.toLocaleString()}
                        <span className="text-[10px] text-muted-foreground ml-0.5">{unit}</span>
                      </span>
                      <span
                        className={cn(
                          "text-[11px] font-medium",
                          trend === "up" ? "text-emerald-600 dark:text-emerald-400" : "text-destructive"
                        )}
                      >
                        {delta}
                      </span>
                    </div>
                  </div>
                ))}
                <Separator />
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Database className="size-3.5 text-primary" />
                    <span className="text-xs text-muted-foreground">DB 응답시간</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <div className="h-1.5 w-20 rounded-full bg-muted overflow-hidden">
                      <div className="h-full w-[18%] rounded-full bg-emerald-500 dark:bg-emerald-400" />
                    </div>
                    <span className="font-mono text-xs font-semibold text-emerald-700 dark:text-emerald-400">12ms</span>
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* 토의방 상태 관리 */}
            <Card className="border-border/50 bg-card flex-1">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <CardTitle className="flex items-center gap-2 text-sm font-semibold">
                    <Radio className="size-4 text-primary" />
                    토의방 상태 관리
                  </CardTitle>
                  <Button variant="ghost" size="sm" className="h-6 gap-1 text-[11px] text-muted-foreground">
                    전체 보기
                    <ChevronRight className="size-3" />
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="flex flex-col gap-2 pt-0">
                {roomStatuses.map((room) => (
                  <div
                    key={room.id}
                    className="flex items-center gap-2 rounded-lg border border-border/50 bg-muted/20 px-3 py-2"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-[11px] font-medium text-foreground">{room.title}</p>
                      <div className="mt-0.5 flex items-center gap-1.5 text-[10px] text-muted-foreground">
                        <Users className="size-3" />
                        {room.participants.toLocaleString()}
                        <span>·</span>
                        <Shield className="size-3" />
                        관리자 {room.moderatorCount}명
                      </div>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      <StatusBadge status={room.adminStatus} />
                      <Button variant="ghost" size="icon" className="size-6">
                        <Eye className="size-3 text-muted-foreground" />
                        <span className="sr-only">보기</span>
                      </Button>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          </div>
        </div>

        {/* Bottom: Detailed report tabs */}
        <div className="mt-6">
          <Card className="border-border/50 bg-card">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-sm font-semibold">
                <BarChart3 className="size-4 text-primary" />
                상세 리포트
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Tabs defaultValue="rooms">
                <TabsList className="mb-4 bg-muted/50 h-9">
                  <TabsTrigger value="rooms" className="text-xs">토의방 현황</TabsTrigger>
                  <TabsTrigger value="reports" className="text-xs">신고 현황</TabsTrigger>
                  <TabsTrigger value="users" className="text-xs">사용자 현황</TabsTrigger>
                </TabsList>

                <TabsContent value="rooms" className="mt-0">
                  <div className="overflow-hidden rounded-lg border border-border/50">
                    <table className="w-full text-xs">
                      <thead>
                        <tr className="border-b border-border/50 bg-muted/50">
                          <th className="px-4 py-2.5 text-left font-medium text-muted-foreground">토픽</th>
                          <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">참여자</th>
                          <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">메시지</th>
                          <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">상태</th>
                          <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">작업</th>
                        </tr>
                      </thead>
                      <tbody>
                        {mockTopics.map((topic) => (
                          <tr
                            key={topic.id}
                            className="border-b border-border/30 transition-colors hover:bg-muted/20"
                          >
                            <td className="px-4 py-3">
                              <div className="flex items-center gap-2">
                                {topic.isLive && (
                                  <span className="size-1.5 rounded-full bg-primary animate-live-pulse shrink-0" />
                                )}
                                <span className="line-clamp-1 font-medium text-foreground max-w-[200px]">
                                  {topic.title}
                                </span>
                              </div>
                            </td>
                            <td className="px-4 py-3 text-right text-muted-foreground">
                              {topic.participants.toLocaleString()}
                            </td>
                            <td className="px-4 py-3 text-right text-muted-foreground">
                              {topic.messages.toLocaleString()}
                            </td>
                            <td className="px-4 py-3 text-right">
                              <Badge
                                variant="outline"
                                className={cn(
                                  "text-[10px]",
                                  topic.status === "OPEN"
                                    ? "border-emerald-300 text-emerald-700 bg-emerald-50 dark:border-emerald-500/30 dark:text-emerald-400 dark:bg-transparent"
                                    : "border-border text-muted-foreground"
                                )}
                              >
                                {topic.status === "OPEN" ? "진행 중" : "종료"}
                              </Badge>
                            </td>
                            <td className="px-4 py-3 text-right">
                              <div className="flex items-center justify-end gap-1">
                                <Button variant="ghost" size="icon" className="size-6">
                                  <Eye className="size-3 text-muted-foreground" />
                                </Button>
                                <Button variant="ghost" size="icon" className="size-6">
                                  <Flag className="size-3 text-muted-foreground" />
                                </Button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </TabsContent>

                <TabsContent value="reports" className="mt-0">
                  <div className="flex flex-col gap-3">
                    {[
                      { category: "스팸/광고", count: 8, percent: 65, color: "bg-destructive" },
                      { category: "허위 정보 유포", count: 6, percent: 48, color: "bg-amber-400" },
                      { category: "부적절한 언어", count: 5, percent: 40, color: "bg-primary" },
                      { category: "개인정보 침해", count: 3, percent: 24, color: "bg-accent" },
                      { category: "기타", count: 1, percent: 8, color: "bg-muted-foreground" },
                    ].map(({ category, count, percent, color }) => (
                      <div key={category} className="flex items-center gap-3">
                        <span className="w-28 shrink-0 text-xs text-muted-foreground">{category}</span>
                        <div className="flex-1 overflow-hidden rounded-full bg-muted h-2">
                          <div
                            className={cn("h-full rounded-full", color)}
                            style={{ width: `${percent}%` }}
                          />
                        </div>
                        <span className="w-8 shrink-0 text-right text-xs font-medium text-foreground">
                          {count}건
                        </span>
                      </div>
                    ))}
                  </div>
                </TabsContent>

                <TabsContent value="users" className="mt-0">
                  <div className="grid gap-4 sm:grid-cols-3">
                    {[
                      { label: "신규 가입", value: "1,284", delta: "+23%", color: "text-primary" },
                      { label: "오늘 활성 사용자", value: "48,291", delta: "+12%", color: "text-emerald-700 dark:text-emerald-400" },
                      { label: "이탈률", value: "3.2%", delta: "-0.8%p", color: "text-accent" },
                    ].map(({ label, value, delta, color }) => (
                      <div
                        key={label}
                        className="rounded-xl border border-border/50 bg-muted/30 p-4 text-center"
                      >
                        <p className={cn("mb-1 text-2xl font-bold", color)}>{value}</p>
                        <p className="text-xs text-muted-foreground">{label}</p>
                        <p className="mt-1 text-[11px] font-medium text-emerald-700 dark:text-emerald-400">{delta}</p>
                      </div>
                    ))}
                  </div>
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
