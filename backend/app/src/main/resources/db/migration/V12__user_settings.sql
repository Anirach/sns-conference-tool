-- Per-user preferences (Study-tab toggles). Previously frontend-only via localStorage;
-- moving server-side so they survive device switches and re-installs of the PWA.

CREATE TABLE user_settings (
    user_id        UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    push_matches   BOOLEAN NOT NULL DEFAULT TRUE,
    push_chat      BOOLEAN NOT NULL DEFAULT TRUE,
    gps_consent    BOOLEAN NOT NULL DEFAULT TRUE,
    keep_register  BOOLEAN NOT NULL DEFAULT FALSE,
    language       VARCHAR(8) NOT NULL DEFAULT 'en',
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One row per existing user with defaults so GET returns a usable shape before the user
-- ever touches the Study tab. New users get a row written lazily by UserSettingsService.
INSERT INTO user_settings (user_id) SELECT user_id FROM users
ON CONFLICT (user_id) DO NOTHING;
