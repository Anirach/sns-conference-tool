# Runbook: `SnsWebSocketReconnectStorm`

Clients are reconnecting faster than expected (>50 CONNECTs/s over 5m).

## Diagnose

1. Sample a failing CONNECT with packet capture or DevTools network tab.
2. Inspect `level=WARN logger=com.sns.chat.ws` for CONNECT rejections.
3. Common causes: JWT decoder misconfigured, upstream LB terminating idle WS too aggressively,
   client retry loop without backoff.

## Remediate

- If JWT decode errors correlate: check that `JWT_PUBLIC_KEY` env is populated on all replicas.
- LB-level: nudge nginx `proxy_read_timeout` / ALB idle timeout up to 3600s.
- Client regression: freeze web release; previous version has working reconnect backoff.

## Escalate

If the storm causes pod OOMs (`jvm_memory_used_bytes > limit`), page platform.
