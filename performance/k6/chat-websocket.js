import ws from "k6/ws";
import { check } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";
import { authCookieJar } from "./lib/auth.js";

// 실행 전 준비:
// 1. ROOM_ID에 해당하는 OPEN 토론방을 생성한다.
// 2. USER_ID_BASE부터 VUS명 사용자를 생성하고 해당 방에 JOINED 상태로 참여시킨다.
// 3. 생성한 사용자의 token_version을 TOKEN_VERSION과 맞춘다.
// 4. MESSAGE_INTERVAL_SECONDS는 채팅 Rate Limiter 정책보다 짧게 설정하지 않는다.
const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const wsUrl = baseUrl.replace(/^http/, "ws") + "/api/v1/ws";
const frontendOrigin = __ENV.FRONTEND_ORIGIN || "http://localhost:3000";
const roomId = Number(__ENV.ROOM_ID || "1");
const userIdBase = Number(__ENV.USER_ID_BASE || "500000");
const messageIntervalSeconds = Number(__ENV.MESSAGE_INTERVAL_SECONDS || "2");
const connectionDurationSeconds = Number(__ENV.CONNECTION_DURATION_SECONDS || "30");

const websocketConnected = new Counter("chat_ws_connected");
const stompConnected = new Counter("chat_stomp_connected");
const messageSent = new Counter("chat_message_sent");
const messageReceived = new Counter("chat_message_received");
const websocketFailureRate = new Rate("chat_ws_failure_rate");
const stompConnectDuration = new Trend("chat_stomp_connect_duration", true);

export const options = {
    scenarios: {
        chatWebSocket: {
            executor: "constant-vus",
            vus: Number(__ENV.VUS || "10"),
            duration: __ENV.DURATION || "1m",
            gracefulStop: "5s",
        },
    },
    thresholds: {
        chat_ws_failure_rate: ["rate<0.05"],
        chat_stomp_connect_duration: ["p(95)<2000"],
    },
};

export default function () {
    const userId = userIdBase + (__VU - 1);
    const connectedAt = Date.now();
    let stompReady = false;
    let sequence = 0;

    const response = ws.connect(
        wsUrl,
        {
            headers: {
                Origin: frontendOrigin,
            },
            jar: authCookieJar(baseUrl, userId),
            tags: { name: "WS /api/v1/ws" },
        },
        (socket) => {
            socket.on("open", () => {
                websocketConnected.add(1);
                socket.send(stompFrame("CONNECT", {
                    "accept-version": "1.2",
                    "heart-beat": "10000,10000",
                }));
            });

            socket.on("message", (message) => {
                if (message.startsWith("CONNECTED")) {
                    stompReady = true;
                    stompConnected.add(1);
                    stompConnectDuration.add(Date.now() - connectedAt);
                    socket.send(stompFrame("SUBSCRIBE", {
                        id: `chat-${userId}`,
                        destination: `/topic/rooms/${roomId}/chat/events`,
                        ack: "auto",
                    }));
                    return;
                }

                if (message.startsWith("MESSAGE")) {
                    messageReceived.add(1);
                    return;
                }

                if (message.startsWith("ERROR")) {
                    websocketFailureRate.add(true);
                    socket.close();
                }
            });

            socket.on("error", () => {
                websocketFailureRate.add(true);
            });

            socket.setInterval(() => {
                if (!stompReady) {
                    return;
                }

                sequence += 1;
                socket.send(stompFrame(
                    "SEND",
                    {
                        destination: `/app/rooms/${roomId}/chat/messages`,
                        "content-type": "application/json",
                    },
                    JSON.stringify({
                        content: `성능 테스트 채팅 ${userId}-${sequence}`,
                    })
                ));
                messageSent.add(1);
            }, messageIntervalSeconds * 1000);

            socket.setTimeout(() => {
                if (stompReady) {
                    socket.send(stompFrame("DISCONNECT", {
                        receipt: `disconnect-${userId}`,
                    }));
                }
                socket.close();
            }, connectionDurationSeconds * 1000);
        }
    );

    const upgraded = check(response, {
        "websocket upgraded": (result) => result?.status === 101,
    });
    websocketFailureRate.add(!upgraded || !stompReady);
}

function stompFrame(command, headers, body = "") {
    const headerLines = Object.entries(headers)
        .map(([key, value]) => `${key}:${value}`)
        .join("\n");
    return `${command}\n${headerLines}\n\n${body}\u0000`;
}
