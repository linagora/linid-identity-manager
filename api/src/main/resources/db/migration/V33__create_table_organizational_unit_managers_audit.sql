CREATE TABLE IF NOT EXISTS organizational_unit_managers_audit
(
    ouma_id    UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    oum_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_organizational_unit_managers()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    payload_json             JSONB;
    organizational_unit_json JSONB;
    account_json             JSONB;
    changed_by_json          JSONB;
BEGIN
    SELECT to_jsonb(o)
    INTO organizational_unit_json
    FROM organizational_units o
    WHERE o.oun_id = COALESCE(NEW.oun_id, OLD.oun_id);

    -- A manager removed by the ON DELETE CASCADE of its organizational unit is audited after the organizational
    -- unit row is deleted: the last audited state of the organizational unit is used instead.
    IF organizational_unit_json IS NULL THEN
        SELECT oua.payload -> 'organizational_unit'
        INTO organizational_unit_json
        FROM organizational_unit_audit oua
        WHERE oua.oun_id = COALESCE(NEW.oun_id, OLD.oun_id)
        ORDER BY oua.operation = 'DELETE' DESC, oua.changed_at DESC
        LIMIT 1;
    END IF;

    SELECT to_jsonb(a)
    INTO account_json
    FROM accounts a
    WHERE a.act_id = COALESCE(NEW.act_id, OLD.act_id);

    -- Same for a manager removed by the ON DELETE CASCADE of its account.
    IF account_json IS NULL THEN
        SELECT aa.payload -> 'account'
        INTO account_json
        FROM accounts_audit aa
        WHERE aa.act_id = COALESCE(NEW.act_id, OLD.act_id)
        ORDER BY aa.operation = 'DELETE' DESC, aa.changed_at DESC
        LIMIT 1;
    END IF;

    SELECT to_jsonb(a)
    INTO changed_by_json
    FROM accounts a
    WHERE a.act_id = COALESCE(NEW.updated_by, OLD.updated_by);

    IF TG_OP = 'DELETE' THEN
        payload_json := jsonb_build_object(
                'organizational_unit_managers', to_jsonb(OLD),
                'organizational_unit', organizational_unit_json,
                'account', account_json,
                'changed_by', changed_by_json
                        );
        INSERT INTO organizational_unit_managers_audit(oum_id, operation, payload)
        VALUES (OLD.oum_id, TG_OP, payload_json);
        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'organizational_unit_managers', to_jsonb(NEW),
                'organizational_unit', organizational_unit_json,
                'account', account_json,
                'changed_by', changed_by_json
                        );
        INSERT INTO organizational_unit_managers_audit(oum_id, operation, payload)
        VALUES (NEW.oum_id, TG_OP, payload_json);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON organizational_unit_managers TO audit_trigger_role;
ALTER FUNCTION f_audit_organizational_unit_managers() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_organizational_unit_managers_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON organizational_unit_managers
    FOR EACH ROW
EXECUTE FUNCTION f_audit_organizational_unit_managers();

REVOKE INSERT, UPDATE, DELETE ON organizational_unit_managers_audit FROM PUBLIC;
GRANT INSERT ON organizational_unit_managers_audit TO audit_trigger_role;

COMMENT ON TABLE organizational_unit_managers_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the organizational_unit_managers table, including the related organizational unit, account and actor JSONB snapshots.';
COMMENT ON COLUMN organizational_unit_managers_audit.ouma_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN organizational_unit_managers_audit.oum_id IS 'Identifier of the organizational_unit_managers row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN organizational_unit_managers_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN organizational_unit_managers_audit.payload IS 'JSONB snapshot of the changed row, the related organizational unit and account, and the actor who performed the change. When the organizational unit or the account has just been deleted (ON DELETE CASCADE), its snapshot is its last audited state.';
COMMENT ON COLUMN organizational_unit_managers_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON TRIGGER tg_organizational_unit_managers_audit ON organizational_unit_managers IS 'Trigger that invokes f_audit_organizational_unit_managers() after every INSERT, UPDATE or DELETE on organizational_unit_managers to capture an audit record.';
