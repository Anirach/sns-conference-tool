# Security

Operational notes for the crypto material SNS depends on. Every key listed here has a dev default
checked into config (`dev-...`) — replace those before a non-local deploy.

## Keys

| Key | Env var | Purpose | Rotation |
|---|---|---|---|
| JWT RS256 keypair | `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` | Signs access tokens. Public key published at `/.well-known/jwks.json`. | Rotate by issuing a new keypair and deploying with both keys in JWKS for 1 × refresh-token TTL (30d), then retire the old. |
| SNS token master key | `SNS_CRYPTO_MASTER_KEY` (maps to `sns.crypto.master-key`) | AES-256-GCM seed for encrypting Facebook / LinkedIn tokens at rest in `sns_links.*_token_enc`. | Use envelope encryption with per-record DEKs in a future iteration. For now, plan a re-encrypt window: bring up the new key, run a one-off job that decrypts with old + encrypts with new, then retire old. |
| QR HMAC key | `SNS_QR_HMAC_KEY` (maps to `sns.qr.hmac-key`) | HMAC-SHA256 over signed QR token payloads. | Rotate quarterly. `QrCodeService` does not accept multiple keys — plan a short overlap where both the old and new backend pods run, then cut over. |
| Audit IP salt | `SNS_AUDIT_IP_SALT` (maps to `sns.audit.ip-salt`) | SHA-256 salt applied before persisting request IP hashes into `audit_log`. | Rotate opportunistically. Old hashes stay valid; new ones are simply non-comparable to old. |
| FCM service-account JSON | `SNS_PUSH_FCM_CREDENTIALS_JSON` | Signs FCM HTTP v1 requests. | Rotate in Firebase console + rolling restart of backend pods. |
| APNs .p8 signing key | `SNS_PUSH_APNS_SIGNING_KEY_PEM` + `SNS_PUSH_APNS_KEY_ID` + `SNS_PUSH_APNS_TEAM_ID` | Signs APNs JWT. | Rotate annually in Apple Developer portal; update env + rolling restart. |
| AWS KMS CMKs | Provisioned by Terraform (`kms_app` + `kms_data`) | RDS + S3 + ElastiCache SSE; app-level envelope encryption seed. | Annual automatic rotation enabled (`enable_key_rotation = true`). |
| Postgres master password | `SPRING_DATASOURCE_PASSWORD` | DB auth. | Managed via AWS Secrets Manager rotation; the `:app` Hikari pool picks up new creds on next connection. |

## Secrets handling

- Never committed to this repo. `.env.local.example` files are templates.
- Kubernetes: use External-Secrets Operator pointing at AWS Secrets Manager. Helm chart expects
  the `secretEnv.*` map, populated via ExternalSecret → Secret → pod env.
- Local dev: MailHog SMTP, Redis, Postgres, MinIO all come up via `infra/docker-compose.dev.yml`
  with hardcoded weak credentials. Those are fine because the compose network is bound to loopback
  on the dev machine.

## Known risks

1. **Ephemeral JWT keypair in dev**: when `JWT_PRIVATE_KEY` is unset, the backend generates a new
   RS256 keypair at each boot. This is intentional for dev ergonomics; it means every pod restart
   invalidates every live token. Never run prod without a persistent keypair.
2. **QR key single-version**: `QrCodeService` has one active HMAC key. Rotation needs a deploy
   window. Future work: support a keyring with key-id prefix and accept signatures from N keys.
3. **Audit log grow-unbounded**: `audit_log` has no TTL. Spec §11.7 calls for 180-day retention;
   a future Flyway migration should add a nightly pruning job (currently stubbed via the
   `HardDeleteJob` pattern).
4. **SNS provider tokens are encrypted but not HSM-backed**: a compromise of the
   `sns.crypto.master-key` in env reveals every linked account's OAuth token. For a hardened
   deploy wire the `AesGcmCipher` to AWS KMS `GenerateDataKey` + encrypt-under-DEK.

## Reporting

Please report security issues to `security@sns.example.com` (placeholder; wire up before launch).
We follow 90-day coordinated disclosure.
