CREATE TABLE IF NOT EXISTS groups_audit
(
    gra_id     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    grp_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_group()
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
                'group', to_jsonb(OLD),
                'changed_by', changed_by_json
                        );

        INSERT INTO groups_audit(grp_id, operation, payload)
        VALUES (OLD.grp_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'group', to_jsonb(NEW),
                'changed_by', changed_by_json
                        );

        INSERT INTO groups_audit(grp_id, operation, payload)
        VALUES (NEW.grp_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON groups TO audit_trigger_role;

ALTER FUNCTION f_audit_group() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_groups_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON groups
    FOR EACH ROW
EXECUTE FUNCTION f_audit_group();

REVOKE INSERT, UPDATE, DELETE ON groups_audit FROM PUBLIC;

GRANT INSERT ON groups_audit TO audit_trigger_role;

COMMENT ON TABLE groups_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the groups table, including the actor JSONB snapshot.';

COMMENT ON COLUMN groups_audit.gra_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN groups_audit.grp_id IS 'Identifier of the groups row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN groups_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN groups_audit.payload IS 'JSONB snapshot of the changed row and the actor who performed the change.';
COMMENT ON COLUMN groups_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_groups_audit ON groups IS 'Trigger that invokes f_audit_group() after every INSERT, UPDATE or DELETE on groups to capture an audit record.';
