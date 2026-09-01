// performance/k6/target-scale-mixed-churn.js
import http from "k6/http";
import ws from "k6/ws";
import { check, sleep } from "k6";
import exec from "k6/execution";
import { Counter, Rate, Trend } from "k6/metrics";
import { authCookieJar, authParams } from "./lib/auth.js";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const wsUrl = baseUrl.replace(/^http/, "ws") + "/api/v1/ws";
const frontendOrigin = __ENV.FRONTEND_ORIGIN || "http://localhost:3000";

const roomIds = (__ENV.ROOM_IDS || "900001")
    .split(",")
    .map((value) => Number(value.trim()))
    .filter(Boolean);

const userIdBase = Number(__ENV.USER_ID_BASE || "100000");
const usersPerRoom = Number(__ENV.USERS_PER_ROOM || "100");

const speechIdBase = Number(__ENV.SPEECH_ID_BASE || "910001");
const speechCount = Number(__ENV.SPEECH_COUNT || "500");

const duration = __ENV.DURATION || "1m";

const enableRead = (__ENV.ENABLE_READ || "false") !== "false";
const enableReaction = (__ENV.ENABLE_REACTION || "true") !== "false";
const enableParticipantChurn = (__ENV.ENABLE_PARTICIPANT_CHURN || "true") !== "false";
const enableWebSocket = (__ENV.ENABLE_WEBSOCKET || "true") !== "false";

const connectionDurationSeconds = Number(__ENV.CONNECTION_DURATION_SECONDS || "60");
const messageIntervalSeconds = Number(__ENV.MESSAGE_INTERVAL_SECONDS || "3");

const mixedHttpFailureRate = new Rate("mixed_http_failure_rate");
const mixedWsFailureRate = new Rate("mixed_ws_failure_rate");
const mixedWsHandshakeFailureRate = new Rate("mixed_ws_handshake_failure_rate");
const mixedWsConnectFrameFailureRate = new Rate("mixed_ws_connect_frame_failure_rate");
const mixedWsSubscribeFailureRate = new Rate("mixed_ws_subscribe_failure_rate");
const mixedWsSendFailureRate = new Rate("mixed_ws_send_failure_rate");
const mixedWsReceiveFailureRate = new Rate("mixed_ws_receive_failure_rate");
const mixedWsErrorFrameRate = new Rate("mixed_ws_error_frame_rate");

const mixedHttpDuration = new Trend("mixed_http_duration", true);
const mixedWsConnectDuration = new Trend("mixed_ws_connect_duration", true);

const mixedReadRequests = new Counter("mixed_read_requests");
const reactionCreated = new Counter("reaction_created");
const reactionCanceled = new Counter("reaction_canceled");
const participantLeft = new Counter("participant_left");
const participantJoined = new Counter("participant_joined");
const mixedWsConnected = new Counter("mixed_ws_connected");
const mixedWsMessageSent = new Counter("mixed_ws_message_sent");
const mixedWsMessageReceived = new Counter("mixed_ws_message_received");

const scenarios = {};

if (enableRead) {
    scenarios.readApis = {
        executor: "constant-arrival-rate",
        rate: Number(__ENV.READ_RATE || "10"),
        timeUnit: "1s",
        duration,
        preAllocatedVUs: Number(__ENV.READ_PRE_ALLOCATED_VUS || "20"),
        maxVUs: Number(__ENV.READ_MAX_VUS || "100"),
        exec: "readApis",
        tags: { workload: "mixed-read" },
    };
}

if (enableReaction) {
    scenarios.reactionApis = {
        executor: "constant-arrival-rate",
        rate: Number(__ENV.REACTION_RATE || "20"),
        timeUnit: "1s",
        duration,
        preAllocatedVUs: Number(__ENV.REACTION_PRE_ALLOCATED_VUS || "30"),
        maxVUs: Number(__ENV.REACTION_MAX_VUS || "150"),
        exec: "reactionApis",
        tags: { workload: "mixed-reaction" },
    };
}

if (enableParticipantChurn) {
    scenarios.participantChurn = {
        executor: "constant-arrival-rate",
        rate: Number(__ENV.CHURN_RATE || "5"),
        timeUnit: "1s",
        duration,
        preAllocatedVUs: Number(__ENV.CHURN_PRE_ALLOCATED_VUS || "10"),
        maxVUs: Number(__ENV.CHURN_MAX_VUS || "80"),
        exec: "participantChurn",
        tags: { workload: "mixed-participant-churn" },
    };
}

if (enableWebSocket) {
    scenarios.websocketChat = {
        executor: "per-vu-iterations",
        vus: Number(__ENV.WS_VUS || "100"),
        iterations: 1,
        maxDuration: __ENV.WS_MAX_DURATION || "3m",
        gracefulStop: "10s",
        exec: "websocketChat",
        tags: { workload: "mixed-websocket-chat" },
    };
}

