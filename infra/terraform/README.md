# Terraform

Minimal AWS composition for SNS — VPC + PostGIS RDS + ElastiCache Redis + S3 article bucket +
KMS + Route53 + ACM. Modules are intentionally small; wire them into new environments by
copying `environments/staging/` and tweaking `cidr` / sizing.

## Layout

```
modules/
  vpc/                  terraform-aws-modules/vpc wrapper
  rds-postgis/          PostGIS-ready RDS instance with KMS + multi-AZ
  elasticache-redis/    Redis replication group with TLS in-flight/at-rest
  s3/                   Versioned bucket with KMS SSE + public-access block
  kms/                  CMK + alias + annual rotation
  route53/              Lookup existing zone + create A / CNAME records
  acm/                  DNS-validated cert
environments/
  staging/              2 AZ, small instance sizes
  prod/                 3 AZ, m7g.large RDS + 3-node Redis
```

## Apply

```bash
cd infra/terraform/environments/staging
terraform init -backend-config=bucket=... -backend-config=region=...
terraform plan -out=plan.out
terraform apply plan.out
```

Secrets (DB password, JWT signing key, Facebook / LinkedIn client secrets, FCM JSON, APNs key)
are expected to come from a secret manager (AWS Secrets Manager / External-Secrets operator) and
are **not** declared here. The Helm chart references them via `secretEnv.*`.

## Drift check

```bash
terraform plan -detailed-exitcode
```
Exit code `2` means drift — investigate before applying.
