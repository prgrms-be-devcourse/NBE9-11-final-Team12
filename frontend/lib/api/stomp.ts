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

function parseFrames(data: string) {
  return data
    .split("\0")
    .map((chunk) => chunk.trim())
    .filter(Boolean)
    .map((chunk) => {
      const [head, ...bodyParts] = chunk.split(/\n\n/)
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

      return { command, headers, body: bodyParts.join("\n\n") }
    })
}

export function subscribeRoomChat(roomId: number, options: ChatSubscriptionOptions): ChatSubscription {
  const socket = new WebSocket(wsUrl("/api/v1/ws"))
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
          destination: `/topic/rooms/${roomId}/chat/messages`,
          ack: "auto",
        }))
      }

      if (stompFrame.command === "MESSAGE" && stompFrame.body) {
        options.onEvent(JSON.parse(stompFrame.body) as ChatEvent)
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
          destination: `/app/rooms/${roomId}/chat/messages`,
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
  const socket = new WebSocket(wsUrl("/api/v1/ws"))
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
        options.onEvent(JSON.parse(stompFrame.body) as TEvent, stompFrame.headers.destination ?? "")
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