export const options = {
    scenarios,
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
    const { roomId, userId } = pickRoomUser(iteration);
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

export function reactionApis() {
    const iteration = scenarioIteration();
    const { userId } = pickRoomUser(iteration);
    const speechId = speechIdBase + (iteration % speechCount);

    const reaction = post(
        `/api/v1/speeches/${speechId}/reactions`,
        userId,
        null,
        "POST /api/v1/speeches/{speechId}/reactions",
        [201, 400, 403, 404, 409]
    );

    if (reaction.status === 201) {
        reactionCreated.add(1);

        const cancel = del(
            `/api/v1/speeches/${speechId}/reactions`,
            userId,
            "DELETE /api/v1/speeches/{speechId}/reactions",
            [200, 204, 400, 403, 404, 409]
        );

        if (cancel.status === 200 || cancel.status === 204) {
            reactionCanceled.add(1);
        }
    }
}

export function participantChurn() {
    const iteration = scenarioIteration();
    const { roomId, userId } = pickRoomUser(iteration);

    const out = post(
        `/api/v1/rooms/${roomId}/participants/out`,
        userId,
        null,
        "POST /api/v1/rooms/{roomId}/participants/out",
        [200, 204, 400, 403, 404, 409]
    );

    if (out.status === 200 || out.status === 204) {
        participantLeft.add(1);
    }

    sleep(Number(__ENV.CHURN_REJOIN_DELAY_SECONDS || "0.2"));

    const join = post(
        `/api/v1/rooms/${roomId}/participants`,
        userId,
        null,
        "POST /api/v1/rooms/{roomId}/participants",
        [200, 201, 400, 403, 404, 409]
    );

    if (join.status === 200 || join.status === 201) {
        participantJoined.add(1);
    }
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
    let errorFrameReceived = false;

    const response = ws.connect(
        wsUrl,
        {
            headers: {
                Origin: frontendOrigin,
            },
            jar: authCookieJar(baseUrl, userId),
            tags: {
                name: "WS /api/v1/ws",
                workload: "mixed-websocket-chat",
                roomId: String(roomId),
            },
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
                    errorFrameReceived = true;
                    mixedWsErrorFrameRate.add(true, { roomId: String(roomId) });
                    socket.close();
                }
            });

            socket.on("error", () => {
                errorFrameReceived = true;
                mixedWsErrorFrameRate.add(true, { roomId: String(roomId) });
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
                        content: `혼합 부하 채팅 room=${roomId}, user=${userId}, ts=${Date.now()}`,
                    })
                ));

                mixedWsMessageSent.add(1, { roomId: String(roomId) });
                sent = true;
            }, messageIntervalSeconds * 1000);

            socket.setTimeout(() => {
                if (stompReady) {
                    socket.send(stompFrame("DISCONNECT", {
                        receipt: `mixed-disconnect-${userId}`,
                    }));
                }

                socket.close();
            }, connectionDurationSeconds * 1000);
        }
    );

    const upgraded = check(response, {
        "mixed websocket upgraded": (result) => result?.status === 101,
    });

    mixedWsHandshakeFailureRate.add(!upgraded, { roomId: String(roomId) });
    mixedWsConnectFrameFailureRate.add(!stompReady, { roomId: String(roomId) });
    mixedWsSubscribeFailureRate.add(!subscribed, { roomId: String(roomId) });
    mixedWsSendFailureRate.add(!sent, { roomId: String(roomId) });
    mixedWsReceiveFailureRate.add(!received, { roomId: String(roomId) });

    if (!errorFrameReceived) {
        mixedWsErrorFrameRate.add(false, { roomId: String(roomId) });
    }

    const ok = upgraded && stompReady && subscribed && sent && received && !errorFrameReceived;
    mixedWsFailureRate.add(!ok, { roomId: String(roomId) });
}

function pickRoomUser(iteration) {
    const roomIndex = iteration % roomIds.length;
    const userOffset = Math.floor(iteration / roomIds.length) % usersPerRoom;

    return {
        roomId: roomIds[roomIndex],
        userId: userIdBase + roomIndex * usersPerRoom + userOffset,
    };
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
    const params = authParams(userId, {
        name,
        workload: "mixed-http",
    });

    params.responseCallback = http.expectedStatuses(...expectedStatuses);

    const response = http.request(
        method,
        `${baseUrl}${path}`,
        body,
        params
    );

    mixedHttpDuration.add(response.timings.duration, { name });

    const ok = check(response, {
        [`${name} expected status`]: (res) => expectedStatuses.includes(res.status),
        [`${name} not 5xx`]: (res) => res.status < 500,
    });

    mixedHttpFailureRate.add(!ok, { name });

    if (method === "GET") {
        mixedReadRequests.add(1, { name });
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