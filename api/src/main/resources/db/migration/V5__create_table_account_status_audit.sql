CREATE TABLE IF NOT EXISTS account_status_audit
(
    asa_id     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    ast_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_account_status()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    payload_json    JSONB;
    account_json    JSONB;
    changed_by_json JSONB;
BEGIN
    SELECT to_jsonb(a)
    INTO account_json
    FROM accounts a
    WHERE a.act_id = COALESCE(NEW.act_id, OLD.act_id);

    SELECT to_jsonb(a)
    INTO changed_by_json
    FROM accounts a
    WHERE a.act_id = COALESCE(NEW.updated_by, OLD.updated_by);

    IF TG_OP = 'DELETE' THEN
        payload_json := jsonb_build_object(
                'account_status', to_jsonb(OLD),
                'account', account_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO account_status_audit(ast_id, operation, payload)
        VALUES (OLD.ast_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'account_status', to_jsonb(NEW),
                'account', account_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO account_status_audit(ast_id, operation, payload)
        VALUES (NEW.ast_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON account_status TO audit_trigger_role;

ALTER FUNCTION f_audit_account_status() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_account_status_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON account_status
    FOR EACH ROW
EXECUTE FUNCTION f_audit_account_status();

REVOKE INSERT, UPDATE, DELETE ON account_status_audit FROM PUBLIC;

GRANT INSERT ON account_status_audit TO audit_trigger_role;

COMMENT ON TABLE account_status_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the account_status table, including the related account and actor JSONB snapshots.';

COMMENT ON COLUMN account_status_audit.asa_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN account_status_audit.ast_id IS 'Identifier of the account_status row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN account_status_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN account_status_audit.payload IS 'JSONB snapshot of the changed row, the related account, and the actor who performed the change.';
COMMENT ON COLUMN account_status_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_account_status_audit ON account_status IS 'Trigger that invokes f_audit_account_status() after every INSERT, UPDATE or DELETE on account_status to capture an audit record.';
