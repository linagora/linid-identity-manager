CREATE TABLE IF NOT EXISTS organizational_unit_accounts_audit
(
    oaa_id     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    oua_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_organizational_unit_accounts()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    payload_json    JSONB;
    organizational_unit_json    JSONB;
    account_json    JSONB;
    changed_by_json JSONB;
BEGIN
    SELECT to_jsonb(a)
    INTO organizational_unit_json
    FROM organizational_units a
    WHERE a.oun_id = COALESCE(NEW.oun_id, OLD.oun_id);

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
                'organizational_unit_accounts', to_jsonb(OLD),
                'organizational_unit', organizational_unit_json,
                'account', account_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO organizational_unit_accounts_audit(oua_id, operation, payload)
        VALUES (OLD.oua_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'organizational_unit_accounts', to_jsonb(New),
                'organizational_unit', organizational_unit_json,
                'account', account_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO organizational_unit_accounts_audit(oua_id, operation, payload)
        VALUES (NEW.oua_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON organizational_unit_accounts TO audit_trigger_role;

ALTER FUNCTION f_audit_organizational_unit_accounts() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_organizational_unit_accounts_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON organizational_unit_accounts
    FOR EACH ROW
EXECUTE FUNCTION f_audit_organizational_unit_accounts();

REVOKE INSERT, UPDATE, DELETE ON organizational_unit_accounts_audit FROM PUBLIC;

GRANT INSERT ON organizational_unit_accounts_audit TO audit_trigger_role;

COMMENT ON TABLE organizational_unit_accounts_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the organizational_unit_accounts table, including the related organizational unit, account and actor JSONB snapshots.';

COMMENT ON COLUMN organizational_unit_accounts_audit.oaa_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN organizational_unit_accounts_audit.oua_id IS 'Identifier of the organizational_unit_accounts row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN organizational_unit_accounts_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN organizational_unit_accounts_audit.payload IS 'JSONB snapshot of the changed row, the related organizational_unit_account, and the actor who performed the change.';
COMMENT ON COLUMN organizational_unit_accounts_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_organizational_unit_accounts_audit ON organizational_unit_accounts IS 'Trigger that invokes f_audit_organizational_unit_accounts() after every INSERT, UPDATE or DELETE on organizational_unit_accounts to capture an audit record.';
