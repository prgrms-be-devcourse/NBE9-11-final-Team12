// performance/k6/room-ranking-read.js
import http from "k6/http";
import { check } from "k6";
import { Rate, Trend } from "k6/metrics";

const baseUrl = __ENV.BASE_URL || "http://localhost:8080";

const failureRate = new Rate("room_ranking_failure_rate");
const rankingDuration = new Trend("GET_rooms_ranking", true);

export const options = {
  scenarios: {
    rankingRead: {
      executor: "constant-arrival-rate",
      rate: Number(__ENV.RATE || "50"),
      timeUnit: "1s",
      duration: __ENV.DURATION || "5m",
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || "50"),
      maxVUs: Number(__ENV.MAX_VUS || "300"),
    },
  },
  thresholds: {
    room_ranking_failure_rate: ["rate<0.01"],
    GET_rooms_ranking: ["avg<300", "p(95)<800", "p(99)<1500"],
    http_req_failed: ["rate<0.01"],
  },
};

export default function () {
  const res = http.get(`${baseUrl}/api/v1/rooms/ranking`, {
    tags: { name: "GET /api/v1/rooms/ranking" },
  });

  rankingDuration.add(res.timings.duration);
  const ok = res.status === 200;

  failureRate.add(!ok);
  check(res, {
    "ranking status is 200": () => ok,
  });
}