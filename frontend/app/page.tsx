import Link from "next/link"
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Separator } from "@/components/ui/separator"
import { Navbar } from "@/components/navbar"
import { TopicCard } from "@/components/topic-card"
import { mockTopics, scheduledRooms } from "@/lib/mock-data"
import {
  Zap,
  TrendingUp,
  Users,
  MessageSquare,
  ChevronRight,
  Sparkles,
  Globe,
  Shield,
  ArrowRight,
  CalendarClock,
  Clock,
  BellRing,
  Flame,
} from "lucide-react"

const hotTopics = mockTopics.filter((t) => t.isTrending)
const otherTopics = mockTopics.filter((t) => !t.isTrending)

const stats = [
  { label: "실시간 참여자",  value: "48,291", icon: Users,         color: "text-primary" },
  { label: "진행 중인 토의", value: "127",    icon: MessageSquare, color: "text-emerald-600" },
  { label: "오늘의 메시지",  value: "312K",   icon: Sparkles,      color: "text-amber-500" },
  { label: "누적 토픽",      value: "5,832",  icon: TrendingUp,    color: "text-rose-500" },
]

const categories = [
  "전체", "AI·기술", "경제·금융", "사회·복지",
  "정치·외교", "문화·연예", "스포츠", "환경·과학",
]

function formatOpenLabel(date: Date): string {
  const diff = date.getTime() - Date.now()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  if (hours < 24) return `${hours}시간 후 오픈`
  const days = Math.floor(hours / 24)
  return `${days}일 후 오픈`
}

