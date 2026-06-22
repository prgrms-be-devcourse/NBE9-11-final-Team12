import http from "k6/http";
import { check } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";
import { authParams } from "./lib/auth.js";

// 실행 전 준비:
// 1. ROOM_ID에 해당하는 OPEN 토론방을 생성하고 maxParticipants를 테스트 목표값으로 설정한다.
// 2. 방의 기존 JOINED 참여자 수를 KNOWN_EXISTING_PARTICIPANTS와 동일하게 맞춘다.
// 3. USER_ID_BASE부터 ATTEMPTS 이상 신규 사용자를 생성한다.
// 4. 생성한 사용자의 token_version을 TOKEN_VERSION과 맞춘다.
// 이 스크립트는 정원 직전 동시 입장에서 최대 인원이 초과되지 않는지 확인한다.
const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const roomId = Number(__ENV.ROOM_ID || "1");
const userIdBase = Number(__ENV.USER_ID_BASE || "400000");
const attempts = Number(__ENV.ATTEMPTS || "10");
const roomCapacity = Number(__ENV.ROOM_CAPACITY || "100");
const existingParticipants = Number(__ENV.KNOWN_EXISTING_PARTICIPANTS || "99");

const joined = new Counter("room_join_succeeded");
const rejectedAsFull = new Counter("room_join_rejected_as_full");
const unexpected = new Counter("room_join_unexpected");
const joinFailureRate = new Rate("room_join_failure_rate");
const joinDuration = new Trend("room_join_duration", true);

export const options = {
    scenarios: {
        simultaneousJoin: {
            executor: "per-vu-iterations",
            vus: attempts,
            iterations: 1,
            maxDuration: __ENV.MAX_DURATION || "30s",
        },
    },
    thresholds: {
        room_join_failure_rate: ["rate==0"],
        http_req_duration: ["p(95)<2000"],
    },
};

export default function () {
    const userId = userIdBase + (__VU - 1);
    const response = http.post(
        `${baseUrl}/api/v1/rooms/${roomId}/participants`,
        null,
        authParams(userId, {
            name: "POST /api/v1/rooms/{roomId}/participants capacity",
        })
    );
    joinDuration.add(response.timings.duration);

    if (response.status === 200 || response.status === 201) {
        joined.add(1);
        joinFailureRate.add(false);
        return;
    }

    if (response.status === 409 || response.status === 400) {
        rejectedAsFull.add(1);
        joinFailureRate.add(false);
        return;
    }

    unexpected.add(1);
    joinFailureRate.add(true);
    check(response, {
        "join returns success or room-full response": () => false,
    });
}

export function handleSummary(data) {
    const allowedJoins = Math.max(roomCapacity - existingParticipants, 0);
    const actualJoins = data.metrics.room_join_succeeded?.values?.count || 0;
    const expectedMaximumMaintained = actualJoins <= allowedJoins;

    return {
        stdout: [
            `room capacity=${roomCapacity}`,
            `existing participants=${existingParticipants}`,
            `allowed joins=${allowedJoins}`,
            `actual joins=${actualJoins}`,
            `capacity invariant maintained=${expectedMaximumMaintained}`,
            "",
        ].join("\n"),
    };
}
