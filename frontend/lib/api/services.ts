import { api } from "@/lib/api/client"
import type {
  AuthUser,
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
  SpeechReportCreateResponse,
  SpeechReportReason,
  SpeechStance,
  SpringPage,
  StageCurrentSpeaker,
  StageQueue,
  StageRequest,
  StageRequestStatus,
  TopicDetail,
  TopicCreateResponse,
  TopicSummary,
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
}

export const speechApi = {
  list: (roomId: number, cursor?: number, size = 20) =>
    api.get<SpeechCursorPage>(
      `/api/v1/rooms/${roomId}/speeches?size=${size}${cursor ? `&cursor=${cursor}` : ""}`,
    ),
  detail: (speechId: number) => api.get<SpeechDetail>(`/api/v1/speeches/${speechId}`),
  create: (roomId: number, body: { content: string; stance: SpeechStance }) =>
    api.post<SpeechCreateResponse>(`/api/v1/rooms/${roomId}/speeches`, body),
  createImageUploadUrl: (speechId: number, body: { contentType: string; fileSize: number }) =>
    api.post<SpeechImageUploadUrl>(`/api/v1/speeches/${speechId}/image-upload-url`, body),
  confirmImage: (speechId: number, imageKey: string) =>
    api.patch<SpeechDetail>(`/api/v1/speeches/${speechId}/image`, { imageKey }),
  update: (speechId: number, body: { content: string; stance: SpeechStance }) =>
    api.patch<SpeechDetail>(`/api/v1/speeches/${speechId}`, body),
  remove: (speechId: number) => api.delete<void>(`/api/v1/speeches/${speechId}`),
  updateLink: (speechId: number, linkUrl: string) =>
    api.patch<SpeechDetail>(`/api/v1/speeches/${speechId}/link`, { linkUrl }),
  report: (speechId: number, reason: SpeechReportReason, description?: string) =>
    api.post<SpeechReportCreateResponse>(`/api/v1/speeches/${speechId}/reports`, { reason, description: description || null }),
}

export const chatApi = {
  messages: (roomId: number, cursor?: number, limit = 50) =>
    api.get<ChatMessageCursorPage>(
      `/api/v1/rooms/${roomId}/chat/messages?limit=${limit}${cursor ? `&cursor=${cursor}` : ""}`,
    ),
  delete: (roomId: number, messageId: number) =>
    api.delete<void>(`/api/v1/rooms/${roomId}/chat/messages/${messageId}`),
}

export const stageApi = {
  current: (roomId: number) => api.get<StageCurrentSpeaker>(`/api/v1/rooms/${roomId}/stage`),
  queueSummary: (roomId: number) =>
    api.get<StageQueue>(`/api/v1/rooms/${roomId}/stage/queue/summary`),
  queue: (roomId: number, offset = 0, size = 20) =>
    api.get<StageQueue>(`/api/v1/rooms/${roomId}/stage/queue?offset=${offset}&size=${size}`),
  myRequestStatus: (roomId: number) =>
    api.get<StageRequestStatus>(`/api/v1/rooms/${roomId}/stage/requests/me`),
  requestTurn: (roomId: number) =>
    api.post<StageRequest>(`/api/v1/rooms/${roomId}/stage/requests`),
  cancelMyRequest: (roomId: number) =>
    api.delete<void>(`/api/v1/rooms/${roomId}/stage/requests/me`),
  completeTurn: (roomId: number) => api.post<void>(`/api/v1/rooms/${roomId}/stage/complete`),
}
