import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs"
import { API_BASE_URL } from "@/lib/api/client"

export type RealtimeStatus = "connecting" | "connected" | "reconnecting" | "offline" | "disconnected"

type RoomStompConnectionOptions = {
  onStatus?: (connected: boolean) => void
  onRealtimeStatus?: (status: RealtimeStatus) => void
  onError?: (message: string) => void
  onBeforeResubscribe?: () => Promise<boolean> | boolean
}

export type RoomStompConnection = {
  connect: () => void
  disconnect: () => void
  subscribe: <TEvent>(
    destination: string,
    onEvent: (event: TEvent, destination: string) => void,
    onError?: (message: string) => void,
  ) => () => void
  sendChat: (content: string) => boolean
  isConnected: () => boolean
  getStatus: () => RealtimeStatus
}

const WS_ENDPOINT = "/api/v1/ws"
const HEARTBEAT_MS = 10000
const RECONNECT_DELAY_MS = 5000

type SubscriptionEntry = {
  destination: string
  handlers: Map<number, {
    onEvent: (event: unknown, destination: string) => void
    onError?: (message: string) => void
  }>
  subscription: StompSubscription | null
}

function chatSendDestination(roomId: number) {
  return `/app/rooms/${roomId}/chat/messages`
}

function isAuthenticationError(message: string) {
  return ["UNAUTHORIZED", "INVALID_TOKEN", "EXPIRED_TOKEN", "REFRESH_TOKEN_NOT_FOUND", "REFRESH_TOKEN_REUSED"]
    .some((code) => message.includes(code))
}

async function reissueAuthToken() {
  const response = await fetch(`${API_BASE_URL}/api/v1/auth/reissue`, {
    method: "POST",
    credentials: "include",
  })
  return response.ok
}

function wsUrl(path: string) {
  const url = new URL(API_BASE_URL)
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:"
  url.pathname = path
  url.search = ""
  return url.toString()
}

function parseJsonBody<TEvent>(
  message: IMessage,
  onError?: (message: string) => void,
): TEvent | null {
  try {
    return JSON.parse(message.body) as TEvent
  } catch {
    onError?.("Invalid WebSocket event payload.")
    return null
  }
}

