// k6 load scenario for GET /api/events/{id}/vicinity.
//
// Ramps to 500 RPS over 5 minutes, holds, ramps down. Asserts p95 ≤ 300 ms and error rate ≤ 0.5%.
// Usage:
//   BASE_URL=http://localhost:8080 JWT=... EVENT_ID=... k6 run infra/load/k6-vicinity.js

import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  scenarios: {
    vicinity: {
      executor: "ramping-arrival-rate",
      timeUnit: "1s",
      preAllocatedVUs: 200,
      maxVUs: 800,
      startRate: 0,
      stages: [
        { duration: "1m", target: 100 },
        { duration: "2m", target: 500 },
        { duration: "2m", target: 500 },
        { duration: "30s", target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.005"],
    http_req_duration: ["p(95)<300"],
  },
};

const BASE = __ENV.BASE_URL || "http://localhost:8080";
const JWT = __ENV.JWT || "";
const EVENT_ID = __ENV.EVENT_ID || "";
const RADIUS = __ENV.RADIUS || "100";

if (!JWT || !EVENT_ID) {
  throw new Error("Set JWT and EVENT_ID env vars before running k6-vicinity.");
}

export default function () {
  const res = http.get(`${BASE}/api/events/${EVENT_ID}/vicinity?radius=${RADIUS}`, {
    headers: { Authorization: `Bearer ${JWT}` },
    tags: { endpoint: "vicinity" },
  });
  check(res, {
    "status is 200": (r) => r.status === 200,
    "has matches array": (r) => Array.isArray(r.json("matches")),
  });
  sleep(1);
}
