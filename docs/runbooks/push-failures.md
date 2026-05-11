# Runbook: `SnsPushDeliveryFailure`

> **Status (v1):** Real push delivery is **deferred**. The push pipeline (`push_outbox` →
> `@Scheduled` drain → `PushGatewayRouter`) routes everything to `LoggingPushGateway`,
> which marks rows DELIVERED in logs. Operationally, this alert can only fire if a future
> Web Push gateway is wired up and starts failing.
>
> Keep this file as the skeleton for the Web Push runbook when it lands.

## Future shape (when Web Push ships)

Push outbox rows moved to `FAILED` status > 5/s for 10 minutes.

### Diagnose
1. `SELECT last_error, count(*) FROM push_outbox WHERE status='FAILED' AND created_at > now() - INTERVAL '30m' GROUP BY 1 ORDER BY 2 DESC;`
2. Common Web Push root causes:
   - **`410 Gone`** → endpoint expired; user uninstalled or revoked. Expected baseline; only actionable if volume spikes.
   - **`401 Unauthorized`** → VAPID keypair rotated but the JWT signer still uses the old key. Roll pods.
   - **`429 Too Many Requests`** → backing off from FCM Web; honour the `Retry-After` header.

### Remediate
- Stale endpoints: enable the cleanup job `sns.push.prune-stale-tokens` (flag) — deletes tokens
  last seen more than 60 days ago.
- VAPID rotation: deploy the new keypair via secret manager, roll the pods.
- Upstream outage: do nothing; outbox retries up to `MAX_ATTEMPTS` (5).

### Escalate
If the outbox grows beyond 10k rows pending, scale the drain interval via
`sns.push.drain-interval-ms` (lower = faster) and provision more workers.
