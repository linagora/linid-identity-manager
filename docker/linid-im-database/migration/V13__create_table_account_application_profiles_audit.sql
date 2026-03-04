CREATE TABLE IF NOT EXISTS account_application_profiles_audit
(
    aaa_id     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    aap_id     UUID,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION f_account_application_profiles_audit()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    payload_json     JSONB;
    account_json      JSONB;
    application_json JSONB;
    changed_by_json  JSONB;
BEGIN
    SELECT to_jsonb(a)
    INTO account_json
    FROM accounts a
    WHERE a.act_id = COALESCE(NEW.act_id, OLD.act_id);
    
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
                'account_application_profile', to_jsonb(OLD),
                'account', account_json,
                'application', application_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO account_application_profiles_audit(aap_id, operation, payload)
        VALUES (OLD.aap_id, TG_OP, payload_json);

        RETURN OLD;
    ELSE
        payload_json := jsonb_build_object(
                'account_application_profile', to_jsonb(NEW),
                'account', account_json,
                'application', application_json,
                'changed_by', changed_by_json
                        );

        INSERT INTO account_application_profiles_audit(aap_id, operation, payload)
        VALUES (NEW.aap_id, TG_OP, payload_json);

        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION f_account_application_profiles_audit() OWNER TO audit_trigger_role;

CREATE TRIGGER tg_account_application_profiles_audit
    AFTER INSERT OR UPDATE OR DELETE
    ON CREATE TRIGGER account_application_profiles
    FOR EACH ROW
EXECUTE FUNCTION f_account_application_profiles_audit();

REVOKE INSERT, UPDATE, DELETE ON account_application_profiles_audit FROM PUBLIC;
GRANT INSERT ON account_application_profiles_audit TO audit_trigger_role;
