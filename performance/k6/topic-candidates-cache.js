// performance/k6/topic-candidates-cache.js
import http from "k6/http";
import { check } from "k6";
import { Rate, Trend } from "k6/metrics";
import crypto from "k6/crypto";
import encoding from "k6/encoding";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";
const adminUserId = Number(__ENV.ADMIN_USER_ID || "1");
const csrfToken = __ENV.CSRF_TOKEN || "perf-csrf-token";
const jwtSecret =
  __ENV.JWT_SECRET || "local-development-jwt-secret-key-must-be-at-least-32-bytes";
const tokenVersion = Number(__ENV.TOKEN_VERSION || "0");

const failureRate = new Rate("topic_candidates_failure_rate");
const cacheHitDuration = new Trend("GET_topic_candidates_cache_hit", true);
const refreshDuration = new Trend("POST_topic_candidates_refresh", true);

export const options = {
  scenarios: {
    cacheHit: {
      executor: "constant-arrival-rate",
      rate: Number(__ENV.RATE || "50"),
      timeUnit: "1s",
      duration: __ENV.DURATION || "5m",
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || "50"),
      maxVUs: Number(__ENV.MAX_VUS || "300"),
    },
  },
  thresholds: {
    topic_candidates_failure_rate: ["rate<0.01"],
    GET_topic_candidates_cache_hit: ["avg<100", "p(95)<300", "p(99)<800"],
    http_req_failed: ["rate<0.01"],
  },
};

export function setup() {
  // 캐시 warm-up. 여기서 한 번 외부 API/AI 호출 후 Redis에 저장됨.
  const res = http.post(
    `${baseUrl}/api/v1/admin/topics/candidates/classified/refresh`,
    null,
    adminParams({ name: "POST /api/v1/admin/topics/candidates/classified/refresh" })
  );

  refreshDuration.add(res.timings.duration);

  if (res.status !== 200) {
    throw new Error(`cache warm-up failed. status=${res.status}, body=${res.body}`);
  }
}

export default function () {
  const res = http.get(
    `${baseUrl}/api/v1/admin/topics/candidates/classified`,
    adminParams({ name: "GET /api/v1/admin/topics/candidates/classified" })
  );

  cacheHitDuration.add(res.timings.duration);

  const ok = res.status === 200;
  failureRate.add(!ok);

  check(res, {
    "topic candidates status is 200": () => ok,
  });
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