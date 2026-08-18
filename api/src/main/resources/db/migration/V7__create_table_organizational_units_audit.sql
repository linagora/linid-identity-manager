CREATE TABLE IF NOT EXISTS organizational_unit_audit
(
    ouna_id    UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    oun_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_organizational_unit()
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
                'organizational_unit', to_jsonb(OLD),
                'changed_by', changed_by_json
                        );

        INSERT INTO organizational_unit_audit(oun_id, operation, payload)
        VALUES (OLD.oun_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'organizational_unit', to_jsonb(NEW),
                'changed_by', changed_by_json
                        );

        INSERT INTO organizational_unit_audit(oun_id, operation, payload)
        VALUES (NEW.oun_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON organizational_units TO audit_trigger_role;

GRANT SELECT ON accounts TO audit_trigger_role;

ALTER FUNCTION f_audit_organizational_unit() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_organizational_unit_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON organizational_units
    FOR EACH ROW
EXECUTE FUNCTION f_audit_organizational_unit();

REVOKE INSERT, UPDATE, DELETE ON organizational_unit_audit FROM PUBLIC;

GRANT INSERT ON organizational_unit_audit TO audit_trigger_role;

COMMENT ON TABLE organizational_unit_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the organizational_units table, including the actor JSONB snapshot.';

COMMENT ON COLUMN organizational_unit_audit.ouna_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN organizational_unit_audit.oun_id IS 'Identifier of the organizational_units row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN organizational_unit_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN organizational_unit_audit.payload IS 'JSONB snapshot of the changed row and the actor who performed the change.';
COMMENT ON COLUMN organizational_unit_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_organizational_unit_audit ON organizational_units IS 'Trigger that invokes f_audit_organizational_unit() after every INSERT, UPDATE or DELETE on organizational_units to capture an audit record.';
