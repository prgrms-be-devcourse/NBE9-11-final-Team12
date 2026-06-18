import { Analytics } from "@vercel/analytics/next"
import type { Metadata, Viewport } from "next"
import { ThemeProvider } from "@/components/theme-provider"
import { AuthProvider } from "@/components/auth-provider"
import "./globals.css"

export const metadata: Metadata = {
  title: "시시비비 - 실시간 토론 아레나",
  description: "최신 이슈를 바탕으로 발언권을 신청하고 의견을 나누는 실시간 토론 서비스",
  keywords: ["토론", "실시간", "이슈", "발언", "시시비비"],
  authors: [{ name: "Team Sisibibi" }],
  openGraph: {
    title: "시시비비 - 실시간 토론 아레나",
    description: "지금 가장 뜨거운 이슈를 실시간으로 토론하세요.",
    type: "website",
    locale: "ko_KR",
  },
}

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f8fafc" },
    { media: "(prefers-color-scheme: dark)", color: "#0d1117" },
  ],
  width: "device-width",
  initialScale: 1,
}

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko" className="bg-background" suppressHydrationWarning>
      <body className="bg-background font-sans antialiased">
        <ThemeProvider attribute="class" defaultTheme="light" enableSystem disableTransitionOnChange>
          <AuthProvider>{children}</AuthProvider>
        </ThemeProvider>
        {process.env.NODE_ENV === "production" && <Analytics />}
      </body>
    </html>
  )
}
