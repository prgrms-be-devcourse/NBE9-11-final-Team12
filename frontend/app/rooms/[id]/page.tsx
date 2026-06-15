"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Separator } from "@/components/ui/separator"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog"
import { Navbar } from "@/components/navbar"
import { MainStage } from "@/components/main-stage"
import { ChatPanel } from "@/components/chat-panel"
import { mockTopics } from "@/lib/mock-data"
import { useAuth } from "@/components/auth-provider"
import { roomApi } from "@/lib/api/services"
import { ApiError } from "@/lib/api/client"
import {
  ArrowLeft,
  Users,
  MessageSquare,
  ThumbsUp,
  Share2,
  Flag,
  Clock,
  Zap,
  TrendingUp,
  Eye,
} from "lucide-react"

const topic = mockTopics[0]

const participantAvatars = [
  { id: "p1", initial: "LH" },
  { id: "p2", initial: "MC" },
  { id: "p3", initial: "TS" },
  { id: "p4", initial: "JK" },
  { id: "p5", initial: "PW" },
]

const ROOM_REPORT_REASONS = [
  "불법 / 유해 콘텐츠",
  "스팸 / 반복 도배",
  "허위 정보 확산",
  "특정 집단 혐오",
  "주제 부적절",
  "기타",
]

export default function RoomDetailPage() {
  const params = useParams<{ id: string }>()
  const roomId = Number(params.id)
  const router = useRouter()
  const { user, loading: authLoading } = useAuth()
  const [joinError, setJoinError] = useState("")
  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(topic.likes)

  // Room report modal
  const [roomReportOpen, setRoomReportOpen] = useState(false)
  const [roomReportReason, setRoomReportReason] = useState<string | null>(null)
  const [roomReportSubmitted, setRoomReportSubmitted] = useState(false)

  useEffect(() => {
    if (!Number.isSafeInteger(roomId) || roomId <= 0) {
      router.replace("/rooms")
      return
    }
    if (authLoading || !user) return

    roomApi.join(roomId).catch((error) => {
      if (error instanceof ApiError && error.code === "ROOM_ALREADY_PARTICIPATED") return
      setJoinError(error instanceof ApiError ? error.message : "토론방 입장에 실패했습니다.")
    })
  }, [authLoading, roomId, router, user])

  const handleLike = () => {
    setLiked(!liked)
    setLikeCount((c) => (liked ? c - 1 : c + 1))
  }

  const handleRoomReportClose = () => {
    setRoomReportOpen(false)
    setTimeout(() => {
      setRoomReportReason(null)
      setRoomReportSubmitted(false)
    }, 300)
  }

  return (
    <div className="flex flex-col bg-background" style={{ height: "100dvh" }}>
      <Navbar />

      {/* Room top bar */}
      <div className="shrink-0 border-b border-border/50 bg-background/95 backdrop-blur-md">
        <div className="mx-auto flex max-w-7xl items-center gap-3 px-4 py-3 md:px-6">
          <Link href="/rooms">
            <Button variant="ghost" size="icon" className="size-8 shrink-0">
              <ArrowLeft className="size-4" />
              <span className="sr-only">뒤로</span>
            </Button>
          </Link>

          <div className="flex min-w-0 flex-1 items-center gap-2">
            <Badge className="gap-1.5 shrink-0 bg-primary/20 text-primary border-primary/30 text-[11px]">
              <span className="size-1.5 rounded-full bg-primary animate-live-pulse" />
              LIVE
            </Badge>
            <h1 className="truncate text-sm font-semibold text-foreground">
              {topic.title}
            </h1>
          </div>

          <div className="flex shrink-0 items-center gap-3 text-xs text-muted-foreground">
            <span className="hidden items-center gap-1 sm:flex">
              <Users className="size-3.5" />
              {topic.participants.toLocaleString()}
            </span>
            <span className="hidden items-center gap-1 sm:flex">
              <Eye className="size-3.5" />
              실시간 24
            </span>
            <div className="flex items-center gap-1">
              <Clock className="size-3.5" />
              <span className="font-mono">01:18</span>
            </div>
          </div>

          <div className="flex items-center gap-1">
            <button
              onClick={handleLike}
              className={`flex items-center gap-1.5 rounded-md px-2 py-1.5 text-xs transition-colors ${liked
                ? "text-primary bg-primary/10"
                : "text-muted-foreground hover:text-foreground hover:bg-muted"
                }`}
            >
              <ThumbsUp
                className={`size-4 ${liked ? "fill-primary text-primary" : ""}`}
              />
              <span className="font-medium hidden sm:inline">{likeCount}</span>
            </button>
            <Button variant="ghost" size="icon" className="size-8">
              <Share2 className="size-4 text-muted-foreground" />
              <span className="sr-only">공유</span>
            </Button>
          </div>
        </div>
      </div>

      {/* Body — fills remaining space, scrollable on mobile, fixed on desktop */}
      <div className="min-h-0 flex-1 overflow-y-auto lg:overflow-hidden">
        <div className="mx-auto flex h-full min-h-0 w-full max-w-7xl flex-col px-4 py-4 md:px-6 lg:py-4">
          {joinError && <p className="mb-3 rounded-lg bg-destructive/10 px-3 py-2 text-xs text-destructive">{joinError}</p>}

          {/* 3-column layout on desktop */}
          <div className="flex min-h-0 flex-col gap-6 lg:flex-1 lg:flex-row">
            {/* LEFT: Topic info panel */}
            <aside className="w-full shrink-0 lg:w-64 xl:w-72 lg:overflow-y-auto">
              <div className="flex flex-col gap-4">
                {/* Topic card */}
                <div className="rounded-xl border border-border/50 bg-card p-4">
                  <Badge
                    variant="outline"
                    className="mb-2 border-primary/30 text-primary text-[10px]"
                  >
                    {topic.category}
                  </Badge>
                  <h2 className="mb-2 text-sm font-semibold leading-snug text-foreground">
                    {topic.title}
                  </h2>
                  <p className="mb-3 text-xs leading-relaxed text-muted-foreground">
                    {topic.description}
                  </p>

                  <Separator className="mb-3" />

                  <div className="grid grid-cols-3 gap-2 text-center">
                    {[
                      { label: "참여자", value: `${(topic.participants / 1000).toFixed(1)}k`, icon: Users },
                      { label: "메시지", value: `${(topic.messages / 1000).toFixed(1)}k`, icon: MessageSquare },
                      { label: "공감", value: likeCount.toString(), icon: ThumbsUp },
                    ].map(({ label, value, icon: Icon }) => (
                      <div key={label} className="flex flex-col items-center gap-0.5">
                        <Icon className="size-3.5 text-muted-foreground" />
                        <span className="text-sm font-bold text-foreground">{value}</span>
                        <span className="text-[10px] text-muted-foreground">{label}</span>
                      </div>
                    ))}
                  </div>

                  {topic.tags && topic.tags.length > 0 && (
                    <>
                      <Separator className="my-3" />
                      <div className="flex flex-wrap gap-1">
                        {topic.tags.map((tag) => (
                          <span
                            key={tag}
                            className="rounded-full bg-muted px-2 py-0.5 text-[10px] text-muted-foreground"
                          >
                            #{tag}
                          </span>
                        ))}
                      </div>
                    </>
                  )}
                </div>

                {/* Participants preview */}
                <div className="rounded-xl border border-border/50 bg-card p-4">
                  <div className="mb-3 flex items-center justify-between">
                    <span className="text-xs font-semibold text-foreground">참여자</span>
                    <span className="text-[11px] text-muted-foreground">
                      {topic.participants.toLocaleString()}명
                    </span>
                  </div>
                  <div className="flex items-center gap-1">
                    {participantAvatars.map((p, idx) => (
                      <Avatar
                        key={p.id}
                        className="size-7 border-2 border-background"
                        style={{ marginLeft: idx > 0 ? "-8px" : "0" }}
                      >
                        <AvatarFallback className="bg-muted text-[9px] font-bold text-muted-foreground">
                          {p.initial}
                        </AvatarFallback>
                      </Avatar>
                    ))}
                    <span className="ml-2 text-[11px] text-muted-foreground">
                      +{(topic.participants - 5).toLocaleString()}명
                    </span>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex flex-col gap-2">
                  <Button variant="outline" size="sm" className="w-full gap-2 text-xs">
                    <TrendingUp className="size-3.5" />
                    관련 토픽 보기
                  </Button>
                  {/* Room-level report — distinct from speaker/chat report */}
                  <Button
                    variant="ghost"
                    size="sm"
                    className="w-full gap-2 text-xs text-muted-foreground hover:text-destructive"
                    onClick={() => setRoomReportOpen(true)}
                  >
                    <Flag className="size-3.5" />
                    토론방 신고
                  </Button>
                </div>
              </div>
            </aside>

            {/* CENTER: Main Stage */}
            <div className="min-h-0 flex-1 lg:flex lg:flex-col">
              <div className="min-h-0 rounded-xl border border-border/50 bg-card overflow-hidden lg:flex-1 lg:flex lg:flex-col">                {/* Mobile tab view */}
                <div className="block lg:hidden">
                  <Tabs defaultValue="stage">
                    <TabsList className="w-full rounded-none border-b border-border/50 bg-transparent p-0 h-auto">
                      <TabsTrigger
                        value="stage"
                        className="flex-1 rounded-none border-b-2 border-transparent py-3 text-xs data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary"
                      >
                        <Zap className="size-3.5 mr-1.5" />
                        Main Stage
                      </TabsTrigger>
                      <TabsTrigger
                        value="chat"
                        className="flex-1 rounded-none border-b-2 border-transparent py-3 text-xs data-[state=active]:border-primary data-[state=active]:bg-transparent data-[state=active]:text-primary"
                      >
                        <MessageSquare className="size-3.5 mr-1.5" />
                        채팅
                      </TabsTrigger>
                    </TabsList>
                    <TabsContent value="stage" className="m-0 h-[70vh] min-h-[500px]">
                      <MainStage roomId={roomId} />
                    </TabsContent>
                    <TabsContent value="chat" className="m-0 h-[70vh] min-h-[500px]">
                      <ChatPanel />
                    </TabsContent>
                  </Tabs>
                </div>

                {/* Desktop: Side by side, fills remaining height */}
                <div className="hidden min-h-0 lg:flex lg:flex-1">
                  <div className="min-h-0 flex-1 border-r border-border/50">
                    <MainStage roomId={roomId} />
                  </div>
                  <div className="min-h-0 w-80 xl:w-96">
                    <ChatPanel />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Bottom related rooms — only visible on mobile scroll / desktop overflow area */}
          <section className="mt-8 pb-6 lg:hidden">
            <div className="mb-4 flex items-center gap-2">
              <TrendingUp className="size-4 text-primary" />
              <h2 className="text-sm font-semibold text-foreground">관련 토의방</h2>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              {mockTopics.slice(1, 4).map((t) => (
                <Link
                  key={t.id}
                  href={`/rooms/${t.id}`}
                  className="group flex items-start gap-3 rounded-xl border border-border/50 bg-card p-4 transition-colors hover:border-primary/30"
                >
                  <div className="flex-1 min-w-0">
                    <div className="mb-1 flex items-center gap-1.5">
                      <Badge
                        variant="outline"
                        className="border-border/50 text-[10px] text-muted-foreground"
                      >
                        {t.category}
                      </Badge>
                      {t.isLive && (
                        <span className="size-1.5 rounded-full bg-primary animate-live-pulse" />
                      )}
                    </div>
                    <p className="line-clamp-2 text-xs font-medium text-foreground group-hover:text-primary transition-colors">
                      {t.title}
                    </p>
                    <p className="mt-1 flex items-center gap-1 text-[11px] text-muted-foreground">
                      <Users className="size-3" />
                      {t.participants.toLocaleString()}명
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          </section>

        </div>{/* max-w-7xl inner */}
      </div>{/* body scroll wrapper */}

      {/* Room report modal */}
      <Dialog open={roomReportOpen} onOpenChange={handleRoomReportClose}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-sm">
              <Flag className="size-4 text-destructive" />
              토론방 신고
            </DialogTitle>
            <DialogDescription className="text-xs">
              이 토론방 전체를 신고합니다. 신고 내용은 운영팀이 검토합니다.
            </DialogDescription>
          </DialogHeader>

          {roomReportSubmitted ? (
            <div className="flex flex-col items-center gap-3 py-4">
              <div className="flex size-10 items-center justify-center rounded-full bg-primary/10">
                <Flag className="size-5 text-primary" />
              </div>
              <p className="text-sm font-semibold text-foreground">신고가 접수되었습니다</p>
              <p className="text-xs text-muted-foreground text-center">
                검토 후 커뮤니티 가이드라인에 따라 처리됩니다.
              </p>
              <Button size="sm" className="mt-1 w-full" onClick={handleRoomReportClose}>
                확인
              </Button>
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              <p className="text-xs font-medium text-foreground">신고 사유를 선택하세요</p>
              <div className="flex flex-col gap-1.5">
                {ROOM_REPORT_REASONS.map((reason) => (
                  <button
                    key={reason}
                    onClick={() => setRoomReportReason(reason)}
                    className={`rounded-lg border px-3 py-2.5 text-left text-xs transition-colors ${roomReportReason === reason
                      ? "border-primary/50 bg-primary/10 text-primary font-medium"
                      : "border-border text-foreground hover:border-border/80 hover:bg-muted/60"
                      }`}
                  >
                    {reason}
                  </button>
                ))}
              </div>
              <div className="flex gap-2 pt-1">
                <Button
                  variant="outline"
                  size="sm"
                  className="flex-1 text-xs"
                  onClick={handleRoomReportClose}
                >
                  취소
                </Button>
                <Button
                  size="sm"
                  variant="destructive"
                  className="flex-1 text-xs"
                  disabled={!roomReportReason}
                  onClick={() => setRoomReportSubmitted(true)}
                >
                  신고하기
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  )
}
