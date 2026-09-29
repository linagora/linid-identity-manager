CREATE TABLE IF NOT EXISTS applications_audit
(
    apa_id     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    app_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_application()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    payload_json    JSONB;
    changed_by_json JSONB;
BEGIN
    SELECT to_jsonb(a)
    INTO changed_by_json
    FROM accounts a
    WHERE a.act_id = COALESCE(NEW.updated_by, NEW.created_by, OLD.updated_by, OLD.created_by);

    IF TG_OP = 'DELETE' THEN
        payload_json := jsonb_build_object(
                'application', to_jsonb(OLD),
                'changed_by', changed_by_json
                        );

        INSERT INTO applications_audit(app_id, operation, payload)
        VALUES (OLD.app_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'application', to_jsonb(NEW),
                'changed_by', changed_by_json
                        );

        INSERT INTO applications_audit(app_id, operation, payload)
        VALUES (NEW.app_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON applications TO audit_trigger_role;

GRANT SELECT ON accounts TO audit_trigger_role;

ALTER FUNCTION f_audit_application() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_applications_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON applications
    FOR EACH ROW
EXECUTE FUNCTION f_audit_application();

REVOKE INSERT, UPDATE, DELETE ON applications_audit FROM PUBLIC;

GRANT INSERT ON applications_audit TO audit_trigger_role;

COMMENT ON TABLE applications_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the applications table, including the actor JSONB snapshot.';

COMMENT ON COLUMN applications_audit.apa_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN applications_audit.app_id IS 'Identifier of the applications row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN applications_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN applications_audit.payload IS 'JSONB snapshot of the changed row and the actor who performed the change.';
COMMENT ON COLUMN applications_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_applications_audit ON applications IS 'Trigger that invokes f_audit_application() after every INSERT, UPDATE or DELETE on applications to capture an audit record.';
