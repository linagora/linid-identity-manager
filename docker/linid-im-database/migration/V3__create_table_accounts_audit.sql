CREATE TABLE IF NOT EXISTS accounts_audit
(
    aca_id     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    act_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_audit_account()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    payload_json JSONB;
    actor        JSONB;
BEGIN
    SELECT to_jsonb(a)
    INTO actor
    FROM accounts a
    WHERE a.external_id = COALESCE(NEW.updated_by, OLD.updated_by);

    IF TG_OP = 'DELETE' THEN
        payload_json := jsonb_build_object(
                'account', to_jsonb(OLD),
                'changed_by', actor
                        );
        INSERT INTO accounts_audit(act_id, operation, payload)
        VALUES (OLD.act_id, TG_OP, payload_json);
        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'account', to_jsonb(NEW),
                'changed_by', actor
                        );
        INSERT INTO accounts_audit(act_id, operation, payload)
        VALUES (NEW.act_id, TG_OP, payload_json);
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION f_audit_account() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_accounts_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON accounts
    FOR EACH ROW
EXECUTE FUNCTION f_audit_account();

REVOKE INSERT, UPDATE, DELETE ON accounts_audit FROM PUBLIC;

GRANT INSERT ON accounts_audit TO audit_trigger_role;