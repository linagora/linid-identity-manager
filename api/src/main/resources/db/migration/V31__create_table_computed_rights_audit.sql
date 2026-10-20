CREATE TABLE IF NOT EXISTS computed_rights_audit
(
    cpra_id    UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    cpr_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_computed_rights()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    payload_json     JSONB;
    account_json     JSONB;
    application_json JSONB;
    changed_by_json  JSONB;
BEGIN
    SELECT to_jsonb(a)
    INTO account_json
    FROM accounts a
    WHERE a.act_id = COALESCE(NEW.act_id, OLD.act_id);

    SELECT to_jsonb(ap) - ARRAY['script', 'claims_template', 'configuration']
    INTO application_json
    FROM applications ap
    WHERE ap.app_id = COALESCE(NEW.app_id, OLD.app_id);

    SELECT to_jsonb(a)
    INTO changed_by_json
    FROM accounts a
    WHERE a.act_id = COALESCE(NEW.updated_by, OLD.updated_by);

    IF TG_OP = 'DELETE' THEN
        payload_json := jsonb_build_object(
                'computed_rights', to_jsonb(OLD),
                'account', account_json,
                'application', application_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO computed_rights_audit(cpr_id, operation, payload)
        VALUES (OLD.cpr_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'computed_rights', to_jsonb(NEW),
                'account', account_json,
                'application', application_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO computed_rights_audit(cpr_id, operation, payload)
        VALUES (NEW.cpr_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON computed_rights TO audit_trigger_role;

ALTER FUNCTION f_audit_computed_rights() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_computed_rights_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON computed_rights
    FOR EACH ROW
EXECUTE FUNCTION f_audit_computed_rights();

REVOKE INSERT, UPDATE, DELETE ON computed_rights_audit FROM PUBLIC;

GRANT INSERT ON computed_rights_audit TO audit_trigger_role;

COMMENT ON TABLE computed_rights_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the computed_rights table, including the related account and application (without its script, claims template and configuration) and the actor JSONB snapshots.';

COMMENT ON COLUMN computed_rights_audit.cpra_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN computed_rights_audit.cpr_id IS 'Identifier of the computed_rights row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN computed_rights_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN computed_rights_audit.payload IS 'JSONB snapshot of the changed row, the related account and application (without its script, claims template and configuration), and the actor who performed the change.';
COMMENT ON COLUMN computed_rights_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_computed_rights_audit ON computed_rights IS 'Trigger that invokes f_audit_computed_rights() after every INSERT, UPDATE or DELETE on computed_rights to capture an audit record.';
