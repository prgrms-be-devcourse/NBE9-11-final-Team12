export type ApiResponse<T> = {
  status: number
  code: string
  message: string
  data?: T
}

export type ApiErrorData = Record<string, string> | undefined

export type AuthUser = {
  userId: number
  email: string
  nickname: string
  role?: "USER" | "ADMIN"
  status?: "ACTIVE" | "INACTIVE" | "BANNED"
}

export type TopicSummary = {
  id: number
  title: string
  category: string
  sourceUrl: string | null
  createdAt: string
  approvedAt: string | null
}

export type TopicDetail = TopicSummary & {
  description: string | null
}

export type TopicCreateResponse = {
  topicId: number
  status: "PENDING" | "APPROVED" | "REJECTED"
}

export type RoomCreateResponse = {
  roomId: number
  topicId: number
  title: string
  status: "OPEN" | "CLOSED"
  startedAt: string
  createdAt: string
}

export type SpringPage<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export type SpeechStance = "PRO" | "CON"
export type SpeechStatus = "READY" | "SPEAKING" | "COMPLETED"

export type SpeechSummary = {
  speechId: number
  roomId: number
  userId: number
  content: string
  stance: SpeechStance | null
  status: SpeechStatus
  createdAt: string
}

export type SpeechDetail = SpeechSummary & {
  linkUrl: string | null
  imageUrl: string | null
  startedAt: string | null
  endedAt: string | null
  updatedAt: string
}

export type SpeechCreateResponse = Omit<SpeechSummary, "createdAt">

export type SpeechCursorPage = {
  items: SpeechSummary[]
  nextCursor: number | null
  hasNext: boolean
}

export type SpeechReportReason =
  | "ABUSE_HARASSMENT"
  | "HATE_SPEECH"
  | "SEXUAL_CONTENT"
  | "THREAT_VIOLENCE"
  | "SPAM"
  | "MISINFORMATION"
  | "PRIVACY_VIOLATION"
  | "OFF_TOPIC"
  | "OTHER"
