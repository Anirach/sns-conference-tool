# Runbook: `SnsPgReplicationLag`

Standby replication lag > 30 seconds.

## Diagnose
1. On primary: `SELECT pid, client_addr, state, write_lag, flush_lag, replay_lag FROM pg_stat_replication;`
2. Check disk IO on standby (CloudWatch / equivalent).
3. Recent bulk writes (match sweep on a large event)?

## Remediate
- Short-term: reduce write pressure — raise `sns.matching.sweep-interval-ms`.
- Medium: increase standby instance class.
- Standby stuck: fail over and re-provision from snapshot.

## Escalate
If lag > 5m, page platform. Readers should be routed to primary until resolved.
