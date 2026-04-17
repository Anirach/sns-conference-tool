-- Users, profiles, refresh tokens, audit log (Phase 1).

CREATE TABLE users (
    user_id         UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           CITEXT      NOT NULL UNIQUE,
    email_verified  BOOLEAN     NOT NULL DEFAULT FALSE,
    password_hash   VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_deleted_at ON users (deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE profiles (
    user_id              UUID         PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    last_name            VARCHAR(100),
    first_name           VARCHAR(100),
    academic_title       VARCHAR(100),
    institution          VARCHAR(200),
    profile_picture_url  TEXT,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE email_verifications (
    verification_id   UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    email             CITEXT      NOT NULL,
    tan_hash          VARCHAR(255) NOT NULL,
    verification_token UUID,
    consumed_at       TIMESTAMPTZ,
    expires_at        TIMESTAMPTZ NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_email_verifications_email ON email_verifications (email);

CREATE TABLE refresh_tokens (
    jti         UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    replaced_by UUID        REFERENCES refresh_tokens(jti) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);

CREATE TABLE audit_log (
    id              BIGSERIAL   PRIMARY KEY,
    actor_user_id   UUID,
    action          VARCHAR(80) NOT NULL,
    resource_type   VARCHAR(80),
    resource_id     VARCHAR(120),
    ip_hash         VARCHAR(64),
    payload         JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_actor_time ON audit_log (actor_user_id, created_at DESC);
CREATE INDEX idx_audit_action_time ON audit_log (action, created_at DESC);
