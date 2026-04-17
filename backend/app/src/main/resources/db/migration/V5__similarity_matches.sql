-- Similarity matches computed per-event between pairs of participants. Canonical
-- ordering is enforced: user_id_a < user_id_b (lexicographic). The pair uniqueness
-- constraint lets us upsert idempotently without hunting for either direction.

CREATE TABLE similarity_matches (
    match_id         UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id         UUID        NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    user_id_a        UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    user_id_b        UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    similarity       REAL        NOT NULL,
    common_keywords  TEXT[]      NOT NULL DEFAULT '{}',
    mutual           BOOLEAN     NOT NULL DEFAULT FALSE,
    notified_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (user_id_a < user_id_b)
);

CREATE UNIQUE INDEX idx_matches_pair ON similarity_matches (event_id, user_id_a, user_id_b);
CREATE INDEX idx_matches_a ON similarity_matches (user_id_a, event_id);
CREATE INDEX idx_matches_b ON similarity_matches (user_id_b, event_id);
CREATE INDEX idx_matches_event_similarity ON similarity_matches (event_id, similarity DESC);
