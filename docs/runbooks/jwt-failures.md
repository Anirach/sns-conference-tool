# Runbook: `SnsJwtDecodeErrors`

Authentication failure rate > 10/s for 10 minutes.

## Diagnose

1. **Audit log for theft signals.** Run
   `SELECT count(*) FROM audit_log WHERE action='auth.refresh.reuse_detected' AND created_at > now() - INTERVAL '15 min';`
   Refresh-token reuse means an old (revoked) refresh token was presented after the legitimate
   client rotated — classic theft signal. The reuse path also revokes the entire family for that
   user, so a spike here is followed by login churn from displaced sessions.
2. **Logs.** `level=WARN logger=org.springframework.security.oauth2` — typical messages:
   - `Invalid signature` → key mismatch (see §3).
   - `Expired at …` → expected at low volume; spikes mean clock drift.
   - `An error occurred while attempting to decode the Jwt: Issuer "…"` → cross-environment
     leakage. Verify the offending token's `iss` claim doesn't belong to a sibling environment
     sharing the keypair.
   - `An error occurred while attempting to decode the Jwt: Aud "…"` → audience mismatch. Check
     `sns.jwt.audience` matches the issuer's configured value.
3. **Brute force.** `SELECT action, count(*) FROM audit_log WHERE action LIKE 'auth.login.%' AND created_at > now() - INTERVAL '15 min' GROUP BY 1;`
   A flood of `auth.login.failure` against many emails is credential stuffing; rate-limit
   counters in Grafana (look for `429` rate spike on `/api/auth/login`) confirm the limiter is
   firing.
4. **Key rotation.** Has the signing key been rotated recently? Ensure all replicas serve the
   new public key (`curl /.well-known/jwks.json` on each pod).

## Remediate

- **Token-theft spike** (`auth.refresh.reuse_detected` > 0): coordinate with the affected users
  via support. The session-family revoke already kicked them back to password login. If the spike
  is broad-based, rotate `JWT_PRIVATE_KEY` to invalidate every outstanding token.
- **Issuer / audience mismatch**: confirm `sns.jwt.issuer` and `sns.jwt.audience` are the same
  values in env that the issuer side uses. Mismatch is almost always a config drift between
  staging and prod sharing keys.
- **Key rotation without propagation**: trigger a rolling restart.
- **Clock skew**: verify NTP on all nodes; `sns.jwt.clock-skew` is `PT30S` by default.
- **Malformed tokens flooding from one client**: coordinate with web / mobile owner to ship a
  client fix; in the meantime, the rate-limit on `/api/auth/refresh` (60/h/IP) blunts the volume.

## Escalate

If genuine users cannot log in (4xx on `/api/auth/login` rising) or if `auth.refresh.reuse_detected`
exceeds 100/min (suggesting a broad token-theft incident), page oncall, prepare to rollback the
last identity-module deploy, and consider rotating the JWT signing key to mass-invalidate.
