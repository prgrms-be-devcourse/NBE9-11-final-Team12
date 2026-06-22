import http from "k6/http";
import { check, sleep } from "k6";
import exec from "k6/execution";
import { Counter, Rate, Trend } from "k6/metrics";
import { authParams } from "./lib/auth.js";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const mode = __ENV.MODE || "smoke";
const roomId = Number(__ENV.ROOM_ID || "1");
const speechIdBase = Number(__ENV.SPEECH_ID_BASE || __ENV.SPEECH_ID || "1");
const speechCount = Number(__ENV.SPEECH_COUNT || "1");
const userIdBase = Number(__ENV.USER_ID_BASE || "100000");

const requestFailed = new Rate("core_api_failure_rate");
const requestDuration = new Trend("core_api_duration", true);
const participantJoined = new Counter("participant_joined");
const speechCreated = new Counter("speech_created");
const reactionCreated = new Counter("reaction_created");
const reactionCanceled = new Counter("reaction_canceled");
const reportCreated = new Counter("report_created");
const stageRequested = new Counter("stage_requested");

export const options = buildOptions();

export default function () {
    const iteration = exec.scenario.iterationInTest;
    const userId = userIdBase + iteration;
    const speechId = speechIdBase + (iteration % speechCount);

    if (exec.scenario.name === "readApis") {
        readApis(userId);
        return;
    }

    if (exec.scenario.name === "writeApis") {
        writeApis(userId, speechId, iteration);
        return;
    }

    readApis(userId);
    writeApis(userId, speechId, iteration);
    sleep(Number(__ENV.SLEEP_SECONDS || "1"));
}

function buildOptions() {
    if (mode === "load") {
        return {
            scenarios: {
                readApis: {
                    executor: "constant-arrival-rate",
                    rate: Number(__ENV.READ_RATE || "30"),
                    timeUnit: "1s",
                    duration: __ENV.DURATION || "5m",
                    preAllocatedVUs: Number(__ENV.READ_PRE_ALLOCATED_VUS || "50"),
                    maxVUs: Number(__ENV.READ_MAX_VUS || "200"),
                    exec: "default",
                    tags: { workload: "read" },
                },
                writeApis: {
                    executor: "constant-arrival-rate",
                    rate: Number(__ENV.WRITE_RATE || "10"),
                    timeUnit: "1s",
                    duration: __ENV.DURATION || "5m",
                    preAllocatedVUs: Number(__ENV.WRITE_PRE_ALLOCATED_VUS || "30"),
                    maxVUs: Number(__ENV.WRITE_MAX_VUS || "120"),
                    exec: "default",
                    tags: { workload: "write" },
                },
            },
            thresholds: thresholds(),
        };
    }

    return {
        scenarios: {
            smoke: {
                executor: "constant-vus",
                vus: Number(__ENV.VUS || "3"),
                duration: __ENV.DURATION || "1m",
                exec: "default",
                tags: { workload: "smoke" },
            },
        },
        thresholds: thresholds(),
    };
}

function thresholds() {
    return {
        http_req_failed: ["rate<0.05"],
        core_api_failure_rate: ["rate<0.05"],
        http_req_duration: ["p(95)<2000", "p(99)<5000"],
    };
}

function readApis(userId) {
    get("/api/v1/users/me", userId, "GET /api/v1/users/me", [200]);
    get("/api/v1/rooms/open", userId, "GET /api/v1/rooms/open", [200]);
    get(`/api/v1/rooms/${roomId}`, userId, "GET /api/v1/rooms/{roomId}", [200]);
    get(`/api/v1/rooms/${roomId}/participants/count`, userId, "GET /api/v1/rooms/{roomId}/participants/count", [200]);
    get(`/api/v1/rooms/${roomId}/speeches?size=20`, userId, "GET /api/v1/rooms/{roomId}/speeches", [200]);
    get(`/api/v1/rooms/${roomId}/stage`, userId, "GET /api/v1/rooms/{roomId}/stage", [200]);
    get(`/api/v1/rooms/${roomId}/stage/requests/me`, userId, "GET /api/v1/rooms/{roomId}/stage/requests/me", [200, 404]);
    get(`/api/v1/rooms/${roomId}/best-speech`, userId, "GET /api/v1/rooms/{roomId}/best-speech", [200, 404]);
}

function writeApis(userId, speechId, iteration) {
    const join = post(`/api/v1/rooms/${roomId}/participants`, userId, null, "POST /api/v1/rooms/{roomId}/participants", [200, 201, 409]);
    if (join.status === 200 || join.status === 201) {
        participantJoined.add(1);
    }

    const speech = post(
        `/api/v1/rooms/${roomId}/speeches`,
        userId,
        JSON.stringify({
            content: `성능 테스트 의견 ${iteration}`,
            stance: iteration % 2 === 0 ? "PRO" : "CON",
        }),
        "POST /api/v1/rooms/{roomId}/speeches",
        [201, 400, 403, 409]
    );
    if (speech.status === 201) {
        speechCreated.add(1);
    }

    const reaction = post(
        `/api/v1/speeches/${speechId}/reactions`,
        userId,
        null,
        "POST /api/v1/speeches/{speechId}/reactions",
        [201, 400, 403, 404, 409]
    );
    if (reaction.status === 201) {
        reactionCreated.add(1);
    }

    const reactionCancel = del(
        `/api/v1/speeches/${speechId}/reactions`,
        userId,
        "DELETE /api/v1/speeches/{speechId}/reactions",
        [200, 204, 400, 403, 404, 409]
    );
    if (reactionCancel.status === 200 || reactionCancel.status === 204) {
        reactionCanceled.add(1);
    }

    const report = post(
        `/api/v1/speeches/${speechId}/reports`,
        userId,
        JSON.stringify({
            reason: "ABUSE_HARASSMENT",
            description: "성능 테스트 신고 데이터",
        }),
        "POST /api/v1/speeches/{speechId}/reports",
        [201, 400, 403, 404, 409]
    );
    if (report.status === 201) {
        reportCreated.add(1);
    }

    const stage = post(
        `/api/v1/rooms/${roomId}/stage/requests`,
        userId,
        JSON.stringify({ stance: iteration % 2 === 0 ? "PRO" : "CON" }),
        "POST /api/v1/rooms/{roomId}/stage/requests",
        [201, 400, 403, 404, 409]
    );
    if (stage.status === 201) {
        stageRequested.add(1);
    }
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
    const response = http.request(
        method,
        `${baseUrl}${path}`,
        body,
        authParams(userId, { name })
    );
    requestDuration.add(response.timings.duration);

    const ok = check(response, {
        [`${name} expected status`]: (res) => expectedStatuses.includes(res.status),
        [`${name} not 5xx`]: (res) => res.status < 500,
    });

    requestFailed.add(!ok);
    return response;
}
