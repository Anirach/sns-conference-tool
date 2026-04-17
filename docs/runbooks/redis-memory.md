# Runbook: `SnsRedisMemoryHigh`

Redis memory above 85% of `maxmemory` for 10 minutes.

## Diagnose
1. `redis-cli INFO memory` — confirm `used_memory` vs `maxmemory`.
2. `redis-cli --bigkeys` (sample) — which key namespace is dominant?
3. Expected namespaces: `ws:chat:*` (short-lived fan-out), `sns:matching:lock:*`, `rate:*`.

## Remediate
- Noisy key namespace: bump TTL down via ConfigMap `sns.cache.*-ttl` and rolling restart.
- Organic growth: scale up Redis tier (Sentinel → larger instance; Cluster: add shard).

## Escalate
If Redis evicts hot WS fan-out keys (`evicted_keys > 0`), chat delivery starts to drop. Page platform.
