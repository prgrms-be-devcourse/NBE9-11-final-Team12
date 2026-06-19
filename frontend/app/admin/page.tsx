"use client"

import Link from "next/link"
import { FormEvent, useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/components/auth-provider"
import { adminApi, roomApi, topicApi } from "@/lib/api/services"
import { ApiError } from "@/lib/api/client"
import type { RoomSummary, TopicDetail } from "@/lib/api/types"
import {
  CheckCircle2,
  Edit3,
  Radio,
  RefreshCw,
  Shield,
  Trash2,
  Users,
  Zap,
} from "lucide-react"

type AdminRoomRow = {
  room: RoomSummary
  topic: TopicDetail | null
  participantCount: number
}

function messageOf(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

export default function AdminDashboardPage() {
  const { user, loading } = useAuth()
  const [title, setTitle] = useState("")
  const [description, setDescription] = useState("")
  const [category, setCategory] = useState("")
  const [sourceUrl, setSourceUrl] = useState("")
  const [createdRoomId, setCreatedRoomId] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState("")
  const [rooms, setRooms] = useState<AdminRoomRow[]>([])
  const [roomsLoading, setRoomsLoading] = useState(true)
  const [roomsError, setRoomsError] = useState("")
  const [editingRoomId, setEditingRoomId] = useState<number | null>(null)
  const [editingTitle, setEditingTitle] = useState("")
  const [mutatingId, setMutatingId] = useState<number | null>(null)

  const loadRooms = async () => {
    setRoomsLoading(true)
    setRoomsError("")
    try {
      const response = await roomApi.list()
      const rows = await Promise.all(
        response.map(async (room) => {
          const [topicResult, countResult] = await Promise.allSettled([
            topicApi.detail(room.topicId),
            roomApi.participantCount(room.roomId),
          ])
          return {
            room,
            topic: topicResult.status === "fulfilled" ? topicResult.value : null,
            participantCount: countResult.status === "fulfilled" ? countResult.value.participantCount : 0,
          }
        }),
      )
      setRooms(rows)
    } catch (error) {
      setRoomsError(messageOf(error, "토론방 목록을 불러오지 못했습니다."))
    } finally {
      setRoomsLoading(false)
    }
  }

  useEffect(() => {
    if (!loading && user?.role === "ADMIN") {
      void loadRooms()
    }
  }, [loading, user?.role])

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
      setTitle("")
      setDescription("")
      setCategory("")
      setSourceUrl("")
      await loadRooms()
    } catch (error) {
      setCreateError(messageOf(error, "토픽과 토론방 생성에 실패했습니다."))
    } finally {
      setCreating(false)
    }
  }

  const startEdit = (row: AdminRoomRow) => {
    setEditingRoomId(row.room.roomId)
    setEditingTitle(row.room.title)
  }

  const saveRoomTitle = async (roomId: number) => {
    if (!editingTitle.trim()) return
    setMutatingId(roomId)
    setRoomsError("")
    try {
      await adminApi.updateRoom(roomId, { title: editingTitle.trim() })
      setEditingRoomId(null)
      setEditingTitle("")
      await loadRooms()
    } catch (error) {
      setRoomsError(messageOf(error, "토론방 수정에 실패했습니다."))
    } finally {
      setMutatingId(null)
    }
  }

  const deleteRoom = async (roomId: number) => {
    setMutatingId(roomId)
    setRoomsError("")
    try {
      await adminApi.deleteRoom(roomId)
      await loadRooms()
    } catch (error) {
      setRoomsError(messageOf(error, "토론방 삭제에 실패했습니다."))
    } finally {
      setMutatingId(null)
    }
  }

  const deleteTopic = async (topicId: number) => {
    setMutatingId(topicId)
    setRoomsError("")
    try {
      await adminApi.deleteTopic(topicId)
      await loadRooms()
    } catch (error) {
      setRoomsError(messageOf(error, "토픽 삭제에 실패했습니다. 연결된 토론방이 있으면 먼저 방을 삭제해야 합니다."))
    } finally {
      setMutatingId(null)
    }
  }

  if (loading) return <div className="flex min-h-screen items-center justify-center">인증 확인 중...</div>
  if (!user || user.role !== "ADMIN") {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3">
        <Shield className="size-8 text-destructive" />
        <p className="font-semibold">관리자만 접근할 수 있습니다.</p>
        <Link href="/"><Button>홈으로</Button></Link>
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
            <Badge className="bg-accent/20 text-accent border-accent/30 text-[10px]">
              관리자
            </Badge>
          </Link>

          <div className="flex items-center gap-3">
            <Button variant="outline" size="sm" className="gap-2 text-xs" onClick={loadRooms}>
              <RefreshCw className="size-3.5" />
              새로고침
            </Button>
            <Avatar className="size-8">
              <AvatarFallback className="bg-primary/20 text-primary text-xs font-bold">
                AD
              </AvatarFallback>
            </Avatar>
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-screen-xl px-4 py-6 md:px-6">
        <div className="mb-6">
          <h1 className="text-xl font-bold text-foreground">관리자</h1>
          <p className="text-sm text-muted-foreground">
            백엔드 관리자 API가 제공하는 토픽/토론방 생성, 수정, 삭제 기능만 표시합니다.
          </p>
        </div>

        <Card className="mb-6 border-primary/20">
          <CardHeader>
            <CardTitle className="text-sm">토픽 및 토론방 생성</CardTitle>
            <CardDescription>승인된 토픽을 생성한 뒤 같은 토픽으로 토론방을 생성합니다.</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="grid gap-3 md:grid-cols-2" onSubmit={createTopicAndRoom}>
              <Input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="토픽 제목" required />
              <Input value={category} onChange={(event) => setCategory(event.target.value)} placeholder="카테고리" required />
              <Input value={sourceUrl} onChange={(event) => setSourceUrl(event.target.value)} placeholder="출처 URL (선택)" className="md:col-span-2" />
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="토픽 설명 (선택)"
                rows={3}
                className="resize-none rounded-lg border bg-background px-3 py-2 text-sm md:col-span-2"
              />
              {createError && <p className="text-xs text-destructive md:col-span-2">{createError}</p>}
              {createdRoomId && (
                <p className="flex items-center gap-1.5 text-xs text-emerald-600 md:col-span-2">
                  <CheckCircle2 className="size-3.5" />
                  토론방 #{createdRoomId} 생성 완료 · <Link className="underline" href={`/rooms/${createdRoomId}`}>입장하기</Link>
                </p>
              )}
              <Button type="submit" disabled={creating || !title.trim() || !category.trim()} className="md:col-span-2">
                {creating ? "생성 중..." : "토픽 및 토론방 생성"}
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <div className="flex items-center justify-between gap-3">
              <div>
                <CardTitle className="flex items-center gap-2 text-sm">
                  <Radio className="size-4 text-primary" />
                  토론방 관리
                </CardTitle>
                <CardDescription>현재 서버에 등록된 토론방입니다.</CardDescription>
              </div>
              <Badge variant="outline">{rooms.length}개</Badge>
            </div>
          </CardHeader>
          <CardContent>
            {roomsError && <p className="mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{roomsError}</p>}
            {roomsLoading ? (
              <div className="rounded-lg border border-border px-4 py-8 text-center text-sm text-muted-foreground">
                토론방 목록을 불러오는 중...
              </div>
            ) : rooms.length === 0 ? (
              <div className="rounded-lg border border-border px-4 py-8 text-center text-sm text-muted-foreground">
                등록된 토론방이 없습니다.
              </div>
            ) : (
              <div className="overflow-hidden rounded-lg border border-border/50">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="border-b border-border/50 bg-muted/50">
                      <th className="px-4 py-2.5 text-left font-medium text-muted-foreground">토론방</th>
                      <th className="px-4 py-2.5 text-left font-medium text-muted-foreground">카테고리</th>
                      <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">참여자</th>
                      <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">상태</th>
                      <th className="px-4 py-2.5 text-right font-medium text-muted-foreground">작업</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rooms.map((row) => (
                      <tr key={row.room.roomId} className="border-b border-border/30 transition-colors hover:bg-muted/20">
                        <td className="px-4 py-3">
                          {editingRoomId === row.room.roomId ? (
                            <div className="flex items-center gap-2">
                              <Input
                                value={editingTitle}
                                onChange={(event) => setEditingTitle(event.target.value)}
                                className="h-8 text-xs"
                              />
                              <Button size="sm" className="h-8 text-xs" disabled={mutatingId === row.room.roomId} onClick={() => saveRoomTitle(row.room.roomId)}>
                                저장
                              </Button>
                            </div>
                          ) : (
                            <Link className="font-medium text-foreground hover:text-primary" href={`/rooms/${row.room.roomId}`}>
                              {row.room.title}
                            </Link>
                          )}
                        </td>
                        <td className="px-4 py-3 text-muted-foreground">{row.topic?.category ?? "-"}</td>
                        <td className="px-4 py-3 text-right text-muted-foreground">
                          <span className="inline-flex items-center gap-1">
                            <Users className="size-3" />
                            {row.participantCount.toLocaleString()}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <Badge variant="outline" className={row.room.status === "OPEN" ? "border-emerald-300 text-emerald-700" : "border-border text-muted-foreground"}>
                            {row.room.status === "OPEN" ? "진행 중" : "종료"}
                          </Badge>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-end gap-1">
                            <Button variant="ghost" size="icon" className="size-7" onClick={() => startEdit(row)}>
                              <Edit3 className="size-3.5" />
                              <span className="sr-only">수정</span>
                            </Button>
                            <Button variant="ghost" size="icon" className="size-7 text-destructive" disabled={mutatingId === row.room.roomId} onClick={() => deleteRoom(row.room.roomId)}>
                              <Trash2 className="size-3.5" />
                              <span className="sr-only">방 삭제</span>
                            </Button>
                            <Button variant="ghost" size="sm" className="h-7 text-[11px] text-destructive" disabled={mutatingId === row.room.topicId} onClick={() => deleteTopic(row.room.topicId)}>
                              토픽 삭제
                            </Button>
                          </div>
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
    </div>
  )
}
