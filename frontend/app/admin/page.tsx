"use client"

import Link from "next/link"
import { FormEvent, type ReactNode, useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { useAuth } from "@/components/auth-provider"
import { ApiError } from "@/lib/api/client"
import { adminApi, topicApi } from "@/lib/api/services"
import type { ClassifiedIssueCandidate, ClassifiedIssueNews, TopicSummary } from "@/lib/api/types"
import {
  CheckCircle2,
  ExternalLink,
  FileText,
  Loader2,
  Pencil,
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

function messageOf(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
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
  const [mutatingKey, setMutatingKey] = useState("")

  const isAdmin = user?.role === "ADMIN"

  const candidateCountLabel = useMemo(
    () => `${candidates.length.toLocaleString()}개`,
    [candidates.length],
  )

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

  useEffect(() => {
    if (!loading && isAdmin) {
      void loadCandidates()
      void loadTopics()
    }
  }, [loading, isAdmin])

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
      const message = `토픽 #${created.topicId}이 등록되었습니다.`
      setCandidateMessage(message)
      setTopicMessage(message)
      await loadTopics()
      return true
    } catch (error) {
      setCandidatesError(messageOf(error, "토픽 등록에 실패했습니다."))
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

  const createRoomFromTopic = async (topicId: number) => {
    setMutatingKey(`room-create-${topicId}`)
    setTopicsError("")
    setTopicMessage("")
    try {
      const room = await adminApi.createRoom(topicId)
      setTopicMessage(`토론방 #${room.roomId}이 생성되었습니다.`)
    } catch (error) {
      setTopicsError(messageOf(error, "토론방 생성에 실패했습니다. 이미 토론방이 있는 토픽일 수 있습니다."))
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
          <h1 className="text-xl font-bold text-foreground">토픽 관리</h1>
          <p className="text-sm text-muted-foreground">
            스케줄러가 모은 이슈 후보를 확인하고, 필요한 후보만 승인된 토픽으로 등록합니다.
          </p>
        </div>

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
                      3시간마다 자동 갱신되는 후보를 보거나, 지금 즉시 새로 가져올 수 있습니다.
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
                              )})}
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
                <CardDescription>후보에 없는 주제도 바로 승인된 토픽으로 등록할 수 있습니다.</CardDescription>
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
                    <CardDescription>DB에 저장되어 토론방 생성에 사용할 수 있는 토픽입니다.</CardDescription>
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
                              <span className="text-[11px] text-muted-foreground">
                                #{topic.id}
                              </span>
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
                              disabled={mutatingKey === `room-create-${topic.id}`}
                              onClick={() => createRoomFromTopic(topic.id)}
                            >
                              {mutatingKey === `room-create-${topic.id}` ? <Loader2 className="size-3.5 animate-spin" /> : <Zap className="size-3.5" />}
                              토론방 생성
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
