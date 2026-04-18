# Security

Operational reference for the security surface of SNS — keys, hardening choices, and the
boot-time gate that prevents accidentally shipping dev defaults to production.

## Boot-time gate (do this first)

`ProductionSecretsCheck` (active under `spring.profiles.active=prod`) refuses to start the app
if any of the following holds its dev default or is unset:

- `sns.qr.hmac-key`
- `sns.crypto.master-key`
- `sns.audit.ip-salt`
- `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY` (both required)

A failed boot logs the offending property names and points back here. Set every key listed under
**Keys** below before promoting an environment.

## Keys

| Key | Env var | Purpose | Rotation |
|---|---|---|---|
| JWT RS256 keypair | `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` | Signs access tokens. Public key published at `/.well-known/jwks.json`. Tokens validated for `iss=sns-conf` and `aud=sns.jwt.audience`. | Issue a new keypair, deploy with both keys in JWKS for at least 1 × refresh-token TTL (30 d), then retire the old key. |
| JWT audience | `JWT_AUDIENCE` (maps to `sns.jwt.audience`) | Required `aud` claim. Stops a token minted for a sibling environment from being accepted here. | Pin per environment; bump when sharing keypairs across environments is unavoidable (avoid). |
| QR HMAC key | `SNS_QR_HMAC_KEY` (maps to `sns.qr.hmac-key`) | HMAC-SHA256 over signed QR token payloads. | Rotate quarterly. `QrCodeService` keeps a single active key — plan a short deploy window where both old and new pods coexist. |
| SNS token master key | `SNS_CRYPTO_MASTER_KEY` (maps to `sns.crypto.master-key`) | AES-256-GCM seed for encrypting Facebook / LinkedIn tokens at rest in `sns_links.*_token_enc`. | Re-encrypt window: bring up the new key, run a one-off job that decrypts with old + encrypts with new, then retire the old. Future work: envelope encryption with per-record DEKs. |
| Audit IP salt | `SNS_AUDIT_IP_SALT` (maps to `sns.audit.ip-salt`) | SHA-256 salt applied before persisting request IP hashes into `audit_log`. | Rotate opportunistically. Old hashes stay valid; new ones are simply non-comparable to old. |
| Prometheus scrape token | `SNS_ACTUATOR_SCRAPE_TOKEN` (maps to `sns.actuator.scrape-token`) | Bearer token Prometheus uses to scrape `/actuator/prometheus` (constant-time compare). When unset, the endpoint falls back to JWT auth. | Rotate when the Prometheus deployment rotates its scrape config. |
| FCM service-account JSON | `SNS_PUSH_FCM_CREDENTIALS_JSON` | Signs FCM HTTP v1 requests. | Rotate in Firebase console + rolling restart of backend pods. |
| APNs .p8 signing key | `SNS_PUSH_APNS_SIGNING_KEY_PEM` + `SNS_PUSH_APNS_KEY_ID` + `SNS_PUSH_APNS_TEAM_ID` | Signs APNs JWT. | Rotate annually in Apple Developer portal; update env + rolling restart. |
| AWS KMS CMKs | Provisioned by Terraform (`kms_app` + `kms_data`) | RDS + S3 + ElastiCache SSE; app-level envelope encryption seed. | Annual automatic rotation enabled (`enable_key_rotation = true`). |
| Postgres master password | `SPRING_DATASOURCE_PASSWORD` | DB auth. | Managed via AWS Secrets Manager rotation; the `:app` Hikari pool picks up new creds on next connection. |

## Auth hardening

- **Rate limit buckets** (see `RateLimitFilter`):
  - `register_ip` — 5 / hour / IP (default; `sns.rate-limit.register-per-ip-per-hour`).
  - `login_ip` — 30 / hour / IP.
  - `login_email` — 10 / hour / lower(email). Slows credential stuffing without giving an attacker
    a DoS lever against legitimate users (deliberate — no account lockout).
  - `refresh_ip` — 60 / hour / IP.
  - Backend: in-memory by default; flip to Redisson via `sns.rate-limit.backend=redis` in prod.
- **Timing-safe login.** `AuthService.login` runs BCrypt against `PHANTOM_HASH` for unknown emails
  so wall-clock matches the bad-password branch — closes user-enumeration via timing.
- **Constant-time TAN compare.** `VerificationService.consumeTan` uses `MessageDigest.isEqual`.
- **Refresh-token reuse detection.** Presenting an already-revoked refresh token revokes the
  entire `replaced_by` chain plus every other live token for the user. Audit emits
  `auth.refresh.reuse_detected`.
