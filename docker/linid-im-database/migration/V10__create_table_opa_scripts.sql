CREATE TABLE IF NOT EXISTS opa_scripts
(
    opa_id      UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    app_id      UUID REFERENCES applications (app_id) ON DELETE CASCADE,
    script      TEXT         NOT NULL,
    checksum    VARCHAR(64)  NOT NULL, -- TODO: comment
    created_by  VARCHAR(128) NOT NULL,
    updated_by  VARCHAR(128) NOT NULL,
    insert_date TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Trigger pour mise à jour automatique
CREATE TRIGGER tg_opa_scripts_set_update_date
    BEFORE UPDATE
    ON opa_scripts
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE opa_scripts IS 'Table storing OPA Rego scripts that evaluate access and roles. One script per application or one global script (app_id NULL).';

COMMENT ON COLUMN opa_scripts.opa_id IS 'Primary key. UUID automatically generated for each script record.';
COMMENT ON COLUMN opa_scripts.app_id IS 'Foreign key referencing the application for which this script is defined. NULL if the script is global.';
COMMENT ON COLUMN opa_scripts.script IS 'The OPA Rego script. Expected to return a JSON object { access: boolean, roles: [] }.';
COMMENT ON COLUMN opa_scripts.checksum IS 'Deterministic hash (e.g. SHA-256) computed from the script content. Used for change detection, integrity verification, and to trigger policy reload or cache invalidation when the Rego definition is modified.';
COMMENT ON COLUMN opa_scripts.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN opa_scripts.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN opa_scripts.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN opa_scripts.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON TRIGGER tg_opa_scripts_set_update_date ON opa_scripts IS 'Trigger executed before each UPDATE to automatically set update_date to the current timestamp via update_timestamp() function.';