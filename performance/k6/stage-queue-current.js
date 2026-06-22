import crypto from "k6/crypto";
import encoding from "k6/encoding";
import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";
import { Counter, Rate, Trend } from "k6/metrics";

// 실행 전 준비:
// 1. ROOM_ID_BASE부터 ROOM_COUNT만큼 OPEN 토론방을 생성한다.
// 2. USER_ID_BASE부터 INITIAL_USERS 이상 사용자를 생성한다.
// 3. 생성한 사용자의 token_version을 TOKEN_VERSION과 맞춘다.
// 4. 발언권 신청·순번 조회·종료가 같은 데이터 집합에서 이어지므로 다른 스크립트와 ID 범위를 분리한다.
const baseUrl = __ENV.BASE_URL || "http://127.0.0.1:8080";
const jwtSecret =
    __ENV.JWT_SECRET || "local-development-jwt-secret-key-must-be-at-least-32-bytes";
const mode = __ENV.MODE || "constant";
const roomIdBase = Number(__ENV.ROOM_ID_BASE || "930000");
const roomCount = Number(__ENV.ROOM_COUNT || "10");
const userIdBase = Number(__ENV.USER_ID_BASE || "930000000");
const csrfToken = "perf-csrf-token";

const requestCreated = new Counter("stage_request_created");
const requestFailed = new Counter("stage_request_failed");
const statusOk = new Counter("stage_status_ok");
const statusNotFound = new Counter("stage_status_not_found");
const completeAttempted = new Counter("stage_complete_attempted");
const completeSucceeded = new Counter("stage_complete_succeeded");
const completeSkipped = new Counter("stage_complete_skipped");
const stageFailureRate = new Rate("stage_failure_rate");
const stageRequestDuration = new Trend("stage_request_duration", true);
const stageStatusDuration = new Trend("stage_status_duration", true);
const stageCompleteDuration = new Trend("stage_complete_duration", true);

export const options = buildOptions();

export default function () {
    if (exec.scenario.name === "rankLookup") {
        lookupMyStatus();
        return;
    }

    if (exec.scenario.name === "completeTurns") {
        completeCurrentSpeaker();
        return;
    }

    requestSpeakingTurn();
}

function buildOptions() {
    if (mode === "realistic") {
        return {
            scenarios: {
                initialRequests: {
                    executor: "constant-arrival-rate",
                    rate: Number(__ENV.INITIAL_RATE || "100"),
                    timeUnit: "1s",
                    duration: __ENV.INITIAL_DURATION || "10s",
                    preAllocatedVUs: Number(__ENV.INITIAL_PRE_ALLOCATED_VUS || "120"),
                    maxVUs: Number(__ENV.INITIAL_MAX_VUS || "500"),
                    exec: "default",
                    tags: { workload: "initial-request" },
                },
                rankLookup: {
                    executor: "constant-arrival-rate",
                    startTime: __ENV.RANK_START_TIME || "10s",
                    rate: Number(__ENV.RANK_RATE || "10"),
                    timeUnit: "1s",
                    duration: __ENV.RANK_DURATION || "180s",
                    preAllocatedVUs: Number(__ENV.RANK_PRE_ALLOCATED_VUS || "40"),
                    maxVUs: Number(__ENV.RANK_MAX_VUS || "200"),
                    exec: "default",
                    tags: { workload: "rank-lookup" },
                },
                completeTurns: {
                    executor: "constant-arrival-rate",
                    startTime: __ENV.COMPLETE_START_TIME || "20s",
                    rate: Number(__ENV.COMPLETE_RATE || "1"),
                    timeUnit: __ENV.COMPLETE_TIME_UNIT || "2s",
                    duration: __ENV.COMPLETE_DURATION || "180s",
                    preAllocatedVUs: Number(__ENV.COMPLETE_PRE_ALLOCATED_VUS || "10"),
                    maxVUs: Number(__ENV.COMPLETE_MAX_VUS || "50"),
                    exec: "default",
                    tags: { workload: "complete-turn" },
                },
            },
            thresholds: commonThresholds(),
        };
    }

    return {
        scenarios: {
            requestLoad: {
                executor: "constant-arrival-rate",
                rate: Number(__ENV.RATE || "100"),
                timeUnit: "1s",
                duration: __ENV.DURATION || "30s",
                preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || "250"),
                maxVUs: Number(__ENV.MAX_VUS || "1500"),
                exec: "default",
                tags: { workload: "request-load" },
            },
        },
        thresholds: commonThresholds(),
    };
}

