"use client"

import Link from "next/link"
import { FormEvent, useEffect, useState } from "react"
import { Activity, Loader2, PlusCircle, Radio, Shield, Trash2, Users, Zap, type LucideIcon } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { ApiError } from "@/lib/api/client"
import { adminApi, roomApi, topicApi } from "@/lib/api/services"
import type { RoomSummary, TopicSummary } from "@/lib/api/types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

export default function AdminDashboardPage() {
  const { user, loading: authLoading } = useAuth()
  const [rooms, setRooms] = useState<RoomSummary[]>([])
  const [topics, setTopics] = useState<TopicSummary[]>([])
  const [title, setTitle] = useState("")
  const [description, setDescription] = useState("")
  const [category, setCategory] = useState("")
  const [sourceUrl, setSourceUrl] = useState("")
  const [createdRoomId, setCreatedRoomId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState("")

  const canAdmin = user?.role === "ADMIN"

  const loadData = async () => {
    setLoading(true)
    setError("")
    try {
      const [roomList, topicPage] = await Promise.all([roomApi.list(), topicApi.list(0, 50)])
      setRooms(roomList)
      setTopics(topicPage.content)
    } catch {
      setError("관리 데이터를 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!authLoading && canAdmin) void loadData()
  }, [authLoading, canAdmin])

  const createTopicAndRoom = async (event: FormEvent) => {
    event.preventDefault()
    setCreating(true)
    setError("")
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
      setTitle("")
      setDescription("")
      setCategory("")
      setSourceUrl("")
      await loadData()
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : "토픽과 토론방 생성에 실패했습니다.")
    } finally {
      setCreating(false)
    }
  }

  const deleteRoom = async (roomId: number) => {
    setError("")
    try {
      await adminApi.deleteRoom(roomId)
      await loadData()
    } catch (requestError) {
      setError(requestError instanceof ApiError ? requestError.message : "토론방 삭제에 실패했습니다.")
    }
  }

  if (authLoading) {
    return <FullScreenMessage icon={Loader2} message="인증 정보를 확인하는 중입니다." spin />
  }

  if (!canAdmin) {
    return (
      <FullScreenMessage icon={Shield} message="관리자만 접근할 수 있습니다.">
        <Link href="/">
          <Button>홈으로</Button>
        </Link>
      </FullScreenMessage>
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
            <Badge className="border-primary/30 bg-primary/10 text-primary text-[10px]">관리자</Badge>
          </Link>
          <div className="flex items-center gap-1.5 text-xs font-medium text-emerald-700 dark:text-emerald-400">
            <span className="size-2 rounded-full bg-emerald-500" />
            {user?.nickname}
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-screen-xl px-4 py-6 md:px-6">
        <div className="mb-6">
          <h1 className="text-xl font-bold text-foreground">관리자 대시보드</h1>
          <p className="text-sm text-muted-foreground">승인 토픽 생성과 토론방 운영 상태를 관리합니다.</p>
        </div>

        {error && <p className="mb-4 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{error}</p>}
        {createdRoomId && (
          <p className="mb-4 rounded-lg bg-emerald-50 px-3 py-2 text-xs text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-400">
            토론방 #{createdRoomId} 생성 완료. <Link className="underline" href={`/rooms/${createdRoomId}`}>입장하기</Link>
          </p>
        )}

        <div className="mb-6 grid gap-4 sm:grid-cols-3">
          <StatCard label="토론방" value={rooms.length.toLocaleString()} icon={Radio} />
          <StatCard label="열린 방" value={rooms.filter((room) => room.status === "OPEN").length.toLocaleString()} icon={Activity} />
          <StatCard label="승인 토픽" value={topics.length.toLocaleString()} icon={Users} />
        </div>

        <div className="grid gap-6 lg:grid-cols-[0.9fr_1.1fr]">
          <Card>
            <CardHeader>
              <CardTitle className="text-sm">토픽 및 토론방 생성</CardTitle>
              <CardDescription>백엔드 계약에 따라 승인 토픽을 만든 뒤 토론방을 생성합니다.</CardDescription>
            </CardHeader>
            <CardContent>
              <form className="grid gap-3" onSubmit={createTopicAndRoom}>
                <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="토픽 제목" required className="rounded-lg border bg-background px-3 py-2 text-sm" />
                <input value={category} onChange={(event) => setCategory(event.target.value)} placeholder="카테고리" required className="rounded-lg border bg-background px-3 py-2 text-sm" />
                <input value={sourceUrl} onChange={(event) => setSourceUrl(event.target.value)} placeholder="출처 URL (선택)" className="rounded-lg border bg-background px-3 py-2 text-sm" />
                <textarea value={description} onChange={(event) => setDescription(event.target.value)} placeholder="토픽 설명 (선택)" rows={4} className="resize-none rounded-lg border bg-background px-3 py-2 text-sm" />
                <Button type="submit" disabled={creating || !title.trim() || !category.trim()} className="gap-2">
                  {creating ? <Loader2 className="size-4 animate-spin" /> : <PlusCircle className="size-4" />}
                  {creating ? "생성 중" : "토론방 생성"}
                </Button>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-sm">토론방 운영 현황</CardTitle>
              <CardDescription>현재 등록된 토론방 목록입니다.</CardDescription>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="flex h-48 items-center justify-center">
                  <Loader2 className="size-5 animate-spin text-primary" />
                </div>
              ) : rooms.length === 0 ? (
                <div className="flex h-48 items-center justify-center text-sm text-muted-foreground">토론방이 없습니다.</div>
              ) : (
                <div className="overflow-hidden rounded-lg border border-border/50">
                  <table className="w-full text-xs">
                    <thead>
                      <tr className="border-b border-border/50 bg-muted/50">
                        <th className="px-4 py-2.5 text-left font-medium text-muted-foreground">토론방</th>
                        <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">상태</th>
                        <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">작업</th>
                      </tr>
                    </thead>
                    <tbody>
                      {rooms.map((room) => (
                        <tr key={room.roomId} className="border-b border-border/30 transition-colors hover:bg-muted/20">
                          <td className="px-4 py-3">
                            <Link href={`/rooms/${room.roomId}`} className="line-clamp-1 font-medium text-foreground hover:text-primary">
                              {room.title}
                            </Link>
                          </td>
                          <td className="px-4 py-3 text-right">
                            <Badge variant="outline" className="text-[10px]">{room.status}</Badge>
                          </td>
                          <td className="px-4 py-3 text-right">
                            <Button variant="ghost" size="icon-sm" onClick={() => void deleteRoom(room.roomId)} aria-label="토론방 삭제">
                              <Trash2 className="size-3.5 text-destructive" />
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  )
}

function StatCard({ label, value, icon: Icon }: { label: string; value: string; icon: LucideIcon }) {
  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-card px-4 py-3">
      <Icon className="size-5 text-primary" />
      <div>
        <p className="text-sm font-semibold text-foreground">{value}</p>
        <p className="text-[11px] text-muted-foreground">{label}</p>
      </div>
    </div>
  )
}

function FullScreenMessage({
  icon: Icon,
  message,
  spin = false,
  children,
}: {
  icon: LucideIcon
  message: string
  spin?: boolean
  children?: React.ReactNode
}) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3">
      <Icon className={`size-8 text-primary ${spin ? "animate-spin" : ""}`} />
      <p className="font-semibold">{message}</p>
      {children}
    </div>
  )
}
