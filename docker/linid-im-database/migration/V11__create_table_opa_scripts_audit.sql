CREATE TABLE IF NOT EXISTS opa_scripts_audit
(
    osa_id     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    opa_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_opa_scripts()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    payload_json     JSONB;
    application_json JSONB;
    changed_by_json  JSONB;
BEGIN
    SELECT to_jsonb(a)
    INTO application_json
    FROM applications a
    WHERE a.app_id = COALESCE(NEW.app_id, OLD.app_id);

    SELECT to_jsonb(a)
    INTO changed_by_json
    FROM accounts a
    WHERE a.external_id = COALESCE(NEW.updated_by, OLD.updated_by);

    IF TG_OP = 'DELETE' THEN
        payload_json := jsonb_build_object(
                'opa_script', to_jsonb(OLD),
                'application', application_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO opa_scripts_audit(opa_id, operation, payload)
        VALUES (OLD.opa_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'opa_script', to_jsonb(NEW),
                'application', application_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO opa_scripts_audit(opa_id, operation, payload)
        VALUES (NEW.opa_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION f_audit_opa_scripts() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_opa_scripts_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON opa_scripts
    FOR EACH ROW
EXECUTE FUNCTION f_audit_opa_scripts();

REVOKE INSERT, UPDATE, DELETE ON opa_scripts_audit FROM PUBLIC;
GRANT INSERT ON opa_scripts_audit TO audit_trigger_role;