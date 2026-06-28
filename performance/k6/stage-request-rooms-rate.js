import http from "k6/http";
import { check } from "k6";
import exec from "k6/execution";
import { Counter } from "k6/metrics";
import { authParams } from "./lib/auth.js";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const roomIds = (__ENV.ROOM_IDS || __ENV.ROOM_ID || "910001")
    .split(",")
    .map((roomId) => Number(roomId.trim()))
    .filter((roomId) => Number.isFinite(roomId));
const userIdBase = Number(__ENV.USER_ID_BASE || "1000000");
const usersPerRoom = Number(__ENV.USERS_PER_ROOM || "100");
const rate = Number(__ENV.RATE || "10");
const duration = __ENV.DURATION || "30s";

const createdRequests = new Counter("stage_requests_created");
const rejectedRequests = new Counter("stage_requests_rejected");

export const options = {
    scenarios: {
        stageRequests: {
            executor: "constant-arrival-rate",
            rate,
            timeUnit: "1s",
            duration,
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
