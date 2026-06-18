import { api } from "@/lib/api/client"
import type {
  AuthUser,
  ChatMessageCursorPage,
  RoomCreateResponse,
  RoomDetail,
  RoomParticipant,
  RoomParticipantCount,
  RoomSummary,
  SpeechCursorPage,
  SpeechCreateResponse,
  SpeechDetail,
  SpeechReportReason,
  SpeechStance,
  SpringPage,
  StageCurrentSpeaker,
  StageQueue,
  StageRequest,
  StageRequestStatus,
  TopicCreateResponse,
  TopicDetail,
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
  candidates: () => api.get<unknown[]>("/api/v1/topics/issues/candidates"),
}

export const roomApi = {
  list: () => api.get<RoomSummary[]>("/api/v1/rooms"),
  open: () => api.get<RoomSummary[]>("/api/v1/rooms/open"),
  detail: (roomId: number) => api.get<RoomDetail>(`/api/v1/rooms/${roomId}`),
  join: (roomId: number) => api.post<RoomParticipant>(`/api/v1/rooms/${roomId}/participants`),
  leave: (roomId: number) => api.post<void>(`/api/v1/rooms/${roomId}/participants/out`),
  participants: (roomId: number) => api.get<RoomParticipant[]>(`/api/v1/rooms/${roomId}/participants`),
  participantCount: (roomId: number) =>
    api.get<RoomParticipantCount>(`/api/v1/rooms/${roomId}/participants/count`),
}

export const adminApi = {
  createTopic: (body: { title: string; description?: string; category: string; sourceUrl?: string }) =>
    api.post<TopicCreateResponse>("/api/v1/admin/topics", body),
  createRoom: (topicId: number) => api.post<RoomCreateResponse>("/api/v1/admin/rooms", { topicId }),
  updateRoom: (roomId: number, body: { title?: string; status?: "OPEN" | "CLOSED" }) =>
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
  update: (speechId: number, body: { content: string; stance: SpeechStance }) =>
    api.patch<SpeechDetail>(`/api/v1/speeches/${speechId}`, body),
  remove: (speechId: number) => api.delete<void>(`/api/v1/speeches/${speechId}`),
  updateLink: (speechId: number, linkUrl: string) =>
    api.patch<SpeechDetail>(`/api/v1/speeches/${speechId}/link`, { linkUrl }),
  report: (speechId: number, reason: SpeechReportReason, description?: string) =>
    api.post(`/api/v1/speeches/${speechId}/reports`, { reason, description: description || null }),
}

export const stageApi = {
  current: (roomId: number) => api.get<StageCurrentSpeaker>(`/api/v1/rooms/${roomId}/stage`),
  queue: (roomId: number, offset = 0, size = 10) =>
    api.get<StageQueue>(`/api/v1/rooms/${roomId}/stage/queue?offset=${offset}&size=${size}`),
  request: (roomId: number) => api.post<StageRequest>(`/api/v1/rooms/${roomId}/stage/requests`),
  cancelRequest: (roomId: number) => api.delete<void>(`/api/v1/rooms/${roomId}/stage/requests/me`),
  myRequest: (roomId: number) => api.get<StageRequestStatus>(`/api/v1/rooms/${roomId}/stage/requests/me`),
  complete: (roomId: number) => api.post<void>(`/api/v1/rooms/${roomId}/stage/complete`),
}

export const chatApi = {
  list: (roomId: number, cursor?: number, limit = 50) =>
    api.get<ChatMessageCursorPage>(
      `/api/v1/rooms/${roomId}/chat/messages?limit=${limit}${cursor ? `&cursor=${cursor}` : ""}`,
    ),
  remove: (roomId: number, messageId: number) =>
    api.delete<void>(`/api/v1/rooms/${roomId}/chat/messages/${messageId}`),
}