export default function HomePage() {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      {/* ── Hero ────────────────────────────────────────────── */}
      <section className="relative overflow-hidden border-b border-border">
        <div
          className="pointer-events-none absolute inset-0 opacity-[0.03] dark:opacity-[0.06]"
          style={{
            backgroundImage: `linear-gradient(var(--border) 1px, transparent 1px), linear-gradient(90deg, var(--border) 1px, transparent 1px)`,
            backgroundSize: "40px 40px",
          }}
        />
        <div className="pointer-events-none absolute left-1/2 top-0 -translate-x-1/2 h-[300px] w-[700px] rounded-full bg-primary/6 blur-3xl dark:bg-primary/10" />

        <div className="relative mx-auto max-w-7xl px-4 py-16 md:px-6 md:py-24">
          <div className="flex flex-col items-center gap-12 text-center lg:flex-row lg:text-left lg:gap-20">

            {/* Left copy */}
            <div className="flex flex-1 flex-col gap-5">
              <div className="flex justify-center lg:justify-start">
                <Badge
                  variant="outline"
                  className="gap-1.5 border-primary/25 bg-primary/5 text-primary px-3 py-1 text-xs font-medium"
                >
                  <Sparkles className="size-3" />
                  AI가 실시간으로 이슈를 탐지합니다
                </Badge>
              </div>

              <h1 className="text-balance text-4xl font-bold leading-[1.15] tracking-tight text-foreground md:text-5xl lg:text-[3.5rem]">
                실시간{" "}
                <span className="text-primary">광장형</span>
                <br />
                토의 아레나
              </h1>

              <p className="max-w-lg text-balance text-base leading-relaxed text-muted-foreground md:text-[17px]">
                최신 이슈를 AI가 탐지하고, 발언권을 공유하며 다양한 관점을 나누는
                대규모 라이브 토의 플랫폼
              </p>

              <div className="flex flex-wrap justify-center gap-3 lg:justify-start">
                <Link href="/rooms">
                  <Button size="lg" className="gap-2 font-semibold shadow-sm">
                    <Zap className="size-4" />
                    토의 시작하기
                  </Button>
                </Link>
                <Link href="/signup">
                  <Button variant="outline" size="lg" className="gap-2 font-semibold">
                    무료로 시작하기
                    <ChevronRight className="size-4" />
                  </Button>
                </Link>
              </div>

              <p className="text-xs text-muted-foreground">
                오늘{" "}
                <span className="font-semibold text-foreground">48,291명</span>
                이 토의에 참여하고 있습니다
              </p>
            </div>

            {/* Right: Featured topic card */}
            <div className="w-full max-w-sm shrink-0 lg:max-w-md">
              <div className="rounded-2xl border border-border bg-card p-5 shadow-card">
                <div className="mb-4 flex items-center justify-between">
                  <Badge variant="outline" className="border-primary/30 text-primary text-[11px] font-semibold">
                    AI·기술
                  </Badge>
                  <div className="flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Users className="size-3.5" />
                    <span className="font-medium text-foreground">12.4k</span>
                    <span>참여 중</span>
                  </div>
                </div>

                <h3 className="mb-1 text-balance text-[15px] font-semibold leading-snug text-foreground">
                  AI 생성 뉴스 시대, 신뢰를 어떻게 설계할까?
                </h3>
                <p className="mb-4 text-xs leading-relaxed text-muted-foreground">
                  ChatGPT 등 생성형 AI가 만든 기사가 언론을 장악하고 있습니다. 팩트체크 불가능한 AI 뉴스 시대에 미디어 신뢰를 어떻게 회복할 수 있을까요?
                </p>

                <Separator className="mb-4" />

                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <MessageSquare className="size-3.5" />
                      <span className="font-medium text-foreground">8.4k</span>
                    </span>
                    <span className="flex items-center gap-1">
                      <Flame className="size-3.5 text-amber-500" />
                      <span className="font-medium text-foreground">HOT</span>
                    </span>
                  </div>
                  <Link href="/rooms/1">
                    <Button size="sm" className="gap-1.5 text-xs font-semibold shadow-sm">
                      입장하기
                      <ArrowRight className="size-3.5" />
                    </Button>
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Stats bar ───────────────────────────────────────── */}
      <section className="border-b border-border bg-card">
        <div className="mx-auto max-w-7xl px-4 py-4 md:px-6">
          <div className="grid grid-cols-2 gap-6 md:grid-cols-4">
            {stats.map((stat) => {
              const Icon = stat.icon
              return (
                <div key={stat.label} className="flex items-center gap-3">
                  <div className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-muted">
                    <Icon className={`size-4 ${stat.color}`} />
                  </div>
                  <div>
                    <p className="text-[15px] font-bold leading-tight text-foreground">{stat.value}</p>
                    <p className="text-[11px] text-muted-foreground">{stat.label}</p>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </section>

      {/* ── Main content ─────────────────────────────────────── */}
      <main className="mx-auto max-w-7xl px-4 py-10 md:px-6">

        {/* Feature highlight */}
        <div className="mb-8 rounded-xl border border-border bg-card p-5 md:p-6">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="flex-1">
              <div className="mb-2 flex items-center gap-2">
                <div className="flex size-6 items-center justify-center rounded-md bg-primary/10">
                  <Sparkles className="size-3.5 text-primary" />
                </div>
                <span className="text-xs font-semibold uppercase tracking-wider text-primary">AI Powered</span>
              </div>
              <h2 className="mb-1.5 text-[17px] font-bold text-foreground">
                지금 가장 뜨거운 이슈를 실시간으로 토의하세요
              </h2>
              <p className="text-sm leading-relaxed text-muted-foreground">
                외부 API 기반으로 최신 트렌드를 수집하고, 관리자 승인을 통해 토론방이 오픈됩니다.
                오픈 전 알림을 신청해 중요한 토의를 놓치지 마세요.
              </p>
            </div>
            <div className="flex shrink-0 flex-wrap gap-2">
              {[
                { icon: Globe,         label: "실시간 이슈 수집", color: "text-primary",                            bg: "bg-primary/8 dark:bg-primary/15" },
                { icon: Shield,        label: "커뮤니티 보호",    color: "text-violet-600 dark:text-violet-400",    bg: "bg-violet-50 dark:bg-violet-500/10" },
                { icon: Zap,           label: "발언권 시스템",    color: "text-amber-600 dark:text-amber-400",      bg: "bg-amber-50 dark:bg-amber-500/10" },
              ].map(({ icon: Icon, label, color, bg }) => (
                <div
                  key={label}
                  className={`flex items-center gap-1.5 rounded-lg px-3 py-2 text-xs font-medium text-foreground ${bg}`}
                >
                  <Icon className={`size-3.5 ${color}`} />
                  {label}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* ── Upcoming rooms ────────────────────────────────── */}
        {scheduledRooms.length > 0 && (
          <section className="mb-10">
            <div className="mb-4 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CalendarClock className="size-4 text-primary" />
                <h2 className="text-[15px] font-semibold text-foreground">곧 열리는 토론방</h2>
                <Badge variant="outline" className="border-primary/30 text-primary text-[10px]">
                  {scheduledRooms.length}개 예정
                </Badge>
              </div>
              <Link
                href="/rooms"
                className="flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-primary transition-colors"
              >
                전체 일정 <ArrowRight className="size-3.5" />
              </Link>
            </div>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {scheduledRooms.map((room) => (
                <div
                  key={room.id}
                  className="flex flex-col gap-2.5 rounded-xl border border-border bg-card p-4 transition-all duration-200 hover:border-primary/30 hover:shadow-sm"
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-xs font-medium text-muted-foreground">{room.category}</span>
                    <div className="flex shrink-0 items-center gap-1 rounded-md bg-muted px-2 py-0.5 text-[11px] font-semibold text-foreground">
                      <Clock className="size-3 text-muted-foreground" />
                      {formatOpenLabel(room.scheduledAt)}
                    </div>
                  </div>
                  <h3 className="line-clamp-2 text-sm font-semibold leading-snug text-foreground">
                    {room.title}
                  </h3>
                  <div className="flex items-center gap-3 text-[11px] text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <BellRing className="size-3.5" />
                      {(room.notifyCount ?? 0).toLocaleString()}명 알림 신청
                    </span>
                    <span className="flex items-center gap-1">
                      <Users className="size-3.5" />
                      예상 {(room.estimatedParticipants ?? 0).toLocaleString()}명
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Category filter */}
        <div className="mb-6 flex items-center gap-2 overflow-x-auto pb-1">
          {categories.map((cat, idx) => (
            <button
              key={cat}
              className={`flex-shrink-0 rounded-full px-3.5 py-1.5 text-[13px] font-medium transition-colors ${
                idx === 0
                  ? "bg-primary text-primary-foreground shadow-sm"
                  : "bg-muted text-muted-foreground hover:bg-muted/70 hover:text-foreground"
              }`}
            >
              {cat}
            </button>
          ))}
        </div>

        {/* Hot topics */}
        {hotTopics.length > 0 && (
          <section className="mb-10">
            <div className="mb-4 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <TrendingUp className="size-4 text-destructive" />
                <h2 className="text-[15px] font-semibold text-foreground">지금 뜨거운 토픽</h2>
                <Badge variant="outline" className="border-destructive/30 text-destructive text-[10px]">HOT</Badge>
              </div>
              <Link
                href="/rooms"
                className="flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-primary transition-colors"
              >
                전체 보기 <ArrowRight className="size-3.5" />
              </Link>
            </div>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {hotTopics.map((topic) => (
                <TopicCard key={topic.id} topic={topic} />
              ))}
            </div>
          </section>
        )}

        {/* All topics */}
        <section>
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-[15px] font-semibold text-foreground">전체 토픽</h2>
            <Link
              href="/rooms"
              className="flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-primary transition-colors"
            >
              더 보기 <ArrowRight className="size-3.5" />
            </Link>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {otherTopics.map((topic) => (
              <TopicCard key={topic.id} topic={topic} />
            ))}
          </div>
        </section>
      </main>

      {/* ── Footer ──────────────────────────────────────────── */}
      <footer className="mt-16 border-t border-border">
        <div className="mx-auto max-w-7xl px-4 py-8 md:px-6">
          <div className="flex flex-col items-center justify-between gap-4 md:flex-row">
            <div className="flex items-center gap-2">
              <div className="flex size-6 items-center justify-center rounded-md bg-primary">
                <Zap className="size-3.5 text-primary-foreground" />
              </div>
              <span className="text-sm font-bold text-foreground">시시비비</span>
              <span className="text-xs text-muted-foreground">ARENA TALK © 2026</span>
            </div>
            <div className="flex items-center gap-4 text-xs text-muted-foreground">
              <Link href="#" className="hover:text-foreground transition-colors">이용약관</Link>
              <Link href="#" className="hover:text-foreground transition-colors">개인정보처리방침</Link>
              <Link href="#" className="hover:text-foreground transition-colors">문의하기</Link>
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}
