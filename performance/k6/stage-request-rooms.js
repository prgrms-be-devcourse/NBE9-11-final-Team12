import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";
import { Counter } from "k6/metrics";
import { authParams } from "./lib/auth.js";

// 방 1개 또는 여러 개에 발언권 신청 부하를 분산한다.
// 테스트 데이터는 USER_ID_BASE부터 room 개수 단위로 room별 참여자가 섞여 있어야 한다.
// 예: ROOM_IDS=10,11,12 USER_ID_BASE=1000이면
// user 1000 -> room 10, user 1001 -> room 11, user 1002 -> room 12,
// user 1003 -> room 10 순서로 매핑된다.
const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const roomIds = (__ENV.ROOM_IDS || __ENV.ROOM_ID || "910001")
    .split(",")
    .map((roomId) => Number(roomId.trim()))
    .filter((roomId) => Number.isFinite(roomId));
const userIdBase = Number(__ENV.USER_ID_BASE || "1000000");
const usersPerRoom = Number(__ENV.USERS_PER_ROOM || "100");

const createdRequests = new Counter("stage_requests_created");
const rejectedRequests = new Counter("stage_requests_rejected");

export const options = {
    scenarios: {
        stageRequests: {
            executor: "shared-iterations",
            vus: Number(__ENV.VUS || "20"),
            iterations: Number(__ENV.ITERATIONS || String(roomIds.length * usersPerRoom)),
            maxDuration: __ENV.MAX_DURATION || "30s",
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(95)<2000"],
        stage_requests_rejected: ["count==0"],
    },
};

export default function () {
    const iteration = exec.scenario.iterationInTest;
    const totalUsers = roomIds.length * usersPerRoom;
    if (iteration >= totalUsers) {
        throw new Error(
            `Not enough test users. iteration=${iteration}, totalUsers=${totalUsers}`
        );
    }

    const userSequence = iteration;
    const roomIndex = userSequence % roomIds.length;
    const roomRound = Math.floor(userSequence / roomIds.length);
    const roomId = roomIds[roomIndex];
    const userId = userIdBase + roomRound * roomIds.length + roomIndex;
    const stance = userSequence % 2 === 0 ? "PRO" : "CON";

    const response = http.post(
        `${baseUrl}/api/v1/rooms/${roomId}/stage/requests`,
        JSON.stringify({ stance }),
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
