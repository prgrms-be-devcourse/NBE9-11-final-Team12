export type ApiResponse<T> = {
  status: number
  code: string
  message: string
  data?: T
}

export type AuthUser = {
  userId: number
  email: string
  nickname: string
  role?: "USER" | "ADMIN"
  status?: "ACTIVE" | "INACTIVE" | "BANNED"
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

export type RoomStatus = "OPEN" | "CLOSED"

export type RoomSummary = {
  roomId: number
  topicId: number
  title: string
  status: RoomStatus
  startedAt: string
  createdAt: string
}

export type RoomDetail = RoomSummary & {
  endedAt: string | null
}

export type RoomCreateResponse = RoomSummary

export type RoomParticipant = {
  roomParticipantId: number
  roomId: number
  userId: number
  status: "ACTIVE" | "LEFT"
  joinedAt: string
}

export type RoomParticipantCount = {
  roomId: number
  participantCount: number
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

export type ChatMessage = {
  messageId: number
  roomId: number
  userId: number
  nicknameSnapshot: string
  content: string
  createdAt: string
}

export type ChatMessageCursorPage = {
  items: ChatMessage[]
  nextCursor: number | null
  hasNext: boolean
}

export type StageCurrentSpeaker = {
  hasCurrentSpeaker: boolean
  currentSpeaker: {
    userId: number
    nickname: string
    queueOrder: number
    assignedAt: string
    expiresAt: string
  } | null
}

export type StageQueue = {
  totalWaitingCount: number
  offset: number
  size: number
  hasNext: boolean
  items: {
    rank: number
    userId: number
    nickname: string
  }[]
}

export type StageRequest = {
  status: "WAITING" | "SPEAKING" | "COMPLETED" | "CANCELED" | "EXPIRED"
  roomId: number
  userId: number
  queueOrder: number
  requestedAt: string
}

export type StageRequestStatus = {
  hasRequest: boolean
  status: StageRequest["status"] | null
  roomId: number | null
  userId: number | null
  queueOrder: number | null
  currentRank: number | null
  cancelable: boolean
  requestedAt: string | null
  assignedAt: string | null
  expiresAt: string | null
}
