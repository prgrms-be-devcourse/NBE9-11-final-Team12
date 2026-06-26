import { api } from "@/lib/api/client"
import type {
  ActiveUserSanction,
  AuthUser,
  BestSpeech,
  ChatReportCreateResponse,
  ChatReportReason,
  ChatMessageCursorPage,
  ClassifiedIssueCandidate,
  RoomDetail,
  RoomParticipant,
  RoomParticipantCount,
  RoomCreateResponse,
  RoomSummary,
  RoomTitlePreview,
  SpeechCursorPage,
  SpeechCreateResponse,
  SpeechDetail,
  SpeechImageUploadUrl,
  SpeechReactionCreateResponse,
  SpeechReportCreateResponse,
  SpeechReportReason,
  SpeechReportDetail,
  SpeechReportReviewAction,
  SpeechReportReviewResponse,
  SpeechReportStatus,
  SpeechReportSummary,
  SpeechStance,
  SpringPage,
  StageCurrentSpeaker,
  StageQueue,
  StageRequest,
  StageRequestStatus,
  TopicDetail,
  TopicCreateResponse,
  TopicSummary,
  UserTrustDetail,
  UserTrustSummary,
  UserSanction,
  UserSanctionRecommendation,
  UserSanctionType,
  ViolationSeverity,
} from "@/lib/api/types"

export const authApi = {
  signup: (body: { email: string; password: string; nickname: string }) =>
    api.post<AuthUser>("/api/v1/auth/signup", body),
  login: (body: { email: string; password: string }) =>
    api.post<AuthUser>("/api/v1/auth/login", body),
  logout: () => api.post<void>("/api/v1/auth/logout"),
  me: () => api.get<AuthUser>("/api/v1/users/me"),
  updateMe: (nickname: string) => api.patch<AuthUser>("/api/v1/users/me", { nickname }),
}

export const topicApi = {
  list: (page = 0, size = 20) =>
    api.get<SpringPage<TopicSummary>>(`/api/v1/topics/issues?page=${page}&size=${size}`),
  detail: (topicId: number) => api.get<TopicDetail>(`/api/v1/topics/issues/${topicId}`),
}

export const roomApi = {
  list: () => api.get<RoomSummary[]>("/api/v1/rooms"),
  open: () => api.get<RoomSummary[]>("/api/v1/rooms/open"),
  detail: (roomId: number) => api.get<RoomDetail>(`/api/v1/rooms/${roomId}`),
  join: (roomId: number) => api.post(`/api/v1/rooms/${roomId}/participants`),
  leave: (roomId: number) => api.post<void>(`/api/v1/rooms/${roomId}/participants/out`),
  participants: (roomId: number) =>
    api.get<RoomParticipant[]>(`/api/v1/rooms/${roomId}/participants`),
  participantCount: (roomId: number) =>
    api.get<RoomParticipantCount>(`/api/v1/rooms/${roomId}/participants/count`),
}

export const adminApi = {
  createTopic: (body: { title: string; description?: string; category: string; sourceUrl?: string }) =>
    api.post<TopicCreateResponse>("/api/v1/admin/topics", body),
  classifiedCandidates: () =>
    api.get<ClassifiedIssueCandidate[]>("/api/v1/admin/topics/candidates/classified"),
  refreshClassifiedCandidates: () =>
    api.post<ClassifiedIssueCandidate[]>("/api/v1/admin/topics/candidates/classified/refresh"),
  updateTopic: (
    topicId: number,
    body: { title: string; description?: string; category: string; sourceUrl?: string },
  ) => api.patch<TopicDetail>(`/api/v1/admin/topics/${topicId}`, body),
  deleteTopic: (topicId: number) => api.delete<void>(`/api/v1/admin/topics/${topicId}`),
  previewRoomTitle: (topicId: number) =>
    api.post<RoomTitlePreview>("/api/v1/admin/rooms/title-preview", { topicId }),
  createRoom: (body: { topicId: number; title: string; maxParticipants?: number }) =>
    api.post<RoomCreateResponse>("/api/v1/admin/rooms", body),
  updateRoom: (roomId: number, body: { title?: string; startedAt?: string; endedAt?: string }) =>
    api.patch<RoomDetail>(`/api/v1/admin/rooms/${roomId}`, body),
  deleteRoom: (roomId: number) => api.delete<void>(`/api/v1/admin/rooms/${roomId}`),
  reports: (params: { status?: SpeechReportStatus | ""; reason?: SpeechReportReason | ""; page?: number; size?: number } = {}) => {
    const query = new URLSearchParams()
    if (params.status) query.set("status", params.status)
    if (params.reason) query.set("reason", params.reason)
    query.set("page", String(params.page ?? 0))
    query.set("size", String(params.size ?? 20))
    return api.get<SpringPage<SpeechReportSummary>>(`/api/v1/admin/reports?${query.toString()}`)
  },
  reportDetail: (reportId: number) => api.get<SpeechReportDetail>(`/api/v1/admin/reports/${reportId}`),
  reviewReport: (
    reportId: number,
    body: { action: SpeechReportReviewAction; resolutionNote?: string; severity?: ViolationSeverity },
  ) => api.patch<SpeechReportReviewResponse>(`/api/v1/admin/reports/${reportId}`, body),
  sanctions: (userId: number, page = 0, size = 20) =>
    api.get<SpringPage<UserSanction>>(`/api/v1/admin/users/${userId}/sanctions?page=${page}&size=${size}`),
  sanctionRecommendation: (userId: number, reportId: number) =>
    api.get<UserSanctionRecommendation>(`/api/v1/admin/users/${userId}/sanctions/recommendation?reportId=${reportId}`),
  createSanction: (
    userId: number,
    body: { type: UserSanctionType; reason: string; durationHours?: number | null; reportId?: number | null },
  ) => api.post<UserSanction>(`/api/v1/admin/users/${userId}/sanctions`, body),
}

