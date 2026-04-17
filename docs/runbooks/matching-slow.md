# Runbook: `SnsMatchRecomputeSlow`

Matching recompute p95 > 5 s for 10 minutes.

## Diagnose
1. Inspect `sns_matching_recompute_seconds_bucket` broken down by `event_id` (if tagged).
2. Count current participants per event: `SELECT event_id, count(*) FROM participations GROUP BY 1 ORDER BY 2 DESC;`
3. Check the average interest count per user. A user with 100+ interests inflates the TF-IDF vector.

## Remediate
- Large events (>1000 participants): raise `sns.matching.sweep-interval-ms` and rely on trigger-based recompute.
- Pathological user vectors: introduce a keyword cap (`KeywordExtractor.TOP_K`) via config.
- Consider incremental recompute (only pairs touching the changed user) — tracked in backlog.

## Escalate
Sustained slowness that delays match notifications by > 1 minute end-to-end: escalate to the algorithms team.
