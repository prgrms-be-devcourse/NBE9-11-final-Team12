import { API_BASE_URL } from "@/lib/api/client"
import type { ChatEvent } from "@/lib/api/types"

type ChatSubscription = {
  send: (content: string) => boolean
  disconnect: () => void
}

type RoomEventSubscription = {
  disconnect: () => void
}

type ChatSubscriptionOptions = {
  onEvent: (event: ChatEvent) => void
  onStatus?: (connected: boolean) => void
  onError?: (message: string) => void
}

type RoomEventSubscriptionOptions<TEvent> = {
  destinations: string[]
  onEvent: (event: TEvent, destination: string) => void
  onStatus?: (connected: boolean) => void
  onError?: (message: string) => void
}

type StompFrame = {
  command: string
  headers: Record<string, string>
  body: string
}

const WS_ENDPOINT = "/api/v1/ws"

function chatEventsDestination(roomId: number) {
  return `/topic/rooms/${roomId}/chat/events`
}

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

function frame(command: string, headers: Record<string, string> = {}, body = "") {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`)
  return [command, ...headerLines, "", body].join("\n") + "\0"
}

function parseFrames(data: string): StompFrame[] {
  return data
    .split("\0")
    .filter(Boolean)
    .map((chunk) => {
      const separator = chunk.indexOf("\n\n")
      const head = separator >= 0 ? chunk.slice(0, separator) : chunk
      const body = separator >= 0 ? chunk.slice(separator + 2) : ""
      const [command, ...headerLines] = head.split("\n")
      const headers = Object.fromEntries(
        headerLines
          .map((line) => {
            const separator = line.indexOf(":")
            if (separator < 0) return null
            return [line.slice(0, separator), line.slice(separator + 1)]
          })
          .filter((line): line is [string, string] => Boolean(line)),
      )

      return { command, headers, body }
    })
}

function parseJsonBody<TEvent>(
  stompFrame: StompFrame,
  onError?: (message: string) => void,
): TEvent | null {
  try {
    return JSON.parse(stompFrame.body) as TEvent
  } catch {
    onError?.("Invalid WebSocket event payload.")
    return null
  }
}

export function subscribeRoomChat(roomId: number, options: ChatSubscriptionOptions): ChatSubscription {
  const socket = new WebSocket(wsUrl(WS_ENDPOINT))
  let connected = false

  socket.addEventListener("open", () => {
    socket.send(frame("CONNECT", {
      "accept-version": "1.2",
      "heart-beat": "10000,10000",
    }))
  })

  socket.addEventListener("message", (message) => {
    if (message.data === "\n") return

    for (const stompFrame of parseFrames(String(message.data))) {
      if (stompFrame.command === "CONNECTED") {
        connected = true
        options.onStatus?.(true)
        socket.send(frame("SUBSCRIBE", {
          id: `room-${roomId}-chat`,
          destination: chatEventsDestination(roomId),
          ack: "auto",
        }))
      }

      if (stompFrame.command === "MESSAGE" && stompFrame.body) {
        const event = parseJsonBody<ChatEvent>(stompFrame, options.onError)
        if (event) {
          options.onEvent(event)
        }
      }

      if (stompFrame.command === "ERROR") {
        options.onError?.(stompFrame.headers.message ?? "채팅 연결 오류가 발생했습니다.")
      }
    }
  })

  socket.addEventListener("close", () => {
    connected = false
    options.onStatus?.(false)
  })

  socket.addEventListener("error", () => {
    options.onError?.("채팅 서버에 연결하지 못했습니다.")
  })

  return {
    send(content: string) {
      if (!connected || socket.readyState !== WebSocket.OPEN) return false
      socket.send(frame(
        "SEND",
        {
          destination: chatSendDestination(roomId),
          "content-type": "application/json",
        },
        JSON.stringify({ content }),
      ))
      return true
    },
    disconnect() {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(frame("DISCONNECT"))
      }
      socket.close()
    },
  }
}

export function subscribeRoomEvents<TEvent>(
  roomId: number,
  options: RoomEventSubscriptionOptions<TEvent>,
): RoomEventSubscription {
  const socket = new WebSocket(wsUrl(WS_ENDPOINT))
  let connected = false

  socket.addEventListener("open", () => {
    socket.send(frame("CONNECT", {
      "accept-version": "1.2",
      "heart-beat": "10000,10000",
    }))
  })

  socket.addEventListener("message", (message) => {
    if (message.data === "\n") return

    for (const stompFrame of parseFrames(String(message.data))) {
      if (stompFrame.command === "CONNECTED") {
        connected = true
        options.onStatus?.(true)
        options.destinations.forEach((destination, index) => {
          socket.send(frame("SUBSCRIBE", {
            id: `room-${roomId}-event-${index}`,
            destination,
            ack: "auto",
          }))
        })
      }

      if (stompFrame.command === "MESSAGE" && stompFrame.body) {
        const event = parseJsonBody<TEvent>(stompFrame, options.onError)
        if (event) {
          options.onEvent(event, stompFrame.headers.destination ?? "")
        }
      }

      if (stompFrame.command === "ERROR") {
        options.onError?.(stompFrame.headers.message ?? "실시간 이벤트 연결 오류가 발생했습니다.")
      }
    }
  })

  socket.addEventListener("close", () => {
    connected = false
    options.onStatus?.(false)
  })

  socket.addEventListener("error", () => {
    options.onError?.("실시간 이벤트 서버에 연결하지 못했습니다.")
  })

  return {
    disconnect() {
      if (connected && socket.readyState === WebSocket.OPEN) {
        socket.send(frame("DISCONNECT"))
      }
      socket.close()
    },
  }
}
