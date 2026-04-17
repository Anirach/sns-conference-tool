# Runbook: `SnsApiHighErrorRate`

Triggers when the 5xx rate on `sns-backend` exceeds 2% for 5 minutes.

## Diagnose

1. Dashboard: Grafana → **SNS Overview** → "5xx rate" panel. Spike concentrated on one route?
2. Logs:
   ```
   app=sns-backend env=prod level=ERROR
   | line_format "{{ .requestId }} {{ .msg }}"
   ```
   Look for repeated exception class names.
3. If the spike correlates with a recent deploy, check rollout status:
   ```
   kubectl -n prod rollout status deploy/sns
   ```

## Remediate

- Deploy-induced regression:
  ```
  helm -n prod rollback sns <previous-revision>
  ```
  Monitor the Grafana panel; error rate should drop within 2 minutes.
- Downstream dependency (Postgres / Redis):
  - Check `SnsPgReplicationLag`, `SnsRedisMemoryHigh` alerts.
  - If Postgres is degraded, the feature flag `sns.matching.sweep-interval-ms` can be raised via
    ConfigMap to reduce load; requires a rolling restart.
- Poison request (specific payload crashing a handler): identify via request id; coordinate with
  the API owner to roll out a hotfix.

## Escalate

- If error rate stays above 5% for 10 minutes after rollback, page the platform on-call.
- File a Sev-2 incident in the tracker with the request id trace attached.
