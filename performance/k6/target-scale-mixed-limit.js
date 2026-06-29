import http from "k6/http";
import ws from "k6/ws";
import { check } from "k6";
import exec from "k6/execution";
import { Counter, Rate, Trend } from "k6/metrics";
import { accessToken, authParams } from "./lib/auth.js";

// 실제 병목 확인용 혼합 부하 테스트
// 준비:
// - performance/sql/cleanup-performance-data.sql 실행
// - performance/sql/seed-performance-data.sql 실행
// - 기본 seed 기준: 10개 방(900001~900010), 방당 100명 참여자, 성능 사용자 100000~101199, 의견 910001~910500
// 목적:
// - REST 조회, REST 쓰기, 발언권 신청, WebSocket 채팅을 동시에 발생시켜 자원 경합을 확인한다.
// - 단독 API 성능이 아니라 실제 서비스에 가까운 혼합 부하에서 p95/p99, 실패율, dropped iteration을 본다.

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const wsUrl = baseUrl.replace(/^http/, "ws") + "/api/v1/ws";
const frontendOrigin = __ENV.FRONTEND_ORIGIN || "http://localhost:3000";
const roomIds = (__ENV.ROOM_IDS || "900001,900002,900003,900004,900005,900006,900007,900008,900009,900010")
    .split(",")
    .map((value) => Number(value.trim()))
    .filter(Boolean);
const userIdBase = Number(__ENV.USER_ID_BASE || "100000");
const usersPerRoom = Number(__ENV.USERS_PER_ROOM || "100");
const speechIdBase = Number(__ENV.SPEECH_ID_BASE || "910001");
const speechCount = Number(__ENV.SPEECH_COUNT || "500");
const connectionDurationSeconds = Number(__ENV.CONNECTION_DURATION_SECONDS || "60");
const messageIntervalSeconds = Number(__ENV.MESSAGE_INTERVAL_SECONDS || "5");

const mixedHttpFailureRate = new Rate("mixed_http_failure_rate");
const mixedWsFailureRate = new Rate("mixed_ws_failure_rate");
const mixedHttpDuration = new Trend("mixed_http_duration", true);
const mixedWsConnectDuration = new Trend("mixed_ws_connect_duration", true);
const mixedReadRequests = new Counter("mixed_read_requests");
const mixedWriteRequests = new Counter("mixed_write_requests");
const mixedStageRequests = new Counter("mixed_stage_requests");
const mixedWsConnected = new Counter("mixed_ws_connected");
const mixedWsMessageSent = new Counter("mixed_ws_message_sent");
const mixedWsMessageReceived = new Counter("mixed_ws_message_received");

