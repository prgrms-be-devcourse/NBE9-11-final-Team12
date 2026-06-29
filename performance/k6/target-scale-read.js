import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";
import { Rate, Trend } from "k6/metrics";
import { authParams } from "./lib/auth.js";

// 목표 규모 읽기 부하 시나리오.
// 기본 전제: performance/sql/seed-performance-data.sql 실행 후 사용한다.
// 기본값은 10개 방 × 방당 100명 = 1000명 동시접속 규모를 조회 API로 압박한다.
// RATE는 iteration/s이며, iteration 1회가 HTTP 10개를 호출한다.
// 예: RATE=30이면 약 300 HTTP RPS.
const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const roomIds = (__ENV.ROOM_IDS || "900001,900002,900003,900004,900005,900006,900007,900008,900009,900010")
    .split(",")
    .map((roomId) => Number(roomId.trim()))
    .filter((roomId) => Number.isFinite(roomId));
const userIdBase = Number(__ENV.USER_ID_BASE || "100000");
const userCount = Number(__ENV.USER_COUNT || "1000");

const failureRate = new Rate("target_scale_read_failure_rate");
const usersMe = new Trend("GET_users_me", true);
const roomsOpen = new Trend("GET_rooms_open", true);
const roomDetail = new Trend("GET_room_detail", true);
const participantsCount = new Trend("GET_participants_count", true);
const speeches = new Trend("GET_room_speeches", true);
const stage = new Trend("GET_stage", true);
const myStageRequest = new Trend("GET_stage_request_me", true);
const bestSpeech = new Trend("GET_best_speech", true);
const myTrust = new Trend("GET_users_me_trust", true);
const publicTrust = new Trend("GET_user_trust", true);

export const options = {
    scenarios: {
        targetScaleRead: {
            executor: "constant-arrival-rate",
            rate: Number(__ENV.RATE || "30"),
            timeUnit: "1s",
            duration: __ENV.DURATION || "5m",
            preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || "100"),
            maxVUs: Number(__ENV.MAX_VUS || "500"),
        },
    },
    thresholds: {
        target_scale_read_failure_rate: ["rate<0.01"],
        http_req_duration: ["p(95)<1000", "p(99)<2000"],
    },
};

export default function () {
    const iteration = exec.scenario.iterationInTest;
    const userId = userIdBase + (iteration % userCount);
    const roomId = roomIds[iteration % roomIds.length];

    get("/api/v1/users/me", userId, usersMe, [200], "GET /api/v1/users/me");
    get("/api/v1/rooms/open", userId, roomsOpen, [200], "GET /api/v1/rooms/open");
    get(`/api/v1/rooms/${roomId}`, userId, roomDetail, [200], "GET /api/v1/rooms/{roomId}");
    get(`/api/v1/rooms/${roomId}/participants/count`, userId, participantsCount, [200], "GET /api/v1/rooms/{roomId}/participants/count");
    get(`/api/v1/rooms/${roomId}/speeches?size=20`, userId, speeches, [200], "GET /api/v1/rooms/{roomId}/speeches");
    get(`/api/v1/rooms/${roomId}/stage`, userId, stage, [200], "GET /api/v1/rooms/{roomId}/stage");
    get(`/api/v1/rooms/${roomId}/stage/requests/me`, userId, myStageRequest, [200, 404], "GET /api/v1/rooms/{roomId}/stage/requests/me");
    get(`/api/v1/rooms/${roomId}/best-speech`, userId, bestSpeech, [200, 404], "GET /api/v1/rooms/{roomId}/best-speech");
    get("/api/v1/users/me/trust", userId, myTrust, [200], "GET /api/v1/users/me/trust");
    get(`/api/v1/users/${userId}/trust`, userId, publicTrust, [200], "GET /api/v1/users/{userId}/trust");
}

function get(path, userId, trend, expectedStatuses, name) {
    const response = http.get(`${baseUrl}${path}`, authParams(userId, { name }));
    trend.add(response.timings.duration);
    const ok = expectedStatuses.includes(response.status);
    failureRate.add(!ok);
    check(response, {
        [`${name} expected status`]: () => ok,
    });
}
