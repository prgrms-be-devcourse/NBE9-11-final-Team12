import { api } from "@/lib/api/client"
import type {
  AuthUser,
  RoomCreateResponse,
  SpeechCursorPage,
  SpeechCreateResponse,
  SpeechDetail,
  SpeechReportReason,
  SpeechStance,
  SpringPage,
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
  join: (roomId: number) => api.post(`/api/v1/rooms/${roomId}/participants`),
}

export const adminApi = {
  createTopic: (body: { title: string; description?: string; category: string; sourceUrl?: string }) =>
    api.post<TopicCreateResponse>("/api/v1/admin/topics", body),
  createRoom: (topicId: number) => api.post<RoomCreateResponse>("/api/v1/admin/rooms", { topicId }),
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