export function createRoomStompConnection(
  roomId: number,
  options: RoomStompConnectionOptions = {},
): RoomStompConnection {
  let connected = false
  let status: RealtimeStatus = "disconnected"
  let subscriptionSequence = 0
  let intentionalDisconnect = false
  let authRecoveryTried = false
  let authRecoveryInProgress = false
  const subscriptions = new Map<string, SubscriptionEntry>()

  const setStatus = (nextStatus: RealtimeStatus) => {
    if (status === nextStatus) return
    status = nextStatus
    options.onRealtimeStatus?.(nextStatus)
  }

  const setConnected = (nextConnected: boolean) => {
    if (connected === nextConnected) return
    connected = nextConnected
    options.onStatus?.(nextConnected)
  }

  const safelyUnsubscribe = (entry: SubscriptionEntry) => {
    try {
      entry.subscription?.unsubscribe()
    } catch {
    } finally {
      entry.subscription = null
    }
  }

  const subscribeEntry = (entry: SubscriptionEntry) => {
    safelyUnsubscribe(entry)
    entry.subscription = client.subscribe(
      entry.destination,
      (message) => {
        const event = parseJsonBody(message, options.onError)
        if (event) {
          entry.handlers.forEach((handler) => {
            handler.onEvent(event, message.headers.destination ?? entry.destination)
          })
        }
      },
      {
        ack: "auto",
        id: `room-${roomId}-subscription-${subscriptionSequence++}`,
      },
    )
  }

  const resubscribeAll = () => {
    subscriptions.forEach(subscribeEntry)
  }

  const clearActiveSubscriptions = () => {
    subscriptions.forEach((entry) => {
      entry.subscription = null
    })
  }

  const handleOnline = () => {
    if (!connected) {
      setStatus("reconnecting")
      if (!client.active) client.activate()
    }
  }

  const handleOffline = () => {
    setConnected(false)
    setStatus("offline")
    clearActiveSubscriptions()
  }

  const stopForAuthenticationFailure = () => {
    intentionalDisconnect = true
    setConnected(false)
    setStatus("disconnected")
    options.onError?.("인증이 만료되었습니다. 다시 로그인해주세요.")
    void client.deactivate()
  }

  const recoverAuthentication = async () => {
    if (authRecoveryInProgress) return
    if (authRecoveryTried) {
      stopForAuthenticationFailure()
      return
    }

    authRecoveryTried = true
    authRecoveryInProgress = true
    setConnected(false)
    setStatus("reconnecting")

    try {
      const reissued = await reissueAuthToken()
      if (!reissued || intentionalDisconnect) {
        stopForAuthenticationFailure()
        return
      }

      await client.deactivate()
      if (!intentionalDisconnect) {
        intentionalDisconnect = false
        client.activate()
      }
    } catch {
      stopForAuthenticationFailure()
    } finally {
      authRecoveryInProgress = false
    }
  }

  const completeConnection = async () => {
    try {
      const canSubscribe = await options.onBeforeResubscribe?.()
      if (canSubscribe === false || intentionalDisconnect) {
        setConnected(false)
        setStatus("disconnected")
        return
      }

      authRecoveryTried = false
      setConnected(true)
      setStatus("connected")
      resubscribeAll()
    } catch (error) {
      setConnected(false)
      setStatus("reconnecting")
      options.onError?.(error instanceof Error ? error.message : "실시간 연결 복구에 실패했습니다.")
    }
  }

  const client = new Client({
    webSocketFactory: () => new WebSocket(wsUrl(WS_ENDPOINT)),
    reconnectDelay: RECONNECT_DELAY_MS,
    heartbeatIncoming: HEARTBEAT_MS,
    heartbeatOutgoing: HEARTBEAT_MS,
    onConnect: () => {
      void completeConnection()
    },
    onDisconnect: () => {
      setConnected(false)
      setStatus("disconnected")
      clearActiveSubscriptions()
    },
    onWebSocketClose: () => {
      setConnected(false)
      setStatus(intentionalDisconnect ? "disconnected" : status === "offline" ? "offline" : "reconnecting")
      clearActiveSubscriptions()
    },
    onWebSocketError: () => {
      setConnected(false)
      setStatus(status === "offline" ? "offline" : "reconnecting")
      options.onError?.("실시간 이벤트 서버에 연결하지 못했습니다.")
    },
    onStompError: (frame) => {
      const message = frame.headers.message ?? "실시간 이벤트 연결 오류가 발생했습니다."
      if (isAuthenticationError(message)) {
        void recoverAuthentication()
        return
      }

      setConnected(false)
      setStatus("reconnecting")
      options.onError?.(message)
    },
  })

  return {
    connect() {
      if (client.active) return
      intentionalDisconnect = false
      setStatus(typeof navigator !== "undefined" && !navigator.onLine ? "offline" : "connecting")
      if (typeof window !== "undefined") {
        window.addEventListener("online", handleOnline)
        window.addEventListener("offline", handleOffline)
      }
      client.activate()
    },
    disconnect() {
      intentionalDisconnect = true
      authRecoveryInProgress = false
      setConnected(false)
      setStatus("disconnected")
      if (typeof window !== "undefined") {
        window.removeEventListener("online", handleOnline)
        window.removeEventListener("offline", handleOffline)
      }
      subscriptions.forEach(safelyUnsubscribe)
      subscriptions.clear()
      void client.deactivate()
    },
    subscribe<TEvent>(
      destination: string,
      onEvent: (event: TEvent, destination: string) => void,
      onError = options.onError,
    ) {
      let entry = subscriptions.get(destination)
      if (!entry) {
        entry = {
          destination,
          handlers: new Map(),
          subscription: null,
        }
        subscriptions.set(destination, entry as SubscriptionEntry)
      }

      const handlerId = subscriptionSequence++
      entry.handlers.set(handlerId, {
        onEvent: (event, eventDestination) => onEvent(event as TEvent, eventDestination),
        onError,
      })

      if (connected && !entry.subscription) subscribeEntry(entry)

      return () => {
        const current = subscriptions.get(destination)
        if (!current) return
        current.handlers.delete(handlerId)
        if (current.handlers.size > 0) return
        safelyUnsubscribe(current)
        subscriptions.delete(destination)
      }
    },
    sendChat(content: string) {
      if (!connected) return false
      client.publish({
        destination: chatSendDestination(roomId),
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ content }),
      })
      return true
    },
    isConnected() {
      return connected
    },
    getStatus() {
      return status
    },
  }
}
