"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { useState } from "react"
import { useTheme } from "next-themes"
import { LogIn, Menu, MessageSquare, Moon, Shield, Sun, User, X, Zap } from "lucide-react"
import { useAuth } from "@/components/auth-provider"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

const navItems = [
  { href: "/", label: "홈", icon: Zap },
  { href: "/rooms", label: "토론방", icon: MessageSquare },
  { href: "/admin", label: "관리", icon: Shield },
]

export function Navbar() {
  const pathname = usePathname()
  const [mobileOpen, setMobileOpen] = useState(false)
  const { theme, setTheme } = useTheme()
  const { user, loading, logout } = useAuth()

  const toggleTheme = () => setTheme(theme === "dark" ? "light" : "dark")

  return (
    <header className="sticky top-0 z-50 w-full border-b border-border bg-background/90 backdrop-blur-md">
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between px-4 md:px-6">
        <Link href="/" className="flex items-center gap-2">
          <div className="flex size-7 items-center justify-center rounded-lg bg-primary">
            <Zap className="size-3.5 text-primary-foreground" />
          </div>
          <span className="text-[15px] font-bold tracking-tight text-foreground">시시비비</span>
          <Badge variant="outline" className="hidden border-primary/30 text-primary text-[10px] font-semibold sm:flex">
            ARENA TALK
          </Badge>
        </Link>

        <nav className="hidden items-center gap-0.5 md:flex">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href)
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                  isActive ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-muted hover:text-foreground",
                )}
              >
                <Icon className="size-3.5" />
                {item.label}
              </Link>
            )
          })}
        </nav>

        <div className="flex items-center gap-1.5">
          <Button variant="ghost" size="icon" className="size-8 text-muted-foreground" onClick={toggleTheme} aria-label="테마 전환">
            <Sun className="size-4 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
            <Moon className="absolute size-4 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
          </Button>

          {!loading && user ? (
            <>
              <span className="hidden text-xs font-medium text-foreground sm:inline">{user.nickname}</span>
              <Button variant="ghost" size="sm" className="hidden h-8 text-sm text-muted-foreground sm:flex" onClick={() => void logout()}>
                로그아웃
              </Button>
            </>
          ) : !loading ? (
            <>
              <Link href="/login" className="hidden sm:flex">
                <Button variant="ghost" size="sm" className="h-8 gap-1.5 text-sm text-muted-foreground hover:text-foreground">
                  <LogIn className="size-3.5" />
                  로그인
                </Button>
              </Link>
              <Link href="/signup" className="hidden sm:flex">
                <Button size="sm" className="h-8 gap-1.5 text-sm">
                  <User className="size-3.5" />
                  회원가입
                </Button>
              </Link>
            </>
          ) : null}

          <Button variant="ghost" size="icon" className="size-8 md:hidden" onClick={() => setMobileOpen((open) => !open)} aria-label="메뉴 열기">
            {mobileOpen ? <X className="size-4" /> : <Menu className="size-4" />}
          </Button>
        </div>
      </div>

      {mobileOpen && (
        <div className="border-t border-border bg-background px-4 py-3 md:hidden">
          <nav className="flex flex-col gap-0.5">
            {navItems.map((item) => {
              const Icon = item.icon
              const isActive = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href)
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  onClick={() => setMobileOpen(false)}
                  className={cn(
                    "flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium",
                    isActive ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-muted hover:text-foreground",
                  )}
                >
                  <Icon className="size-3.5" />
                  {item.label}
                </Link>
              )
            })}
            {!user && (
              <div className="mt-2 flex gap-2 border-t border-border pt-2">
                <Link href="/login" className="flex-1" onClick={() => setMobileOpen(false)}>
                  <Button variant="outline" size="sm" className="w-full gap-1.5 text-xs">
                    <LogIn className="size-3.5" />
                    로그인
                  </Button>
                </Link>
                <Link href="/signup" className="flex-1" onClick={() => setMobileOpen(false)}>
                  <Button size="sm" className="w-full gap-1.5 text-xs">
                    <User className="size-3.5" />
                    회원가입
                  </Button>
                </Link>
              </div>
            )}
          </nav>
        </div>
      )}
    </header>
  )
}
