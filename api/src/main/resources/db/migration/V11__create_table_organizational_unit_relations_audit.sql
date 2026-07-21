CREATE TABLE IF NOT EXISTS organizational_unit_relations_audit
(
    oura_id    UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    our_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_organizational_unit_relations()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    payload_json    JSONB;
    parent_json     JSONB;
    child_json      JSONB;
    changed_by_json JSONB;
BEGIN
    SELECT to_jsonb(o)
    INTO parent_json
    FROM organizational_units o
    WHERE o.oun_id = COALESCE(NEW.parent_id, OLD.parent_id);

    SELECT to_jsonb(o)
    INTO child_json
    FROM organizational_units o
    WHERE o.oun_id = COALESCE(NEW.child_id, OLD.child_id);

    SELECT to_jsonb(a)
    INTO changed_by_json
    FROM accounts a
    WHERE a.act_id = COALESCE(NEW.updated_by, OLD.updated_by);

    IF TG_OP = 'DELETE' THEN
        payload_json := jsonb_build_object(
                'organizational_unit_relation', to_jsonb(OLD),
                'parent', parent_json,
                'child', child_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO organizational_unit_relations_audit(our_id, operation, payload)
        VALUES (OLD.our_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'organizational_unit_relation', to_jsonb(NEW),
                'parent', parent_json,
                'child', child_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO organizational_unit_relations_audit(our_id, operation, payload)
        VALUES (NEW.our_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

GRANT SELECT ON organizational_unit_relations TO audit_trigger_role;

ALTER FUNCTION f_audit_organizational_unit_relations() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_organizational_unit_relations_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON organizational_unit_relations
    FOR EACH ROW
EXECUTE FUNCTION f_audit_organizational_unit_relations();

REVOKE INSERT, UPDATE, DELETE ON organizational_unit_relations_audit FROM PUBLIC;

GRANT INSERT ON organizational_unit_relations_audit TO audit_trigger_role;

COMMENT ON TABLE organizational_unit_relations_audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the organizational_unit_relations table, including the parent, child and actor JSONB snapshots.';

COMMENT ON COLUMN organizational_unit_relations_audit.oura_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN organizational_unit_relations_audit.our_id IS 'Identifier of the organizational_unit_relations row affected by the change. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN organizational_unit_relations_audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN organizational_unit_relations_audit.payload IS 'JSONB snapshot of the changed relation, its parent and child organizational units, and the actor who performed the change.';
COMMENT ON COLUMN organizational_unit_relations_audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_organizational_unit_relations_audit ON organizational_unit_relations IS 'Trigger that invokes f_audit_organizational_unit_relations() after every INSERT, UPDATE or DELETE on organizational_unit_relations to capture an audit record.';
