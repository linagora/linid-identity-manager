CREATE TABLE IF NOT EXISTS group_accounts_audit
(
    gaa_id     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    gra_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_group_accounts()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    payload_json    JSONB;
    group_json      JSONB;
    account_json    JSONB;
    changed_by_json JSONB;
BEGIN
    SELECT to_jsonb(g)
    INTO group_json
    FROM groups g
    WHERE g.grp_id = COALESCE(NEW.grp_id, OLD.grp_id);

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
                'group_accounts', to_jsonb(OLD),
                'group', group_json,
                'account', account_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO group_accounts_audit(gra_id, operation, payload)
        VALUES (OLD.gra_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'group_accounts', to_jsonb(NEW),
                'group', group_json,
                'account', account_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO group_accounts_audit(gra_id, operation, payload)
        VALUES (NEW.gra_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON group_accounts TO audit_trigger_role;

ALTER FUNCTION f_audit_group_accounts() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_group_accounts_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON group_accounts
    FOR EACH ROW
EXECUTE FUNCTION f_audit_group_accounts();

REVOKE INSERT, UPDATE, DELETE ON group_accounts_audit FROM PUBLIC;

GRANT INSERT ON group_accounts_audit TO audit_trigger_role;

COMMENT ON TABLE group_accounts_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the group_accounts table, including the related group, account and actor JSONB snapshots.';

COMMENT ON COLUMN group_accounts_audit.gaa_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN group_accounts_audit.gra_id IS 'Identifier of the group_accounts row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN group_accounts_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN group_accounts_audit.payload IS 'JSONB snapshot of the changed row, the related group and account, and the actor who performed the change.';
COMMENT ON COLUMN group_accounts_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_group_accounts_audit ON group_accounts IS 'Trigger that invokes f_audit_group_accounts() after every INSERT, UPDATE or DELETE on group_accounts to capture an audit record.';
