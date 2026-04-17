-- SNS (Facebook, LinkedIn) OAuth2 linked accounts. Tokens are stored as AES-256-GCM
-- ciphertext (BYTEA) with a random IV per record; decryption uses the key derived from
-- the `sns.crypto.master-key` configuration value.

CREATE TABLE sns_links (
    sns_id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    provider            VARCHAR(16) NOT NULL CHECK (provider IN ('FACEBOOK','LINKEDIN')),
    provider_user_id    VARCHAR(120) NOT NULL,
    access_token_enc    BYTEA,
    refresh_token_enc   BYTEA,
    token_iv            BYTEA,
    token_expires_at    TIMESTAMPTZ,
    last_fetch          TIMESTAMPTZ,
    imported_data       JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_sns_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_sns_links_provider ON sns_links (provider);
