-- Chat messages, device push tokens, and an at-least-once outbox used by the
-- push worker to stream match + chat notifications to FCM and APNs.

CREATE TABLE chat_messages (
    message_id    UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id      UUID        NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    from_user_id  UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    to_user_id    UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    content       VARCHAR(4000) NOT NULL,
    read_flag     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Canonical-ordered pair index speeds up history queries in both directions.
CREATE INDEX idx_chat_event_pair_time ON chat_messages
  (event_id, LEAST(from_user_id, to_user_id), GREATEST(from_user_id, to_user_id), created_at DESC);
CREATE INDEX idx_chat_to_unread ON chat_messages (to_user_id, read_flag) WHERE read_flag = FALSE;

CREATE TABLE device_tokens (
    token_id     UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    platform     VARCHAR(10) NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    token        TEXT        NOT NULL,
    app_version  VARCHAR(40),
    last_seen    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_device_tokens_user_token ON device_tokens (user_id, token);

CREATE TABLE push_outbox (
    outbox_id    UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    kind         VARCHAR(40) NOT NULL,
    payload      JSONB       NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','DELIVERED','FAILED')),
    attempts     SMALLINT    NOT NULL DEFAULT 0,
    last_error   TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    delivered_at TIMESTAMPTZ
);
CREATE INDEX idx_push_outbox_pending ON push_outbox (status, created_at) WHERE status = 'PENDING';
