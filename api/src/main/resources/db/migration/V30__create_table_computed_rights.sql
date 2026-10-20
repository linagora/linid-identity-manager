CREATE TABLE IF NOT EXISTS computed_rights
(
    cpr_id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    act_id                   UUID         NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    app_id                   UUID         NOT NULL REFERENCES applications (app_id) ON DELETE CASCADE,
    allow                    BOOLEAN      NOT NULL,
    roles                    JSONB        NOT NULL DEFAULT '[]'::JSONB,
    claims                   JSONB        NOT NULL,
    payload_checksum         TEXT         NOT NULL,
    script_checksum          TEXT         NOT NULL,
    claims_template_checksum TEXT         NOT NULL,
    sync_instance_id         VARCHAR(255) NOT NULL,
    sync_job_execution_id    BIGINT       NOT NULL,
    created_by               UUID         NOT NULL,
    updated_by               UUID         NOT NULL,
    insert_date              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_computed_rights_act_id_app_id UNIQUE (act_id, app_id),
    CONSTRAINT ck_computed_rights_roles_array CHECK (jsonb_typeof(roles) = 'array')
);

CREATE INDEX idx_computed_rights_app_id ON computed_rights (app_id);

CREATE TRIGGER tg_computed_rights_set_update_date
    BEFORE UPDATE
    ON computed_rights
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE computed_rights IS 'Table storing the rights computed by sync-api for each account and application pair (allow, roles and claims), with the checksums used by the computation and the sync-api run that wrote it. A failed computation leaves the row unchanged, so that its outdated checksums select the pair again at the next cycle.';

COMMENT ON COLUMN computed_rights.cpr_id IS 'Primary key. UUID automatically generated for each account and application pair.';
COMMENT ON COLUMN computed_rights.act_id IS 'Identifier of the account. Foreign key to accounts, cascaded on delete.';
COMMENT ON COLUMN computed_rights.app_id IS 'Identifier of the application. Foreign key to applications, cascaded on delete.';
COMMENT ON COLUMN computed_rights.allow IS 'Allow value returned by OPA for the account on the application.';
COMMENT ON COLUMN computed_rights.roles IS 'JSONB array of the application roles returned by OPA for the account.';
COMMENT ON COLUMN computed_rights.claims IS 'JSONB claims computed from the OPA result and the claims template of the application.';
COMMENT ON COLUMN computed_rights.payload_checksum IS 'Checksum of the account payload (account_payloads.checksum) used by the computation.';
COMMENT ON COLUMN computed_rights.script_checksum IS 'Checksum of the application script (applications.script_checksum) used by the computation.';
COMMENT ON COLUMN computed_rights.claims_template_checksum IS 'Checksum of the application claims template (applications.claims_template_checksum) used by the computation.';
COMMENT ON COLUMN computed_rights.sync_instance_id IS 'Identifier of the sync-api instance that wrote the row.';
COMMENT ON COLUMN computed_rights.sync_job_execution_id IS 'Identifier of the Spring Batch job execution that wrote the row.';
COMMENT ON COLUMN computed_rights.created_by IS 'Identifier of the technical account of sync-api that created this record.';
COMMENT ON COLUMN computed_rights.updated_by IS 'Identifier of the technical account of sync-api that last updated this record.';
COMMENT ON COLUMN computed_rights.insert_date IS 'Date and time when the computed rights record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN computed_rights.update_date IS 'Date and time of the last successful computation. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_computed_rights_act_id_app_id ON computed_rights IS 'Ensures that an account and application pair has at most one rights row. Its index also serves the lookups by account.';
COMMENT ON CONSTRAINT ck_computed_rights_roles_array ON computed_rights IS 'Ensures that roles is a JSONB array, so that a malformed OPA result is rejected at write time.';

COMMENT ON INDEX idx_computed_rights_app_id IS 'Index on application identifier to optimize the lookups by application and the cascade deletion of an application.';

COMMENT ON TRIGGER tg_computed_rights_set_update_date ON computed_rights IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
