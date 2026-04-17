# Runbook: `SnsPushDeliveryFailure`

Push outbox rows moved to `FAILED` status > 5/s for 10 minutes.

## Diagnose
1. `SELECT last_error, count(*) FROM push_outbox WHERE status='FAILED' AND created_at > now() - INTERVAL '30m' GROUP BY 1 ORDER BY 2 DESC;`
2. Common root causes:
   - **FCM**: `UNREGISTERED` → token is stale; expected baseline volume, only actionable if volume spikes.
   - **APNs**: `BadDeviceToken` → same. `InternalServerError` → check Apple status.
   - Gateway credentials misconfigured (freshly rotated, not propagated).

## Remediate
- Stale tokens: enable the cleanup job `sns.push.prune-stale-tokens` (flag) — it deletes tokens
  last seen more than 60 days ago.
- Credentials: rotate via secret manager, roll the pods.
- Upstream (FCM/APNs) outage: do nothing; the outbox will retry up to `MAX_ATTEMPTS` (5).

## Escalate
If the outbox grows beyond 10k rows pending, scale the drain interval via
`sns.push.drain-interval-ms` (lower = faster) and provision more workers.
