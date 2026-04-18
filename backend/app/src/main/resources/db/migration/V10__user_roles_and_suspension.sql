-- Adds RBAC for the system-management web app (admin console under /admin).
-- Participant code paths still authenticate the same way; admin endpoints gate on `role`.
-- `suspended_at` is the "temporarily disabled" state distinct from GDPR soft-delete.

ALTER TABLE users
    ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'
        CHECK (role IN ('USER','ORGANIZER','ADMIN','SUPER_ADMIN')),
    ADD COLUMN suspended_at TIMESTAMPTZ;

-- Partial index — admin lookups are rare; participant lookups (the hot path) stay unaffected.
CREATE INDEX idx_users_role ON users (role) WHERE role <> 'USER';