export const speechApi = {
  list: (roomId: number, cursor?: number, size = 20) =>
    api.get<SpeechCursorPage>(
      `/api/v1/rooms/${roomId}/speeches?size=${size}${cursor ? `&cursor=${cursor}` : ""}`,
    ),
  detail: (speechId: number) => api.get<SpeechDetail>(`/api/v1/speeches/${speechId}`),
  create: (roomId: number, body: { content: string; stance: SpeechStance | null }) =>
    api.post<SpeechCreateResponse>(`/api/v1/rooms/${roomId}/speeches`, body),
  createImageUploadUrl: (speechId: number, body: { contentType: string; fileSize: number }) =>
    api.post<SpeechImageUploadUrl>(`/api/v1/speeches/${speechId}/image-upload-url`, body),
  confirmImage: (speechId: number, imageKey: string) =>
    api.patch<SpeechDetail>(`/api/v1/speeches/${speechId}/image`, { imageKey }),
  update: (speechId: number, body: { content: string; stance: SpeechStance | null }) =>
    api.patch<SpeechDetail>(`/api/v1/speeches/${speechId}`, body),
  remove: (speechId: number) => api.delete<void>(`/api/v1/speeches/${speechId}`),
  updateLink: (speechId: number, linkUrl: string) =>
    api.patch<SpeechDetail>(`/api/v1/speeches/${speechId}/link`, { linkUrl }),
  createReaction: (speechId: number) =>
    api.post<SpeechReactionCreateResponse>(`/api/v1/speeches/${speechId}/reactions`),
  deleteReaction: (speechId: number) =>
    api.delete<void>(`/api/v1/speeches/${speechId}/reactions`),
  best: (roomId: number) => api.get<BestSpeech>(`/api/v1/rooms/${roomId}/best-speech`),
  report: (speechId: number, reason: SpeechReportReason, description?: string) =>
    api.post<SpeechReportCreateResponse>(`/api/v1/speeches/${speechId}/reports`, { reason, description: description || null }),
}

export const trustApi = {
  me: () => api.get<UserTrustDetail>("/api/v1/users/me/trust"),
  user: (userId: number) => api.get<UserTrustSummary>(`/api/v1/users/${userId}/trust`),
}

export const sanctionApi = {
  active: () => api.get<ActiveUserSanction[]>("/api/v1/users/me/sanctions/active"),
}

export const chatApi = {
  messages: (roomId: number, cursor?: number, limit = 50) =>
    api.get<ChatMessageCursorPage>(
      `/api/v1/rooms/${roomId}/chat/messages?limit=${limit}${cursor ? `&cursor=${cursor}` : ""}`,
    ),
  delete: (roomId: number, messageId: number) =>
    api.delete<void>(`/api/v1/rooms/${roomId}/chat/messages/${messageId}`),
  report: (roomId: number, messageId: number, reason: ChatReportReason, description?: string) =>
    api.post<ChatReportCreateResponse>(
      `/api/v1/rooms/${roomId}/chat/messages/${messageId}/reports`,
      { reason, description: description || null },
    ),
}

export const stageApi = {
  current: (roomId: number) => api.get<StageCurrentSpeaker>(`/api/v1/rooms/${roomId}/stage`),
  queueSummary: (roomId: number) =>
    api.get<StageQueue>(`/api/v1/rooms/${roomId}/stage/queue/summary`),
  queue: (roomId: number, offset = 0, size = 20) =>
    api.get<StageQueue>(`/api/v1/rooms/${roomId}/stage/queue?offset=${offset}&size=${size}`),
  myRequestStatus: (roomId: number) =>
    api.get<StageRequestStatus>(`/api/v1/rooms/${roomId}/stage/requests/me`),
  requestTurn: (roomId: number, stance: SpeechStance | null = null) =>
    api.post<StageRequest>(`/api/v1/rooms/${roomId}/stage/requests`, { stance }),
  cancelMyRequest: (roomId: number) =>
    api.delete<void>(`/api/v1/rooms/${roomId}/stage/requests/me`),
  completeTurn: (roomId: number) => api.post<void>(`/api/v1/rooms/${roomId}/stage/complete`),
}
