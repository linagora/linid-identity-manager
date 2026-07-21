CREATE TABLE IF NOT EXISTS application_rules
(
    aru_id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    app_id          UUID         NOT NULL REFERENCES applications (app_id) ON DELETE CASCADE,
    code            VARCHAR(255) NOT NULL,
    description     VARCHAR(255),
    priority        INTEGER      NOT NULL,
    script          TEXT         NOT NULL,
    script_checksum TEXT         NOT NULL,
    disabled        BOOLEAN      NOT NULL DEFAULT true,
    created_by      UUID         NOT NULL,
    updated_by      UUID         NOT NULL,
    insert_date     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_application_rules_app_id_code UNIQUE (app_id, code)
);

CREATE INDEX idx_application_rules_app_id ON application_rules (app_id);

CREATE TRIGGER tg_application_rules_set_update_date
    BEFORE UPDATE
    ON application_rules
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE application_rules IS 'Table storing executable rules attached to an application, each defining a prioritized script with enable/disable control and audit data.';

COMMENT ON COLUMN application_rules.aru_id IS 'Primary key. UUID automatically generated for each application rule.';
COMMENT ON COLUMN application_rules.app_id IS 'Identifier of the application the rule belongs to. Foreign key to applications, cascaded on delete.';
COMMENT ON COLUMN application_rules.code IS 'Functional identifier of the rule, unique within a given application.';
COMMENT ON COLUMN application_rules.description IS 'Optional free-text description of the rule.';
COMMENT ON COLUMN application_rules.priority IS 'Execution priority of the rule. Lower values are executed first.';
COMMENT ON COLUMN application_rules.script IS 'OPA Rego policy script computing the access rights granted by the rule.';
COMMENT ON COLUMN application_rules.script_checksum IS 'Deterministic hash (SHA-256) computed from the script. Used to detect changes to the script.';
COMMENT ON COLUMN application_rules.disabled IS 'Whether the rule is disabled. Rules are disabled by default on creation.';
COMMENT ON COLUMN application_rules.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN application_rules.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN application_rules.insert_date IS 'Date and time when the application rule record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN application_rules.update_date IS 'Date and time when the application rule record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_application_rules_app_id_code ON application_rules IS 'Ensures that a rule code is unique within a given application.';

COMMENT ON INDEX idx_application_rules_app_id IS 'Index on application identifier to optimize lookups and joins on application_rules by application.';

COMMENT ON TRIGGER tg_application_rules_set_update_date ON application_rules IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
