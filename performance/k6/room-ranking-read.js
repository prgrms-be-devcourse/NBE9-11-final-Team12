// performance/k6/room-ranking-read.js
import http from "k6/http";
import { check } from "k6";
import { Rate, Trend } from "k6/metrics";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";

const mode = __ENV.MODE || "unknown"; // redis | db
const stageName = __ENV.STAGE_NAME || "manual"; // baseline | load | stress
const rate = Number(__ENV.RATE || "50");
const duration = __ENV.DURATION || "5m";
const preAllocatedVUs = Number(__ENV.PRE_ALLOCATED_VUS || Math.max(50, rate));
const maxVUs = Number(__ENV.MAX_VUS || Math.max(300, rate * 2));

const failureRate = new Rate("room_ranking_failure_rate");
const rankingDuration = new Trend("GET_rooms_ranking", true);

export const options = {
  scenarios: {
    rankingRead: {
      executor: "constant-arrival-rate",
      rate,
      timeUnit: "1s",
      duration,
      preAllocatedVUs,
      maxVUs,
      tags: {
        api: "room-ranking",
        ranking_mode: mode,
        stage: stageName,
      },
    },
  },
  thresholds: {
    room_ranking_failure_rate: ["rate<0.01"],
    GET_rooms_ranking: ["avg<300", "p(95)<800", "p(99)<1500"],
    http_req_failed: ["rate<0.01"],
  },
  tags: {
    api: "room-ranking",
    ranking_mode: mode,
    stage: stageName,
  },
};

export default function () {
  const res = http.get(`${baseUrl}/api/v1/rooms/ranking`, {
    tags: {
      name: "GET /api/v1/rooms/ranking",
      api: "room-ranking",
      ranking_mode: mode,
      stage: stageName,
    },
  });

  rankingDuration.add(res.timings.duration, {
    ranking_mode: mode,
    stage: stageName,
  });

  const ok = res.status === 200;
  failureRate.add(!ok, {
    ranking_mode: mode,
    stage: stageName,
  });

  check(res, {
    "ranking status is 200": () => ok,
  });
}

export function handleSummary(data) {
  const safeMode = mode.replace(/[^a-zA-Z0-9_-]/g, "_");
  const safeStage = stageName.replace(/[^a-zA-Z0-9_-]/g, "_");
  const safeRate = String(rate).replace(/[^0-9]/g, "");

  return {
    [`performance/results/room-ranking-${safeMode}-${safeStage}-${safeRate}rps-summary.json`]:
      JSON.stringify(data, null, 2),
    stdout: textSummary(data),
  };
}

function textSummary(data) {
  const metrics = data.metrics;

  const durationMetric = metrics.GET_rooms_ranking;
  const failureMetric = metrics.room_ranking_failure_rate;
  const httpFailed = metrics.http_req_failed;
  const httpReqs = metrics.http_reqs;

  return `
room ranking read test
mode: ${mode}
stage: ${stageName}
rate: ${rate} rps
duration: ${duration}

GET_rooms_ranking avg: ${durationMetric?.values?.avg ?? "-"} ms
GET_rooms_ranking p95: ${durationMetric?.values?.["p(95)"] ?? "-"} ms
GET_rooms_ranking p99: ${durationMetric?.values?.["p(99)"] ?? "-"} ms
room_ranking_failure_rate: ${failureMetric?.values?.rate ?? "-"}
http_req_failed: ${httpFailed?.values?.rate ?? "-"}
http_reqs rate: ${httpReqs?.values?.rate ?? "-"} req/s
`;
}