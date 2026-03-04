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
    WHERE a.external_id = COALESCE(NEW.updated_by, OLD.updated_by);

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

ALTER FUNCTION f_audit_account_status() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_account_status_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON account_status
    FOR EACH ROW
EXECUTE FUNCTION f_audit_account_status();

REVOKE INSERT, UPDATE, DELETE ON account_status_audit FROM PUBLIC;
GRANT INSERT ON account_status_audit TO audit_trigger_role;
