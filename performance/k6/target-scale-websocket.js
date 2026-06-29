import ws from "k6/ws";
import { check } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";
import { accessToken } from "./lib/auth.js";

// 목표 규모 WebSocket 테스트
// 준비 데이터:
// - performance/sql/seed-performance-data.sql 실행
// - ROOM_IDS 기본값: 900001~900010
// - USER_ID_BASE 기본값: 100000
// - USERS_PER_ROOM 기본값: 100
// - 각 방마다 USER_ID_BASE + roomIndex * USERS_PER_ROOM + offset 사용자가 JOINED 상태여야 한다.
// WebSocket 테스트 후 참가자가 LEFT 될 수 있으므로 재실행 전 seed SQL로 JOINED 상태를 복구한다.
// 실행 예시:
// BASE_URL=http://localhost:8080 ROOM_IDS=900001,900002,900003,900004,900005,900006,900007,900008,900009,900010 \
// USER_ID_BASE=100000 USERS_PER_ROOM=100 VUS=500 MAX_DURATION=2m CONNECTION_DURATION_SECONDS=90 MESSAGE_INTERVAL_SECONDS=10 \
// k6 run performance/k6/target-scale-websocket.js

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const wsUrl = baseUrl.replace(/^http/, "ws") + "/api/v1/ws";
const frontendOrigin = __ENV.FRONTEND_ORIGIN || "http://localhost:3000";
const roomIds = (__ENV.ROOM_IDS || "900001,900002,900003,900004,900005,900006,900007,900008,900009,900010")
    .split(",")
    .map((value) => Number(value.trim()))
    .filter(Boolean);
const userIdBase = Number(__ENV.USER_ID_BASE || "100000");
const usersPerRoom = Number(__ENV.USERS_PER_ROOM || "100");
const messageIntervalSeconds = Number(__ENV.MESSAGE_INTERVAL_SECONDS || "10");
const connectionDurationSeconds = Number(__ENV.CONNECTION_DURATION_SECONDS || "60");

const websocketConnected = new Counter("target_ws_connected");
const stompConnected = new Counter("target_stomp_connected");
const subscriptions = new Counter("target_ws_subscribed");
const messageSent = new Counter("target_ws_message_sent");
const messageReceived = new Counter("target_ws_message_received");
const failureRate = new Rate("target_ws_failure_rate");
const connectDuration = new Trend("target_ws_connect_duration", true);

export const options = {
    scenarios: {
        targetWebSocket: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.VUS || "100"),
            iterations: 1,
            maxDuration: __ENV.MAX_DURATION || "3m",
            gracefulStop: "10s",
        },
    },
    thresholds: {
        target_ws_failure_rate: ["rate<0.05"],
        target_ws_connect_duration: ["p(95)<2000"],
    },
};

export default function () {
    const vuIndex = __VU - 1;
    const roomIndex = vuIndex % roomIds.length;
    const userOffset = Math.floor(vuIndex / roomIds.length) % usersPerRoom;
    const roomId = roomIds[roomIndex];
    const userId = userIdBase + roomIndex * usersPerRoom + userOffset;
    const connectedAt = Date.now();
    let stompReady = false;
    let subscribed = false;
    let sent = false;
    let received = false;

    const response = ws.connect(
        wsUrl,
        {
            headers: {
                Cookie: `accessToken=${accessToken(userId)}`,
                Origin: frontendOrigin,
            },
            tags: { name: "WS /api/v1/ws", roomId: String(roomId) },
        },
        (socket) => {
            socket.on("open", () => {
                websocketConnected.add(1, { roomId: String(roomId) });
                socket.send(stompFrame("CONNECT", {
                    "accept-version": "1.2",
                    "heart-beat": "10000,10000",
                }));
            });

            socket.on("message", (message) => {
                if (message.startsWith("CONNECTED")) {
                    stompReady = true;
                    stompConnected.add(1, { roomId: String(roomId) });
                    connectDuration.add(Date.now() - connectedAt, { roomId: String(roomId) });
                    socket.send(stompFrame("SUBSCRIBE", {
                        id: `chat-${userId}`,
                        destination: `/topic/rooms/${roomId}/chat/events`,
                        ack: "auto",
                    }));
                    subscriptions.add(1, { roomId: String(roomId) });
                    subscribed = true;
                    return;
                }

                if (message.startsWith("MESSAGE")) {
                    messageReceived.add(1, { roomId: String(roomId) });
                    received = true;
                    return;
                }

                if (message.startsWith("ERROR")) {
                    failureRate.add(true, { roomId: String(roomId) });
                    socket.close();
                }
            });

            socket.on("error", () => {
                failureRate.add(true, { roomId: String(roomId) });
            });

            socket.setInterval(() => {
                if (!stompReady) {
                    return;
                }

                socket.send(stompFrame(
                    "SEND",
                    {
                        destination: `/app/rooms/${roomId}/chat/messages`,
                        "content-type": "application/json",
                    },
                    JSON.stringify({
                        content: `목표 규모 채팅 ${roomId}-${userId}-${Date.now()}`,
                    })
                ));
                messageSent.add(1, { roomId: String(roomId) });
                sent = true;
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
    failureRate.add(!upgraded || !stompReady || !subscribed, { roomId: String(roomId) });
    check({ sent, received }, {
        "chat sent after connect": (result) => result.sent,
        "chat broadcast received": (result) => result.received,
    });
}

function stompFrame(command, headers, body = "") {
    const headerLines = Object.entries(headers)
        .map(([key, value]) => `${key}:${value}`)
        .join("\n");
    return `${command}\n${headerLines}\n\n${body}\u0000`;
}
