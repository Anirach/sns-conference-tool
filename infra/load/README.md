# Load scenarios

Two [k6](https://k6.io/) scenarios covering the two hot paths the spec calls out.

## `k6-vicinity.js`

Ramps to 500 RPS on `GET /api/events/{id}/vicinity?radius=100` and holds for 2 minutes. Asserts p95 ≤ 300 ms and < 0.5% errors.

```bash
BASE_URL=https://api.staging.sns.example.com \
JWT=eyJhbGciOi...                             \
EVENT_ID=00000000-0000-0000-0000-000000000000 \
k6 run infra/load/k6-vicinity.js
```

## `k6-chat.js`

Opens 1000 concurrent STOMP over WebSocket sessions, each sending 10 msg/s for 2 minutes. Asserts p95 round-trip ≤ 200 ms.

```bash
BASE_WS=wss://api.staging.sns.example.com/ws                    \
JWT=eyJhbGciOi...                                               \
EVENT_ID=00000000-0000-0000-0000-000000000000                   \
PEER_USER_ID=00000000-0000-0000-0000-000000000000               \
k6 run infra/load/k6-chat.js
```

## Before running against staging

1. Register a throwaway load-test user, grab an access token via the `/api/auth/**` flow.
2. Seed a NEURIPS-style event if not already present; grab its `eventId`.
3. Have at least one peer participant with overlapping interests so vicinity returns non-empty lists (otherwise the cached empty response hides contention).
4. Confirm the target Helm release has HPA headroom (`kubectl top pods -l app=sns-backend`).
5. Watch Grafana → **SNS Overview** while the test runs; record p50/p95/p99 and error rate.

Targets come from `docs/SNS-system.md` §18 (latency & load budgets).
