// topic-candidates-refresh.js
import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";
import crypto from "k6/crypto";
import encoding from "k6/encoding";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const adminUserId = Number(__ENV.ADMIN_USER_ID || "1");
const csrfToken = __ENV.CSRF_TOKEN || "perf-csrf-token";
const jwtSecret =
  __ENV.JWT_SECRET || "local-development-jwt-secret-key-must-be-at-least-32-bytes";
const tokenVersion = Number(__ENV.TOKEN_VERSION || "0");

const failureRate = new Rate("topic_candidates_refresh_failure_rate");
const refreshDuration = new Trend("POST_topic_candidates_refresh", true);

export const options = {
  scenarios: {
    refresh: {
      executor: "shared-iterations",
      vus: 1,
      iterations: Number(__ENV.ITERATIONS || "10"),
    },
  },
};

export default function () {
  const res = http.post(
    `${baseUrl}/api/v1/admin/topics/candidates/classified/refresh`,
    null,
    adminParams({ name: "POST /api/v1/admin/topics/candidates/classified/refresh" })
  );

  if (res.status !== 200) {
    console.log(`status=${res.status}, body=${res.body}`);
  }

  refreshDuration.add(res.timings.duration);

  const ok = res.status === 200;
  failureRate.add(!ok);

  check(res, {
    "refresh status is 200": () => ok,
  });

  sleep(Number(__ENV.SLEEP_SECONDS || "5"));
}

function adminParams(tags = {}) {
  return {
    headers: {
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": csrfToken,
      Cookie: `accessToken=${accessToken(adminUserId, "ADMIN")}; XSRF-TOKEN=${csrfToken}`,
    },
    tags,
  };
}

function accessToken(userId, role) {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payload = base64Url(JSON.stringify({
    sub: String(userId),
    jti: `${userId}-${now}`,
    email: `perf-admin-${userId}@sisibibi.test`,
    role,
    tokenType: "ACCESS",
    tokenVersion,
    iat: now,
    exp: now + Number(__ENV.ACCESS_TOKEN_TTL_SECONDS || "1800"),
  }));

  const unsignedToken = `${header}.${payload}`;
  const signature = crypto.hmac("sha256", jwtSecret, unsignedToken, "base64rawurl");
  return `${unsignedToken}.${signature}`;
}

function base64Url(value) {
  return encoding.b64encode(value, "rawurl");
}