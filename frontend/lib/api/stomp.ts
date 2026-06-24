import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs"
import { API_BASE_URL } from "@/lib/api/client"

type RoomStompConnectionOptions = {
  onStatus?: (connected: boolean) => void
  onError?: (message: string) => void
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
}

const WS_ENDPOINT = "/api/v1/ws"
const HEARTBEAT_MS = 10000

function chatSendDestination(roomId: number) {
  return `/app/rooms/${roomId}/chat/messages`
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
  let subscriptionSequence = 0

  const setConnected = (nextConnected: boolean) => {
    if (connected === nextConnected) return
    connected = nextConnected
    options.onStatus?.(nextConnected)
  }

  const client = new Client({
    webSocketFactory: () => new WebSocket(wsUrl(WS_ENDPOINT)),
    reconnectDelay: 0,
    heartbeatIncoming: HEARTBEAT_MS,
    heartbeatOutgoing: HEARTBEAT_MS,
    onConnect: () => setConnected(true),
    onDisconnect: () => setConnected(false),
    onWebSocketClose: () => setConnected(false),
    onWebSocketError: () => {
      options.onError?.("실시간 이벤트 서버에 연결하지 못했습니다.")
    },
    onStompError: (frame) => {
      options.onError?.(frame.headers.message ?? "실시간 이벤트 연결 오류가 발생했습니다.")
    },
  })

  return {
    connect() {
      if (client.active) return
      client.activate()
    },
    disconnect() {
      setConnected(false)
      void client.deactivate()
    },
    subscribe<TEvent>(
      destination: string,
      onEvent: (event: TEvent, destination: string) => void,
      onError = options.onError,
    ) {
      if (!connected) {
        onError?.("실시간 이벤트 연결이 완료된 뒤 다시 시도해주세요.")
        return () => {}
      }

      let subscription: StompSubscription | null = client.subscribe(
        destination,
        (message) => {
          const event = parseJsonBody<TEvent>(message, onError)
          if (event) {
            onEvent(event, message.headers.destination ?? destination)
          }
        },
        {
          ack: "auto",
          id: `room-${roomId}-subscription-${subscriptionSequence++}`,
        },
      )

      return () => {
        subscription?.unsubscribe()
        subscription = null
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
  }
}