export const options = {
    scenarios: {
        readApis: {
            executor: "constant-arrival-rate",
            rate: Number(__ENV.READ_RATE || "200"),
            timeUnit: "1s",
            duration: __ENV.DURATION || "2m",
            preAllocatedVUs: Number(__ENV.READ_PRE_ALLOCATED_VUS || "100"),
            maxVUs: Number(__ENV.READ_MAX_VUS || "500"),
            exec: "readApis",
            tags: { workload: "mixed-read" },
        },
        writeApis: {
            executor: "constant-arrival-rate",
            rate: Number(__ENV.WRITE_RATE || "50"),
            timeUnit: "1s",
            duration: __ENV.DURATION || "2m",
            preAllocatedVUs: Number(__ENV.WRITE_PRE_ALLOCATED_VUS || "50"),
            maxVUs: Number(__ENV.WRITE_MAX_VUS || "300"),
            exec: "writeApis",
            tags: { workload: "mixed-write" },
        },
        stageRequests: {
            executor: "constant-arrival-rate",
            rate: Number(__ENV.STAGE_RATE || "100"),
            timeUnit: "1s",
            duration: __ENV.DURATION || "2m",
            preAllocatedVUs: Number(__ENV.STAGE_PRE_ALLOCATED_VUS || "100"),
            maxVUs: Number(__ENV.STAGE_MAX_VUS || "500"),
            exec: "stageRequests",
            tags: { workload: "mixed-stage" },
        },
        websocketChat: {
            executor: "per-vu-iterations",
            vus: Number(__ENV.WS_VUS || "500"),
            iterations: 1,
            maxDuration: __ENV.WS_MAX_DURATION || "3m",
            gracefulStop: "10s",
            exec: "websocketChat",
            tags: { workload: "mixed-websocket" },
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.10"],
        mixed_http_failure_rate: ["rate<0.10"],
        mixed_ws_failure_rate: ["rate<0.10"],
        http_req_duration: ["p(95)<3000", "p(99)<7000"],
        mixed_ws_connect_duration: ["p(95)<5000"],
    },
};

export function readApis() {
    const iteration = scenarioIteration();
    const userId = userIdBase + (iteration % 1000);
    const roomId = roomIds[iteration % roomIds.length];
    const speechId = speechIdBase + (iteration % speechCount);

    get("/api/v1/users/me", userId, "GET /api/v1/users/me", [200]);
    get("/api/v1/rooms/open", userId, "GET /api/v1/rooms/open", [200]);
    get(`/api/v1/rooms/${roomId}`, userId, "GET /api/v1/rooms/{roomId}", [200]);
    get(`/api/v1/rooms/${roomId}/participants/count`, userId, "GET /api/v1/rooms/{roomId}/participants/count", [200]);
    get(`/api/v1/rooms/${roomId}/speeches?size=20`, userId, "GET /api/v1/rooms/{roomId}/speeches", [200]);
    get(`/api/v1/rooms/${roomId}/stage`, userId, "GET /api/v1/rooms/{roomId}/stage", [200]);
    get(`/api/v1/rooms/${roomId}/best-speech`, userId, "GET /api/v1/rooms/{roomId}/best-speech", [200, 404]);
    get(`/api/v1/users/${userId}/trust`, userId, "GET /api/v1/users/{userId}/trust", [200]);
    get(`/api/v1/speeches/${speechId}`, userId, "GET /api/v1/speeches/{speechId}", [200, 404]);
}

export function writeApis() {
    const iteration = scenarioIteration();
    const roomIndex = iteration % roomIds.length;
    const roomId = roomIds[roomIndex];
    const userId = userIdBase + roomIndex * usersPerRoom + (iteration % usersPerRoom);
    const speechId = speechIdBase + (iteration % speechCount);

    const reaction = post(
        `/api/v1/speeches/${speechId}/reactions`,
        userId,
        null,
        "POST /api/v1/speeches/{speechId}/reactions",
        [201, 400, 403, 404, 409]
    );
    if (reaction.status === 201) {
        del(`/api/v1/speeches/${speechId}/reactions`, userId, "DELETE /api/v1/speeches/{speechId}/reactions", [200, 204, 400, 403, 404, 409]);
    }

    post(
        `/api/v1/speeches/${speechId}/reports`,
        userId,
        JSON.stringify({
            reason: "ABUSE_HARASSMENT",
            description: "혼합 부하 테스트 신고 데이터",
        }),
        "POST /api/v1/speeches/{speechId}/reports",
        [201, 400, 403, 404, 409]
    );
}

export function stageRequests() {
    const iteration = scenarioIteration();
    const roomIndex = iteration % roomIds.length;
    const roomId = roomIds[roomIndex];
    const userId = userIdBase + roomIndex * usersPerRoom + (iteration % usersPerRoom);

    post(
        `/api/v1/rooms/${roomId}/stage/requests`,
        userId,
        JSON.stringify({ stance: iteration % 2 === 0 ? "PRO" : "CON" }),
        "POST /api/v1/rooms/{roomId}/stage/requests",
        [201, 400, 403, 404, 409]
    );
}

export function websocketChat() {
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
            tags: { name: "WS /api/v1/ws", workload: "mixed-websocket", roomId: String(roomId) },
        },
        (socket) => {
            socket.on("open", () => {
                mixedWsConnected.add(1, { roomId: String(roomId) });
                socket.send(stompFrame("CONNECT", {
                    "accept-version": "1.2",
                    "heart-beat": "10000,10000",
                }));
            });

            socket.on("message", (message) => {
                if (message.startsWith("CONNECTED")) {
                    stompReady = true;
                    mixedWsConnectDuration.add(Date.now() - connectedAt, { roomId: String(roomId) });
                    socket.send(stompFrame("SUBSCRIBE", {
                        id: `mixed-chat-${userId}`,
                        destination: `/topic/rooms/${roomId}/chat/events`,
                        ack: "auto",
                    }));
                    subscribed = true;
                    return;
                }

                if (message.startsWith("MESSAGE")) {
                    mixedWsMessageReceived.add(1, { roomId: String(roomId) });
                    received = true;
                    return;
                }

                if (message.startsWith("ERROR")) {
                    mixedWsFailureRate.add(true, { roomId: String(roomId) });
                    socket.close();
                }
            });

            socket.on("error", () => {
                mixedWsFailureRate.add(true, { roomId: String(roomId) });
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
                    JSON.stringify({ content: `혼합 부하 채팅 ${roomId}-${userId}-${Date.now()}` })
                ));
                mixedWsMessageSent.add(1, { roomId: String(roomId) });
                sent = true;
            }, messageIntervalSeconds * 1000);

            socket.setTimeout(() => {
                if (stompReady) {
                    socket.send(stompFrame("DISCONNECT", { receipt: `mixed-disconnect-${userId}` }));
                }
                socket.close();
            }, connectionDurationSeconds * 1000);
        }
    );

    const upgraded = check(response, {
        "mixed websocket upgraded": (result) => result?.status === 101,
    });
    const ok = upgraded && stompReady && subscribed && sent && received;
    mixedWsFailureRate.add(!ok, { roomId: String(roomId) });
}

function get(path, userId, name, expectedStatuses) {
    return request("GET", path, userId, null, name, expectedStatuses);
}

function post(path, userId, body, name, expectedStatuses) {
    return request("POST", path, userId, body, name, expectedStatuses);
}

function del(path, userId, name, expectedStatuses) {
    return request("DELETE", path, userId, null, name, expectedStatuses);
}

function request(method, path, userId, body, name, expectedStatuses) {
    const params = authParams(userId, { name, workload: "mixed-http" });
    params.responseCallback = http.expectedStatuses(...expectedStatuses);
    const response = http.request(method, `${baseUrl}${path}`, body, params);
    mixedHttpDuration.add(response.timings.duration, { name });

    const ok = check(response, {
        [`${name} expected status`]: (res) => expectedStatuses.includes(res.status),
        [`${name} not 5xx`]: (res) => res.status < 500,
    });

    mixedHttpFailureRate.add(!ok, { name });
    if (method === "GET") {
        mixedReadRequests.add(1, { name });
    } else if (path.includes("/stage/requests")) {
        mixedStageRequests.add(1, { name });
    } else {
        mixedWriteRequests.add(1, { name });
    }
    return response;
}

function stompFrame(command, headers, body = "") {
    const headerLines = Object.entries(headers)
        .map(([key, value]) => `${key}:${value}`)
        .join("\n");
    return `${command}\n${headerLines}\n\n${body}\u0000`;
}

function scenarioIteration() {
    return exec.scenario.iterationInTest;
}
