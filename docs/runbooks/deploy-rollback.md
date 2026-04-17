# Runbook: Deploy rollback

Used when a freshly-deployed backend version regresses SLO (error rate, latency, WS connect
failures) or a data-integrity issue is observed.

## Diagnose

1. Grafana → **SNS Overview** → confirm the regression started at the deploy.
2. `kubectl -n prod rollout history deploy/sns` — list known revisions.
3. `helm -n prod history sns` — Helm revision numbers match the Deployment's.

## Remediate

```bash
helm -n prod rollback sns <previous-revision> --wait --timeout=5m
kubectl -n prod rollout status deploy/sns
```

Confirm:
- All pods `READY`.
- `/actuator/health/readiness` green on each pod (`kubectl -n prod get endpoints sns`).
- Grafana 5xx rate returns to baseline within 2 minutes.

## Data-migration caveats

If the new version ran a Flyway migration that the previous version doesn't understand, the
rollback will fail to start. In that case, the safest path is forward-fix:

1. Revert the offending commit on `main` (git revert, not force-push).
2. Build a new container image.
3. Deploy that image. Migrations are idempotent and forward-only — Flyway won't re-run.
4. If the migration itself corrupted data, escalate to DB restore
   (see [db-restore.md](db-restore.md)).

## Escalate

If rollback fails and forward-fix is more than 15 minutes away, page the platform on-call and
post in `#sns-incident`.
