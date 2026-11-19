CREATE TABLE IF NOT EXISTS group_managers_audit
(
    grma_id    UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    grm_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_group_managers()
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

    -- A manager removed by the ON DELETE CASCADE of its group is audited after the group row is deleted:
    -- the last audited state of the group is used instead.
    IF group_json IS NULL THEN
        SELECT ga.payload -> 'group'
        INTO group_json
        FROM groups_audit ga
        WHERE ga.grp_id = COALESCE(NEW.grp_id, OLD.grp_id)
        ORDER BY ga.operation = 'DELETE' DESC, ga.changed_at DESC
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
                'group_managers', to_jsonb(OLD),
                'group', group_json,
                'account', account_json,
                'changed_by', changed_by_json
                        );
        INSERT INTO group_managers_audit(grm_id, operation, payload)
        VALUES (OLD.grm_id, TG_OP, payload_json);
        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'group_managers', to_jsonb(NEW),
                'group', group_json,
                'account', account_json,
                'changed_by', changed_by_json
                        );
        INSERT INTO group_managers_audit(grm_id, operation, payload)
        VALUES (NEW.grm_id, TG_OP, payload_json);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON group_managers TO audit_trigger_role;
ALTER FUNCTION f_audit_group_managers() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_group_managers_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON group_managers
    FOR EACH ROW
EXECUTE FUNCTION f_audit_group_managers();

REVOKE INSERT, UPDATE, DELETE ON group_managers_audit FROM PUBLIC;
GRANT INSERT ON group_managers_audit TO audit_trigger_role;

COMMENT ON TABLE group_managers_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the group_managers table, including the related group, account and actor JSONB snapshots.';
COMMENT ON COLUMN group_managers_audit.grma_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN group_managers_audit.grm_id IS 'Identifier of the group_managers row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN group_managers_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN group_managers_audit.payload IS 'JSONB snapshot of the changed row, the related group and account, and the actor who performed the change. When the group or the account has just been deleted (ON DELETE CASCADE), its snapshot is its last audited state.';
COMMENT ON COLUMN group_managers_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON TRIGGER tg_group_managers_audit ON group_managers IS 'Trigger that invokes f_audit_group_managers() after every INSERT, UPDATE or DELETE on group_managers to capture an audit record.';
