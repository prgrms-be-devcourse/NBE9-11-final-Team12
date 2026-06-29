import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";
import { Counter, Rate, Trend } from "k6/metrics";
import { authParams } from "./lib/auth.js";

// 목표 규모 발언권 신청 burst 시나리오.
// 기본 전제: performance/sql/seed-performance-data.sql 실행 후 사용한다.
// 사용자별 중복 신청 정책 때문에 같은 DB에서 반복 실행하면 409가 정상적으로 증가한다.
// 순수 생성 TPS를 재측정하려면 cleanup 후 seed를 다시 실행한다.
const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const roomIds = (__ENV.ROOM_IDS || "900001,900002,900003,900004,900005,900006,900007,900008,900009,900010")
    .split(",")
    .map((roomId) => Number(roomId.trim()))
    .filter((roomId) => Number.isFinite(roomId));
const userIdBase = Number(__ENV.USER_ID_BASE || "100000");
const usersPerRoom = Number(__ENV.USERS_PER_ROOM || "100");
const userCount = Number(__ENV.USER_COUNT || String(roomIds.length * usersPerRoom));

const failureRate = new Rate("target_stage_failure_rate");
const created = new Counter("stage_requests_created");
const businessRejected = new Counter("stage_requests_business_rejected");
const stageRequestDuration = new Trend("POST_stage_request", true);

export const options = {
    scenarios: {
        targetStageRequests: {
            executor: "constant-arrival-rate",
            rate: Number(__ENV.RATE || "30"),
            timeUnit: "1s",
            duration: __ENV.DURATION || "1m",
            preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || "80"),
            maxVUs: Number(__ENV.MAX_VUS || "300"),
        },
    },
    thresholds: {
        target_stage_failure_rate: ["rate<0.01"],
        http_req_duration: ["p(95)<1000", "p(99)<2000"],
    },
};

export default function () {
    const iteration = exec.scenario.iterationInTest;
    const roomIndex = iteration % roomIds.length;
    const roomRound = Math.floor(iteration / roomIds.length) % usersPerRoom;
    const userId = userIdBase + roomIndex * usersPerRoom + roomRound;
    const roomId = roomIds[roomIndex];
    const response = http.post(
        `${baseUrl}/api/v1/rooms/${roomId}/stage/requests`,
        JSON.stringify({ stance: iteration % 2 === 0 ? "PRO" : "CON" }),
        authParams(userId, { name: "POST /api/v1/rooms/{roomId}/stage/requests" })
    );

    stageRequestDuration.add(response.timings.duration);

    if (response.status === 201) {
        created.add(1);
        failureRate.add(false);
        return;
    }

    if ([400, 403, 409].includes(response.status)) {
        businessRejected.add(1);
        failureRate.add(false);
        return;
    }

    failureRate.add(true);
    check(response, {
        "stage request returns expected status": () => false,
    });
}
