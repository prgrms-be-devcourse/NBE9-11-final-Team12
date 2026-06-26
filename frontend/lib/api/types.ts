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

export type IssueNews = {
  title: string
  originallink: string
  link: string
  description: string
  pubDate: string
}

export type ClassifiedIssueNews = {
  news: IssueNews
  category: string
  keywords: string[]
}

export type ClassifiedIssueCandidate = {
  keyword: string
  searchVolume: number | null
  increasePercentage: number | null
  news: ClassifiedIssueNews[]
}

export type RoomCreateResponse = {
  roomId: number
  topicId: number
  title: string
  status: "OPEN" | "CLOSED"
  startedAt: string
  createdAt: string
}

export type RoomSummary = RoomCreateResponse

export type RoomDetail = RoomSummary & {
  endedAt: string | null
}

export type RoomTitlePreview = {
  topicId: number
  title: string
}

export type RoomParticipantStatus = "JOINED" | "LEFT"

export type RoomParticipant = {
  roomParticipantId: number
  roomId: number
  userId: number
  status: RoomParticipantStatus
  joinedAt: string
}

export type RoomParticipantCount = {
  roomId: number
  participantCount: number
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
  imageUrl: string | null
  reactionCount: number
  reactedByMe: boolean
  createdAt: string
}

export type SpeechDetail = SpeechSummary & {
  linkUrl: string | null
  imageUrl: string | null
  startedAt: string | null
  endedAt: string | null
  updatedAt: string
}

export type SpeechCreateResponse = {
  speechId: number
  roomId: number
  userId: number
  content: string
  stance: SpeechStance | null
  status: SpeechStatus
}

export type SpeechImageUploadUrl = {
  uploadUrl: string
  imageUrl: string
  imageKey: string
  expiresAt: string
}

export type SpeechReactionCreateResponse = {
  speechId: number
  reactionCount: number
  reactedByMe: boolean
}

export type BestSpeech = {
  speechId: number
  roomId: number
  userId: number
  content: string
  stance: SpeechStance | null
  status: SpeechStatus
  createdAt: string
  reactionCount: number
}

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

export type SpeechReportCreateResponse = {
  reportId: number
  speechId: number
  reason: SpeechReportReason
  status: "PENDING" | "REVIEWING" | "RESOLVED" | "REJECTED"
  createdAt: string
}

export type ChatReportReason = SpeechReportReason

export type ChatReportCreateResponse = {
  reportId: number
  roomId: number
  messageId: number
  reason: ChatReportReason
  status: "PENDING" | "REVIEWING" | "RESOLVED" | "REJECTED"
  createdAt: string
}

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

export type ChatEventType = "MESSAGE_CREATED" | "MESSAGE_DELETED"

export type ChatMessageEventPayload = {
  type: ChatEventType
  messageId: number
  roomId: number
  userId: number
  nicknameSnapshot: string
  content: string | null
  createdAt: string
  deletedAt: string | null
}

export type ChatEvent = WebSocketEventEnvelope<ChatMessageEventPayload, ChatEventType>

export type SpeakingQueueStatus = "WAITING" | "ASSIGNED" | "CANCELED" | "COMPLETED"

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
  status: SpeakingQueueStatus
  roomId: number
  userId: number
  stance: SpeechStance | null
  queueOrder: number
  requestedAt: string
}

export type StageRequestStatus = {
  hasRequest: boolean
  status: SpeakingQueueStatus | null
  roomId: number | null
  userId: number | null
  stance: SpeechStance | null
  queueOrder: number | null
  currentRank: number | null
  cancelable: boolean
  requestedAt: string | null
  assignedAt: string | null
  expiresAt: string | null
}

export type WebSocketEventEnvelope<TData, TEventType extends string = string> = {
  eventId: string
  eventType: TEventType
  roomId: number | null
  data: TData
  occurredAt: string
}

export type RoomParticipantEvent = WebSocketEventEnvelope<
  {
    roomId: number
    userId: number
    participantCount: number
    occurredAt: string
  },
  "PARTICIPANT_JOINED" | "PARTICIPANT_LEFT"
>

export type StageEvent = WebSocketEventEnvelope<
  {
    roomId: number
    userId: number
    queueOrder: number | null
    status: SpeakingQueueStatus
    assignedAt: string | null
    expiresAt: string | null
    endReason: "COMPLETED" | "EXPIRED" | null
    occurredAt: string
  },
  "SPEAKING_REQUESTED" | "SPEAKING_CANCELED" | "SPEAKER_ASSIGNED" | "SPEAKER_COMPLETED" | "SPEAKER_EXPIRED"