function commonThresholds() {
    return {
        http_req_failed: ["rate<0.05"],
        stage_failure_rate: ["rate<0.05"],
    };
}

function requestSpeakingTurn() {
    const iteration = exec.scenario.iterationInTest;
    const roomId = roomIdBase + (iteration % roomCount);
    const userId = userIdBase + iteration;
    const stance = iteration % 2 === 0 ? "PRO" : "CON";
    const response = post(
        `/api/v1/rooms/${roomId}/stage/requests`,
        userId,
        JSON.stringify({ stance }),
        { name: "POST /api/v1/rooms/{roomId}/stage/requests" }
    );

    stageRequestDuration.add(response.timings.duration);
    const ok = check(response, {
        "stage request returns 201": (res) => res.status === 201,
        "stage request returns SUCCESS": (res) => safeJson(res, "code") === "SUCCESS",
    });

    if (ok) {
        requestCreated.add(1);
        stageFailureRate.add(false);
    } else {
        requestFailed.add(1);
        stageFailureRate.add(true);
    }
}

function lookupMyStatus() {
    const iteration = exec.scenario.iterationInTest;
    const userOffset = iteration % Number(__ENV.INITIAL_USERS || "1000");
    const roomId = roomIdBase + (userOffset % roomCount);
    const userId = userIdBase + userOffset;
    const response = get(
        `/api/v1/rooms/${roomId}/stage/requests/me`,
        userId,
        { name: "GET /api/v1/rooms/{roomId}/stage/requests/me" }
    );

    stageStatusDuration.add(response.timings.duration);
    if (response.status === 200) {
        statusOk.add(1);
        stageFailureRate.add(false);
    } else if (response.status === 404) {
        statusNotFound.add(1);
        stageFailureRate.add(false);
    } else {
        stageFailureRate.add(true);
    }
}

function completeCurrentSpeaker() {
    const iteration = exec.scenario.iterationInTest;
    const roomId = roomIdBase + (iteration % roomCount);
    const inspectorUserId = userIdBase + 9_000_000 + iteration;
    const current = get(
        `/api/v1/rooms/${roomId}/stage`,
        inspectorUserId,
        { name: "GET /api/v1/rooms/{roomId}/stage" }
    );

    if (current.status !== 200 || !safeJson(current, "data.hasCurrentSpeaker")) {
        completeSkipped.add(1);
        stageFailureRate.add(current.status >= 500);
        return;
    }

    const speakerUserId = Number(safeJson(current, "data.currentSpeaker.userId"));
    completeAttempted.add(1);
    const completed = post(
        `/api/v1/rooms/${roomId}/stage/complete`,
        speakerUserId,
        null,
        { name: "POST /api/v1/rooms/{roomId}/stage/complete" }
    );

    stageCompleteDuration.add(completed.timings.duration);
    if (completed.status === 200) {
        completeSucceeded.add(1);
        stageFailureRate.add(false);
    } else if (completed.status === 404 || completed.status === 403) {
        stageFailureRate.add(false);
    } else {
        stageFailureRate.add(true);
    }
}

function get(path, userId, tags) {
    return http.get(`${baseUrl}${path}`, params(userId, tags));
}

function post(path, userId, body, tags) {
    return http.post(`${baseUrl}${path}`, body, params(userId, tags));
}

function params(userId, tags) {
    return {
        headers: {
            "Content-Type": "application/json",
            "X-XSRF-TOKEN": csrfToken,
            Cookie: `accessToken=${accessToken(userId)}; XSRF-TOKEN=${csrfToken}`,
        },
        tags,
    };
}

function accessToken(userId) {
    const now = Math.floor(Date.now() / 1000);
    const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
    const payload = base64Url(JSON.stringify({
        sub: String(userId),
        jti: `${userId}-${now}`,
        email: `perf-${userId}@sisibibi.test`,
        role: "USER",
        tokenType: "ACCESS",
        tokenVersion: Number(__ENV.TOKEN_VERSION || "0"),
        iat: now,
        exp: now + 1800,
    }));
    const unsignedToken = `${header}.${payload}`;
    const signature = crypto.hmac("sha256", jwtSecret, unsignedToken, "base64rawurl");
    return `${unsignedToken}.${signature}`;
}

function base64Url(value) {
    return encoding.b64encode(value, "rawurl");
}

function safeJson(response, selector) {
    try {
        return response.json(selector);
    } catch (error) {
        return null;
    }
}
