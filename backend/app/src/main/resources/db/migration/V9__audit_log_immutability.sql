-- Make audit_log append-only at the database level. UPDATE / DELETE on audit_log raises an
-- exception unless the session GUC `app.audit_prune` is set to 'on' — set by the prune job
-- before deleting, never touched anywhere else.

CREATE OR REPLACE FUNCTION audit_log_block_modification() RETURNS trigger AS $$
BEGIN
    IF current_setting('app.audit_prune', true) = 'on' THEN
        RETURN COALESCE(OLD, NEW);
    END IF;
    RAISE EXCEPTION 'audit_log is append-only (action=%)', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_immutable
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_block_modification();

-- Index supports the nightly retention sweep without a sequential scan.
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_log (created_at);
