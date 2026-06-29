import { Analytics } from '@vercel/analytics/next'
import type { Metadata, Viewport } from 'next'
import { ThemeProvider } from '@/components/theme-provider'
import { AuthProvider } from '@/components/auth-provider'
import './globals.css'

export const metadata: Metadata = {
  title: '이슈톡 — 실시간 광장형 토론',
  description: '최신 이슈를 AI가 탐지하고, 유저가 발언권을 공유 후 채팅으로 다양한 관점을 나누는 대규모 라이브 토론 플랫폼',
  keywords: ['토론', '실시간', '이슈', '광장', '의견', '이슈톡'],
  authors: [{ name: 'Team 이슈톡' }],
  openGraph: {
    title: '이슈톡 — 실시간 광장형 토론',
    description: '지금 가장 뜨거운 이슈를 실시간으로 토론하세요',
    type: 'website',
    locale: 'ko_KR',
  },
}

export const viewport: Viewport = {
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#f8fafc' },
    { media: '(prefers-color-scheme: dark)',  color: '#0d1117' },
  ],
  width: 'device-width',
  initialScale: 1,
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="ko" className="bg-background" suppressHydrationWarning>
      <body className="font-sans antialiased bg-background">
        <ThemeProvider
          attribute="class"
          defaultTheme="light"
          enableSystem
          disableTransitionOnChange
        >
          <AuthProvider>{children}</AuthProvider>
        </ThemeProvider>
        {process.env.NODE_ENV === 'production' && <Analytics />}
      </body>
    </html>
  )
}
