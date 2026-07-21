CREATE TABLE IF NOT EXISTS application_rules_audit
(
    arua_id    UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    aru_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_application_rule()
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
                'application_rule', to_jsonb(OLD),
                'changed_by', changed_by_json
                        );

        INSERT INTO application_rules_audit(aru_id, operation, payload)
        VALUES (OLD.aru_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'application_rule', to_jsonb(NEW),
                'changed_by', changed_by_json
                        );

        INSERT INTO application_rules_audit(aru_id, operation, payload)
        VALUES (NEW.aru_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON application_rules TO audit_trigger_role;

GRANT SELECT ON accounts TO audit_trigger_role;

ALTER FUNCTION f_audit_application_rule() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_application_rules_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON application_rules
    FOR EACH ROW
EXECUTE FUNCTION f_audit_application_rule();

REVOKE INSERT, UPDATE, DELETE ON application_rules_audit FROM PUBLIC;

GRANT INSERT ON application_rules_audit TO audit_trigger_role;

COMMENT ON TABLE application_rules_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the application_rules table, including the actor JSONB snapshot.';

COMMENT ON COLUMN application_rules_audit.arua_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN application_rules_audit.aru_id IS 'Identifier of the application_rules row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN application_rules_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN application_rules_audit.payload IS 'JSONB snapshot of the changed row and the actor who performed the change.';
COMMENT ON COLUMN application_rules_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_application_rules_audit ON application_rules IS 'Trigger that invokes f_audit_application_rule() after every INSERT, UPDATE or DELETE on application_rules to capture an audit record.';
