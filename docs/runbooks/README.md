# SNS on-call runbooks

Targeted responses to the seven Prometheus alerts defined in `infra/prometheus/alert-rules.yaml`.
Each runbook follows the same shape: **Diagnose → Remediate → Escalate**.

| Alert | Runbook |
|---|---|
| `SnsApiHighErrorRate` | [api-error-rate.md](api-error-rate.md) |
| `SnsWebSocketReconnectStorm` | [ws-reconnect-storm.md](ws-reconnect-storm.md) |
| `SnsRedisMemoryHigh` | [redis-memory.md](redis-memory.md) |
| `SnsPgReplicationLag` | [pg-replication.md](pg-replication.md) |
| `SnsMatchRecomputeSlow` | [matching-slow.md](matching-slow.md) |
| `SnsJwtDecodeErrors` | [jwt-failures.md](jwt-failures.md) |
| `SnsPushDeliveryFailure` | [push-failures.md](push-failures.md) |

Operational procedures:

| Procedure | Runbook |
|---|---|
| Deploy rollback | [deploy-rollback.md](deploy-rollback.md) |
| DB restore (PITR / per-table) | [db-restore.md](db-restore.md) |

## Quick reference

- **Dashboards**: Grafana `sns-overview` (`infra/grafana/sns-overview.json`).
- **Logs**: Loki — filter `app=sns-backend env=prod`. Correlate via `requestId`.
- **Traces**: Tempo / Jaeger — search by `X-Request-Id` header value.
- **Pager rotation**: see `PagerDuty → SNS` service; primary rotates weekly.
- **Rollback**: `helm rollback sns <previous-revision>` — never force-push to `main`.
