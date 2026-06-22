import http from "k6/http";
import { check } from "k6";
import { Counter } from "k6/metrics";
import { authParams } from "./lib/auth.js";

// 실행 전 준비:
// 1. ROOM_ID에 해당하는 OPEN 토론방을 생성한다.
// 2. USER_ID_BASE부터 RATE * DURATION 이상 사용자를 생성한다.
// 3. 생성한 사용자의 token_version을 TOKEN_VERSION과 맞춘다.
// 이 스크립트는 발언권 신청 상태를 생성하므로 core-api-mixed.js와 사용자 범위를 분리한다.
const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const roomId = Number(__ENV.ROOM_ID || "910001");
const userIdBase = Number(__ENV.USER_ID_BASE || "1000000");

const createdRequests = new Counter("stage_requests_created");
const rejectedRequests = new Counter("stage_requests_rejected");

export const options = {
    scenarios: {
        stageRequests: {
            executor: "constant-arrival-rate",
            rate: Number(__ENV.RATE || "50"),
            timeUnit: "1s",
            duration: __ENV.DURATION || "20s",
            preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || "20"),
            maxVUs: Number(__ENV.MAX_VUS || "100"),
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(95)<2000"],
        stage_requests_rejected: ["count==0"],
    },
};

export default function () {
    const userId = userIdBase + (__VU * 1_000_000) + __ITER;
    const url = `${baseUrl}/api/v1/rooms/${roomId}/stage/requests`;

    const response = http.post(
        url,
        JSON.stringify({ stance: __ITER % 2 === 0 ? "PRO" : "CON" }),
        authParams(userId, {
            name: "POST /api/v1/rooms/{roomId}/stage/requests",
        })
    );

    const created = check(response, {
        "stage request returns 201": (res) => res.status === 201,
        "stage request returns SUCCESS": (res) => {
            if (res.status !== 201) {
                return false;
            }

            return res.json("code") === "SUCCESS";
        },
    });

    if (created) {
        createdRequests.add(1);
    } else {
        rejectedRequests.add(1);
    }
}
