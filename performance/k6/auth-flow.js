import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

// 실행 전 준비:
// 1. AUTH_USER_ID_BASE부터 AUTH_USER_COUNT만큼 사용자를 생성한다.
// 2. 이메일은 `${AUTH_EMAIL_PREFIX}${userId}@sisibibi.test` 형식으로 생성한다.
// 3. 모든 테스트 사용자의 비밀번호를 AUTH_PASSWORD와 동일하게 생성한다.
// 4. Redis가 비어 있어도 로그인부터 시작하므로 Refresh Token은 실행 중 생성된다.
const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const authUserIdBase = Number(__ENV.AUTH_USER_ID_BASE || "300000");
const authUserCount = Number(__ENV.AUTH_USER_COUNT || "100");
const emailPrefix = __ENV.AUTH_EMAIL_PREFIX || "perf-auth-";
const password = __ENV.AUTH_PASSWORD || "test1234!";
const mode = __ENV.MODE || "login";

const authFailureRate = new Rate("auth_failure_rate");
const loginDuration = new Trend("auth_login_duration", true);
const reissueDuration = new Trend("auth_reissue_duration", true);

export const options = {
    scenarios: {
        authFlow: {
            executor: "constant-vus",
            vus: Number(__ENV.VUS || "5"),
            duration: __ENV.DURATION || "1m",
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.05"],
        auth_failure_rate: ["rate<0.05"],
        auth_login_duration: ["p(95)<2000"],
        auth_reissue_duration: ["p(95)<2000"],
    },
};

export default function () {
    const userId = authUserIdBase + ((__VU - 1) % authUserCount);
    const email = `${emailPrefix}${userId}@sisibibi.test`;
    const login = http.post(
        `${baseUrl}/api/v1/auth/login`,
        JSON.stringify({ email, password }),
        {
            headers: { "Content-Type": "application/json" },
            tags: { name: "POST /api/v1/auth/login" },
        }
    );

    loginDuration.add(login.timings.duration);
    const loginOk = check(login, {
        "login returns 200": (response) => response.status === 200,
    });
    authFailureRate.add(!loginOk);

    if (!loginOk || mode !== "flow") {
        sleep(Number(__ENV.SLEEP_SECONDS || "1"));
        return;
    }

    const reissue = http.post(
        `${baseUrl}/api/v1/auth/reissue`,
        null,
        {
            tags: { name: "POST /api/v1/auth/reissue" },
        }
    );

    reissueDuration.add(reissue.timings.duration);
    const reissueOk = check(reissue, {
        "reissue returns 200": (response) => response.status === 200,
    });
    authFailureRate.add(!reissueOk);
    sleep(Number(__ENV.SLEEP_SECONDS || "1"));
}
