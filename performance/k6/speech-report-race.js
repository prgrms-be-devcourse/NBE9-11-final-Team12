import http from "k6/http";
import { check } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";
import { authParams } from "./lib/auth.js";

// 같은 사용자가 같은 의견을 동시에 여러 번 신고할 때
// 중복 신고가 최대 1건만 생성되는지 확인한다.
// 실행 전 준비:
// 1. SPEECH_ID에 해당하는 삭제되지 않은 의견이 있어야 한다.
// 2. USER_ID는 해당 방 JOINED 참여자여야 한다.
// 3. USER_ID는 대상 의견 작성자가 아니어야 한다.
// 4. USER_ID가 해당 의견을 아직 신고하지 않은 상태여야 순수 생성 경합을 확인할 수 있다.
const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const speechId = Number(__ENV.SPEECH_ID || "1");
const userId = Number(__ENV.USER_ID || "100030");
const attempts = Number(__ENV.ATTEMPTS || "20");

const created = new Counter("speech_report_race_created");
const duplicateRejected = new Counter("speech_report_race_duplicate_rejected");
const unexpected = new Counter("speech_report_race_unexpected");
const failureRate = new Rate("speech_report_race_failure_rate");
const duration = new Trend("speech_report_race_duration", true);

export const options = {
    scenarios: {
        simultaneousReport: {
            executor: "per-vu-iterations",
            vus: attempts,
            iterations: 1,
            maxDuration: __ENV.MAX_DURATION || "30s",
        },
    },
    thresholds: {
        speech_report_race_failure_rate: ["rate==0"],
        http_req_duration: ["p(95)<2000"],
    },
};

export default function () {
    const response = http.post(
        `${baseUrl}/api/v1/speeches/${speechId}/reports`,
        JSON.stringify({
            reason: "SPAM",
            description: "동시성 테스트 신고입니다.",
        }),
        authParams(userId, { name: "POST /api/v1/speeches/{speechId}/reports race" })
    );
    duration.add(response.timings.duration);

    if (response.status === 201) {
        created.add(1);
        failureRate.add(false);
        return;
    }

    if ([400, 409].includes(response.status)) {
        duplicateRejected.add(1);
        failureRate.add(false);
        return;
    }

    unexpected.add(1);
    failureRate.add(true);
    check(response, {
        "speech report race returns created or duplicate rejection": () => false,
    });
}

export function handleSummary(data) {
    const createdCount = data.metrics.speech_report_race_created?.values?.count || 0;
    const invariantMaintained = createdCount <= 1;
    return {
        stdout: [
            `attempts=${attempts}`,
            `created=${createdCount}`,
            `duplicate invariant maintained=${invariantMaintained}`,
            "",
        ].join("\n"),
    };
}
