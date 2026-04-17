# backend/

Reserved for **pass 2**. Spring Boot 3.3 / Java 21 implementation per [docs/SNS-system.md §6](../docs/SNS-system.md).

## Pass-2 scope (planned)

- Gradle multi-module layout (`app/`, `modules/{identity,profile,interest,event,location,matching,chat,sns,notification}`, `common/`, `infrastructure/`).
- Flyway migrations V1–V7 for PostgreSQL + PostGIS schema (§7.1).
- JWT auth (RS256) with access/refresh rotation.
- REST endpoints per §14.1 OpenAPI contract (drop-in for MSW mocks used in pass 1).
- WebSocket + STOMP + Redis Pub/Sub fan-out for chat (§9).
- Similarity engine: OpenNLP keyword extraction + TF-IDF cosine (§10).
- SNS OAuth2 client (Facebook + LinkedIn) with encrypted-at-rest tokens (§12).
- FCM + APNs push dispatch via Redis Streams (§13).

Until then, the web frontend runs fully on MSW-mocked endpoints — see `web/lib/api/mocks/`.
