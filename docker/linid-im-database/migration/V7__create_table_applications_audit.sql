CREATE TABLE IF NOT EXISTS applications_audit
(
    apa_id     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    app_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_applications()
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
    WHERE a.external_id = COALESCE(NEW.updated_by, OLD.updated_by);

    IF TG_OP = 'DELETE' THEN
        payload_json := jsonb_build_object(
                'application', to_jsonb(OLD),
                'changed_by', changed_by_json
                        );

        INSERT INTO applications_audit(app_id, operation, payload)
        VALUES (OLD.app_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'application', to_jsonb(NEW),
                'changed_by', changed_by_json
                        );

        INSERT INTO applications_audit(app_id, operation, payload)
        VALUES (NEW.app_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION f_audit_applications() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_applications_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON applications
    FOR EACH ROW
EXECUTE FUNCTION f_audit_applications();

REVOKE INSERT, UPDATE, DELETE ON applications_audit FROM PUBLIC;
GRANT INSERT ON applications_audit TO audit_trigger_role;