>

export type RoomEvent = WebSocketEventEnvelope<
  {
    roomId: number
    status: "OPEN" | "CLOSED"
    message: string
    closedAt: string
  },
  "ROOM_CLOSED"
>

export type SpeechReactionEvent = WebSocketEventEnvelope<
  {
    roomId: number
    speechId: number
    reactionCount: number
    occurredAt: string
  },
  "SPEECH_REACTION_CHANGED"
>

export type SpeechEvent = WebSocketEventEnvelope<
  {
    roomId: number
    speechId: number
    userId: number
    occurredAt: string
  },
  "SPEECH_CREATED" | "SPEECH_UPDATED" | "SPEECH_DELETED" | "SPEECH_LINK_UPDATED"
>

export type UserSanctionType =
  | "WARNING"
  | "CHAT_RESTRICTION"
  | "SPEECH_RESTRICTION"
  | "STAGE_RESTRICTION"
  | "ACCOUNT_SUSPENSION"

export type UserSanctionState = "SCHEDULED" | "ACTIVE" | "EXPIRED" | "REVOKED"

export type UserSanctionEvent = WebSocketEventEnvelope<
  {
    sanctionId: number
    type: UserSanctionType
    reason: string
    state: UserSanctionState
    startsAt: string
    endsAt: string | null
  },
  "SANCTION_CREATED" | "SANCTION_EXTENDED" | "SANCTION_REVOKED"
>

export type UserTrustLevel = "CAUTION" | "NORMAL" | "RELIABLE" | "TRUSTED"

export type UserActivityLevel = "NEW" | "ACTIVE" | "CONTRIBUTOR" | "LEADER"

export type UserTrustDetail = {
  userId: number
  nickname: string
  score: number
  trustLevel: UserTrustLevel
  activityLevel: UserActivityLevel
  receivedReactionCount: number
  completedSpeechCount: number
  participatedRoomCount: number
  resolvedViolationCount: number
  positiveScore: number
  penaltyScore: number
  policyVersion: string
  calculatedAt: string
}

export type UserTrustSummary = {
  userId: number
  nickname: string
  score: number
  trustLevel: UserTrustLevel
  activityLevel: UserActivityLevel
  policyVersion: string
  calculatedAt: string
}

export type ActiveUserSanction = {
  sanctionId: number
  type: UserSanctionType
  reason: string
  startsAt: string
  endsAt: string | null
}

export type SpeechReportStatus = "PENDING" | "REVIEWING" | "RESOLVED" | "REJECTED"
export type SpeechReportReviewAction = "START_REVIEW" | "RESOLVE" | "REJECT"
export type ViolationSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"

export type SpeechReportSummary = {
  reportId: number
  speechId: number
  reportedUserId: number
  reporterUserId: number
  reason: SpeechReportReason
  status: SpeechReportStatus
  createdAt: string
}

export type SpeechReportDetail = SpeechReportSummary & {
  contentSnapshot: string
  description: string | null
  reviewedBy: number | null
  reviewedAt: string | null
  resolutionNote: string | null
  severity: ViolationSeverity | null
  updatedAt: string
}

export type SpeechReportReviewResponse = {
  reportId: number
  status: SpeechReportStatus
  reviewedBy: number | null
  reviewedAt: string | null
  resolutionNote: string | null
  severity: ViolationSeverity | null
}

export type UserSanction = {
  sanctionId: number
  userId: number
  adminUserId: number
  reportId: number | null
  type: UserSanctionType
  reason: string
  state: UserSanctionState
  startsAt: string
  endsAt: string | null
  revokedAt: string | null
  revokedBy: number | null
  revocationReason: string | null
  extendedAt: string | null
  extendedBy: number | null
  extensionReason: string | null
  createdAt: string
}

export type UserSanctionRecommendation = {
  reportId: number
  userId: number
  currentSeverity: ViolationSeverity | null
  lookbackDays: number
  resolvedViolationCount: number
  lowCount: number
  mediumCount: number
  highCount: number
  criticalCount: number
  weightedScore: number
  recommendedType: UserSanctionType
  recommendedDurationHours: number | null
  accountSuspensionReviewRecommended: boolean
  activeSameTypeSanction: boolean
  activeSameTypeSanctionId: number | null
  activeSameTypeEndsAt: string | null
  recommendationReason: string
}
