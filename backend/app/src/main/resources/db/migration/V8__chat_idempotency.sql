-- Chat send idempotency — client supplies an opaque clientMessageId per logical send;
-- reconnect replays surface a duplicate and we return the original row.

ALTER TABLE chat_messages
    ADD COLUMN client_message_id VARCHAR(80);

CREATE UNIQUE INDEX idx_chat_client_message_id
    ON chat_messages (from_user_id, client_message_id)
    WHERE client_message_id IS NOT NULL;
