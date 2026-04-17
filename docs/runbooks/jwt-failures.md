# Runbook: `SnsJwtDecodeErrors`

Authentication failure rate > 10/s for 10 minutes.

## Diagnose
1. Logs: `level=WARN logger=org.springframework.security.oauth2`.
2. Is the error "Invalid signature" / "Expired at" / "Malformed"?
3. Has the signing key been rotated recently? Ensure all replicas picked up the new public key
   (check `/.well-known/jwks.json` on each pod).

## Remediate
- Key rotation without propagation: trigger a rolling restart.
- Clock skew: verify NTP on all nodes.
- Malformed tokens flooding from a single client: coordinate with web / mobile owner to rate-limit
  or block the offending version.

## Escalate
If genuine users cannot log in (4xx on `/api/auth/login` rising), page oncall and prepare to
rollback the last identity-module deploy.