- **Password policy** (`PasswordPolicy`): rejects passwords < 8 chars, equal to email local-part,
  or matching one of ~100 entries from the SecLists / RockYou top-100 (inlined). Generic error
  message ("Password is too common or matches your email") so probing attacks can't infer which
  rule fired.

## Transport hardening

- **CORS.** `sns.security.cors.allowed-origins` is a CSV used by both the HTTP filter chain
  (`SnsCorsConfiguration`) and the STOMP handshake (`WebSocketConfig`). Default empty = same-origin
  only. No `setAllowedOriginPatterns("*")` anywhere.
- **Headers.** `SecurityHeadersFilter` sets HSTS, X-Content-Type-Options, X-Frame-Options,
  Referrer-Policy, Permissions-Policy, and a tight CSP on every response. `/api/auth/**` adds
  `Cache-Control: no-store, no-cache, must-revalidate` + `Pragma: no-cache` so a misconfigured CDN
  can't cache bearer tokens.
- **TLS.** Terminated at the LB; the Helm `Ingress` template ties `tls.secretName` to the ACM cert
  produced by Terraform. Internal pod-to-pod traffic uses cluster-internal CA (default).
- **Upload safety.** `InterestController.upload` enforces a MIME allowlist
  (`application/pdf`, `text/plain`, `text/markdown`) and sniffs file magic bytes — rejects 415 on
  mismatch. `spring.servlet.multipart.max-file-size` and `max-request-size` cap at 10 MB.

## Audit log

- **Append-only at the database.** Flyway V9 installs a `BEFORE UPDATE OR DELETE` trigger that
  raises an exception unless the session GUC `app.audit_prune` is set. Compromised app code can't
  modify or wipe audit rows.
- **Retention.** `AuditLogPruneJob` (`@Scheduled`, default cron `0 30 3 * * *`) sets the GUC and
  pages 500-row deletes for rows older than `sns.audit.retention-days` (default 180). The prune
  job is the *only* code path that can legally DELETE.
- **Writes.** `AuditLogger` is invoked from `AuthService` (register, verify, complete, login,
  refresh, logout, refresh-reuse), `ProfileService` (update, soft_delete), `SnsService`
  (link, unlink, enrich), and `ExportController` (export.download). IPs are SHA-256-salted via
  `sns.audit.ip-salt` before storage; payloads pass through `PiiScrubber`.

## Actuator

- `/actuator/health` and `/actuator/health/**` — public.
- `/actuator/prometheus` — gated by `sns.actuator.scrape-token` (constant-time bearer compare).
  Authenticated users with a JWT can also hit it via the normal chain. When the token is unset
  the endpoint stays JWT-gated only.
- All other actuator endpoints are not exposed (`management.endpoints.web.exposure.include`
  intentionally narrow).

## Secrets handling

- Never committed to this repo. `.env.local.example` files are templates.
- Kubernetes: use External-Secrets Operator pointing at AWS Secrets Manager. The Helm chart
  expects `secretEnv.*` populated via `ExternalSecret → Secret → pod env`.
- Local dev: MailHog SMTP, Redis, Postgres, MinIO all come up via
  `infra/docker-compose.dev.yml` with hardcoded weak credentials — fine because the compose
  network is bound to loopback on the dev machine.

## Known residual risks

1. **Ephemeral JWT keypair in dev.** When `JWT_PRIVATE_KEY` is unset the backend generates a new
   RS256 keypair at each boot — every pod restart invalidates every live token. Never run prod
   without a persistent keypair (the boot-time gate enforces this).
2. **QR key single-version.** `QrCodeService` has one active HMAC key. Rotation needs a brief
   deploy window. Future work: support a keyring with `kid` prefix and accept signatures from N
   keys.
3. **SNS provider tokens are encrypted but not HSM-backed.** A compromise of
   `sns.crypto.master-key` in env reveals every linked account's OAuth token. For a hardened
   deploy wire the `AesGcmCipher` to AWS KMS `GenerateDataKey` + encrypt-under-DEK.
4. **No 2FA.** Account takeover via stolen password remains possible until WebAuthn / TOTP is
   added — separate feature, not security hardening of existing surface.
5. **No WAF / IDS / network-layer brute-force defence.** Rate-limit buckets cover the common
   cases at L7; blanket protection (fail2ban-style, per-AS abuse, etc.) belongs at the LB.

## Reporting

Please report security issues to `security@sns.example.com` (placeholder; wire up before launch).
We follow 90-day coordinated disclosure.
