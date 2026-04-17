# Runbook: Database restore

When a Flyway migration, a faulty cascade delete, or a corrupt write has poisoned the primary DB
and rollback isn't sufficient.

## Prerequisites

- RDS automated backups retain 14 days (`backup_retention_period = 14` in Terraform).
- Point-in-time recovery is enabled by default on the RDS instance.
- The application expects the DB to contain migrations V1–V8; a restored snapshot must be at or
  above that version before routing traffic back.

## Diagnose

1. Confirm the scope: `SELECT count(*) FROM users WHERE deleted_at IS NOT NULL AND deleted_at > now() - INTERVAL '15 minutes';`
2. Inspect `audit_log` for recent `profile.soft_delete` / `profile.hard_delete` / admin activity.
3. Check `pg_stat_activity` for long-running or rogue transactions.

## Remediate — scenario A: bad data in a single table

Minimize blast radius by restoring a single table from a snapshot into a parallel instance,
exporting the bad rows, and replaying only the delta.

```bash
# 1. Restore the last good snapshot to a temporary instance.
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier sns-restore \
  --db-snapshot-identifier <snapshot-id>

# 2. pg_dump one table from the restore, reload into prod.
pg_dump -h sns-restore... -t chat_messages -Fc sns > chat.dump
pg_restore -h sns-primary... --single-transaction -d sns chat.dump
```

## Remediate — scenario B: entire DB corrupt

Point-in-time restore to a new instance, then flip the application's `SPRING_DATASOURCE_URL`.

```bash
# 1. PITR a few minutes before the incident.
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier sns-prod \
  --target-db-instance-identifier sns-prod-restore \
  --restore-time <ISO-8601>

# 2. Run Flyway against the restored instance (migration history carries over, no-op unless the
# restored snapshot predates V8 etc.).
kubectl -n prod create job sns-flyway-once \
  --from=deploy/sns -- \
  /bin/sh -c 'java -cp app.jar org.flywaydb.core.Main migrate -url=jdbc:postgresql://sns-prod-restore.../sns -user=sns -password=$PGP'

# 3. Update the Secret backing SPRING_DATASOURCE_URL.
kubectl -n prod rollout restart deploy/sns

# 4. Verify audit_log + chat_messages reconciled; update DR runbook with the observed RPO.
```

## Escalate

RTO > 30 minutes: declare a Sev-1 incident, inform Comms. Coordinate with support on proactive
user comms.
